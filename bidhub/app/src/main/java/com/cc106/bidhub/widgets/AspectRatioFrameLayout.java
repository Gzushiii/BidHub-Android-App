package com.cc106.bidhub.widgets;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * FrameLayout that enforces a 1:1 aspect ratio
 * Used for item card images to maintain square dimensions
 */
public class AspectRatioFrameLayout extends FrameLayout {
    
    public AspectRatioFrameLayout(Context context) {
        super(context);
    }
    
    public AspectRatioFrameLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    
    public AspectRatioFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Get width
        int width = MeasureSpec.getSize(widthMeasureSpec);
        
        // Calculate height to maintain 1:1 aspect ratio
        int height = width; // 1:1 ratio
        
        // Create new measure specs
        int newWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        int newHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        
        // Measure children with square dimensions
        super.onMeasure(newWidthMeasureSpec, newHeightMeasureSpec);
    }
}


