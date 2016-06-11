package com.wass08.vlcsimpleplayer.translator;

import java.util.List;

import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class Api {

    private static final String TAG = "media-center-android";

    private static Yandex yandex;

    public static Yandex getYandex() {
        if (Api.yandex == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://dictionary.yandex.net/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            Api.yandex = retrofit.create(Yandex.class);
        }

        return Api.yandex;
    }

    public interface Yandex {
        @GET("/api/v1/dicservice.json/lookup?key=dict.1.1.20140616T070444Z.ecfe60ba07dd3ebc.9ce897a05d9daa488b050e5ec030f625d666530a&lang=en-ru&flags=4")
        public Call<TranslationResult> getTranslations(@Query("text") String text);
    }

}