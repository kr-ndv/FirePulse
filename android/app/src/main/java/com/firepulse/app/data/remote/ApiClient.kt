package com.firepulse.app.data.remote
import com.firepulse.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object ApiClient {
 private val logger=HttpLoggingInterceptor().apply { level=if(BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE }
 private val client=OkHttpClient.Builder().addInterceptor(logger).connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).build()
 val firePulseApi:FirePulseApi by lazy { Retrofit.Builder().baseUrl(BuildConfig.API_BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build().create(FirePulseApi::class.java) }
}
