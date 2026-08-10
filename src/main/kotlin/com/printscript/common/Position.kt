package com.printscript.common

/**
 * Represents a position in the source code.
 *
 * Both line and column are 1-based.
 */
data class Position(
    val line: Int,
    val column: Int
)