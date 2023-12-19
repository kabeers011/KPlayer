package com.kbizsoft.KPlayer.java;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface VideoService {
    @GET("temp")
    Call<ResponseBody> getVideoData(@Query("time") String time);
}
