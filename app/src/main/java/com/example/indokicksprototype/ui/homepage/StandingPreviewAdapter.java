package com.example.indokicksprototype.ui.homepage;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.StandingRow;
import com.example.indokicksprototype.model.Team;

import java.util.ArrayList;
import java.util.List;

public class StandingPreviewAdapter extends RecyclerView.Adapter<StandingPreviewAdapter.VH> {

    private final List<StandingRow> items = new ArrayList<>();

    public void setItems(List<StandingRow> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing_preview, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        StandingRow row = items.get(position);
        Team t = row.getTeam();

        h.tvRank.setText(String.valueOf(row.getRank()));
        h.tvTeamName.setText(t != null && t.getName() != null ? t.getName() : "-");
        h.tvPoints.setText(String.valueOf(row.getPoints()));

        String logo = (t != null) ? t.getLogo() : null;
        Glide.with(h.itemView.getContext())
                .load(logo)
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivLogo);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvTeamName, tvPoints;
        ImageView ivLogo;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRankPreview);
            tvTeamName = itemView.findViewById(R.id.tvTeamNamePreview);
            tvPoints = itemView.findViewById(R.id.tvPointsPreview);
            ivLogo = itemView.findViewById(R.id.ivTeamLogoPreview);
        }
    }
}
