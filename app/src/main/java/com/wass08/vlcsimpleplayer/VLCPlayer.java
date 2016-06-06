package com.wass08.vlcsimpleplayer;

import android.content.Intent;
import android.os.Bundle;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;


public class VLCPlayer extends ReactContextBaseJavaModule {

    private ReactApplicationContext context;

    public VLCPlayer(ReactApplicationContext reactContext) {
        super(reactContext);
        this.context = reactContext;
    }

    @Override
    public String getName() {
        return "VLCPlayer";
    }

    @ReactMethod
    public void play(String path) {
        Intent toFullscreen = new Intent(context, FullscreenVlcPlayer.class);
        Bundle b = new Bundle();

        // Pass the url from the input to the player
        b.putString("url", path);

        toFullscreen.putExtras(b); //Put your id to your next Intent
        toFullscreen.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        this.context.startActivity(toFullscreen);
    }

}
