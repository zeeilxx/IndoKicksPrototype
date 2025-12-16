package com.example.indokicksprototype.network;

import java.util.List;

// root response: { "response": [ ... ] }
public class FixturesApiResponse {
    private List<ApiFixture> response;

    public List<ApiFixture> getResponse() {
        return response;
    }
}
