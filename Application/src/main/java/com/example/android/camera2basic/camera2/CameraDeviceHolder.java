package com.example.android.camera2basic.camera2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.camera2.util.CompareSizesByArea;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.IStateCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class CameraDeviceHolder extends CameraDevice.StateCallback implements ICameraDeviceHolder {
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private final String mCameraId;
    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private final Semaphore mCameraOpenCloseLock = new Semaphore(1);
    private CameraDevice mCameraDevice;
    private CameraCharacteristics mCharacteristics;
    private Context mContext;
    private IStateCallback mCallerStateCallback;
    private boolean mIsFront;

    public CameraDeviceHolder(Context context, String cameraId, boolean isFront) throws CameraAccessException {
        mContext = context;
        mCameraId = cameraId;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mCharacteristics = manager.getCameraCharacteristics(mCameraId);
        mIsFront = isFront;
    }

    public boolean shouldSwapDimensions(int displayRotation) {
        boolean swappedDimensions = false;
        double sensorOrientation = getSensorOrientation();
        switch (displayRotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                if (sensorOrientation == 90 || sensorOrientation == 270) {
                    swappedDimensions = true;
                }
                break;
            case Surface.ROTATION_90:
            case Surface.ROTATION_270:
                if (sensorOrientation == 0 || sensorOrientation == 180) {
                    swappedDimensions = true;
                }
                break;
            default:
                throw new RuntimeException("Display rotation is invalid: " + displayRotation);
        }
        return swappedDimensions;
    }

    @Override
    public void onOpened(@NonNull CameraDevice cameraDevice) {
        // This method is called when the camera is opened.  We start camera preview here.
        mCameraOpenCloseLock.release();
        mCameraDevice = cameraDevice;
        mCallerStateCallback.onOpened();
    }

    @Override
    public void onDisconnected(@NonNull CameraDevice cameraDevice) {
        mCameraOpenCloseLock.release();
        mCallerStateCallback.onDisconnected();
        cameraDevice.close();
        mCameraDevice = null;
    }

    @Override
    public void onError(@NonNull CameraDevice cameraDevice, int error) {
        mCameraOpenCloseLock.release();
        mCallerStateCallback.onError(error);
        cameraDevice.close();
        mCameraDevice = null;
    }

    public void acquireLock() {
        try {
            mCameraOpenCloseLock.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
        }
    }

    public void releaseLock() {
        mCameraOpenCloseLock.release();
    }

    @Override
    public void closeCamera() {
        try {
            acquireLock();
            if (mCameraDevice != null) {
                mCameraDevice.close();
            }
        } finally {
            releaseLock();
        }
    }

    @Override
    @SuppressLint("MissingPermission")
    public void openCamera(Handler backgroundHandler, IStateCallback callerStateCallback) {
        mCallerStateCallback = callerStateCallback;
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        try {
            boolean result;
            try {
                mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS);
                manager.openCamera(mCameraId, this, backgroundHandler);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while trying to lock camera opening.", e);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public CaptureRequest.Builder createPreviewRequestBuilder(Surface surface) throws CameraAccessException {
        CaptureRequest.Builder previewRequestBuilder
                = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);
        // Auto focus should be continuous for camera preview.
        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        if (isFlashAvailable()) {
            previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
        return previewRequestBuilder;
    }

    public CaptureRequest.Builder createStillCaptureRequest(Surface surface, int rotation) throws CameraAccessException {
        CaptureRequest.Builder stillCaptureBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
        stillCaptureBuilder.addTarget(surface);
        stillCaptureBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
        if (isFlashAvailable()) {
            stillCaptureBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                    CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
        }
        stillCaptureBuilder.set(CaptureRequest.JPEG_ORIENTATION, rotation);
        return stillCaptureBuilder;
    }

    public CaptureRequest.Builder getLockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        // This is how to tell the camera to lock focus.
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_START);
        return mPreviewRequestBuilder;
    }

    public CaptureRequest.Builder getTriggerPrecaptureRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        // This is how to tell the camera to trigger.
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
                CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
        return mPreviewRequestBuilder;
    }

    public CaptureRequest.Builder getUnlockFocusRequestBuilder(CaptureRequest.Builder mPreviewRequestBuilder) {
        mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
                CameraMetadata.CONTROL_AF_TRIGGER_CANCEL);
        return mPreviewRequestBuilder;
    }

    @Override
    public Size getLargestSize(boolean isPhoto) {
        return isPhoto ? getLargestPictureSize() : getLargestVideoSize();
    }

    public boolean isFlashAvailable() {
        // Check if the flash is supported.
        Boolean available = mCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
        return available == null ? false : available;
    }

    @Override
    public boolean isFront() {
        return mIsFront;
    }

    public int getSensorOrientation() {
        return mCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    public int getOrientation(int rotation) {
        // Sensor orientation is 90 for most devices, or 270 for some devices (eg. Nexus 5X)
        // We have to take that into account and rotate JPEG properly.
        // For devices with orientation of 90, we simply return our mapping from ORIENTATIONS.
        // For devices with orientation of 270, we need to rotate the JPEG 180 degrees.
        return (ORIENTATIONS.get(rotation) + getSensorOrientation() + 270) % 360;
    }

    public StreamConfigurationMap getStreamConfigurationMap() {
        return mCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    }

    public Size getLargestPictureSize() {
        return getLargestSizeForFormat();
    }

    private Size getLargestSizeForFormat() {
        StreamConfigurationMap map = getStreamConfigurationMap();
        return Collections.max(
                Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                new CompareSizesByArea());
    }

    public Size[] getPreviewSizes() {
        StreamConfigurationMap map = getStreamConfigurationMap();
        return map.getOutputSizes(SurfaceTexture.class);
    }

    public Size getLargestVideoSize() {
        return new Size(1920, 1080);
    }

    public CaptureRequest.Builder createVideoCaptureRequest(Surface surface, Surface recordSurface) throws CameraAccessException {
        CaptureRequest.Builder builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
        builder.addTarget(surface);
        builder.addTarget(recordSurface);
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        return builder;
    }

    @Override
    public void createCaptureSession(IPreviewHandler mPreviewSurface, ISaveHandler mSaveSurface, CaptureSessionHolder captureSessionHolder, Handler mHandler) {
        Log.d("Sundeep ", "createCaptureSession isFront " + isFront());
        try {
            mCameraDevice.createCaptureSession(Arrays.asList(mPreviewSurface.getTarget(), mSaveSurface.getTarget()), captureSessionHolder, mHandler);
        } catch (CameraAccessException e) {
            throw new RuntimeException("Unable to access camera");
        }
    }
}