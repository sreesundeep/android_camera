package com.example.android.camera2basic.interfaces;

import android.graphics.Point;
import android.util.Size;
import android.view.Surface;

public interface IPreviewHandler {
    Surface getTarget();

    void initialize();

    void configureTransform(int rotation);

    void calculateBestPreviewSize(Size largest, boolean swappedDimensions, Point displaySize, int orientation, Size[] previewSizes);

    void close();

    void setSurfaceAvailableCallback(ISurfaceAvailableCallback callback);

    boolean isAvailable();

    interface ISurfaceAvailableCallback {
        void onSurfaceAvailable(int width, int height);
        void onSurfaceResized(int width, int height);
    }
}
