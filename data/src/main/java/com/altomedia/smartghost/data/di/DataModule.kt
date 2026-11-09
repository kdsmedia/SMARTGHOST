package com.altomedia.smartghost.data.di

import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.HttpClientFactory
import com.altomedia.smartghost.data.diagnostics.OkHttpEndpointChecker
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = HttpClientFactory.create()

    @Provides
    @Singleton
    fun provideEndpointChecker(client: OkHttpClient): EndpointChecker =
        OkHttpEndpointChecker(client)
}
