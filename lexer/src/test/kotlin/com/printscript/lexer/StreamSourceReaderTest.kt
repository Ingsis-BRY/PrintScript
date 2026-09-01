package com.printscript.lexer

import java.io.IOException
import java.io.Reader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StreamSourceReaderTest : SourceReaderTest() {
    override fun readerOf(source: String): SourceReader = StreamSourceReader(StringReader(source))

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
        private val source: String,
    ) : Reader() {
        var charsRead: Int = 0
            private set

        private var index: Int = 0

        override fun read(
            buffer: CharArray,
            offset: Int,
            length: Int,
        ): Int {
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
        private val char: Char,
    ) : Reader() {
        override fun read(
            buffer: CharArray,
            offset: Int,
            length: Int,
        ): Int {
            if (length == 0) {
                return 0
            }

            buffer[offset] = char

            return 1
        }

        override fun close() {}
    }

    @Test
    fun `an io failure is handed over as a value, not thrown`() {
        val reader = StreamSourceReader(BreakingReader(after = 2))

        assertEquals(SourceChar.Character('a'), reader.next())
        assertEquals(SourceChar.Character('b'), reader.next())
        assertEquals(SourceChar.Failed("disk went away"), reader.next())
    }

    @Test
    fun `a source that breaks on the very first read still fails as a value`() {
        val reader = StreamSourceReader(BreakingReader(after = 0))

        assertEquals(SourceChar.Failed("disk went away"), reader.next())
    }

    @Test
    fun `the stream is not read again after it broke`() {
        val stream = BreakingReader(after = 1)
        val reader = StreamSourceReader(stream)

        repeat(5) {
            reader.next()
        }

        assertEquals(2, stream.reads)
    }

    @Test
    fun `a broken stream reports end of source once the failure was handed over`() {
        val reader = StreamSourceReader(BreakingReader(after = 0))

        assertEquals(SourceChar.Failed("disk went away"), reader.next())
        assertEquals(SourceChar.EndOfSource, reader.next())
        assertFalse(reader.hasNext())
    }

    /**
     * yields [after] characters and then throws, the way a stream backed by a
     * failing device would
     */
    private class BreakingReader(
        private val after: Int,
    ) : Reader() {
        var reads: Int = 0
            private set

        override fun read(
            buffer: CharArray,
            offset: Int,
            length: Int,
        ): Int {
            if (length == 0) {
                return 0
            }

            reads++

            if (reads > after) {
                throw IOException("disk went away")
            }

            buffer[offset] = 'a' + (reads - 1)

            return 1
        }

        override fun close() {}
    }
}
