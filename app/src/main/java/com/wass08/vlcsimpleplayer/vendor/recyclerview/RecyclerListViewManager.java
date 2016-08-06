package com.wass08.vlcsimpleplayer.vendor.recyclerview;

/**
 * Created by ewnd9 on 19.07.16.
 */
// Copyright 2004-present Facebook. All Rights Reserved.


import javax.annotation.Nullable;

import java.util.Map;

import android.util.Log;
import android.view.View;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.ViewGroupManager;
import com.facebook.react.views.recyclerview.RecyclerViewBackedScrollView;
import com.facebook.react.views.scroll.ReactScrollViewCommandHelper;
import com.facebook.react.views.scroll.ScrollEventType;

/**
 * View manager for {@link RecyclerViewBackedScrollView}.
 */
public class RecyclerListViewManager extends
        ViewGroupManager<RecyclerListView>
        implements ReactScrollViewCommandHelper.ScrollCommandHandler<RecyclerListView> {

    private static final String REACT_CLASS = "RecyclerListView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    // TODO(8624925): Implement removeClippedSubviews support for native MyListView

    @ReactProp(name = "onContentSizeChange")
    public void setOnContentSizeChange(RecyclerListView view, boolean value) {
        view.setSendContentSizeChangeEvents(value);
    }

    @Override
    protected RecyclerListView createViewInstance(ThemedReactContext reactContext) {
        return new RecyclerListView(reactContext);
    }

    @Override
    public void addView(RecyclerListView parent, View child, int index) {
        parent.addViewToAdapter(child, index);
    }

    @Override
    public int getChildCount(RecyclerListView parent) {
        return parent.getChildCountFromAdapter();
    }

    @Override
    public View getChildAt(RecyclerListView parent, int index) {
        return parent.getChildAtFromAdapter(index);
    }

    @Override
    public void removeViewAt(RecyclerListView parent, int index) {
        parent.removeViewFromAdapter(index);
    }

    @Override
    public void receiveCommand(
            RecyclerListView view,
            int commandId,
            @Nullable ReadableArray args) {
        ReactScrollViewCommandHelper.receiveCommand(this, view, commandId, args);
    }

    @Override
    public void scrollTo(
            RecyclerListView scrollView,
            ReactScrollViewCommandHelper.ScrollToCommandData data) {
        scrollView.scrollTo(data.mDestX, data.mDestY, data.mAnimated);
    }

    @ReactProp(name = "initialPosition")
    public void setInitialPosition(RecyclerListView view, int position) {
        view.scrollToPositionDelayed(position);
        Log.v("ReactNativeJS", "initialPosition " + position);

    }

    @Override
    public
    @Nullable
    Map getExportedCustomDirectEventTypeConstants() {
        return MapBuilder.builder()
                .put(ScrollEventType.SCROLL.getJSEventName(), MapBuilder.of("registrationName", "onScroll"))
                .build();
    }
}