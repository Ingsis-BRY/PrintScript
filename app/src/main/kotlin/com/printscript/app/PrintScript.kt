package com.printscript.app

import com.printscript.ast.Statement
import com.printscript.cli.Cli
import com.printscript.cli.Program
import com.printscript.cli.ProgressPrinter
import com.printscript.cli.StatementSource
import com.printscript.interpreter.Environment
import com.printscript.interpreter.Interpreter
import com.printscript.interpreter.OutputEmitter
import com.printscript.interpreter.ValueOps
import com.printscript.lexer.Lexer
import com.printscript.lexer.StreamSourceReader
import com.printscript.lexer.recognizer.TokenRecognizers
import com.printscript.parser.Parser
import com.printscript.pipeline.StatementParser
import com.printscript.pipeline.StatementStream
import com.printscript.pipeline.TokenSource
import com.printscript.report.ErrorRenderer
import com.printscript.report.Result
import com.printscript.token.Token
import java.io.Reader
import java.nio.file.Files
import java.nio.file.Path

/**
 * el composition root: el unico lugar que nombra una implementacion concreta de
 * lexer, parser, pipeline o interprete.
 *
 * los tres sinks son las unicas dependencias volatiles del grafo - todo lo demas
 * es determinista y sin I/O - asi que son lo unico que recibe: la consola en
 * produccion, colectores en un test. eso es lo que permite que los tests end to
 * end reusen esta composicion en lugar de copiarla, que es como un test termina
 * comprobando un cableado que nadie ejecuta.
 *
 * PrintScriptCommand queda con los argumentos y los codigos de salida, para que
 * armar el grafo no dependa de un exitProcess que no se puede ejercitar desde un
 * test.
 */
class PrintScript(
    private val output: OutputEmitter,
    private val progress: Appendable,
    private val errors: Appendable,
) {
    fun cli(): Cli =
        Cli(
            newStatements = ::statementsIn,
            newProgram = ::newProgram,
            renderer = ErrorRenderer(),
            progress = ProgressPrinter(progress),
            errors = errors,
        )

    /**
     * abre el archivo y arma el pipeline que lo recorre.
     *
     * entra como factory y no como instancia porque depende del Path, que no
     * existe cuando se arma el grafo, y porque la fuente es de un solo uso
     */
    private fun statementsIn(file: Path): StatementSource {
        val reader = Files.newBufferedReader(file)
        val lexer = Lexer(StreamSourceReader(reader), TokenRecognizers.DEFAULT)

        return StreamStatementSource(
            reader = reader,
            stream =
                StatementStream(
                    source = LexerTokens(lexer),
                    parser = ParserStatements,
                ),
        )
    }

    /**
     * un Environment vacio por corrida: el estado del interprete no se filtra de
     * una ejecucion a la siguiente
     */
    private fun newProgram(): Program {
        val interpreter = Interpreter(Environment(), output, ValueOps())

        return InterpreterProgram(interpreter)
    }

    companion object {
        /** la version que se usa cuando la linea de comandos no pide una */
        const val DEFAULT_VERSION: String = "1.0"

        /**
         * si existe una composicion para [version].
         *
         * vive en el root y no en Cli porque una version del lenguaje es una
         * composicion distinta - otro catalogo de recognizers, otro parser, otro
         * interprete - y no un if adentro del CLI. cuando llegue la 1.1 esto pasa
         * a devolver el grafo correspondiente en lugar de un booleano.
         */
        fun supports(version: String): Boolean = version == DEFAULT_VERSION
    }
}

/**
 * Los tres adapters de abajo conectan cada componente con lo que su consumidor
 * declaro que necesita.
 *
 * Viven aca porque el root es el unico modulo que ve las dos puntas: :pipeline
 * no conoce a :lexer ni a :parser, y :cli no conoce a :interpreter. Como los
 * submodulos son obligatorios, un modulo no puede nombrar las clases que
 * consume; declara la interfaz y el root la satisface.
 *
 * Son clases con nombre y no lambdas: asi hay una declaracion explicita de que
 * LexerTokens es un TokenSource, y el compilador la verifica. Con una referencia
 * a metodo alcanzaba, pero solo porque la firma coincidia - nada decia que Lexer
 * cumpliera el contrato.
 */
private class LexerTokens(
    private val lexer: Lexer,
) : TokenSource {
    override fun tokens(): Sequence<Result<Token>> = lexer.tokens()
}

/** Parser es un object, asi que su adapter tambien puede serlo */
private object ParserStatements : StatementParser {
    override fun parse(tokens: List<Token>): Result<Statement> = Parser.parse(tokens)
}

private class InterpreterProgram(
    private val interpreter: Interpreter,
) : Program {
    override fun execute(statement: Statement): Result<Unit> = interpreter.execute(statement)
}

/**
 * adapta el StatementStream del pipeline a lo que el CLI declaro que necesita, y
 * se queda con el reader para cerrarlo.
 *
 * es la unica pieza que sabe que detras de un programa hay un archivo abierto:
 * por eso vive en el root y no en :cli ni en :pipeline.
 */
private class StreamStatementSource(
    private val reader: Reader,
    private val stream: StatementStream,
) : StatementSource {
    override fun hasNext(): Boolean = stream.hasNext()

    override fun next(): Result<Statement> = stream.next()

    override fun close() = reader.close()
}
