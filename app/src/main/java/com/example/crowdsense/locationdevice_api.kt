package com.example.crowdsense

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
interface locationdevice_api {
    @POST("/r/")
    fun add_cred(@Body locationEvent: LocationEvent1): Call<ResponseBody>
}