package com.example.aleksejkocergin.randomwebm.components;

import com.example.aleksejkocergin.randomwebm.fragments.RandomFragment;
import com.example.aleksejkocergin.randomwebm.module.ExoPlayerModule;

import dagger.Component;

@Component(modules = ExoPlayerModule.class)
@PlayerScope
public interface PlayerComponent {
    void inject(RandomFragment randomFragment);
}
