package com.example.aleksejkocergin.randomwebm.util;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.WebmQuery;
import com.example.aleksejkocergin.randomwebm.interfaces.WebmData;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


public class WebmDetailsFetcher {

    private CompositeDisposable disposable = new CompositeDisposable();
    private WebmData webmData;

    private String webmId = "";

    public WebmDetailsFetcher(WebmData webmData) {
        this.webmData = webmData;
    }

    public WebmDetailsFetcher(WebmData webmData, String webmId) {
        this.webmData = webmData;
        this.webmId = webmId;
    }

    public void fetchWebmDetails() {
        ApolloCall<WebmQuery.Data> webmQuery = WebmApolloClient.getWebmApolloClient()
                .query(new WebmQuery(webmId));
        disposable.add(Rx2Apollo.from(webmQuery)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribeWith(new DisposableObserver<Response<WebmQuery.Data>>() {
            @Override
            public void onNext(Response<WebmQuery.Data> dataResponse) {
                webmData.render(dataResponse.data());
            }

            @Override
            public void onError(Throwable e) {
                webmData.showErrorSnackbar();
            }

            @Override
            public void onComplete() {
            }
        }));

    }
}
