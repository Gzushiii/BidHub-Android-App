package com.cc106.bidhub.toast;

import com.cc106.bidhub.R;

/**
 * Enumeration of toast notification types with their associated styling and behavior
 */
public enum ToastType {
    SUCCESS("Success", R.color.success_green, R.color.white, R.drawable.ic_check_circle),
    ERROR("Error", R.color.error_red, R.color.white, R.drawable.ic_error),
    WARNING("Warning", R.color.warning_yellow, R.color.black, R.drawable.ic_warning),
    INFO("Info", R.color.info_blue, R.color.white, R.drawable.ic_info),
    LOADING("Loading", R.color.primary_blue, R.color.white, R.drawable.ic_loading),
    CUSTOM("Custom", R.color.primary_blue, R.color.white, 0);

    private final String displayName;
    private final int backgroundColorRes;
    private final int textColorRes;
    private final int iconRes;

    ToastType(String displayName, int backgroundColorRes, int textColorRes, int iconRes) {
        this.displayName = displayName;
        this.backgroundColorRes = backgroundColorRes;
        this.textColorRes = textColorRes;
        this.iconRes = iconRes;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getBackgroundColorRes() {
        return backgroundColorRes;
    }

    public int getTextColorRes() {
        return textColorRes;
    }

    public int getIconRes() {
        return iconRes;
    }
}
