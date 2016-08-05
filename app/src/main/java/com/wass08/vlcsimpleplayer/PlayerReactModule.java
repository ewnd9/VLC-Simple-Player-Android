package com.wass08.vlcsimpleplayer;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;


public class PlayerReactModule extends ReactContextBaseJavaModule {

    private static ReactApplicationContext context;

    public PlayerReactModule(ReactApplicationContext reactContext) {
        super(reactContext);
        PlayerReactModule.context = reactContext;
    }

    @Override
    public String getName() {
        return "VLCPlayer";
    }

    @ReactMethod
    public void play(String path, String title) {
        Intent toFullscreen = new Intent(PlayerReactModule.context, PlayerActivity.class);
        Bundle b = new Bundle();

        b.putString(PlayerActivity.EXTRA_URL, path);
        b.putString(PlayerActivity.EXTRA_TITLE, title);
        b.putBoolean(PlayerActivity.EXTRA_HIDE_SEEK_BAR, true);

        toFullscreen.putExtras(b); //Put your id to your next Intent
        toFullscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PlayerReactModule.context.startActivity(toFullscreen);
    }

    @ReactMethod
    public void setSubtitles(String text) {
        Intent intent = new Intent(PlayerActivity.INTENT_FILTER_NAME);
        intent.putExtra(PlayerActivity.INTENT_EXTRA_TEXT, text);

        context.sendBroadcast(intent);
    }

    public static void sendPositionToReact(long position, long duration) {
        WritableMap params = Arguments.createMap();
        params.putString("position", Long.toString(position * 1000)); // nanoseconds
        params.putString("duration", Long.toString(duration * 1000)); // nanoseconds

        PlayerReactModule.context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("positionUpdate", params);
    }

    public static void sendMarkToReact(long position, long duration) {
        WritableMap params = Arguments.createMap();
        params.putString("position", Long.toString(position * 1000));
        params.putString("duration", Long.toString(duration * 1000));

        PlayerReactModule.context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("markEmit", params);
    }
}
