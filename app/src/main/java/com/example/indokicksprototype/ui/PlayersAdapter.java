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
import com.example.indokicksprototype.model.PlayerSimple;

import java.util.ArrayList;
import java.util.List;

public class PlayersAdapter extends RecyclerView.Adapter<PlayersAdapter.VH> {

    private final List<PlayerSimple> items = new ArrayList<>();

    public void setItems(List<PlayerSimple> players) {
        items.clear();
        if (players != null) items.addAll(players);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_player_row, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PlayerSimple p = items.get(position);
        h.tvName.setText(p.getName());

        String extra = "";
        if (p.getNumber() != null) extra += "#" + p.getNumber() + "  ";
        if (p.getPosition() != null) extra += p.getPosition();
        h.tvInfo.setText(extra.trim());

        String sub = "";
        if (p.getAge() != null) sub += p.getAge() + " yrs";
        if (p.getNationality() != null) {
            if (!sub.isEmpty()) sub += " • ";
            sub += p.getNationality();
        }
        h.tvSubInfo.setText(sub);

        Glide.with(h.itemView.getContext())
                .load(p.getPhoto())
                .placeholder(R.mipmap.ic_launcher)
                .into(h.ivPhoto);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        TextView tvName, tvInfo, tvSubInfo;

        VH(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.ivPlayerPhoto);
            tvName = itemView.findViewById(R.id.tvPlayerName);
            tvInfo = itemView.findViewById(R.id.tvPlayerInfo);
            tvSubInfo = itemView.findViewById(R.id.tvPlayerSubInfo);
        }
    }
}
