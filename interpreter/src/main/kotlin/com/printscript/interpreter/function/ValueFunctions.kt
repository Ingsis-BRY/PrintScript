package com.printscript.interpreter.function

import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
import com.printscript.interpreter.render
import com.printscript.report.Diagnostic
import com.printscript.report.Failure

object ValueFunctions {
    val NONE: Map<String, ValueFunction> = emptyMap()

    fun readers(
        input: InputProvider,
        environment: EnvironmentSource,
    ): Map<String, ValueFunction> =
        mapOf(
            "readInput" to readInput(input),
            "readEnv" to readEnv(environment),
        )

    private fun readInput(input: InputProvider): ValueFunction =
        ValueFunction { argument, expected, span, context ->
            val prompt = render(argument)

            context.emit(prompt)

            val answer =
                input.read(prompt)
                    ?: return@ValueFunction Failure(Diagnostic.MissingInput(span))

            coerce(answer, expected, span)
        }

    private fun readEnv(environment: EnvironmentSource): ValueFunction =
        ValueFunction { argument, expected, span, _ ->
            val name = render(argument)

            val value =
                environment.read(name)
                    ?: return@ValueFunction Failure(
                        Diagnostic.MissingEnvironmentVariable(name, span),
                    )

            coerce(value, expected, span)
        }
}
