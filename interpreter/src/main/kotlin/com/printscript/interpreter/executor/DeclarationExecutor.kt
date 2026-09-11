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
                .declare(
                    name = declaration.name,
                    type = declaration.declaredType,
                    mutable = declaration.mutable,
                    span = declaration.span,
                ).flatMap {
                    val initializer =
                        declaration.initializer
                            ?: return@flatMap Success(Unit)

                    context
                        .evaluate(initializer, declaration.declaredType)
                        .flatMap { value ->
                            context.environment
                                .initialize(declaration.name, value, value.type, declaration.span)
                        }
                }
        }
}
