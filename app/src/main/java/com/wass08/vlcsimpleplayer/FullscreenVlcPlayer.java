package com.wass08.vlcsimpleplayer;

import com.wass08.vlcsimpleplayer.translator.TranslationArrayAdapter;
import com.wass08.vlcsimpleplayer.translator.Translator;
import com.wass08.vlcsimpleplayer.util.Callback;
import com.wass08.vlcsimpleplayer.util.SystemUiHider;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;

import java.lang.ref.WeakReference;


/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 *
 * @see SystemUiHider
 */
public class FullscreenVlcPlayer extends Activity {

    private static final String TAG = "ReactNativeJS";

    public static final String EXTRA_URL = "FullscreenVlcPlayer.EXTRA_URL";
    public static final String EXTRA_HIDE_SEEK_BAR = "FullscreenVlcPlayer.EXTRA_HIDE_SEEK_BAR";

    private LinearLayout vlcContainer;
    private PlayerView playerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();
        setContentView(R.layout.activity_fullscreen_vlc_player);

        Bundle b = getIntent().getExtras();

        String urlToStream = b.getString(EXTRA_URL, null);
        boolean hideSeekBar = b.getBoolean(EXTRA_HIDE_SEEK_BAR, false);

        vlcContainer = (LinearLayout) findViewById(R.id.vlc_container);

        // AUTOSTART
        playerView = (PlayerView) findViewById(R.id.player_view);
        playerView.init(urlToStream, hideSeekBar, "URL TO STREAM", new Callback<Boolean>() {
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

        vlcContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playerView.onClick();
            }
        });

        if (!playerView.playMovie()) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        //releasePlayer();
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
