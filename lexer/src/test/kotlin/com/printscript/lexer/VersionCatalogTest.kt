package com.printscript.lexer

import com.printscript.lexer.recognizer.TokenRecognizer
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VersionCatalogTest {
    private fun tokensOf(
        source: String,
        recognizers: List<TokenRecognizer>,
    ): List<Token> {
        val results: List<Result<Token>> =
            Lexer(StringSourceReader(source), recognizers).tokens().toList()

        return results.map { assertIs<Success<Token>>(it).value }
    }

    private fun tokens11(source: String) = tokensOf(source, TokenRecognizers.V1_1)

    private fun tokens10(source: String) = tokensOf(source, TokenRecognizers.V1_0)

    @Test
    fun `const is a keyword in 1 point 1`() {
        assertIs<Token.ConstToken>(tokens11("const").single())
    }

    @Test
    fun `if and else are keywords in 1 point 1`() {
        assertIs<Token.IfToken>(tokens11("if").single())
        assertIs<Token.ElseToken>(tokens11("else").single())
    }

    @Test
    fun `boolean is a type name in 1 point 1`() {
        assertEquals("boolean", assertIs<Token.TypeNameToken>(tokens11("boolean").single()).lexeme)
    }

    @Test
    fun `the two boolean literals carry their value`() {
        assertTrue(assertIs<Token.BooleanLiteralToken>(tokens11("true").single()).value)
        assertTrue(!assertIs<Token.BooleanLiteralToken>(tokens11("false").single()).value)
    }

    @Test
    fun `braces are tokens in 1 point 1`() {
        val tokens = tokens11("{}")

        assertIs<Token.LeftBraceToken>(tokens.first())
        assertIs<Token.RightBraceToken>(tokens.last())
    }

    @Test
    fun `const is an ordinary identifier in 1 point 0`() {
        assertIs<Token.IdentifierToken>(tokens10("const").single())
    }

    @Test
    fun `if and else are ordinary identifiers in 1 point 0`() {
        assertIs<Token.IdentifierToken>(tokens10("if").single())
        assertIs<Token.IdentifierToken>(tokens10("else").single())
    }

    @Test
    fun `boolean is an ordinary identifier in 1 point 0`() {
        assertIs<Token.IdentifierToken>(tokens10("boolean").single())
    }

    @Test
    fun `true and false are ordinary identifiers in 1 point 0`() {
        assertIs<Token.IdentifierToken>(tokens10("true").single())
        assertIs<Token.IdentifierToken>(tokens10("false").single())
    }

    @Test
    fun `a brace is not a character any 1 point 0 token can begin with`() {
        val results = Lexer(StringSourceReader("{"), TokenRecognizers.V1_0).tokens().toList()

        assertIs<com.printscript.report.Failure>(results.single())
    }

    @Test
    fun `a longer identifier beats a keyword it starts with`() {
        assertIs<Token.IdentifierToken>(tokens11("constant").single())
        assertIs<Token.IdentifierToken>(tokens11("iffy").single())
        assertIs<Token.IdentifierToken>(tokens11("elsewhere").single())
        assertIs<Token.IdentifierToken>(tokens11("truthy").single())
    }

    @Test
    fun `1 point 1 still reads everything 1 point 0 does`() {
        val source = "let x: number = 5 + 4 * 3 / 2 - 1;"

        assertEquals(
            tokens10(source).map { it::class to it.lexeme },
            tokens11(source).map { it::class to it.lexeme },
        )
    }

    @Test
    fun `a whole conditional lexes into the tokens it is made of`() {
        val tokens = tokens11("if (a) { println(1); } else { println(2); }")

        assertIs<Token.IfToken>(tokens.first())
        assertEquals(1, tokens.count { it is Token.ElseToken })
        assertEquals(2, tokens.count { it is Token.LeftBraceToken })
        assertEquals(2, tokens.count { it is Token.RightBraceToken })
    }
}
