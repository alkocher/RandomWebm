package com.example.aleksejkocergin.randomwebm.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmListQuery;
import com.example.aleksejkocergin.myapplication.type.Order;
import com.example.aleksejkocergin.randomwebm.activity.PlayerActivity;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.adapter.WebmRecyclerAdapter;
import com.example.aleksejkocergin.randomwebm.util.WebmApolloClient;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class WebmListFragment extends Fragment {

    private static final String TAG = WebmListFragment.class.getSimpleName();
    private static final int PAGE_SIZE = 10;
    private String order = "";
    private String tagName = "";
    private List<String> likedWebms;
    private int currentPage = 0;
    boolean userScrolled = false;
    boolean isLastPage = false;

    private WebmRecyclerAdapter webmAdapter;
    private LinearLayoutManager mLayoutManager;
    private final CompositeDisposable disposables = new CompositeDisposable();

    @BindView(R.id.recycler_view)
    RecyclerView mRecyclerView;
    @BindView(R.id.swipe_container)
    SwipeRefreshLayout swipeContainer;
    @BindView(R.id.bottom_progress)
    RelativeLayout bottomLayout;
    @BindView(R.id.error_no_results)
    LinearLayout errorNoResults;
    @BindView(R.id.error_check_connection)
    LinearLayout errorCheckConnection;

    public static WebmListFragment newInstance(String order, String tagName) {
        WebmListFragment webmListFragment = new WebmListFragment();
        Bundle args = new Bundle();
        args.putString("order", order);
        args.putString("tagName", tagName);
        webmListFragment.setArguments(args);
        return webmListFragment;
    }

    public static WebmListFragment newInstance(String order, String tagName, ArrayList<String> likedWebms) {
        WebmListFragment webmListFragment = new WebmListFragment();
        Bundle args = new Bundle();
        args.putString("order", order);
        args.putString("tagName", tagName);
        args.putStringArrayList("likedWebm", likedWebms);
        webmListFragment.setArguments(args);
        return webmListFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        ButterKnife.bind(this, v);

        order = getArguments().getString("order");
        tagName = getArguments().getString("tagName");
        likedWebms = getArguments().getStringArrayList("likedWebm");
        mLayoutManager = new LinearLayoutManager(getActivity());

        webmAdapter = new WebmRecyclerAdapter(getActivity(), new ArrayList<>());
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setAdapter(webmAdapter);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                    userScrolled = true;
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int pastVisibleItems = mLayoutManager.findFirstVisibleItemPosition();
                if (!isLastPage) {
                    if (userScrolled && (visibleItemCount + pastVisibleItems) == totalItemCount - 1) {
                        userScrolled = false;
                        fetchWebmList();
                    }
                }
            }
        });

        webmAdapter.SetOnItemClickListener((view, position, id) -> {
            Intent intent = new Intent(getActivity(), PlayerActivity.class);
            intent.putExtra("id", id);
            startActivity(intent);
        });

        swipeContainer.setOnRefreshListener(this::fetchWebmList);

        swipeContainer.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        fetchWebmList();
        return v;
    }

    @Override
    public void onPause() {
        swipeContainer.clearAnimation();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.dispose();
    }

    private void setWebmData(WebmListQuery.Data data) {
        if (data != null) {
            webmAdapter.addWebms(data.getWebmList());
        }
        if (mLayoutManager.getItemCount() == 0){
            errorNoResults.setVisibility(View.VISIBLE);
        } else {
            errorNoResults.setVisibility(View.GONE);
        }
    }

    private void fetchWebmList() {
        errorNoResults.setVisibility(View.GONE);
        bottomLayout.setVisibility(View.VISIBLE);
        ApolloCall<WebmListQuery.Data> webmListQuery = WebmApolloClient.getWebmApolloClient()
                .query(new WebmListQuery(
                        PAGE_SIZE,
                        Order.valueOf(order),
                        ++currentPage,
                        tagName,
                        likedWebms));
        disposables.add(Rx2Apollo.from(webmListQuery)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(new DisposableObserver<Response<WebmListQuery.Data>>() {
                @Override
                public void onNext(Response<WebmListQuery.Data> dataResponse) {
                    setWebmData(dataResponse.data());
                }

                @Override
                public void onError(Throwable e) {
                    bottomLayout.setVisibility(View.GONE);
                    swipeContainer.setRefreshing(false);
                    errorCheckConnection.setVisibility(View.VISIBLE);
                    Log.e(TAG, e.getMessage(), e);
                }

                @Override
                public void onComplete() {
                    swipeContainer.setRefreshing(false);
                    bottomLayout.setVisibility(View.GONE);
                }
            }));
    }
}
