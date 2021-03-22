package com.example.android.camera2basic.util;

import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.camera2.util.NoOpCaptureCallback;

import java.util.List;

public class ComboCaptureSessionCaptureCallback extends NoOpCaptureCallback {
    private final List<CameraCaptureSession.CaptureCallback> mCaptureCallbacks;

    public ComboCaptureSessionCaptureCallback(List<CameraCaptureSession.CaptureCallback> captureCallbacks) {
        mCaptureCallbacks = captureCallbacks;
    }

    @Override
    public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureStarted(session, request, timestamp, frameNumber);
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureResult partialResult) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureProgressed(session, request, partialResult);
        }
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureCompleted(session, request, result);
        }
    }

    @Override
    public void onCaptureFailed(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull CaptureFailure failure) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureFailed(session, request, failure);
        }
    }

    @Override
    public void onCaptureSequenceCompleted(@NonNull CameraCaptureSession session, int sequenceId, long frameNumber) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureSequenceCompleted(session, sequenceId, frameNumber);
        }
    }

    @Override
    public void onCaptureSequenceAborted(@NonNull CameraCaptureSession session, int sequenceId) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureSequenceAborted(session, sequenceId);
        }
    }

    @Override
    public void onCaptureBufferLost(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull Surface target, long frameNumber) {
        for (CameraCaptureSession.CaptureCallback callback : mCaptureCallbacks) {
            callback.onCaptureBufferLost(session, request, target, frameNumber);
        }
    }
}
