package com.printscript.parser.syntax

import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.parseIdentifier
import com.printscript.parser.ParsingSupport.parseLeftParen
import com.printscript.parser.ParsingSupport.parseRightParen
import com.printscript.parser.ParsingSupport.parseSemicolon
import com.printscript.report.Result
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

class CallSyntax(
    private val name: String,
) : StatementSyntax {
    override fun matches(token: Token): Boolean =
        token is Token.IdentifierToken && token.lexeme == name

    override fun parse(context: ParsingContext): Result<Statement> {
        val cursor = context.cursor

        return parseIdentifier(cursor).flatMap { callee ->
            parseLeftParen(cursor).flatMap {
                context.parseExpression().flatMap { argument ->
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
    }
}
