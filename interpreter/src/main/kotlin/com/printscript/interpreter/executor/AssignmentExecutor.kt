package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.report.flatMap

object AssignmentExecutor : StatementExecutor {
    override fun matches(statement: Statement): Boolean = statement is Statement.Assignment

    override fun execute(
        statement: Statement,
        context: ExecutionContext,
    ): Result<Unit> =
        statement.narrow<Statement.Assignment> { assignment ->
            val expected = context.environment.declaredTypeOf(assignment.name)

            context.evaluate(assignment.value, expected).flatMap { value ->
                context.environment.assign(assignment.name, value, value.type, assignment.span)
            }
        }
}
