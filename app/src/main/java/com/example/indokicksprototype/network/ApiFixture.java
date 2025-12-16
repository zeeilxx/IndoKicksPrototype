package com.example.indokicksprototype.network;

public class ApiFixture {
    public FixtureInfo fixture;
    public Teams teams;
    public Goals goals;
    public League league;

    public static class FixtureInfo {
        public int id;
        public long timestamp;
        public Status status;
        public Venue venue;
        public String referee;
    }

    public static class Status {
        public String _short; // "short" di JSON
    }

    public static class Teams {
        public TeamInfo home;
        public TeamInfo away;
    }

    public static class TeamInfo {
        public int id;
        public String name;
        public String logo;
    }

    public static class Goals {
        public Integer home;
        public Integer away;
    }

    public static class League {
        public String name;
        public String round;
    }

    public static class Venue {
        public String name;
        public String city;
    }
}
