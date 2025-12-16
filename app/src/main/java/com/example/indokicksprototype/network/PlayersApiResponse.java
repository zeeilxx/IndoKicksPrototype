package com.example.indokicksprototype.network;

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
        public int age;
        public String nationality;
        public String photo;
    }

    public static class Statistics {
        public Games games;
    }

    public static class Games {
        public String position;
        public Integer number;
        public Integer appearances;
        public Integer minutes;
    }
}
