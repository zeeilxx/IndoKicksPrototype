package com.example.indokicksprototype.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PlayersApiResponse {

    private List<PlayerItem> response;

    public List<PlayerItem> getResponse() {
        return response;
    }

    public static class PlayerItem {
        public Player player;
        public List<Statistics> statistics;
    }

    public static class Player {
        public int id;
        public String name;
        public String firstname;
        public String lastname;
        public Integer age;
        public Birth birth;
        public String nationality;
        public String height;
        public String weight;
        public Boolean injured;
        public String photo;
    }

    public static class Birth {
        public String date;
        public String place;
        public String country;
    }

    public static class Statistics {
        public Team team;
        public League league;
        public Games games;
    }

    public static class Team {
        public int id;
        public String name;
        public String logo;
    }

    public static class League {
        public int id;
        public String name;
        public String country;
        public Integer season;
        public String logo;
        public String flag;
    }

    public static class Games {
        public Integer appearences; // spelling dari API-Sports: appearences
        public Integer lineups;
        public Integer minutes;
        public Integer number;
        public String position;
        public String rating;
        public Boolean captain;
    }
}
