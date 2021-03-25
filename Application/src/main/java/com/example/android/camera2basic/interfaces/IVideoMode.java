package com.example.android.camera2basic.interfaces;

public interface IVideoMode extends ICameraMode {
    void startRecording();

    void stopRecording();

    boolean isRecording();

    void takePicture();

    void setFilePath(String filePath);
}
