package com.wass08.vlcsimpleplayer;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.wass08.vlcsimpleplayer.util.Callback;

/**
 * Created by ewnd9 on 20.06.16.
 */
public class PlayerView extends FrameLayout {

    private static final String TAG = "ReactNativeJS";

    private static final long TIME_TO_DISAPPEAR = 3000;

    //    private static int POSITION_UPDATE_INTERVAL = 1000 * 30; // 30 seconds
    private static int POSITION_UPDATE_INTERVAL = 100; // 0.1 seconds
    //    private static int POSITION_UPDATE_INITIAL = 1000 * 10; // 10 seconds
    private static int POSITION_UPDATE_INITIAL = 50; // 0.1 seconds

    private ViewGroup overlayLayout;
    private ViewGroup overlayControlLayout;
    private ImageView vlcButtonPlayPause;
    private ImageView vlcMarkButton;
    private Handler handlerOverlay;
    private Runnable runnableOverlay;

    private TextView overlayTitle;
    private TextView subtitlesTextView;
    private TextView onTouchInfoTextView;

    private VLCWrapper vlcWrapper;

    private View vlcSeekBarLayout;
    private Handler handlerSeekBar;
    private Runnable runnableSeekBar;
    private SeekBar vlcSeekBar;
    private TextView vlcDuration;

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

        final BroadcastReceiver resizeBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                vlcWrapper.recalcSize();
            }
        };

        activity.registerReceiver(resizeBroadcastReceiver, new IntentFilter("onConfigurationChanged"));
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

        vlcWrapper = new VLCWrapper(
                getContext(),
                (SurfaceView) findViewById(R.id.vlc_surface),
                POSITION_UPDATE_INITIAL,
                POSITION_UPDATE_INTERVAL
        );

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

//                Log.v(TAG, "deltaX " + deltaX + " " + event.getAction());
//                Log.v(TAG, "deltaTime " + ((int) deltaTime));

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    onTouchIsMoving = true;

                    if (deltaX > 10) {
                        onTouchInfoTextView.setVisibility(View.VISIBLE);
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
                        long nowMs = vlcWrapper.getTime();
                        long nextMs = nowMs + ((isFastForward ? +1 : -1) * (int) deltaTime * 1000);

                        if (!isFastForward) {
                            maximumPosition = nowMs;
                        }

                        Log.v(TAG, "now " + nowMs + " " + nextMs + " " + 1);
                        vlcWrapper.setTime(nextMs);
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
        onTouchInfoTextView = (TextView) findViewById(R.id.on_touch_info_text_view);

        subtitlesTextView = (TextView) findViewById(R.id.subtitles_text_view);
        subtitlesTextView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "hello world");
            }
        });
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
                if (vlcWrapper.isPlaying()) {
                    vlcWrapper.pause();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_play_circle_outline_black_24dp));
                } else {
                    vlcWrapper.play();
                    vlcButtonPlayPause.setImageDrawable(getResources().getDrawable(R.drawable.ic_pause_circle_outline_black_24dp));
                }
            }
        });

        // MARK
        vlcMarkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PlayerReactModule.sendMarkToReact(vlcWrapper.getTime(), vlcWrapper.getLength());
            }
        });

        // SEEK BAR
        handlerSeekBar = new Handler();
        runnableSeekBar = new Runnable() {
            @Override
            public void run() {
                if (vlcWrapper.isInit()) {
                    long curTime = vlcWrapper.getTime();
                    long totalTime = (long) (curTime / vlcWrapper.getPosition());

                    int minutes = (int) (curTime / (60 * 1000));
                    int seconds = (int) ((curTime / 1000) % 60);
                    int endMinutes = (int) (totalTime / (60 * 1000));
                    int endSeconds = (int) ((totalTime / 1000) % 60);

                    String duration = String.format("%02d:%02d / %02d:%02d", minutes, seconds, endMinutes, endSeconds);

                    vlcSeekBar.setProgress((int) (vlcWrapper.getPosition() * 100));
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
        float nowMs = vlcWrapper.getTime();

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
        if (vlcWrapper.isInit() && vlcWrapper.isPlaying()) {
            return false;
        }

        vlcWrapper.onPlayFile();

        createPlayer(urlToStream);
        return true;
    }

    private void toggleFullscreen(boolean fullscreen) {
        if (this.onToggleFullscreen != null) {
            this.onToggleFullscreen.call(fullscreen);
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

        vlcWrapper.onCreatePlayer(media);
    }

    public void releasePlayer() {
        if (handlerSeekBar != null && runnableSeekBar != null) {
            handlerSeekBar.removeCallbacks(runnableSeekBar);
        }

        vlcWrapper.onReleasePlayer();
    }

    public void recalcSize() {
        vlcWrapper.recalcSize();
    }

    public void setTitle(String title) {
        this.title = title;
        overlayTitle.setText(this.title);
    }

    public void setHideSeekBar(boolean hideSeekBar) {
        this.hideSeekBar = hideSeekBar;
        this.vlcSeekBarLayout.setVisibility(hideSeekBar ? View.INVISIBLE : View.VISIBLE);
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
