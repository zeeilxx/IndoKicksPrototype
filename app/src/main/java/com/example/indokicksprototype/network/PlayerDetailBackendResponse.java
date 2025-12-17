package com.example.indokicksprototype.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class PlayerDetailBackendResponse {

    private List<ResponseObj> response;

    public List<ResponseObj> getResponse() {
        return response;
    }

    public static class ResponseObj {
        public Player player;
        public List<Statistic> statistics;
    }

    public static class Player {
        public int id;
        public String name;
        public Integer age;
        public String nationality;
        public String photo;
    }

    public static class Statistic {
        public Team team;
        public Games games;
        public Goals goals;
        public Shots shots;

        // backend: {success,total}
        public Map<String, Integer> passes;

        public Defence defence;
        public Cards cards;
        public Discipline discipline;
        public History history;

        @SerializedName("_source")
        public Object source;
    }

    public static class Team {
        public String name;
    }

    public static class Games {
        public Integer number;
        public String position;

        // backend kamu: "appearences" (typo mengikuti api-football)
        @SerializedName("appearences")
        public Integer appearences;
    }

    public static class Goals {
        public Integer total;
        public Integer assists;
    }

    public static class Shots {
        public Integer total;
        public Integer on;
    }

    public static class Defence {
        public Integer tackles;
        public Integer interceptions;
        public Integer clearances;
    }

    public static class Cards {
        public Integer yellow;
        public Integer red;
    }

    public static class Discipline {
        public Integer fouls;
        public Integer offsides;
    }

    public static class History {
        @SerializedName("club_history")
        public List<Object> clubHistory;

        @SerializedName("match_history")
        public List<Object> matchHistory;
    }
}
