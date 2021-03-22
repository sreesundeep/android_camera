package com.example.android.camera2basic.ui;

import android.graphics.Point;

public class DisplayParams {
    private final int mRotation;
    private final int mOrientation;
    private final Point mDisplaySize;

    public DisplayParams(int rotation, int orientation, Point displaySize) {
        mRotation = rotation;
        mDisplaySize = displaySize;
        mOrientation = orientation;
    }

    public int getRotation() {
        return mRotation;
    }

    public int getOrientation() {
        return mOrientation;
    }

    public Point getDisplaySize() {
        return mDisplaySize;
    }
}