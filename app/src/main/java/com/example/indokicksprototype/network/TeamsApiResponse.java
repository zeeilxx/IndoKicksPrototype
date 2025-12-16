package com.example.indokicksprototype.network;

import java.util.List;

public class TeamsApiResponse {

    private List<ResponseItem> response;

    public List<ResponseItem> getResponse() {
        return response;
    }

    public static class ResponseItem {
        public Team team;
        public Venue venue;
    }

    public static class Team {
        public int id;
        public String name;
        public String code;
        public String country;
        public int founded;
        public String logo;
        public boolean national;
    }

    public static class Venue {
        public int id;
        public String name;
        public String address;
        public String city;
        public int capacity;
        public String surface;
        public String image;
    }
}
