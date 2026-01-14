package com.bluebubbles.messaging.di

import android.content.Context
import androidx.room.Room
import com.bluebubbles.messaging.data.local.BlueBubblesDatabase
import com.bluebubbles.messaging.data.local.dao.AttachmentDao
import com.bluebubbles.messaging.data.local.dao.ConversationDao
import com.bluebubbles.messaging.data.local.dao.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

  @Provides
  @Singleton
  fun provideDatabase(@ApplicationContext context: Context): BlueBubblesDatabase {
    return Room.databaseBuilder(
      context,
      BlueBubblesDatabase::class.java,
      BlueBubblesDatabase.DATABASE_NAME
    )
      .fallbackToDestructiveMigration()
      .build()
  }

  @Provides
  @Singleton
  fun provideConversationDao(database: BlueBubblesDatabase): ConversationDao {
    return database.conversationDao()
  }

  @Provides
  @Singleton
  fun provideMessageDao(database: BlueBubblesDatabase): MessageDao {
    return database.messageDao()
  }

  @Provides
  @Singleton
  fun provideAttachmentDao(database: BlueBubblesDatabase): AttachmentDao {
    return database.attachmentDao()
  }
}
