package com.example.indokicksprototype.network;

import com.example.indokicksprototype.BuildConfig;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClientExternal {

    private static final String BASE_URL = "https://v3.football.api-sports.io/";
    private static FootballApiService service;

    public static FootballApiService getService() {
        if (service == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

            Interceptor headerInterceptor = chain -> {
                Request original = chain.request();
                Request request = original.newBuilder()
                        .addHeader("x-rapidapi-host", "v3.football.api-sports.io")
                        .addHeader("x-rapidapi-key", BuildConfig.API_FOOTBALL_KEY)
                        .build();
                return chain.proceed(request);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(headerInterceptor)
                    .addInterceptor(logging)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            service = retrofit.create(FootballApiService.class);
        }
        return service;
    }
}
