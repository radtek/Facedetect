package com.example.hzmt.facedetect;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by xun on 2017/8/30.
 */

public class MyApplication extends Application {
    public static RequestQueue requestQueue;
    public static String FaceDetectUrl = "http://192.168.1.12:8070/AppFaceDetect";
    public static byte[] PhotoImageData = null;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
    }
}
