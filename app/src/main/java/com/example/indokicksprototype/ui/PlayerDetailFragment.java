package com.example.indokicksprototype.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.PlayerDetail;

public class PlayerDetailFragment extends Fragment {

    public static final String ARG_PLAYER_DETAIL = "playerDetail";

    private ImageView ivPhoto;
    private TextView tvName;
    private TextView tvMeta;
    private TextView tvStats;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_player_detail, container, false);
    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ivPhoto = v.findViewById(R.id.ivPlayerPhoto);
        tvName = v.findViewById(R.id.tvPlayerName);
        tvMeta = v.findViewById(R.id.tvPlayerMeta);
        tvStats = v.findViewById(R.id.tvPlayerStats);

        Bundle args = getArguments();
        if (args == null) {
            tvName.setText("Data pemain tidak ditemukan");
            return;
        }

        PlayerDetail p = args.getParcelable(ARG_PLAYER_DETAIL);
        if (p == null) {
            tvName.setText("Data pemain tidak ditemukan");
            return;
        }

        tvName.setText(p.getName() != null ? p.getName() : "-");

        // meta singkat
        StringBuilder meta = new StringBuilder();
        if (p.getNationality() != null) meta.append(p.getNationality());
        if (p.getAge() != null) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append("Age ").append(p.getAge());
        }
        if (!TextUtils.isEmpty(p.getPosition())) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append(p.getPosition());
        }
        if (p.getNumber() != null) {
            if (meta.length() > 0) meta.append(" • ");
            meta.append("#").append(p.getNumber());
        }
        tvMeta.setText(meta.length() > 0 ? meta.toString() : "-");

        // stats detail
        StringBuilder stats = new StringBuilder();

        appendLine(stats, "Appearences", p.getAppearences());
        appendLine(stats, "Lineups", p.getLineups());
        appendLine(stats, "Minutes", p.getMinutes());
        appendLine(stats, "Rating", p.getRating());

        if (!TextUtils.isEmpty(p.getBirthDate()) || !TextUtils.isEmpty(p.getBirthPlace())) {
            String birth = "";
            if (!TextUtils.isEmpty(p.getBirthDate())) birth += p.getBirthDate();
            if (!TextUtils.isEmpty(p.getBirthPlace())) birth += (birth.isEmpty() ? "" : " • ") + p.getBirthPlace();
            appendLine(stats, "Birth", birth);
        }

        if (!TextUtils.isEmpty(p.getHeight())) appendLine(stats, "Height", p.getHeight());
        if (!TextUtils.isEmpty(p.getWeight())) appendLine(stats, "Weight", p.getWeight());

        tvStats.setText(stats.length() > 0 ? stats.toString() : "-");

        Glide.with(requireContext())
                .load(p.getPhoto())
                .placeholder(R.mipmap.ic_launcher)
                .into(ivPhoto);
    }

    private void appendLine(StringBuilder sb, String label, Integer value) {
        if (value == null) return;
        sb.append(label).append(": ").append(value).append("\n");
    }

    private void appendLine(StringBuilder sb, String label, String value) {
        if (TextUtils.isEmpty(value)) return;
        sb.append(label).append(": ").append(value).append("\n");
    }
}
