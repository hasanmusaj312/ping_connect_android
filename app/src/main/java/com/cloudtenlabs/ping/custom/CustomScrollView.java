package com.cloudtenlabs.ping.custom;

import android.widget.ScrollView;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class CustomScrollView extends ScrollView {

    private boolean scrollable;

    public CustomScrollView(Context context, boolean scrollable) {
        super(context);
        this.scrollable = scrollable;
    }

    public CustomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!scrollable) return false;
        return super.onInterceptTouchEvent(ev);
    }

}