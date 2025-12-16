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
import com.example.indokicksprototype.model.Club;

import java.util.ArrayList;
import java.util.List;

public class ClubsAdapter extends RecyclerView.Adapter<ClubsAdapter.VH> {

    public interface OnClubClickListener {
        void onClubClick(Club club);
    }

    private final List<Club> items = new ArrayList<>();
    private final OnClubClickListener listener;

    public ClubsAdapter(OnClubClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Club> clubs) {
        items.clear();
        if (clubs != null) items.addAll(clubs);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_club_card, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Club c = items.get(position);
        h.tvClubName.setText(c.getName());

        Glide.with(h.itemView.getContext())
                .load(c.getLogo())
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivClubLogo);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClubClick(c);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivClubLogo;
        TextView tvClubName;

        VH(@NonNull View itemView) {
            super(itemView);
            ivClubLogo = itemView.findViewById(R.id.ivClubLogo);
            tvClubName = itemView.findViewById(R.id.tvClubName);
        }
    }
}
