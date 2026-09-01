package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals

class IdentifierRecognizerTest {
    @Test
    fun `is pending on the empty lexeme`() {
        assertEquals(RecognizerState.Pending, IdentifierRecognizer.recognize(""))
    }

    @Test
    fun `accepts a single letter`() {
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("x"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("X"))
    }

    @Test
    fun `accepts letters, digits and underscores after the first character`() {
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("counter"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("a1"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("mi_var"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("valor2_b3"))
    }

    @Test
    fun `accepts an underscore as the first character`() {
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("_priv"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("_"))
        assertEquals(RecognizerState.Accepted, IdentifierRecognizer.recognize("__"))
    }

    @Test
    fun `rejects a leading digit`() {
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("2"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("2fast"))
    }

    @Test
    fun `rejects a character outside the alphabet anywhere in the lexeme`() {
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("$"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("a\$b"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("a-b"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("a b"))
    }

    @Test
    fun `rejects non ascii letters`() {
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("ñ"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("año"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("café"))
    }

    @Test
    fun `stays rejected once a bad character appeared, no matter how long the lexeme grows`() {
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("2f"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("2fas"))
        assertEquals(RecognizerState.Rejected, IdentifierRecognizer.recognize("2fastest"))
    }

    @Test
    fun `builds an identifier token carrying both positions`() {
        val token = IdentifierRecognizer.tokenOf("counter", Position(3, 5), Position(3, 11))

        assertEquals(
            Token.IdentifierToken("counter", Position(3, 5), Position(3, 11)),
            token,
        )
    }
}
