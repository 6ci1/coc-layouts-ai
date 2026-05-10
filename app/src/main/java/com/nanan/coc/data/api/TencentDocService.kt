package com.nanan.coc.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface TencentDocService {

    @GET("dop-api/opendoc")
    suspend fun getDoc(
        @Query("id") docId: String,
        @Query("t") t: Int = 0,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 (Linux; Android 14)",
        @Header("Referer") referer: String = "https://docs.qq.com/doc/$docId"
    ): Response<String>
}
