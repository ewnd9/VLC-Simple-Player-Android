package com.wass08.vlcsimpleplayer.reader;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.infer.annotation.Assertions;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

/**
 * Created by ewnd9 on 19.07.16.
 */
public class ReactReaderListView extends SimpleViewManager<ReaderListView> {

    public static final String REACT_CLASS = "RCTReaderListView";
    public static final int COMMAND_SET_FULL_ITEM = 1;

    Activity activity;

    public ReactReaderListView(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected ReaderListView createViewInstance(ThemedReactContext reactContext) {
        return new ReaderListView(reactContext, activity);
    }
//
//    @ReactProp(name = "src")
//    public void items(ReaderListView view, String src) {
//        Log.v("ReactNativeJS", "here " + src);
////        view.setUrlToStreamAndPlay(src);
//    }

    @ReactProp(name = "items")
    public void setItems(ReaderListView view, ReadableArray items) {
        Log.v("ReactNativeJS", items.size() + " ");
        view.setItems(items);
    }

    @ReactProp(name = "initialPosition")
    public void setInitialPosition(ReaderListView view, int initialPosition) {
        Log.v("ReactNativeJS", " " + initialPosition);
        view.scrollToPosition(initialPosition);
    }

    @Override
    public Map<String,Integer> getCommandsMap() {
        Log.d("React"," View manager getCommandsMap:");
        return MapBuilder.of(
                "setFullItem",
                COMMAND_SET_FULL_ITEM);
    }

    @Override
    public void receiveCommand(
            ReaderListView view,
            int commandType,
            @Nullable ReadableArray args) {

        Assertions.assertNotNull(view);
        Assertions.assertNotNull(args);

        switch (commandType) {
            case COMMAND_SET_FULL_ITEM: {
                view.setFullItem(args.getInt(0), args.getMap(1));
                return;
            }

            default:
                throw new IllegalArgumentException(String.format(
                        "Unsupported command %d received by %s.",
                        commandType,
                        getClass().getSimpleName()));
        }
    }
}
