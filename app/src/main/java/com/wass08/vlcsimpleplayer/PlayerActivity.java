package com.wass08.vlcsimpleplayer;

import com.wass08.vlcsimpleplayer.util.Callback;
import com.wass08.vlcsimpleplayer.util.SystemUiHider;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Toast;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class PlayerActivity extends Activity {

    private static final String TAG = "ReactNativeJS";

    public static final String EXTRA_URL = "PlayerActivity.EXTRA_URL";
    public static final String EXTRA_TITLE = "PlayerActivity.EXTRA_TITLE";
    public static final String EXTRA_HIDE_SEEK_BAR = "PlayerActivity.EXTRA_HIDE_SEEK_BAR";

    public static final String INTENT_FILTER_NAME = "com.ewnd9.mediacenter.USER_ACTION";
    public static final String INTENT_EXTRA_TEXT = "com.ewnd9.mediacenter.USER_ACTION.text";

    private LinearLayout vlcContainer;
    private PlayerView playerView;

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra(INTENT_EXTRA_TEXT);
            playerView.setSubtitlesText(text);
        }
    };

    private IntentFilter intentFilter = new IntentFilter(INTENT_FILTER_NAME);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();
        setContentView(R.layout.activity_player);

        Bundle b = getIntent().getExtras();

        String urlToStream = b.getString(EXTRA_URL, null);
        String title = b.getString(EXTRA_TITLE, null);
        boolean hideSeekBar = b.getBoolean(EXTRA_HIDE_SEEK_BAR, false);

        vlcContainer = (LinearLayout) findViewById(R.id.vlc_container);

        // AUTOSTART
        playerView = (PlayerView) findViewById(R.id.player_view);
        playerView.init(urlToStream, hideSeekBar, title, new Callback<Boolean>() {
            @Override
            public void call(Boolean fullscreen) {
                WindowManager.LayoutParams attrs = getWindow().getAttributes();

                if (fullscreen) {
                    attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;

                    vlcContainer.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    );
                } else {
                    attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
                }

                getWindow().setAttributes(attrs);
            }
        });

        if (!playerView.playMovie()) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(myReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(myReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.playerView.releasePlayer();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        this.playerView.recalcSize();
    }
}
