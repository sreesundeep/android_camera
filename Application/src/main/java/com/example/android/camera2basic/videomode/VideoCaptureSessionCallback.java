package com.example.android.camera2basic.videomode;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ISaveHandler;

class VideoCaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
    private final ICameraDeviceHolder mCameraDeviceHolder;
    private final ICaptureSessionHolder mCaptureSessionHolder;
    private final int mRotation;
    private final ISaveHandler mSaveHandler;
    private final CameraCaptureSession.CaptureCallback mCallerCaptureCallback;
    /**
     * {@link CaptureRequest.Builder} for the camera preview
     */
    private CaptureRequest.Builder mPreviewRequestBuilder;
    /**
     * {@link CaptureRequest} generated by {@link #mPreviewRequestBuilder}
     */
    private CaptureRequest mPreviewRequest;

    public VideoCaptureSessionCallback(ICaptureSessionHolder captureSessionHolder, ICameraDeviceHolder deviceHolder, int rotation, ISaveHandler saveHandler, CameraCaptureSession.CaptureCallback callerCaptureCallback) {
        mCaptureSessionHolder = captureSessionHolder;
        mCameraDeviceHolder = deviceHolder;
        mRotation = rotation;
        mSaveHandler = saveHandler;
        mCallerCaptureCallback = callerCaptureCallback;
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult partialResult) {
        mCallerCaptureCallback.onCaptureProgressed(session, request, partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        mCallerCaptureCallback.onCaptureCompleted(session, request, result);
    }

    public void createPreviewRequest(Surface surface, Surface recordSurface) {
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDeviceHolder.createVideoCaptureRequest(surface, recordSurface);
            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSessionHolder.setRepeatingRequest(mPreviewRequest, this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mCaptureSessionHolder.close();
    }
}
