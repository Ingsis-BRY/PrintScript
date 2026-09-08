package com.printscript.parser.syntax

import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.parseAssign
import com.printscript.parser.ParsingSupport.parseIdentifier
import com.printscript.parser.ParsingSupport.parseSemicolon
import com.printscript.report.Result
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

object AssignmentSyntax : StatementSyntax {
    override fun matches(token: Token): Boolean = token is Token.IdentifierToken

    override fun parse(context: ParsingContext): Result<Statement> {
        val cursor = context.cursor

        return parseIdentifier(cursor).flatMap { nameToken ->
            parseAssign(cursor).flatMap {
                context.parseExpression().flatMap { value ->
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
    }
}
