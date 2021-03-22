package com.example.android.camera2basic.savehandlers;

import android.media.MediaRecorder;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.interfaces.IVideoSaveHandler;

import java.io.File;
import java.io.IOException;

public class MediaRecorderSaveHandler implements IVideoSaveHandler {
    private final File mFile;
    MediaRecorder mMediaRecorder;
    private String mNextVideoAbsolutePath;

    public MediaRecorderSaveHandler(File file) {
        mMediaRecorder = new MediaRecorder();
        mFile = file;
    }

    @Override
    public Surface getTarget() {
        return mMediaRecorder.getSurface();
    }

    @Override
    public void initialize(Handler handler, Size largest) {
        try {
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            if (mNextVideoAbsolutePath == null || mNextVideoAbsolutePath.isEmpty()) {
                mNextVideoAbsolutePath = getVideoFilePath(mFile);
            }
            mMediaRecorder.setOutputFile(mNextVideoAbsolutePath);
            mMediaRecorder.setVideoEncodingBitRate(10000000);
            mMediaRecorder.setVideoFrameRate(30);
            mMediaRecorder.setVideoSize(largest.getWidth(), largest.getHeight());
            mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.prepare();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getVideoFilePath(File mFile) {
        return mFile.getAbsolutePath();
    }

    @Override
    public void close() {
        mMediaRecorder.reset();
    }

    @Override
    public void startRecording() {
        mMediaRecorder.start();
    }

    @Override
    public void stopRecording() {
        mMediaRecorder.stop();
    }
}
