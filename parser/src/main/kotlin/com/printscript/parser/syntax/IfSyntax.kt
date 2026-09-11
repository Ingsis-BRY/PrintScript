package com.printscript.parser.syntax

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Position
import com.printscript.parser.ParsingSupport.parseKeyword
import com.printscript.parser.ParsingSupport.parseLeftBrace
import com.printscript.parser.ParsingSupport.parseLeftParen
import com.printscript.parser.ParsingSupport.parseRightBrace
import com.printscript.parser.ParsingSupport.parseRightParen
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.SyntaxSymbol
import com.printscript.report.flatMap
import com.printscript.report.map
import com.printscript.token.Token

object IfSyntax : StatementSyntax {
    override fun matches(token: Token): Boolean = token is Token.IfToken

    override fun parse(context: ParsingContext): Result<Statement> =
        parseKeyword(context.cursor, SyntaxSymbol.IF) { it is Token.IfToken }
            .flatMap { ifToken ->
                parseCondition(context).flatMap { condition ->
                    parseBranches(context, condition, ifToken.start)
                }
            }

    private fun parseBranches(
        context: ParsingContext,
        condition: Expression,
        start: Position,
    ): Result<Statement> =
        parseBlock(context).flatMap { consequence ->
            parseAlternative(context).map { alternative ->
                Statement.IfStatement(
                    condition = condition,
                    consequence = consequence.statements,
                    alternative = alternative?.statements,
                    start = start,
                    end = (alternative ?: consequence).end,
                )
            }
        }

    private fun parseCondition(context: ParsingContext): Result<Expression> =
        parseLeftParen(context.cursor).flatMap {
            context.parseExpression().flatMap { condition ->
                parseRightParen(context.cursor).flatMap {
                    if (condition is Expression.VariableReference) {
                        Success(condition)
                    } else {
                        Failure(Diagnostic.NonVariableCondition(condition.span))
                    }
                }
            }
        }

    private fun parseAlternative(context: ParsingContext): Result<Block?> {
        if (context.cursor.peek() !is Token.ElseToken) {
            return Success(null)
        }

        context.cursor.consume()

        return parseBlock(context)
    }

    private fun parseBlock(context: ParsingContext): Result<Block> {
        val cursor = context.cursor

        return parseLeftBrace(cursor).flatMap {
            val statements = mutableListOf<Statement>()

            while (cursor.peek() != null && cursor.peek() !is Token.RightBraceToken) {
                when (val statement = context.parseStatement()) {
                    is Failure -> return@flatMap statement
                    is Success -> statements.add(statement.value)
                }
            }

            parseRightBrace(cursor).map { close ->
                Block(statements, close.end)
            }
        }
    }

    private data class Block(
        val statements: List<Statement>,
        val end: Position,
    )
}
