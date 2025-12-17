package com.example.indokicksprototype.ui.homepage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.StandingRow;
import com.example.indokicksprototype.model.Team;
import com.example.indokicksprototype.network.ApiClient;
import com.example.indokicksprototype.network.ApiClientBackend;
import com.example.indokicksprototype.network.ApiClientNews;
import com.example.indokicksprototype.network.NewsApiResponse;
import com.example.indokicksprototype.network.StandingsApiResponse;
import com.example.indokicksprototype.network.TeamsApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    // Standings UI
    private Spinner spinnerSeasonHome;
    private ProgressBar progressStandings;
    private TextView tvErrorStandings;
    private StandingPreviewAdapter standingsPreviewAdapter;

    // News UI
    private ProgressBar progressNews;
    private TextView tvNewsError;
    private NewsAdapter newsAdapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_homepage, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // ---------- Standings section ----------
        spinnerSeasonHome = view.findViewById(R.id.spinnerSeasonHome);
        progressStandings = view.findViewById(R.id.progressStandingsHome);
        tvErrorStandings = view.findViewById(R.id.tvErrorStandingsHome);

        RecyclerView rvStandings = view.findViewById(R.id.rvStandingsPreview);
        rvStandings.setLayoutManager(new LinearLayoutManager(requireContext()));
        standingsPreviewAdapter = new StandingPreviewAdapter();
        rvStandings.setAdapter(standingsPreviewAdapter);

        setupSeasonSpinner();

        // ---------- News section ----------
        progressNews = view.findViewById(R.id.progressNews);
        tvNewsError = view.findViewById(R.id.tvNewsError);

        RecyclerView rvNews = view.findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(requireContext()));
        newsAdapter = new NewsAdapter();
        rvNews.setAdapter(newsAdapter);

        // Default load
        spinnerSeasonHome.setSelection(4); // 2025
        loadStandingsPreview(getSelectedSeason());
        loadNews(); // <-- ini yang kemungkinan hilang sebelumnya
    }

    // ===================== SEASON SPINNER =====================

    private void setupSeasonSpinner() {
        List<Integer> seasons = new ArrayList<>();
        seasons.add(2021);
        seasons.add(2022);
        seasons.add(2023);
        seasons.add(2024);
        seasons.add(2025);

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                seasons
        );
        spinnerSeasonHome.setAdapter(adapter);

        spinnerSeasonHome.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                loadStandingsPreview(getSelectedSeason());
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    private int getSelectedSeason() {
        Object o = spinnerSeasonHome.getSelectedItem();
        if (o instanceof Integer) return (Integer) o;
        return 2025;
    }

    // ===================== STANDINGS (TOP 5) =====================

    private void loadStandingsPreview(int season) {
        showStandingsLoading(true);
        tvErrorStandings.setVisibility(View.GONE);

        boolean useBackend = (season == 2024 || season == 2025);

        Call<StandingsApiResponse> call = useBackend
                ? ApiClientBackend.getService().getStandings(LIGA_1_ID, season)
                : ApiClient.getService().getStandings(LIGA_1_ID, season);

        call.enqueue(new Callback<StandingsApiResponse>() {
            @Override
            public void onResponse(Call<StandingsApiResponse> call, Response<StandingsApiResponse> response) {
                if (!isAdded()) return;
                showStandingsLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
                    showStandingsError("Gagal memuat standings.");
                    standingsPreviewAdapter.setItems(null);
                    return;
                }

                List<StandingsApiResponse.ResponseItem> raw = response.body().getResponse();
                List<StandingRow> rows = mapStandings(raw);

                if (rows == null || rows.isEmpty()) {
                    showStandingsError("Data klasemen kosong.");
                    standingsPreviewAdapter.setItems(null);
                    return;
                }

                List<StandingRow> top5 = rows.size() > 5 ? rows.subList(0, 5) : rows;

                if (useBackend) {
                    overlayLogosFromExternal(top5);
                } else {
                    standingsPreviewAdapter.setItems(top5);
                }
            }

            @Override
            public void onFailure(Call<StandingsApiResponse> call, Throwable t) {
                if (!isAdded()) return;
                showStandingsLoading(false);
                showStandingsError("Error jaringan standings: " + t.getMessage());
                standingsPreviewAdapter.setItems(null);
            }
        });
    }

    private void overlayLogosFromExternal(List<StandingRow> topRows) {
        // Logo tetap dari API luar (season 2023)
        ApiClient.getService().getTeams(LIGA_1_ID, 2023).enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                if (!isAdded()) return;

                Map<Integer, String> logoById = new HashMap<>();
                Map<String, String> logoByName = new HashMap<>();

                if (response.isSuccessful() && response.body() != null && response.body().getResponse() != null) {
                    for (TeamsApiResponse.ResponseItem r : response.body().getResponse()) {
                        if (r == null || r.team == null) continue;
                        logoById.put(r.team.id, r.team.logo);
                        if (r.team.name != null) {
                            logoByName.put(r.team.name.trim().toLowerCase(), r.team.logo);
                        }
                    }
                }

                for (StandingRow row : topRows) {
                    Team t = row.getTeam();
                    if (t == null) continue;

                    String logo = null;

                    // by id
                    logo = logoById.get(t.getId());

                    // fallback by name
                    if (logo == null && t.getName() != null) {
                        logo = logoByName.get(t.getName().trim().toLowerCase());
                    }

                    if (logo != null) t.setLogo(logo);
                }

                standingsPreviewAdapter.setItems(topRows);
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
                if (!isAdded()) return;
                standingsPreviewAdapter.setItems(topRows);
            }
        });
    }

    private List<StandingRow> mapStandings(List<StandingsApiResponse.ResponseItem> raw) {
        List<StandingRow> out = new ArrayList<>();
        if (raw == null || raw.isEmpty()) return out;

        StandingsApiResponse.ResponseItem item0 = raw.get(0);
        if (item0 == null || item0.league == null || item0.league.standings == null || item0.league.standings.isEmpty())
            return out;

        List<StandingsApiResponse.Standing> list = item0.league.standings.get(0);
        if (list == null) return out;

        for (StandingsApiResponse.Standing s : list) {
            if (s == null || s.team == null || s.all == null || s.all.goals == null) continue;

            Team team = new Team(s.team.id, s.team.name, s.team.logo);

            StandingRow row = new StandingRow(
                    s.rank,
                    team,
                    s.all.played,
                    s.all.win,
                    s.all.draw,
                    s.all.lose,
                    s.all.goals.goalsFor,
                    s.all.goals.goalsAgainst,
                    s.goalsDiff,
                    s.points,
                    s.form
            );

            out.add(row);
        }

        return out;
    }

    private void showStandingsLoading(boolean loading) {
        progressStandings.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showStandingsError(String msg) {
        tvErrorStandings.setVisibility(View.VISIBLE);
        tvErrorStandings.setText(msg);
    }

    // ===================== NEWS =====================

    private void loadNews() {
        showNewsLoading(true);
        tvNewsError.setVisibility(View.GONE);

        ApiClientNews.getService().getNews().enqueue(new Callback<NewsApiResponse>() {
            @Override
            public void onResponse(Call<NewsApiResponse> call, Response<NewsApiResponse> response) {
                if (!isAdded()) return;
                showNewsLoading(false);

                if (!response.isSuccessful() || response.body() == null) {
                    showNewsError("Gagal memuat news.");
                    newsAdapter.setItems(null);
                    return;
                }

                NewsApiResponse body = response.body();
                if (!body.success || body.data == null || body.data.isEmpty()) {
                    showNewsError("News kosong.");
                    newsAdapter.setItems(null);
                    return;
                }

                newsAdapter.setItems(body.data);
            }

            @Override
            public void onFailure(Call<NewsApiResponse> call, Throwable t) {
                if (!isAdded()) return;
                showNewsLoading(false);
                showNewsError("Error jaringan news: " + t.getMessage());
                newsAdapter.setItems(null);
            }
        });
    }

    private void showNewsLoading(boolean loading) {
        progressNews.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showNewsError(String msg) {
        tvNewsError.setVisibility(View.VISIBLE);
        tvNewsError.setText(msg);
    }
}
