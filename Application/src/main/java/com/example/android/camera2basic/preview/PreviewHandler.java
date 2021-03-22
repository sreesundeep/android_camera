package com.example.android.camera2basic.preview;

import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.view.Surface;

import com.example.android.camera2basic.util.NoOpSurfaceTextureListener;
import com.example.android.camera2basic.ui.AutoFitTextureView;

public class PreviewHandler extends BasePreviewHandler {
    private AutoFitTextureView mTextureView;
    private Surface mSurface;

    public PreviewHandler(AutoFitTextureView surfaceTexture) {
        mTextureView = surfaceTexture;
    }

    @Override
    public void initialize() {
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        // We configure the size of default buffer to be the size of camera preview we want.
        //surfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        surfaceTexture.setDefaultBufferSize(500,500);
        mSurface = new Surface(surfaceTexture);
    }

    @Override
    public Surface getTarget() {
        return mSurface;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isAvailable() {
        return mTextureView.isAvailable();
    }

    @Override
    public void setSurfaceAvailableCallback(ISurfaceAvailableCallback callback) {
        super.setSurfaceAvailableCallback(callback);
        mTextureView.setSurfaceTextureListener(new SurfaceTextureCallback());
    }

    protected void setAspectRatio(int width, int height) {
        mTextureView.setAspectRatio(width, height);
    }

    protected int getViewWidth() {
        return mTextureView.getWidth();
    }

    protected int getViewHeight() {
        return mTextureView.getHeight();
    }

    protected void setTransform(Matrix matrix) {
        mTextureView.setTransform(matrix);
    }

    class SurfaceTextureCallback extends NoOpSurfaceTextureListener {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            mSurfaceAvailableCallback.onSurfaceAvailable(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            mSurfaceAvailableCallback.onSurfaceResized(width, height);
        }
    }
}
