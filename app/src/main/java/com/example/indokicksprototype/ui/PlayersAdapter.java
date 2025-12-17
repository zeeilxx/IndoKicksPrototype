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
import com.example.indokicksprototype.model.PlayerDetail;

import java.util.ArrayList;
import java.util.List;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.VH> {

    public interface OnPlayerClick {
        void onClick(PlayerDetail player);
    }

    private final List<PlayerDetail> items = new ArrayList<>();
    private final OnPlayerClick listener;

    public PlayersAdapter(OnPlayerClick listener) {
        this.listener = listener;
    }

    public void setItems(List<PlayerDetail> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_simple, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PlayerDetail p = items.get(position);

        h.tvName.setText(p.getName() != null ? p.getName() : "-");

        String meta = "";
        if (p.getPosition() != null) meta += p.getPosition();
        if (p.getNumber() != null) meta += (meta.isEmpty() ? "" : " • ") + "#" + p.getNumber();
        if (p.getAge() != null) meta += (meta.isEmpty() ? "" : " • ") + "Age " + p.getAge();
        h.tvMeta.setText(meta.isEmpty() ? "-" : meta);

        Glide.with(h.itemView.getContext())
                .load(p.getPhoto())
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivPhoto);

        h.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvMeta;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            tvName = itemView.findViewById(R.id.tvPlayerName);
            tvMeta = itemView.findViewById(R.id.tvPlayerMeta);
        }
    }
}
