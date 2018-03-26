package com.example.aleksejkocergin.randomwebm.dagger;

import com.example.aleksejkocergin.randomwebm.activity.MainActivity;
import com.example.aleksejkocergin.randomwebm.activity.PlayerActivity;
import com.example.aleksejkocergin.randomwebm.fragments.RandomFragment;

import dagger.Component;

@Component(modules = ExoPlayerModule.class)
@PlayerScope
public interface PlayerComponent {
    void inject(RandomFragment randomFragment);
    void inject(PlayerActivity playerActivity);
}
