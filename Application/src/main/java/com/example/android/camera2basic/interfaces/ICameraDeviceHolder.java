package com.example.android.camera2basic.interfaces;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.camera2.CaptureSessionHolder;

import java.util.List;

public interface ICameraDeviceHolder {
    void closeCamera();

    void openCamera(Handler backgroundHandler, IStateCallback callerStateCallback);

    boolean isFlashAvailable();

    CaptureRequest.Builder getLockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder);

    CaptureRequest.Builder getTriggerPrecaptureRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder);

    CaptureRequest.Builder createPreviewRequestBuilder(Surface surface) throws CameraAccessException;

    boolean shouldSwapDimensions(int rotation);

    Size[] getPreviewSizes();

    int getOrientation(int mRotation);

    CaptureRequest.Builder createStillCaptureRequest(Surface target, int orientation) throws CameraAccessException;

    CaptureRequest.Builder getUnlockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder);

    Size getLargestSize(boolean isPhoto);

    CaptureRequest.Builder createVideoCaptureRequest(Surface surface, Surface recordSurface) throws CameraAccessException;

    void createCaptureSession(IPreviewHandler mPreviewSurface, ISaveHandler mSaveSurface, CaptureSessionHolder captureSessionHolder, Handler mHandler);
}
