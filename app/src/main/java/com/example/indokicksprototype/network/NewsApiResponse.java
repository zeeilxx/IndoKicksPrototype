package com.example.indokicksprototype.network;

import java.util.List;

public class NewsApiResponse {
    public boolean success;
    public int status_code;
    public String message;
    public List<NewsItemDto> data;

    public static class NewsItemDto {
        public String title;
        public String thumbnail;
        public String url;
    }
}
