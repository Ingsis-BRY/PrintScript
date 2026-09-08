package com.printscript.parser.syntax

import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.parseIdentifier
import com.printscript.parser.ParsingSupport.parseLeftParen
import com.printscript.parser.ParsingSupport.parseRightParen
import com.printscript.parser.ParsingSupport.parseSemicolon
import com.printscript.parser.TokenCursor
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

class CallSyntax(
    private val name: String,
    private val symbol: SyntaxSymbol,
) : StatementSyntax {
    override fun matches(token: Token): Boolean =
        token is Token.IdentifierToken && token.lexeme == name

    override fun parse(
        cursor: TokenCursor,
        expressions: Expressions,
    ): Result<Statement> =
        parseCallee(cursor).flatMap { callee ->
            parseLeftParen(cursor).flatMap {
                expressions.parse().flatMap { argument ->
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

    private fun parseCallee(cursor: TokenCursor): Result<Token.IdentifierToken> =
        parseIdentifier(cursor).flatMap { token ->
            if (token.lexeme == name) {
                Success(token)
            } else {
                Failure(Diagnostic.ExpectedSymbol(symbol, token.span))
            }
        }
}
