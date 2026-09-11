package com.printscript.interpreter.function

import com.printscript.ast.Type
import com.printscript.common.Span
import com.printscript.interpreter.Value
import com.printscript.report.Diagnostic
import com.printscript.report.Failure
import com.printscript.report.Result
import com.printscript.report.Success

internal fun coerce(
    text: String,
    expected: Type?,
    span: Span,
): Result<Value> {
    val target = expected ?: Type.StringType

    return when (target) {
        Type.StringType ->
            Success(Value.StringValue(text))

        Type.NumberType ->
            text
                .trim()
                .toDoubleOrNull()
                ?.let { Success(Value.NumberValue(it)) }
                ?: uninterpretable(text, target, span)

        Type.BooleanType ->
            when (text.trim()) {
                "true" -> Success(Value.BooleanValue(true))
                "false" -> Success(Value.BooleanValue(false))
                else -> uninterpretable(text, target, span)
            }
    }
}

private fun uninterpretable(
    text: String,
    expected: Type,
    span: Span,
): Failure = Failure(Diagnostic.UninterpretableInput(text, expected, span))
