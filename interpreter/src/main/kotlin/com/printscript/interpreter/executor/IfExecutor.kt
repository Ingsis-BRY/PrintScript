package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.interpreter.Value
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap

object IfExecutor : StatementExecutor {
    override fun matches(statement: Statement): Boolean = statement is Statement.IfStatement

    override fun execute(
        statement: Statement,
        context: ExecutionContext,
    ): Result<Unit> =
        statement.narrow<Statement.IfStatement> { conditional ->
            context.evaluate(conditional.condition, Type.BooleanType).flatMap { condition ->
                if (condition !is Value.BooleanValue) {
                    return@flatMap Failure(
                        Diagnostic.NonBooleanCondition(
                            actual = condition.type,
                            span = conditional.condition.span,
                        ),
                    )
                }

                branchOf(conditional, condition.value)
                    ?.let(context::executeBlock)
                    ?: Success(Unit)
            }
        }

    private fun branchOf(
        conditional: Statement.IfStatement,
        condition: Boolean,
    ): List<Statement>? = if (condition) conditional.consequence else conditional.alternative
}
