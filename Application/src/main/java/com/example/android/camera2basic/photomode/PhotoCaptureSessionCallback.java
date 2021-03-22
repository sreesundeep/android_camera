package com.example.android.camera2basic.photomode;

import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.view.Surface;

import androidx.annotation.NonNull;

import com.example.android.camera2basic.util.ComboCaptureSessionCaptureCallback;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ISaveHandler;

import java.util.Arrays;

class PhotoCaptureSessionCallback extends CameraCaptureSession.CaptureCallback {
    /**
     * Camera state: Showing camera preview.
     */
    private static final int STATE_PREVIEW = 0;
    /**
     * Camera state: Waiting for the focus to be locked.
     */
    private static final int STATE_WAITING_LOCK = 1;
    /**
     * Camera state: Waiting for the exposure to be precapture state.
     */
    private static final int STATE_WAITING_PRECAPTURE = 2;
    /**
     * Camera state: Waiting for the exposure state to be something other than precapture.
     */
    private static final int STATE_WAITING_NON_PRECAPTURE = 3;
    /**
     * Camera state: Picture was taken.
     */
    private static final int STATE_PICTURE_TAKEN = 4;
    private final ICameraDeviceHolder mCameraDeviceHolder;
    private final ICaptureSessionHolder mCaptureSessionHolder;
    private final int mRotation;
    private final ISaveHandler mSaveHandler;
    private final CameraCaptureSession.CaptureCallback mCallerCaptureCallback;
    /**
     * The current state of camera state for taking pictures.
     */
    private int mState = STATE_PREVIEW;
    private CaptureRequest.Builder mPreviewRequestBuilder;
    private CaptureRequest mPreviewRequest;

    public PhotoCaptureSessionCallback(ICaptureSessionHolder captureSessionHolder, ICameraDeviceHolder deviceHolder, int rotation, ISaveHandler saveHandler, CameraCaptureSession.CaptureCallback callerCaptureCallback) {
        mCaptureSessionHolder = captureSessionHolder;
        mCameraDeviceHolder = deviceHolder;
        mRotation = rotation;
        mSaveHandler = saveHandler;
        mCallerCaptureCallback = callerCaptureCallback;
    }

    private void process(CaptureResult result) {
        switch (mState) {
            case STATE_PREVIEW: {
                // We have nothing to do when the camera preview is working normally.
                break;
            }
            case STATE_WAITING_LOCK: {
                Integer afState = result.get(CaptureResult.CONTROL_AF_STATE);
                if (afState == null) {
                    captureStillPicture();
                } else if (CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED == afState ||
                        CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED == afState) {
                    // CONTROL_AE_STATE can be null on some devices
                    Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                    if (aeState == null ||
                            aeState == CaptureResult.CONTROL_AE_STATE_CONVERGED) {
                        mState = STATE_PICTURE_TAKEN;
                        captureStillPicture();
                    } else {
                        runPrecaptureSequence();
                    }
                }
                break;
            }
            case STATE_WAITING_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null ||
                        aeState == CaptureResult.CONTROL_AE_STATE_PRECAPTURE ||
                        aeState == CaptureRequest.CONTROL_AE_STATE_FLASH_REQUIRED) {
                    mState = STATE_WAITING_NON_PRECAPTURE;
                }
                break;
            }
            case STATE_WAITING_NON_PRECAPTURE: {
                // CONTROL_AE_STATE can be null on some devices
                Integer aeState = result.get(CaptureResult.CONTROL_AE_STATE);
                if (aeState == null || aeState != CaptureResult.CONTROL_AE_STATE_PRECAPTURE) {
                    mState = STATE_PICTURE_TAKEN;
                    captureStillPicture();
                }
                break;
            }
        }
    }

    @Override
    public void onCaptureProgressed(@NonNull CameraCaptureSession session,
                                    @NonNull CaptureRequest request,
                                    @NonNull CaptureResult partialResult) {
        process(partialResult);
    }

    @Override
    public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                   @NonNull CaptureRequest request,
                                   @NonNull TotalCaptureResult result) {
        process(result);
    }

    public void createPreviewRequest(Surface surface) {
        try {
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewRequestBuilder = mCameraDeviceHolder.createPreviewRequestBuilder(surface);
            // Finally, we start displaying the camera preview.
            mPreviewRequest = mPreviewRequestBuilder.build();
            mCaptureSessionHolder.setRepeatingRequest(mPreviewRequest, this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void takePicture() {
        lockFocus();
    }

    private void lockFocus() {
        try {
            // Tell #mCaptureCallback to wait for the lock.
            mState = STATE_WAITING_LOCK;
            mCaptureSessionHolder.capture(mCameraDeviceHolder.getLockFocusRequestBuilder(mPreviewRequestBuilder).build(), this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void runPrecaptureSequence() {
        try {
            // Tell #mCaptureCallback to wait for the precapture sequence to be set.
            mState = STATE_WAITING_PRECAPTURE;
            mCaptureSessionHolder.capture(mCameraDeviceHolder.getTriggerPrecaptureRequestBuilder(mPreviewRequestBuilder).build(), this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void captureStillPicture() {
        try {
            // This is the CaptureRequest.Builder that we use to take a picture.
            final CaptureRequest.Builder captureBuilder =
                    mCameraDeviceHolder.createStillCaptureRequest(mSaveHandler.getTarget(), mCameraDeviceHolder.getOrientation(mRotation));
            ComboCaptureSessionCaptureCallback multiCaptureSessionStateCallback = new ComboCaptureSessionCaptureCallback(Arrays.asList(
                    mCallerCaptureCallback,
                    new CameraCaptureSession.CaptureCallback() {
                        @Override
                        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                       @NonNull CaptureRequest request,
                                                       @NonNull TotalCaptureResult result) {
                            unlockFocus();
                        }
                    }
            ));
            mCaptureSessionHolder.stopRepeating(true);
            mCaptureSessionHolder.capture(captureBuilder.build(), multiCaptureSessionStateCallback, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void unlockFocus() {
        try {
            // After this, the camera will go back to the normal state of preview.
            mState = STATE_PREVIEW;
            // Reset the auto-focus trigger
            mCaptureSessionHolder.capture(mCameraDeviceHolder.getUnlockFocusRequestBuilder(mPreviewRequestBuilder).build(), this);
            mCaptureSessionHolder.setRepeatingRequest(mPreviewRequest, this);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        mCaptureSessionHolder.close();
    }
}
