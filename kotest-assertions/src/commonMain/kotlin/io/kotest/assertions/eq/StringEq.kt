package io.kotest.assertions.eq

import io.kotest.assertions.diffLargeString
import io.kotest.assertions.failure
import io.kotest.assertions.show.Printed
import io.kotest.assertions.show.show
import io.kotest.assertions.supportsStringDiff
import io.kotest.mpp.sysprop

/**
 * An [Eq] implementation for String's that generates diffs for errors when the string inputs
 * are of a certain size. The min size for the diff is retrieved by [largeStringDiffMinSize].
 */
object StringEq : Eq<String> {

   override fun equals(actual: String, expected: String): Throwable? {
      return when {
         actual == expected -> null
         equalIgnoringWhitespace(actual, expected) ->
            failure("expected: ${escapeLineBreaks(expected)} but was: ${escapeLineBreaks(actual)}\n(contents match, but line-breaks differ; output has been escaped to show line-breaks)")
         supportsStringDiff() && useDiff(expected, actual) -> diff(expected, actual)
         else -> failure(expected.show(), actual.show())
      }
   }

   private fun equalIgnoringWhitespace(actual: String, expected: String): Boolean {
      val a = linebreaks.replace(expected, "")
      val b = linebreaks.replace(actual, "")
      return a == b
   }

   private fun diff(expected: String, actual: String): Throwable {
      val (expectedRepr, actualRepr) = diffLargeString(
         expected,
         actual
      )
      return failure(Printed(expectedRepr), Printed(actualRepr))
   }

   private fun useDiff(expected: String, actual: String): Boolean {
      val minSizeForDiff = largeStringDiffMinSize()
      return expected.lines().size >= minSizeForDiff
         && actual.lines().size >= minSizeForDiff &&
         sysprop("kotest.assertions.multi-line-diff") != "simple"
   }
}

private val linebreaks = Regex("\r?\n|\r")

fun escapeLineBreaks(input: String): String {
   return input
      .replace("\n", "\\n")
      .replace("\r", "\\r")
}

fun largeStringDiffMinSize() = sysprop("kotest.assertions.multi-line-diff-size", "50").toInt()
