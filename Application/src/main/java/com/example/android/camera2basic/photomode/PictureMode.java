package com.example.android.camera2basic.photomode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCaptureSession;
import android.media.Image;
import android.media.ImageReader;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;

import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ISessionStateCallback;
import com.example.android.camera2basic.ui.Camera2BasicFragment;
import com.example.android.camera2basic.ui.DisplayParams;
import com.example.android.camera2basic.camera2.CaptureSessionHolder;
import com.example.android.camera2basic.util.ComboCaptureSessionStateCallback;
import com.example.android.camera2basic.util.NoOpCaptureSessionStateCallback;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.IVideoMode;
import com.example.android.camera2basic.interfaces.IVideoSaveHandler;
import com.example.android.camera2basic.videomode.VideoCaptureSessionCallback;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PictureMode implements IVideoMode {
    private final IPreviewHandler mBackPreviewHandler;
    private final IPreviewHandler mFrontPreviewHandler;
    private final IVideoSaveHandler mBackSaveHandler;
    private final IVideoSaveHandler mFrontSaveHandler;
    private Handler mBackgroundHandler;
    private Handler mBackgroundHandler2;
    private final DisplayParams mDisplayParams;
    private final ICameraDeviceHolder mBackCamera;
    private final ICameraDeviceHolder mFrontCamera;
    private VideoCaptureSessionCallback mBackCaptureCallback;
    private VideoCaptureSessionCallback mFrontCaptureCallback;
    private boolean mRecording = false;
    private ICaptureSessionHolder mBackCaptureSessionHolder;
    private ICaptureSessionHolder mFrontCaptureSessionHolder;
    private ImageReader mFrontPreviewFrameReader;
    private ImageReader mBackPreviewFrameReader;

    ConcurrentLinkedQueue<Bitmap> ffc_bitmap_queue = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Bitmap> rfc_bitmap_queue = new ConcurrentLinkedQueue<>();
    private Context mContext;
    private Bitmap cs;
    private Bitmap ffcBitmap;
    private Bitmap rfcBitmap;

    int finalPhotoWidth = 960;
    int finalPhotoHeight = 640;

    public PictureMode(Context context, IPreviewHandler backPreviewHandler, IPreviewHandler frontPreviewHandler, IVideoSaveHandler backSaveHandler, IVideoSaveHandler frontSaveHandler, DisplayParams displayParams, ICameraDeviceHolder backCamera, ICameraDeviceHolder frontCamera) {
        mContext = context;
        mBackPreviewHandler = backPreviewHandler;
        mFrontPreviewHandler = frontPreviewHandler;
        mBackSaveHandler = backSaveHandler;
        mFrontSaveHandler = frontSaveHandler;
        mDisplayParams = displayParams;
        mBackCamera = backCamera;
        mFrontCamera = frontCamera;
    }

    @Override
    public void initialize(
            ISessionStateCallback captureSessionStateCallback,
            CameraCaptureSession.CaptureCallback callerCaptureCallback,
            ICameraDeviceHolder cameraDeviceHolder) {
        Log.d("Sundeep", "initialize isFront " + cameraDeviceHolder.isFront());
        if (cameraDeviceHolder.isFront()) {
            mFrontPreviewHandler.initialize();
            mFrontCaptureSessionHolder =
                    new CaptureSessionHolder(
                            mFrontCamera, mFrontPreviewHandler, mFrontSaveHandler, mBackgroundHandler2, mFrontPreviewFrameReader);
            ComboCaptureSessionStateCallback comboCaptureSessionStateCallback =
                    new ComboCaptureSessionStateCallback(
                            Arrays.asList(new PreviewStartTask(cameraDeviceHolder.isFront()), captureSessionStateCallback));
            mFrontCaptureCallback =
                    new VideoCaptureSessionCallback(
                            mFrontCaptureSessionHolder,
                            mFrontCamera,
                            mDisplayParams.getRotation(),
                            mFrontSaveHandler,
                            callerCaptureCallback);
            mFrontCaptureSessionHolder.createSession(comboCaptureSessionStateCallback);
        } else {
            mBackPreviewHandler.initialize();
            mBackCaptureSessionHolder =
                    new CaptureSessionHolder(
                            mBackCamera, mBackPreviewHandler, mBackSaveHandler, mBackgroundHandler, mBackPreviewFrameReader);
            ComboCaptureSessionStateCallback comboCaptureSessionStateCallback =
                    new ComboCaptureSessionStateCallback(
                            Arrays.asList(new PreviewStartTask(cameraDeviceHolder.isFront()), captureSessionStateCallback));
            mBackCaptureCallback =
                    new VideoCaptureSessionCallback(
                            mBackCaptureSessionHolder,
                            mBackCamera,
                            mDisplayParams.getRotation(),
                            mBackSaveHandler,
                            callerCaptureCallback);
            mBackCaptureSessionHolder.createSession(comboCaptureSessionStateCallback);
        }
    }

    @Override
    public void onHandlerAvailable(Handler handler1, Handler handler2) {
        mBackgroundHandler = handler1;
        mBackgroundHandler2 = handler2;
    }

    @Override
    public void close() {
        try {
            if (mBackCaptureCallback != null) {
                mBackCaptureCallback.close();
            }
            mBackCaptureSessionHolder.close();
            mBackPreviewHandler.close();
            mBackSaveHandler.close();

            if (mFrontCaptureCallback != null) {
                mFrontCaptureCallback.close();
            }
            mFrontCaptureSessionHolder.close();
            mFrontPreviewHandler.close();
            mFrontSaveHandler.close();
        }
        catch(Exception e){
            Log.e("Error", e.toString());
        }
    }

    @Override
    public void onTextureAvailable(boolean isFront) {
        // Front
        setUpCameraOutputs(mDisplayParams.getOrientation(), mFrontSaveHandler, mFrontPreviewHandler);
        mFrontPreviewHandler.configureTransform(mDisplayParams.getRotation());
        prepareFrontPreviewFrameReader(mFrontPreviewHandler.getWidth(), mFrontPreviewHandler.getWidth());

        // Back
        setUpCameraOutputs(mDisplayParams.getOrientation(), mBackSaveHandler, mBackPreviewHandler);
        mBackPreviewHandler.configureTransform(mDisplayParams.getRotation());
        prepareBackPreviewFrameReader(mBackPreviewHandler.getWidth(), mBackPreviewHandler.getWidth());
    }

    @Override
    public void updateTransform() {
        mBackPreviewHandler.configureTransform(mDisplayParams.getRotation());
    }

    private void setUpCameraOutputs(int orientation, ISaveHandler saveHandler, IPreviewHandler previewHandler) {
        // For still image captures, we use the largest available size.
        Size largest = mBackCamera.getLargestSize(false);
        saveHandler.initialize(mBackgroundHandler, largest);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        boolean swappedDimensions = mBackCamera.shouldSwapDimensions(mDisplayParams.getRotation());
        previewHandler.calculateBestPreviewSize(largest, swappedDimensions, mDisplayParams.getDisplaySize(), orientation, mBackCamera.getPreviewSizes());
    }

    private void prepareFrontPreviewFrameReader(int width, int height) {
        Log.d("Sundeep", "prepareFrontPreviewFrameReader");
        mFrontPreviewFrameReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        mFrontPreviewFrameReader.setOnImageAvailableListener(reader -> {
            // Log.d("Sundeep", "Front Frame");
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    ffcBitmap = fromByteBuffer(buffer);
                    image.close();
                }
            } catch (Exception e) {
            }
        }, mBackgroundHandler2);
    }

    private void prepareBackPreviewFrameReader(int width, int height) {
        Log.d("Sundeep", "prepareBackPreviewFrameReader");
        mBackPreviewFrameReader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
        mBackPreviewFrameReader.setOnImageAvailableListener(reader -> {
            // Log.d("Sundeep", "Back Frame");
            Image image = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    rfcBitmap = fromByteBuffer(buffer);
                    image.close();
                }
            } catch (Exception e) {
            }
        }, mBackgroundHandler);
    }

    private Bitmap fromByteBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes, 0, bytes.length);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    @Override
    public void startRecording() {
        //saveMergedPicture();
    }

    public void takePicture() {
        saveMergedPicture();
    }

    private void saveMergedPicture() {
        if (ffcBitmap != null & rfcBitmap != null) {
            ffcBitmap = getRotatedBitmap(ffcBitmap, ffcBitmap.getWidth(), ffcBitmap.getHeight(), Camera2BasicFragment.mIsZetaHardware ? 90 : 270);
            rfcBitmap = getRotatedBitmap(rfcBitmap, rfcBitmap.getWidth(), rfcBitmap.getHeight(), 90);

            cs = Bitmap.createBitmap(finalPhotoWidth, finalPhotoHeight, Bitmap.Config.ARGB_8888);
            Canvas comboImage = new Canvas(cs);
            comboImage.drawBitmap(ffcBitmap, 0, 0, null);
            comboImage.drawBitmap(rfcBitmap, ffcBitmap.getWidth(), 0, null);
        }
        String filename = "Merged_FFC_RFC_" + new SimpleDateFormat("MMddHHmmss").format(new Date()) + ".jpeg";
        File sd = mContext.getExternalFilesDir(null);
        File dest = new File(sd, filename);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(dest);
            cs.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, int width, int height, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    }

    @Override
    public void stopRecording() {

    }

    @Override
    public boolean isPhotoMode() {
        return true;
    }

    @Override
    public String getName() {
        return "Photo";
    }

    @Override
    public boolean isRecording() {
        return mRecording;
    }

    private class PreviewStartTask extends NoOpCaptureSessionStateCallback {
        private boolean mIsFront;

        PreviewStartTask(boolean isFront) {
            mIsFront = isFront;
        }

        @Override
        public void onConfigured() {
            Log.d("Sundeep", "CaptureSession Configured isFront " + mIsFront);
            if (mIsFront) {
                mFrontCaptureCallback.createPreviewRequest(mFrontPreviewHandler.getTarget(), mFrontPreviewFrameReader.getSurface());
            } else {
                mBackCaptureCallback.createPreviewRequest(mBackPreviewHandler.getTarget(), mBackPreviewFrameReader.getSurface());

            }
        }
    }
}
