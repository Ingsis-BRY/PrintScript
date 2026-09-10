package com.printscript.parser

import com.printscript.ast.Type
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntacticUnit
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
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

    fun parseKeyword(
        cursor: TokenCursor,
        expected: SyntaxSymbol,
        opens: (Token) -> Boolean,
    ): Result<Token> {
        val token = cursor.consume()

        return if (token != null && opens(token)) {
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

    fun parseIdentifier(cursor: TokenCursor): Result<Token.IdentifierToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.IDENTIFIER,
        )

    fun parseColon(cursor: TokenCursor): Result<Token.ColonToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.COLON,
        )

    /**
     * parses a type name into its corresponding AST type
     */
    fun parseType(cursor: TokenCursor): Result<Type> =
        parseExpectedToken<Token.TypeNameToken>(
            cursor,
            SyntaxSymbol.TYPE_NAME,
        ).flatMap { token ->
            when (token.lexeme) {
                "number" ->
                    Success(Type.NumberType)

                "string" ->
                    Success(Type.StringType)

                "boolean" ->
                    Success(Type.BooleanType)

                else ->
                    Failure(Diagnostic.UnknownType(token.lexeme, token.span))
            }
        }

    fun parseAssign(cursor: TokenCursor): Result<Token.AssignToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.ASSIGN,
        )

    fun parseSemicolon(cursor: TokenCursor): Result<Token.SemicolonToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.SEMICOLON,
        )

    fun parseLeftParen(cursor: TokenCursor): Result<Token.LeftParenToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.LEFT_PAREN,
        )

    fun parseRightParen(cursor: TokenCursor): Result<Token.RightParenToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.RIGHT_PAREN,
        )

    fun parseLeftBrace(cursor: TokenCursor): Result<Token.LeftBraceToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.LEFT_BRACE,
        )

    fun parseRightBrace(cursor: TokenCursor): Result<Token.RightBraceToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.RIGHT_BRACE,
        )
}
