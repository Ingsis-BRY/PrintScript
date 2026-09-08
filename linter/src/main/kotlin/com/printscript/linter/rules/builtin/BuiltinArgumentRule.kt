package com.printscript.linter.rules.builtin

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Span
import com.printscript.linter.report.LintFinding
import com.printscript.linter.report.LintNode
import com.printscript.linter.rules.LintRule

class BuiltinArgumentRule(
    private val builtinName: String,
    private val enabled: Boolean,
    private val invalidFinding: (Span) -> LintFinding,
) : LintRule {
    override fun check(node: LintNode): List<LintFinding> {
        if (!enabled || node !is LintNode.Statement) {
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
