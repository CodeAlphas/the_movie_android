package com.codealphas.themovie.presentation.map

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    abstract fun bindCurrentLocationProvider(impl: FusedCurrentLocationProvider): CurrentLocationProvider
}
