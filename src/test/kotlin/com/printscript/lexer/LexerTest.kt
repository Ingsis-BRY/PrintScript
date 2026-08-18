package com.printscript.lexer

import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success
import com.printscript.token.Token
import java.io.IOException
import java.io.Reader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class LexerTest {

    @Test
    fun `empty source yields no tokens`() {
        assertEquals(emptyList(), tokensOf(""))
    }

    @Test
    fun `source with only whitespace yields no tokens`() {
        assertEquals(emptyList(), tokensOf("  \t\n  \r\n "))
    }

    @Test
    fun `every fixed literal is recognized on its own`() {
        val expected = mapOf<String, (Position, Position) -> Token>(
            ":" to { start, end -> Token.ColonToken(":", start, end) },
            "=" to { start, end -> Token.AssignToken("=", start, end) },
            ";" to { start, end -> Token.SemicolonToken(";", start, end) },
            "+" to { start, end -> Token.PlusToken("+", start, end) },
            "-" to { start, end -> Token.MinusToken("-", start, end) },
            "*" to { start, end -> Token.StarToken("*", start, end) },
            "/" to { start, end -> Token.SlashToken("/", start, end) },
            "(" to { start, end -> Token.LeftParenToken("(", start, end) },
            ")" to { start, end -> Token.RightParenToken(")", start, end) }
        )

        for ((lexeme, build) in expected) {
            assertEquals(
                listOf(build(Position(1, 1), Position(1, 1))),
                tokensOf(lexeme),
                "failed on \"$lexeme\""
            )
        }
    }

    @Test
    fun `adjacent literals are split without whitespace`() {
        val tokens = tokensOf(":=;+-*/()")

        assertEquals(9, tokens.size)
        assertEquals("[:, =, ;, +, -, *, /, (, )]", tokens.map { it.lexeme }.toString())
    }

    @Test
    fun `a single character token starts and ends at the same position`() {
        val tokens = tokensOf("  ;")

        assertEquals(Position(1, 3), tokens.single().start)
        assertEquals(Position(1, 3), tokens.single().end)
    }

    @Test
    fun `whitespace between literals is skipped without shifting positions`() {
        val tokens = tokensOf("( \t )")

        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(1, 5), tokens[1].start)
    }

    @Test
    fun `a line feed starts a new line and resets the column`() {
        val tokens = tokensOf("(\n)")

        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(2, 1), tokens[1].start)
    }

    @Test
    fun `carriage returns do not open a line of their own`() {
        val tokens = tokensOf("(\r\n)")

        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(2, 1), tokens[1].start)
    }

    @Test
    fun `positions keep advancing across several lines`() {
        val tokens = tokensOf("+\n  -\n\n   *")

        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(2, 3), tokens[1].start)
        assertEquals(Position(4, 4), tokens[2].start)
    }

    @Test
    fun `a character no recognizer matches is reported at its position`() {
        val results = resultsOf("( @ )")

        assertEquals(
            Diagnostic("Unexpected character '@'", Position(1, 3)),
            failureAt(results, index = 1)
        )
    }

    @Test
    fun `an unexpected character is reported on the line it appears on`() {
        val results = resultsOf("(\n  )\n  #")

        assertEquals(
            Diagnostic("Unexpected character '#'", Position(3, 3)),
            failureAt(results, index = 2)
        )
    }

    @Test
    fun `scanning stops at the first error`() {
        val results = resultsOf("( @ ) ; ;")

        assertEquals(2, results.size)
        assertTrue(results[0] is Success)
        assertTrue(results[1] is Failure)
    }

    @Test
    fun `a multi character token ends on its last character`() {
        val token = singleTokenOf("  counter")

        assertEquals(Position(1, 3), token.start)
        assertEquals(Position(1, 9), token.end)
    }

    @Test
    fun `an identifier spanning to the end of a line keeps its line`() {
        val tokens = tokensOf("alpha\n  beta")

        assertEquals(Position(1, 1), tokens[0].start)
        assertEquals(Position(1, 5), tokens[0].end)
        assertEquals(Position(2, 3), tokens[1].start)
        assertEquals(Position(2, 6), tokens[1].end)
    }

    @Test
    fun `an unexpected character after an identifier is reported at its own column`() {
        val results = resultsOf("counter @")

        assertEquals(
            Diagnostic("Unexpected character '@'", Position(1, 9)),
            failureAt(results, index = 1)
        )
    }

    @Test
    fun `a digit opens a number, so no identifier can begin with one`() {
        val tokens = tokensOf("2fast")

        assertEquals(listOf("2", "fast"), tokens.map { it.lexeme })
        assertIs<Token.NumberLiteralToken>(tokens[0])
        assertIs<Token.IdentifierToken>(tokens[1])
    }

    @Test
    fun `tokens are produced lazily`() {
        val reader = StringSourceReader(";".repeat(1_000))

        val firstTwo = Lexer(reader).tokens().take(2).toList()

        assertEquals(2, firstTwo.size)
        assertTrue(reader.hasNext(), "the whole source was consumed before it was needed")
    }

    @Test
    fun `a source that breaks mid stream reports the failure instead of throwing`() {
        val results = resultsFrom(BreakingReader("x;"))

        assertEquals("x", tokenAt(results, index = 0).lexeme)
        assertEquals(";", tokenAt(results, index = 1).lexeme)

        assertEquals(
            Diagnostic("Could not read source: disk went away", Position(1, 3)),
            failureAt(results, index = 2)
        )
    }

    @Test
    fun `a source that breaks on the first read fails without yielding tokens`() {
        val results = resultsFrom(BreakingReader(""))

        assertEquals(
            Diagnostic("Could not read source: disk went away", Position(1, 1)),
            failureAt(results, index = 0)
        )
        assertEquals(1, results.size)
    }

    @Test
    fun `the failure is reported once and the stream ends there`() {
        val results = resultsFrom(BreakingReader("let"))

        assertEquals(2, results.size)
        assertEquals("let", tokenAt(results, index = 0).lexeme)
        assertTrue(results[1] is Failure)
    }

    private fun resultsFrom(reader: Reader): List<Result<Token>> =
        Lexer(StreamSourceReader(reader)).tokens().toList()

    /**
     * yields the whole source and then throws instead of signalling a clean end
     */
    private class BreakingReader(
        private val source: String
    ) : Reader() {

        private var index: Int = 0

        override fun read(buffer: CharArray, offset: Int, length: Int): Int {
            if (length == 0) {
                return 0
            }

            if (index >= source.length) {
                throw IOException("disk went away")
            }

            buffer[offset] = source[index++]

            return 1
        }

        override fun close() {}
    }
}
