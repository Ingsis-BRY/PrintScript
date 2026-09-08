package com.printscript.parser

import com.printscript.ast.Statement
import com.printscript.parser.ParsingSupport.unexpectedEndOfStatement
import com.printscript.parser.ParsingSupport.unexpectedToken
import com.printscript.parser.syntax.StatementSyntax
import com.printscript.report.Result

internal class StatementParser(
    private val cursor: TokenCursor,
    private val syntaxes: List<StatementSyntax>,
) {
    private val expressionParser = ExpressionParser(cursor)

    fun parse(): Result<Statement> {
        val token =
            cursor.peek()
                ?: return unexpectedEndOfStatement(cursor)

        val syntax =
            syntaxes.firstOrNull { it.matches(token) }
                ?: return unexpectedToken(token)

        return syntax.parse(cursor, expressionParser)
    }
}
