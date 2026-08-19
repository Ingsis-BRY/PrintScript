package com.printscript.language

import com.printscript.common.Diagnostic
import com.printscript.common.Failure
import com.printscript.common.Result
import com.printscript.common.Span
import com.printscript.common.Success

object NumberCodec {

    fun parse(
        text: String,
        span: Span
    ): Result<Double> {
        val value = text.toDoubleOrNull()

        return if (value != null) {
            Success(value)
        } else {
            Failure(Diagnostic.MalformedNumber(text, span))
        }
    }

    fun render(value: Double): String =
        if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            value.toString()
        }
}
