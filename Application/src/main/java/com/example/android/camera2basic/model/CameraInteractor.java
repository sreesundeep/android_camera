package com.example.android.camera2basic.model;

import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.android.camera2basic.camera2.util.NoOpCaptureCallback;
import com.example.android.camera2basic.savehandlers.MediaRecorderSaveHandler;
import com.example.android.camera2basic.util.NoOpCaptureSessionStateCallback;
import com.example.android.camera2basic.util.NoOpStateCallback;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICameraMode;
import com.example.android.camera2basic.interfaces.IPhotoMode;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.IVideoMode;
import com.example.android.camera2basic.opengl.CustomGLSurfaceView;
import com.example.android.camera2basic.photomode.PictureMode;
import com.example.android.camera2basic.preview.PreviewHandler;
import com.example.android.camera2basic.savehandlers.ImageSaveHandler;
import com.example.android.camera2basic.ui.AutoFitTextureView;
import com.example.android.camera2basic.ui.DisplayParams;
import com.example.android.camera2basic.videomode.VideoMode;
import com.example.android.camera2basic.viewmodels.ControlPanel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CameraInteractor implements DefaultLifecycleObserver {
    private final ControlPanel controlPanel = new ControlPanel(this);
    private final DisplayParams mDisplayParams;
    private final Context mContext;
    private List<ICameraMode> cameraModes = new ArrayList<>();
    private ICameraMode currentMode;
    private ICameraDeviceHolder mBackCamera;
    private ICameraDeviceHolder mFrontCamera;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;
    private HandlerThread mBackgroundThread2;
    private Handler mBackgroundHandler2;
    private File mFile1;
    private File mFile2;
    private IPreviewHandler backCameraPreviewHandler;
    private IPreviewHandler frontCameraPreviewHandler;

    public CameraInteractor(
            Context context,
            AutoFitTextureView backTextureView,
            AutoFitTextureView frontTextureView,
            DisplayParams displayParams,
            CustomGLSurfaceView customGLSurfaceView,
            ICameraDeviceHolder backCamera,
            ICameraDeviceHolder frontCamera) {
        mContext = context;
        backCameraPreviewHandler = new PreviewHandler(backTextureView);
        frontCameraPreviewHandler = new PreviewHandler(frontTextureView);
        // previewHandler = new OGLPreviewHandler(customGLSurfaceView);
        mDisplayParams = displayParams;
        mBackCamera = backCamera;
        mFrontCamera = frontCamera;
        String fileName1 = "FFC_Video_"+new SimpleDateFormat("MMddHHmmss").format(new Date())+".mp4";
        mFile1 = new File(mContext.getExternalFilesDir(null), fileName1);
        String fileName2 = "RFC_Video_"+new SimpleDateFormat("MMddHHmmss").format(new Date())+".mp4";
        mFile2 = new File(mContext.getExternalFilesDir(null), fileName2);
        //cameraModes.add(new PictureMode(backCameraPreviewHandler, frontCameraPreviewHandler, new ImageSaveHandler(mFile), mDisplayParams, backCamera, frontCamera));
        cameraModes.add(new VideoMode(mContext, backCameraPreviewHandler, frontCameraPreviewHandler, new MediaRecorderSaveHandler(mFile1), new MediaRecorderSaveHandler(mFile2), mDisplayParams, mBackCamera, mFrontCamera));
        currentMode = cameraModes.get(controlPanel.getDefaultMode());
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        startBackgroundThread();

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
        if (backCameraPreviewHandler.isAvailable()) {
            mBackCamera.openCamera(mBackgroundHandler, new OpenDeviceCallback(mBackCamera));
        } else {
            backCameraPreviewHandler.setSurfaceAvailableCallback(new IPreviewHandler.ISurfaceAvailableCallback() {
                @Override
                public void onSurfaceAvailable(int width, int height) {
                    currentMode.onTextureAvailable(false);
                    mBackCamera.openCamera(mBackgroundHandler, new OpenDeviceCallback(mBackCamera));
                }

                @Override
                public void onSurfaceResized(int width, int height) {
                    currentMode.updateTransform();
                }
            });
        }

        if (currentMode.isPhotoMode()) {
            controlPanel.setButtonLabel("Picture");
        } else {
            controlPanel.setButtonLabel("Record");
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        mBackCamera.closeCamera();
        currentMode.close();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        mBackgroundThread = new HandlerThread("CameraBackground1");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());

        mBackgroundThread2 = new HandlerThread("CameraBackground2");
        mBackgroundThread2.start();
        mBackgroundHandler2 = new Handler(mBackgroundThread2.getLooper());
        for (ICameraMode mode : cameraModes) {
            mode.onHandlerAvailable(mBackgroundHandler, mBackgroundHandler2);
        }
    }

    private void stopBackgroundThread() {
        mBackgroundThread.quitSafely();
        mBackgroundThread2.quitSafely();
        try {
            mBackgroundThread.join();
            mBackgroundThread = null;
            mBackgroundHandler = null;
            mBackgroundThread2.join();
            mBackgroundThread2 = null;
            mBackgroundHandler2 = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void capture() {
        if (currentMode.isPhotoMode()) {
            ((IPhotoMode) currentMode).takePicture();
        } else {
            IVideoMode currentMode = (IVideoMode) this.currentMode;
            if (currentMode.isRecording()) {
                currentMode.stopRecording();
                controlPanel.setButtonLabel("Record");
            } else {
                currentMode.startRecording();
                controlPanel.setButtonLabel("Stop");
            }
        }
    }

    public ControlPanel getControlPanel() {
        return controlPanel;
    }

    public void setMode(int position) {
        /*currentMode.close();
        currentMode = cameraModes.get(position);
        if (currentMode.isPhotoMode()) {
            controlPanel.setButtonLabel("Capture");
        } else {
            controlPanel.setButtonLabel("Record");
        }
        currentMode.onTextureAvailable();

        currentMode.initialize(
                new CaptureSessionStateCallback(),
                new CaptureCallback(),
                mBackCamera);*/
    }

    public List<String> getCameraModes() {
        return cameraModes.stream().map(ICameraMode::getName).collect(Collectors.toList());
    }

    class CaptureSessionStateCallback extends NoOpCaptureSessionStateCallback {
        @Override
        public void onConfigureFailed() {
            Log.d("Sundeep", "Failed to created capture session");
            controlPanel.setErrorMessage("Failed");
        }
    }

    private class OpenDeviceCallback extends NoOpStateCallback {
        private ICameraDeviceHolder mCameraDevice;

        OpenDeviceCallback(ICameraDeviceHolder cameraDeviceHolder){
            mCameraDevice = cameraDeviceHolder;
        }

        @Override
        public void onOpened() {
            Log.d("Sundeep ", "Camera Opened isFront " + mCameraDevice.isFront());
            currentMode.initialize(new CaptureSessionStateCallback(), new CaptureCallback(), mCameraDevice);

            if (mCameraDevice.equals(mBackCamera)) {
                if (frontCameraPreviewHandler.isAvailable()) {
                    mFrontCamera.openCamera(mBackgroundHandler2, new OpenDeviceCallback(mFrontCamera));
                } else {
                    frontCameraPreviewHandler.setSurfaceAvailableCallback(new IPreviewHandler.ISurfaceAvailableCallback() {
                        @Override
                        public void onSurfaceAvailable(int width, int height) {
                            currentMode.onTextureAvailable(true);
                            mFrontCamera.openCamera(mBackgroundHandler2, new OpenDeviceCallback(mFrontCamera));
                        }

                        @Override
                        public void onSurfaceResized(int width, int height) {
                            currentMode.updateTransform();
                        }
                    });
                }
            }
        }

        @Override
        public void onError(int error) {
            controlPanel.setCameraNotAvailable(true);
        }
    }

    private class CaptureCallback extends NoOpCaptureCallback {
        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            controlPanel.setFilePath(mFile1.toString());
        }
    }
}
