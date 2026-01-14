package com.bluebubbles.messaging.data.local.dao

import androidx.room.*
import com.bluebubbles.messaging.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

  @Query("SELECT * FROM conversations ORDER BY lastMessageDate DESC")
  fun getAllConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isPinned = 1 ORDER BY lastMessageDate DESC")
  fun getPinnedConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE isArchived = 0 ORDER BY lastMessageDate DESC")
  fun getActiveConversations(): Flow<List<ConversationEntity>>

  @Query("SELECT * FROM conversations WHERE guid = :guid")
  suspend fun getConversation(guid: String): ConversationEntity?

  @Query("SELECT * FROM conversations WHERE guid = :guid")
  fun observeConversation(guid: String): Flow<ConversationEntity?>

  @Query("""
    SELECT * FROM conversations
    WHERE displayName LIKE '%' || :query || '%'
    OR chatIdentifier LIKE '%' || :query || '%'
    ORDER BY lastMessageDate DESC
  """)
  fun searchConversations(query: String): Flow<List<ConversationEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversation(conversation: ConversationEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertConversations(conversations: List<ConversationEntity>)

  @Update
  suspend fun updateConversation(conversation: ConversationEntity)

  @Query("UPDATE conversations SET unreadCount = :count WHERE guid = :guid")
  suspend fun updateUnreadCount(guid: String, count: Int)

  @Query("UPDATE conversations SET unreadCount = 0 WHERE guid = :guid")
  suspend fun markAsRead(guid: String)

  @Query("UPDATE conversations SET isPinned = :pinned WHERE guid = :guid")
  suspend fun setPinned(guid: String, pinned: Boolean)

  @Query("UPDATE conversations SET isMuted = :muted WHERE guid = :guid")
  suspend fun setMuted(guid: String, muted: Boolean)

  @Query("UPDATE conversations SET isArchived = :archived WHERE guid = :guid")
  suspend fun setArchived(guid: String, archived: Boolean)

  @Delete
  suspend fun deleteConversation(conversation: ConversationEntity)

  @Query("DELETE FROM conversations WHERE guid = :guid")
  suspend fun deleteConversationByGuid(guid: String)

  @Query("DELETE FROM conversations")
  suspend fun deleteAllConversations()

  @Query("SELECT COUNT(*) FROM conversations WHERE unreadCount > 0")
  fun getTotalUnreadCount(): Flow<Int>
}
