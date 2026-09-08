package com.printscript.app

import com.printscript.interpreter.CollectingOutput
import picocli.CommandLine
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * la linea de comandos, ejercitada entera.
 *
 * antes esto no se podia testear: el parseo de argumentos vivia en main, al lado
 * del exitProcess que hubiera matado al worker. ahora el comando devuelve el
 * codigo y recibe sus sinks, asi que un test lo corre sin tocar la consola.
 */
class PrintScriptCommandTest {
    // lo que una corrida escribe, mas la salida de uso que imprime picocli
    private class Run {
        val output = CollectingOutput()
        val errors = StringBuilder()
        val usage = StringWriter()

        private val command = PrintScriptCommand(output, errors)

        fun execute(vararg args: String): Int =
            CommandLine(command)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setErr(PrintWriter(usage))
                .setOut(PrintWriter(usage))
                .execute(*args)
    }

    // Codigos de salida

    @Test
    fun `a program that runs exits with zero`() {
        val run = Run()

        val code = run.execute("execution", sourceFile("println(1);").toString())

        assertEquals(CommandLine.ExitCode.OK, code)
        assertEquals(listOf("1"), run.output.lines())
    }

    @Test
    fun `a program that fails exits with the software code`() {
        val run = Run()

        val code = run.execute("execution", sourceFile("println(1 / 0);").toString())

        assertEquals(CommandLine.ExitCode.SOFTWARE, code)
        assertContains(run.errors.toString(), "Division by zero.")
    }

    @Test
    fun `an unknown operation is a usage error`() {
        val run = Run()

        assertEquals(CommandLine.ExitCode.USAGE, run.execute("bogus", "any.ps"))
        assertTrue(run.output.lines().isEmpty())
    }

    @Test
    fun `a missing file is a usage error`() {
        val run = Run()

        val code = run.execute("execution", "no-such-file.ps")

        assertEquals(CommandLine.ExitCode.USAGE, code)
        assertContains(run.errors.toString(), "no such file")
    }

    @Test
    fun `too few arguments is a usage error`() {
        val run = Run()

        assertEquals(CommandLine.ExitCode.USAGE, run.execute("execution"))
    }

    // Operacion

    @Test
    fun `the operation is read regardless of case`() {
        val run = Run()

        val code = run.execute("EXECUTION", sourceFile("println(1);").toString())

        assertEquals(CommandLine.ExitCode.OK, code)
        assertEquals(listOf("1"), run.output.lines())
    }

    @Test
    fun `validation parses without running the program`() {
        val run = Run()

        val code = run.execute("validation", sourceFile("println(1);").toString())

        assertEquals(CommandLine.ExitCode.OK, code)
        assertTrue(run.output.lines().isEmpty(), "validation no ejecuta")
    }

    // Version

    @Test
    fun `the supported version is accepted`() {
        val run = Run()

        val code = run.execute("execution", sourceFile("println(1);").toString(), "1.0")

        assertEquals(CommandLine.ExitCode.OK, code)
        assertEquals(listOf("1"), run.output.lines())
    }

    @Test
    fun `an unsupported version is rejected before the program runs`() {
        val run = Run()

        val code = run.execute("execution", sourceFile("println(1);").toString(), "9.9")

        assertEquals(CommandLine.ExitCode.USAGE, code)
        assertContains(run.errors.toString(), "Unsupported version: 9.9")
        assertTrue(run.output.lines().isEmpty(), "no se ejecuta nada")
    }

    // Progreso

    @Test
    fun `progress is quiet by default`() {
        val run = Run()

        run.execute("validation", sourceFile("println(1); println(2);").toString())

        assertTrue(run.errors.toString().isBlank())
    }

    @Test
    fun `verbose reports the progress of every statement`() {
        val run = Run()

        run.execute("validation", sourceFile("println(1); println(2);").toString(), "--verbose")

        assertEquals(2, run.errors.lines().count { it.isNotBlank() })
    }

    @Test
    fun `the short verbose flag works too`() {
        val run = Run()

        run.execute("validation", sourceFile("println(1);").toString(), "-v")

        assertContains(run.errors.toString(), "parsed statement")
    }

    // Ayuda

    @Test
    fun `help lists the operands and exits cleanly`() {
        val run = Run()

        val code = run.execute("--help")

        assertEquals(CommandLine.ExitCode.OK, code)
        assertContains(run.usage.toString(), "printscript")
        assertContains(run.usage.toString(), "OPERATION")
    }
}
