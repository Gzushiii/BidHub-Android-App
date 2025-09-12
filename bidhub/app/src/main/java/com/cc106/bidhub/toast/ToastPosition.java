package com.cc106.bidhub.toast;

import android.view.Gravity;

/**
 * Enumeration of toast notification positions
 */
public enum ToastPosition {
    TOP(Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 100),
    TOP_LEFT(Gravity.TOP | Gravity.START, 50, 100),
    TOP_RIGHT(Gravity.TOP | Gravity.END, 50, 100),
    CENTER(Gravity.CENTER, 0, 0),
    BOTTOM(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 100),
    BOTTOM_LEFT(Gravity.BOTTOM | Gravity.START, 50, 100),
    BOTTOM_RIGHT(Gravity.BOTTOM | Gravity.END, 50, 100);

    private final int gravity;
    private final int xOffset;
    private final int yOffset;

    ToastPosition(int gravity, int xOffset, int yOffset) {
        this.gravity = gravity;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public int getGravity() {
        return gravity;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }
}
