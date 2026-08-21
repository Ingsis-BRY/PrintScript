package com.printscript.lexer.recognizer

import com.printscript.report.LexicalFault
import com.printscript.common.Position
import com.printscript.token.Token
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StringLiteralRecognizerTest {

    @Test
    fun `is pending on the empty lexeme`() {
        assertEquals(RecognizerState.Pending, StringLiteralRecognizer.recognize(""))
    }

    @Test
    fun `is pending while the literal is still open`() {
        assertEquals(RecognizerState.Pending, StringLiteralRecognizer.recognize("\""))
        assertEquals(RecognizerState.Pending, StringLiteralRecognizer.recognize("\"ho"))
        assertEquals(RecognizerState.Pending, StringLiteralRecognizer.recognize("'ho"))
    }

    @Test
    fun `accepts a literal closed by its own quote`() {
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("\"hola\""))
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("'hola'"))
    }

    @Test
    fun `accepts an empty literal`() {
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("\"\""))
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("''"))
    }

    @Test
    fun `accepts the other quote inside the body`() {
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("\"it's\""))
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("'say \"hi\"'"))
    }

    @Test
    fun `treats a backslash as an ordinary character, since there are no escapes`() {
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("\"a\\nb\""))
        assertEquals(RecognizerState.Accepted, StringLiteralRecognizer.recognize("\"a\\\""))
    }

    @Test
    fun `rejects growing past the closing quote`() {
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("\"ab\"x"))
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("\"ab\"\""))
    }

    @Test
    fun `rejects a line break inside an open literal`() {
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("\"ab\n"))
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("\"ab\r"))
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("\"ab\ncd\""))
    }

    @Test
    fun `rejects a lexeme that does not open with a quote`() {
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("hola"))
        assertEquals(RecognizerState.Rejected, StringLiteralRecognizer.recognize("5"))
    }

    @Test
    fun `diagnoses anything that opened a quote`() {
        assertEquals(LexicalFault.UNTERMINATED_STRING, StringLiteralRecognizer.diagnose("\"ab"))
        assertEquals(LexicalFault.UNTERMINATED_STRING, StringLiteralRecognizer.diagnose("\"ab\n"))
        assertEquals(LexicalFault.UNTERMINATED_STRING, StringLiteralRecognizer.diagnose("'ab"))
    }

    @Test
    fun `stays quiet about lexemes that never opened a quote`() {
        assertNull(StringLiteralRecognizer.diagnose("hola"))
        assertNull(StringLiteralRecognizer.diagnose("@"))
        assertNull(StringLiteralRecognizer.diagnose(""))
    }

    @Test
    fun `builds a string token whose value drops the quotes`() {
        val token = StringLiteralRecognizer.tokenOf("\"hola\"", Position(1, 5), Position(1, 10))

        assertEquals(
            Token.StringLiteralToken("\"hola\"", "hola", Position(1, 5), Position(1, 10)),
            token
        )
    }

    @Test
    fun `builds an empty valued token for an empty literal`() {
        val token = StringLiteralRecognizer.tokenOf("''", Position(1, 1), Position(1, 2))

        assertEquals(
            Token.StringLiteralToken("''", "", Position(1, 1), Position(1, 2)),
            token
        )
    }
}
