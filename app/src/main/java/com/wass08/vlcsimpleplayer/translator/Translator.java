package com.wass08.vlcsimpleplayer.translator;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Translator {

    private static final String TAG = "ReactNativeJS"; // @TODO replace later

    private EditText input;
    private TranslationArrayAdapter adapter;

    public Translator(EditText input, TranslationArrayAdapter adapter) {
        this.input = input;
        this.input.addTextChangedListener(watcher);

        this.adapter = adapter;
    }

    private TextWatcher watcher = new TextWatcher() {
        public void afterTextChanged(Editable s) {
            Log.v(TAG, "textChanged: " + s.toString());
            Api.getYandex().getTranslations(s.toString()).enqueue(apiCallback);
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // nothing
        }
    };

    private Callback<TranslationResult> apiCallback = new Callback<TranslationResult>() {
        @Override
        public void onResponse(Call<TranslationResult> call, Response<TranslationResult> response) {
            List<TranslationResult.Translation> translations = response.body().getTranslations();
            adapter.clear();
            adapter.addAll(translations);
        }

        @Override
        public void onFailure(Call<TranslationResult> call, Throwable t) {
            Log.v(TAG, "apiCallback", t);
        }
    };

}

