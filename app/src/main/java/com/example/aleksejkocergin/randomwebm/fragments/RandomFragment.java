package com.example.aleksejkocergin.randomwebm.fragments;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.adapter.TagsAdapter;
import com.example.aleksejkocergin.randomwebm.dagger.DaggerPlayerComponent;
import com.example.aleksejkocergin.randomwebm.dagger.PlayerComponent;
import com.example.aleksejkocergin.randomwebm.interfaces.ToggleVotes;
import com.example.aleksejkocergin.randomwebm.interfaces.WebmData;
import com.example.aleksejkocergin.randomwebm.dagger.ExoPlayerModule;
import com.example.aleksejkocergin.randomwebm.util.ToggleVotesUtil;
import com.example.aleksejkocergin.randomwebm.util.WebmDetailsFetcher;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RandomFragment extends Fragment implements ToggleVotes, WebmData {
    @BindView(R.id.player_view)
    SimpleExoPlayerView playerView;
    @BindView(R.id.txt_createdAt)
    TextView createdAt;
    @BindView(R.id.txt_views)
    TextView views;
    @BindView(R.id.button_random)
    Button randomButton;
    @BindView(R.id.tags_recycler_view)
    RecyclerView mTagsRecycler;
    @BindView(R.id.loading_bar)
    ProgressBar progressBar;

    // Likes N dislikes
    @BindView(R.id.like_count)
    TextView tvLikeCount;
    @BindView(R.id.dislike_count)
    TextView tvDislikeCount;
    @BindView(R.id.thumb_up_button)
    ToggleButton thumbUpButton;
    @BindView(R.id.thumb_down_button)
    ToggleButton thumbDownButton;

    @Inject
    DefaultBandwidthMeter bandwidthMeter;
    @Inject
    TrackSelection.Factory videoTrackSelectionFactory;
    @Inject
    TrackSelector trackSelector;
    @Inject
    SimpleExoPlayer player;
    @Inject
    DataSource.Factory dataSourceFactory;

    private String webmId;
    private int likeCount;
    private int dislikeCount;
    private boolean hasLike = false;
    private boolean hasDislike = false;
    private ToggleVotesUtil toggleVotesUtil;
    private WebmDetailsFetcher webmFetcher;

    private TagsAdapter mTagsAdapter;

    public static RandomFragment newInstance() {
        return new RandomFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.random_fragment, container, false);
        ButterKnife.bind(this, v);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        mTagsRecycler.setLayoutManager(mLayoutManager);
        toggleVotesUtil = new ToggleVotesUtil(this);
        webmFetcher = new WebmDetailsFetcher(this);
        randomButton.setOnClickListener(view -> webmFetcher.fetchWebmDetails());
        initPlayerComponent();
        webmFetcher.fetchWebmDetails();

        return v;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().hide();
            getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            SimpleExoPlayerView.LayoutParams params =
                    (SimpleExoPlayerView.LayoutParams) playerView.getLayoutParams();
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            playerView.setLayoutParams(params);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            ((AppCompatActivity) getActivity()).getSupportActionBar().show();
            getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            SimpleExoPlayerView.LayoutParams params =
                    (SimpleExoPlayerView.LayoutParams) playerView.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.player_size);
            playerView.setLayoutParams(params);
        }
    }

    @Override
    public void plusWebmLike() {
        tvLikeCount.setText(String.valueOf(likeCount += 1));
    }

    @Override
    public void plusWebmDislike() {
        tvDislikeCount.setText(String.valueOf(dislikeCount += 1));
    }

    @Override
    public void onStop() {
        super.onStop();
        player.release();
    }

    private void initPlayerComponent() {
        if (player == null) {
            PlayerComponent component = DaggerPlayerComponent.builder()
                    .exoPlayerModule(new ExoPlayerModule(getActivity())).build();
            component.inject(this);
        }
    }

    @Override
    public void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        if (getWebm != null) {
            likeCount = getWebm.likes();
            dislikeCount = getWebm.dislikes();
            webmId = getWebm.id();

            initPlayer(getWebm.url());

            createdAt.setText(getWebm.createdAt());
            views.setText(String.valueOf(getWebm.views()));
            tvLikeCount.setText(String.valueOf(likeCount));
            tvDislikeCount.setText(String.valueOf(dislikeCount));

            List<String> tagsList = new ArrayList<>();
            for (int i = 0; i < getWebm.tags().size(); ++i) {
                tagsList.add(getWebm.tags().get(i).name());
            }
            mTagsAdapter = new TagsAdapter(getContext(), tagsList);
            mTagsRecycler.setAdapter(mTagsAdapter);
            mTagsAdapter.SetOnItemClickListener((view, position) -> {
                String tagName = mTagsAdapter.getItem(position).toLowerCase();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment fragment = WebmListFragment.newInstance("createdAt", tagName);
                ft.replace(R.id.container, fragment).commit();
            });

            thumbUpButton.setOnClickListener(view -> toggleVotesUtil
                    .toggleLike(webmId, hasLike, hasDislike));

            thumbDownButton.setOnClickListener(view -> toggleVotesUtil
                    .toggleDislike(webmId, hasLike, hasDislike));
        }
    }

    private void initPlayer(String VIDEO_URL) {
        playerView.setPlayer(player);
        MediaSource mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(VIDEO_URL));
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);

        // Progress bar
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
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                if (playbackState == ExoPlayer.STATE_BUFFERING) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onRepeatModeChanged(int repeatMode) {

            }

            @Override
            public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {

            }

            @Override
            public void onPositionDiscontinuity(int reason) {

            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

            }

            @Override
            public void onSeekProcessed() {

            }
        });
    }
}