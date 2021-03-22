package com.example.android.camera2basic.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.example.android.camera2basic.R;

/**
 * Shows OK/Cancel confirmation dialog about camera permission.
 */
public class ConfirmationDialog extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Fragment parent = getParentFragment();
        return new AlertDialog.Builder(getActivity())
                .setMessage(R.string.request_permission)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> parent.requestPermissions(Camera2BasicFragment.VIDEO_PERMISSIONS,
                        Camera2BasicFragment.REQUEST_VIDEO_PERMISSION))
                .setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> {
                            Activity activity = parent.getActivity();
                            if (activity != null) {
                                activity.finish();
                            }
                        })
                .create();
    }
}
