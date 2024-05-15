package com.ItInfraApp.AlertCar.model

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection

object Client {
    private var rertofit: Retrofit? = null

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val hostnameVerifier = HostnameVerifier { hostname, session ->
        if (hostname == "svr.kiwiwip.duckdns.org") true else HttpsURLConnection.getDefaultHostnameVerifier()
            .verify(hostname, session)
    }


    val apiService: ApiService by lazy {
        getRetrogfit().create(ApiService::class.java)
    }

    private fun getRetrogfit(): Retrofit {
        if (rertofit == null) {
            rertofit = Retrofit.Builder()
                .baseUrl("https://svr.kiwiwip.duckdns.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(
                    OkHttpClient.Builder()
                        .addInterceptor(logging)
                        .hostnameVerifier(hostnameVerifier)
                        .build()
                )
                .build()
        }

        return rertofit!!
    }
}

