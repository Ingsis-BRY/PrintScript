package com.printscript.lexer

sealed interface SourceChar {

    data class Character(val value: Char) : SourceChar

    data object EndOfSource : SourceChar
}
