package com.example.aleksejkocergin.randomwebm.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.aleksejkocergin.myapplication.WebmListQuery;
import com.example.aleksejkocergin.randomwebm.R;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WebmRecyclerAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private OnItemClickListener mItemClickListener;
    private Context context;
    private List<WebmListQuery.GetWebmList> webmList = Collections.emptyList();

    public WebmRecyclerAdapter(Context context, List<WebmListQuery.GetWebmList> webmList) {
        this.context = context;
        this.webmList = webmList;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        final View itemView = layoutInflater.inflate(R.layout.item_webm_list, parent, false);
        return new WebmHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        final WebmListQuery.GetWebmList webmInfo = this.webmList.get(position);
        WebmHolder webmHolder = (WebmHolder) holder;
        webmHolder.id = webmInfo.id();
        webmHolder.createdAt.setText(webmInfo.createdAt());
        webmHolder.numViews.setText(String.valueOf(webmInfo.views()));
        webmHolder.likeCount.setText(String.valueOf(webmInfo.likes()));
        webmHolder.dislikeCount.setText(String.valueOf(webmInfo.dislikes()));
        Glide.with(context).load(webmInfo.previewUrl()).into(webmHolder.imageView);
    }

    @Override
    public int getItemCount() {
        return (null != webmList ? webmList.size() : 0);
    }

    public void addWebms(List<WebmListQuery.GetWebmList> webmList) {
        this.webmList.addAll(webmList);
        notifyDataSetChanged();
    }

    class WebmHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        @BindView(R.id.createdAt)
        TextView createdAt;
        @BindView(R.id.num_views)
        TextView numViews;
        @BindView(R.id.image_item)
        ImageView imageView;
        @BindView(R.id.like_count)
        TextView likeCount;
        @BindView(R.id.dislike_count)
        TextView dislikeCount;

        String id;

        private WebmHolder(View itemView) {
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
    public interface OnItemClickListener {
        void onItemClick(View view, int position, String id);
    }

    public void SetOnItemClickListener(final OnItemClickListener mItemClickListener) {
        this.mItemClickListener = mItemClickListener;
    }
}
