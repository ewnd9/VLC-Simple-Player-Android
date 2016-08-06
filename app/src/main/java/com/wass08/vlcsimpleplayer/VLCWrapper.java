package com.wass08.vlcsimpleplayer;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Toast;

import com.facebook.react.bridge.Callback;
import com.wass08.vlcsimpleplayer.handlers.VlcHandler;

import org.videolan.libvlc.EventHandler;
import org.videolan.libvlc.IVideoPlayer;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaList;

/**
 * Created by ewnd9 on 06.08.16.
 */
public class VLCWrapper {

    public final static int VIDEO_SIZE_CHANGED = -1;

    private Context context;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private int surfaceWidth;
    private int surfaceHeight;

    private LibVLC libvlc;
    private int mVideoWidth;
    private int mVideoHeight;

    private int onPositionUpdateInitialDelay;
    private int onPositionUpdateInterval;

    private Handler mPositionHandler = new Handler();
    private Handler mHandler = new VlcHandler(this);

    private Runnable mPositionHandlerTask;

    public VLCWrapper(Context context, SurfaceView view, final Callback onPositionUpdate, int onPositionUpdateInitialDelay, int onPositionUpdateInterval) {
        this.context = context;
        this.surfaceView = view;
        this.onPositionUpdateInitialDelay = onPositionUpdateInitialDelay;
        this.onPositionUpdateInterval = onPositionUpdateInterval;

        this.mPositionHandler = new Handler();
        this.mHandler = new VlcHandler(this);

        this.mPositionHandlerTask = new Runnable() {
            @Override
            public void run() {
                if (surfaceHolder != null) {
                    onPositionUpdate.invoke(libvlc.getTime(), libvlc.getLength());
                    mPositionHandler.postDelayed(mPositionHandlerTask, VLCWrapper.this.onPositionUpdateInterval);
                }
            }
        };
    }

    public void onPlayFile() {
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceholder, int format,
                                       int width, int height) {
                if (libvlc != null) {
                    libvlc.attachSurface(surfaceholder.getSurface(), new IVideoPlayer() {
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
                    });
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceholder) {
            }
        });
    }

    public void onCreatePlayer(String media) {
        try {
            if (media.length() > 0) {
                Toast toast = Toast.makeText(context, media, Toast.LENGTH_LONG);
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
            libvlc.setNetworkCaching(60000); // maximum should be lower than 60 seconds judging by the VLC projects on Github

            LibVLC.restart(context);
            EventHandler.getInstance().addHandler(mHandler);

            surfaceHolder.setFormat(PixelFormat.RGBX_8888);
            surfaceHolder.setKeepScreenOn(true);

            MediaList list = libvlc.getMediaList();

            list.clear();
            list.add(new Media(libvlc, LibVLC.PathToURI(media)), false);

            libvlc.playIndex(0);
            mPositionHandler.postDelayed(mPositionHandlerTask, onPositionUpdateInitialDelay);
        } catch (Exception e) {
            Toast.makeText(context, "Could not create Vlc Player", Toast.LENGTH_LONG).show();
        }
    }

    public void onReleasePlayer() {
        EventHandler.getInstance().removeHandler(mHandler);

        if (libvlc == null) {
            return;
        }

        libvlc.stop();
        libvlc.detachSurface();

        surfaceHolder = null;
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

        Window window = ((Activity) context).getWindow();

        int w = window.getDecorView().getWidth();
        int h = window.getDecorView().getHeight();

        boolean isPortrait = context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

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

        if (surfaceHolder != null) {
            surfaceHolder.setFixedSize(mVideoWidth, mVideoHeight);
        }

        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();

        lp.width = w;
        lp.height = h;

        surfaceView.setLayoutParams(lp);
        surfaceView.invalidate();
    }

    public boolean isInit() {
        return libvlc != null;
    }

    public long getTime() {
        return libvlc.getTime();
    }

    public void setTime(long time) {
        libvlc.setTime(time);
    }

    public boolean isPlaying() {
        return libvlc.isPlaying();
    }

    public void play() {
        libvlc.play();
    }

    public void pause() {
        libvlc.pause();
    }

    public long getLength() {
        return libvlc.getLength();
    }

    public float getPosition() {
        return libvlc.getPosition();
    }

}
