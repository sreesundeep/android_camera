package com.example.android.camera2basic.util;

import android.hardware.camera2.CameraDevice;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.IStateCallback;

public class NoOpStateCallback implements IStateCallback {
    @Override
    public void onOpened() {
    }

    @Override
    public void onClosed() {
    }

    @Override
    public void onDisconnected() {
    }

    @Override
    public void onError(int error) {
    }
}
