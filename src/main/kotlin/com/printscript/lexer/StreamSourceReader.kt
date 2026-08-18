package com.printscript.lexer

import java.io.Reader

/**
 * Reads the source from a [Reader], keeping only the current character and a
 * single lookahead in memory.
 *
 * The stream is never read past the lookahead, so an arbitrarily large (or
 * endless) source can be consumed without loading it whole.
 */
class StreamSourceReader(
    private val reader: Reader
) : SourceReader {

    private var exhausted: Boolean = false
    private var current: SourceChar = readChar()
    private var lookahead: SourceChar = readChar()

    override fun peek(): SourceChar {
        return current
    }

    override fun peekNext(): SourceChar {
        return lookahead
    }

    override fun next(): SourceChar {
        if (!hasNext()) {
            return SourceChar.EndOfSource
        }

        val consumed = current

        current = lookahead
        lookahead = readChar()

        return consumed
    }

    override fun hasNext(): Boolean {
        return current !is SourceChar.EndOfSource
    }

    /**
     * reads a single character, returning end of source once the stream runs out
     * an exhausted stream is never read again
     */
    private fun readChar(): SourceChar {
        if (exhausted) {
            return SourceChar.EndOfSource
        }

        val code = reader.read()

        if (code < 0) {
            exhausted = true
            return SourceChar.EndOfSource
        }

        return SourceChar.Character(code.toChar())
    }
}
