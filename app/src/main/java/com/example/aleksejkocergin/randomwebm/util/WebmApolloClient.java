package com.example.aleksejkocergin.randomwebm.util;

import com.apollographql.apollo.ApolloClient;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;


public class WebmApolloClient {

    private static final String BASE_URL = "https://randomwebm.herokuapp.com/graphql";
    private static ApolloClient webmApolloClient;

    public static ApolloClient getWebmApolloClient() {

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .build();

        webmApolloClient = ApolloClient.builder()
                .serverUrl(BASE_URL)
                .okHttpClient(okHttpClient)
                .build();

        return webmApolloClient;
    }
}
