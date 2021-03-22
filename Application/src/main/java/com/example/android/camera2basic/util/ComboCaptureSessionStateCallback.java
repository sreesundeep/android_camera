package com.example.android.camera2basic.util;

import com.example.android.camera2basic.interfaces.ISessionStateCallback;

import java.util.List;

public class ComboCaptureSessionStateCallback implements ISessionStateCallback {
    private final List<ISessionStateCallback> mCaptureSessionStateCallbacks;

    public ComboCaptureSessionStateCallback(List<ISessionStateCallback> captureSessionStateCallbacks) {
        mCaptureSessionStateCallbacks = captureSessionStateCallbacks;
    }

    @Override
    public void onConfigured() {
        for (ISessionStateCallback callback : mCaptureSessionStateCallbacks) {
            callback.onConfigured();
        }
    }

    @Override
    public void onConfigureFailed() {
        for (ISessionStateCallback callback : mCaptureSessionStateCallbacks) {
            callback.onConfigureFailed();
        }
    }
}
