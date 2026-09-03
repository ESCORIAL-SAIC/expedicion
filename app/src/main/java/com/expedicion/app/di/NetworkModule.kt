package com.expedicion.app.di

import com.expedicion.app.BuildConfig
import com.expedicion.app.data.config.ApiConfigStore
import com.expedicion.app.data.remote.ApiService
import com.expedicion.app.data.remote.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json, apiConfigStore: ApiConfigStore): Retrofit {
        // La URL configurada por el usuario en runtime (ApiConfigScreen) manda sobre el default de
        // build-time. Retrofit no soporta cambiar el baseUrl de una instancia ya creada, por eso
        // cualquier cambio posterior de URL exige reiniciar el proceso (ver AppRestarter) para que
        // este provider se vuelva a ejecutar con el valor persistido actualizado.
        val baseUrl = apiConfigStore.readBlocking() ?: BuildConfig.API_BASE_URL
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)
}
