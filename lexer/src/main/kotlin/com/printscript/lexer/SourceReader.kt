package com.printscript.lexer

interface SourceReader {
    fun peek(): SourceChar

    fun peekNext(): SourceChar

    fun next(): SourceChar

    fun hasNext(): Boolean
}
