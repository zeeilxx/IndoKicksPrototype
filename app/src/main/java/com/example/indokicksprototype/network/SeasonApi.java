package com.example.indokicksprototype.network;

public class SeasonApi {

    public static boolean useBackend(int season) {
        return season == 2024 || season == 2025;
    }

    public static boolean useExternal(int season) {
        return season >= 2021 && season <= 2023;
    }

    public static int logoSeasonForExternal(int seasonRequested) {
        if (useExternal(seasonRequested)) return seasonRequested;
        return 2023;
    }
}
