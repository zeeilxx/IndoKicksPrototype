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
import com.example.indokicksprototype.network.ApiClientBackend;
import com.example.indokicksprototype.network.StandingsApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StandingsFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    private Spinner spinnerSeason;
    private ProgressBar progressBar;
    private TextView tvError;
    private StandingsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_standings, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerSeason = view.findViewById(R.id.spinnerSeasonStandings); // pastikan id ini ada di XML
        progressBar = view.findViewById(R.id.progressStandings);
        tvError = view.findViewById(R.id.tvErrorStandings);

        RecyclerView rv = view.findViewById(R.id.rvStandings);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new StandingsAdapter();
        rv.setAdapter(adapter);

        setupSeasonFilter();
    }

    private void setupSeasonFilter() {
        Integer[] seasons = new Integer[]{2021, 2022, 2023, 2024, 2025};

        ArrayAdapter<Integer> seasonAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                seasons
        );
        seasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeason.setAdapter(seasonAdapter);
        spinnerSeason.setSelection(seasons.length - 1);

        spinnerSeason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer season = (Integer) spinnerSeason.getSelectedItem();
                if (season == null) return;

                if (season <= 2023) {
                    loadStandingsExternal(season);
                } else {
                    loadStandingsBackend(season);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        Integer initial = (Integer) spinnerSeason.getSelectedItem();
        if (initial != null) {
            if (initial <= 2023) loadStandingsExternal(initial);
            else loadStandingsBackend(initial);
        }
    }

    private void loadStandingsBackend(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);

        Call<StandingsApiResponse> call = ApiClientBackend.getService().getStandings(LIGA_1_ID, season);
        call.enqueue(new Callback<StandingsApiResponse>() {
            @Override
            public void onResponse(Call<StandingsApiResponse> call, Response<StandingsApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null || response.body().getResponse().isEmpty()) {
                    showError("Gagal memuat standings (backend).");
                    adapter.setItems(null);
                    return;
                }

                List<StandingRow> rows = mapStandings(response.body());
                if (rows.isEmpty()) {
                    showError("Data klasemen kosong.");
                    adapter.setItems(null);
                } else {
                    adapter.setItems(rows);
                }
            }

            @Override
            public void onFailure(Call<StandingsApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan backend: " + t.getMessage());
            }
        });
    }

    private void loadStandingsExternal(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);

        Call<StandingsApiResponse> call = ApiClient.getService().getStandings(LIGA_1_ID, season);
        call.enqueue(new Callback<StandingsApiResponse>() {
            @Override
            public void onResponse(Call<StandingsApiResponse> call, Response<StandingsApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null || response.body().getResponse().isEmpty()) {
                    showError("Gagal memuat standings (external).");
                    adapter.setItems(null);
                    return;
                }

                List<StandingRow> rows = mapStandings(response.body());
                if (rows.isEmpty()) {
                    showError("Data klasemen kosong.");
                    adapter.setItems(null);
                } else {
                    adapter.setItems(rows);
                }
            }

            @Override
            public void onFailure(Call<StandingsApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan external: " + t.getMessage());
            }
        });
    }

    // ✅ Mapper generik: backend maupun external sama-sama StandingsApiResponse
    private List<StandingRow> mapStandings(StandingsApiResponse body) {
        List<StandingRow> out = new ArrayList<>();
        if (body == null || body.getResponse() == null || body.getResponse().isEmpty()) return out;

        StandingsApiResponse.ResponseItem first = body.getResponse().get(0);
        if (first == null || first.league == null || first.league.standings == null || first.league.standings.isEmpty())
            return out;

        List<StandingsApiResponse.Standing> standings = first.league.standings.get(0);
        if (standings == null) return out;

        for (StandingsApiResponse.Standing s : standings) {
            if (s == null || s.team == null || s.all == null || s.all.goals == null) continue;

            Team team = new Team(
                    s.team.id,
                    s.team.name,
                    s.team.logo // ✅ backend sekarang sudah isi logo, external juga biasanya ada
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
            out.add(row);
        }

        return out;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
