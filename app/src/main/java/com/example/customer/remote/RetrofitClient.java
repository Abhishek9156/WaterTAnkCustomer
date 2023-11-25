package com.example.customer.remote;

import retrofit2.Retrofit;

import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    public static Retrofit instance;
    public static Retrofit getInstance(){
        return instance==null ? new Retrofit.Builder()
                .baseUrl("https://maps.google.com/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .build():instance;
    }
}
