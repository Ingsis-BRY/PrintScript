package com.printscript.interpreter

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.interpreter.executor.ExecutionContext
import com.printscript.interpreter.executor.StatementExecutor
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap

/**
* executes one statement at a time: evaluates expressions, updates the
* [Environment] and emits `println` output. holds the only mutable state,
* the environment, on purpose.
*/
class Interpreter(
    private val environment: Environment,
    private val output: OutputEmitter,
    private val valueOps: ValueOps,
    private val executors: List<StatementExecutor>,
) {
    private val context = Context()

    fun execute(statement: Statement): Result<Unit> {
        val executor =
            executors.firstOrNull { it.matches(statement) }
                ?: return Failure(Diagnostic.UnsupportedStatement(statement.span))

        return executor.execute(statement, context)
    }

    private fun evaluate(expression: Expression): Result<Value> =
        when (expression) {
            is Expression.NumberLiteral -> Success(Value.NumberValue(expression.value))
            is Expression.StringLiteral -> Success(Value.StringValue(expression.value))
            is Expression.VariableReference -> environment.lookup(expression.name, expression.span)
            is Expression.BinaryExpression -> evaluateBinary(expression)
        }

    private fun evaluateBinary(expression: Expression.BinaryExpression): Result<Value> =
        evaluate(expression.left).flatMap { left ->
            evaluate(expression.right).flatMap { right ->
                valueOps.apply(expression.operator, left, right, expression.span)
            }
        }

    private inner class Context : ExecutionContext {
        override val environment: Environment
            get() = this@Interpreter.environment

        override fun evaluate(expression: Expression): Result<Value> =
            this@Interpreter.evaluate(expression)

        override fun emit(line: String) = output.emit(line)
    }
}
