package com.printscript.parser

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.unexpectedEndOfStatement
import com.printscript.parser.ParsingSupport.unexpectedToken
import com.printscript.parser.expression.PrefixParselet
import com.printscript.parser.syntax.ParsingContext
import com.printscript.parser.syntax.StatementSyntax
import com.printscript.report.Result

internal class StatementParser(
    private val cursor: TokenCursor,
    private val syntaxes: List<StatementSyntax>,
    parselets: List<PrefixParselet>,
) {
    private val expressionParser = ExpressionParser(cursor, parselets)
    private val context = Context()

    fun parse(): Result<Statement> {
        val token =
            cursor.peek()
                ?: return unexpectedEndOfStatement(cursor)

        val syntax =
            syntaxes.firstOrNull { it.matches(token) }
                ?: return unexpectedToken(token)

        return syntax.parse(context)
    }

    private inner class Context : ParsingContext {
        override val cursor: TokenCursor
            get() = this@StatementParser.cursor

        override fun parseExpression(): Result<Expression> = expressionParser.parse()

        override fun parseStatement(): Result<Statement> = this@StatementParser.parse()
    }
}
