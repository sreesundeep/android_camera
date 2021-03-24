package com.example.android.camera2basic.preview;

import android.content.res.Configuration;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.camera2.util.CompareSizesByArea;
import com.example.android.camera2basic.interfaces.IPreviewHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class BasePreviewHandler implements IPreviewHandler {
    public static final String TAG = "PreviewHandler";
    static final int MAX_PREVIEW_WIDTH = 1920;
    static final int MAX_PREVIEW_HEIGHT = 1080;
    protected Size mPreviewSize;
    protected ISurfaceAvailableCallback mSurfaceAvailableCallback;
    int mHeight = MAX_PREVIEW_HEIGHT;
    int mWidth = MAX_PREVIEW_WIDTH;

    public void calculateBestPreviewSize(Size largest, boolean swappedDimensions, Point displaySize, int orientation, Size[] outputSizes) {
        int rotatedPreviewWidth = swappedDimensions ? getViewHeight() : getViewWidth();
        int rotatedPreviewHeight = swappedDimensions ? getViewWidth() : getViewHeight();
        int maxPreviewWidth = swappedDimensions ? displaySize.y : displaySize.x;
        int maxPreviewHeight = swappedDimensions ? displaySize.x : displaySize.y;
        if (maxPreviewWidth > MAX_PREVIEW_WIDTH) {
            maxPreviewWidth = MAX_PREVIEW_WIDTH;
        }
        if (maxPreviewHeight > MAX_PREVIEW_HEIGHT) {
            maxPreviewHeight = MAX_PREVIEW_HEIGHT;
        }
        // Danger, W.R.! Attempting to use too large a preview size could  exceed the camera
        // bus' bandwidth limitation, resulting in gorgeous previews but the storage of
        // garbage capture data.
        /*mPreviewSize = chooseOptimalSize(outputSizes,
                rotatedPreviewWidth, rotatedPreviewHeight, maxPreviewWidth,
                maxPreviewHeight, largest);

        Log.d(TAG, "Sundeep swappedDimensions "+swappedDimensions);
        Log.d(TAG, "Sundeep final choose preview size "+mPreviewSize);*/

        mPreviewSize = new Size(640, 480);
        // We fit the aspect ratio of TextureView to the size of preview we picked.
        /*if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        } else {
            setAspectRatio(
                    mPreviewSize.getHeight(), mPreviewSize.getWidth());
        }*/
        setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getWidth());
        mWidth = mPreviewSize.getWidth();
        mHeight = mPreviewSize.getHeight();
    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public void setSurfaceAvailableCallback(ISurfaceAvailableCallback callback) {
        mSurfaceAvailableCallback = callback;
    }

    public void configureTransform(int rotation) {
        Matrix matrix = new Matrix();
        RectF viewRect = new RectF(0, 0, getViewWidth(), getViewHeight());
        RectF bufferRect = new RectF(0, 0, mPreviewSize.getHeight(), mPreviewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) getViewHeight() / mPreviewSize.getHeight(),
                    (float) getViewWidth() / mPreviewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180, centerX, centerY);
        }
        setTransform(matrix);
    }

    private Size chooseOptimalSize(Size[] choices, int textureViewWidth,
                                   int textureViewHeight, int maxWidth, int maxHeight, Size aspectRatio) {
        // Collect the supported resolutions that are at least as big as the preview Surface
        List<Size> bigEnough = new ArrayList<>();
        // Collect the supported resolutions that are smaller than the preview Surface
        List<Size> notBigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            Log.d(TAG, "Sundeep preview size "+option);
            Log.d(TAG, "Sundeep texture size "+new Size(textureViewWidth, textureViewHeight));
            if (option.getWidth() <= maxWidth && option.getHeight() <= maxHeight &&
                    option.getHeight() == option.getWidth() * h / w) {
                if (option.getWidth() >= textureViewWidth &&
                        option.getHeight() >= textureViewHeight) {
                    bigEnough.add(option);
                } else {
                    notBigEnough.add(option);
                }
            }
        }
        // Pick the smallest of those big enough. If there is no one big enough, pick the
        // largest of those not big enough.
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else if (notBigEnough.size() > 0) {
            return Collections.max(notBigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }

    protected abstract void setAspectRatio(int width, int height);

    protected abstract int getViewWidth();

    protected abstract int getViewHeight();

    protected abstract void setTransform(Matrix matrix);
}
