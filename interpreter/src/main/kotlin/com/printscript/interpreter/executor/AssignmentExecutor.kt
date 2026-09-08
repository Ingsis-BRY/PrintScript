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
            context.evaluate(assignment.value).flatMap { value ->
                context.environment.assign(assignment.name, value, assignment.span)
            }
        }
}
