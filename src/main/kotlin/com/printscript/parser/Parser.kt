package com.printscript.parser

import com.printscript.ast.BinaryOperator
import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Position
import com.printscript.common.Result
import com.printscript.common.Success
import com.printscript.common.flatMap
import com.printscript.common.map
import com.printscript.language.NumberCodec
import com.printscript.token.Token

object Parser {

    //Public API
    /**
     * parses a list of tokens into a statement
     */
    fun parse(tokens: List<Token>): Result<Statement> =
        parseStatement(TokenCursor(tokens))

    /**
     * parses a list of tokens into an expression using Pratt parsing
     */
    fun parseExpression(tokens: List<Token>): Result<Expression> =
        parseExpression(TokenCursor(tokens), 0)

    //Statement Parsing
    /**
     * parses a statement based on its first token
     */
    private fun parseStatement(
        cursor: TokenCursor
    ): Result<Statement> {
        val token = cursor.peek()
            ?: return unexpectedEndOfStatement()

        return when (token) {
            is Token.LetToken ->
                parseVariableDeclaration(cursor)

            is Token.IdentifierToken ->
                if (token.lexeme == "println") {
                    parseCallStatement(cursor)
                } else {
                    parseAssignment(cursor)
                }

            else ->
                unexpectedToken(token)
        }
    }

    /**
     * parses a variable declaration with an optional initializer
     */
    private fun parseVariableDeclaration(
        cursor: TokenCursor
    ): Result<Statement> {
        val letToken = cursor.consume() as Token.LetToken

        return parseIdentifier(cursor).flatMap { nameToken ->
            parseColon(cursor).flatMap {
                parseType(cursor).flatMap { declaredType ->
                    parseOptionalInitializer(cursor).flatMap { initializer ->
                        parseSemicolon(cursor).map { semicolon ->
                            Statement.VariableDeclaration(
                                name = nameToken.lexeme,
                                declaredType = declaredType,
                                initializer = initializer,
                                start = letToken.start,
                                end = semicolon.end
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
    private fun parseAssignment(
        cursor: TokenCursor
    ): Result<Statement> =
        parseIdentifier(cursor).flatMap { nameToken ->
            parseAssign(cursor).flatMap {
                parseExpression(cursor, 0).flatMap { value ->
                    parseSemicolon(cursor).map { semicolon ->
                        Statement.Assignment(
                            name = nameToken.lexeme,
                            value = value,
                            start = nameToken.start,
                            end = semicolon.end
                        )
                    }
                }
            }
        }

    /**
     * parses a println call statement
     */
    private fun parseCallStatement(
        cursor: TokenCursor
    ): Result<Statement> =
        parsePrintln(cursor).flatMap { callee ->
            parseLeftParen(cursor).flatMap {
                parseExpression(cursor, 0).flatMap { argument ->
                    parseRightParen(cursor).flatMap {
                        parseSemicolon(cursor).map { semicolon ->
                            Statement.CallStatement(
                                callee = callee.lexeme,
                                argument = argument,
                                start = callee.start,
                                end = semicolon.end
                            )
                        }
                    }
                }
            }
        }

    // Statement Components
    private fun parseIdentifier(
        cursor: TokenCursor
    ): Result<Token.IdentifierToken> =
        parseExpectedToken(
            cursor,
            "Expected identifier"
        )

    private fun parseColon(
        cursor: TokenCursor
    ): Result<Token.ColonToken> =
        parseExpectedToken(
            cursor,
            "Expected ':'"
        )

    /**
     * parses a type name into its corresponding AST type
     */
    private fun parseType(
        cursor: TokenCursor
    ): Result<Type> =
        parseExpectedToken<Token.TypeNameToken>(
            cursor,
            "Expected type"
        ).flatMap { token ->
            when (token.lexeme) {
                "number" ->
                    Success(Type.NumberType)

                "string" ->
                    Success(Type.StringType)

                else ->
                    Failure(
                        Diagnostic(
                            message = "Unknown type: ${token.lexeme}",
                            position = token.start
                        )
                    )
            }
        }

    private fun parseAssign(
        cursor: TokenCursor
    ): Result<Token.AssignToken> =
        parseExpectedToken(
            cursor,
            "Expected '='"
        )

    private fun parseSemicolon(
        cursor: TokenCursor
    ): Result<Token.SemicolonToken> =
        parseExpectedToken(
            cursor,
            "Expected ';' at end of statement"
        )
    private fun parseLeftParen(
        cursor: TokenCursor
    ): Result<Token.LeftParenToken> =
        parseExpectedToken(
            cursor,
            "Expected '('"
        )

    private fun parseRightParen(
        cursor: TokenCursor
    ): Result<Token.RightParenToken> =
        parseExpectedToken(
            cursor,
            "Expected ')'"
        )

    private fun parsePrintln(
        cursor: TokenCursor
    ): Result<Token.IdentifierToken> =
        parseIdentifier(cursor).flatMap { token ->
            if (token.lexeme == "println") {
                Success(token)
            } else {
                Failure(
                    Diagnostic(
                        message = "Expected 'println'",
                        position = token.start
                    )
                )
            }
        }

    private fun parseOptionalInitializer(
        cursor: TokenCursor
    ): Result<Expression?> {
        if (cursor.peek() !is Token.AssignToken) {
            return Success(null)
        }

        cursor.consume()

        return parseExpression(cursor, 0)
    }

    /**
     * parses an expression while respecting the minimum operator precedence
     */
    private fun parseExpression(
        cursor: TokenCursor,
        minPrecedence: Int
    ): Result<Expression> =
        parsePrimary(cursor).flatMap { left ->
            parseBinaryExpression(cursor, left, minPrecedence)
        }

    /**
     * builds binary expressions according to operator precedence
     */
    private fun parseBinaryExpression(
        cursor: TokenCursor,
        left: Expression,
        minPrecedence: Int
    ): Result<Expression> {
        var currentLeft = left

        while (true) {
            val token = cursor.peek() ?: break
            val precedence = PrecedenceTable.precedenceOf(token)

            if (precedence <= minPrecedence) {
                break
            }

            cursor.consume()

            val operator = binaryOperatorOf(token)

            when (val right = parseExpression(cursor, precedence)) {
                is Failure -> return right

                is Success -> {
                    currentLeft = createBinaryExpression(
                        left = currentLeft,
                        operator = operator,
                        right = right.value
                    )
                }
            }
        }

        return Success(currentLeft)
    }

    /**
     * parses the atomic expressions that can appear before a binary operator
     */
    private fun parsePrimary(
        cursor: TokenCursor
    ): Result<Expression> {
        val token = cursor.consume()
            ?: return unexpectedEndOfExpression()

        return parsePrimaryToken(cursor, token)
    }

    /**
     * parses a token into the corresponding primary expression
     */
    private fun parsePrimaryToken(
        cursor: TokenCursor,
        token: Token
    ): Result<Expression> =
        when (token) {
            is Token.NumberLiteralToken ->
                parseNumber(token)

            is Token.StringLiteralToken ->
                Success(
                    Expression.StringLiteral(
                        value = token.value,
                        start = token.start,
                        end = token.end
                    )
                )

            is Token.IdentifierToken ->
                Success(
                    Expression.VariableReference(
                        name = token.lexeme,
                        start = token.start,
                        end = token.end
                    )
                )

            is Token.LeftParenToken ->
                parseParenthesizedExpression(cursor, token)

            else ->
                unexpectedToken(token)
        }

    /**
     * parses a numeric literal using [NumberCodec], preserving its source position
     */
    private fun parseNumber(
        token: Token.NumberLiteralToken
    ): Result<Expression> =
        NumberCodec.parse(
            text = token.value,
            start = token.start,
            end = token.end
        ).map { value ->
            Expression.NumberLiteral(
                value = value,
                start = token.start,
                end = token.end
            )
        }

    /**
     * parses the expression inside parentheses and extends its position to include them
     */
    private fun parseParenthesizedExpression(
        cursor: TokenCursor,
        openingParen: Token.LeftParenToken
    ): Result<Expression> =
        parseExpression(cursor, 0).flatMap { expression ->
            val closingParen = cursor.consume()

            if (closingParen is Token.RightParenToken) {
                Success(
                    withPosition(
                        expression,
                        openingParen.start,
                        closingParen.end
                    )
                )
            } else {
                Failure(
                    Diagnostic(
                        message = "Expected closing parenthesis",
                        position = closingParen?.start ?: openingParen.end
                    )
                )
            }
        }

    /**
     * creates a binary expression from its left operand, operator, and right operand
     */
    private fun createBinaryExpression(
        left: Expression,
        operator: BinaryOperator,
        right: Expression
    ): Expression =
        Expression.BinaryExpression(
            left = left,
            operator = operator,
            right = right,
            start = left.start,
            end = right.end
        )

    /**
     * maps a binary operator token to its corresponding AST operator
     * assumes [token] has already been identified as a binary operator
     */
    private fun binaryOperatorOf(token: Token): BinaryOperator =
        when (token) {
            is Token.PlusToken -> BinaryOperator.Addition
            is Token.MinusToken -> BinaryOperator.Subtraction
            is Token.StarToken -> BinaryOperator.Multiplication
            is Token.SlashToken -> BinaryOperator.Division
            else -> error("Token is not a binary operator")
        }

    /**
     * updates an expression's source range without changing its structure
     */
    private fun withPosition(
        expression: Expression,
        start: Position,
        end: Position
    ): Expression =
        when (expression) {
            is Expression.NumberLiteral ->
                expression.copy(start = start, end = end)

            is Expression.StringLiteral ->
                expression.copy(start = start, end = end)

            is Expression.VariableReference ->
                expression.copy(start = start, end = end)

            is Expression.BinaryExpression ->
                expression.copy(start = start, end = end)
        }

    // Diagnostics
    /**
     * creates a diagnostic for an unexpected token
     */
    private fun unexpectedToken(token: Token): Failure =
        Failure(
            Diagnostic(
                message = "Unexpected token: ${token.lexeme}",
                position = token.start
            )
        )

    private fun unexpectedEndOfExpression(): Failure =
        Failure(
            Diagnostic(
                message = "Unexpected end of expression",
                position = Position(0, 0)
            )
        )

    private fun unexpectedEndOfStatement(): Failure =
        Failure(
            Diagnostic(
                message = "Unexpected end of statement",
                position = Position(0, 0)
            )
        )

    /**
     * consumes a token and verifies that it has the expected type
     */
    private inline fun <reified T : Token> parseExpectedToken(
        cursor: TokenCursor,
        message: String
    ): Result<T> {
        val token = cursor.consume()

        return if (token is T) {
            Success(token)
        } else {
            Failure(
                Diagnostic(
                    message = message,
                    position = token?.start ?: Position(0, 0)
                )
            )
        }
    }
}