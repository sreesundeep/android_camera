package com.example.android.camera2basic.viewmodels;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.ImageButton;

import androidx.core.content.FileProvider;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.android.camera2basic.model.CameraInteractor;
import com.example.control_panel.ModeSwitchScrollView;

import java.io.File;

public class ControlPanel implements ModeSwitchScrollView.SelectListener {
    final MutableLiveData<String> mSavedFilePath = new MutableLiveData<String>();
    final MutableLiveData<String> mErrorMessage = new MutableLiveData<String>();
    final MutableLiveData<Boolean> mCameraNotAvailable = new MutableLiveData<Boolean>();
    final MutableLiveData<String> mButtonLabel = new MutableLiveData<String>();
    private final CameraInteractor mCameraInteractor;
    private ImageButton mThumbNailView;
    private String mFileName;

    public ControlPanel(CameraInteractor cameraInteractor) {
        mCameraInteractor = cameraInteractor;
    }

    public void setButtonLabel(String label) {
        mButtonLabel.postValue(label);
    }

    public void setFilePath(String filePath) {
        mSavedFilePath.postValue(filePath);
        mFileName = filePath;
    }

    public void observeSavedFilePath(LifecycleOwner viewLifecycleOwner, Observer<? super String> showToast) {
        mSavedFilePath.observe(viewLifecycleOwner, showToast);
    }

    public void observeButtonLabel(LifecycleOwner viewLifecycleOwner, Observer<? super String> observer) {
        mButtonLabel.observe(viewLifecycleOwner, observer);
    }

    public void observeCameraNotAvailable(LifecycleOwner viewLifecycleOwner, Observer<? super Boolean> cameraNotAvailable) {
        mCameraNotAvailable.observe(viewLifecycleOwner, cameraNotAvailable);
    }

    public void observeErrorMessage(LifecycleOwner viewLifecycleOwner, Observer<? super String> showToast) {
        mErrorMessage.observe(viewLifecycleOwner, showToast);
    }

    public void setErrorMessage(String failed) {
        mErrorMessage.postValue(failed);
    }

    public void setCameraNotAvailable(boolean b) {
        mCameraNotAvailable.postValue(b);
    }

    public void showPreview(Context context) {
        File file = new File(mFileName);
        Uri path = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        Intent intent = new Intent(Intent.ACTION_VIEW, path);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(intent);
    }

    public int getDefaultMode() {
        return 0;
    }

    @Override
    public void onSelect(int beforePosition, int position) {
        mCameraInteractor.setMode(position);
    }

    public void setThumbNailView(ImageButton thumbNailView) {
        this.mThumbNailView = thumbNailView;
    }

    public void updateThumbNail(boolean isPhotoMode) {
        try {
            File file = new File(mFileName);
            Bitmap bitmap = null;
            if (file.exists()) {
                if (isPhotoMode) {
                    bitmap = ThumbnailUtils.createImageThumbnail(file, new Size(64, 64), null);
                } else {
                    Log.d("vishal", "file does  exist");
                    bitmap = ThumbnailUtils.createVideoThumbnail(file, new Size(64, 64), null);
                    Log.d("vishal", Integer.toString(bitmap.getByteCount()));
                }
                mThumbNailView.setImageBitmap(bitmap);
            } else {
                Log.d("vishal", "file does not exist");
            }
        } catch (Exception e) {
            Log.d("vishal", "error creating thumbnail");
            e.printStackTrace();
        }
    }
}