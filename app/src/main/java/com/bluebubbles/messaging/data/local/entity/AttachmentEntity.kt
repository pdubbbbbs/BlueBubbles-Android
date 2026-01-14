package com.bluebubbles.messaging.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bluebubbles.messaging.data.models.Attachment

@Entity(
  tableName = "attachments",
  foreignKeys = [
    ForeignKey(
      entity = MessageEntity::class,
      parentColumns = ["guid"],
      childColumns = ["messageGuid"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["messageGuid"])]
)
data class AttachmentEntity(
  @PrimaryKey
  val guid: String,
  val messageGuid: String,
  val mimeType: String?,
  val fileName: String?,
  val localPath: String?, // Downloaded file path
  val width: Int?,
  val height: Int?,
  val totalBytes: Long?,
  val isSticker: Boolean = false,
  val isDownloaded: Boolean = false,
  val downloadProgress: Int = 0
)

fun AttachmentEntity.toAttachment(): Attachment {
  return Attachment(
    guid = guid,
    mimeType = mimeType,
    fileName = fileName,
    filePath = localPath,
    width = width,
    height = height,
    totalBytes = totalBytes,
    isSticker = isSticker
  )
}

fun Attachment.toEntity(messageGuid: String): AttachmentEntity {
  return AttachmentEntity(
    guid = guid,
    messageGuid = messageGuid,
    mimeType = mimeType,
    fileName = fileName,
    localPath = filePath,
    width = width,
    height = height,
    totalBytes = totalBytes,
    isSticker = isSticker
  )
}
