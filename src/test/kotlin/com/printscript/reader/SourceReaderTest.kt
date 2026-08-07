package com.printscript.com.printscript.reader

import com.printscript.reader.SourceReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SourceReaderTest {

    @Test
    fun `peek should return next character without consuming`() {
        val reader = SourceReader("abc")

        assertEquals('a', reader.peek())
        assertEquals('a', reader.peek())
    }

    @Test
    fun `next should return next character and advance`() {
        val reader = SourceReader("abc")

        assertEquals('a', reader.next())
        assertEquals('b', reader.next())
        assertEquals('c', reader.next())
    }

    @Test
    fun `hasNext should return true while there are characters`() {
        val reader = SourceReader("abc")

        assertTrue(reader.hasNext())

        reader.next()
        reader.next()

        assertTrue(reader.hasNext())

        reader.next()

        assertFalse(reader.hasNext())
    }

    @Test
    fun `peek should return null when source is exhausted`() {
        val reader = SourceReader("a")

        reader.next()

        assertEquals(null, reader.peek())
    }

    @Test
    fun `next should return null when source is exhausted`() {
        val reader = SourceReader("a")

        reader.next()

        assertEquals(null, reader.next())
    }

    @Test
    fun `empty source should have no next character`() {
        val reader = SourceReader("")

        assertFalse(reader.hasNext())
        assertEquals(null, reader.peek())
        assertEquals(null, reader.next())
    }
}