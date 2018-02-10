package com.example.aleksejkocergin.randomwebm.module;


import android.content.Context;
import android.net.Uri;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.Util;

import android.os.Handler;

import dagger.Module;
import dagger.Provides;


@Module
public class ExoPlayerModule {

    private final Context context;
    private String videoUri;
    public ExoPlayerModule(Context context, String  playerUri) {
        this.context = context;
        this.videoUri = playerUri;
    }

    @Provides
    public Context context() {
        return context;
    }

    @Provides
    public Handler provideVideoHandler() {
        return new Handler();
    }

    @Provides
    public DefaultBandwidthMeter providesDefaultBandwidthMeter() {
        return new DefaultBandwidthMeter();
    }

    @Provides
    public TrackSelection.Factory providesVideoTrackSelection(DefaultBandwidthMeter bandwidthMeter) {
        return new AdaptiveTrackSelection.Factory(bandwidthMeter);
    }

    @Provides
    public TrackSelector providesTracSelector(TrackSelection.Factory videoTrackSelectionFactory) {
        return new DefaultTrackSelector(videoTrackSelectionFactory);
    }

    @Provides
    public LoadControl providesLoadControl() {
        return new DefaultLoadControl();
    }

    @Provides
    public SimpleExoPlayer provideExoPlayer(
            Context context, TrackSelector trackSelector, LoadControl loadControl) {
        return ExoPlayerFactory.newSimpleInstance(context, trackSelector, loadControl);
    }

    @Provides
    public String providesUserAgent() {
        return Util.getUserAgent(context, "RandomWebm");
    }

    @Provides
    public HttpDataSource.Factory providesHttpDataSource(
            String userAgent, DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultHttpDataSourceFactory(userAgent, bandwidthMeter);
    }

    @Provides
    public DataSource.Factory providesDataSource(
            Context context, DefaultBandwidthMeter bandwidthMeter, HttpDataSource.Factory httpDataSource) {
        return new DefaultDataSourceFactory(context, bandwidthMeter, httpDataSource);
    }

    @Provides
    public MediaSource providesMediaSource(DataSource.Factory dataSourceFactory, Handler mainHandler) {
        Uri uri = Uri.parse(videoUri);
        return new ExtractorMediaSource(uri, dataSourceFactory, new DefaultExtractorsFactory(), mainHandler, null);
    }
}
