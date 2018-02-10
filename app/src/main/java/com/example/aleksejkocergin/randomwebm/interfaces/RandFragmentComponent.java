package com.example.aleksejkocergin.randomwebm.interfaces;

import com.example.aleksejkocergin.randomwebm.fragments.RandomFragment;
import com.example.aleksejkocergin.randomwebm.module.ContextModule;
import com.example.aleksejkocergin.randomwebm.module.ExoPlayerModule;

import dagger.Component;


@Component(modules = {ExoPlayerModule.class, ContextModule.class})
public interface RandFragmentComponent {

    void inject(RandomFragment randFragment);
}
