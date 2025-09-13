package com.cc106.bidhub.toast;

import com.cc106.bidhub.R;

/**
 * Enumeration of toast notification animations
 */
public enum ToastAnimation {
    FADE_IN_OUT(android.R.anim.fade_in, android.R.anim.fade_out),
    SLIDE_FROM_TOP(R.anim.slide_in_top, R.anim.slide_out_top),
    SLIDE_FROM_BOTTOM(R.anim.slide_in_bottom, R.anim.slide_out_bottom),
    SLIDE_FROM_LEFT(R.anim.slide_in_left, R.anim.slide_out_left),
    SLIDE_FROM_RIGHT(R.anim.slide_in_right, R.anim.slide_out_right),
    SCALE_IN_OUT(R.anim.scale_in, R.anim.scale_out),
    BOUNCE_IN_OUT(R.anim.bounce_in, R.anim.bounce_out),
    NONE(0, 0);

    private final int enterAnimation;
    private final int exitAnimation;

    ToastAnimation(int enterAnimation, int exitAnimation) {
        this.enterAnimation = enterAnimation;
        this.exitAnimation = exitAnimation;
    }

    public int getEnterAnimation() {
        return enterAnimation;
    }

    public int getExitAnimation() {
        return exitAnimation;
    }
}
