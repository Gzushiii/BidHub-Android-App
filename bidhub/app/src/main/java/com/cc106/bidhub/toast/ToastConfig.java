package com.cc106.bidhub.toast;

/**
 * Configuration class for toast notifications
 */
public class ToastConfig {
    private ToastType type;
    private ToastDuration duration;
    private ToastPosition position;
    private ToastAnimation animation;
    private boolean showIcon;
    private boolean enableHapticFeedback;
    private boolean enableSound;
    private boolean dismissible;
    private int customDurationMs;
    private int customBackgroundColor;
    private int customTextColor;
    private int customIcon;

    public ToastConfig() {
        // Default configuration
        this.type = ToastType.INFO;
        this.duration = ToastDuration.MEDIUM;
        this.position = ToastPosition.TOP;
        this.animation = ToastAnimation.SLIDE_FROM_TOP;
        this.showIcon = true;
        this.enableHapticFeedback = true;
        this.enableSound = false;
        this.dismissible = true;
        this.customDurationMs = 0;
        this.customBackgroundColor = 0;
        this.customTextColor = 0;
        this.customIcon = 0;
    }

    // Builder pattern for easy configuration
    public static class Builder {
        private ToastConfig config;

        public Builder() {
            this.config = new ToastConfig();
        }

        public Builder setType(ToastType type) {
            config.type = type;
            return this;
        }

        public Builder setDuration(ToastDuration duration) {
            config.duration = duration;
            return this;
        }

        public Builder setCustomDuration(int milliseconds) {
            config.duration = ToastDuration.CUSTOM;
            config.customDurationMs = milliseconds;
            return this;
        }

        public Builder setPosition(ToastPosition position) {
            config.position = position;
            return this;
        }

        public Builder setAnimation(ToastAnimation animation) {
            config.animation = animation;
            return this;
        }

        public Builder setShowIcon(boolean showIcon) {
            config.showIcon = showIcon;
            return this;
        }

        public Builder setHapticFeedback(boolean enable) {
            config.enableHapticFeedback = enable;
            return this;
        }

        public Builder setSound(boolean enable) {
            config.enableSound = enable;
            return this;
        }

        public Builder setDismissible(boolean dismissible) {
            config.dismissible = dismissible;
            return this;
        }

        public Builder setCustomColors(int backgroundColor, int textColor) {
            config.customBackgroundColor = backgroundColor;
            config.customTextColor = textColor;
            return this;
        }

        public Builder setCustomIcon(int iconRes) {
            config.customIcon = iconRes;
            return this;
        }

        public ToastConfig build() {
            return config;
        }
    }

    // Getters
    public ToastType getType() { return type; }
    public ToastDuration getDuration() { return duration; }
    public ToastPosition getPosition() { return position; }
    public ToastAnimation getAnimation() { return animation; }
    public boolean isShowIcon() { return showIcon; }
    public boolean isHapticFeedbackEnabled() { return enableHapticFeedback; }
    public boolean isSoundEnabled() { return enableSound; }
    public boolean isDismissible() { return dismissible; }
    public int getCustomDurationMs() { return customDurationMs; }
    public int getCustomBackgroundColor() { return customBackgroundColor; }
    public int getCustomTextColor() { return customTextColor; }
    public int getCustomIcon() { return customIcon; }

    // Preset configurations
    public static ToastConfig success() {
        return new Builder()
                .setType(ToastType.SUCCESS)
                .setDuration(ToastDuration.MEDIUM)
                .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                .build();
    }

    public static ToastConfig error() {
        return new Builder()
                .setType(ToastType.ERROR)
                .setDuration(ToastDuration.LONG)
                .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                .build();
    }

    public static ToastConfig warning() {
        return new Builder()
                .setType(ToastType.WARNING)
                .setDuration(ToastDuration.MEDIUM)
                .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
                .build();
    }

    public static ToastConfig info() {
        return new Builder()
                .setType(ToastType.INFO)
                .setDuration(ToastDuration.SHORT)
                .setAnimation(ToastAnimation.FADE_IN_OUT)
                .build();
    }

    public static ToastConfig loading() {
        return new Builder()
                .setType(ToastType.LOADING)
                .setDuration(ToastDuration.VERY_LONG)
                .setAnimation(ToastAnimation.SCALE_IN_OUT)
                .setDismissible(false)
                .build();
    }
}
