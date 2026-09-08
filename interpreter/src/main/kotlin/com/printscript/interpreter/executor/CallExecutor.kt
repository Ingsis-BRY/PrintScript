package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.flatMap

class CallExecutor(
    private val builtins: Map<String, Builtin>,
) : StatementExecutor {
    override fun matches(statement: Statement): Boolean = statement is Statement.CallStatement

    override fun execute(
        statement: Statement,
        context: ExecutionContext,
    ): Result<Unit> =
        statement.narrow<Statement.CallStatement> { call ->
            val builtin =
                builtins[call.callee]
                    ?: return@narrow Failure(Diagnostic.UnknownFunction(call.callee, call.span))

            context.evaluate(call.argument).flatMap { argument ->
                builtin.call(argument, context)
            }
        }
}
