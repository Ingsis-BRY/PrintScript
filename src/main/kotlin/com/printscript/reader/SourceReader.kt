package com.printscript.reader

class SourceReader(
    private val source: CharSequence
) {
    private var index: Int = 0

    fun peek(): Char? {
        return if (hasNext()) source[index] else null
    }

    fun next(): Char? {
        return if (hasNext()) source[index++] else null
    }

    fun hasNext(): Boolean {
        return index < source.length
    }
}
