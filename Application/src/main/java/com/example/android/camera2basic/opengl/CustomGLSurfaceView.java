package com.example.android.camera2basic.opengl;

import android.content.Context;
import android.graphics.Matrix;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;

public class CustomGLSurfaceView extends GLSurfaceView {
    private final CustomGLRenderer mRenderer;

    public CustomGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2);
        mRenderer = new CustomGLRenderer(this);
        setRenderer(mRenderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        super.surfaceDestroyed(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        super.surfaceChanged(holder, format, w, h);
    }

    @Override
    public void onPause() {
        mRenderer.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mRenderer.onResume();
    }

    public void setAspectRatio(int width, int height) {
        mRenderer.setAspectRatio(width, height);
    }

    public Surface getTarget() {
        return mRenderer.getTarget();
    }

    public boolean isAvailable() {
        return mRenderer.isAvailable();
    }

    public CustomGLRenderer getRenderer() {
        return mRenderer;
    }

    public void setTransform(Matrix matrix) {
        mRenderer.setTransform(matrix);
    }
}
