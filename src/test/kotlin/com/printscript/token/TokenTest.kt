package com.printscript.token

import com.printscript.common.Position
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TokenTest {

    private val start = Position(1, 1)
    private val end = Position(1, 5)

    @Test
    fun `let token should expose its data`() {
        val token = Token.LetToken("let", start, end)

        assertEquals("let", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `identifier token should expose its data`() {
        val token = Token.IdentifierToken("foo", start, end)

        assertEquals("foo", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `type name token should expose its data`() {
        val token = Token.TypeNameToken("number", start, end)

        assertEquals("number", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `number literal token should preserve its text`() {
        val token = Token.NumberLiteralToken("3.50", "3.50", start, end)

        assertEquals("3.50", token.lexeme)
        assertEquals("3.50", token.value)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `string literal token should expose content without quotes`() {
        val token = Token.StringLiteralToken("\"hello\"", "hello", start, end)

        assertEquals("\"hello\"", token.lexeme)
        assertEquals("hello", token.value)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `assign token should expose its data`() {
        val token = Token.AssignToken("=", start, end)

        assertEquals("=", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `colon token should expose its data`() {
        val token = Token.ColonToken(":", start, end)

        assertEquals(":", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `semicolon token should expose its data`() {
        val token = Token.SemicolonToken(";", start, end)

        assertEquals(";", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `plus token should expose its data`() {
        val token = Token.PlusToken("+", start, end)

        assertEquals("+", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `minus token should expose its data`() {
        val token = Token.MinusToken("-", start, end)

        assertEquals("-", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `star token should expose its data`() {
        val token = Token.StarToken("*", start, end)

        assertEquals("*", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `slash token should expose its data`() {
        val token = Token.SlashToken("/", start, end)

        assertEquals("/", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `left parenthesis token should expose its data`() {
        val token = Token.LeftParenToken("(", start, end)

        assertEquals("(", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `right parenthesis token should expose its data`() {
        val token = Token.RightParenToken(")", start, end)

        assertEquals(")", token.lexeme)
        assertEquals(start, token.start)
        assertEquals(end, token.end)
    }

    @Test
    fun `tokens should have structural equality`() {
        val first = Token.IdentifierToken("foo", start, end)
        val second = Token.IdentifierToken("foo", start, end)

        assertEquals(first, second)
    }

    @Test
    fun `tokens should expose their position`() {
        val token = Token.NumberLiteralToken(
            lexeme = "42",
            value = "42",
            start = Position(3, 10),
            end = Position(3, 12)
        )

        assertEquals(Position(3, 10), token.start)
        assertEquals(Position(3, 12), token.end)
    }

    @Test
    fun `all token variants should implement Token`() {
        val tokens: List<Token> = listOf(
            Token.LetToken("let", start, end),
            Token.IdentifierToken("foo", start, end),
            Token.TypeNameToken("number", start, end),
            Token.NumberLiteralToken("42", "42", start, end),
            Token.StringLiteralToken("\"hello\"", "hello", start, end),
            Token.AssignToken("=", start, end),
            Token.ColonToken(":", start, end),
            Token.SemicolonToken(";", start, end),
            Token.PlusToken("+", start, end),
            Token.MinusToken("-", start, end),
            Token.StarToken("*", start, end),
            Token.SlashToken("/", start, end),
            Token.LeftParenToken("(", start, end),
            Token.RightParenToken(")", start, end)
        )

        assertTrue(tokens.all { it is Token })
    }

    @Test
    fun `toString should include token positions`() {
        val token = Token.IdentifierToken("foo", start, end)

        val text = token.toString()

        assertTrue(text.contains(start.toString()))
        assertTrue(text.contains(end.toString()))
    }
}