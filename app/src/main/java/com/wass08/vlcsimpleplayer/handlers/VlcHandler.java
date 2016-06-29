package com.wass08.vlcsimpleplayer.handlers;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.wass08.vlcsimpleplayer.PlayerView;

import org.videolan.libvlc.EventHandler;

import java.lang.ref.WeakReference;

/**
 * Created by ewnd9 on 27.06.16.
 */
public class VlcHandler extends Handler {

    private static final String TAG = "ReactNative";

    private WeakReference<PlayerView> mOwner;

    public VlcHandler(PlayerView owner) {
        mOwner = new WeakReference<>(owner);
    }

    @Override
    public void handleMessage(Message msg) {
        PlayerView playerView = mOwner.get();

        // Player events
        if (msg.what == PlayerView.VIDEO_SIZE_CHANGED) {
            playerView.setSize(msg.arg1, msg.arg2);
            return;
        }

        // Libvlc events
        Bundle b = msg.getData();

        switch (b.getInt("event")) {
            case EventHandler.MediaPlayerEndReached:
                playerView.releasePlayer();
                break;
            case EventHandler.MediaPlayerPlaying:
                Log.v(TAG, "MediaPlayerPlaying");
                break;
            case EventHandler.MediaPlayerPaused:
            case EventHandler.MediaPlayerStopped:
            case EventHandler.MediaPlayerBuffering:
                Log.v(TAG, "MediaPlayerBuffering");
            default:
                break;
        }
    }
}