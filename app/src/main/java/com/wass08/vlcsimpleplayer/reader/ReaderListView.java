package com.wass08.vlcsimpleplayer.reader;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by ewnd9 on 19.07.16.
 */
public class ReaderListView extends ListView {

    private static final String TAG = "ReactNativeJS";

    private Activity activity;
    private ReaderListViewAdapter adapter;

    private Queue<Pair<Integer, ReadableMap>> queue;
    private Handler handler;

    private boolean hasScheduledTask = false;

    private int scrollState = -1;
    private int prevFirstVisibleItem = -1;
    private Map<Integer, Boolean> sentRequests;

    Callback onRequestFullText = new Callback() {
        @Override
        public void invoke(Object... args) {
            int position = (int) args[0];

            if (sentRequests.get(position) != null) {
                return;
            }

            WritableMap params = Arguments.createMap();
            params.putInt("position", position);

            sendEvent("positionUpdate", params);
        }
    };

    Callback onSendSelection = new Callback() {
        @Override
        public void invoke(Object... args) {
            int position = (int) args[0];
            int beginning = (int) args[1];
            int end = (int) args[2];
            String text = (String) args[3];

            WritableMap params = Arguments.createMap();

            params.putInt("position", position);
            params.putString("text", text);
            params.putInt("beginning", beginning);
            params.putInt("end", end);

            sendEvent("onSelection", params);
        }
    };


    public ReaderListView(ReactContext context, Activity activity) {
        super(context);

        this.activity = activity;

        this.handler = new Handler();

        this.adapter = new ReaderListViewAdapter(context, activity.getWindowManager(), onRequestFullText, onSendSelection);
        this.setAdapter(adapter);

        this.queue = new LinkedList<>();
        this.sentRequests = new HashMap<>();

        this.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                ReaderListView.this.scrollState = scrollState;
                dispatchQueueProcessing();
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    public boolean isScrolling() {
        return this.scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL;
    }

    public void sendEvent(String eventName, WritableMap params) {
        ((ReactContext) getContext())
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    public void setItems(ReadableArray xs) {
        adapter.clear();

        for (int i = 0 ; i < xs.size() ; i++) {
            ReadableMap map = xs.getMap(i);
            ReaderListViewItem item = new ReaderListViewItem();

            item.id = map.getString("id");
            item.text = map.getString("text");

            adapter.add(item);
        }

        //scrollToPosition(50);
    }

    public void scrollToPosition(final int position, View v) {
        final int top = (v == null) ? 0 : (v.getTop() - this.getPaddingTop());

        handler.post(new Runnable() {
            @Override
            public void run() {
                ReaderListView.this.setSelectionFromTop(position, top);
            }
        });
    }

    public void scrollToPosition(int position) {
        View v = this.getChildAt(position);
        scrollToPosition(position, v);
    }

    public void setFullItem(int position, final ReadableMap text) {
        queue.add(new Pair<>(position, text));
        Log.v(TAG, "setFullItem enqueue " + position + " hasScheduledTask = " + hasScheduledTask);
        dispatchQueueProcessing();
    }

    public void dispatchQueueProcessing() {
        if (isScrolling() || hasScheduledTask || queue.size() == 0) {
            return;
        }

        hasScheduledTask = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isScrolling()) {
                    return;
                }

                boolean isNotEmpty = queue.size() > 0;

                while (!queue.isEmpty()) {
                    Pair<Integer, ReadableMap> pair = queue.poll();

                    int position = pair.first;
                    ReadableMap text = pair.second;

                    ReaderListViewItem item = adapter.getItem(position);
                    Log.v("ReactNativeJS", "setFullItem" + " " + position + " ");

                    item.fullText = "LOL";
                    item.data = text;
                }

                if (isNotEmpty) {
                    adapter.notifyDataSetChanged();
                    ReaderListView.this.refreshDrawableState();
                }


                hasScheduledTask = false;
            }
        }, 0);
    }
}
