package com.example.indokicksprototype.ui.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.NewsItem;

import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {
    private final List<NewsItem> items;
    public NewsAdapter(List<NewsItem> items) { this.items = items; }

    static class VH extends RecyclerView.ViewHolder {
        ImageView cover; TextView title; TextView summary;
        VH(@NonNull View v) {
            super(v);
            cover = v.findViewById(R.id.ivNewsCover);
            title = v.findViewById(R.id.tvNewsTitle);
            summary = v.findViewById(R.id.tvNewsSummary);
        }
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_homepage_news_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        NewsItem n = items.get(position);
        h.title.setText(n.getTitle());
        h.summary.setText(n.getSummary());
        h.cover.setImageResource(R.mipmap.ic_launcher); // placeholder
    }

    @Override public int getItemCount() { return items.size(); }
}
