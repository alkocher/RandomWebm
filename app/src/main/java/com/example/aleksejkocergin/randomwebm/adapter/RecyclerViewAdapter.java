package com.example.aleksejkocergin.randomwebm.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.aleksejkocergin.myapplication.WebmListQuery;
import com.example.aleksejkocergin.randomwebm.R;
import com.squareup.picasso.Picasso;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final int VIEW_TYPE_ITEM = 0;
    private final int VIEW_TYPE_FOOTER = 1;

    private Context context;
    private boolean showFooter = true;
    private OnItemClickListener mItemClickListener;
    private List<WebmListQuery.GetWebmList> webmList = Collections.emptyList();

    // Constructor
    public RecyclerViewAdapter(Context context, List<WebmListQuery.GetWebmList> webmList) {
        this.context = context;
        this.webmList = webmList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View itemView = layoutInflater.inflate(R.layout.item_webm_list, parent, false);
            return new MyHolder(itemView);
        } else if (viewType == VIEW_TYPE_FOOTER) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            View itemView = layoutInflater.inflate(R.layout.item_loader_layout, parent, false);
            return new LoadingViewHolder(itemView);
        }
        throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof MyHolder) {
            MyHolder myHolder = (MyHolder) holder;
            myHolder.id = webmList.get(position).id();
            myHolder.createdAt.setText(webmList.get(position).createdAt());
            myHolder.numViews.setText(String.valueOf(webmList.get(position).views()));
            myHolder.likeCount.setText(String.valueOf(webmList.get(position).likes()));
            myHolder.dislikeCount.setText(String.valueOf(webmList.get(position).dislikes()));
            Picasso.with(context).load(webmList.get(position).previewUrl()).into(myHolder.imageView);
        } else if (holder instanceof LoadingViewHolder) {
            LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
            loadingViewHolder.progressBar.setIndeterminate(true);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (showFooter) {
            if (isPositionFooter(position)) {
                return VIEW_TYPE_FOOTER;
            }
            return VIEW_TYPE_ITEM;
        } else {
            return VIEW_TYPE_ITEM;
        }
    }

    private boolean isPositionFooter(int position) {
        return position == webmList.size();
    }

    public void hideFooter() {
        showFooter = false;
        notifyDataSetChanged();
    }

    public void showFooter() {
        showFooter = true;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        if (showFooter) {
            return webmList.size() + 1; // Add footer
        } else {
            return webmList.size();
        }
    }

    public void clear() {
        webmList.clear();
        notifyDataSetChanged();
    }

    public void addWebms(List<WebmListQuery.GetWebmList> webmList) {
        this.webmList.addAll(webmList);
        notifyDataSetChanged();
    }

    public class MyHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.createdAt) TextView createdAt;
        @BindView(R.id.num_views) TextView numViews;
        @BindView(R.id.image_item) ImageView imageView;
        @BindView(R.id.like_count) TextView likeCount;
        @BindView(R.id.dislike_count) TextView dislikeCount;

        String id;

        MyHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);
            ButterKnife.bind(this, itemView);
        }

        @Override
        public void onClick(View view) {
            if (mItemClickListener != null) {
                mItemClickListener.onItemClick(view, getAdapterPosition(), id);
            }
        }
    }

    class LoadingViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.progress_bar) ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position, String id);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }
}
