package com.example.aleksejkocergin.randomwebm.fragments;

import android.content.res.Configuration;
import android.support.v4.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.example.aleksejkocergin.randomwebm.adapter.TagsAdapter;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
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
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class RandomFragment extends Fragment implements View.OnClickListener, ExoPlayer.EventListener,
        PlaybackControlView.VisibilityListener {

    private static final String TAG = RandomFragment.class.getSimpleName();
    private RandomWebmApplication application;
    private CompositeDisposable disposables = new CompositeDisposable();

    private DefaultBandwidthMeter defaultBandwidthMeter;
    private DataSource.Factory mediaDataSourceFactory;
    private SimpleExoPlayer player;
    private ExtractorsFactory extractorsFactory;
    private TagsAdapter tagsAdapter;
    private LinearLayoutManager mLayoutManager;

    @BindView(R.id.loading_bar)
    ProgressBar progressBar;
    @BindView(R.id.txt_createdAt)
    TextView createdAt;
    @BindView(R.id.txt_views)
    TextView viewers;
    @BindView(R.id.next_button)
    Button retryButton;
    @BindView(R.id.player_view)
    SimpleExoPlayerView simpleExoPlayerView;
    @BindView(R.id.root)
    View rootView;
    @BindView(R.id.frame_player)
    FrameLayout frameLayout;
    @BindView(R.id.tags_recycler_view)
    RecyclerView tagsRecyclerView;

    String videoUrl;
    String id = "";
    String order = "createdAt";
    String tagName;

    private boolean shouldAutoPlay;
    private int resumeWindow;
    private long resumePosition;

    public static RandomFragment newInstance() {
        RandomFragment randomFragment = new RandomFragment();
        return randomFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.random_fragment, container, false);
        ButterKnife.bind(this, v);
        application = (RandomWebmApplication) getActivity().getApplication();
        retryButton.setOnClickListener(this);
        rootView.setOnClickListener(this);
        shouldAutoPlay = true;
        simpleExoPlayerView.setControllerVisibilityListener(this);
        simpleExoPlayerView.requestFocus();
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        tagsRecyclerView.setLayoutManager(mLayoutManager);
        clearResumePosition();
        fetchWebmDetails();

        return v;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        int position = getActivity().getResources().getConfiguration().orientation;
        if (position == Configuration.ORIENTATION_LANDSCAPE) {
            setLayoutParamsLandscape();
        }
    }

        @Override
    public void onResume() {
        super.onResume();
        initializePlayer();
    }

    @Override
    public void onPause() {
        super.onPause();
        releasePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override public void onDestroy(){
        super.onDestroy();
        disposables.dispose();
    }

    public void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory adaptiveTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector =
                new DefaultTrackSelector(adaptiveTrackSelectionFactory);

        player = ExoPlayerFactory.newSimpleInstance(getActivity().getApplicationContext(), trackSelector);
        simpleExoPlayerView.setPlayer(player);
        simpleExoPlayerView.setControllerVisibilityListener(playbackControlViewVisibilityListener);
        player.addListener(this);

        mediaDataSourceFactory = new DefaultDataSourceFactory(getActivity().getApplicationContext(),
                Util.getUserAgent(getActivity().getApplicationContext(), "Random Webm"), defaultBandwidthMeter);
        extractorsFactory = new DefaultExtractorsFactory();
        player.setPlayWhenReady(shouldAutoPlay);
    }

    private void releasePlayer() {
        if (player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            updateResumePosition();
            player.release();
            player = null;
        }
    }

    private void clearResumePosition() {
        resumeWindow = C.INDEX_UNSET;
        resumePosition = C.TIME_UNSET;
    }

    private void updateResumePosition() {
        resumeWindow = player.getCurrentWindowIndex();
        resumePosition = player.isCurrentWindowSeekable() ? Math.max(0, player.getCurrentPosition())
                : C.TIME_UNSET;
    }

    @Override
    public void onClick(View view) {
        if (view == retryButton & player != null) {
            shouldAutoPlay = player.getPlayWhenReady();
            player.release();
            player = null;
            fetchWebmDetails();
            initializePlayer();
        }
    }

    public void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        if (getWebm != null) {
            createdAt.setText(getWebm.createdAt());
            viewers.setText(String.valueOf(getWebm.views()));

            final List<String> tags = new ArrayList<>();
            for (int i = 0; i < getWebm.tags().size(); ++i) {
                tags.add(getWebm.tags().get(i).name());
            }
            tagsAdapter = new TagsAdapter(getContext(), tags);
            tagsRecyclerView.setAdapter(tagsAdapter);
            tagsAdapter.SetOnItemClickListener(new TagsAdapter.OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    tagName = tagsAdapter.getItem(position).toLowerCase();
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    Fragment fragment = WebmListFragment.newInstance(order, tagName);
                    ft.replace(R.id.container, fragment).commit();
                }
            });

            // Set video URL
            videoUrl = getWebm.url();
            MediaSource extractorMediaSource = new ExtractorMediaSource(Uri.parse(videoUrl),
                    mediaDataSourceFactory,
                    extractorsFactory,
                    null, null);
            boolean haveResumePosition = resumeWindow != C.INDEX_UNSET;
            if (haveResumePosition) {
                player.seekTo(resumeWindow, resumePosition);
            }
            player.prepare(extractorMediaSource, !haveResumePosition, false);
        }
    }

    private void fetchWebmDetails() {
        ApolloCall<WebmQuery.Data> webmQuery = application.apolloClient()
                .query(new WebmQuery(id));
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            setLayoutParamsLandscape();
        }
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setLayoutParamsPortrait();
        }
    }

    private void setLayoutParamsLandscape() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SimpleExoPlayerView.LayoutParams params = (SimpleExoPlayerView.LayoutParams) simpleExoPlayerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        simpleExoPlayerView.setLayoutParams(params);
    }

    private void setLayoutParamsPortrait() {
        ((AppCompatActivity) getActivity()).getSupportActionBar().show();
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        SimpleExoPlayerView.LayoutParams params = (SimpleExoPlayerView.LayoutParams) simpleExoPlayerView.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.height = getResources().getDimensionPixelSize(R.dimen.player_size);
        simpleExoPlayerView.setLayoutParams(params);
    }

    /*private void hidePortraitSystemUI() {
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }*/

    private void showPortraitSystemUI() {
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private void hideLandscapeSystemUI() {
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE
        );
    }

    private void showLandscapeSystemUI() {
        final View decorView = getActivity().getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    private PlaybackControlView.VisibilityListener playbackControlViewVisibilityListener = new PlaybackControlView.VisibilityListener() {
        @Override
        public void onVisibilityChange(int visibility) {
            int orientation = getContext().getResources().getConfiguration().orientation;
            switch (orientation) {
                case Configuration.ORIENTATION_PORTRAIT:
                    if (visibility == View.GONE) {
                        showPortraitSystemUI();
                    }
                    break;
                case Configuration.ORIENTATION_LANDSCAPE:
                    if (visibility == View.GONE) {
                        hideLandscapeSystemUI();
                    } else {
                        showLandscapeSystemUI();
                    }
                    break;
                default:
                    break;
            }
        }
    };

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
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == player.STATE_BUFFERING) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
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
    public void onVisibilityChange(int visibility) {

    }

}
