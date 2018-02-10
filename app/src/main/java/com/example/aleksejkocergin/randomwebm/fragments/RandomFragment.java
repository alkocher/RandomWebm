package com.example.aleksejkocergin.randomwebm.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.example.aleksejkocergin.randomwebm.adapter.TagsAdapter;
import com.example.aleksejkocergin.randomwebm.interfaces.DaggerRandFragmentComponent;
import com.example.aleksejkocergin.randomwebm.interfaces.RandFragmentComponent;
import com.example.aleksejkocergin.randomwebm.module.ExoPlayerModule;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.HttpDataSource;

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
    private static final String TAG = RandomFragment.class.getSimpleName();
    private String id = "";
    private String tagName;
    private String order = "createdAt";
    private String videoUrl = "https://randomwebm.s3.eu-central-1.amazonaws.com/webms/68f8f8e2fc61f12a3ba69b6cb1323eb9.webm";

    @BindView(R.id.player_view)
    SimpleExoPlayerView mPlayerView;
    @BindView(R.id.txt_createdAt)
    TextView createdAt;
    @BindView(R.id.txt_views)
    TextView views;
    @BindView(R.id.next_button)
    Button randomButton;
    @BindView(R.id.tags_recycler_view)
    RecyclerView mTagsRecycler;

    private LinearLayoutManager mLayoutManager;
    private TagsAdapter mTagsAdapter;
    private RandomWebmApplication mApplication;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    @Inject
    Handler handler;
    @Inject
    DefaultBandwidthMeter bandwidthMeter;
    @Inject
    TrackSelection.Factory videoTrackSelectionFactory;
    @Inject
    TrackSelector trackSelector;
    @Inject
    LoadControl loadControl;
    @Inject
    SimpleExoPlayer player;
    @Inject
    String userAgent;
    @Inject
    HttpDataSource.Factory httpDataSourceFactory;
    @Inject
    DataSource.Factory dataSourceFactory;
    @Inject
    MediaSource videoSource;

    public static RandomFragment newInstance() {
        return new RandomFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.random_fragment, container, false);
        ButterKnife.bind(this, v);
        mApplication = (RandomWebmApplication) getActivity().getApplication();
        mLayoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        mTagsRecycler.setLayoutManager(mLayoutManager);

        RandFragmentComponent component = DaggerRandFragmentComponent
                .builder()
                .exoPlayerModule(new ExoPlayerModule(getContext(), videoUrl))
                .build();
        component.inject(this);

        mPlayerView.setPlayer(player);
        player.prepare(videoSource);

        return v;
    }

    @Override
    public void onStop() {
        super.onStop();
        player.release();
    }


    private void setWebmData(WebmQuery.Data data) {
        final WebmQuery.GetWebm getWebm = data.getWebm();
        if (getWebm != null) {
            createdAt.setText(getWebm.createdAt());
            views.setText(String.valueOf(getWebm.views()));
            final List<String> tagsList = new ArrayList<>();
            for (int i = 0; i < getWebm.tags().size(); ++i) {
                tagsList.add(getWebm.tags().get(i).name());
            }
            mTagsAdapter = new TagsAdapter(getContext(), tagsList);
            mTagsRecycler.setAdapter(mTagsAdapter);
            mTagsAdapter.SetOnItemClickListener((view, position) -> {
                tagName = mTagsAdapter.getItem(position).toLowerCase();
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                Fragment fragment = WebmListFragment.newInstance(order, tagName);
                ft.replace(R.id.container, fragment).commit();
            });
        }
    }

    private void fetchWebmDetails() {
        ApolloCall<WebmQuery.Data> webmQuery = mApplication.apolloClient()
                .query(new WebmQuery(id));
        mDisposable.add(Rx2Apollo.from(webmQuery)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribeWith(new DisposableObserver<Response<WebmQuery.Data>>() {
            @Override
            public void onNext(Response<WebmQuery.Data> dataResponse) {
                setWebmData(dataResponse.data());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        }));
    }
}
