package com.bluebubbles.messaging.data.repository

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class ContactInfo(
  val contactId: Long,
  val displayName: String,
  val photoUri: Uri?,
  val phoneNumbers: List<String> = emptyList(),
  val emails: List<String> = emptyList()
)

@Singleton
class ContactRepository @Inject constructor(
  @ApplicationContext private val context: Context
) {

  private val contactCache = mutableMapOf<String, ContactInfo?>()

  suspend fun findContactByAddress(address: String): ContactInfo? {
    // Check cache first
    contactCache[normalizeAddress(address)]?.let { return it }

    return withContext(Dispatchers.IO) {
      val contact = if (address.contains("@")) {
        findContactByEmail(address)
      } else {
        findContactByPhone(address)
      }

      // Cache the result (including null)
      contactCache[normalizeAddress(address)] = contact
      contact
    }
  }

  private fun findContactByPhone(phoneNumber: String): ContactInfo? {
    val normalizedPhone = normalizePhoneNumber(phoneNumber)

    val uri = Uri.withAppendedPath(
      ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
      Uri.encode(normalizedPhone)
    )

    val projection = arrayOf(
      ContactsContract.PhoneLookup._ID,
      ContactsContract.PhoneLookup.DISPLAY_NAME,
      ContactsContract.PhoneLookup.PHOTO_URI
    )

    return try {
      context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup._ID)
          val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
          val photoIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.PHOTO_URI)

          ContactInfo(
            contactId = if (idIndex >= 0) cursor.getLong(idIndex) else 0,
            displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else "",
            photoUri = if (photoIndex >= 0) cursor.getString(photoIndex)?.let { Uri.parse(it) } else null
          )
        } else null
      }
    } catch (e: Exception) {
      null
    }
  }

  private fun findContactByEmail(email: String): ContactInfo? {
    val selection = "${ContactsContract.CommonDataKinds.Email.ADDRESS} = ?"
    val selectionArgs = arrayOf(email.lowercase())

    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Email.CONTACT_ID,
      ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Email.PHOTO_URI
    )

    return try {
      context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        projection,
        selection,
        selectionArgs,
        null
      )?.use { cursor ->
        if (cursor.moveToFirst()) {
          val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
          val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
          val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.PHOTO_URI)

          ContactInfo(
            contactId = if (idIndex >= 0) cursor.getLong(idIndex) else 0,
            displayName = if (nameIndex >= 0) cursor.getString(nameIndex) ?: "" else "",
            photoUri = if (photoIndex >= 0) cursor.getString(photoIndex)?.let { Uri.parse(it) } else null
          )
        } else null
      }
    } catch (e: Exception) {
      null
    }
  }

  fun getAllContacts(): Flow<List<ContactInfo>> = flow {
    val contacts = mutableListOf<ContactInfo>()

    val projection = arrayOf(
      ContactsContract.Contacts._ID,
      ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
      ContactsContract.Contacts.PHOTO_URI,
      ContactsContract.Contacts.HAS_PHONE_NUMBER
    )

    try {
      context.contentResolver.query(
        ContactsContract.Contacts.CONTENT_URI,
        projection,
        "${ContactsContract.Contacts.HAS_PHONE_NUMBER} = 1",
        null,
        "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
      )?.use { cursor ->
        val idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID)
        val nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
        val photoIndex = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI)

        while (cursor.moveToNext()) {
          val contactId = if (idIndex >= 0) cursor.getLong(idIndex) else continue
          val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
          val photoUri = if (photoIndex >= 0) cursor.getString(photoIndex)?.let { Uri.parse(it) } else null

          if (!name.isNullOrEmpty()) {
            val phoneNumbers = getPhoneNumbers(contactId)
            val emails = getEmails(contactId)

            contacts.add(
              ContactInfo(
                contactId = contactId,
                displayName = name,
                photoUri = photoUri,
                phoneNumbers = phoneNumbers,
                emails = emails
              )
            )
          }
        }
      }
    } catch (e: Exception) {
      // Permission denied or other error
    }

    emit(contacts)
  }.flowOn(Dispatchers.IO)

  private fun getPhoneNumbers(contactId: Long): List<String> {
    val numbers = mutableListOf<String>()

    try {
      context.contentResolver.query(
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
      )?.use { cursor ->
        val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
        while (cursor.moveToNext()) {
          if (numberIndex >= 0) {
            cursor.getString(numberIndex)?.let { numbers.add(normalizePhoneNumber(it)) }
          }
        }
      }
    } catch (e: Exception) {
      // Ignore
    }

    return numbers.distinct()
  }

  private fun getEmails(contactId: Long): List<String> {
    val emails = mutableListOf<String>()

    try {
      context.contentResolver.query(
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
        "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
        arrayOf(contactId.toString()),
        null
      )?.use { cursor ->
        val emailIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
        while (cursor.moveToNext()) {
          if (emailIndex >= 0) {
            cursor.getString(emailIndex)?.let { emails.add(it.lowercase()) }
          }
        }
      }
    } catch (e: Exception) {
      // Ignore
    }

    return emails.distinct()
  }

  fun clearCache() {
    contactCache.clear()
  }

  private fun normalizeAddress(address: String): String {
    return if (address.contains("@")) {
      address.lowercase().trim()
    } else {
      normalizePhoneNumber(address)
    }
  }

  private fun normalizePhoneNumber(phone: String): String {
    // Remove all non-digit characters except leading +
    val cleaned = phone.replace(Regex("[^0-9+]"), "")
    // If it starts with +1, keep it. Otherwise normalize to US format
    return when {
      cleaned.startsWith("+") -> cleaned
      cleaned.length == 10 -> "+1$cleaned"
      cleaned.length == 11 && cleaned.startsWith("1") -> "+$cleaned"
      else -> cleaned
    }
  }
}
