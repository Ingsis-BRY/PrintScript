package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NumberLiteralRecognizerTest {
    @Test
    fun `is pending on the empty lexeme`() {
        assertEquals(RecognizerState.Pending, NumberLiteralRecognizer.recognize(""))
    }

    @Test
    fun `accepts a run of digits`() {
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("5"))
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("0"))
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("42"))
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("1000"))
    }

    @Test
    fun `accepts a decimal with digits on both sides`() {
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("3.5"))
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("0.25"))
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("10.0"))
    }

    @Test
    fun `is pending on a trailing dot, so the lexer can fall back to the whole part`() {
        assertEquals(RecognizerState.Accepted, NumberLiteralRecognizer.recognize("5"))
        assertEquals(RecognizerState.Pending, NumberLiteralRecognizer.recognize("5."))
        assertEquals(RecognizerState.Pending, NumberLiteralRecognizer.recognize("42."))
    }

    @Test
    fun `rejects a leading dot`() {
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("."))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize(".5"))
    }

    @Test
    fun `rejects more than one dot`() {
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5.."))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5.5.5"))
    }

    @Test
    fun `rejects a sign, since the minus is always an operator`() {
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("-5"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("+5"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("-3.5"))
    }

    @Test
    fun `rejects anything that is not a digit`() {
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5a"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("a5"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5_0"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5.a"))
    }

    @Test
    fun `rejects digits from other scripts`() {
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("\u0665"))
        assertEquals(RecognizerState.Rejected, NumberLiteralRecognizer.recognize("5\u0665"))
    }

    @Test
    fun `builds a number token whose value is the lexeme itself`() {
        val token = NumberLiteralRecognizer.tokenOf("3.5", Position(2, 4), Position(2, 6))

        assertEquals(
            Token.NumberLiteralToken("3.5", "3.5", Position(2, 4), Position(2, 6)),
            token,
        )
    }

    @Test
    fun `has nothing to diagnose`() {
        assertNull(NumberLiteralRecognizer.diagnose("5."))
        assertNull(NumberLiteralRecognizer.diagnose(".5"))
    }
}
