package com.shoestringresearch.spamscreener.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

class SpamCallScreeningServiceTest {

  private fun matchesScreeningPatterns(phoneNumber: String, patterns: List<String>): Boolean {
    val digitsOnly = phoneNumber.filter { it.isDigit() }
    for (patternString in patterns) {
      if (patternString.isBlank()) continue
      try {
        val pattern = Pattern.compile(patternString)
        if (pattern.matcher(digitsOnly).find() || pattern.matcher(digitsOnly).matches()) {
          return true
        }
      } catch (_: PatternSyntaxException) {
        // Skip invalid regex entries
      }
    }
    return false
  }

  @Test
  fun testMatchesFormattedPhoneNumberAgainstDigitsRegex() {
    val incomingAni = "+1 (800) 555-1234"
    val patterns = listOf("^1?800\\d{7}$")
    assertTrue(matchesScreeningPatterns(incomingAni, patterns))
  }

  @Test
  fun testDoesNotMatchNonMatchingDigits() {
    val incomingAni = "+1 (212) 555-1234"
    val patterns = listOf("^1?800\\d{7}$")
    assertFalse(matchesScreeningPatterns(incomingAni, patterns))
  }
}
