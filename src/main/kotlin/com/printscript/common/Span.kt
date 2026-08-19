package com.printscript.common

/**
 * The stretch of source an error points at, from the first character of the
 * offending text to its last. Both ends are inclusive, so a one-character
 * problem has [start] equal to [end].
 */
data class Span(
    val start: Position,
    val end: Position
) {
    companion object {

        /**
         * the span of a single character
         */
        fun at(position: Position): Span = Span(position, position)
    }
}
