package com.example.indokicksprototype.network;

import java.util.List;

public class LineupItem {

    public Team team;
    public Coach coach;
    public String formation;
    public List<PlayerWrapper> startXI;
    public List<PlayerWrapper> substitutes;

    public static class Team {
        public int id;
        public String name;
        public String logo;
    }

    public static class Coach {
        public int id;
        public String name;
        public String photo;
    }

    // API mengembalikan array "startXI": [{ "player": { ... } }]
    public static class PlayerWrapper {
        public Player player;
    }

    public static class Player {
        public int id;
        public String name;
        public int number;
        public String pos;   // posisi (G, D, M, F)
        public String grid;  // posisi di lapangan (misal "1:2")
    }
}
