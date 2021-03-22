package com.example.android.camera2basic.interfaces;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;

public interface ICaptureSessionHolder {
    void createSession(ISessionStateCallback callerStateCallback);

    void close();

    void setRepeatingRequest(CaptureRequest mPreviewRequest, CameraCaptureSession.CaptureCallback mCaptureCallback) throws CameraAccessException;

    void stopRepeating(boolean abortExisting) throws CameraAccessException;

    void capture(CaptureRequest build, CameraCaptureSession.CaptureCallback captureCallback, boolean useBackgroundThread) throws CameraAccessException;

    void capture(CaptureRequest build, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException;
}
