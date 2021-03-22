package com.example.android.camera2basic.providers;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;

import com.example.android.camera2basic.camera2.CameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;

public class CameraProvider {

    public static ICameraDeviceHolder getBackCamera(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return new CameraDeviceHolder(context, cameraId);
                }
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("Unable to access the camera");
        }
        return null;
    }

    public static ICameraDeviceHolder getFrontCamera(Context context) {
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            for (String cameraId : manager.getCameraIdList()) {
                CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                // We don't use a front facing camera in this sample.
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return new CameraDeviceHolder(context, cameraId);
                }
            }
        } catch (CameraAccessException e) {
            throw new RuntimeException("Unable to access the camera");
        }
        return null;
    }
}
