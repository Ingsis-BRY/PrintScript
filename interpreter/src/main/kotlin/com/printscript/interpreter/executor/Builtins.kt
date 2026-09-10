package com.printscript.interpreter.executor

import com.printscript.interpreter.render
import com.printscript.report.Success

object Builtins {
    val V1_0: Map<String, Builtin> = printing()

    val V1_1: Map<String, Builtin> = printing()

    private fun printing(): Map<String, Builtin> =
        mapOf(
            "println" to
                Builtin { argument, context ->
                    context.emit(render(argument))
                    Success(Unit)
                },
        )
}
