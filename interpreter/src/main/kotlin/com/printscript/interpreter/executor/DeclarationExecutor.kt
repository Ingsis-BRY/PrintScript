package com.printscript.interpreter.executor

import com.printscript.ast.Statement
import com.printscript.report.Result
import com.printscript.report.Success
import com.printscript.report.flatMap

object DeclarationExecutor : StatementExecutor {
    override fun matches(statement: Statement): Boolean = statement is Statement.VariableDeclaration

    override fun execute(
        statement: Statement,
        context: ExecutionContext,
    ): Result<Unit> =
        statement.narrow<Statement.VariableDeclaration> { declaration ->
            context.environment
                .declare(declaration.name, declaration.declaredType, declaration.span)
                .flatMap {
                    val initializer =
                        declaration.initializer
                            ?: return@flatMap Success(Unit)

                    context.evaluate(initializer).flatMap { value ->
                        context.environment.initialize(declaration.name, value, declaration.span)
                    }
                }
        }
}
