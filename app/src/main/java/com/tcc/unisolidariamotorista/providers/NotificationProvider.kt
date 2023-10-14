package com.tcc.unisolidariamotorista.providers

import com.tcc.unisolidariamotorista.api.IFCMApi
import com.tcc.unisolidariamotorista.api.RetrofitClient
import com.tcc.unisolidariamotorista.models.FCMBody
import com.tcc.unisolidariamotorista.models.FCMResponse
import retrofit2.Call

class NotificationProvider {

    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClient.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}