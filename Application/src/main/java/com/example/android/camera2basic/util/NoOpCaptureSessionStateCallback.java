package com.example.android.camera2basic.util;

import android.hardware.camera2.CameraCaptureSession;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.interfaces.ISessionStateCallback;

public class NoOpCaptureSessionStateCallback implements ISessionStateCallback {
    @Override
    public void onConfigured() {
    }

    @Override
    public void onConfigureFailed() {
    }
}
