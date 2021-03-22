package com.example.android.camera2basic.interfaces;

import android.hardware.camera2.CameraCaptureSession;
import android.os.Handler;

public interface ICameraMode {
    void initialize(
            ISessionStateCallback captureSessionStateCallback,
            CameraCaptureSession.CaptureCallback callerCaptureCallback,
            ICameraDeviceHolder cameraDeviceHolder);

    void close();

    void onHandlerAvailable(Handler handler);

    void onHandlerAvailable(Handler handler1, Handler handler2);

    void onTextureAvailable();

    void updateTransform();

    boolean isPhotoMode();

    String getName();
}
