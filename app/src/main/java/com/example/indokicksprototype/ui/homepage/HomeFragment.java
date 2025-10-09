package com.example.indokicksprototype.ui.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.Fixture;
import com.example.indokicksprototype.model.Team;
import com.example.indokicksprototype.model.NewsItem;
import com.google.android.material.card.MaterialCardView;

import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_homepage, container, false);

        // --- HERO MATCH (dummy) ---
        Team home = new Team(1, "Persija", "");
        Team away = new Team(2, "Persib", "");
        Fixture hero = new Fixture(101, System.currentTimeMillis() + 60 * 60 * 1000, "NS", home, away, 1);

        TextView tvLeague = v.findViewById(R.id.tvLeague);
        TextView tvStatusTime = v.findViewById(R.id.tvStatusTime);
        TextView tvHome = v.findViewById(R.id.tvHomeName);
        TextView tvAway = v.findViewById(R.id.tvAwayName);
        ImageView ivHome = v.findViewById(R.id.ivHomeLogo);
        ImageView ivAway = v.findViewById(R.id.ivAwayLogo);
        MaterialCardView btnDetails = v.findViewById(R.id.btnSeeDetails);

        tvLeague.setText("Liga 1 - Hero Match");
        tvHome.setText(hero.getHome().getName());
        tvAway.setText(hero.getAway().getName());
        ivHome.setImageResource(R.mipmap.ic_launcher);
        ivAway.setImageResource(R.mipmap.ic_launcher);

        if ("LIVE".equalsIgnoreCase(hero.getStatus()) || "1H".equals(hero.getStatus()) || "2H".equals(hero.getStatus())) {
            tvStatusTime.setText("LIVE • " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(hero.getTimestamp())));
        } else if ("NS".equalsIgnoreCase(hero.getStatus())) {
            tvStatusTime.setText("Kickoff " + new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(hero.getTimestamp())));
        } else {
            tvStatusTime.setText(hero.getStatus());
        }

        btnDetails.setOnClickListener(view ->
                Navigation.findNavController(view).navigate(R.id.nav_matches)
        );

        // --- NEWS LIST (dummy) ---
        RecyclerView rvNews = v.findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        List<NewsItem> news = new ArrayList<>();
        news.add(new NewsItem("Jelang Big Match Persija vs Persib",
                "Duel klasik bertajuk el clasico Indonesia akan tersaji akhir pekan ini.", ""));
        news.add(new NewsItem("Update Bursa Transfer",
                "Beberapa klub Liga 1 meresmikan pemain baru untuk paruh musim.", ""));
        news.add(new NewsItem("5 Pemain Muda Paling Menonjol",
                "Statistik menunjukkan kontribusi signifikan pada 5 laga terakhir.", ""));
        rvNews.setAdapter(new NewsAdapter(news));

        return v;
    }
}
