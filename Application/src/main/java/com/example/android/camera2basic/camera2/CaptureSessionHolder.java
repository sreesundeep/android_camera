package com.example.android.camera2basic.camera2;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.os.Handler;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.ISessionStateCallback;

public class CaptureSessionHolder extends CameraCaptureSession.StateCallback implements ICaptureSessionHolder {
    protected final IPreviewHandler mPreviewSurface;
    private final ICameraDeviceHolder mCameraDevice;
    private final ISaveHandler mSaveSurface;
    private final Handler mHandler;
    protected CameraCaptureSession mCameraCaptureSession;
    private ISessionStateCallback mCallerStateCallback;

    public CaptureSessionHolder(ICameraDeviceHolder cameraDevice, IPreviewHandler previewSurface, ISaveHandler saveSurface, Handler handler) {
        this.mCameraDevice = cameraDevice;
        this.mPreviewSurface = previewSurface;
        this.mSaveSurface = saveSurface;
        this.mHandler = handler;
    }

    @Override
    public void createSession(ISessionStateCallback callerStateCallback) {
        mCallerStateCallback = callerStateCallback;
        mCameraDevice.createCaptureSession(mPreviewSurface, mSaveSurface, this, mHandler);
    }

    @Override
    public void onConfigured(@NonNull CameraCaptureSession session) {
        mCameraCaptureSession = session;
        mCallerStateCallback.onConfigured();
    }

    @Override
    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
        mCallerStateCallback.onConfigureFailed();
    }

    @Override
    public void close() {
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
    }

    @Override
    public void setRepeatingRequest(CaptureRequest mPreviewRequest, CameraCaptureSession.CaptureCallback mCaptureCallback) throws CameraAccessException {
        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, mCaptureCallback, mHandler);
    }

    @Override
    public void stopRepeating(boolean abortExisting) throws CameraAccessException {
        mCameraCaptureSession.stopRepeating();
        if (abortExisting) {
            mCameraCaptureSession.abortCaptures();
        }
    }

    @Override
    public void capture(CaptureRequest build, CameraCaptureSession.CaptureCallback captureCallback, boolean useBackgroundThread) throws CameraAccessException {
        mCameraCaptureSession.capture(build, captureCallback, useBackgroundThread ? mHandler : null);
    }

    @Override
    public void capture(CaptureRequest build, CameraCaptureSession.CaptureCallback captureCallback) throws CameraAccessException {
        capture(build, captureCallback, true);
    }
}
