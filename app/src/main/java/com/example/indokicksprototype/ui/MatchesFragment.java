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
import com.example.indokicksprototype.network.ApiClient;
import com.example.indokicksprototype.network.ApiFixture;
import com.example.indokicksprototype.network.FixturesApiResponse;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MatchesFragment extends Fragment {

    private ProgressBar progressBar;
    private TextView tvError;
    private MatchesAdapter adapter;
    private Spinner spinnerYear, spinnerMonth;

    // ID Liga 1 Indonesia
    private static final int LIGA_1_ID = 274;
    private static final int ITEMS_PER_PAGE = 10;

    private final List<Fixture> allFixtures = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLoadingMore = false;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_matches, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        spinnerYear = view.findViewById(R.id.spinnerYear);
        spinnerMonth = view.findViewById(R.id.spinnerMonth);
        RecyclerView rv = view.findViewById(R.id.rvMatches);

        rv.setLayoutManager(new LinearLayoutManager(requireContext()));

        NavController navController = NavHostFragment.findNavController(this);

        adapter = new MatchesAdapter(fixture -> {
            Bundle args = new Bundle();
            args.putInt("fixtureId", fixture.getId());
            args.putInt("homeTeamId", fixture.getHome().getId());
            args.putInt("awayTeamId", fixture.getAway().getId());
            navController.navigate(R.id.nav_match_detail, args);
        });

        rv.setAdapter(adapter);

        // Endless scroll listener
        rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;

                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm == null) return;

                int visibleItemCount = lm.getChildCount();
                int totalItemCount = lm.getItemCount();
                int firstVisibleItem = lm.findFirstVisibleItemPosition();

                if (!isLoadingMore && totalItemCount < allFixtures.size()) {
                    if ((visibleItemCount + firstVisibleItem) >= totalItemCount - 2) {
                        loadMorePage();
                    }
                }
            }
        });

        setupFilters();
    }

    private void setupFilters() {
        // Tahun 2021-2023
        Integer[] years = {2021, 2022, 2023};
        ArrayAdapter<Integer> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Bulan Jan–Des (pakai index 1-12)
        String[] months = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                months
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        // Set default: tahun 2023, bulan Oktober (sesuai contoh awal)
        spinnerYear.setSelection(2);   // index 2 = 2023
        spinnerMonth.setSelection(9 - 1); // Oktober = 10 -> index 9, tapi array 0-based

        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            boolean firstCall = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Biar tidak langsung trigger dua kali sebelum UI siap
                if (firstCall) {
                    firstCall = false;
                    // langsung load pertama kali
                    reloadMatchesForSelectedFilter();
                    return;
                }
                reloadMatchesForSelectedFilter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        };

        spinnerYear.setOnItemSelectedListener(listener);
        spinnerMonth.setOnItemSelectedListener(listener);
    }

    private void reloadMatchesForSelectedFilter() {
        Integer year = (Integer) spinnerYear.getSelectedItem();
        int monthIndex = spinnerMonth.getSelectedItemPosition(); // 0-11
        int month = monthIndex + 1; // 1-12

        if (year == null) return;

        loadMatches(year, month);
    }

    private void loadMatches(int year, int month) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        allFixtures.clear();
        currentPage = 0;
        adapter.setItems(new ArrayList<>());

        // Hitung from & to (hari pertama & terakhir bulan)
        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        String from = String.format(Locale.US, "%04d-%02d-01", year, month);
        String to = String.format(Locale.US, "%04d-%02d-%02d", year, month, lastDay);

        Call<FixturesApiResponse> call = ApiClient.getService()
                .getFixtures(LIGA_1_ID, year, from, to);

        call.enqueue(new Callback<FixturesApiResponse>() {
            @Override
            public void onResponse(Call<FixturesApiResponse> call, Response<FixturesApiResponse> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    showError("Gagal memuat data (" + response.code() + ")");
                    return;
                }

                List<ApiFixture> apiList = response.body().getResponse();
                if (apiList == null || apiList.isEmpty()) {
                    showError("Tidak ada pertandingan pada periode ini.");
                    return;
                }

                for (ApiFixture api : apiList) {
                    Team home = new Team(
                            api.teams.home.id,
                            api.teams.home.name,
                            api.teams.home.logo
                    );
                    Team away = new Team(
                            api.teams.away.id,
                            api.teams.away.name,
                            api.teams.away.logo
                    );

                    Fixture f = new Fixture(
                            api.fixture.id,
                            api.fixture.timestamp,
                            api.fixture.status._short,
                            home,
                            away,
                            LIGA_1_ID
                    );

                    if (api.goals != null) {
                        f.setGoals(api.goals.home, api.goals.away);
                    }

                    allFixtures.add(f);
                }

                // Urutkan dari tanggal terdekat ke terlama
                Collections.sort(allFixtures, new Comparator<Fixture>() {
                    @Override
                    public int compare(Fixture o1, Fixture o2) {
                        return Long.compare(o1.getTimestamp(), o2.getTimestamp());
                    }
                });

                if (allFixtures.isEmpty()) {
                    showError("Tidak ada pertandingan pada periode ini.");
                } else {
                    loadMorePage(); // load page pertama
                }
            }

            @Override
            public void onFailure(Call<FixturesApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Terjadi kesalahan jaringan: " + t.getMessage());
            }
        });
    }

    private void loadMorePage() {
        if (allFixtures.isEmpty()) return;
        if (currentPage * ITEMS_PER_PAGE >= allFixtures.size()) return;

        isLoadingMore = true;
        currentPage++;

        int fromIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        int toIndex = Math.min(currentPage * ITEMS_PER_PAGE, allFixtures.size());

        List<Fixture> sub = allFixtures.subList(fromIndex, toIndex);
        if (currentPage == 1) {
            adapter.setItems(new ArrayList<>(sub));
        } else {
            adapter.addItems(new ArrayList<>(sub));
        }
        isLoadingMore = false;
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
