package com.printscript.app

import com.printscript.interpreter.CollectingOutput
import com.printscript.interpreter.EnvironmentSource
import com.printscript.interpreter.InputProvider
import java.nio.file.Files
import java.nio.file.Path

internal fun sourceFile(source: String): Path = temporaryFile(source, ".ps")

internal fun configFile(settings: String): Path = temporaryFile(settings, ".json")

private fun temporaryFile(
    content: String,
    suffix: String,
): Path {
    val file = Files.createTempFile("printscript", suffix)
    file.toFile().deleteOnExit()
    Files.writeString(file, content)
    return file
}

internal class QueuedInput(
    answers: List<String>,
) : InputProvider {
    private val remaining = ArrayDeque(answers)

    override fun read(prompt: String): String? = remaining.removeFirstOrNull()
}

internal class FixedEnvironment(
    private val variables: Map<String, String>,
) : EnvironmentSource {
    override fun read(name: String): String? = variables[name]
}

internal class Run(
    version: String = Dialect.DEFAULT_VERSION,
    val output: CollectingOutput = CollectingOutput(),
    val out: StringBuilder = StringBuilder(),
    val progress: StringBuilder = StringBuilder(),
    val errors: StringBuilder = StringBuilder(),
    input: InputProvider = QueuedInput(emptyList()),
    environment: EnvironmentSource = FixedEnvironment(emptyMap()),
    config: Path? = null,
) {
    val cli =
        PrintScript(
            dialect = requireNotNull(Dialect.of(version)) { "no dialect for $version" },
            output = output,
            input = input,
            environment = environment,
            out = out,
            progress = progress,
            errors = errors,
            config = config,
        ).cli()
}
