package com.example.aleksejkocergin.randomwebm.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.ApolloCallback;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.example.aleksejkocergin.myapplication.WebmListQuery;
import com.example.aleksejkocergin.myapplication.type.Order;
import com.example.aleksejkocergin.randomwebm.activity.PlayerActivity;
import com.example.aleksejkocergin.randomwebm.R;
import com.example.aleksejkocergin.randomwebm.RandomWebmApplication;
import com.example.aleksejkocergin.randomwebm.adapter.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import butterknife.BindView;
import butterknife.ButterKnife;


public class ListOrderFragment extends Fragment {

    private static final String TAG = ListOrderFragment.class.getSimpleName();
    public static final int PAGE_SIZE = 15;

    RandomWebmApplication application;
    RecyclerViewAdapter mAdapter;
    Handler uiHandler = new Handler(Looper.getMainLooper());
    ApolloCall<WebmListQuery.Data> webmCall;
    LinearLayoutManager mLayoutManager;

    @BindView(R.id.recycler_view) RecyclerView mRecyclerView;
    @BindView(R.id.swipe_container) SwipeRefreshLayout swipeContainer;

    private String order = "";
    private String tagName = "";
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private int currentPage = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_list, container, false);
        ButterKnife.bind(this, v);
        application = (RandomWebmApplication) getActivity().getApplication();
        order = getActivity().getIntent().getStringExtra("order");
        tagName = getActivity().getIntent().getStringExtra("tagName");
        mLayoutManager = new LinearLayoutManager(getContext());
        mAdapter = new RecyclerViewAdapter(getContext(), new ArrayList<WebmListQuery.GetWebmList>());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int visibleItemCount = mLayoutManager.getChildCount();
                int totalItemCount = mLayoutManager.getItemCount();
                int firstVisibleItemPosition = mLayoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0 && totalItemCount >= PAGE_SIZE) {
                        mAdapter.showFooter();
                        getNextPage();
                    }
                }
            }
        });
        mAdapter.SetOnItemClickListener(new RecyclerViewAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, String id) {
                Intent intent = new Intent(getActivity(), PlayerActivity.class);
                intent.putExtra("id", id);
                startActivity(intent);
            }
        });
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mAdapter.hideFooter();
                firstLoadItems();
            }
        });
        swipeContainer.setColorSchemeResources(
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        firstLoadItems();
        return v;
    }

    @Override
    public void onPause() {
        swipeContainer.clearAnimation();
        super.onPause();
    }

    private ApolloCall.Callback<WebmListQuery.Data> fetchDataCallback = new ApolloCallback<>(new ApolloCall.Callback<WebmListQuery.Data>(){
        @Override
        public void onResponse(@Nonnull Response<WebmListQuery.Data> response) {
            if (responseWebmList(response).size() == 0) {
                isLastPage = true;
                mAdapter.hideFooter();
                mAdapter.notifyDataSetChanged();
            }
            mAdapter.addWebms(responseWebmList(response));
            isLoading = false;
            swipeContainer.setRefreshing(false);
        }
        @Override
        public void onFailure(@Nonnull ApolloException e) {
            Log.e(TAG, e.getMessage(), e);
        }
    }, uiHandler);


    private List<WebmListQuery.GetWebmList> responseWebmList(Response<WebmListQuery.Data> response) {
        List<WebmListQuery.GetWebmList> webmList = new ArrayList<>();
        final WebmListQuery.Data responseData = response.data();

        if (responseData == null) {
            return Collections.emptyList();
        }
        final List<WebmListQuery.GetWebmList> webms = responseData.getWebmList();
        if (webms != null) {
            if (webms.size() > 0) {
                webmList.addAll(webms);
            }
        }
        return webmList;
    }

    private void firstLoadItems() {
        currentPage = 1;
        mAdapter.clear();
        final WebmListQuery webmListQuery = WebmListQuery.builder()
                .page(currentPage)
                .pageSize(PAGE_SIZE)
                .order(Order.valueOf(order))
                .tagName(tagName)
                .build();
        webmCall = application.apolloClient()
                .query(webmListQuery);
        webmCall.enqueue(fetchDataCallback);
    }

    private void getNextPage() {
        isLoading = true;
        currentPage += 1;
        final WebmListQuery webmListQuery = WebmListQuery.builder()
                .page(currentPage)
                .pageSize(PAGE_SIZE)
                .order(Order.valueOf(order))
                .tagName(tagName)
                .build();
        webmCall = application.apolloClient()
                .query(webmListQuery);
        webmCall.enqueue(fetchDataCallback);
    }
}
