package com.printscript.linter.rules.builtin

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Span
import com.printscript.linter.report.LintFinding
import com.printscript.linter.rules.LintNode
import com.printscript.linter.rules.LintRule

class BuiltinArgumentRule(
    private val builtinName: String,
    private val enabled: Boolean,
    private val invalidFinding: (Span) -> LintFinding,
) : LintRule {
    override fun check(node: LintNode): List<LintFinding> {
        if (!enabled) {
            return emptyList()
        }

        val argument =
            argumentOf(node)
                ?: return emptyList()

        if (isAllowed(argument)) {
            return emptyList()
        }

        return listOf(invalidFinding(argument.span))
    }

    private fun argumentOf(node: LintNode): Expression? =
        when (node) {
            is LintNode.Statement ->
                (node.value as? Statement.CallStatement)
                    ?.takeIf { it.callee == builtinName }
                    ?.argument

            is LintNode.Expression ->
                (node.value as? Expression.FunctionCall)
                    ?.takeIf { it.callee == builtinName }
                    ?.argument
        }

    private fun isAllowed(expression: Expression): Boolean =
        expression !is Expression.BinaryExpression
}
