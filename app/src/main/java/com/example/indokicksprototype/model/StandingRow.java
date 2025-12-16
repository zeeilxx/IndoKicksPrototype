package com.example.indokicksprototype.model;

public class StandingRow {

    private int rank;
    private Team team;
    private int played;
    private int win;
    private int draw;
    private int lose;
    private int goalsFor;
    private int goalsAgainst;
    private int goalsDiff;
    private int points;
    private String form;

    public StandingRow(int rank, Team team, int played, int win, int draw, int lose,
                       int goalsFor, int goalsAgainst, int goalsDiff, int points, String form) {
        this.rank = rank;
        this.team = team;
        this.played = played;
        this.win = win;
        this.draw = draw;
        this.lose = lose;
        this.goalsFor = goalsFor;
        this.goalsAgainst = goalsAgainst;
        this.goalsDiff = goalsDiff;
        this.points = points;
        this.form = form;
    }

    public int getRank() { return rank; }
    public Team getTeam() { return team; }
    public int getPlayed() { return played; }
    public int getWin() { return win; }
    public int getDraw() { return draw; }
    public int getLose() { return lose; }
    public int getGoalsFor() { return goalsFor; }
    public int getGoalsAgainst() { return goalsAgainst; }
    public int getGoalsDiff() { return goalsDiff; }
    public int getPoints() { return points; }
    public String getForm() { return form; }
}
