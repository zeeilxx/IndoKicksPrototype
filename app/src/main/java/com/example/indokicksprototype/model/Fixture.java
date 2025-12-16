package com.example.indokicksprototype.model;

public class Fixture {
    private int id;
    private long timestamp;
    private String status; // NS, 1H, HT, 2H, FT
    private Team home;
    private Team away;
    private int leagueId;

    // skor (boleh null kalau belum main)
    private Integer goalsHome;
    private Integer goalsAway;

    public Fixture(int id, long timestamp, String status, Team home, Team away, int leagueId) {
        this.id = id;
        this.timestamp = timestamp;
        this.status = status;
        this.home = home;
        this.away = away;
        this.leagueId = leagueId;
    }

    public int getId() { return id; }
    public long getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
    public Team getHome() { return home; }
    public Team getAway() { return away; }
    public int getLeagueId() { return leagueId; }

    public Integer getGoalsHome() { return goalsHome; }
    public Integer getGoalsAway() { return goalsAway; }

    public void setGoals(Integer goalsHome, Integer goalsAway) {
        this.goalsHome = goalsHome;
        this.goalsAway = goalsAway;
    }
}
