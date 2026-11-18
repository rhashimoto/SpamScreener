package com.shoestringresearch.spamscreener.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberHelperTest {

  @Test
  fun testTenDigits() {
    val formatted = PhoneNumberHelper.formatForDisplay("8005551234")
    assertEquals("800-555-1234", formatted)
  }

  @Test
  fun testElevenDigitsWithCountryCode() {
    val formatted = PhoneNumberHelper.formatForDisplay("18005551234")
    assertEquals("800-555-1234", formatted)
  }

  @Test
  fun testWithSpecialCharacters() {
    val formatted = PhoneNumberHelper.formatForDisplay("+1 (800) 555-1234")
    assertEquals("800-555-1234", formatted)
  }
}
