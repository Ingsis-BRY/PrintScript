package com.printscript.parser

import com.printscript.common.Position
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TokenCursorTest {
    private val start = Position(1, 1)
    private val end = Position(1, 2)

    private val tokens =
        listOf(
            Token.IdentifierToken("x", start, end),
            Token.AssignToken("=", start, end),
            Token.NumberLiteralToken("42", "42", start, end),
            Token.SemicolonToken(";", start, end),
        )

    @Test
    fun `hasNext should be true while tokens remain`() {
        val cursor = TokenCursor(tokens)

        assertTrue(cursor.hasNext())

        cursor.consume()
        cursor.consume()
        cursor.consume()

        assertTrue(cursor.hasNext())

        cursor.consume()

        assertFalse(cursor.hasNext())
    }

    @Test
    fun `peek should return next token without consuming it`() {
        val cursor = TokenCursor(tokens)

        assertEquals(tokens[0], cursor.peek())
        assertTrue(cursor.hasNext())

        assertEquals(tokens[0], cursor.consume())
        assertEquals(tokens[1], cursor.peek())
    }

    @Test
    fun `consume should traverse tokens in order`() {
        val cursor = TokenCursor(tokens)

        assertEquals(tokens[0], cursor.consume())
        assertEquals(tokens[1], cursor.consume())
        assertEquals(tokens[2], cursor.consume())
        assertEquals(tokens[3], cursor.consume())

        assertFalse(cursor.hasNext())
    }

    @Test
    fun `peek should return null at the end`() {
        val cursor = TokenCursor(tokens)

        repeat(tokens.size) {
            cursor.consume()
        }

        assertNull(cursor.peek())
    }

    @Test
    fun `consume should return null at the end`() {
        val cursor = TokenCursor(tokens)

        repeat(tokens.size) {
            cursor.consume()
        }

        assertNull(cursor.consume())
        assertFalse(cursor.hasNext())
    }

    @Test
    fun `empty cursor should have no next token`() {
        val cursor = TokenCursor(emptyList())

        assertFalse(cursor.hasNext())
        assertNull(cursor.peek())
        assertNull(cursor.consume())
    }
}
