package com.shoestringresearch.spamscreener.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexValidatorTest {

  @Test
  fun testEmptyPatternIsValid() {
    val result = RegexValidator.validate("")
    assertTrue(result.isValid)
  }

  @Test
  fun testValidPattern() {
    val result = RegexValidator.validate("^\\+?1?[2-9]\\d{2}[2-9]\\d{6}$")
    assertTrue(result.isValid)
  }

  @Test
  fun testInvalidPatternUnclosedBracket() {
    val result = RegexValidator.validate("abc[def")
    assertFalse(result.isValid)
    assertTrue(result.errorIndex >= 0)
  }

  @Test
  fun testInvalidPatternDanglingMetaCharacter() {
    val result = RegexValidator.validate("+12345")
    assertFalse(result.isValid)
    assertEquals(0, result.errorIndex)
  }
}
