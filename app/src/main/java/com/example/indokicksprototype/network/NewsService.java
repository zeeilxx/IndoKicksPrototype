package com.example.indokicksprototype.network;

import retrofit2.Call;
import retrofit2.http.GET;

public interface NewsService {

    @GET("api/v1/news")
    Call<NewsApiResponse> getNews();
}
