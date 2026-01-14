package com.bluebubbles.messaging.di

import android.content.Context
import com.bluebubbles.messaging.data.api.BlueBubblesApi
import com.bluebubbles.messaging.data.repository.ChatRepository
import com.bluebubbles.messaging.data.repository.ServerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    val loggingInterceptor = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BODY
    }

    val authInterceptor = Interceptor { chain ->
      val request = chain.request().newBuilder()
        .addHeader("Accept", "application/json")
        .addHeader("Content-Type", "application/json")
        // Password header will be added dynamically
        .build()
      chain.proceed(request)
    }

    return OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(loggingInterceptor)
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
  }

  @Provides
  @Singleton
  fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
    // Default URL - will be updated when server is configured
    return Retrofit.Builder()
      .baseUrl("http://localhost:1234/")
      .client(okHttpClient)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
  }

  @Provides
  @Singleton
  fun provideBlueBubblesApi(retrofit: Retrofit): BlueBubblesApi {
    return retrofit.create(BlueBubblesApi::class.java)
  }

  @Provides
  @Singleton
  fun provideChatRepository(api: BlueBubblesApi): ChatRepository {
    return ChatRepository(api)
  }

  @Provides
  @Singleton
  fun provideServerRepository(
    @ApplicationContext context: Context,
    api: BlueBubblesApi
  ): ServerRepository {
    return ServerRepository(context, api)
  }
}
