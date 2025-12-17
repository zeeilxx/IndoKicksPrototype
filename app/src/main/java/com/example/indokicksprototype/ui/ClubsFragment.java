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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.Club;
import com.example.indokicksprototype.network.ApiClientBackend;
import com.example.indokicksprototype.network.ApiClientExternal;
import com.example.indokicksprototype.network.TeamsApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubsFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    private Spinner spinnerSeason;
    private ProgressBar progressBar;
    private TextView tvError;
    private RecyclerView rvClubs;

    private ClubsAdapter adapter;

    // season yang sedang dipilih user
    private int selectedSeason = 2025;

    // cache logo dari API external (pakai season 2023 sebagai sumber logo)
    // key: normalized team name, value: logo url
    private final Map<String, String> externalLogoByName = new HashMap<>();
    private boolean logoCacheReady = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clubs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        spinnerSeason = view.findViewById(R.id.spinnerSeasonClubs);
        progressBar = view.findViewById(R.id.progressClubs);
        tvError = view.findViewById(R.id.tvErrorClubs);
        rvClubs = view.findViewById(R.id.rvClubs);

        rvClubs.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        NavController navController = NavHostFragment.findNavController(this);

        // Klik item club -> buka detail (bawa slug + season)
        adapter = new ClubsAdapter(club -> {
            Bundle args = new Bundle();
            args.putInt("teamId", club.getId());
            args.putString("teamSlug", club.getSlug()); // ✅ ambil dari model, bukan variabel clubSlug
            args.putInt("season", selectedSeason);
            navController.navigate(R.id.nav_club_detail, args);
        });
        rvClubs.setAdapter(adapter);

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

        // default: 2025
        spinnerSeason.setSelection(seasons.length - 1);
        selectedSeason = 2025;

        spinnerSeason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
                Integer s = (Integer) spinnerSeason.getSelectedItem();
                if (s == null) return;
                selectedSeason = s;

                // agar logo untuk season 2024-2025 tetap pakai external,
                // kita siapkan cache logo dari external season 2023 dulu (sekali saja)
                if (!logoCacheReady) {
                    preloadExternalLogosThenLoad(selectedSeason);
                } else {
                    loadClubsBySeason(selectedSeason);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // trigger awal
        if (!logoCacheReady) {
            preloadExternalLogosThenLoad(selectedSeason);
        } else {
            loadClubsBySeason(selectedSeason);
        }
    }

    private void preloadExternalLogosThenLoad(int seasonToLoad) {
        // Ambil logo club dari external season 2023 (karena external hanya punya 2021-2023)
        showLoading(true);
        tvError.setVisibility(View.GONE);

        Call<TeamsApiResponse> call = ApiClientExternal.getService()
                .getTeams(LIGA_1_ID, 2023);

        call.enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getResponse() != null) {
                    externalLogoByName.clear();

                    for (TeamsApiResponse.ResponseItem item : response.body().getResponse()) {
                        if (item == null || item.team == null) continue;

                        String name = item.team.name;
                        String logo = item.team.logo;
                        if (name == null || logo == null) continue;

                        externalLogoByName.put(normKey(name), logo);
                    }

                    logoCacheReady = true;
                    showLoading(false);

                    loadClubsBySeason(seasonToLoad);
                } else {
                    // Kalau gagal ambil cache logo, tetap lanjut load clubs (nanti logo fallback dari response)
                    logoCacheReady = true;
                    showLoading(false);

                    loadClubsBySeason(seasonToLoad);
                }
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
                // Sama: cache logo gagal, tapi tetap lanjut load clubs
                logoCacheReady = true;
                showLoading(false);

                loadClubsBySeason(seasonToLoad);
            }
        });
    }

    private void loadClubsBySeason(int season) {
        if (season <= 2023) {
            loadClubsFromExternal(season);
        } else {
            loadClubsFromBackend(season);
        }
    }

    private void loadClubsFromExternal(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        Call<TeamsApiResponse> call = ApiClientExternal.getService()
                .getTeams(LIGA_1_ID, season);

        call.enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
                    showError("Gagal memuat clubs (external).");
                    return;
                }

                List<Club> clubs = mapTeamsResponseToClubs(response.body().getResponse(), true);
                if (clubs.isEmpty()) {
                    showError("Data clubs kosong.");
                } else {
                    adapter.setItems(clubs);
                }
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan external: " + t.getMessage());
            }
        });
    }

    private void loadClubsFromBackend(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        Call<TeamsApiResponse> call = ApiClientBackend.getService()
                .getTeams(LIGA_1_ID, season);

        call.enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                showLoading(false);

                if (!response.isSuccessful() || response.body() == null || response.body().getResponse() == null) {
                    showError("Gagal memuat clubs (backend).");
                    return;
                }

                // Untuk season 2024-2025: tetap pakai logo dari external cache kalau tersedia
                List<Club> clubs = mapTeamsResponseToClubs(response.body().getResponse(), false);

                if (clubs.isEmpty()) {
                    showError("Data clubs kosong.");
                } else {
                    adapter.setItems(clubs);
                }
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Error jaringan backend: " + t.getMessage());
            }
        });
    }

    /**
     * Map response -> Club model (constructor 10 argumen: termasuk slug).
     *
     * @param items response list
     * @param isExternal kalau true, logo dari response external; kalau false (backend), logo coba ambil dari cache external
     */
    private List<Club> mapTeamsResponseToClubs(List<TeamsApiResponse.ResponseItem> items, boolean isExternal) {
        List<Club> out = new ArrayList<>();
        if (items == null) return out;

        for (TeamsApiResponse.ResponseItem r : items) {
            if (r == null || r.team == null) continue;

            int id = r.team.id;
            String name = r.team.name;

            // slug penting untuk detail backend /teams/{team_slug}
            String slug = r.team.slug;
            if (slug == null) {
                // fallback: bikin slug sederhana dari name (biar tidak null)
                slug = (name != null)
                        ? name.trim().toUpperCase(Locale.US).replace(" ", "_")
                        : "";
            }

            String country = r.team.country;
            Integer founded = r.team.founded > 0 ? r.team.founded : null;

            String stadiumName = (r.venue != null) ? r.venue.name : null;
            String stadiumCity = (r.venue != null) ? r.venue.city : null;
            String stadiumAddress = (r.venue != null) ? r.venue.address : null;
            Integer stadiumCapacity = (r.venue != null && r.venue.capacity > 0) ? r.venue.capacity : null;

            String logo;
            if (isExternal) {
                // external: langsung pakai logo response
                logo = r.team.logo;
            } else {
                // backend: wajib coba pakai logo external cache (supaya "semua gambar dari external")
                String cached = externalLogoByName.get(normKey(name));
                logo = (cached != null) ? cached : r.team.logo; // fallback kalau cache tidak ada
            }

            // ✅ PENTING: constructor Club sekarang 10 argumen (ada slug)
            Club club = new Club(
                    id,
                    name,
                    slug,           // ✅ argumen ke-3 (slug)
                    logo,
                    country,
                    founded,
                    stadiumName,
                    stadiumCity,
                    stadiumAddress,
                    stadiumCapacity
            );

            out.add(club);
        }

        return out;
    }

    private String normKey(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase(Locale.US);
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
