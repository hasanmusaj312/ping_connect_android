package com.cloudtenlabs.ping.activity;

import android.graphics.Canvas;
import android.graphics.Point;
import android.view.View;
import android.view.View.DragShadowBuilder;

public class MyDragShadowBuilder extends DragShadowBuilder {

    MyDragShadowBuilder(View view) {
        super(view);
    }

    @Override
    public void onProvideShadowMetrics(Point shadowSize, Point shadowTouchPoint) {
        // get width, height
        int width = getView().getWidth();
        int height = getView().getHeight();

        // set shadow
        shadowSize.set(width, height);
        shadowTouchPoint.set(width/2, height/2);
    }

    @Override
    public void onDrawShadow(Canvas canvas) {
        super.onDrawShadow(canvas);
    }
}