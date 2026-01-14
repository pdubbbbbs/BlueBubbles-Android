package com.bluebubbles.messaging.data.local.dao

import androidx.room.*
import com.bluebubbles.messaging.data.local.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

  @Query("SELECT * FROM attachments WHERE messageGuid = :messageGuid")
  fun getAttachmentsForMessage(messageGuid: String): Flow<List<AttachmentEntity>>

  @Query("SELECT * FROM attachments WHERE messageGuid = :messageGuid")
  suspend fun getAttachmentsForMessageSync(messageGuid: String): List<AttachmentEntity>

  @Query("SELECT * FROM attachments WHERE guid = :guid")
  suspend fun getAttachment(guid: String): AttachmentEntity?

  @Query("SELECT * FROM attachments WHERE isDownloaded = 0")
  suspend fun getPendingDownloads(): List<AttachmentEntity>

  @Query("SELECT * FROM attachments WHERE mimeType LIKE 'image/%'")
  fun getAllImages(): Flow<List<AttachmentEntity>>

  @Query("SELECT * FROM attachments WHERE mimeType LIKE 'video/%'")
  fun getAllVideos(): Flow<List<AttachmentEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachment(attachment: AttachmentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAttachments(attachments: List<AttachmentEntity>)

  @Update
  suspend fun updateAttachment(attachment: AttachmentEntity)

  @Query("UPDATE attachments SET localPath = :localPath, isDownloaded = 1 WHERE guid = :guid")
  suspend fun markDownloaded(guid: String, localPath: String)

  @Query("UPDATE attachments SET downloadProgress = :progress WHERE guid = :guid")
  suspend fun updateDownloadProgress(guid: String, progress: Int)

  @Delete
  suspend fun deleteAttachment(attachment: AttachmentEntity)

  @Query("DELETE FROM attachments WHERE guid = :guid")
  suspend fun deleteAttachmentByGuid(guid: String)

  @Query("DELETE FROM attachments WHERE messageGuid = :messageGuid")
  suspend fun deleteAttachmentsForMessage(messageGuid: String)

  @Query("DELETE FROM attachments")
  suspend fun deleteAllAttachments()

  @Query("SELECT SUM(totalBytes) FROM attachments WHERE isDownloaded = 1")
  suspend fun getTotalDownloadedSize(): Long?
}
