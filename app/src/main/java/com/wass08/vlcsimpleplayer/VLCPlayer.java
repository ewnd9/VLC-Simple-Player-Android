package com.wass08.vlcsimpleplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
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
    public void play(String path) {
        Intent toFullscreen = new Intent(VLCPlayer.context, FullscreenVlcPlayer.class);
        Bundle b = new Bundle();

        b.putString(FullscreenVlcPlayer.EXTRA_URL, path);
        b.putBoolean(FullscreenVlcPlayer.EXTRA_HIDE_SEEK_BAR, true);

        toFullscreen.putExtras(b); //Put your id to your next Intent
        toFullscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        VLCPlayer.context.startActivity(toFullscreen);
    }

    public static void sendPositionToReact(float position) {
        WritableMap params = Arguments.createMap();
        params.putDouble("position", position);

        VLCPlayer.context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("positionUpdate", params);
    }
}
