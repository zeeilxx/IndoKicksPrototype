package com.example.indokicksprototype.network;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BackendService {

    @GET("fixtures")
    Call<FixturesApiResponse> getFixtures(
            @Query("league") int league,
            @Query("season") int season,
            @Query("from") String from,
            @Query("to") String to
    );

    @GET("standings")
    Call<StandingsApiResponse> getStandings(
            @Query("league") int league,
            @Query("season") int season
    );

    @GET("teams")
    Call<TeamsApiResponse> getTeams(
            @Query("league") int league,
            @Query("season") int season
    );

    @GET("players/{player_slug}")
    Call<PlayerDetailBackendResponse> getPlayerDetail(
            @Path("player_slug") String playerSlug,
            @Query("league") int league,
            @Query("season") int season,
            @Query("token") String token
    );

    @GET("teams/{team_slug}")
    Call<TeamsApiResponse> getTeamDetail(
            @Path("team_slug") String teamSlug,
            @Query("league") int league,
            @Query("season") int season,
            @Query("include_players") boolean includePlayers
    );


}
