package com.bluebubbles.messaging.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.bluebubbles.messaging.data.local.dao.AttachmentDao
import com.bluebubbles.messaging.data.local.dao.ConversationDao
import com.bluebubbles.messaging.data.local.dao.MessageDao
import com.bluebubbles.messaging.data.local.entity.AttachmentEntity
import com.bluebubbles.messaging.data.local.entity.ConversationEntity
import com.bluebubbles.messaging.data.local.entity.MessageEntity

@Database(
  entities = [
    ConversationEntity::class,
    MessageEntity::class,
    AttachmentEntity::class
  ],
  version = 1,
  exportSchema = true
)
abstract class BlueBubblesDatabase : RoomDatabase() {

  abstract fun conversationDao(): ConversationDao
  abstract fun messageDao(): MessageDao
  abstract fun attachmentDao(): AttachmentDao

  companion object {
    const val DATABASE_NAME = "bluebubbles_db"
  }
}
