package com.example.indokicksprototype.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.indokicksprototype.R;
import com.example.indokicksprototype.model.PlayerSimple;
import com.example.indokicksprototype.network.ApiClient;
import com.example.indokicksprototype.network.PlayersApiResponse;
import com.example.indokicksprototype.network.TeamsApiResponse;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ClubDetailFragment extends Fragment {

    private static final String ARG_TEAM_ID = "teamId";
    private static final String ARG_SEASON = "season";

    private ImageView ivLogo;
    private TextView tvName, tvCountry, tvFounded, tvStadium;
    private ProgressBar progressPlayers;
    private TextView tvPlayersError;
    private PlayersAdapter playersAdapter;

    private int teamId;
    private int season;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_club_detail, container, false);
    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        ivLogo = v.findViewById(R.id.ivClubLogoDetail);
        tvName = v.findViewById(R.id.tvClubNameDetail);
        tvCountry = v.findViewById(R.id.tvClubCountryDetail);
        tvFounded = v.findViewById(R.id.tvClubFoundedDetail);
        tvStadium = v.findViewById(R.id.tvClubStadiumDetail);
        progressPlayers = v.findViewById(R.id.progressPlayers);
        tvPlayersError = v.findViewById(R.id.tvPlayersError);
        RecyclerView rvPlayers = v.findViewById(R.id.rvPlayers);

        rvPlayers.setLayoutManager(new LinearLayoutManager(requireContext()));
        playersAdapter = new PlayersAdapter();
        rvPlayers.setAdapter(playersAdapter);

        Bundle args = getArguments();
        if (args != null) {
            teamId = args.getInt(ARG_TEAM_ID, -1);
            season = args.getInt(ARG_SEASON, 2023);
        }

        if (teamId == -1) {
            tvPlayersError.setVisibility(View.VISIBLE);
            tvPlayersError.setText("Team ID tidak ditemukan.");
            return;
        }

        loadClubDetail();
        loadPlayers();
    }

    private void loadClubDetail() {
        Call<TeamsApiResponse> call = ApiClient.getService().getTeamById(teamId);
        call.enqueue(new Callback<TeamsApiResponse>() {
            @Override
            public void onResponse(Call<TeamsApiResponse> call, Response<TeamsApiResponse> response) {
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null ||
                        response.body().getResponse().isEmpty()) {
                    return;
                }

                TeamsApiResponse.ResponseItem r = response.body().getResponse().get(0);

                Glide.with(requireContext())
                        .load(r.team.logo)
                        .placeholder(R.mipmap.ic_launcher)
                        .into(ivLogo);

                tvName.setText(r.team.name);

                if (r.team.country != null) {
                    tvCountry.setText(r.team.country);
                }

                if (r.team.founded > 0) {
                    tvFounded.setText("Founded: " + r.team.founded);
                }

                if (r.venue != null) {
                    String s = "Stadium: " + r.venue.name;
                    if (r.venue.city != null) s += " (" + r.venue.city + ")";
                    if (r.venue.capacity > 0) s += " • Kapasitas " + r.venue.capacity;
                    tvStadium.setText(s);
                }
            }

            @Override
            public void onFailure(Call<TeamsApiResponse> call, Throwable t) { }
        });
    }

    private void loadPlayers() {
        showPlayersLoading(true);
        tvPlayersError.setVisibility(View.GONE);

        Call<PlayersApiResponse> call = ApiClient.getService()
                .getPlayers(teamId, season, 1);

        call.enqueue(new Callback<PlayersApiResponse>() {
            @Override
            public void onResponse(Call<PlayersApiResponse> call, Response<PlayersApiResponse> response) {
                showPlayersLoading(false);
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null) {
                    showPlayersError("Gagal memuat squad.");
                    return;
                }

                List<PlayersApiResponse.PlayerItem> list = response.body().getResponse();
                if (list.isEmpty()) {
                    showPlayersError("Squad tidak tersedia.");
                    return;
                }

                List<PlayerSimple> players = new ArrayList<>();
                for (PlayersApiResponse.PlayerItem item : list) {
                    if (item.player == null) continue;

                    String position = null;
                    Integer number = null;
                    if (item.statistics != null && !item.statistics.isEmpty() &&
                            item.statistics.get(0).games != null) {
                        position = item.statistics.get(0).games.position;
                        number = item.statistics.get(0).games.number;
                    }

                    PlayerSimple p = new PlayerSimple(
                            item.player.id,
                            item.player.name,
                            position,
                            number,
                            item.player.age,
                            item.player.nationality,
                            item.player.photo
                    );
                    players.add(p);
                }

                if (players.isEmpty()) {
                    showPlayersError("Squad tidak tersedia.");
                } else {
                    playersAdapter.setItems(players);
                }
            }

            @Override
            public void onFailure(Call<PlayersApiResponse> call, Throwable t) {
                showPlayersLoading(false);
                showPlayersError("Terjadi kesalahan jaringan: " + t.getMessage());
            }
        });
    }

    private void showPlayersLoading(boolean loading) {
        progressPlayers.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showPlayersError(String msg) {
        tvPlayersError.setVisibility(View.VISIBLE);
        tvPlayersError.setText(msg);
    }
}
