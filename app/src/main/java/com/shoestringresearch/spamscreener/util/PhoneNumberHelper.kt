package com.shoestringresearch.spamscreener.util

import android.telephony.PhoneNumberUtils
import java.util.Locale

object PhoneNumberHelper {
  /**
   * Formats a phone number for display as ###-###-#### when possible,
   * or falls back to standard PhoneNumberUtils formatting / raw number.
   */
  fun formatForDisplay(rawNumber: String): String {
    val digits = rawNumber.filter { it.isDigit() }
    return when (digits.length) {
      10 -> {
        "${digits.substring(0, 3)}-${digits.substring(3, 6)}-${digits.substring(6)}"
      }
      11 -> {
        if (digits.startsWith("1")) {
          val tenDigits = digits.substring(1)
          "${tenDigits.substring(0, 3)}-${tenDigits.substring(3, 6)}-${tenDigits.substring(6)}"
        } else {
          PhoneNumberUtils.formatNumber(rawNumber, Locale.getDefault().country) ?: rawNumber
        }
      }
      else -> {
        PhoneNumberUtils.formatNumber(rawNumber, Locale.getDefault().country) ?: rawNumber
      }
    }
  }
}
