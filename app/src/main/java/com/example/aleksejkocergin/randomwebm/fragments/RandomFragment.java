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
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.example.aleksejkocergin.randomwebm.adapter.TagsAdapter;
import com.example.aleksejkocergin.randomwebm.components.DaggerPlayerComponent;
import com.example.aleksejkocergin.randomwebm.components.PlayerComponent;
import com.example.aleksejkocergin.randomwebm.module.ExoPlayerModule;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class RandomFragment extends Fragment {
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

    @Inject DefaultBandwidthMeter bandwidthMeter;
    @Inject TrackSelection.Factory videoTrackSelectionFactory;
    @Inject TrackSelector trackSelector;
    @Inject SimpleExoPlayer player;
    @Inject DataSource.Factory dataSourceFactory;

    private MediaSource mediaSource;
    private TagsAdapter mTagsAdapter;
    private RandomWebmApplication mApplication;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public static RandomFragment newInstance() {
        return new RandomFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.random_fragment, container, false);
        ButterKnife.bind(this, v);
        mApplication = (RandomWebmApplication) getActivity().getApplication();
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(
                getContext(), LinearLayoutManager.HORIZONTAL, false);
        mTagsRecycler.setLayoutManager(mLayoutManager);
        initPlayerComponent();
        fetchWebmDetails();
        randomButton.setOnClickListener(view -> {
            mediaSource.releaseSource();
            fetchWebmDetails();
        });

        return v;
    }

    private void initPlayerComponent() {
        if (player == null) {
            PlayerComponent component = DaggerPlayerComponent
                    .builder()
                    .exoPlayerModule(new ExoPlayerModule(getActivity()))
                    .build();
            component.inject(this);
        }
    }

    private void initPlayer(String VIDEO_URL) {
        mediaSource = new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(VIDEO_URL));
        playerView.setPlayer(player);
        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        player.release();
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

    private void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        if (getWebm != null) {
            initPlayer(getWebm.url());
            createdAt.setText(getWebm.createdAt());
            views.setText(String.valueOf(getWebm.views()));

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
        }
    }

    private void fetchWebmDetails() {
        ApolloCall<WebmQuery.Data> webmQuery = mApplication.apolloClient()
                .query(new WebmQuery(""));
        mDisposable.add(Rx2Apollo.from(webmQuery)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribeWith(new DisposableObserver<Response<WebmQuery.Data>>() {
            @Override
            public void onNext(Response<WebmQuery.Data> dataResponse) {
                setWebmData(dataResponse.data());
            }

            @Override
            public void onError(Throwable e) {}

            @Override
            public void onComplete() {}
        }));
    }
}