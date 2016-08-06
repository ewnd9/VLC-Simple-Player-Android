package com.wass08.vlcsimpleplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class VLCPlayer extends ReactContextBaseJavaModule {

    private static ReactApplicationContext context;

    public VLCPlayer(ReactApplicationContext reactContext) {
        super(reactContext);
        VLCPlayer.context = reactContext;
    }

    @Override
    public String getName() {
        return "VLCPlayer";
    }

    @ReactMethod
    public void play(String path, String title) {
        Intent toFullscreen = new Intent(VLCPlayer.context, FullscreenVlcPlayer.class);
        Bundle b = new Bundle();

        b.putString(FullscreenVlcPlayer.EXTRA_URL, path);
        b.putString(FullscreenVlcPlayer.EXTRA_TITLE, title);
        b.putBoolean(FullscreenVlcPlayer.EXTRA_HIDE_SEEK_BAR, true);

        toFullscreen.putExtras(b); //Put your id to your next Intent
        toFullscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        VLCPlayer.context.startActivity(toFullscreen);
    }

    @ReactMethod
    public void setSubtitles(String text) {
        Intent intent = new Intent(FullscreenVlcPlayer.INTENT_FILTER_NAME);
        intent.putExtra(FullscreenVlcPlayer.INTENT_EXTRA_TEXT, text);

        context.sendBroadcast(intent);
    }

    public static void sendPositionToReact(long position, long duration) {
        WritableMap params = Arguments.createMap();
        params.putString("position", Long.toString(position * 1000)); // nanoseconds
        params.putString("duration", Long.toString(duration * 1000)); // nanoseconds

        VLCPlayer.context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("positionUpdate", params);
    }

    public static void sendMarkToReact(long position, long duration) {
        WritableMap params = Arguments.createMap();
        params.putString("position", Long.toString(position * 1000));
        params.putString("duration", Long.toString(duration * 1000));

        VLCPlayer.context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("markEmit", params);
    }
}
