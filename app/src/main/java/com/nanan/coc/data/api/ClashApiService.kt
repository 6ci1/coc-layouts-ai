package com.nanan.coc.data.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface ClashApiService {

    @GET("v1/players/{tag}")
    suspend fun getPlayerInfo(
        @Path("tag") tag: String,
        @Header("Authorization") apiKey: String
    ): String
}
