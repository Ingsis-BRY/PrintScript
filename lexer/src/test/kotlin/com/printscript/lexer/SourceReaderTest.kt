package com.printscript.lexer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

abstract class SourceReaderTest {
    abstract fun readerOf(source: String): SourceReader

    @Test
    fun `empty source has nothing to read`() {
        val reader = readerOf("")

        assertFalse(reader.hasNext())
        assertEquals(SourceChar.EndOfSource, reader.peek())
        assertEquals(SourceChar.EndOfSource, reader.peekNext())
        assertEquals(SourceChar.EndOfSource, reader.next())
    }

    @Test
    fun `single character source is consumed once`() {
        val reader = readerOf("a")

        assertTrue(reader.hasNext())
        assertEquals(char('a'), reader.peek())
        assertEquals(SourceChar.EndOfSource, reader.peekNext())

        assertEquals(char('a'), reader.next())

        assertFalse(reader.hasNext())
        assertEquals(SourceChar.EndOfSource, reader.peek())
        assertEquals(SourceChar.EndOfSource, reader.next())
    }

    @Test
    fun `next traverses the source in order`() {
        val reader = readerOf("let x")

        assertEquals("let x", consumeAll(reader))
        assertFalse(reader.hasNext())
    }

    @Test
    fun `peek does not consume`() {
        val reader = readerOf("ab")

        repeat(3) {
            assertEquals(char('a'), reader.peek())
        }

        assertTrue(reader.hasNext())
        assertEquals(char('a'), reader.next())
        assertEquals(char('b'), reader.peek())
    }

    @Test
    fun `peekNext does not consume`() {
        val reader = readerOf("abc")

        repeat(3) {
            assertEquals(char('b'), reader.peekNext())
        }

        assertEquals(char('a'), reader.next())
        assertEquals(char('c'), reader.peekNext())
    }

    @Test
    fun `peekNext returns end of source on the last character`() {
        val reader = readerOf("ab")

        reader.next()

        assertEquals(char('b'), reader.peek())
        assertEquals(SourceChar.EndOfSource, reader.peekNext())
    }

    @Test
    fun `lookahead distinguishes a decimal from a trailing dot`() {
        val decimal = readerOf("3.5")
        val trailingDot = readerOf("3.;")

        repeat(2) {
            decimal.next()
            trailingDot.next()
        }

        assertEquals(char('5'), decimal.peek())
        assertEquals(char(';'), trailingDot.peek())
    }

    @Test
    fun `line breaks are returned as regular characters`() {
        val reader = readerOf("a\nb\r\nc")

        assertEquals("a\nb\r\nc", consumeAll(reader))
    }

    @Test
    fun `reading past the end keeps returning end of source`() {
        val reader = readerOf("a")

        assertEquals(char('a'), reader.next())

        repeat(3) {
            assertEquals(SourceChar.EndOfSource, reader.next())
            assertEquals(SourceChar.EndOfSource, reader.peek())
            assertEquals(SourceChar.EndOfSource, reader.peekNext())
            assertFalse(reader.hasNext())
        }
    }

    private fun char(value: Char): SourceChar = SourceChar.Character(value)

    private fun consumeAll(reader: SourceReader): String {
        val consumed = StringBuilder()

        while (reader.hasNext()) {
            when (val sourceChar = reader.next()) {
                is SourceChar.Character -> consumed.append(sourceChar.value)
                SourceChar.EndOfSource -> fail("next returned end of source while hasNext was true")
                is SourceChar.Failed -> fail("next returned a failure: ${sourceChar.message}")
            }
        }

        return consumed.toString()
    }
}
