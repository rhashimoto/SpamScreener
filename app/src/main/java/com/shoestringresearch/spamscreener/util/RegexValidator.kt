package com.shoestringresearch.spamscreener.util

import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

data class RegexValidationResult(
  val isValid: Boolean,
  val errorIndex: Int = -1,
  val errorMessage: String? = null
)

object RegexValidator {
  fun validate(pattern: String): RegexValidationResult {
    if (pattern.isEmpty()) {
      return RegexValidationResult(isValid = true)
    }
    return try {
      Pattern.compile(pattern)
      RegexValidationResult(isValid = true)
    } catch (e: PatternSyntaxException) {
      RegexValidationResult(
        isValid = false,
        errorIndex = e.index,
        errorMessage = e.description
      )
    } catch (e: Exception) {
      RegexValidationResult(
        isValid = false,
        errorIndex = -1,
        errorMessage = e.message
      )
    }
  }
}
