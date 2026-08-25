Las interfaces viven donde esta el que las consume. Dependency inversion.

Los errores viajan como datos (Diagnostic), no como texto. El unico que abre el ADT completo
es ErrorRenderer: ahi vive el texto de todos los mensajes, y agregar un caso rompe la compilacion ahi.

Result y Diagnostic viven juntos en el modulo report, no en common. Failure(error: Diagnostic)
es un solo tipo: separarlos obligaba a que common conociera el catalogo de errores. common queda
como hoja con lo unico que no depende de nada: Position, Span, Located.

El grafo de objetos se arma en un solo lugar: main, el composition root. Ninguna clase elige con
quien colabora, asi que no hay defaults en los constructores; sacarlos es lo que obliga a que el
root sea el unico archivo que nombra implementaciones concretas.
Las dos piezas que dependen de un valor de runtime entran como factories y no como instancias:
el StatementStream necesita el Reader del archivo y es de un solo uso, y el Interpreter necesita
un Environment vacio por corrida. Un composition root no puede construir lo que todavia no existe.

No se inyecta todo. NumberCodec, OperatorRules, PrecedenceTable y Parser son stateless,
deterministas y sin I/O: son dependencias estables y se siguen llamando directo como object.
Se inyecta solo lo volatil: el I/O (OutputEmitter, SourceReader, los sinks de error y progreso)
y el estado mutable (Environment). Inyectar lo estable agrega ceremonia sin comprar nada.

Descartado: un contenedor de DI (Koin, Dagger). A esta escala aporta reflexion, configuracion y
errores en runtime en lugar de en compilacion; Pure DI se lee de arriba a abajo en un archivo y
lo verifica el compilador.

Un modulo declara api() cuando el tipo del otro aparece en su API publica (Parser.parse devuelve
Result<Statement>) e implementation() cuando lo usa solo por dentro. Con implementation en todos
lados el grafo compilaba solo porque cada consumidor volvia a declarar report por su cuenta.

Lexer, parser e interpreter se consumen detras de interfaces, y esas interfaces las declara
el que las usa, no el que las implementa: pipeline declara TokenSource y StatementParser, y cli
declara Program. Son fun interface, asi que el composition root las satisface con una lambda o
una referencia a metodo y no hace falta ninguna clase adaptadora.
El resultado es que pipeline ya no depende de :lexer ni de :parser, y Cli ya no depende de
:interpreter. El unico archivo que nombra una implementacion concreta sigue siendo Main.kt.

Por que estos tres y no todos: la version 1.1 del lenguaje agrega gramatica (if, boolean, const,
readInput), y eso es una segunda implementacion real de lexer, parser e interpreter. Una interfaz
con una sola implementacion es indireccion, no abstraccion (Reused Abstractions Principle): por eso
ErrorRenderer, Environment, ValueOps y StatementStream siguen siendo clases concretas.

Descartado: poner las interfaces al lado de su implementacion (interface Lexer + class
PrintScriptLexer dentro del modulo lexer). Es lo convencional y cambia menos codigo, pero deja la
abstraccion del lado del proveedor: pipeline seguiria dependiendo de :lexer y :parser en tiempo de
compilacion, que es justo lo que la inversion de dependencias viene a sacar.

El composition root vive en su propio modulo, :app. Antes estaba en :cli, y eso obligaba a :cli a
depender de :lexer, :parser e :interpreter solo para que Main pudiera construirlos: la regla "solo
el root nombra implementaciones" era una convencion que nada impedia romper.
Ahora :cli no tiene esos modulos en el classpath, asi que la hace cumplir Gradle. Sus tests pasaron
a manejarse con fakes de TokenSource, StatementParser y Program, que es exactamente lo que las
interfaces tenian que habilitar; los tests end to end, que si arman el grafo real, viven en :app.

