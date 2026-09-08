package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.report.Result

interface StatementExecutor {
    fun matches(statement: Statement): Boolean

    fun execute(
        statement: Statement,
        context: ExecutionContext,
    ): Result<Unit>
}
