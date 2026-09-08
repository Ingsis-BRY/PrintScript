package com.printscript.linter.rules.identifier

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Span
import com.printscript.linter.config.identifier.IdentifierFormatConfig
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode
import com.printscript.linter.rules.LintRule

class IdentifierFormatingRule(
    private val config: IdentifierFormatConfig,
) : LintRule {
    private val validator = IdentifierValidator(config.style)

    override fun check(node: LintNode): List<LintFinding> {
        if (!config.enabled) {
            return emptyList()
        }

        return when (node) {
            is LintNode.Statement -> checkStatement(node.value)
            is LintNode.Expression -> checkExpression(node.value)
        }
    }

    private fun checkStatement(statement: Statement): List<LintFinding> =
        when (statement) {
            is Statement.VariableDeclaration ->
                checkName(
                    statement.name,
                    Span(statement.start, statement.end),
                )

            is Statement.Assignment ->
                checkName(
                    statement.name,
                    Span(statement.start, statement.end),
                )

            is Statement.CallStatement ->
                emptyList()
        }

    private fun checkExpression(expression: Expression): List<LintFinding> =
        when (expression) {
            is Expression.VariableReference ->
                checkName(
                    expression.name,
                    Span(expression.start, expression.end),
                )

            is Expression.NumberLiteral,
            is Expression.StringLiteral,
            is Expression.BinaryExpression,
            ->
                emptyList()
        }

    private fun checkName(
        name: String,
        span: Span,
    ): List<LintFinding> {
        if (validator.isValid(name)) {
            return emptyList()
        }

        return listOf(
            LintFinding.InvalidIdentifier(
                name = name,
                expectedStyle = config.style,
                span = span,
            ),
        )
    }
}
