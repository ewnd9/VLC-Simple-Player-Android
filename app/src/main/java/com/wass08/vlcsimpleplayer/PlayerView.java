package com.wass08.vlcsimpleplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.react.bridge.ReactContext;
import com.wass08.vlcsimpleplayer.handlers.VlcHandler;
import com.wass08.vlcsimpleplayer.util.Callback;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;

/**
 * Created by ewnd9 on 20.06.16.
 */
public class PlayerView extends FrameLayout implements SurfaceHolder.Callback, IVideoPlayer {

    private static final String TAG = "ReactNativeJS";

    public final static int VIDEO_SIZE_CHANGED = -1;
    private static final long TIME_TO_DISAPPEAR = 3000;

    // Display Surface
    private SurfaceView mSurface;
    private SurfaceHolder holder;

    private ViewGroup overlayLayout;
    private ViewGroup overlayControlLayout;
    private ImageView vlcButtonPlayPause;
    private ImageView vlcMarkButton;
    private Handler handlerOverlay;
    private Runnable runnableOverlay;

    private TextView overlayTitle;
    private TextView subtitlesTextView;
    private TextView onTouchInfoTextView;

    private View vlcSeekBarLayout;
    private Handler handlerSeekBar;
    private Runnable runnableSeekBar;
    private SeekBar vlcSeekBar;
    private TextView vlcDuration;

    private int surfaceWidth;
    private int surfaceHeight;

    // media player
    private LibVLC libvlc;
    private int mVideoWidth;
    private int mVideoHeight;

    private String urlToStream;
    private boolean hideSeekBar;
    private String title;

    private Activity activity;
    private Callback<Boolean> onToggleFullscreen;

    private boolean onTouchIsMoving = false;
    private float onTouchOffsetX = 0;
    private float onTouchOffsetY = 0;

    private float maximumPosition = 0f;
    private String currSubtitles;

    public PlayerView(Context context) {
        super(context);
        initInternals();
    }

    public PlayerView(Context context, Activity activity) {
        super(context);
        initInternals();

        this.activity = activity;

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                recalcSize();
            }
        };

        activity.registerReceiver(receiver, new IntentFilter("onConfigurationChanged"));
    }

    public PlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initInternals();
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initInternals();
    }

    private void initInternals() {
        inflate(getContext(), R.layout.view_player, this);

        // VLC
        mSurface = (SurfaceView) findViewById(R.id.vlc_surface);

        overlayLayout = (ViewGroup) findViewById(R.id.vlc_overlay);
        overlayLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    onTouchOffsetX = event.getRawX();
                    onTouchOffsetY = event.getRawY();

                    onTouchIsMoving = false;
                    return true;
                }

                Log.v(TAG, "action " + event.getAction());

                if (event.getAction() != MotionEvent.ACTION_MOVE && event.getAction() != MotionEvent.ACTION_UP) {
                    return false;
                }

                float x = event.getRawX();
                float y = event.getRawY();

                float deltaX = Math.abs(x - onTouchOffsetX);
                float deltaY = Math.abs(y - onTouchOffsetY);

                float deltaTime = deltaX / 20;
                boolean isFastForward = x > onTouchOffsetX;

                Log.v(TAG, "deltaX " + deltaX + " " + event.getAction());
                Log.v(TAG, "deltaTime " + ((int) deltaTime));

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    onTouchIsMoving = true;
                    onTouchInfoTextView.setVisibility(View.VISIBLE);

                    if (deltaX > 10) {
                        onTouchInfoTextView.setText((isFastForward ? "+" : "-") + String.format("%.0f", deltaTime));
                    }
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    onTouchInfoTextView.setVisibility(View.GONE);

                    if (deltaX < 10.0) {
                        setOverlayVisibility(View.VISIBLE);

                        handlerOverlay.removeCallbacks(runnableOverlay);
                        handlerOverlay.postDelayed(runnableOverlay, TIME_TO_DISAPPEAR);

                        return true;
                    } else {
                        long nowMs = libvlc.getTime();
                        long nextMs = nowMs + ((isFastForward ? +1 : -1) * (int) deltaTime * 1000);

                        if (!isFastForward) {
                            maximumPosition = nowMs;
                        }

                        Log.v(TAG, "now " + nowMs + " " + nextMs + " " + 1);
                        libvlc.setTime(nextMs);
                    }

                    if (onTouchIsMoving) {
                        return true;
                    }
                }

                return false;
            }
        });
        overlayControlLayout = (ViewGroup) findViewById(R.id.overlay_control_layout);

        vlcButtonPlayPause = (ImageView) findViewById(R.id.vlc_button_play_pause);
        vlcMarkButton = (ImageView) findViewById(R.id.vlc_button_mark);

        vlcSeekBar = (SeekBar) findViewById(R.id.vlc_seekbar);
        vlcDuration = (TextView) findViewById(R.id.vlc_duration);

        vlcSeekBarLayout = findViewById(R.id.vlc_seek_bar_layout);
        vlcSeekBarLayout.setVisibility(View.INVISIBLE);

        overlayTitle = (TextView) findViewById(R.id.vlc_overlay_title);
        subtitlesTextView = (TextView) findViewById(R.id.subtitles_text_view);
        onTouchInfoTextView = (TextView) findViewById(R.id.on_touch_info_text_view);
    }

    public void init(String urlToStream, boolean hideSeekBar, String title, Callback<Boolean> onToggleFullscreen) {
        this.setTitle(title);
        this.setUrlToStream(urlToStream);
        this.setHideSeekBar(hideSeekBar);
        this.onToggleFullscreen = onToggleFullscreen;

        Log.v(TAG, "init");
    }

    private void setupControls() {
        // PLAY PAUSE
        vlcButtonPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (libvlc.isPlaying()) {
                    libvlc.pause();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline_black_24dp));
                } else {
                    libvlc.play();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_circle_outline_black_24dp));
                }
            }
        });

        // MARK
        vlcMarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                VLCPlayer.sendMarkToReact(libvlc.getTime(), libvlc.getLength());
            }
        });

        // SEEK BAR
        handlerSeekBar = new Handler();
        runnableSeekBar = new Runnable() {
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

                    vlcSeekBar.setProgress((int) (libvlc.getPosition() * 100));
                    vlcDuration.setText(duration);
                }

                handlerSeekBar.postDelayed(runnableSeekBar, 1000);
            }
        };

        runnableSeekBar.run();
        vlcSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        handlerOverlay = new Handler();
        runnableOverlay = new Runnable() {
            @Override
            public void run() {
                setOverlayVisibility(View.INVISIBLE);
                toggleFullscreen(true);
            }
        };

        handlerOverlay.postDelayed(runnableOverlay, TIME_TO_DISAPPEAR);
    }

    public void setSubtitlesText(String text) {
        float nowMs = libvlc.getTime();

        if (nowMs < maximumPosition || (currSubtitles != null && currSubtitles.length() > 0 && currSubtitles.equals(text))) {
            subtitlesTextView.setText(text);
            currSubtitles = text;
        } else {
            subtitlesTextView.setText("");
        }
    }

    private void setOverlayVisibility(int visibility) {
        overlayControlLayout.setVisibility(visibility);
        overlayTitle.setVisibility(visibility);
    }

    public boolean playMovie() {
        if (libvlc != null && libvlc.isPlaying()) {
            return false;
        }

        holder = mSurface.getHolder();
        holder.addCallback(this);

        createPlayer(urlToStream);
        return true;
    }

    private void toggleFullscreen(boolean fullscreen) {
        if (this.onToggleFullscreen != null) {
            this.onToggleFullscreen.call(fullscreen);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceholder, int format,
                               int width, int height) {
        if (libvlc != null) {
            libvlc.attachSurface(surfaceholder.getSurface(), this);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
    }

    @Override
    public void setSurfaceSize(int width, int height, int visible_width,
                               int visible_height, int sar_num, int sar_den) {
        if (surfaceWidth != width || surfaceHeight != height) {
            surfaceWidth = width;
            surfaceHeight = height;

            Message msg = Message.obtain(mHandler, VIDEO_SIZE_CHANGED, surfaceWidth, surfaceHeight);
            msg.sendToTarget();
        }
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
                Toast toast = Toast.makeText(getContext(), media, Toast.LENGTH_LONG);
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
            libvlc.setNetworkCaching(60000); // 5 seconds is just a test, maximum should be lower than 60 seconds judging by github projects
            // getNetworkCaching and others getters are called from c++

            LibVLC.restart(getContext());

            EventHandler.getInstance().addHandler(mHandler);

            holder.setFormat(PixelFormat.RGBX_8888);
            holder.setKeepScreenOn(true);

            MediaList list = libvlc.getMediaList();

            list.clear();
            list.add(new Media(libvlc, LibVLC.PathToURI(media)), false);

            libvlc.playIndex(0);
            mPositionHandler.postDelayed(mPositionHandlerTask, POSITION_UPDATE_INITIAL);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not create Vlc Player", Toast.LENGTH_LONG).show();
        }
    }

    public void releasePlayer() {
        if (handlerSeekBar != null && runnableSeekBar != null) {
            handlerSeekBar.removeCallbacks(runnableSeekBar);
        }

        EventHandler.getInstance().removeHandler(mHandler);

        if (libvlc == null) {
            return;
        }

        libvlc.stop();
        libvlc.detachSurface();

        holder = null;
        libvlc.closeAout();

        mVideoWidth = 0;
        mVideoHeight = 0;
    }

    public void recalcSize() {
        setSize(mVideoWidth, mVideoHeight);
    }

    public void setSize(int width, int height) {
        mVideoWidth = width;
        mVideoHeight = height;

        if (mVideoWidth * mVideoHeight <= 1) {
            return;
        }

        Context context = getContext();
        Window window;

        if (context instanceof ReactContext) {
            if (activity != null) {
                window = activity.getWindow();
            } else {
                Log.v(TAG, "ReactContext");
                return;
            }
        } else {
            window = ((Activity) context).getWindow();
        }

        int w = window.getDecorView().getWidth();
        int h = window.getDecorView().getHeight();

        boolean isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

        if (w > h && isPortrait || w < h && !isPortrait) {
            int i = w;
            w = h;
            h = i;
        }

        float videoAR = (float) mVideoWidth / (float) mVideoHeight;
        float screenAR = (float) w / (float) h;

        if (screenAR < videoAR) {
            h = (int) (w / videoAR);
        } else {
            w = (int) (h * videoAR);
        }

        if (holder != null) {
            holder.setFixedSize(mVideoWidth, mVideoHeight);
        }

        ViewGroup.LayoutParams lp = mSurface.getLayoutParams();
        lp.width = w;
        lp.height = h;
        mSurface.setLayoutParams(lp);
        mSurface.invalidate();
    }

    /**
     * **********
     * Events
     * ***********
     */
    private Handler mPositionHandler = new Handler();
//    private static int POSITION_UPDATE_INTERVAL = 1000 * 30; // 30 seconds
    private static int POSITION_UPDATE_INTERVAL = 100; // 0.1 seconds
//    private static int POSITION_UPDATE_INITIAL = 1000 * 10; // 10 seconds
    private static int POSITION_UPDATE_INITIAL = 50; // 0.1 seconds

    private Runnable mPositionHandlerTask = new Runnable() {
        @Override
        public void run() {
            if (holder != null) {
                VLCPlayer.sendPositionToReact(libvlc.getTime(), libvlc.getLength());
                mPositionHandler.postDelayed(mPositionHandlerTask, POSITION_UPDATE_INTERVAL);
            }
        }
    };

    private Handler mHandler = new VlcHandler(this);

    public void setTitle(String title) {
        this.title = title;
        overlayTitle.setText(this.title);
    }

    public void setHideSeekBar(boolean hideSeekBar) {
        this.hideSeekBar = hideSeekBar;
        this.vlcSeekBarLayout.setVisibility(hideSeekBar ? View.INVISIBLE : View.VISIBLE);
    }

    public int getVideoWidth() {
        return mVideoWidth;
    }

    public int getVideoHeight() {
        return mVideoHeight;
    }

    private void showOverlay() {
        setOverlayVisibility(View.VISIBLE);
    }

    private void hideOverlay() {
        setOverlayVisibility(View.INVISIBLE);
    }

    public void setUrlToStream(String urlToStream) {
        this.urlToStream = urlToStream;
    }

    public void setUrlToStreamAndPlay(String urlToStream) {
        this.setUrlToStream(urlToStream);
        this.playMovie();
    }
}
