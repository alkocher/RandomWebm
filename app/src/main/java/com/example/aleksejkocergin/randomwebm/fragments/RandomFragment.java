package com.example.aleksejkocergin.randomwebm.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.adapter.TagsAdapter;
import com.example.aleksejkocergin.randomwebm.dagger.DaggerPlayerComponent;
import com.example.aleksejkocergin.randomwebm.dagger.PlayerComponent;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RandomFragment extends Fragment implements WebmData {

    private static final String DISLIKED_WEBM_ID = "disliked_ids_list";
    private static final String WEBM_APP_PREFS = "webm_settings";
    private static final String LIKED_WEBM_ID = "liked_ids_list";

    private String webmId;
    private int likeCount;
    private int dislikeCount;
    private boolean hasLike;
    private boolean hasDislike;
    private ArrayList<String> likedWebmList;
    private ArrayList<String> dislikedWebmList;
    private SharedPreferences prefs;
    private ToggleVotesUtil toggleVotesUtil;
    private WebmDetailsFetcher webmFetcher;
    private TagsAdapter mTagsAdapter;

    // DI ExoPlayer
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

    // Likes N dislikes
    @BindView(R.id.like_count)
    TextView tvLikeCount;
    @BindView(R.id.dislike_count)
    TextView tvDislikeCount;
    @BindView(R.id.thumb_up_button)
    ToggleButton thumbUpButton;
    @BindView(R.id.thumb_down_button)
    ToggleButton thumbDownButton;

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


    public static RandomFragment newInstance() {
        return new RandomFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.random_fragment, container, false);
        ButterKnife.bind(this, v);
        prefs = getActivity().getSharedPreferences(WEBM_APP_PREFS, Context.MODE_PRIVATE);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        mTagsRecycler.setLayoutManager(mLayoutManager);
        toggleVotesUtil = new ToggleVotesUtil();
        webmFetcher = new WebmDetailsFetcher(this);
        likedWebmList = new ArrayList<>();
        dislikedWebmList = new ArrayList<>();
        initPlayerComponent();
        retrieveSharedPreferences();
        webmFetcher.fetchWebmDetails();

        randomButton.setOnClickListener(view -> webmFetcher.fetchWebmDetails());

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
    public void onStop() {
        super.onStop();
        player.release();
    }

    @Override
    public void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        if (getWebm != null) {
            hasLike = false;
            hasDislike = false;
            likeCount = getWebm.likes();
            dislikeCount = getWebm.dislikes();
            webmId = getWebm.id();

            initPlayer(getWebm.url());
            checkWebmId();

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

            thumbUpButton.setChecked(hasLike);
            thumbUpButton.setOnClickListener((View view) -> toggleLike());

            thumbDownButton.setChecked(hasDislike);
            thumbDownButton.setOnClickListener((View view) -> toggleDislike());
        }
    }

    private void checkWebmId() {
        for (int i = 0; i < likedWebmList.size(); ++i) {
            if (likedWebmList.get(i).equals(webmId)) {
                Log.d("webm_id", "Уже в списке лайков!");
                hasLike = true;
            }
        }

        for (int i = 0; i < dislikedWebmList.size(); ++i) {
            if (dislikedWebmList.get(i).equals(webmId)) {
                Log.d("webm_id", "Уже в списке дизлайков");
                hasDislike = true;
            }
        }
    }

    private void toggleLike() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean isLiked = hasLike;
        boolean isDisliked = hasDislike;

        if (hasLike) {
            likedWebmList.remove(webmId);
            Set<String> likedWebm = new HashSet<>(likedWebmList);
            editor.putStringSet(LIKED_WEBM_ID, likedWebm);
            editor.apply();
            tvLikeCount.setText(String.valueOf(likeCount -= 1));
            hasLike = false;
            Log.d("like_webm_list", "" + likedWebm.size());
        } else {
            likedWebmList.add(webmId);
            Set<String> likedWebm = new HashSet<>(likedWebmList);
            editor.putStringSet(LIKED_WEBM_ID, likedWebm);
            editor.apply();
            tvLikeCount.setText(String.valueOf(likeCount += 1));
            hasLike = true;
            Log.d("like_webm_list", "" + likedWebm.size());
        }

        if(hasDislike) {
            dislikedWebmList.remove(webmId);
            Set<String> dislikedWebm = new HashSet<>(dislikedWebmList);
            editor.putStringSet(DISLIKED_WEBM_ID, dislikedWebm);
            editor.apply();
            tvDislikeCount.setText(String.valueOf(dislikeCount -= 1));
            hasDislike = false;
            thumbDownButton.setChecked(false);
            Log.d("dis_webm_list", "" + dislikedWebm.size());
        }

        thumbUpButton.setChecked(hasLike);
        toggleVotesUtil.toggleLike(webmId, isLiked, isDisliked);
    }

    private void toggleDislike() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean isLiked = hasLike;
        boolean isDisliked = hasDislike;

        if (hasDislike) {
            dislikedWebmList.remove(webmId);
            Set<String> dislikedWebm = new HashSet<>(dislikedWebmList);
            editor.putStringSet(DISLIKED_WEBM_ID, dislikedWebm);
            editor.apply();
            tvDislikeCount.setText(String.valueOf(dislikeCount -= 1));
            hasDislike = false;
            Log.d("dis_webm_list", "" + dislikedWebm.size());
        } else {
            dislikedWebmList.add(webmId);
            Set<String> dislikedWebm = new HashSet<>(dislikedWebmList);
            editor.putStringSet(DISLIKED_WEBM_ID, dislikedWebm);
            editor.apply();
            tvDislikeCount.setText(String.valueOf(dislikeCount += 1));
            hasDislike = true;
            Log.d("dis_webm_list", "" + dislikedWebm.size());
        }

        if (hasLike) {
            likedWebmList.remove(webmId);
            Set<String> likedWebm = new HashSet<>(likedWebmList);
            editor.putStringSet(LIKED_WEBM_ID, likedWebm);
            editor.apply();
            tvLikeCount.setText(String.valueOf(likeCount -= 1));
            hasLike = false;
            thumbUpButton.setChecked(false);
            Log.d("like_webm_list", "" + likedWebm.size());
        }

        thumbDownButton.setChecked(hasDislike);
        toggleVotesUtil.toggleDislike(webmId, isLiked, isDisliked);
    }

    private void retrieveSharedPreferences() {
        likedWebmList.addAll(prefs.getStringSet(LIKED_WEBM_ID, null));
        dislikedWebmList.addAll(prefs.getStringSet(DISLIKED_WEBM_ID, null));
        Log.d("retrieveLikePrefs_rand", "" + likedWebmList);
        Log.d("retrieveDislike_rand", "" + dislikedWebmList);
    }

    private void initPlayerComponent() {
        if (player == null) {
            PlayerComponent component = DaggerPlayerComponent.builder()
                    .exoPlayerModule(new ExoPlayerModule(getActivity())).build();
            component.inject(this);
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