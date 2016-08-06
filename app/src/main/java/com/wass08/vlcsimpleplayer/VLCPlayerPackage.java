package com.wass08.vlcsimpleplayer;

import android.app.Activity;

import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.uimanager.ViewManager;
import com.wass08.vlcsimpleplayer.reader.ReactReaderListView;
import com.wass08.vlcsimpleplayer.vendor.recyclerview.RecyclerListViewManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class VLCPlayerPackage implements ReactPackage {

    private Activity activity;

    public VLCPlayerPackage(Activity activity) {
        this.activity = activity;
    }

    @Override
    public List<NativeModule> createNativeModules(ReactApplicationContext reactContext) {
        List<NativeModule> modules = new ArrayList<>();
        modules.add(new VLCPlayer(reactContext));
        return modules;
    }

    @Override
    public List<Class<? extends JavaScriptModule>> createJSModules() {
        return Collections.emptyList();
    }

    @Override
    public List<ViewManager> createViewManagers(
            ReactApplicationContext reactContext) {
        return Arrays.<ViewManager>asList(
                new ReactPlayerManager(activity),
                new ReactReaderListView(activity),
                new RecyclerListViewManager()
        );
    }

}
