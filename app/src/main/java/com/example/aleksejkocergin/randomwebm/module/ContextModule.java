package com.example.aleksejkocergin.randomwebm.module;

import android.content.Context;

import dagger.Module;


@Module
public class ContextModule {

    private Context context;

    public ContextModule(Context context) {
        this.context = context;
    }
}