package com.printscript.report

import com.printscript.common.Position
import com.printscript.common.Span
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame

class ResultTest {
    private val error = Diagnostic.UnexpectedToken("+", Span.Companion.at(Position(1, 5)))
    private val otherError = Diagnostic.VariableNotDeclared("x", Span.Companion.at(Position(2, 3)))

    @Test
    fun `success should expose its value`() {
        val result: Result<Int> = Success(42)

        assertEquals(42, (result as Success).value)
    }

    @Test
    fun `failure should expose its error`() {
        val result: Result<Int> = Failure(error)

        assertEquals(error, (result as Failure).error)
    }

    @Test
    fun `results with same content should be equal`() {
        assertEquals(Success(42), Success(42))
        assertEquals(Failure(error), Failure(error))
    }

    @Test
    fun `failure should be usable as a result of any type`() {
        val failure = Failure(error)

        val asNumbers: Result<Int> = failure
        val asStatements: Result<List<String>> = failure

        assertSame(failure, asNumbers)
        assertSame(failure, asStatements)
    }

    @Test
    fun `map should transform the value of a success`() {
        val result = Success(21).map { it * 2 }

        assertEquals(Success(42), result)
    }

    @Test
    fun `map should be able to change the type`() {
        val result: Result<String> = Success(42).map { it.toString() }

        assertEquals(Success("42"), result)
    }

    @Test
    fun `map should short-circuit on a failure`() {
        var applied = false

        val result: Result<Int> =
            Failure(error).map { value: Int ->
                applied = true
                value * 2
            }

        assertFalse(applied, "transform should not run on a failure")
        assertEquals(Failure(error), result)
    }

    @Test
    fun `flatMap should chain successful steps`() {
        val result =
            Success(2)
                .flatMap { Success(it + 3) }
                .flatMap { Success(it * 10) }

        assertEquals(Success(50), result)
    }

    @Test
    fun `flatMap should short-circuit on a failure`() {
        var applied = false

        val result: Result<Int> =
            Failure(error).flatMap { value: Int ->
                applied = true
                Success(value * 2)
            }

        assertFalse(applied, "transform should not run on a failure")
        assertEquals(Failure(error), result)
    }

    @Test
    fun `flatMap should propagate the failure returned by a step`() {
        val result =
            Success(2)
                .flatMap { Failure(error) }
                .flatMap { Success(it) }

        assertEquals(Failure(error), result)
    }

    @Test
    fun `flatMap should skip every step after the first failure`() {
        val executedSteps = mutableListOf<String>()

        val result =
            Success(1)
                .flatMap {
                    executedSteps.add("first")
                    Success(it + 1)
                }.flatMap {
                    executedSteps.add("second")
                    Failure(error)
                }.flatMap {
                    executedSteps.add("third")
                    Success(it)
                }.map {
                    executedSteps.add("fourth")
                    it
                }

        assertEquals(listOf("first", "second"), executedSteps)
        assertEquals(Failure(error), result)
    }

    @Test
    fun `flatMap should keep the first error of the chain`() {
        val result =
            Success(1)
                .flatMap { Failure(error) }
                .flatMap { Failure(otherError) }

        assertEquals(Failure(error), result)
    }

    @Test
    fun `map and flatMap should interleave without losing the value`() {
        val result =
            Success("4")
                .map { it.toInt() }
                .flatMap { if (it > 0) Success(it * 5) else Failure(error) }
                .map { "value: $it" }

        assertEquals(Success("value: 20"), result)
    }

    @Test
    fun `fold should collapse a success`() {
        val folded =
            Success(42).fold(
                onSuccess = { "ok: $it" },
                onFailure = { "error at line ${it.span.start.line}" },
            )

        assertEquals("ok: 42", folded)
    }

    @Test
    fun `fold should collapse a failure`() {
        val result: Result<Int> = Failure(error)

        val folded =
            result.fold(
                onSuccess = { "ok: $it" },
                onFailure = { "error at line ${it.span.start.line}" },
            )

        assertEquals("error at line 1", folded)
    }

    @Test
    fun `results should be exhaustively matchable`() {
        val results: List<Result<Int>> = listOf(Success(1), Failure(error))

        val descriptions =
            results.map { result ->
                when (result) {
                    is Success -> "success"
                    is Failure -> "failure"
                }
            }

        assertEquals(listOf("success", "failure"), descriptions)
    }
}
