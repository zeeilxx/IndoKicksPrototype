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
import com.example.indokicksprototype.model.Fixture;
import com.example.indokicksprototype.model.Team;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchesAdapter extends RecyclerView.Adapter<MatchesAdapter.VH> {

    public interface OnMatchClickListener {
        void onMatchClick(Fixture fixture);
    }

    private final List<Fixture> items = new ArrayList<>();
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, dd MMM HH:mm", new Locale("id", "ID"));
    private final OnMatchClickListener listener;

    public MatchesAdapter(OnMatchClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Fixture> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    public void addItems(List<Fixture> moreItems) {
        if (moreItems == null || moreItems.isEmpty()) return;
        int start = items.size();
        items.addAll(moreItems);
        notifyItemRangeInserted(start, moreItems.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_match_fixture, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Fixture f = items.get(position);
        Team home = f.getHome();
        Team away = f.getAway();

        long millis = f.getTimestamp() * 1000L;
        String dateTime = dateFormat.format(new Date(millis));
        h.tvDateTime.setText(dateTime);

        h.tvStatus.setText(f.getStatus());

        h.tvHomeTeam.setText(home.getName());
        h.tvAwayTeam.setText(away.getName());

        Integer gh = f.getGoalsHome();
        Integer ga = f.getGoalsAway();
        String score = (gh != null && ga != null) ? (gh + " - " + ga) : "-";
        h.tvScore.setText(score);

        if (home.getLogo() != null && !home.getLogo().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(home.getLogo())
                    .placeholder(R.mipmap.ic_launcher)
                    .into(h.ivHomeLogo);
        } else {
            h.ivHomeLogo.setImageResource(R.mipmap.ic_launcher);
        }

        if (away.getLogo() != null && !away.getLogo().isEmpty()) {
            Glide.with(h.itemView.getContext())
                    .load(away.getLogo())
                    .placeholder(R.mipmap.ic_launcher)
                    .into(h.ivAwayLogo);
        } else {
            h.ivAwayLogo.setImageResource(R.mipmap.ic_launcher);
        }

        h.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onMatchClick(f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public Fixture getItem(int position) {
        return items.get(position);
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvDateTime, tvStatus, tvHomeTeam, tvAwayTeam, tvScore;
        ImageView ivHomeLogo, ivAwayLogo;

        VH(@NonNull View itemView) {
            super(itemView);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvHomeTeam = itemView.findViewById(R.id.tvHomeTeam);
            tvAwayTeam = itemView.findViewById(R.id.tvAwayTeam);
            tvScore = itemView.findViewById(R.id.tvScore);
            ivHomeLogo = itemView.findViewById(R.id.ivHomeLogo);
            ivAwayLogo = itemView.findViewById(R.id.ivAwayLogo);
        }
    }
}
