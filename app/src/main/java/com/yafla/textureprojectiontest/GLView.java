package com.yafla.textureprojectiontest;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class GLView extends GLSurfaceView implements View.OnTouchListener {
    GLRenderer renderer;

    Context context;
    public GLView(Context context) {
        super(context);
        this.context = context;

        this.setEGLContextClientVersion(2);
        this.setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        renderer = new GLRenderer(this.getContext());
        renderer.setPerspectiveCorrection(currentPerspectiveCorrectionMode);
        this.setRenderer(renderer);
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        this.setPreserveEGLContextOnPause(false);
        this.setOnTouchListener(this);

    }

    boolean currentPerspectiveCorrectionMode = true;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                // switch between perspective correction modes.
                currentPerspectiveCorrectionMode = !currentPerspectiveCorrectionMode;
                renderer.setPerspectiveCorrection(currentPerspectiveCorrectionMode);

                String displayText;
                if (currentPerspectiveCorrectionMode) {
                    displayText = " ++++ Enabled Texture Projection";
                } else {
                    displayText = " Disable Texture Projection ----";
                }
                Toast.makeText(this.context, displayText, Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }
}
