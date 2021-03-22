package com.example.android.camera2basic.videomode;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;
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
    private final IPreviewHandler mPreviewHandler;
    private final IVideoSaveHandler mSaveHandler;
    private Handler mBackgroundHandler;
    private final DisplayParams mDisplayParams;
    private final ICameraDeviceHolder mDeviceHolder;
    private VideoCaptureSessionCallback mCaptureCallback;
    private boolean mRecording = false;
    private ICaptureSessionHolder mCaptureSessionHolder;

    public VideoMode(IPreviewHandler previewHandler, IVideoSaveHandler saveHandler, DisplayParams displayParams, ICameraDeviceHolder deviceHolder) {
        mPreviewHandler = previewHandler;
        mSaveHandler = saveHandler;
        mDisplayParams = displayParams;
        mDeviceHolder = deviceHolder;
    }

    @Override
    public void initialize(
            ISessionStateCallback captureSessionStateCallback,
            CameraCaptureSession.CaptureCallback callerCaptureCallback,
            ICameraDeviceHolder cameraDeviceHolder) {
        mPreviewHandler.initialize();
        mCaptureSessionHolder =
                new CaptureSessionHolder(
                        mDeviceHolder, mPreviewHandler, mSaveHandler, mBackgroundHandler);
        ComboCaptureSessionStateCallback comboCaptureSessionStateCallback =
                new ComboCaptureSessionStateCallback(
                        Arrays.asList(new PreviewStartTask(), captureSessionStateCallback));
        mCaptureCallback =
                new VideoCaptureSessionCallback(
                        mCaptureSessionHolder,
                        mDeviceHolder,
                        mDisplayParams.getRotation(),
                        mSaveHandler,
                        callerCaptureCallback);
        mCaptureSessionHolder.createSession(comboCaptureSessionStateCallback);
    }

    @Override
    public void onHandlerAvailable(Handler handler) {
        mBackgroundHandler = handler;
    }

    @Override
    public void onHandlerAvailable(Handler handler1, Handler handler2) {

    }

    @Override
    public void close() {
        if (mCaptureCallback != null) {
            mCaptureCallback.close();
        }
        mCaptureSessionHolder.close();
        mPreviewHandler.close();
        mSaveHandler.close();
    }

    @Override
    public void onTextureAvailable() {
        setUpCameraOutputs(mDisplayParams.getOrientation(), mSaveHandler, mPreviewHandler);
        mPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    @Override
    public void updateTransform() {
        mPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    private void setUpCameraOutputs(int orientation, ISaveHandler saveHandler, IPreviewHandler previewHandler) {
        // For still image captures, we use the largest available size.
        Size largest = mDeviceHolder.getLargestSize(isPhotoMode());
        saveHandler.initialize(mBackgroundHandler, largest);
        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        boolean swappedDimensions = mDeviceHolder.shouldSwapDimensions(mDisplayParams.getRotation());
        previewHandler.calculateBestPreviewSize(largest, swappedDimensions, mDisplayParams.getDisplaySize(), orientation, mDeviceHolder.getPreviewSizes());
    }

    @Override
    public void startRecording() {
        mRecording = true;
        mSaveHandler.startRecording();
    }

    @Override
    public void stopRecording() {
        if (mRecording) {
            mRecording = false;
            mSaveHandler.stopRecording();
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
        @Override
        public void onConfigured() {
            mCaptureCallback.createPreviewRequest(mPreviewHandler.getTarget(), mSaveHandler.getTarget());
        }
    }
}
