package edu.temple.androidintents;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import edu.temple.androidintents.util.OnSwipeTouchListener;

@SuppressWarnings("deprecation")
public class CameraActivity extends Activity implements SurfaceHolder.Callback{

    //Camera Controls
    LinearLayout cameraControls, cameraControlsSpacer, cameraControlsShutter, cameraControlsSender;

    SharedPreferences settings;
    SurfaceHolder holder;

    ImageView cameraFlashControl, sendImage, shutterImage;
    File imageFile;
    String videoFileName = "";
    Camera mCamera;

    private boolean hasFlash;
    private boolean pictureTaken;
    Camera.PreviewCallback previewCallback;
    OrientationEventListener orientationListener;

    private int imageWidth = 1024;
    private int imageHeight = 1024;

    private boolean saveFile;

    private String LAST_CAMERA_PREFERENCE = "us.textr.last_camera_preference";
    private String LAST_CAMERA_FLASH_PREFERENCE = "us.textr.camera_flash";

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        settings = this.getSharedPreferences("sharedPreferencesFileName", Context.MODE_PRIVATE);

        videoFileName = getFilesDir() + File.separator + String.valueOf(Calendar.getInstance().getTimeInMillis());
        imageFile = new File(videoFileName + "tmp");

        setContentView(R.layout.activity_camera);

        SurfaceView cameraView = (SurfaceView) findViewById(R.id.surface_camera);
        shutterImage = (ImageView) findViewById(R.id.shutterImageView);
        sendImage = (ImageView) findViewById(R.id.sendImageView);
        cameraFlashControl = (ImageView) findViewById(R.id.camera_flash_control);

        cameraControls = (LinearLayout) findViewById(R.id.camera_controls);
        cameraControlsSpacer = (LinearLayout) findViewById(R.id.camera_controls_spacer);
        cameraControlsShutter = (LinearLayout) findViewById(R.id.camera_controls_shutter);
        cameraControlsSender = (LinearLayout) findViewById(R.id.camera_controls_sender);

        holder = cameraView.getHolder();
        holder.addCallback(this);

        // required for pre 3.0 devices
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB)
            holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (Camera.getNumberOfCameras() < 2)
            findViewById(R.id.pictureTakingInstructions).setVisibility(View.GONE);

        cameraView.setClickable(true);

        previewCallback = new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                updateWidgetsHandler.sendEmptyMessage(0);
            }
        };

        sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("fileName", imageFile.getAbsolutePath());
                setResult(RESULT_OK, resultIntent);
                saveFile = true;
                finish();
            }
        });

        shutterImage.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("NewApi")
            @Override
            public void onClick(View v) {
                if (!pictureTaken){
                    takePicture();
                } else {
                    pictureTaken = false;
                    shutterImage.setImageResource(R.drawable.shutter);
                    setCameraDisplayOrientation(CameraActivity.this, getLastCameraIndex(), mCamera);

                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            Camera.Parameters params = mCamera.getParameters();
                            if (params.isAutoExposureLockSupported())
                                params.setAutoExposureLock(false);
                            if (params.isAutoWhiteBalanceLockSupported())
                                params.setAutoWhiteBalanceLock(false);
                            mCamera.setParameters(params);
                        }
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        mCamera.startPreview();
                        updateWidgetsHandler.sendEmptyMessage(0);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        });

        cameraView.setOnTouchListener(new OnSwipeTouchListener(this) {
            @SuppressLint("NewApi")
            public void onClick(){
                focusCamera();
            }

            public void onSwipeTop() {
                Thread t = new Thread() {
                    public void run() {
                        switchCamera();
                    }
                };
                t.start();
            }

            public void onSwipeRight() {
                Thread t = new Thread() {
                    public void run() {
                        switchCamera();
                    }
                };
                t.start();
            }
            public void onSwipeLeft() {
                Thread t = new Thread() {
                    public void run() {
                        switchCamera();
                    }
                };
                t.start();
            }
            public void onSwipeBottom() {
                Thread t = new Thread() {
                    public void run() {
                        switchCamera();
                    }
                };
                t.start();
            }
        });

        cameraFlashControl.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (hasFlash){
                    String flashMode = mCamera.getParameters().getFlashMode();
                    switch (flashMode) {
                        case Camera.Parameters.FLASH_MODE_AUTO:
                            flashMode = Camera.Parameters.FLASH_MODE_ON;
                            break;
                        case Camera.Parameters.FLASH_MODE_ON:
                            flashMode = Camera.Parameters.FLASH_MODE_OFF;
                            break;
                        default:
                            flashMode = Camera.Parameters.FLASH_MODE_AUTO;
                    }

                    SharedPreferences.Editor mPrefs = settings.edit();
                    mPrefs.putString(LAST_CAMERA_FLASH_PREFERENCE, flashMode);
                    mPrefs.apply();

                    if (!pictureTaken){
                        mCamera.stopPreview();
                    }
                    Camera.Parameters parameters = mCamera.getParameters();
                    parameters.setFlashMode(flashMode);
                    try {
                        mCamera.setParameters(parameters);
                        setCameraFlashControlImage(flashMode);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    if (!pictureTaken) {
                        mCamera.setOneShotPreviewCallback(previewCallback);
                        mCamera.startPreview();
                    }
                }
            }
        });

        changeWidgetOrientation(getResources().getConfiguration().orientation);
    }

    public void switchCamera(){
        if (Camera.getNumberOfCameras() > 1){
            int cameraIndex;
            if (settings.getInt(LAST_CAMERA_PREFERENCE, Camera.CameraInfo.CAMERA_FACING_BACK) == Camera.CameraInfo.CAMERA_FACING_BACK){
                cameraIndex = Camera.CameraInfo.CAMERA_FACING_FRONT;
            } else {
                cameraIndex = Camera.CameraInfo.CAMERA_FACING_BACK;
            }
            SharedPreferences.Editor mPrefs = settings.edit();
            mPrefs.putInt(LAST_CAMERA_PREFERENCE, cameraIndex);
            mPrefs.apply();

            try {
                releaseCamera();
                mCamera = getCameraInstance(cameraIndex);
                setCameraDimensions(imageWidth, imageHeight);
                mCamera.setPreviewDisplay(holder);
                mCamera.setOneShotPreviewCallback(previewCallback);
                setCameraDisplayOrientation(this, getLastCameraIndex(), mCamera);
                mCamera.startPreview();
                pictureTaken = false;
                updateWidgetsHandler.sendEmptyMessage(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Handler updateWidgetsHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 0){
                shutterImage.setImageResource(R.drawable.shutter);
                sendImage.setVisibility(View.INVISIBLE);
            } else {
                shutterImage.setImageResource(android.R.drawable.ic_menu_revert);
                sendImage.setVisibility(View.VISIBLE);
            }
            return false;
        }
    });

    public void releaseCamera() {
        try {
            if (mCamera != null) {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.lock();
                mCamera.release();
                mCamera=null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PictureCallback pictureTakenCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                updateWidgetsHandler.sendEmptyMessage(1);
                FileOutputStream fos = new FileOutputStream(imageFile);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    private void takePicture(){
        //Prevent double tap race condition when calling autoFocus
        if (!pictureTaken) {
            pictureTaken = true;
            mCamera.takePicture(null, null, pictureTakenCallback);
        }
    }

    private void focusCamera(){
        if (mCamera.getParameters().getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            try {
                mCamera.autoFocus(autoFocusCallback);
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
        @SuppressLint("NewApi")
        public void onAutoFocus(boolean success, Camera camera) {
            if (success) {
                Log.i("AutoFucus:", "Success");
                try {
                    if (camera != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                            Camera.Parameters params = camera.getParameters();
                            if (params.isAutoExposureLockSupported())
                                params.setAutoExposureLock(true);
                            if (params.isAutoWhiteBalanceLockSupported())
                                params.setAutoWhiteBalanceLock(true);
                            camera.setParameters(params);
                        }
                    }
                }catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                Log.i("AutoFucus:", "Failed");
            }
        }
    };

    private Camera.Size getBestPictureSize(int width, int height, Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result=size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }

        return result;
    }

    private int getLastCameraIndex(){
        return settings.getInt(LAST_CAMERA_PREFERENCE, Camera.CameraInfo.CAMERA_FACING_BACK);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera == null)
                mCamera = getCameraInstance(getLastCameraIndex());
            setCameraDimensions(imageWidth, imageHeight);
            setCameraDisplayOrientation(this, getLastCameraIndex(), mCamera);
            mCamera.setPreviewDisplay(holder);
            mCamera.setOneShotPreviewCallback(previewCallback);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setCameraDimensions(int width, int height){
        Camera.Parameters parameters;
        Camera.Size size;
        try {
            parameters = mCamera.getParameters();
            size = getBestPictureSize (width, height, parameters);
            if (size != null)
                parameters.setPictureSize(size.width, size.height);
            mCamera.setParameters(parameters);
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (!pictureTaken){
            setCameraDimensions(imageWidth, imageHeight);
            setCameraDisplayOrientation(this, getLastCameraIndex(), mCamera);
            mCamera.startPreview();
        }
    }

    private Camera getCameraInstance(int cameraIndex){
        Camera camera = Camera.open(cameraIndex);
        String flashMode;
        if (camera.getParameters().getFlashMode() == null) {
            Log.i("Flash mode", "No flash: Turning off");
            hasFlash = false;
            flashMode = Camera.Parameters.FLASH_MODE_OFF;
        } else {
            Log.i("Flash mode", "Flash reported: Turning on");
            hasFlash = true;
            Camera.Parameters parameters = camera.getParameters();
            flashMode = settings.getString(LAST_CAMERA_FLASH_PREFERENCE, Camera.Parameters.FLASH_MODE_AUTO);
            parameters.setFlashMode(flashMode);
            try {
                camera.setParameters(parameters);
            } catch (Exception e){
                hasFlash = false;
                flashMode = Camera.Parameters.FLASH_MODE_OFF;
                e.printStackTrace();
            }
        }

        Message msg = new Message();
        msg.obj = flashMode;
        setFlashControlImageHandler.sendMessage(msg);

        return camera;
    }

    private Handler setFlashControlImageHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message msg) {
            setCameraFlashControlImage((String) msg.obj);
            return false;
        }
    });

    private void setCameraFlashControlImage(String flashMode){
        switch (flashMode) {
            case Camera.Parameters.FLASH_MODE_AUTO:
                cameraFlashControl.setImageResource(R.drawable.camera_flash_auto);
                break;
            case Camera.Parameters.FLASH_MODE_ON:
                cameraFlashControl.setImageResource(R.drawable.camera_flash_on);
                break;
            default:
                cameraFlashControl.setImageResource(R.drawable.camera_flash_off);
        }
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation)
        {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
        {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        }
        else
        { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        try {
            camera.stopPreview();
            camera.setDisplayOrientation(result);
            camera.setOneShotPreviewCallback(previewCallback);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        finish();
    }

    @Override
    public void onPause(){
        releaseCamera();
        orientationListener.disable();
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mCamera == null)
            try {
                mCamera = getCameraInstance(getLastCameraIndex());
                setCameraDimensions(imageWidth, imageHeight);
                mCamera.setPreviewDisplay(holder);
                mCamera.setOneShotPreviewCallback(previewCallback);
                setCameraDisplayOrientation(CameraActivity.this, getLastCameraIndex(), mCamera);
                mCamera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
                releaseCamera();
                Toast.makeText(CameraActivity.this, getString(R.string.error_cannot_connect_to_camera), Toast.LENGTH_SHORT).show();
                finish();
            }

        if (orientationListener == null){
            orientationListener = new OrientationEventListener(CameraActivity.this, SensorManager.SENSOR_DELAY_NORMAL){
                @Override
                public void onOrientationChanged(int orientation) {
                    if (orientation == ORIENTATION_UNKNOWN) return;
                    android.hardware.Camera.CameraInfo info =
                            new android.hardware.Camera.CameraInfo();
                    android.hardware.Camera.getCameraInfo(getLastCameraIndex(), info);
                    orientation = (orientation + 45) / 90 * 90;
                    int rotation;
                    if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
                        rotation = (info.orientation - orientation + 360) % 360;
                    } else {  // back-facing camera
                        rotation = (info.orientation + orientation) % 360;
                    }
                    if (mCamera != null) {
                        try {
                            Camera.Parameters mParameters = mCamera.getParameters();
                            mParameters.setRotation(rotation);
                            mCamera.setParameters(mParameters);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            if (orientationListener.canDetectOrientation())
                orientationListener.enable();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        changeWidgetOrientation(newConfig.orientation);
    }

    private void changeWidgetOrientation(int orientation){
        if (orientation == Configuration.ORIENTATION_LANDSCAPE){
            Log.i("Mode", "Landscape");

            RelativeLayout.LayoutParams lpcc = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
            lpcc.alignWithParent = true;
            lpcc.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
            lpcc.addRule(RelativeLayout.CENTER_VERTICAL);

            cameraControlsSender.removeView(sendImage);
            cameraControlsSpacer.removeView(cameraFlashControl);
            cameraControlsSender.addView(cameraFlashControl);
            cameraControlsSpacer.addView(sendImage);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0);
            lp.weight=1;
            lp.gravity = Gravity.CENTER;
            cameraControlsSpacer.setLayoutParams(lp);
            cameraControlsSender.setLayoutParams(lp);
            cameraControlsShutter.setLayoutParams(lp);

            cameraControls.setLayoutParams(lpcc);

            cameraControls.setOrientation(LinearLayout.VERTICAL);
            cameraControlsSpacer.setOrientation(LinearLayout.VERTICAL);
            cameraControlsShutter.setOrientation(LinearLayout.VERTICAL);
            cameraControlsSender.setOrientation(LinearLayout.VERTICAL);
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT){
            Log.i("Mode", "Portrait");

            RelativeLayout.LayoutParams lpcc = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            lpcc.alignWithParent = true;
            lpcc.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
            lpcc.addRule(RelativeLayout.CENTER_HORIZONTAL);
            cameraControls.setOrientation(LinearLayout.HORIZONTAL);

            if (cameraControlsSender.findViewById(cameraFlashControl.getId()) != null) {
                cameraControlsSender.removeView(cameraFlashControl);
                cameraControlsSpacer.removeView(sendImage);
                cameraControlsSender.addView(sendImage);
                cameraControlsSpacer.addView(cameraFlashControl);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT);
            lp.weight=1;
            lp.gravity = Gravity.CENTER;
            cameraControlsSender.setLayoutParams(lp);
            cameraControlsSpacer.setLayoutParams(lp);
            cameraControlsShutter.setLayoutParams(lp);

            cameraControls.setLayoutParams(lpcc);

            cameraControls.setOrientation(LinearLayout.HORIZONTAL);
            cameraControlsSpacer.setOrientation(LinearLayout.HORIZONTAL);
            cameraControlsShutter.setOrientation(LinearLayout.HORIZONTAL);
            cameraControlsSender.setOrientation(LinearLayout.HORIZONTAL);
        }
    }

}