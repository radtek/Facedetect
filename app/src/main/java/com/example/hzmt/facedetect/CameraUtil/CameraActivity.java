package com.example.hzmt.facedetect.CameraUtil;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.widget.ImageView;
import android.widget.Toast;
import android.view.View;
import android.hardware.Camera;
import android.view.Window;
import android.view.WindowManager;
import android.content.Intent;

import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.LocationListener;
import android.provider.Settings;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import com.example.hzmt.facedetect.MyApplication;
import com.example.hzmt.facedetect.R;
import com.example.hzmt.facedetect.SubActivity;

public class CameraActivity extends AppCompatActivity {
    private static final int PERMISSION_FINE_LOCATION = 0;
    private static final int PERMISSION_COARSE_LOCATION = 1;
    private static final int PERMISSION_CAMERA = 2;
    private static final int TOAST_OFFSET_PERMISSION_RATIONALE = 100;
    List<Integer> mPermissionIdxList = new ArrayList<>();
    private CameraMgt mCameraMgt;
    private SurfaceDraw mFaceRect;
    private ImageView mImgView;
    private FaceTask mFaceTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去除title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //去掉Activity上面的状态栏
        getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
                WindowManager.LayoutParams. FLAG_FULLSCREEN);
        setContentView(R.layout.activity_camera);

        // intent data
        Intent intent=getIntent();
        if(intent!=null) {
            int reqtype = intent.getIntExtra("RequestType", -1);
            if(reqtype == -1)
                CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_LOGIN; // default
            else
                CameraActivityData.RequestType = reqtype;
        }
        else{
            // default
            CameraActivityData.RequestType = CameraActivityData.REQ_TYPE_LOGIN;
        }

        mImgView = (ImageView) findViewById(R.id.imageView);
        mImgView.setVisibility(View.INVISIBLE);
        mFaceRect = (SurfaceDraw) findViewById(R.id.surface_draw);
        mFaceRect.setVisibility(View.VISIBLE);
        mCameraMgt = new CameraMgt(this, R.id.camera_preview);


        // Android 6.0 运行时权限
        String[] permissions = new String[]
                {
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.CAMERA
                };
        List<String> permissionList = new ArrayList<>();

        permissionList.clear();
        mPermissionIdxList.clear();
        for(int i = 0; i < permissions.length; i++){
            if (ContextCompat.checkSelfPermission(this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(permissions[i]);
                mPermissionIdxList.add(i);
            }
        }

        if (permissionList.isEmpty()) {//未授予的权限为空，表示都授予了
            initWork();
        } else {//请求权限方法
            String[] reqpermissions = permissionList.toArray(new String[permissionList.size()]);//将List转为数组
            ActivityCompat.requestPermissions(this, reqpermissions, 1);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1)
        {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            permissions[i])){
                        // 仅此次禁止
                        permissionToast(mPermissionIdxList.get(i)+TOAST_OFFSET_PERMISSION_RATIONALE);
                    }
                    else{
                        //禁止且不再提示
                        permissionToast(mPermissionIdxList.get(i));
                    }
                }
                else{
                    // 允许
                }
            }

        }

        initWork();
    }

    private void initWork(){
        initLocation();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED){
            mCameraMgt.setPreviewCallback(mPreviewCB);
            mCameraMgt.setTakePictureJpegCallback(mTakePictrueJpegCB);
            mCameraMgt.openCamera();
        }
    }

    private void initLocation(){
        Boolean bFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        Boolean bCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        if(!bFine && !bCoarse)
            return;

        LocationManager locationManager = (LocationManager) getSystemService(this.LOCATION_SERVICE);
     //   LocationProvider gpsProvider = locationManager.getProvider(LocationManager.GPS_PROVIDER);
     //   LocationProvider netProvider = locationManager.getProvider(LocationManager.NETWORK_PROVIDER);

        if(bFine){
            // try gps
            if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        3000, 1.0f, mLocationListener);
                //获取Location
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if(location!=null){
                    //showLocation(location);
                }
                return;
            }

            // try network
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                if(bCoarse){
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            3000, 1.0f, mLocationListener);
                    Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if(location!=null){
                        //showLocation(location);
                    }
                    return;
                }
            }

            Toast.makeText(this, "无法定位，请打开定位服务", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 100);
        }
        else{
            // try network
            if(locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                        3000, 1.0f, mLocationListener);
                Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if(location!=null){
                    //showLocation(location);
                }
                return;
            }

            Toast.makeText(this, "无法定位，请打开定位服务", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(intent, 100);
        }
    }

    private void showLocation(Location location){
        String locationStr = " 纬度：" + location.getLatitude() +"   "
                + "经度：" + location.getLongitude();
        Toast.makeText(this, locationStr, Toast.LENGTH_SHORT).show();
    }

    private void permissionToast(int code){
        String msg;
        switch(code){
            case PERMISSION_FINE_LOCATION:
            case PERMISSION_COARSE_LOCATION:
                msg = "请在设置里打开定位服务权限以使用定位信息！";
                break;
            case PERMISSION_CAMERA:
                msg = "请在设置里打开摄像头使用权限！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_FINE_LOCATION:
                msg = "无法使用GPS定位服务！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_COARSE_LOCATION:
                msg = "无法使用网络定位服务！";
                break;
            case TOAST_OFFSET_PERMISSION_RATIONALE+PERMISSION_CAMERA:
                msg = "无法使用摄像头，请退出重试！";
                break;
            default:
                return;
        }

        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private Camera.PreviewCallback mPreviewCB = new Camera.PreviewCallback(){
        @Override
        public void onPreviewFrame(byte[] data, Camera camera){

            if(camera.getParameters().getPreviewFormat() != ImageFormat.NV21)
                return;

            //Camera.Size s = camera.getParameters().getPreviewSize();
            //String pres = s.width+" x " +s.height;
            //Toast.makeText(CameraActivity.this, pres, Toast.LENGTH_LONG).show();

            if(null != mFaceTask){
                switch(mFaceTask.getStatus()){
                    case RUNNING:
                        return;
                    case PENDING:
                        mFaceTask.cancel(false);
                        break;
                }
            }

            mFaceTask = new FaceTask(CameraActivity.this, data, mCameraMgt.getCurrentCameraId(),camera,
                    mImgView, mFaceRect, mCameraMgt.getCameraView());
            mFaceTask.execute((Void)null);
        }
    };

    private Camera.PictureCallback mTakePictrueJpegCB = new Camera.PictureCallback(){
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //Log.e("onPictureTaken","Take Picture success!");
            //Bitmap bm = CameraMgt.getBitmapFromBytes(data, mCameraMgt.getCurrentCameraId(), 1);
            //mFaceRect.setFacePic(bm);

            Intent intent = new Intent();
            intent.setClass(CameraActivity.this, SubActivity.class);
            //intent.putExtra("facedata", data);
            MyApplication.PhotoImageData = data;
            intent.putExtra("cameraid", mCameraMgt.getCurrentCameraId());
            //setResult(CameraActivityData.REQ_TYPE_REGISTER,intent);
            startActivity(intent);
            //CameraActivity.this.finish();

            //camera.startPreview();//重新开始预览
        }
    };

    private LocationListener mLocationListener = new LocationListener(){
        @Override
        public void onLocationChanged(Location location){
            //得到纬度
            double latitude = location.getLatitude();
            //得到经度
            double longitude = location.getLongitude();

            showLocation(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }

        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100){
            initLocation();
        }
    }

    @Override
    public void onBackPressed() {
        mCameraMgt.closeCamera();
        super.onBackPressed();
    }


}
