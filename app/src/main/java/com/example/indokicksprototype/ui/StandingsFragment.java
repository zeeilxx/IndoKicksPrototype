package com.example.indokicksprototype.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
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
import com.example.indokicksprototype.network.StandingsApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StandingsFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    private ProgressBar progressBar;
    private TextView tvError;
    private Spinner spinnerSeason;
    private StandingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_standings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressStandings);
        tvError = view.findViewById(R.id.tvErrorStandings);
        spinnerSeason = view.findViewById(R.id.spinnerSeason);
        RecyclerView rv = view.findViewById(R.id.rvStandings);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StandingsAdapter();
        rv.setAdapter(adapter);

        setupSeasonFilter();
    }

    private void setupSeasonFilter() {
        Integer[] seasons = {2021, 2022, 2023};

        ArrayAdapter<Integer> seasonAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                seasons
        );
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeason.setAdapter(seasonAdapter);

        // default ke 2023 (index 2)
        spinnerSeason.setSelection(2);

        spinnerSeason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer season = (Integer) spinnerSeason.getSelectedItem();
                if (season == null) return;

                // biar tidak 2x call aneh saat pertama kali
                if (first) {
                    first = false;
                }
                loadStandings(season);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadStandings(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        Call<StandingsApiResponse> call = ApiClient.getService()
                .getStandings(LIGA_1_ID, season);

        call.enqueue(new Callback<StandingsApiResponse>() {
            @Override
            public void onResponse(Call<StandingsApiResponse> call, Response<StandingsApiResponse> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null ||
                        response.body().getResponse().isEmpty()) {
                    showError("Gagal memuat klasemen (" + response.code() + ")");
                    return;
                }

                StandingsApiResponse.ResponseItem item = response.body().getResponse().get(0);
                if (item.league == null || item.league.standings == null ||
                        item.league.standings.isEmpty()) {
                    showError("Data klasemen tidak tersedia.");
                    return;
                }

                List<StandingsApiResponse.Standing> standingList = item.league.standings.get(0);
                List<StandingRow> rows = new ArrayList<>();

                for (StandingsApiResponse.Standing s : standingList) {
                    if (s.team == null || s.all == null || s.all.goals == null) continue;

                    Team team = new Team(
                            s.team.id,
                            s.team.name,
                            s.team.logo
                    );

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
                    rows.add(row);
                }

                if (rows.isEmpty()) {
                    showError("Data klasemen tidak tersedia.");
                } else {
                    adapter.setItems(rows);
                }
            }

            @Override
            public void onFailure(Call<StandingsApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Terjadi kesalahan jaringan: " + t.getMessage());
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
