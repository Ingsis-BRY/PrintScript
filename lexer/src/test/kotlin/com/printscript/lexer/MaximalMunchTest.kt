package com.printscript.lexer

import com.printscript.common.Position
import com.printscript.common.Span
import com.printscript.report.Diagnostic
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Covers the two rules that arbitrate between recognizers.
 *
 * Length decides first; registration order only breaks a tie between lexemes
 * of the very same length.
 */
class MaximalMunchTest {
    @Test
    fun `an equal length tie goes to the recognizer registered first`() {
        assertEquals(
            Token.LetToken("let", Position(1, 1), Position(1, 3)),
            singleTokenOf("let"),
        )

        assertEquals(
            Token.TypeNameToken("number", Position(1, 1), Position(1, 6)),
            singleTokenOf("number"),
        )

        assertEquals(
            Token.TypeNameToken("string", Position(1, 1), Position(1, 6)),
            singleTokenOf("string"),
        )
    }

    @Test
    fun `a longer match beats the higher priority recognizer`() {
        assertEquals(
            Token.IdentifierToken("lets", Position(1, 1), Position(1, 4)),
            singleTokenOf("lets"),
        )

        assertEquals(
            Token.IdentifierToken("numbers", Position(1, 1), Position(1, 7)),
            singleTokenOf("numbers"),
        )

        assertEquals(
            Token.IdentifierToken("stringify", Position(1, 1), Position(1, 9)),
            singleTokenOf("stringify"),
        )
    }

    @Test
    fun `a reserved word is only reserved when it stands alone`() {
        assertIs<Token.IdentifierToken>(singleTokenOf("letter"))
        assertIs<Token.IdentifierToken>(singleTokenOf("_let"))
        assertIs<Token.IdentifierToken>(singleTokenOf("let_"))
        assertIs<Token.IdentifierToken>(singleTokenOf("let1"))
        assertIs<Token.IdentifierToken>(singleTokenOf("myLet"))
    }

    @Test
    fun `a reserved word ends where the identifier alphabet ends`() {
        val tokens = tokensOf("let;")

        assertIs<Token.LetToken>(tokens[0])
        assertIs<Token.SemicolonToken>(tokens[1])
    }

    @Test
    fun `println is an ordinary identifier`() {
        assertEquals(
            Token.IdentifierToken("println", Position(1, 1), Position(1, 7)),
            singleTokenOf("println"),
        )
    }

    @Test
    fun `an identifier stops at the first character outside its alphabet`() {
        assertEquals(
            listOf("counter", "-", "x"),
            tokensOf("counter-x").map { it.lexeme },
        )
    }

    @Test
    fun `the scan resumes right after the winning lexeme`() {
        val tokens = tokensOf("lets(let)")

        assertEquals(listOf("lets", "(", "let", ")"), tokens.map { it.lexeme })
        assertIs<Token.IdentifierToken>(tokens[0])
        assertIs<Token.LetToken>(tokens[2])
    }

    @Test
    fun `a full declaration lexes into the expected token stream`() {
        val tokens = tokensOf("let total: number = subtotal;")

        assertEquals(
            listOf("let", "total", ":", "number", "=", "subtotal", ";"),
            tokens.map { it.lexeme },
        )

        assertIs<Token.LetToken>(tokens[0])
        assertIs<Token.IdentifierToken>(tokens[1])
        assertIs<Token.ColonToken>(tokens[2])
        assertIs<Token.TypeNameToken>(tokens[3])
        assertIs<Token.AssignToken>(tokens[4])
        assertIs<Token.IdentifierToken>(tokens[5])
        assertIs<Token.SemicolonToken>(tokens[6])
    }

    @Test
    fun `a trailing dot is consumed, then given back when no digit follows`() {
        val results = resultsOf("5.;")

        // the scan read '5', accepted it, read '.' and stayed pending, then died
        // on ';' and fell back to the last accepted length
        assertEquals(
            Token.NumberLiteralToken("5", "5", Position(1, 1), Position(1, 1)),
            tokenAt(results, index = 0),
        )

        // the pushed-back dot is scanned again and starts nothing
        assertEquals(
            Diagnostic.UnexpectedCharacter('.', Span.at(Position(1, 2))),
            failureAt(results, index = 1),
        )
    }

    @Test
    fun `a trailing dot at the end of the source backs off the same way`() {
        val results = resultsOf("5.")

        assertEquals(
            Token.NumberLiteralToken("5", "5", Position(1, 1), Position(1, 1)),
            tokenAt(results, index = 0),
        )

        assertEquals(
            Diagnostic.UnexpectedCharacter('.', Span.at(Position(1, 2))),
            failureAt(results, index = 1),
        )
    }

    @Test
    fun `a complete decimal is not backed off`() {
        assertEquals(
            Token.NumberLiteralToken("3.5", "3.5", Position(1, 1), Position(1, 3)),
            singleTokenOf("3.5"),
        )
    }

    @Test
    fun `a second dot backs the number off to the first decimal`() {
        val results = resultsOf("5.5.5")

        assertEquals(
            Token.NumberLiteralToken("5.5", "5.5", Position(1, 1), Position(1, 3)),
            tokenAt(results, index = 0),
        )

        assertEquals(
            Diagnostic.UnexpectedCharacter('.', Span.at(Position(1, 4))),
            failureAt(results, index = 1),
        )
    }

    @Test
    fun `a number ends where the digits end`() {
        assertEquals(
            listOf("42", ";"),
            tokensOf("42;").map { it.lexeme },
        )

        assertEquals(
            listOf("(", "3.5", "+", "2", ")"),
            tokensOf("(3.5+2)").map { it.lexeme },
        )
    }

    @Test
    fun `a string swallows characters that would otherwise be tokens`() {
        assertEquals(
            Token.StringLiteralToken(
                "\"a + b; let\"",
                "a + b; let",
                Position(1, 1),
                Position(1, 12),
            ),
            singleTokenOf("\"a + b; let\""),
        )
    }
}
