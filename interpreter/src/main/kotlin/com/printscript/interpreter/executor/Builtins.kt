package com.printscript.interpreter.executor

import com.printscript.interpreter.render
import com.printscript.report.Success

object Builtins {
    val DEFAULT: Map<String, Builtin> =
        mapOf(
            "println" to
                Builtin { argument, _, context ->
                    context.emit(render(argument))
                    Success(Unit)
                },
        )
}
