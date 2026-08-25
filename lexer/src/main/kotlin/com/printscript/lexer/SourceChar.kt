package com.printscript.lexer

/**
 * What a [SourceReader] can hand over: a character, a clean end, or a source
 * that broke while being read.
 *
 * [Failed] exists so an I/O error travels as data instead of as an exception,
 * and so it stays distinguishable from [EndOfSource] — a truncated read must
 * not look like a file that simply ended.
 */
sealed interface SourceChar {
    data class Character(
        val value: Char,
    ) : SourceChar

    data object EndOfSource : SourceChar

    data class Failed(
        val message: String,
    ) : SourceChar
}
