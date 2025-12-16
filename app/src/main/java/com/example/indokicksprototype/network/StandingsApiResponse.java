package com.example.indokicksprototype.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class StandingsApiResponse {

    private List<ResponseItem> response;

    public List<ResponseItem> getResponse() {
        return response;
    }

    public static class ResponseItem {
        public League league;
    }

    public static class League {
        public List<List<Standing>> standings; // biasanya 1 list di index 0
    }

    public static class Standing {
        public int rank;
        public Team team;
        public int points;
        public int goalsDiff;
        public String form;
        public All all;
    }

    public static class Team {
        public int id;
        public String name;
        public String logo;
    }

    public static class All {
        public int played;
        public int win;
        public int draw;
        public int lose;
        public Goals goals;
    }

    public static class Goals {
        @SerializedName("for")
        public int goalsFor;

        @SerializedName("against")
        public int goalsAgainst;
    }
}
