package com.example.android.camera2basic.opengl;

import android.graphics.Matrix;
import android.graphics.Point;
import android.opengl.GLSurfaceView;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.preview.BasePreviewHandler;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class OGLPreviewHandler extends BasePreviewHandler implements GLSurfaceView.Renderer {
    private final CustomGLSurfaceView mSurfaceView;

    public OGLPreviewHandler(CustomGLSurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    @Override
    public Surface getTarget() {
        return mSurfaceView.getTarget();
    }

    @Override
    public void initialize() {
    }

    @Override
    protected void setAspectRatio(int width, int height) {
        mSurfaceView.setAspectRatio(width, height);
    }

    @Override
    protected int getViewWidth() {
        return mSurfaceView.getWidth();
    }

    @Override
    protected int getViewHeight() {
        return mSurfaceView.getHeight();
    }

    @Override
    protected void setTransform(Matrix matrix) {
        mSurfaceView.setTransform(matrix);
    }

    @Override
    public void setSurfaceAvailableCallback(ISurfaceAvailableCallback callback) {
        super.setSurfaceAvailableCallback(callback);
        mSurfaceView.getRenderer().setDelegate(this);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isAvailable() {
        return mSurfaceView.isAvailable();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mSurfaceAvailableCallback.onSurfaceAvailable(0, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mSurfaceAvailableCallback.onSurfaceResized(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
    }
}
