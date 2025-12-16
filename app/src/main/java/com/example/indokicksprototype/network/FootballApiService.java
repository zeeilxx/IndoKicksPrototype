package com.example.indokicksprototype.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface FootballApiService {

    @GET("fixtures")
    Call<FixturesApiResponse> getFixtures(
            @Query("league") int leagueId,
            @Query("season") int season,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("fixtures")
    Call<FixturesApiResponse> getFixtureById(
            @Query("id") int fixtureId
    );

    @GET("fixtures/lineups")
    Call<LineupsApiResponse> getLineups(
            @Query("fixture") int fixtureId
    );

    @GET("standings")
    Call<StandingsApiResponse> getStandings(
            @Query("league") int leagueId,
            @Query("season") int season
    );

    @GET("teams")
    Call<TeamsApiResponse> getTeams(
            @Query("league") int leagueId,
            @Query("season") int season
    );

    // Detail 1 klub (kalau mau spesifik)
    @GET("teams")
    Call<TeamsApiResponse> getTeamById(
            @Query("id") int teamId
    );

    // Daftar pemain per klub & season
    @GET("players")
    Call<PlayersApiResponse> getPlayers(
            @Query("team") int teamId,
            @Query("season") int season,
            @Query("page") int page // bisa kirim 1 saja
    );


}

