package com.example.indokicksprototype.ui.homepage;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.indokicksprototype.R;
import com.example.indokicksprototype.network.NewsApiResponse;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {

    private final List<NewsApiResponse.NewsItemDto> items = new ArrayList<>();

    public void setItems(List<NewsApiResponse.NewsItemDto> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_homepage_news_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NewsApiResponse.NewsItemDto item = items.get(position);

        h.tvTitle.setText(item.title != null ? item.title : "-");

        Glide.with(h.itemView.getContext())
                .load(item.thumbnail)
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivThumb);

        h.itemView.setOnClickListener(v -> openUrl(v.getContext(), item.url));
    }

    private void openUrl(Context ctx, String url) {
        if (url == null || url.trim().isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        ctx.startActivity(i);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvTitle;

        VH(@NonNull View itemView) {
            super(itemView);
            // Sesuaikan ID dengan layout item_homepage_news_card.xml kamu
            ivThumb = itemView.findViewById(R.id.ivNewsThumb);
            tvTitle = itemView.findViewById(R.id.tvNewsTitle);
        }
    }
}
