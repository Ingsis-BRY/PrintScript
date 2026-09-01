package com.printscript.lexer

import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.report.LexicalFault
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * End to end coverage of the two open literals, including the errors they can
 * report and the positions those errors carry.
 */
class LiteralScanningTest {
    @Test
    fun `a number literal keeps the lexeme as its value`() {
        assertEquals(
            Token.NumberLiteralToken("42", "42", Position(1, 1), Position(1, 2)),
            singleTokenOf("42"),
        )

        assertEquals(
            Token.NumberLiteralToken("0.25", "0.25", Position(1, 1), Position(1, 4)),
            singleTokenOf("0.25"),
        )
    }

    @Test
    fun `a double quoted literal drops its quotes from the value`() {
        assertEquals(
            Token.StringLiteralToken("\"hola\"", "hola", Position(1, 1), Position(1, 6)),
            singleTokenOf("\"hola\""),
        )
    }

    @Test
    fun `a single quoted literal works the same way`() {
        assertEquals(
            Token.StringLiteralToken("'hola'", "hola", Position(1, 1), Position(1, 6)),
            singleTokenOf("'hola'"),
        )
    }

    @Test
    fun `an empty literal yields an empty value`() {
        assertEquals(
            Token.StringLiteralToken("\"\"", "", Position(1, 1), Position(1, 2)),
            singleTokenOf("\"\""),
        )
    }

    @Test
    fun `each quote style carries the other one literally`() {
        assertEquals("it's", (singleTokenOf("\"it's\"") as Token.StringLiteralToken).value)
        assertEquals(
            "say \"hi\"",
            (singleTokenOf("'say \"hi\"'") as Token.StringLiteralToken).value,
        )
    }

    @Test
    fun `a backslash stays in the value, since there are no escapes`() {
        assertEquals("a\\nb", (singleTokenOf("\"a\\nb\"") as Token.StringLiteralToken).value)
    }

    @Test
    fun `two literals in a row are not merged`() {
        assertEquals(
            listOf("\"a\"", "\"b\""),
            tokensOf("\"a\" \"b\"").map { it.lexeme },
        )
    }

    @Test
    fun `a literal is positioned where it starts and ends`() {
        val token = singleTokenOf("  \"hola\"")

        assertEquals(Position(1, 3), token.start)
        assertEquals(Position(1, 8), token.end)
    }

    @Test
    fun `a literal left open at the end of the source is reported at its quote`() {
        assertEquals(
            Diagnostic.MalformedLexeme(
                LexicalFault.UNTERMINATED_STRING,
                Span(Position(1, 1), Position(1, 4)),
            ),
            failureAt(resultsOf("\"abc"), index = 0),
        )
    }

    @Test
    fun `a literal left open at the end of the line is reported at its quote`() {
        assertEquals(
            Diagnostic.MalformedLexeme(
                LexicalFault.UNTERMINATED_STRING,
                Span(Position(1, 1), Position(1, 4)),
            ),
            failureAt(resultsOf("\"abc\ndef\""), index = 0),
        )
    }

    @Test
    fun `an unterminated literal keeps the column it opened on`() {
        assertEquals(
            Diagnostic.MalformedLexeme(
                LexicalFault.UNTERMINATED_STRING,
                Span(Position(1, 17), Position(1, 20)),
            ),
            failureAt(resultsOf("let x: string = \"abc"), index = 5),
        )
    }

    @Test
    fun `an unterminated literal keeps the line it opened on`() {
        assertEquals(
            Diagnostic.MalformedLexeme(
                LexicalFault.UNTERMINATED_STRING,
                Span(Position(3, 3), Position(3, 6)),
            ),
            failureAt(resultsOf("let;\n\n  'abc"), index = 2),
        )
    }

    @Test
    fun `an unterminated single quoted literal reports the same way`() {
        assertEquals(
            Diagnostic.MalformedLexeme(
                LexicalFault.UNTERMINATED_STRING,
                Span(Position(1, 1), Position(1, 4)),
            ),
            failureAt(resultsOf("'abc"), index = 0),
        )
    }

    @Test
    fun `a declaration with both literals lexes end to end`() {
        val tokens = tokensOf("let msg: string = \"hola\"; let n: number = 3.5;")

        assertEquals(
            listOf(
                "let",
                "msg",
                ":",
                "string",
                "=",
                "\"hola\"",
                ";",
                "let",
                "n",
                ":",
                "number",
                "=",
                "3.5",
                ";",
            ),
            tokens.map { it.lexeme },
        )
    }
}
