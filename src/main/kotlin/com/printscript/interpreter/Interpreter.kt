package com.printscript.interpreter

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Result
import com.printscript.common.Success
import com.printscript.common.flatMap

/**
* executes one statement at a time: evaluates expressions, updates the
* [Environment] and emits `println` output. holds the only mutable state,
* the environment, on purpose.
*/
class Interpreter(
    private val environment: Environment = Environment(),
    private val output: OutputEmitter = ConsoleOutput(),
    private val valueOps: ValueOps = ValueOps()
) {

    fun execute(statement: Statement): Result<Unit> =
        when (statement) {
            is Statement.VariableDeclaration -> executeDeclaration(statement)
            is Statement.Assignment -> executeAssignment(statement)
            is Statement.CallStatement -> executeCall(statement)
        }

    private fun executeDeclaration(statement: Statement.VariableDeclaration): Result<Unit> =
        environment.declare(statement.name, statement.declaredType, statement.span).flatMap {
            val initializer = statement.initializer
                ?: return@flatMap Success(Unit)

            evaluate(initializer).flatMap { value ->
                environment.initialize(statement.name, value, statement.span)
            }
        }

    private fun executeAssignment(statement: Statement.Assignment): Result<Unit> =
        evaluate(statement.value).flatMap { value ->
            environment.assign(statement.name, value, statement.span)
        }

    private fun executeCall(statement: Statement.CallStatement): Result<Unit> {
        if (statement.callee != "println") {
            return Failure(Diagnostic.UnknownFunction(statement.callee, statement.span))
        }

        return evaluate(statement.argument).flatMap { value ->
            output.emit(render(value))
            Success(Unit)
        }
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
}
