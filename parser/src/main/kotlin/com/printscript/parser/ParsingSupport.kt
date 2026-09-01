package com.printscript.parser

import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntacticUnit
import com.printscript.report.SyntaxSymbol
import com.printscript.token.Token

internal object ParsingSupport {
    /**
     * the parser never words an error: it names the case and the source it
     * blames, and leaves the sentence to the renderer
     */
    fun unexpectedToken(token: Token): Failure =
        Failure(Diagnostic.UnexpectedToken(token.lexeme, token.span))

    fun unexpectedEndOfExpression(cursor: TokenCursor): Failure =
        Failure(
            Diagnostic.UnexpectedEndOfInput(
                unit = SyntacticUnit.EXPRESSION,
                span = cursor.endOfInput(),
            ),
        )

    fun unexpectedEndOfStatement(cursor: TokenCursor): Failure =
        Failure(
            Diagnostic.UnexpectedEndOfInput(
                unit = SyntacticUnit.STATEMENT,
                span = cursor.endOfInput(),
            ),
        )

    /**
     * consumes a token and verifies that it has the expected type
     *
     * a token that is there is blamed over its own span; past the end of the
     * input there is none, so the error lands where the tokens ran out
     */
    inline fun <reified T : Token> parseExpectedToken(
        cursor: TokenCursor,
        expected: SyntaxSymbol,
    ): Result<T> {
        val token = cursor.consume()

        return if (token is T) {
            Success(token)
        } else {
            Failure(
                Diagnostic.ExpectedSymbol(
                    expected = expected,
                    span = token?.span ?: cursor.endOfInput(),
                ),
            )
        }
    }
}
