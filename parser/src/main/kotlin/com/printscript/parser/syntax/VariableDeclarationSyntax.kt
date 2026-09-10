package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.parseColon
import com.printscript.parser.ParsingSupport.parseIdentifier
import com.printscript.parser.ParsingSupport.parseKeyword
import com.printscript.parser.ParsingSupport.parseSemicolon
import com.printscript.parser.ParsingSupport.parseType
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

class VariableDeclarationSyntax(
    private val keyword: SyntaxSymbol,
    private val mutable: Boolean,
    private val opens: (Token) -> Boolean,
) : StatementSyntax {
    override fun matches(token: Token): Boolean = opens(token)

    override fun parse(context: ParsingContext): Result<Statement> {
        val cursor = context.cursor

        return parseKeyword(cursor, keyword, opens).flatMap { keywordToken ->
            parseIdentifier(cursor).flatMap { nameToken ->
                parseColon(cursor).flatMap {
                    parseType(cursor).flatMap { declaredType ->
                        parseOptionalInitializer(context).flatMap { initializer ->
                            parseSemicolon(cursor).map { semicolon ->
                                Statement.VariableDeclaration(
                                    name = nameToken.lexeme,
                                    declaredType = declaredType,
                                    initializer = initializer,
                                    mutable = mutable,
                                    start = keywordToken.start,
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
