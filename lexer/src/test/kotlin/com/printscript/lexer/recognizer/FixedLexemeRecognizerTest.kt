package com.printscript.lexer.recognizer

import com.printscript.common.Position
import com.printscript.token.Token
import com.printscript.token.Token.LeftParenToken
import com.printscript.token.Token.LetToken
import com.printscript.token.Token.SemicolonToken
import kotlin.test.Test
import kotlin.test.assertEquals

class FixedLexemeRecognizerTest {
    @Test
    fun `accepts the exact lexeme`() {
        val recognizer = FixedLexemeRecognizer(";", ::SemicolonToken)

        assertEquals(RecognizerState.Accepted, recognizer.recognize(";"))
    }

    @Test
    fun `rejects a different character`() {
        val recognizer = FixedLexemeRecognizer(";", ::SemicolonToken)

        assertEquals(RecognizerState.Rejected, recognizer.recognize(":"))
    }

    @Test
    fun `rejects anything longer than the expected lexeme`() {
        val recognizer = FixedLexemeRecognizer(";", ::SemicolonToken)

        assertEquals(RecognizerState.Rejected, recognizer.recognize(";;"))
        assertEquals(RecognizerState.Rejected, recognizer.recognize(";a"))
    }

    @Test
    fun `is pending on a proper prefix`() {
        val recognizer = FixedLexemeRecognizer("let", ::LetToken)

        assertEquals(RecognizerState.Pending, recognizer.recognize("l"))
        assertEquals(RecognizerState.Pending, recognizer.recognize("le"))
        assertEquals(RecognizerState.Accepted, recognizer.recognize("let"))
    }

    @Test
    fun `is pending on the empty lexeme`() {
        val recognizer = FixedLexemeRecognizer(";", ::SemicolonToken)

        assertEquals(RecognizerState.Pending, recognizer.recognize(""))
    }

    @Test
    fun `rejects as soon as the lexeme stops being a prefix`() {
        val recognizer = FixedLexemeRecognizer("let", ::LetToken)

        assertEquals(RecognizerState.Rejected, recognizer.recognize("lx"))
        assertEquals(RecognizerState.Rejected, recognizer.recognize("lets"))
    }

    @Test
    fun `builds the token it was configured with, carrying both positions`() {
        val recognizer = FixedLexemeRecognizer("(", ::LeftParenToken)

        val token = recognizer.tokenOf("(", Position(2, 7), Position(2, 7))

        assertEquals(
            Token.LeftParenToken("(", Position(2, 7), Position(2, 7)),
            token,
        )
    }
}
