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
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.Club;
import com.example.indokicksprototype.network.ApiClient;
import com.example.indokicksprototype.network.TeamsApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubsFragment extends Fragment {

    private static final int LIGA_1_ID = 274;

    private ProgressBar progressBar;
    private TextView tvError;
    private Spinner spinnerSeason;
    private ClubsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_clubs, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = view.findViewById(R.id.progressClubs);
        tvError = view.findViewById(R.id.tvErrorClubs);
        spinnerSeason = view.findViewById(R.id.spinnerSeasonClubs);
        RecyclerView rv = view.findViewById(R.id.rvClubs);

        rv.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        NavController navController = NavHostFragment.findNavController(this);

        adapter = new ClubsAdapter(club -> {
            Bundle args = new Bundle();
            args.putInt("teamId", club.getId());
            args.putInt("season", (Integer) spinnerSeason.getSelectedItem());
            navController.navigate(R.id.nav_club_detail, args);
        });

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
        spinnerSeason.setSelection(2); // default 2023

        spinnerSeason.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            boolean first = true;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Integer season = (Integer) spinnerSeason.getSelectedItem();
                if (season == null) return;

                if (first) {
                    first = false;
                }
                loadClubs(season);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    private void loadClubs(int season) {
        showLoading(true);
        tvError.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());

        Call<TeamsApiResponse> call = ApiClient.getService()
                .getTeams(LIGA_1_ID, season);

        call.enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null) {
                    showError("Gagal memuat data klub.");
                    return;
                }

                List<TeamsApiResponse.ResponseItem> list = response.body().getResponse();
                List<Club> clubs = new ArrayList<>();

                for (TeamsApiResponse.ResponseItem r : list) {
                    if (r.team == null) continue;

                    Integer founded = r.team.founded;
                    String stadiumName = r.venue != null ? r.venue.name : null;
                    String stadiumCity = r.venue != null ? r.venue.city : null;
                    String stadiumAddress = r.venue != null ? r.venue.address : null;
                    Integer capacity = r.venue != null ? r.venue.capacity : null;

                    Club club = new Club(
                            r.team.id,
                            r.team.name,
                            r.team.logo,
                            r.team.country,
                            founded,
                            stadiumName,
                            stadiumCity,
                            stadiumAddress,
                            capacity
                    );
                    clubs.add(club);
                }

                if (clubs.isEmpty()) {
                    showError("Tidak ada data klub.");
                } else {
                    adapter.setItems(clubs);
                }
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) {
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
