package com.wass08.vlcsimpleplayer;

import com.wass08.vlcsimpleplayer.translator.TranslationArrayAdapter;
import com.wass08.vlcsimpleplayer.translator.Translator;
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
public class FullscreenVlcPlayer extends Activity implements SurfaceHolder.Callback, IVideoPlayer {

    private static final String TAG = "ReactNativeJS";

    public static final String EXTRA_URL = "FullscreenVlcPlayer.EXTRA_URL";
    public static final String EXTRA_HIDE_SEEK_BAR = "FullscreenVlcPlayer.EXTRA_HIDE_SEEK_BAR";

    private String              urlToStream;

    // Display Surface
    private LinearLayout        vlcContainer;
    private FrameLayout         vlcSurfaceContainer;
    private SurfaceView         mSurface;
    private SurfaceHolder       holder;

    // Editor
    // @TODO implement as a fragment, so it can be swapped to others like comments, description, etc...
    private LinearLayout        editorContainer;
    private EditText            editorInput;
    private ListView            editorListView;
    private TranslationArrayAdapter editorAdapter;
    private Translator          translator;

    // Overlay / Controls

    private FrameLayout         vlcOverlay;
    private ImageView           vlcButtonPlayPause;
    private Handler             handlerOverlay;
    private Runnable            runnableOverlay;
    private Handler             handlerSeekbar;
    private Runnable            runnableSeekbar;
    private SeekBar             vlcSeekbar;
    private TextView            vlcDuration;
    private TextView            overlayTitle;
    private View                vlcSeekBarLayout;

    // media player
    private LibVLC              libvlc;
    private int                 mVideoWidth;
    private int                 mVideoHeight;
    private final static int    VideoSizeChanged = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve our url
        Bundle b = getIntent().getExtras();
        urlToStream = b.getString(EXTRA_URL, null);
        boolean hideSeekBar = b.getBoolean(EXTRA_HIDE_SEEK_BAR, false);

        // HIDE THE ACTION BAR
        getActionBar().hide();

        // SETUP THE UI
        setContentView(R.layout.activity_fullscreen_vlc_player);

        // VLC
        vlcContainer = (LinearLayout) findViewById(R.id.vlc_container);
        vlcSurfaceContainer = (FrameLayout) findViewById(R.id.vlc_surface_container);
        mSurface = (SurfaceView) findViewById(R.id.vlc_surface);

        // editor
        editorContainer = (LinearLayout) findViewById(R.id.editor_container);
        editorInput = (EditText) findViewById(R.id.editor_input);

        editorAdapter = new TranslationArrayAdapter(this);

        editorListView = (ListView) findViewById(R.id.editor_list_view);
        editorListView.setAdapter(editorAdapter);

        translator = new Translator(editorInput, editorAdapter);

        // OVERLAY / CONTROLS
        vlcOverlay = (FrameLayout) findViewById(R.id.vlc_overlay);
        vlcButtonPlayPause = (ImageView) findViewById(R.id.vlc_button_play_pause);
        vlcSeekbar = (SeekBar) findViewById(R.id.vlc_seekbar);
        vlcDuration = (TextView) findViewById(R.id.vlc_duration);

        vlcSeekBarLayout = findViewById(R.id.vlc_seek_bar_layout);
        if (hideSeekBar) {
            vlcSeekBarLayout.setVisibility(View.INVISIBLE);
        }

        overlayTitle = (TextView) findViewById(R.id.vlc_overlay_title);
        overlayTitle.setText(urlToStream);

        // AUTOSTART
        playMovie();
    }

    private void showOverlay() {
        vlcOverlay.setVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        vlcOverlay.setVisibility(View.GONE);
    }

    private void setupControls() {
        getActionBar().hide();
        // PLAY PAUSE
        vlcButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (libvlc.isPlaying()) {
                    libvlc.pause();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_play_over_video));
                } else {
                    libvlc.play();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_action_pause_over_video));
                }
            }
        });

        // SEEKBAR
        handlerSeekbar = new Handler();
        runnableSeekbar = new Runnable() {
            @Override
            public void run() {
                if (libvlc != null) {
                    long curTime = libvlc.getTime();
                    long totalTime = (long) (curTime / libvlc.getPosition());
                    int minutes = (int) (curTime / (60 * 1000));
                    int seconds = (int) ((curTime / 1000) % 60);
                    int endMinutes = (int) (totalTime / (60 * 1000));
                    int endSeconds = (int) ((totalTime / 1000) % 60);
                    String duration = String.format("%02d:%02d / %02d:%02d", minutes, seconds, endMinutes, endSeconds);
                    vlcSeekbar.setProgress((int) (libvlc.getPosition() * 100));
                    vlcDuration.setText(duration);
                }
                handlerSeekbar.postDelayed(runnableSeekbar, 1000);
            }
        };

        runnableSeekbar.run();
        vlcSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.v("NEW POS", "pos is : " + i);
                //if (i != 0)
                //    libvlc.setPosition(((float) i / 100.0f));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        // OVERLAY
        handlerOverlay = new Handler();
        runnableOverlay = new Runnable() {
            @Override
            public void run() {
                vlcOverlay.setVisibility(View.GONE);
                toggleFullscreen(true);
            }
        };
        final long timeToDisappear = 3000;
        handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
        vlcContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vlcOverlay.setVisibility(View.VISIBLE);

                handlerOverlay.removeCallbacks(runnableOverlay);
                handlerOverlay.postDelayed(runnableOverlay, timeToDisappear);
            }
        });
    }

    public void playMovie() {
        if (libvlc != null && libvlc.isPlaying())
            return ;
        vlcContainer.setVisibility(View.VISIBLE);
        holder = mSurface.getHolder();
        holder.addCallback(this);
        createPlayer(urlToStream);
    }


    private void toggleFullscreen(boolean fullscreen)
    {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        if (fullscreen)
        {
            attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
            vlcContainer.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
        else
        {
            attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        }
        getWindow().setAttributes(attrs);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        processSizeChange(mVideoWidth, mVideoHeight);

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
        releasePlayer();
    }

    /**
     * **********
     * Surface
     * ***********
     */

    public void surfaceCreated(SurfaceHolder holder) {
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int format,
                               int width, int height) {
        if (libvlc != null)
            libvlc.attachSurface(surfaceholder.getSurface(), this);
    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
    }

    private boolean isPortrait() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private void processSizeChange(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;
        if (mVideoWidth * mVideoHeight <= 1)
            return;

        // get screen size
        int w = getWindow().getDecorView().getWidth();
        int h = getWindow().getDecorView().getHeight();

        // getWindow().getDecorView() doesn't always take orientation into
        // account, we have to correct the values
        boolean isPortrait = isPortrait();
        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR)
            h = (int) (w / videoAR);
        else
            w = (int) (h * videoAR);

        // force surface buffer size
        if (holder != null)
            holder.setFixedSize(mVideoWidth, mVideoHeight);

        // set display size
        ViewGroup.LayoutParams lp1 = mSurface.getLayoutParams();
        lp1.width = w;
        lp1.height = h;
        mSurface.setLayoutParams(lp1);
        mSurface.invalidate();

        ViewGroup.LayoutParams lp2 = vlcSurfaceContainer.getLayoutParams();
        int editorContainerVisibility;

        if (isPortrait) {
            lp2.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp2.height = mVideoHeight;
            editorContainerVisibility = View.VISIBLE;
        } else {
            lp2.width = ViewGroup.LayoutParams.MATCH_PARENT;
            lp2.height = ViewGroup.LayoutParams.MATCH_PARENT;
            editorContainerVisibility = View.GONE;
        }

        vlcSurfaceContainer.setLayoutParams(lp2);
        editorContainer.setVisibility(editorContainerVisibility);
    }

    @Override
    public void setSurfaceSize(int width, int height, int visible_width,
                               int visible_height, int sar_num, int sar_den) {
        Message msg = Message.obtain(mHandler, VideoSizeChanged, width, height);
        msg.sendToTarget();
    }

    /**
     * **********
     * Player
     * ***********
     */

    private void createPlayer(String media) {
        releasePlayer();
        setupControls();
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(this, media, Toast.LENGTH_LONG);
                toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0,
                        0);
                toast.show();
            }

            // Create a new media player
            libvlc = LibVLC.getInstance();
            libvlc.setHardwareAcceleration(LibVLC.HW_ACCELERATION_FULL);
            libvlc.eventVideoPlayerActivityCreated(true);
            libvlc.setSubtitlesEncoding("");
            libvlc.setAout(LibVLC.AOUT_OPENSLES);
            libvlc.setTimeStretching(true);
            libvlc.setChroma("RV32");
            libvlc.setVerboseMode(true);
            LibVLC.restart(this);
            EventHandler.getInstance().addHandler(mHandler);
            holder.setFormat(PixelFormat.RGBX_8888);
            holder.setKeepScreenOn(true);
            MediaList list = libvlc.getMediaList();
            list.clear();
            list.add(new Media(libvlc, LibVLC.PathToURI(media)), false);
            libvlc.playIndex(0);

            mPositionHandler.postDelayed(mPositionHandlerTask, 1000 * 5); // 5 seconds
        } catch (Exception e) {
            Toast.makeText(this, "Could not create Vlc Player", Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        if (handlerSeekbar != null && runnableSeekbar != null)
            handlerSeekbar.removeCallbacks(runnableSeekbar);
        EventHandler.getInstance().removeHandler(mHandler);
        if (libvlc == null)
            return;
        libvlc.stop();
        libvlc.detachSurface();
        holder = null;
        libvlc.closeAout();

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    /**
     * **********
     * Events
     * ***********
     */

    private Handler mPositionHandler = new Handler();
    private static int POSITION_UPDATE_INTERVAL = 1000 * 60; // 1 min

    private Runnable mPositionHandlerTask = new Runnable() {
        @Override
        public void run() {
            if (holder != null) {
                VLCPlayer.sendPositionToReact(libvlc.getPosition());
                mPositionHandler.postDelayed(mPositionHandlerTask, POSITION_UPDATE_INTERVAL);
            }
        }
    };

    private Handler mHandler = new MyHandler(this);

    private static class MyHandler extends Handler {
        private WeakReference<FullscreenVlcPlayer> mOwner;

        public MyHandler(FullscreenVlcPlayer owner) {
            mOwner = new WeakReference<FullscreenVlcPlayer>(owner);
        }

        @Override
        public void handleMessage(Message msg) {
            FullscreenVlcPlayer player = mOwner.get();

            // Player events
            if (msg.what == VideoSizeChanged) {
                player.processSizeChange(msg.arg1, msg.arg2);
                return;
            }

            // Libvlc events
            Bundle b = msg.getData();
            switch (b.getInt("event")) {
                case EventHandler.MediaPlayerEndReached:
                    player.releasePlayer();
                    break;
                case EventHandler.MediaPlayerPlaying:
                case EventHandler.MediaPlayerPaused:
                case EventHandler.MediaPlayerStopped:
                default:
                    break;
            }
        }
    }

}
