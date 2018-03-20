package com.example.aleksejkocergin.randomwebm.module;

import android.content.Context;

import com.example.aleksejkocergin.randomwebm.components.PlayerScope;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import dagger.Module;
import dagger.Provides;

@Module
public class ExoPlayerModule {
    private final Context context;

    public ExoPlayerModule(Context context) {
        this.context = context;
    }

    @Provides
    @PlayerScope
    public Context context() {
        return context;
    }

    @Provides
    @PlayerScope
    public DefaultBandwidthMeter providesBandwidthMeter() {
        return new DefaultBandwidthMeter();
    }

    @Provides
    @PlayerScope
    public TrackSelection.Factory providesTrackSelectionFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new AdaptiveTrackSelection.Factory(bandwidthMeter);
    }

    @Provides
    @PlayerScope
    public TrackSelector providesTrackSelectorFactory(TrackSelection.Factory videoTrackSelectionFactory) {
        return new DefaultTrackSelector(videoTrackSelectionFactory);
    }

    @Provides
    @PlayerScope
    public SimpleExoPlayer providesExoPlayer(Context context, TrackSelector trackSelector) {
        return ExoPlayerFactory.newSimpleInstance(context, trackSelector);
    }

    @Provides
    @PlayerScope
    public DataSource.Factory providesDataSourceFactory(Context context) {
        return new DefaultDataSourceFactory(
                context, Util.getUserAgent(context, "RandomWebm"),
                new DefaultBandwidthMeter());
    }
}