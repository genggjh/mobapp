package com.stackbase.mobapp.templates.views;

import java.io.IOException;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by bryan on 15/5/20.
 */
public class OCRCameraSurfacePreview  extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    public OCRCameraSurfacePreview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    public void surfaceCreated(SurfaceHolder holder) {

        Log.d("Dennis", "surfaceCreated() is called");

        try {
            // Open the Camera in preview mode
            mCamera = Camera.open();
            mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            Log.d("Dennis", "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        Log.d("Dennis", "surfaceChanged() is called");

        try {
            mCamera.startPreview();

        } catch (Exception e){
            Log.d("Dennis", "Error starting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }

        Log.d("Dennis", "surfaceDestroyed() is called");
    }

    public void takePicture(PictureCallback imageCallback) {
        mCamera.takePicture(null, null, imageCallback);
    }
}
