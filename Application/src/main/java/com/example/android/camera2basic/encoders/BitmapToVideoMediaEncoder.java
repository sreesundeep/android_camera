package com.example.android.camera2basic.encoders;

import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.android.camera2basic.util.DebugUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

public class BitmapToVideoMediaEncoder extends com.example.android.camera2basic.encoders.MediaEncoder {

    private static final String TAG = "BitmapToVideoDebug";

    // private MediaCodec mediaCodec;
    // private MediaMuxer mediaMuxer;

    private final Object mFrameSync = new Object();
    private CountDownLatch mNewFrameLatch;

    private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video Coding
    private static final int BIT_RATE = 16000000;
    private static final int FRAME_RATE = 30; // Frames per second

    private static final int I_FRAME_INTERVAL = 1;

    private int mGenerateIndex = 0;

    private final int mWidth;
    private final int mHeight;

    public BitmapToVideoMediaEncoder(final MediaMuxerWrapper muxer, final MediaEncoderListener listener, final int width, final int height) {
        super(muxer, listener);
        Log.i(TAG, "MediaVideoEncoder: ");
        mWidth = width;
        mHeight = height;
        //	mRenderHandler = RenderHandler.createHandler(TAG);
    }


    public void queueFrame(Bitmap bitmap) {
        if (mMediaCodec == null) {
            Log.d(TAG, "Failed to queue frame. Encoding not started");
            return;
        }

        Log.d(TAG, "Queueing frame");
  //      mEncodeQueue.add(bitmap);

        synchronized (mFrameSync) {
            if ((mNewFrameLatch != null) && (mNewFrameLatch.getCount() > 0)) {
                mNewFrameLatch.countDown();
            }

//            Bitmap bitmap = mEncodeQueue.poll();
            if (bitmap == null) {
                synchronized (mFrameSync) {
                    mNewFrameLatch = new CountDownLatch(1);
                }

                try {
                    mNewFrameLatch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

    //            bitmap = mEncodeQueue.poll();
            }

            if (bitmap == null) return;

            byte[] byteConvertFrame = getNV21(bitmap.getWidth(), bitmap.getHeight(), bitmap);

            long TIMEOUT_USEC = 500000;
            int inputBufIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            long ptsUsec = computePresentationTime(mGenerateIndex);
            if (inputBufIndex >= 0) {
                try {
                    final ByteBuffer inputBuffer = mMediaCodec.getInputBuffer(inputBufIndex);
                    inputBuffer.clear();
                    inputBuffer.put(byteConvertFrame);
                    mMediaCodec.queueInputBuffer(inputBufIndex, 0, byteConvertFrame.length, ptsUsec, 0);
                    mGenerateIndex++;
                    frameAvailableSoon();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    void prepare() {

        MediaCodecInfo codecInfo = selectCodec();
        if (codecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        Log.d(TAG, "found codec: " + codecInfo.getName());
        int colorFormat;
        try {
            colorFormat = selectColorFormat(codecInfo);
        } catch (Exception e) {
            colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        }
        Log.d(TAG, "selected color format: " + colorFormat);
        try {
            mMediaCodec = MediaCodec.createByCodecName(codecInfo.getName());
        } catch (IOException e) {
            Log.e(TAG, "Unable to create MediaCodec " + e.getMessage());
            return;
        }

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE, mWidth, mHeight);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
        mMediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
    }

    //    private void release() {
    //        if (mediaCodec != null) {
    //            mediaCodec.stop();
    //            mediaCodec.release();
    //            mediaCodec = null;
    //            Log.d(TAG, "RELEASE CODEC");
    //        }
    //        if (mediaMuxer != null) {
    //            mediaMuxer.stop();
    //            mediaMuxer.release();
    //            mediaMuxer = null;
    //            Log.d(TAG, "RELEASE MUXER");
    //        }
    //    }

    @Nullable
    private static MediaCodecInfo selectCodec() {
        int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(BitmapToVideoMediaEncoder.MIME_TYPE)
                        && codecInfo.getName().contains("qcom")) {
                    return codecInfo;
                }
            }
        }
        return null;
    }

    private static int selectColorFormat(@NonNull MediaCodecInfo codecInfo) {
        MediaCodecInfo.CodecCapabilities capabilities =
                codecInfo.getCapabilitiesForType(BitmapToVideoMediaEncoder.MIME_TYPE);
        DebugUtils.log("ColorFormats: " + Arrays.toString(capabilities.colorFormats));
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        return 0; // not reached
    }

    private static boolean isRecognizedFormat(int colorFormat) {
        // these are the formats we know how to handle for
        return colorFormat == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
    }

    @NonNull
    private byte[] getNV21(int inputWidth, int inputHeight, @NonNull Bitmap scaled) {

        int[] argb = new int[inputWidth * inputHeight];

        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight);

        byte[] yuv = new byte[inputWidth * inputHeight * 3 / 2];
        encodeYUV420SP(yuv, argb, inputWidth, inputHeight);
        scaled.recycle();

        return yuv;
    }

    private void encodeYUV420SP(byte[] yuv420sp, int[] argb, int width, int height) {
        final int frameSize = width * height;

        int yIndex = 0;
        int uvIndex = frameSize;

        int a, R, G, B, Y, U, V;
        int index = 0;
        for (int j = 0; j < height; j++) {
            for (int i = 0; i < width; i++) {

                a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
                R = (argb[index] & 0xff0000) >> 16;
                G = (argb[index] & 0xff00) >> 8;
                B = (argb[index] & 0xff);

                Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
                U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
                V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

                yuv420sp[yIndex++] = (byte) ((Y < 0) ? 0 : (Math.min(Y, 255)));
                if (j % 2 == 0 && index % 2 == 0) {
                    yuv420sp[uvIndex++] = (byte) ((U < 0) ? 0 : (Math.min(U, 255)));
                    yuv420sp[uvIndex++] = (byte) ((V < 0) ? 0 : (Math.min(V, 255)));
                }

                index++;
            }
        }
    }

    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / BitmapToVideoMediaEncoder.FRAME_RATE;
    }
}
