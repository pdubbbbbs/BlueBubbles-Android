package com.bluebubbles.messaging.data.local.dao

import androidx.room.*
import com.bluebubbles.messaging.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

  @Query("SELECT * FROM messages WHERE chatGuid = :chatGuid ORDER BY dateCreated DESC")
  fun getMessagesForChat(chatGuid: String): Flow<List<MessageEntity>>

  @Query("SELECT * FROM messages WHERE chatGuid = :chatGuid ORDER BY dateCreated DESC LIMIT :limit OFFSET :offset")
  suspend fun getMessagesForChatPaged(chatGuid: String, limit: Int, offset: Int): List<MessageEntity>

  @Query("SELECT * FROM messages WHERE guid = :guid")
  suspend fun getMessage(guid: String): MessageEntity?

  @Query("SELECT * FROM messages WHERE guid = :guid")
  fun observeMessage(guid: String): Flow<MessageEntity?>

  @Query("SELECT * FROM messages WHERE chatGuid = :chatGuid ORDER BY dateCreated DESC LIMIT 1")
  suspend fun getLastMessage(chatGuid: String): MessageEntity?

  @Query("""
    SELECT * FROM messages
    WHERE chatGuid = :chatGuid AND text LIKE '%' || :query || '%'
    ORDER BY dateCreated DESC
  """)
  fun searchMessagesInChat(chatGuid: String, query: String): Flow<List<MessageEntity>>

  @Query("""
    SELECT * FROM messages
    WHERE text LIKE '%' || :query || '%'
    ORDER BY dateCreated DESC
    LIMIT 100
  """)
  fun searchAllMessages(query: String): Flow<List<MessageEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessage(message: MessageEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertMessages(messages: List<MessageEntity>)

  @Update
  suspend fun updateMessage(message: MessageEntity)

  @Query("UPDATE messages SET dateDelivered = :deliveredTime WHERE guid = :guid")
  suspend fun markDelivered(guid: String, deliveredTime: Long)

  @Query("UPDATE messages SET dateRead = :readTime WHERE guid = :guid")
  suspend fun markRead(guid: String, readTime: Long)

  @Query("UPDATE messages SET error = :errorCode WHERE guid = :guid")
  suspend fun setError(guid: String, errorCode: Int)

  @Query("UPDATE messages SET isSending = :sending WHERE guid = :guid")
  suspend fun setSending(guid: String, sending: Boolean)

  @Query("UPDATE messages SET guid = :newGuid, isSending = 0 WHERE tempGuid = :tempGuid")
  suspend fun confirmSent(tempGuid: String, newGuid: String)

  @Delete
  suspend fun deleteMessage(message: MessageEntity)

  @Query("DELETE FROM messages WHERE guid = :guid")
  suspend fun deleteMessageByGuid(guid: String)

  @Query("DELETE FROM messages WHERE chatGuid = :chatGuid")
  suspend fun deleteMessagesForChat(chatGuid: String)

  @Query("DELETE FROM messages")
  suspend fun deleteAllMessages()

  @Query("SELECT COUNT(*) FROM messages WHERE chatGuid = :chatGuid")
  suspend fun getMessageCount(chatGuid: String): Int

  @Query("SELECT * FROM messages WHERE isSending = 1")
  suspend fun getPendingMessages(): List<MessageEntity>
}
