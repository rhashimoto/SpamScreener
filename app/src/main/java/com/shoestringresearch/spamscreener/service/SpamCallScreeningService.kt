package com.shoestringresearch.spamscreener.service

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import androidx.core.content.ContextCompat
import com.shoestringresearch.spamscreener.data.ScreeningPreferencesRepository
import com.shoestringresearch.spamscreener.notification.ScreeningNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class SpamCallScreeningService : CallScreeningService() {

  companion object {
    private const val TAG = "SpamCallScreening"
  }

  private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private lateinit var repository: ScreeningPreferencesRepository

  override fun onCreate() {
    super.onCreate()
    repository = ScreeningPreferencesRepository(applicationContext)
  }

  override fun onScreenCall(callDetails: Call.Details) {
    val isIncoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
    if (!isIncoming) {
      respondAllow(callDetails)
      return
    }

    val handle: Uri? = callDetails.handle
    val rawNumber = handle?.schemeSpecificPart

    serviceScope.launch {
      try {
        val settings = repository.getSettings()
        if (!settings.isScreeningEnabled) {
          respondAllow(callDetails)
          return@launch
        }

        if (rawNumber.isNullOrEmpty()) {
          respondAllow(callDetails)
          return@launch
        }

        // Rule 1: If the ANI matches any number in the user's contacts,
        // do not screen the call, and stop further processing.
        if (isNumberInContacts(rawNumber)) {
          Log.d(TAG, "Call from $rawNumber matches contacts. Allowing.")
          respondAllow(callDetails)
          return@launch
        }

        // Rule 2: If the ANI matches any Screening Patterns entry,
        // send the call to voicemail, post a notification "Screened ###-###-####"
        // with a tap action that opens the phone dialer with that number, and stop further processing.
        if (matchesScreeningPatterns(rawNumber, settings.screeningPatterns)) {
          val callerDisplayName = callDetails.callerDisplayName
          Log.d(TAG, "Call from $rawNumber ($callerDisplayName) matches screening pattern.")
          respondVoicemail(callDetails)
          postNotificationSafely(rawNumber, callerDisplayName)
          return@launch
        }

        // Rule 3: Do not screen the call and stop further processing.
        Log.d(TAG, "Call from $rawNumber did not match any patterns. Allowing.")
        respondAllow(callDetails)
      } catch (e: Exception) {
        Log.e(TAG, "Error screening call, defaulting to allow", e)
        respondAllow(callDetails)
      }
    }
  }

  private fun isNumberInContacts(phoneNumber: String): Boolean {
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
      != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    val lookupUri = Uri.withAppendedPath(
      ContactsContract.PhoneLookup.ENTERPRISE_CONTENT_FILTER_URI,
      Uri.encode(phoneNumber)
    )
    val projection = arrayOf(ContactsContract.PhoneLookup._ID)

    return try {
      contentResolver.query(lookupUri, projection, null, null, null)?.use { cursor ->
        cursor.moveToFirst()
      } ?: false
    } catch (e: Exception) {
      Log.e(TAG, "Failed querying contacts", e)
      false
    }
  }

  private fun matchesScreeningPatterns(phoneNumber: String, patterns: List<String>): Boolean {
    val digitsOnly = phoneNumber.filter { it.isDigit() }
    for (patternString in patterns) {
      if (patternString.isBlank()) continue
      try {
        val pattern = Pattern.compile(patternString)
        if (pattern.matcher(digitsOnly).find() || pattern.matcher(digitsOnly).matches()) {
          return true
        }
      } catch (e: PatternSyntaxException) {
        // Skip invalid regex entries
        Log.w(TAG, "Invalid pattern skipped: $patternString", e)
      }
    }
    return false
  }

  private fun respondAllow(callDetails: Call.Details) {
    val response = CallResponse.Builder()
      .setDisallowCall(false)
      .setRejectCall(false)
      .setSilenceCall(false)
      .setSkipCallLog(false)
      .setSkipNotification(false)
      .build()
    respondToCall(callDetails, response)
  }

  private fun respondVoicemail(callDetails: Call.Details) {
    val response = if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.CINNAMON_BUN_2) {
      Log.d(TAG, "Rejecting call as missed.")
      CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectedAsMissed(true)
        .setSkipNotification(false)
        .setSkipCallLog(false)
        .build()
    } else {
      Log.d(TAG, "Rejecting call.")
      CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipNotification(false)
        .setSkipCallLog(false)
        .build()
    }
    respondToCall(callDetails, response)
  }

  private fun postNotificationSafely(phoneNumber: String, callerDisplayName: String? = null) {
    try {
      ScreeningNotificationHelper.postScreenedCallNotification(
        applicationContext,
        phoneNumber,
        callerDisplayName
      )
    } catch (e: Exception) {
      Log.e(TAG, "Failed to post notification", e)
    }
  }
}
