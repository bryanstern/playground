package com.bryanstern.playground.api;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Headers;
import retrofit.http.Query;
import rx.Observable;

public interface GitHubService {
    @GET("/users")
    @Headers("Cache-Control: max-age=640000")
    Observable<List<GitHubUser>> listUsers(@Query("since") int since);
}
