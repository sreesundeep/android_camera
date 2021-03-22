package com.example.android.camera2basic.interfaces;

public interface IStateCallback {
    void onOpened();
    void onClosed();
    void onDisconnected();
    void onError(int error);
}
