package com.nanan.coc.di

import android.content.Context
import androidx.room.Room
import com.nanan.coc.data.api.ClashApiService
import com.nanan.coc.data.api.TencentDocService
import com.nanan.coc.data.cache.AppDatabase
import com.nanan.coc.data.cache.LayoutDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()
    }

    @Provides
    @Singleton
    @Named("tencent")
    fun provideTencentRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://docs.qq.com/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("clash")
    fun provideClashRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.clashofclans.com/")
            .client(client)
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTencentDocService(@Named("tencent") retrofit: Retrofit): TencentDocService {
        return retrofit.create(TencentDocService::class.java)
    }

    @Provides
    @Singleton
    fun provideClashApiService(@Named("clash") retrofit: Retrofit): ClashApiService {
        return retrofit.create(ClashApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "coc_layouts.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideLayoutDao(db: AppDatabase): LayoutDao {
        return db.layoutDao()
    }
}
