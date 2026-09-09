package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.parseColon
import com.printscript.parser.ParsingSupport.parseIdentifier
import com.printscript.parser.ParsingSupport.parseLet
import com.printscript.parser.ParsingSupport.parseSemicolon
import com.printscript.parser.ParsingSupport.parseType
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

object VariableDeclarationSyntax : StatementSyntax {
    override fun matches(token: Token): Boolean = token is Token.LetToken

    override fun parse(context: ParsingContext): Result<Statement> {
        val cursor = context.cursor

        return parseLet(cursor).flatMap { letToken ->
            parseIdentifier(cursor).flatMap { nameToken ->
                parseColon(cursor).flatMap {
                    parseType(cursor).flatMap { declaredType ->
                        parseOptionalInitializer(context).flatMap { initializer ->
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
    }

    private fun parseOptionalInitializer(context: ParsingContext): Result<Expression?> {
        if (context.cursor.peek() !is Token.AssignToken) {
            return Success(null)
        }

        context.cursor.consume()

        return context.parseExpression()
    }
}
