package com.example.android.camera2basic.videomode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.CameraCaptureSession;
import android.media.Image;
import android.media.ImageReader;
import android.nfc.cardemulation.OffHostApduService;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.example.android.camera2basic.encoders.BitmapToVideoMediaEncoder;
import com.example.android.camera2basic.encoders.MediaAudioEncoder;
import com.example.android.camera2basic.encoders.MediaEncoder;
import com.example.android.camera2basic.encoders.MediaMuxerWrapper;
import com.example.android.camera2basic.interfaces.ICameraDeviceHolder;
import com.example.android.camera2basic.interfaces.ICaptureSessionHolder;
import com.example.android.camera2basic.interfaces.ISessionStateCallback;
import com.example.android.camera2basic.ui.DisplayParams;
import com.example.android.camera2basic.camera2.CaptureSessionHolder;
import com.example.android.camera2basic.util.BitmapToVideoEncoder;
import com.example.android.camera2basic.util.ComboCaptureSessionStateCallback;
import com.example.android.camera2basic.util.NoOpCaptureSessionStateCallback;
import com.example.android.camera2basic.interfaces.IPreviewHandler;
import com.example.android.camera2basic.interfaces.ISaveHandler;
import com.example.android.camera2basic.interfaces.IVideoMode;
import com.example.android.camera2basic.interfaces.IVideoSaveHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

public class VideoMode implements IVideoMode {
    private static final boolean DEBUG = true;
    private static final String TAG = "VideoMode";
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
    private BitmapToVideoMediaEncoder mBitmapToVideoMediaEncoder;
    private MediaMuxerWrapper mMuxer;

    int finalVideoWidth = 960;
    int finalVideoHeight = 640;

    public VideoMode(Context context, IPreviewHandler backPreviewHandler, IPreviewHandler frontPreviewHandler, IVideoSaveHandler backSaveHandler, IVideoSaveHandler frontSaveHandler, DisplayParams displayParams, ICameraDeviceHolder backCamera, ICameraDeviceHolder frontCamera) {
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
            mBackCaptureCallback.close();
            mBackCaptureSessionHolder.close();
            mBackPreviewHandler.close();
            mBackSaveHandler.close();

            mFrontCaptureCallback.close();
            mFrontCaptureSessionHolder.close();
            mFrontPreviewHandler.close();
            mFrontSaveHandler.close();
        } catch (Exception e) {
            e.printStackTrace();
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
        Size largest = mBackCamera.getLargestSize(isPhotoMode());
        saveHandler.initialize(mBackgroundHandler, largest);

        // Find out if we need to swap dimension to get the preview size relative to sensor
        // coordinate.
        boolean swappedDimensions = mBackCamera.shouldSwapDimensions(mDisplayParams.getRotation());
        swappedDimensions = false;
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
                    Bitmap bitmap = fromByteBuffer(buffer);
                    if (mRecording) {
                        ffc_bitmap_queue.add(bitmap);
                        // mergeFrontAndBackCameraFrames
                        mergeFrontAndBackCameraFrames();
                    }
                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
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
                    Bitmap bitmap = fromByteBuffer(buffer);
                    if (mRecording) {
                        rfc_bitmap_queue.add(bitmap);
                    }
                    image.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, mBackgroundHandler);
    }

    private void mergeFrontAndBackCameraFrames() {
        if (!rfc_bitmap_queue.isEmpty() && !ffc_bitmap_queue.isEmpty()) {
            Log.d("Sundeep ", "mergeFrontAndBackCameraFrames");
            Bitmap ffcBitmap = ffc_bitmap_queue.poll();
            ffcBitmap = getRotatedBitmap(ffcBitmap, ffcBitmap.getWidth(), ffcBitmap.getHeight(), 270);
            Bitmap rfcBitmap = rfc_bitmap_queue.poll();
            rfcBitmap = getRotatedBitmap(rfcBitmap, rfcBitmap.getWidth(), rfcBitmap.getHeight(), 90);

            if (ffcBitmap != null & rfcBitmap != null) {
                int width = ffcBitmap.getWidth() * 2;
                int height = ffcBitmap.getHeight();

                Bitmap cs = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas comboImage = new Canvas(cs);
                comboImage.drawBitmap(ffcBitmap, 0, 0, null);
                comboImage.drawBitmap(rfcBitmap, ffcBitmap.getWidth(), 0, null);

                mBitmapToVideoMediaEncoder.queueFrame(cs);

                /**
                 * Saving merged frame to disk.
                 * Comment it for now, as we don't need it now.
                 */

                /*try {
                    String filename = "Merged_FFC_RFC_" + new SimpleDateFormat("MMddHHmmss").format(new Date()) + ".jpg";
                    File sd = mContext.getExternalFilesDir(null);
                    File dest = new File(sd, filename);
                    FileOutputStream out = new FileOutputStream(dest);
                    ffcBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                    Log.d("Sundeep ", "mergeFrontAndBackCameraFrames File Saved ///////");
                } catch (Exception e) {
                    Log.d("Sundeep ", "mergeFrontAndBackCameraFrames File Saving Failed XXXXX ");
                    e.printStackTrace();
                }*/
            }
        }
    }

    private Bitmap getRotatedBitmap(Bitmap bitmap, int width, int height, int rotation) {
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
        return Bitmap.createBitmap(scaledBitmap, 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix, true);
    }

    private Bitmap fromByteBuffer(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes, 0, bytes.length);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

/*
    @Override
    public void startRecording() {
        mRecording = true;
        // mBackSaveHandler.startRecording();
        String filename =
            "Video_FFC_RFC_" + new SimpleDateFormat("MMddHHmmss").format(new Date()) + ".mp4";
        File sd = mContext.getExternalFilesDir(null);
        File dest = new File(sd, filename);
        mBitmapToVideoEncoder.startEncoding(finalVideoWidth, finalVideoHeight, dest);
    }

    @Override
    public void stopRecording() {
        if (mRecording) {
            mRecording = false;
            //mBackSaveHandler.stopRecording();
            mBitmapToVideoEncoder.stopEncoding();
        }
    }
*/


    /**
     * start resorcing
     * This is a sample project and call this on UI thread to avoid being complicated
     * but basically this should be called on private thread because prepareing
     * of encoder is heavy work
     */

    @Override
    public void startRecording() {
        if (DEBUG) Log.v(TAG, "startRecording:");
        mRecording = true;

        String filename =
                "Video_FFC_RFC_" + new SimpleDateFormat("MMddHHmmss").format(new Date()) + ".mp4";
        File sd = mContext.getExternalFilesDir(null);
        File dest = new File(sd, filename);
        try {
            mMuxer = new MediaMuxerWrapper(".mp4", dest.getAbsolutePath());	// if you record audio only, ".m4a" is also OK.
            if (true) {
                // for video capturing
                mBitmapToVideoMediaEncoder =  new BitmapToVideoMediaEncoder(mMuxer, mMediaEncoderListener, finalVideoWidth, finalVideoHeight);
            }
            if (true) {
                // for audio capturing
                new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
            }
            mMuxer.prepare();
            mMuxer.startRecording();
        } catch (final IOException e) {
            Log.e(TAG, "startCapture:", e);
        }
    }

    /**
     * request stop recording
     */
    @Override
    public void stopRecording() {
        if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
        if (mMuxer != null) {
            mMuxer.stopRecording();
            mMuxer = null;
            mRecording = false;
            // you should not wait here
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
            //if (encoder instanceof com.serenegiant.encoder.MediaVideoEncoder)
                //mCameraView.setVideoEncoder((com.serenegiant.encoder.MediaVideoEncoder)encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
          //  if (encoder instanceof com.serenegiant.encoder.MediaVideoEncoder)
              //  mCameraView.setVideoEncoder(null);
        }
    };


    @Override
    public boolean isPhotoMode() {
        return false;
    }

    @Override
    public String getName() {
        return "Video";
    }

    @Override
    public boolean isRecording() {
        return mRecording;
    }

    @Override
    public void takePicture() {

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
