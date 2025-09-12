# Advanced Toast Notification System

A comprehensive, modern toast notification system for Android with queue management, animations, and extensive customization options.

## Features

- **Queue Management**: Automatic queuing of multiple toasts
- **Multiple Types**: Success, Error, Warning, Info, Loading, and Custom
- **Customizable Duration**: Short, Medium, Long, Very Long, or Custom
- **Flexible Positioning**: Top, Bottom, Center, and corner positions
- **Smooth Animations**: Slide, Fade, Scale, Bounce, and more
- **Haptic Feedback**: Optional vibration on toast display
- **Custom Styling**: Custom colors, icons, and backgrounds
- **Easy Integration**: Simple API with convenience methods

## Quick Start

### Basic Usage

```java
// Simple message
ToastHelper.show(context, "Hello World!");

// Success message
ToastHelper.showSuccess(context, "Operation completed!");

// Error message
ToastHelper.showError(context, "Something went wrong!");

// Warning message
ToastHelper.showWarning(context, "Please check your input");

// Info message
ToastHelper.showInfo(context, "This is informational");

// Loading message
ToastHelper.showLoading(context, "Please wait...");
```

### Custom Configuration

```java
// Custom duration and position
ToastConfig config = new ToastConfig.Builder()
    .setType(ToastType.SUCCESS)
    .setDuration(ToastDuration.LONG)
    .setPosition(ToastPosition.TOP)
    .setAnimation(ToastAnimation.SLIDE_FROM_TOP)
    .setHapticFeedback(true)
    .build();

ToastHelper.show(context, "Custom message", config);
```

### Advanced Usage

```java
// Get manager instance for advanced control
ToastManager manager = ToastManager.getInstance(context);

// Show multiple toasts (they will queue automatically)
manager.showSuccess("First message");
manager.showInfo("Second message");
manager.showWarning("Third message");

// Dismiss current toast
manager.dismiss();

// Clear all pending toasts
manager.clearQueue();
```

## Toast Types

| Type | Description | Default Color | Icon |
|------|-------------|---------------|------|
| SUCCESS | Success messages | Green | Check circle |
| ERROR | Error messages | Red | Error |
| WARNING | Warning messages | Yellow | Warning |
| INFO | Informational messages | Blue | Info |
| LOADING | Loading messages | Blue | Loading spinner |
| CUSTOM | Custom styling | Configurable | Configurable |

## Toast Durations

| Duration | Time | Use Case |
|----------|------|----------|
| SHORT | 2 seconds | Quick confirmations |
| MEDIUM | 3.5 seconds | Standard messages |
| LONG | 5 seconds | Important messages |
| VERY_LONG | 7 seconds | Critical messages |
| CUSTOM | Configurable | Specific needs |

## Toast Positions

| Position | Description |
|----------|-------------|
| TOP | Top center |
| TOP_LEFT | Top left corner |
| TOP_RIGHT | Top right corner |
| CENTER | Screen center |
| BOTTOM | Bottom center |
| BOTTOM_LEFT | Bottom left corner |
| BOTTOM_RIGHT | Bottom right corner |

## Toast Animations

| Animation | Description |
|-----------|-------------|
| FADE_IN_OUT | Smooth fade transition |
| SLIDE_FROM_TOP | Slide down from top |
| SLIDE_FROM_BOTTOM | Slide up from bottom |
| SLIDE_FROM_LEFT | Slide in from left |
| SLIDE_FROM_RIGHT | Slide in from right |
| SCALE_IN_OUT | Scale up/down effect |
| BOUNCE_IN_OUT | Bouncy scale effect |
| NONE | No animation |

## Configuration Options

### ToastConfig Builder

```java
ToastConfig config = new ToastConfig.Builder()
    .setType(ToastType.SUCCESS)           // Toast type
    .setDuration(ToastDuration.MEDIUM)     // Display duration
    .setPosition(ToastPosition.TOP)        // Screen position
    .setAnimation(ToastAnimation.SLIDE_FROM_TOP) // Animation
    .setShowIcon(true)                     // Show/hide icon
    .setHapticFeedback(true)               // Enable vibration
    .setSound(false)                       // Enable sound (future)
    .setDismissible(true)                  // Allow tap to dismiss
    .setCustomDuration(3000)               // Custom duration in ms
    .setCustomColors(backgroundColor, textColor) // Custom colors
    .setCustomIcon(iconResource)           // Custom icon
    .build();
```

### Preset Configurations

```java
// Use predefined configurations
ToastConfig.success()    // Success with medium duration
ToastConfig.error()      // Error with long duration
ToastConfig.warning()    // Warning with medium duration
ToastConfig.info()       // Info with short duration
ToastConfig.loading()    // Loading with very long duration
```

## Migration from Old System

### Before (Old Toast)
```java
Toast.makeText(context, "Message", Toast.LENGTH_SHORT).show();
```

### After (New Toast System)
```java
ToastHelper.show(context, "Message");
// or
ToastHelper.showInfo(context, "Message");
```

### UIUtils.showStyledToast() Migration
```java
// Old
UIUtils.showStyledToast(context, "Message", true);  // Error
UIUtils.showStyledToast(context, "Message", false); // Success

// New
ToastHelper.showError(context, "Message");
ToastHelper.showSuccess(context, "Message");
```

## Best Practices

1. **Use appropriate types**: Choose the right toast type for your message
2. **Keep messages concise**: Short, clear messages work best
3. **Use queue management**: Let the system handle multiple toasts
4. **Consider positioning**: Use TOP position for important messages
5. **Test animations**: Ensure animations work well on your target devices
6. **Handle context**: Always check for null context before showing toasts

## Thread Safety

The ToastManager is thread-safe and can be used from any thread. All UI operations are automatically dispatched to the main thread.

## Performance

- Lightweight implementation with minimal memory footprint
- Efficient queue management
- Automatic cleanup of completed toasts
- Optimized animations using native Android animations

## Customization

### Custom Colors
```java
ToastConfig config = new ToastConfig.Builder()
    .setCustomColors(
        ContextCompat.getColor(context, R.color.custom_background),
        ContextCompat.getColor(context, R.color.custom_text)
    )
    .build();
```

### Custom Icons
```java
ToastConfig config = new ToastConfig.Builder()
    .setCustomIcon(R.drawable.custom_icon)
    .build();
```

### Custom Animations
```java
ToastConfig config = new ToastConfig.Builder()
    .setAnimation(ToastAnimation.SCALE_IN_OUT)
    .build();
```

## Troubleshooting

### Common Issues

1. **Toast not showing**: Check if context is valid and not null
2. **Multiple toasts overlapping**: Use queue management or dismiss previous toasts
3. **Animation not working**: Ensure animation resources are properly defined
4. **Custom colors not applied**: Check if color resources exist and are accessible

### Debug Mode

Enable debug logging by adding this to your Application class:
```java
// Add this for debugging toast issues
if (BuildConfig.DEBUG) {
    // ToastManager will log debug information
}
```

## Future Enhancements

- Sound notifications
- Toast persistence across app restarts
- Advanced animation effects
- Toast templates
- Accessibility improvements
- Custom toast layouts
