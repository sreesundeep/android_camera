package com.example.android.camera2basic.interfaces;

import android.os.Handler;
import android.util.Size;
import android.view.Surface;

public interface ISaveHandler {
    void initialize(Handler handler, Size largest);

    Surface getTarget();

    void close();
}
