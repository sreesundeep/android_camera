/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.camera2basic.ui;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.example.android.camera2basic.model.CameraInteractor;
import com.example.android.camera2basic.R;
import com.example.android.camera2basic.opengl.CustomGLSurfaceView;
import com.example.android.camera2basic.providers.CameraProvider;
import com.example.android.camera2basic.viewmodels.ControlPanel;
import com.example.control_panel.ModeSwitchAdapter;
import com.example.control_panel.ModeSwitchScrollView;

import java.io.File;

public class Camera2BasicFragment extends Fragment
        implements View.OnClickListener, ActivityCompat.OnRequestPermissionsResultCallback {
    public static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };
    static final int REQUEST_VIDEO_PERMISSION = 1;
    private static final String FRAGMENT_DIALOG = "dialog";
    private AutoFitTextureView mFrontTextureView;
    private AutoFitTextureView mBackTextureView;
    private CameraInteractor mInteractor;
    private Button mButton;
    private ImageButton mPreview;
    private ControlPanel viewModel;
    private ModeSwitchScrollView mModeSwitcher;
    private ImageView mModeSwitchBarPoint;
    private CustomGLSurfaceView mGLSurfaceView;

    public static Camera2BasicFragment newInstance() {
        return new Camera2BasicFragment();
    }

    private void showToast(final String text) {
        Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_camera2_basic, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        view.findViewById(R.id.picture).setOnClickListener(this);
        view.findViewById(R.id.info).setOnClickListener(this);
        mFrontTextureView = view.findViewById(R.id.fronttexture);
        mBackTextureView = view.findViewById(R.id.backtexture);

        // mGLSurfaceView = view.findViewById(R.id.gltexture);
        mButton = view.findViewById(R.id.picture);
        mPreview = view.findViewById(R.id.preview);
        mPreview.setOnClickListener(v -> {
            viewModel.showPreview(getContext());
        });
        mModeSwitcher = view.findViewById(R.id.mode_switch_bar);
        mModeSwitchBarPoint = view.findViewById(R.id.mode_switch_bar_point);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Point displaySize = new Point();
        getActivity().getWindowManager().getDefaultDisplay().getSize(displaySize);
        mInteractor =
                new CameraInteractor(
                        getContext(),
                        mBackTextureView,
                    mFrontTextureView,
                        new DisplayParams(
                                getActivity().getWindowManager().getDefaultDisplay().getRotation(),
                                getResources().getConfiguration().orientation,
                                displaySize),
                        mGLSurfaceView,
                        CameraProvider.getBackCamera(getContext()),
                        CameraProvider.getFrontCamera(getContext()));
        mModeSwitcher.setAdapter(
                new ModeSwitchAdapter(getContext(), mInteractor.getCameraModes()),
                mModeSwitchBarPoint);
        mModeSwitcher.setEnableSwitchMode(true);
        viewModel = mInteractor.getControlPanel();
        viewModel.setThumbNailView(mPreview);
        viewModel.observeErrorMessage(getViewLifecycleOwner(), this::showToast);
        viewModel.observeCameraNotAvailable(getViewLifecycleOwner(), this::cameraNotAvailable);
        viewModel.observeSavedFilePath(getViewLifecycleOwner(), this::updateThumbnail);
        viewModel.observeButtonLabel(
                getViewLifecycleOwner(),
                (String text) -> {
                    mButton.setText(text);
                });
        mModeSwitcher.setDefaultSelectedIndex(viewModel.getDefaultMode());
        mModeSwitcher.setSelectListener(viewModel);
    }

    private void updateThumbnail(String imagePath) {
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                //Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(file, new Size(64, 64), null);
                //mPreview.setImageBitmap(bitmap);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cameraNotAvailable(Boolean unavailable) {
        if (unavailable) {
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestCameraPermission();
        } else {
            getLifecycle().addObserver(mInteractor);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSION);
        }
    }

    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_VIDEO_PERMISSION) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        ErrorDialog.newInstance(getString(R.string.intro_message))
                                .show(getChildFragmentManager(), FRAGMENT_DIALOG);
                        break;
                    }
                }
            } else {
                ErrorDialog.newInstance(getString(R.string.intro_message))
                        .show(getChildFragmentManager(), FRAGMENT_DIALOG);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.picture: {
                mInteractor.capture();
                break;
            }
            case R.id.info: {
                Activity activity = getActivity();
                if (null != activity) {
                    new AlertDialog.Builder(activity)
                            .setMessage(R.string.intro_message)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                }
                break;
            }
        }
    }
}
