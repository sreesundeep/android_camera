package com.example.android.camera2basic.fake;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.camera2.CaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.IStateCallback;

public class FakeCameraDevice implements ICameraDeviceHolder {
    private IStateCallback mStateCallback;
    private Context mContext;

    public FakeCameraDevice(Context context) {
        mContext = context;
    }

    @Override
    public void closeCamera() {
    }

    @Override
    public void openCamera(Handler backgroundHandler, IStateCallback callerStateCallback) {
        mStateCallback = callerStateCallback;
        backgroundHandler.post(new Runnable() {
            @Override
            public void run() {
                mStateCallback.onOpened();
            }
        });
    }

    @Override
    public boolean isFlashAvailable() {
        return false;
    }

    @Override
    public CaptureRequest.Builder getLockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        return null;
    }

    @Override
    public CaptureRequest.Builder getTriggerPrecaptureRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        return null;
    }

    @Override
    public CaptureRequest.Builder createPreviewRequestBuilder(Surface surface) throws CameraAccessException {
        return null;
    }

    @Override
    public boolean shouldSwapDimensions(int rotation) {
        return false;
    }

    @Override
    public Size[] getPreviewSizes() {
        return new Size[] {new Size(1920, 1080)};
    }

    @Override
    public int getOrientation(int mRotation) {
        return 0;
    }

    @Override
    public CaptureRequest.Builder createStillCaptureRequest(Surface target, int orientation) throws CameraAccessException {
        return null;
    }

    @Override
    public CaptureRequest.Builder getUnlockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        return null;
    }

    @Override
    public Size getLargestSize(boolean isPhoto) {
        return new Size(1920, 1080);
    }

    @Override
    public CaptureRequest.Builder createVideoCaptureRequest(Surface surface, Surface recordSurface) throws CameraAccessException {
        return null;
    }

    @Override
    public void createCaptureSession(IPreviewHandler mPreviewSurface, ISaveHandler mSaveSurface, CaptureSessionHolder captureSessionHolder, Handler mHandler) {
    }
}
