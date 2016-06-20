package com.wass08.vlcsimpleplayer;

import android.app.Activity;
import android.util.Log;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.views.image.ReactImageView;

/**
 * Created by ewnd9 on 20.06.16.
 */
public class ReactPlayerManager extends SimpleViewManager<PlayerView> {

    public static final String REACT_CLASS = "RCTPlayerView";

    Activity activity;

    public ReactPlayerManager(Activity activity) {
        this.activity = activity;
    }

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    protected PlayerView createViewInstance(ThemedReactContext reactContext) {
        return new PlayerView(reactContext, activity);
    }

    @ReactProp(name = "src")
    public void setSrc(PlayerView view, String src) {
        Log.v("ReactNativeJS", "here " + src);
        view.setUrlToStreamAndPlay(src);
    }
}