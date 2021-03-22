package com.example.android.camera2basic.savehandlers;

import android.graphics.ImageFormat;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Size;
import android.view.Surface;

import com.example.android.camera2basic.interfaces.ISaveHandler;

import java.io.File;

public class ImageSaveHandler implements ISaveHandler, ImageReader.OnImageAvailableListener {
    private Handler mBackgroundHandler;
    private final File mFile;
    /**
     * An {@link ImageReader} that handles still image capture.
     */
    private ImageReader mImageReader;

    public ImageSaveHandler(File file) {
        mFile = file;
    }

    @Override
    public void initialize(Handler handler, Size largest) {
        mBackgroundHandler = handler;
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(),
                ImageFormat.JPEG, /*maxImages*/2);
        mImageReader.setOnImageAvailableListener(
                this, mBackgroundHandler);
    }

    @Override
    public Surface getTarget() {
        return mImageReader.getSurface();
    }

    @Override
    public void close() {
        mImageReader.close();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        //mBackgroundHandler.post(new ImageSaver(reader.acquireNextImage(), mFile));
    }
}
