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

import com.bumptech.glide.Glide;
import com.example.indokicksprototype.R;
import com.example.indokicksprototype.network.ApiClient;
import com.example.indokicksprototype.network.ApiFixture;
import com.example.indokicksprototype.network.FixturesApiResponse;
import com.example.indokicksprototype.network.LineupItem;
import com.example.indokicksprototype.network.LineupsApiResponse;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MatchDetailFragment extends Fragment {

    private static final String ARG_FIXTURE_ID = "fixtureId";
    private static final String ARG_HOME_TEAM_ID = "homeTeamId";
    private static final String ARG_AWAY_TEAM_ID = "awayTeamId";

    private ProgressBar progress;
    private TextView tvError;

    private ImageView ivHomeLogo, ivAwayLogo;
    private TextView tvHomeTeam, tvAwayTeam, tvScore;
    private TextView tvDateTime, tvStatus, tvLeagueRound, tvVenue, tvReferee;

    private TextView tvHomeLineupTitle, tvHomeLineupPlayers, tvHomeSubsTitle, tvHomeSubsPlayers;
    private TextView tvAwayLineupTitle, tvAwayLineupPlayers, tvAwaySubsTitle, tvAwaySubsPlayers;

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm", new Locale("id", "ID"));

    private int homeTeamId = -1;
    private int awayTeamId = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_match_detail, container, false);
    }

    @Override
    public void onViewCreated(View v, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(v, savedInstanceState);

        progress = v.findViewById(R.id.progressDetail);
        tvError = v.findViewById(R.id.tvErrorDetail);

        ivHomeLogo = v.findViewById(R.id.ivHomeLogoDetail);
        ivAwayLogo = v.findViewById(R.id.ivAwayLogoDetail);
        tvHomeTeam = v.findViewById(R.id.tvHomeTeamDetail);
        tvAwayTeam = v.findViewById(R.id.tvAwayTeamDetail);
        tvScore = v.findViewById(R.id.tvScoreDetail);
        tvDateTime = v.findViewById(R.id.tvDateTimeDetail);
        tvStatus = v.findViewById(R.id.tvStatusDetail);
        tvLeagueRound = v.findViewById(R.id.tvLeagueRoundDetail);
        tvVenue = v.findViewById(R.id.tvVenueDetail);
        tvReferee = v.findViewById(R.id.tvRefereeDetail);

        tvHomeLineupTitle = v.findViewById(R.id.tvHomeLineupTitle);
        tvHomeLineupPlayers = v.findViewById(R.id.tvHomeLineupPlayers);
        tvHomeSubsTitle = v.findViewById(R.id.tvHomeSubsTitle);
        tvHomeSubsPlayers = v.findViewById(R.id.tvHomeSubsPlayers);

        tvAwayLineupTitle = v.findViewById(R.id.tvAwayLineupTitle);
        tvAwayLineupPlayers = v.findViewById(R.id.tvAwayLineupPlayers);
        tvAwaySubsTitle = v.findViewById(R.id.tvAwaySubsTitle);
        tvAwaySubsPlayers = v.findViewById(R.id.tvAwaySubsPlayers);

        int fixtureId = -1;

        Bundle args = getArguments();
        if (args != null) {
            fixtureId = args.getInt(ARG_FIXTURE_ID, -1);
            homeTeamId = args.getInt(ARG_HOME_TEAM_ID, -1);
            awayTeamId = args.getInt(ARG_AWAY_TEAM_ID, -1);
        }

        if (fixtureId != -1) {
            loadDetail(fixtureId);
            loadLineups(fixtureId);
        } else {
            showError("Fixture ID tidak ditemukan");
        }
    }

    private void loadDetail(int fixtureId) {
        showLoading(true);
        tvError.setVisibility(View.GONE);

        Call<FixturesApiResponse> call = ApiClient.getService().getFixtureById(fixtureId);
        call.enqueue(new Callback<FixturesApiResponse>() {
            @Override
            public void onResponse(Call<FixturesApiResponse> call, Response<FixturesApiResponse> response) {
                showLoading(false);
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null ||
                        response.body().getResponse().isEmpty()) {
                    showError("Gagal memuat detail pertandingan");
                    return;
                }

                ApiFixture api = response.body().getResponse().get(0);

                // Kalau dari Bundle tidak ada, fallback dari API
                if (homeTeamId == -1 && api.teams != null && api.teams.home != null) {
                    homeTeamId = api.teams.home.id;
                }
                if (awayTeamId == -1 && api.teams != null && api.teams.away != null) {
                    awayTeamId = api.teams.away.id;
                }

                tvHomeTeam.setText(api.teams.home.name);
                tvAwayTeam.setText(api.teams.away.name);

                Integer gh = api.goals != null ? api.goals.home : null;
                Integer ga = api.goals != null ? api.goals.away : null;
                String score = (gh != null && ga != null) ? (gh + " - " + ga) : "-";
                tvScore.setText(score);

                long millis = api.fixture.timestamp * 1000L;
                tvDateTime.setText(dateFormat.format(new Date(millis)));
                tvStatus.setText(api.fixture.status._short);

                if (api.league != null) {
                    String leagueText = api.league.name;
                    if (api.league.round != null) {
                        leagueText += " • " + api.league.round;
                    }
                    tvLeagueRound.setText(leagueText);
                }

                if (api.fixture.venue != null) {
                    String venueText = "Venue: " + api.fixture.venue.name;
                    if (api.fixture.venue.city != null) {
                        venueText += " (" + api.fixture.venue.city + ")";
                    }
                    tvVenue.setText(venueText);
                }

                if (api.fixture.referee != null) {
                    tvReferee.setText("Referee: " + api.fixture.referee);
                }

                Glide.with(requireContext())
                        .load(api.teams.home.logo)
                        .placeholder(R.mipmap.ic_launcher)
                        .into(ivHomeLogo);

                Glide.with(requireContext())
                        .load(api.teams.away.logo)
                        .placeholder(R.mipmap.ic_launcher)
                        .into(ivAwayLogo);
            }

            @Override
            public void onFailure(Call<FixturesApiResponse> call, Throwable t) {
                showLoading(false);
                showError("Gagal memuat detail: " + t.getMessage());
            }
        });
    }

    private void loadLineups(int fixtureId) {
        Call<LineupsApiResponse> call = ApiClient.getService().getLineups(fixtureId);
        call.enqueue(new Callback<LineupsApiResponse>() {
            @Override
            public void onResponse(Call<LineupsApiResponse> call, Response<LineupsApiResponse> response) {
                if (!response.isSuccessful() || response.body() == null ||
                        response.body().getResponse() == null) {
                    return;
                }

                List<LineupItem> list = response.body().getResponse();
                if (list.isEmpty()) {
                    setNoLineupsMessage();
                    return;
                }

                LineupItem homeLineup = null;
                LineupItem awayLineup = null;

                for (LineupItem item : list) {
                    if (item.team == null) continue;
                    if (item.team.id == homeTeamId) {
                        homeLineup = item;
                    } else if (item.team.id == awayTeamId) {
                        awayLineup = item;
                    }
                }

                // Fallback: kalau id belum cocok, pakai urutan list
                if (homeLineup == null && !list.isEmpty()) {
                    homeLineup = list.get(0);
                }
                if (awayLineup == null && list.size() > 1) {
                    awayLineup = list.get(1);
                }

                if (homeLineup != null) {
                    String title = homeLineup.team.name + " - Starting XI";
                    if (homeLineup.formation != null) {
                        title += " (" + homeLineup.formation + ")";
                    }
                    tvHomeLineupTitle.setText(title);
                    tvHomeLineupPlayers.setText(buildPlayersText(homeLineup.startXI));

                    tvHomeSubsTitle.setText(homeLineup.team.name + " - Substitutes");
                    tvHomeSubsPlayers.setText(buildPlayersText(homeLineup.substitutes));
                }

                if (awayLineup != null) {
                    String title = awayLineup.team.name + " - Starting XI";
                    if (awayLineup.formation != null) {
                        title += " (" + awayLineup.formation + ")";
                    }
                    tvAwayLineupTitle.setText(title);
                    tvAwayLineupPlayers.setText(buildPlayersText(awayLineup.startXI));

                    tvAwaySubsTitle.setText(awayLineup.team.name + " - Substitutes");
                    tvAwaySubsPlayers.setText(buildPlayersText(awayLineup.substitutes));
                }

                if (homeLineup == null && awayLineup == null) {
                    setNoLineupsMessage();
                }
            }

            @Override
            public void onFailure(Call<LineupsApiResponse> call, Throwable t) {
                // Boleh di-log atau diabaikan; halaman tetap jalan tanpa lineups
            }
        });
    }

    private String buildPlayersText(List<LineupItem.PlayerWrapper> wrappers) {
        if (wrappers == null || wrappers.isEmpty()) return "-";
        StringBuilder sb = new StringBuilder();
        for (LineupItem.PlayerWrapper w : wrappers) {
            if (w == null || w.player == null) continue;
            LineupItem.Player p = w.player;
            sb.append(p.number)
                    .append(" - ")
                    .append(p.name);
            if (p.pos != null) {
                sb.append(" (").append(p.pos).append(")");
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }

    private void setNoLineupsMessage() {
        String msg = "Data susunan pemain belum tersedia.";
        tvHomeLineupPlayers.setText(msg);
        tvHomeSubsPlayers.setText("-");
        tvAwayLineupPlayers.setText("-");
        tvAwaySubsPlayers.setText("-");
    }

    private void showLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String msg) {
        tvError.setVisibility(View.VISIBLE);
        tvError.setText(msg);
    }
}
