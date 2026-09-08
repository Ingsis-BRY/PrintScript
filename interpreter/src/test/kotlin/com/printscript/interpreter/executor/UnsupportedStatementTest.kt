package com.printscript.interpreter.executor

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.common.Position
import com.printscript.interpreter.CollectingOutput
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.Value
import com.printscript.interpreter.ValueOps
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class UnsupportedStatementTest {
    private val start = Position(3, 5)
    private val end = Position(3, 20)

    private val assignment =
        Statement.Assignment(
            name = "x",
            value = Expression.NumberLiteral(1.0, start, end),
            start = start,
            end = end,
        )

    @Test
    fun `a statement no executor claims is reported instead of thrown`() {
        val interpreter = Interpreter(Environment(), CollectingOutput(), ValueOps(), emptyList())

        val error = assertIs<Failure>(interpreter.execute(assignment)).error

        assertIs<Diagnostic.UnsupportedStatement>(error)
        assertEquals(assignment.span, error.span)
    }

    @Test
    fun `an executor handed a statement that is not its own reports it instead of throwing`() {
        val error =
            assertIs<Failure>(
                CallExecutor(Builtins.DEFAULT).execute(assignment, StubContext()),
            ).error

        assertIs<Diagnostic.UnsupportedStatement>(error)
        assertEquals(assignment.span, error.span)
    }

    private class StubContext : ExecutionContext {
        override val environment: Environment = Environment()

        override fun evaluate(expression: Expression): Result<Value> =
            Success(Value.NumberValue(0.0))

        override fun emit(line: String) = Unit
    }
}
