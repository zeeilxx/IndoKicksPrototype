package com.example.indokicksprototype.ui;

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

public class StandingsAdapter extends RecyclerView.Adapter<StandingsAdapter.VH> {

    private final List<StandingRow> items = new ArrayList<>();

    public void setItems(List<StandingRow> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_standing_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        StandingRow row = items.get(position);
        Team t = row.getTeam();

        h.tvRank.setText(String.valueOf(row.getRank()));
        h.tvTeamName.setText(t.getName());
        h.tvPlayed.setText(String.valueOf(row.getPlayed()));
        h.tvWin.setText(String.valueOf(row.getWin()));
        h.tvDraw.setText(String.valueOf(row.getDraw()));
        h.tvLose.setText(String.valueOf(row.getLose()));
        h.tvGoals.setText(row.getGoalsFor() + "-" + row.getGoalsAgainst());

        int gd = row.getGoalsDiff();
        String gdText = (gd >= 0 ? "+" : "") + gd;
        h.tvGoalDiff.setText(gdText);

        h.tvPoints.setText(String.valueOf(row.getPoints()));

        Glide.with(h.itemView.getContext())
                .load(t.getLogo())
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivTeamLogo);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvRank, tvTeamName, tvPlayed, tvWin, tvDraw, tvLose, tvGoals, tvGoalDiff, tvPoints;
        ImageView ivTeamLogo;

        VH(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvTeamName = itemView.findViewById(R.id.tvTeamName);
            tvPlayed = itemView.findViewById(R.id.tvPlayed);
            tvWin = itemView.findViewById(R.id.tvWin);
            tvDraw = itemView.findViewById(R.id.tvDraw);
            tvLose = itemView.findViewById(R.id.tvLose);
            tvGoals = itemView.findViewById(R.id.tvGoals);
            tvGoalDiff = itemView.findViewById(R.id.tvGoalDiff);
            tvPoints = itemView.findViewById(R.id.tvPoints);
            ivTeamLogo = itemView.findViewById(R.id.ivTeamLogo);
        }
    }
}
