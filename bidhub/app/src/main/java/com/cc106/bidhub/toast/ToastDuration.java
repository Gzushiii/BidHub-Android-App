package com.cc106.bidhub.toast;

/**
 * Enumeration of toast notification durations
 */
public enum ToastDuration {
    SHORT(2000),      // 2 seconds
    MEDIUM(3500),     // 3.5 seconds
    LONG(5000),       // 5 seconds
    VERY_LONG(7000),  // 7 seconds
    CUSTOM(0);        // Custom duration

    private final int durationMs;

    ToastDuration(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public static ToastDuration fromMilliseconds(int ms) {
        for (ToastDuration duration : values()) {
            if (duration.durationMs == ms) {
                return duration;
            }
        }
        return CUSTOM;
    }
}
