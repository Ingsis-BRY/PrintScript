package com.printscript.lexer

class StringSourceReader(
    private val source: String
) : SourceReader {

    private var index: Int = 0

    override fun peek(): SourceChar {
        return charAt(index)
    }

    override fun peekNext(): SourceChar {
        return charAt(index + 1)
    }

    override fun next(): SourceChar {
        val consumed = charAt(index)

        if (consumed is SourceChar.Character) {
            index++
        }

        return consumed
    }

    override fun hasNext(): Boolean {
        return index < source.length
    }

    private fun charAt(position: Int): SourceChar {
        return if (position < source.length) {
            SourceChar.Character(source[position])
        } else {
            SourceChar.EndOfSource
        }
    }
}