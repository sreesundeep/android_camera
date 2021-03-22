package com.example.android.camera2basic.videomode;

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
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.IVideoMode;
import com.example.android.camera2basic.interfaces.IVideoSaveHandler;

import java.util.Arrays;

public class VideoMode implements IVideoMode {
    private final IPreviewHandler mBackPreviewHandler;
    private final IPreviewHandler mFrontPreviewHandler;
    private final IVideoSaveHandler mBackSaveHandler;
    private final IVideoSaveHandler mFrontSaveHandler;
    private Handler mBackgroundHandler;
    private Handler mBackgroundHandler2;
    private final DisplayParams mDisplayParams;
    private final ICameraDeviceHolder mBackCamera;
    private final ICameraDeviceHolder mFrontCamera;
    private VideoCaptureSessionCallback mBackCaptureCallback;
    private VideoCaptureSessionCallback mFrontCaptureCallback;
    private boolean mRecording = false;
    private ICaptureSessionHolder mBackCaptureSessionHolder;
    private ICaptureSessionHolder mFrontCaptureSessionHolder;

    public VideoMode(IPreviewHandler backPreviewHandler, IPreviewHandler frontPreviewHandler, IVideoSaveHandler backSaveHandler, IVideoSaveHandler frontSaveHandler, DisplayParams displayParams, ICameraDeviceHolder backCamera, ICameraDeviceHolder frontCamera) {
        mBackPreviewHandler = backPreviewHandler;
        mFrontPreviewHandler = frontPreviewHandler;
        mBackSaveHandler = backSaveHandler;
        mFrontSaveHandler = frontSaveHandler;
        mDisplayParams = displayParams;
        mBackCamera = backCamera;
        mFrontCamera = frontCamera;
    }

    @Override
    public void initialize(
        ISessionStateCallback captureSessionStateCallback,
        CameraCaptureSession.CaptureCallback callerCaptureCallback,
        ICameraDeviceHolder cameraDeviceHolder) {
        Log.d("Sundeep", "initialize isFront " + cameraDeviceHolder.isFront());
        if (cameraDeviceHolder.isFront()) {
            mFrontPreviewHandler.initialize();
            mFrontCaptureSessionHolder =
                new CaptureSessionHolder(
                    mFrontCamera, mFrontPreviewHandler, mFrontSaveHandler, mBackgroundHandler2);
            ComboCaptureSessionStateCallback comboCaptureSessionStateCallback =
                new ComboCaptureSessionStateCallback(
                    Arrays.asList(new PreviewStartTask(cameraDeviceHolder.isFront()), captureSessionStateCallback));
            mFrontCaptureCallback =
                new VideoCaptureSessionCallback(
                    mFrontCaptureSessionHolder,
                    mFrontCamera,
                    mDisplayParams.getRotation(),
                    mFrontSaveHandler,
                    callerCaptureCallback);
            mFrontCaptureSessionHolder.createSession(comboCaptureSessionStateCallback);
        } else {
            mBackPreviewHandler.initialize();
            mBackCaptureSessionHolder =
                new CaptureSessionHolder(
                    mBackCamera, mBackPreviewHandler, mBackSaveHandler, mBackgroundHandler);
            ComboCaptureSessionStateCallback comboCaptureSessionStateCallback =
                new ComboCaptureSessionStateCallback(
                    Arrays.asList(new PreviewStartTask(cameraDeviceHolder.isFront()), captureSessionStateCallback));
            mBackCaptureCallback =
                new VideoCaptureSessionCallback(
                    mBackCaptureSessionHolder,
                    mBackCamera,
                    mDisplayParams.getRotation(),
                    mBackSaveHandler,
                    callerCaptureCallback);
            mBackCaptureSessionHolder.createSession(comboCaptureSessionStateCallback);
        }
    }

    @Override
    public void onHandlerAvailable(Handler handler1, Handler handler2) {
        mBackgroundHandler = handler1;
        mBackgroundHandler2 = handler2;
    }

    @Override
    public void close() {
        if (mBackCaptureCallback != null) {
            mBackCaptureCallback.close();
        }
        mBackCaptureSessionHolder.close();
        mBackPreviewHandler.close();
        mBackSaveHandler.close();

        if (mFrontCaptureCallback != null) {
            mFrontCaptureCallback.close();
        }
        mFrontCaptureSessionHolder.close();
        mFrontPreviewHandler.close();
        mFrontSaveHandler.close();
    }

    @Override
    public void onTextureAvailable(boolean isFront) {
        // Front
        setUpCameraOutputs(mDisplayParams.getOrientation(), mFrontSaveHandler, mFrontPreviewHandler);
        mFrontPreviewHandler.configureTransform(mDisplayParams.getRotation());

        // Back
        setUpCameraOutputs(mDisplayParams.getOrientation(), mBackSaveHandler, mBackPreviewHandler);
        mBackPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    @Override
    public void updateTransform() {
        mBackPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    private void setUpCameraOutputs(int orientation, ISaveHandler saveHandler, IPreviewHandler previewHandler) {
        // For still image captures, we use the largest available size.
        Size largest = mBackCamera.getLargestSize(isPhotoMode());
        saveHandler.initialize(mBackgroundHandler, largest);
        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        boolean swappedDimensions = mBackCamera.shouldSwapDimensions(mDisplayParams.getRotation());
        previewHandler.calculateBestPreviewSize(largest, swappedDimensions, mDisplayParams.getDisplaySize(), orientation, mBackCamera.getPreviewSizes());
    }

    @Override
    public void startRecording() {
        mRecording = true;
        mBackSaveHandler.startRecording();
    }

    @Override
    public void stopRecording() {
        if (mRecording) {
            mRecording = false;
            mBackSaveHandler.stopRecording();
        }
    }

    @Override
    public boolean isPhotoMode() {
        return false;
    }

    @Override
    public String getName() {
        return "Video";
    }

    @Override
    public boolean isRecording() {
        return mRecording;
    }

    private class PreviewStartTask extends NoOpCaptureSessionStateCallback {
        private boolean mIsFront;

        PreviewStartTask(boolean isFront) {
            mIsFront = isFront;
        }

        @Override
        public void onConfigured() {
            Log.d("Sundeep", "CaptureSession Configured isFront " + mIsFront);
            if (mIsFront) {
                mFrontCaptureCallback.createPreviewRequest(mFrontPreviewHandler.getTarget(), mFrontSaveHandler.getTarget());
            } else {
                mBackCaptureCallback.createPreviewRequest(mBackPreviewHandler.getTarget(), mBackSaveHandler.getTarget());

            }
        }
    }
}
