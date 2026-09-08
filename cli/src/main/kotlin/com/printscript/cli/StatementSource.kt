package com.printscript.cli

import com.printscript.ast.Statement
import com.printscript.report.Result

/**
 * el programa que el CLI recorre, una sentencia por vez, junto con el recurso
 * que lo respalda.
 *
 * el CLI no sabe que detras hay un archivo, un lexer y un parser: pide la
 * siguiente sentencia y cierra cuando termina. quien lo arma es el composition
 * root, el unico que sabe como encajan.
 *
 * no es una fun interface porque tiene tres miembros; el cierre viaja con la
 * fuente para que el CLI no tenga que conocer el recurso que hay detras.
 */
interface StatementSource : AutoCloseable {
    fun hasNext(): Boolean

    fun next(): Result<Statement>
}
