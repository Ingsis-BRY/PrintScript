package com.printscript.linter.rules.builtin

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Span
import com.printscript.linter.config.builtin.BuiltinConfig
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode
import com.printscript.linter.rules.LintRule

class BuiltinArgumentRule(
    private val builtinName: String,
    private val config: BuiltinConfig,
    private val invalidFinding: (Span) -> LintFinding,
) : LintRule {
    override fun check(node: LintNode): List<LintFinding> {
        if (!config.enabled || node !is LintNode.Statement) {
            return emptyList()
        }

        val statement = node.value

        if (statement !is Statement.CallStatement ||
            statement.callee != builtinName
        ) {
            return emptyList()
        }

        if (isAllowed(statement.argument)) {
            return emptyList()
        }

        return listOf(
            invalidFinding(
                Span(
                    statement.argument.start,
                    statement.argument.end,
                ),
            ),
        )
    }

    private fun isAllowed(expression: Expression): Boolean =
        expression !is Expression.BinaryExpression
}
