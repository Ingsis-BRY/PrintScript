package com.printscript.interpreter

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.interpreter.executor.ExecutionContext
import com.printscript.interpreter.executor.StatementExecutor
import com.printscript.interpreter.function.ValueFunction
import com.printscript.language.Environment
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap

class Interpreter(
    private val globalScope: Environment<Value>,
    private val output: OutputEmitter,
    private val valueOps: ValueOps,
    private val executors: List<StatementExecutor>,
    private val functions: Map<String, ValueFunction>,
) {
    fun execute(statement: Statement): Result<Unit> = execute(statement, globalScope)

    private fun execute(
        statement: Statement,
        scope: Environment<Value>,
    ): Result<Unit> {
        val executor =
            executors.firstOrNull { it.matches(statement) }
                ?: return Failure(Diagnostic.UnsupportedStatement(statement.span))

        return executor.execute(statement, Context(scope))
    }

    private fun evaluate(
        expression: Expression,
        expected: Type?,
        scope: Environment<Value>,
    ): Result<Value> =
        when (expression) {
            is Expression.NumberLiteral -> Success(Value.NumberValue(expression.value))
            is Expression.StringLiteral -> Success(Value.StringValue(expression.value))
            is Expression.BooleanLiteral -> Success(Value.BooleanValue(expression.value))
            is Expression.VariableReference -> scope.lookup(expression.name, expression.span)
            is Expression.BinaryExpression -> evaluateBinary(expression, scope)
            is Expression.FunctionCall -> evaluateCall(expression, expected, scope)
        }

    private fun evaluateBinary(
        expression: Expression.BinaryExpression,
        scope: Environment<Value>,
    ): Result<Value> =
        evaluate(expression.left, null, scope).flatMap { left ->
            evaluate(expression.right, null, scope).flatMap { right ->
                valueOps.apply(expression.operator, left, right, expression.span)
            }
        }

    private fun evaluateCall(
        expression: Expression.FunctionCall,
        expected: Type?,
        scope: Environment<Value>,
    ): Result<Value> {
        val function =
            functions[expression.callee]
                ?: return Failure(
                    Diagnostic.UnknownFunction(expression.callee, expression.span),
                )

        return evaluate(expression.argument, Type.StringType, scope).flatMap { argument ->
            function.call(argument, expected, expression.span, Context(scope))
        }
    }

    private inner class Context(
        override val environment: Environment<Value>,
    ) : ExecutionContext {
        override fun evaluate(
            expression: Expression,
            expected: Type?,
        ): Result<Value> = this@Interpreter.evaluate(expression, expected, environment)

        override fun executeBlock(statements: List<Statement>): Result<Unit> {
            val block = environment.child()

            for (statement in statements) {
                val executed = execute(statement, block)

                if (executed is Failure) {
                    return executed
                }
            }

            return Success(Unit)
        }

        override fun emit(line: String) = output.emit(line)
    }
}
