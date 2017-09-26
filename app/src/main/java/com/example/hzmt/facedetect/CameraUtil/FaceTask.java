package com.example.hzmt.facedetect.CameraUtil;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.PointF;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.media.FaceDetector;
import android.util.Log;
import android.widget.Toast;

import android.view.SurfaceView;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


import com.example.hzmt.facedetect.MyApplication;
import com.example.hzmt.facedetect.util.B64Util;
import com.example.hzmt.facedetect.util.HttpUtil;
import com.example.hzmt.facedetect.util.SystemUtil;

/**
 * Created by xun on 2017/8/29.
 */

public class FaceTask extends AsyncTask<Void, Void, FaceDetector.Face>{
    private Activity mActivity;
    private byte[] mData;
    private Camera mCamera;
    private int mCameraIdx;
    private ImageView mImgView;
    private SurfaceDraw mSurface;
    private SurfaceView mCameraView;
    private Bitmap mScreenBm;
    private Bitmap mSendBm;
    private byte[] mSendRawImage;

    //构造函数
    FaceTask(Activity activity, byte[] data, int cameraId, Camera camera,
             ImageView imgview, SurfaceDraw surface, SurfaceView cameraview){
        super();
        this.mActivity = activity;
        this.mData = data;
        this.mCamera = camera;
        this.mImgView = imgview;
        this.mSurface = surface;
        this.mCameraView = cameraview;
        this.mCameraIdx = cameraId;
    }

    @Override
    protected FaceDetector.Face doInBackground(Void... params) {
        // TODO Auto-generated method stub
        Camera.Size previewSize;
        try {
            previewSize = mCamera.getParameters().getPreviewSize();
        }
        catch(Exception e){
            return null;
        }


        YuvImage yuvimage = new YuvImage(
                mData,
                ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(
                new Rect(0, 0, previewSize.width, previewSize.height),
                80,
                baos);
        byte[] rawImage =baos.toByteArray();
        mScreenBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 4);

        // FaceDetector
        Bitmap bmcopy = mScreenBm.copy(Bitmap.Config.RGB_565, true); // 必须为RGB_565
        FaceDetector faceDetector = new FaceDetector(bmcopy.getWidth(),
                bmcopy.getHeight(), CameraActivityData.FaceDetectNum);
        FaceDetector.Face[] faces = new FaceDetector.Face[CameraActivityData.FaceDetectNum];
        int faceNum = faceDetector.findFaces(bmcopy, faces);
        if(faceNum > 0){
            //mSendBm = CameraMgt.getBitmapFromBytes(rawImage, mCameraIdx, 1);
            mSendBm = mScreenBm;
            mSendRawImage = rawImage;
            return faces[0];
        }
        else{
            return null;
        }
    }

    @Override
    protected void onPostExecute(FaceDetector.Face face) {
        try {
            if (face != null) {
                PointF pointF = new PointF();
                face.getMidPoint(pointF);//获取人脸中心点
                float eyesDistance = face.eyesDistance();//获取人脸两眼的间距

                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(mCameraIdx, cameraInfo); // get camerainfo
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    pointF.x = mScreenBm.getWidth() - pointF.x;
                }

                int maxX = mSurface.getWidth();
                int maxY = mSurface.getHeight();
                pointF.x = pointF.x * maxX / mScreenBm.getWidth();
                pointF.y = pointF.y * maxY / mScreenBm.getHeight();
                eyesDistance = eyesDistance * maxY / mScreenBm.getHeight();

                int l = (int) (pointF.x - eyesDistance);
                if (l < 0) l = 1;
                int t = (int) (pointF.y - eyesDistance);
                if (t < 0) t = 1;
                int r = (int) (pointF.x + eyesDistance);
                if (r > maxX) r = maxX - 1;
                int b = (int) (pointF.y + eyesDistance);
                if (b > maxY) b = maxY - 1;
                //String pres=l+","+t+","+r+","+b;
                //Toast.makeText(mActivity, pres, Toast.LENGTH_LONG).show();
                mSurface.setFaceRect(l, t, r, b);

                // http work
                /*
                sendBitmapData(bmcopy, MyApplication.FaceDetectUrl, new SendDataCallback(){
                    @Override
                    public void onSuccess(int l, int t, int w, int h){
                        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                        Camera.getCameraInfo(mCameraIdx, cameraInfo); // get camerainfo
                        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT){
                            l = mScreenBm.getWidth()-l-w;
                        }
                        mSurface.setFaceRect2(l*4,t*4,(l+w)*4,(t+h)*4);
                    }
                });*/
                //sendBitmapData(mScreenBm, MyApplication.FaceDetectUrl,null);
                FaceHttpThread httpth = new FaceHttpThread(mSendRawImage, mCameraIdx, MyApplication.FaceDetectUrl,null);
                httpth.start();

                //Thread.sleep(500);
            } else
                mSurface.setFaceRect(0, 0, 0, 0);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    private static void sendRawImageData(byte[] rawImage, int cameraIdx,
                                         String urlstring, final SendDataCallback callback){
        Bitmap bm = CameraMgt.getBitmapFromBytes(rawImage, cameraIdx, 1);
        sendBitmapData(bm, urlstring, callback);
    }

    private static void sendBitmapData(Bitmap bm, String urlstring, final SendDataCallback callback){
        String bmbase64 = "data:image/jpeg;base64," + B64Util.bitmapToBase64(bm);

        Map<String, String> map = new HashMap<>();
        map.put("imguri", bmbase64);
        //map.put("imguri", "");
        String macaddr = SystemUtil.getMacAddress();
        map.put("mac", macaddr);
        JSONObject object = new JSONObject(map);
        JSONObject resultJSON = HttpUtil.JsonObjectRequest(object, urlstring);
        if(resultJSON != null) {
            //int t = resultJSON.optInt("t");
            //int l = resultJSON.optInt("l");
            //int w = resultJSON.optInt("w");
            //int h = resultJSON.optInt("h");
            //if(callback != null)
            //    callback.onSuccess(l, t, w, h);
        }
    }

    public interface SendDataCallback{
        void onSuccess(int l, int t, int w, int h);
    }


    private static class FaceHttpThread extends Thread {
        private byte[] rawImage;
        private int cameraIdx;
        private Bitmap bm;
        private String url;
        private SendDataCallback cb;

        public FaceHttpThread(byte[] rawImage, int cameraIdx, String url, SendDataCallback cb) {
            this.rawImage = rawImage;
            this.cameraIdx = cameraIdx;
            this.bm = null;
            this.url = url;
            this.cb = cb;
        }

        public FaceHttpThread(Bitmap bm, String url, SendDataCallback cb) {
            this.rawImage = null;
            this.bm = bm;
            this.url = url;
            this.cb = cb;
        }

        @Override
        public void run() {
            if(null!=bm)
                sendBitmapData(bm, url, cb);
            else
                sendRawImageData(rawImage, cameraIdx, url, cb);
        }
    }
}
