package com.printscript.interpreter.executor

import com.printscript.ast.Expression
import com.printscript.ast.Statement
import com.printscript.ast.Type
import com.printscript.common.Position
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatementExecutorsTest {
    @Test
    fun `every statement in the hierarchy has an executor`() {
        val uncovered =
            Statement::class.sealedSubclasses.filter { subclass ->
                StatementExecutors.DEFAULT.none { it.matches(sampleOf(subclass)) }
            }

        assertEquals(
            emptyList(),
            uncovered,
            "these statements have no executor in StatementExecutors.DEFAULT",
        )
    }

    @Test
    fun `the hierarchy is not empty`() {
        assertTrue(Statement::class.sealedSubclasses.isNotEmpty())
    }

    private fun sampleOf(subclass: KClass<out Statement>): Statement {
        val start = Position(1, 1)
        val end = Position(1, 1)
        val argument = Expression.NumberLiteral(1.0, start, end)

        return when (subclass) {
            Statement.VariableDeclaration::class ->
                Statement.VariableDeclaration("x", Type.NumberType, null, start, end)

            Statement.Assignment::class ->
                Statement.Assignment("x", argument, start, end)

            Statement.CallStatement::class ->
                Statement.CallStatement("println", argument, start, end)

            else ->
                error("StatementExecutorsTest has no sample for ${subclass.simpleName}")
        }
    }
}
