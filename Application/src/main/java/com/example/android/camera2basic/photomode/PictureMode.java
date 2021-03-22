package com.example.android.camera2basic.photomode;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ISessionStateCallback;
import com.example.android.camera2basic.ui.DisplayParams;
import com.example.android.camera2basic.camera2.CaptureSessionHolder;
import com.example.android.camera2basic.util.ComboCaptureSessionStateCallback;
import com.example.android.camera2basic.util.NoOpCaptureSessionStateCallback;
import com.example.android.camera2basic.interfaces.IPhotoMode;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;

import java.util.Arrays;

public class PictureMode implements IPhotoMode {
    private final IPreviewHandler mBackCameraPreviewHandler;
    private final IPreviewHandler mFakeCameraPreviewHandler;
    private final ISaveHandler mSaveHandler;
    private Handler mBackCameraBackgroundHandler;
    private Handler mFakeCameraBackgroundHandler;
    private final ICameraDeviceHolder mBackCamera;
    private final ICameraDeviceHolder mFakeCamera;
    private final DisplayParams mDisplayParams;
    private PhotoCaptureSessionCallback mBackCaptureCallback;
    private PhotoCaptureSessionCallback mFakeCaptureCallback;
    private ICaptureSessionHolder mBackCameraCaptureSessionHolder;
    private ICaptureSessionHolder mFakeCameraCaptureSessionHolder;

    public PictureMode(
            IPreviewHandler backCameraPreviewHandler,
            IPreviewHandler fakeCameraPreviewHandler,
            ISaveHandler saveHandler,
            DisplayParams displayParams,
            ICameraDeviceHolder backCamera,
            ICameraDeviceHolder fakeCamera) {
        mBackCameraPreviewHandler = backCameraPreviewHandler;
        mFakeCameraPreviewHandler = fakeCameraPreviewHandler;
        mSaveHandler = saveHandler;
        mDisplayParams = displayParams;
        mBackCamera = backCamera;
        mFakeCamera = fakeCamera;
    }

    @Override
    public void initialize(
            ISessionStateCallback captureSessionStateCallback,
            CameraCaptureSession.CaptureCallback callerCaptureCallback,
            ICameraDeviceHolder cameraDeviceHolder) {
        if (cameraDeviceHolder.equals(mBackCamera)) {
            Log.d("Sundeep ", "initialize picture mode BACK");
            mBackCameraPreviewHandler.initialize();
            mBackCameraCaptureSessionHolder =
                new CaptureSessionHolder(
                    mBackCamera,
                    mBackCameraPreviewHandler,
                    mSaveHandler,
                    mBackCameraBackgroundHandler);
            ComboCaptureSessionStateCallback comboBackCaptureSessionStateCallback =
                new ComboCaptureSessionStateCallback(
                    Arrays.asList(
                        new PreviewStartTask(mBackCameraPreviewHandler),
                        captureSessionStateCallback));
            mBackCameraCaptureSessionHolder.createSession(comboBackCaptureSessionStateCallback);
            mBackCaptureCallback =
                new PhotoCaptureSessionCallback(
                    mBackCameraCaptureSessionHolder,
                    mBackCamera,
                    mDisplayParams.getRotation(),
                    mSaveHandler,
                    callerCaptureCallback);
        } else if (cameraDeviceHolder.equals(mFakeCamera)) {
            mFakeCameraPreviewHandler.initialize();
            mFakeCameraCaptureSessionHolder =
                new CaptureSessionHolder(
                    mFakeCamera,
                    mFakeCameraPreviewHandler,
                    mSaveHandler,
                    mFakeCameraBackgroundHandler);
            ComboCaptureSessionStateCallback comboBackCaptureSessionStateCallback =
                new ComboCaptureSessionStateCallback(
                    Arrays.asList(
                        new PreviewStartTask(mFakeCameraPreviewHandler),
                        captureSessionStateCallback));
            mFakeCameraCaptureSessionHolder.createSession(comboBackCaptureSessionStateCallback);
            mFakeCaptureCallback =
                new PhotoCaptureSessionCallback(
                    mFakeCameraCaptureSessionHolder,
                    mFakeCamera,
                    mDisplayParams.getRotation(),
                    mSaveHandler,
                    callerCaptureCallback);
        }
    }

    @Override
    public void close() {
        try {
            mBackCameraCaptureSessionHolder.close();
            mBackCameraPreviewHandler.close();
            mSaveHandler.close();
            mBackCaptureCallback = null;

            mFakeCameraCaptureSessionHolder.close();
            mFakeCameraPreviewHandler.close();
            mSaveHandler.close();
            mFakeCaptureCallback = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onHandlerAvailable(Handler handler) {
        //mBackCameraBackgroundHandler = handler;
    }

    @Override
    public void onHandlerAvailable(Handler handler1, Handler handler2) {
        mBackCameraBackgroundHandler = handler1;
        mFakeCameraBackgroundHandler = handler2;
    }

    @Override
    public void onTextureAvailable() {
        setUpCameraOutputs(mDisplayParams.getOrientation(), mSaveHandler, mBackCameraPreviewHandler, mBackCamera);
        mBackCameraPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    @Override
    public void updateTransform() {
        mBackCameraPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    private void setUpCameraOutputs(
            int orientation,
            ISaveHandler saveHandler,
            IPreviewHandler previewHandler,
            ICameraDeviceHolder cameraDeviceHolder) {
        ICameraDeviceHolder deviceHolder = cameraDeviceHolder;
        // For still image captures, we use the largest available size.
        Size largest = deviceHolder.getLargestSize(isPhotoMode());
        saveHandler.initialize(mBackCameraBackgroundHandler, largest);
        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        boolean swappedDimensions = deviceHolder.shouldSwapDimensions(mDisplayParams.getRotation());
        previewHandler.calculateBestPreviewSize(
                largest,
                swappedDimensions,
                mDisplayParams.getDisplaySize(),
                orientation,
                deviceHolder.getPreviewSizes());
    }

    @Override
    public void takePicture() {
        mBackCaptureCallback.takePicture();
    }

    @Override
    public boolean isPhotoMode() {
        return true;
    }

    @Override
    public String getName() {
        return "Photo";
    }

    class PreviewStartTask extends NoOpCaptureSessionStateCallback {
        private IPreviewHandler mCameraPreviewHandler;

        PreviewStartTask(IPreviewHandler previewHandler) {
            mCameraPreviewHandler = previewHandler;
        }

        @Override
        public void onConfigured() {
            if (mCameraPreviewHandler.equals(mBackCameraPreviewHandler)) {
                mBackCaptureCallback.createPreviewRequest(mCameraPreviewHandler.getTarget());
            }else{
                mFakeCaptureCallback.createPreviewRequest(mCameraPreviewHandler.getTarget());
            }
        }
    }
}