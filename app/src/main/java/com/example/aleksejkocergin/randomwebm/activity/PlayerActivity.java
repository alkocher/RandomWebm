package com.example.aleksejkocergin.randomwebm.activity;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = PlayerActivity.class.getSimpleName();

    ProgressBar progressBar;
    SimpleExoPlayerView playerView;
    private MediaSource mVideoSource;
    private SimpleExoPlayer player;
    private ExtractorsFactory extractorsFactory;
    private RandomWebmApplication application;
    private CompositeDisposable disposables = new CompositeDisposable();

    private int mResumeWindow;
    private long mResumePosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_player);
        application = (RandomWebmApplication) getApplication();
        progressBar = (ProgressBar) findViewById(R.id.loadings_bar);
        playerView = (SimpleExoPlayerView) findViewById(R.id.exo_player);
        fetchWebmDetails();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeExoPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mResumeWindow = playerView.getPlayer().getCurrentWindowIndex();
        mResumePosition = Math.max(0, playerView.getPlayer().getContentPosition());
        if (playerView != null && playerView.getPlayer() != null) {
            playerView.getPlayer().release();
        }
    }

    private void initializeExoPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);
        LoadControl loadControl = new DefaultLoadControl();
        extractorsFactory = new DefaultExtractorsFactory();
        player = ExoPlayerFactory.newSimpleInstance(new DefaultRenderersFactory(this), trackSelector, loadControl);
        playerView.setPlayer(player);
        playerView.getPlayer().setPlayWhenReady(true);
        // Loading Bar
        player.addListener(new ExoPlayer.EventListener() {
            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {

            }
            @Override
            public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

            }
            @Override
            public void onLoadingChanged(boolean isLoading) {

            }
            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }
            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }
            @Override
            public void onPositionDiscontinuity() {

            }
            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerView.setLayoutParams(params);
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) playerView.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            playerView.setLayoutParams(params);
        }
    }

    private void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        boolean haveResumePosition = mResumeWindow != C.INDEX_UNSET;
        String streamUrl;
        if (getWebm != null) {
            streamUrl = getWebm.url();
            String userAgent = Util.getUserAgent(this, getApplicationContext().getApplicationInfo().packageName);
            DefaultHttpDataSourceFactory httpDataSourceFactory = new DefaultHttpDataSourceFactory(userAgent, null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true);
            DefaultDataSourceFactory dataSourceFactory = new DefaultDataSourceFactory(this, null, httpDataSourceFactory);
            Uri daUri = Uri.parse(streamUrl);
            mVideoSource = new ExtractorMediaSource(daUri, dataSourceFactory, extractorsFactory, null, null);
            playerView.getPlayer().setPlayWhenReady(true);
        }
        if (haveResumePosition) {
            player.seekTo(mResumeWindow, mResumePosition);
        }
        playerView.getPlayer().prepare(mVideoSource);
    }

    private void fetchWebmDetails() {
        ApolloCall<WebmQuery.Data> webmQuery = application.apolloClient()
                .query(new WebmQuery(getIntent().getStringExtra("id")));
        disposables.add(Rx2Apollo.from(webmQuery)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribeWith(new DisposableObserver<Response<WebmQuery.Data>>() {
                    @Override
                    public void onNext(Response<WebmQuery.Data> dataResponse) {
                        setWebmData(dataResponse.data());
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, e.getMessage(), e);
                    }

                    @Override
                    public void onComplete() {

                    }
                }));
    }

}
