package com.printscript.parser

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.parser.ParsingSupport.parseExpectedToken
import com.printscript.parser.ParsingSupport.unexpectedEndOfStatement
import com.printscript.parser.ParsingSupport.unexpectedToken
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

class StatementParser(
    private val cursor: TokenCursor,
) {
    private val expressionParser = ExpressionParser(cursor)

    fun parse(): Result<Statement> = parseStatement(cursor)

    /**
     * parses a statement based on its first token
     */
    private fun parseStatement(cursor: TokenCursor): Result<Statement> {
        val token =
            cursor.peek()
                ?: return unexpectedEndOfStatement(cursor)

        return when (token) {
            is Token.LetToken ->
                parseVariableDeclaration(cursor)

            is Token.IdentifierToken ->
                if (token.lexeme == "println") {
                    parseCallStatement(cursor)
                } else {
                    parseAssignment(cursor)
                }

            else -> unexpectedToken(token)
        }
    }

    /**
     * parses a variable declaration with an optional initializer
     */
    private fun parseVariableDeclaration(cursor: TokenCursor): Result<Statement> =
        parseExpectedToken<Token.LetToken>(
            cursor,
            SyntaxSymbol.LET,
        ).flatMap { letToken ->
            parseIdentifier(cursor).flatMap { nameToken ->
                parseColon(cursor).flatMap {
                    parseType(cursor).flatMap { declaredType ->
                        parseOptionalInitializer(cursor).flatMap { initializer ->
                            parseSemicolon(cursor).map { semicolon ->
                                Statement.VariableDeclaration(
                                    name = nameToken.lexeme,
                                    declaredType = declaredType,
                                    initializer = initializer,
                                    start = letToken.start,
                                    end = semicolon.end,
                                )
                            }
                        }
                    }
                }
            }
        }

    /**
     * parses an assignment to a variable
     */
    private fun parseAssignment(cursor: TokenCursor): Result<Statement> =
        parseIdentifier(cursor).flatMap { nameToken ->
            parseAssign(cursor).flatMap {
                expressionParser.parse().flatMap { value ->
                    parseSemicolon(cursor).map { semicolon ->
                        Statement.Assignment(
                            name = nameToken.lexeme,
                            value = value,
                            start = nameToken.start,
                            end = semicolon.end,
                        )
                    }
                }
            }
        }

    /**
     * parses a println call statement
     */
    private fun parseCallStatement(cursor: TokenCursor): Result<Statement> =
        parsePrintln(cursor).flatMap { callee ->
            parseLeftParen(cursor).flatMap {
                expressionParser.parse().flatMap { argument ->
                    parseRightParen(cursor).flatMap {
                        parseSemicolon(cursor).map { semicolon ->
                            Statement.CallStatement(
                                callee = callee.lexeme,
                                argument = argument,
                                start = callee.start,
                                end = semicolon.end,
                            )
                        }
                    }
                }
            }
        }

    // Statement Components
    private fun parseIdentifier(cursor: TokenCursor): Result<Token.IdentifierToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.IDENTIFIER,
        )

    private fun parseColon(cursor: TokenCursor): Result<Token.ColonToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.COLON,
        )

    /**
     * parses a type name into its corresponding AST type
     */
    private fun parseType(cursor: TokenCursor): Result<Type> =
        parseExpectedToken<Token.TypeNameToken>(
            cursor,
            SyntaxSymbol.TYPE_NAME,
        ).flatMap { token ->
            when (token.lexeme) {
                "number" ->
                    Success(Type.NumberType)

                "string" ->
                    Success(Type.StringType)

                else ->
                    Failure(Diagnostic.UnknownType(token.lexeme, token.span))
            }
        }

    private fun parseAssign(cursor: TokenCursor): Result<Token.AssignToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.ASSIGN,
        )

    private fun parseSemicolon(cursor: TokenCursor): Result<Token.SemicolonToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.SEMICOLON,
        )

    private fun parseLeftParen(cursor: TokenCursor): Result<Token.LeftParenToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.LEFT_PAREN,
        )

    private fun parseRightParen(cursor: TokenCursor): Result<Token.RightParenToken> =
        parseExpectedToken(
            cursor,
            SyntaxSymbol.RIGHT_PAREN,
        )

    private fun parsePrintln(cursor: TokenCursor): Result<Token.IdentifierToken> =
        parseIdentifier(cursor).flatMap { token ->
            if (token.lexeme == "println") {
                Success(token)
            } else {
                Failure(Diagnostic.ExpectedSymbol(SyntaxSymbol.PRINTLN, token.span))
            }
        }

    private fun parseOptionalInitializer(cursor: TokenCursor): Result<Expression?> {
        if (cursor.peek() !is Token.AssignToken) {
            return Success(null)
        }

        cursor.consume()

        return expressionParser.parse()
    }
}
