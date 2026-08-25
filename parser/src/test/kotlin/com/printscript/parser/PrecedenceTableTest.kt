package com.printscript.parser

import com.printscript.common.Position
import com.printscript.token.Token
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals

class PrecedenceTableTest {
    private val start = Position(1, 1)
    private val end = Position(1, 2)

    @Test
    fun `plus should have additive precedence`() {
        val token = Token.PlusToken("+", start, end)

        assertEquals(1, PrecedenceTable.precedenceOf(token))
    }

    @Test
    fun `minus should have additive precedence`() {
        val token = Token.MinusToken("-", start, end)

        assertEquals(1, PrecedenceTable.precedenceOf(token))
    }

    @Test
    fun `star should have multiplicative precedence`() {
        val token = Token.StarToken("*", start, end)

        assertEquals(2, PrecedenceTable.precedenceOf(token))
    }

    @Test
    fun `slash should have multiplicative precedence`() {
        val token = Token.SlashToken("/", start, end)

        assertEquals(2, PrecedenceTable.precedenceOf(token))
    }

    @Test
    fun `multiplicative operators should have higher precedence than additive operators`() {
        val plus = Token.PlusToken("+", start, end)
        val star = Token.StarToken("*", start, end)

        assertTrue(
            PrecedenceTable.precedenceOf(star) >
                PrecedenceTable.precedenceOf(plus),
        )
    }

    @Test
    fun `non operator token should have minimum precedence`() {
        val token = Token.IdentifierToken("foo", start, end)

        assertEquals(0, PrecedenceTable.precedenceOf(token))
    }
}
