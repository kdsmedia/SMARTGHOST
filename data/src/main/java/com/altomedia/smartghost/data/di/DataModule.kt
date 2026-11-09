package com.altomedia.smartghost.data.di

import com.altomedia.smartghost.data.diagnostics.EndpointChecker
import com.altomedia.smartghost.data.diagnostics.HttpClientFactory
import com.altomedia.smartghost.data.diagnostics.OkHttpEndpointChecker
import com.altomedia.smartghost.data.history.DiagnosticHistoryRepository
import com.altomedia.smartghost.data.history.DiagnosticHistoryStore
import com.altomedia.smartghost.data.prefs.SettingsRepository
import com.altomedia.smartghost.data.prefs.SettingsSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    abstract fun bindSettingsSource(impl: SettingsRepository): SettingsSource

    @Binds
    abstract fun bindHistoryStore(impl: DiagnosticHistoryRepository): DiagnosticHistoryStore
}

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
