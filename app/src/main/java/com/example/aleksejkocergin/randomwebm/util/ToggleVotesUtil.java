package com.example.aleksejkocergin.randomwebm.util;

import com.apollographql.apollo.ApolloMutationCall;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.rx2.Rx2Apollo;
import com.example.aleksejkocergin.myapplication.ToggleDislikeMutation;
import com.example.aleksejkocergin.myapplication.ToggleLikeMutation;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class ToggleVotesUtil {

    private CompositeDisposable mDisposable = new CompositeDisposable();

    public void toggleLike(String webmId, boolean hasLike, boolean hasDislike) {
        ApolloMutationCall<ToggleLikeMutation.Data> likeMutationCall =
                WebmApolloClient.getWebmApolloClient()
                    .mutate(new ToggleLikeMutation(webmId, hasLike, hasDislike));
        mDisposable.add(Rx2Apollo.from(likeMutationCall)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribeWith(new DisposableObserver<Response<ToggleLikeMutation.Data>>() {
            @Override
            public void onNext(Response<ToggleLikeMutation.Data> dataResponse) {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        }));
    }

    public void toggleDislike(String webmId, boolean hasLike, boolean hasDislike) {
        ApolloMutationCall<ToggleDislikeMutation.Data> dislikeMutationCall =
                WebmApolloClient.getWebmApolloClient()
                        .mutate(new ToggleDislikeMutation(webmId, hasLike, hasDislike));
        mDisposable.add(Rx2Apollo.from(dislikeMutationCall)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribeWith(new DisposableObserver<Response<ToggleDislikeMutation.Data>>() {
            @Override
            public void onNext(Response<ToggleDislikeMutation.Data> dataResponse) {

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
