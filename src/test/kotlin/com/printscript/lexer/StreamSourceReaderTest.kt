package com.printscript.lexer

import java.io.Reader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StreamSourceReaderTest : SourceReaderTest() {

    override fun readerOf(source: String): SourceReader {
        return StreamSourceReader(StringReader(source))
    }

    @Test
    fun `only the lookahead is read ahead of the consumed characters`() {
        val stream = CountingReader("x".repeat(10_000))
        val reader = StreamSourceReader(stream)

        assertEquals(2, stream.charsRead)

        repeat(5) {
            reader.next()
        }

        assertEquals(7, stream.charsRead)
    }

    @Test
    fun `an endless source can be read without exhausting it`() {
        val reader = StreamSourceReader(EndlessReader('x'))

        repeat(1_000) {
            assertEquals(SourceChar.Character('x'), reader.next())
        }

        assertTrue(reader.hasNext())
    }

    @Test
    fun `the stream is not read again once exhausted`() {
        val stream = CountingReader("ab")
        val reader = StreamSourceReader(stream)

        repeat(5) {
            reader.next()
        }

        assertEquals(3, stream.charsRead)
    }

    private class CountingReader(
        private val source: String
    ) : Reader() {

        var charsRead: Int = 0
            private set

        private var index: Int = 0

        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            if (length == 0) {
                return 0
            }

            charsRead++

            if (index >= source.length) {
                return -1
            }

            buffer[offset] = source[index++]

            return 1
        }

        override fun close() {}
    }

    private class EndlessReader(
        private val char: Char
    ) : Reader() {

        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            if (length == 0) {
                return 0
            }

            buffer[offset] = char

            return 1
        }

        override fun close() {}
    }
}
