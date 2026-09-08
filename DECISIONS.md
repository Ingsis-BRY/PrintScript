Las interfaces viven donde esta el que las consume. Dependency inversion.

Los errores viajan como datos (Diagnostic), no como texto. El unico que abre el ADT completo
es ErrorRenderer: ahi vive el texto de todos los mensajes, y agregar un caso rompe la compilacion ahi.

Result y Diagnostic viven juntos en el modulo report, no en common. Failure(error: Diagnostic)
es un solo tipo: separarlos obligaba a que common conociera el catalogo de errores. common queda
como hoja con lo unico que no depende de nada: Position, Span, Located.

El grafo de objetos se arma en un solo lugar: PrintScript.kt, el composition root. Ninguna clase
elige con quien colabora, asi que no hay defaults en los constructores; sacarlos es lo que obliga a
que el root sea el unico archivo que nombra implementaciones concretas.
PrintScriptCommand queda solo con los argumentos y los codigos de salida, y main con el exitProcess.
Estan separados porque el root y el parseo de argumentos tienen que poder ejercitarse desde un test,
y lo unico que no se puede es terminar el proceso: mataria al worker.
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
El CLI hace lo mismo con la fuente de sentencias: declara StatementSource, un AutoCloseable que
entrega una sentencia por vez, y recibe una fabrica sobre Path. No sabe que detras hay un archivo,
un lexer y un parser; el adapter que los une, y que cierra el reader, vive en el root.
El resultado es que pipeline ya no depende de :lexer ni de :parser, y cli no depende ni de
:pipeline ni de :interpreter: le quedan :common, :ast y :report. El unico archivo que nombra una
implementacion concreta es PrintScript.kt.

Se abstrae lo que borra una arista de compilacion, no todo lo que se podria. Por eso ErrorRenderer
sigue siendo una clase concreta en el constructor de Cli: :cli necesita :report igual, para Result y
Diagnostic, asi que una interfaz ahi no sacaria ninguna dependencia. Seria indireccion sin nada que
mostrar.

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

Un test que arma su propio sujeto no es un composition root; uno que arma la aplicacion entera si.
La distincion importa porque un test es su propio entry point, asi que componer adentro de un test
no esta mal por si mismo: lo que estaba mal era que EndToEndTest duplicara el grafo de produccion.
Un duplicado asi deja pasar el caso peor - se cambia el cableado en PrintScript.kt y la suite sigue
verde contra el cableado viejo. Por eso EndToEndTest y ErrorReportingTest consumen PrintScript y
reemplazan solo los tres sinks, mientras que CliTest sigue armando sus propios fakes (es unitario,
su sujeto es Cli) y StatementStreamTest sigue armando su lexer y su parser (es de integracion, y
esos son los colaboradores contra los que el pipeline esta especificado).

ErrorReportingTest se mudo de :report a :app por lo mismo: necesita el grafo entero, y el grafo se
arma en un solo lugar. Cablearlo a mano obligaba a :report - un modulo hoja - a tener :lexer,
:parser, :token e :interpreter en su classpath de test.

:app entra en la medicion de cobertura; lo unico excluido es MainKt, que no se puede invocar desde
un test porque termina el proceso. Cuando el cableado vivia en Main.kt habia que excluir el modulo
entero, y eso dejaba los dos tests end to end sin contar para el umbral.

Elegir la version del lenguaje es una decision del composition root, no del CLI: PrintScript.supports
la contesta antes de construir un solo objeto. Cuando llegue la 1.1 eso pasa a devolver el grafo que
corresponde - otro catalogo de recognizers, otro parser, otro interprete - en lugar de un booleano.
Antes vivia adentro de Cli como un require() que tiraba IllegalArgumentException y que main atajaba
para traducirlo a un codigo de salida: era la ultima excepcion usada como control de flujo, en un
codigo cuya primera invariante es que los errores son datos.

picocli parsea argv y nada mas: no arma ningun grafo ni inyecta nada, asi que la composicion sigue
siendo constructores que verifica el compilador. Es la primera dependencia de terceros del proyecto,
y se gano el lugar porque borro codigo en lugar de agregarlo - el string de uso, el conteo de
operandos, el filtrado de --verbose y el parseo de la operacion - y porque sus codigos de salida por
defecto (OK 0, SOFTWARE 1, USAGE 2) son exactamente los que el CLI ya usaba.
La contra honesta: lee sus anotaciones por reflexion, y obliga a que los campos del comando sean var
y lateinit porque los asigna despues de construir. Eso convive con "el compilador verifica el grafo"
porque lo que refleja es argv, en el borde del proceso, y no las dependencias entre modulos; pero es
una tension real y conviene decirla antes de que la encuentren.
