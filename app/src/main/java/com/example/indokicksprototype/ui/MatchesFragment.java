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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.Fixture;
import com.example.indokicksprototype.model.Team;
import com.example.indokicksprototype.network.ApiClient;         // <-- EXTERNAL
import com.example.indokicksprototype.network.ApiClientBackend;  // <-- BACKEND
import com.example.indokicksprototype.network.ApiFixture;
import com.example.indokicksprototype.network.FixturesApiResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchesFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    private Spinner spinnerYear;
    private Spinner spinnerMonth;
    private ProgressBar progressBar;
    private TextView tvError;

    private MatchesAdapter adapter;

    // untuk menghindari double-trigger ketika spinner Year dan Month sama-sama set listener
    private boolean initial = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matches, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerYear = view.findViewById(R.id.spinnerYear);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);

        RecyclerView rvMatches = view.findViewById(R.id.rvMatches);
        rvMatches.setLayoutManager(new LinearLayoutManager(requireContext()));

        NavController navController = NavHostFragment.findNavController(this);
        adapter = new MatchesAdapter(f -> {
            Bundle args = new Bundle();
            args.putInt("fixtureId", f.getId());
            navController.navigate(R.id.nav_match_detail, args);
        });
        rvMatches.setAdapter(adapter);

        setupFilters();
    }

    private void setupFilters() {
        Integer[] years = new Integer[]{2021, 2022, 2023, 2024, 2025};
        Integer[] months = new Integer[]{1,2,3,4,5,6,7,8,9,10,11,12};

        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setSelection(years.length - 1); // default 2025

        ArrayAdapter<Integer> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
        int monthIndex = 0;
        for (int i = 0; i < months.length; i++) {
            if (months[i] == currentMonth) { monthIndex = i; break; }
        }
        spinnerMonth.setSelection(monthIndex);

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer year = (Integer) spinnerYear.getSelectedItem();
                Integer month = (Integer) spinnerMonth.getSelectedItem();
                if (year == null || month == null) return;

                // optional: kalau kamu mau benar-benar skip dobel trigger pertama, aktifkan return ini
                if (initial) {
                    initial = false;
                    // return; // kalau kamu aktifkan return, initial load dilakukan di bawah (manual)
                }

                String from = String.format(Locale.US, "%04d-%02d-01", year, month);
                String to = getMonthEndDate(year, month);

                loadFixturesBySeason(year, from, to);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spinnerYear.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);

        // initial load
        Integer year = (Integer) spinnerYear.getSelectedItem();
        Integer month = (Integer) spinnerMonth.getSelectedItem();
        if (year != null && month != null) {
            String from = String.format(Locale.US, "%04d-%02d-01", year, month);
            String to = getMonthEndDate(year, month);
            loadFixturesBySeason(year, from, to);
        }
    }

    private void loadFixturesBySeason(int season, String from, String to) {
        // 2021-2023 -> External, 2024-2025 -> Backend
        if (season <= 2023) {
            loadFixturesFromExternal(season, from, to);
        } else {
            loadFixturesFromBackend(season, from, to);
        }
    }

    private String getMonthEndDate(int year, int month) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, lastDay);
    }

    // ===================== BACKEND (2024-2025) =====================

    private void loadFixturesFromBackend(int season, String from, String to) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        Call<FixturesApiResponse> call = ApiClientBackend.getService()
                .getFixtures(LIGA_1_ID, season, from, to);

        call.enqueue(new Callback<FixturesApiResponse>() {
            @Override
            public void onResponse(Call<FixturesApiResponse> call, Response<FixturesApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
                    showError("Gagal memuat pertandingan (backend).");
                    return;
                }

                List<Fixture> fixtures = mapFixtures(response.body().getResponse());

                if (fixtures.isEmpty()) {
                    showError("Tidak ada jadwal pada periode ini.");
                } else {
                    adapter.setItems(fixtures);
                }
            }

            @Override
            public void onFailure(Call<FixturesApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan backend: " + t.getMessage());
            }
        });
    }

    // ===================== EXTERNAL (2021-2023) =====================

    private void loadFixturesFromExternal(int season, String from, String to) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        // Pastikan ApiClient service kamu punya method getFixtures yang sama:
        // getFixtures(@Query("league") int league, @Query("season") int season,
        //            @Query("from") String from, @Query("to") String to)
        Call<FixturesApiResponse> call = ApiClient.getService()
                .getFixtures(LIGA_1_ID, season, from, to);

        call.enqueue(new Callback<FixturesApiResponse>() {
            @Override
            public void onResponse(Call<FixturesApiResponse> call, Response<FixturesApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
                    showError("Gagal memuat pertandingan (external).");
                    return;
                }

                List<Fixture> fixtures = mapFixtures(response.body().getResponse());

                if (fixtures.isEmpty()) {
                    showError("Tidak ada jadwal pada periode ini.");
                } else {
                    adapter.setItems(fixtures);
                }
            }

            @Override
            public void onFailure(Call<FixturesApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan external: " + t.getMessage());
            }
        });
    }

    // ===================== MAPPER (shared) =====================

    private List<Fixture> mapFixtures(List<ApiFixture> apiFixtures) {
        List<Fixture> fixtures = new ArrayList<>();
        if (apiFixtures == null) return fixtures;

        for (ApiFixture a : apiFixtures) {
            if (a == null || a.fixture == null || a.teams == null || a.teams.home == null || a.teams.away == null) {
                continue;
            }

            int fixtureId = a.fixture.id;
            long timestamp = a.fixture.timestamp;

            String statusShort = null;
            if (a.fixture.status != null) statusShort = a.fixture.status._short;

            Integer goalsHome = (a.goals != null) ? a.goals.home : null;
            Integer goalsAway = (a.goals != null) ? a.goals.away : null;

            Team home = new Team(a.teams.home.id, a.teams.home.name, a.teams.home.logo);
            Team away = new Team(a.teams.away.id, a.teams.away.name, a.teams.away.logo);

            Fixture f = new Fixture(fixtureId, timestamp, statusShort, home, away, LIGA_1_ID);
            f.setGoals(goalsHome, goalsAway);

            fixtures.add(f);
        }

        return fixtures;
    }

    // ===================== UI HELPERS =====================

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
