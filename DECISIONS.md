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

No se inyecta todo. NumberCodec, OperatorRules y PrecedenceTable son stateless, deterministas y sin
I/O: son dependencias estables y se siguen llamando directo como object.
Se inyecta solo lo volatil: el I/O (OutputEmitter, SourceReader, los sinks de error y progreso),
el estado mutable (Environment) y los catalogos que definen que entiende cada version del lenguaje
(los recognizers, las sintaxis, los executors). Inyectar lo estable agrega ceremonia sin comprar nada.

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

Elegir la version del lenguaje es una decision del composition root, no del CLI. Con la 1.1 eso
dejo de ser un booleano y paso a ser Dialect.of(version), que devuelve el grafo que corresponde -
otros catalogos de recognizers, de sintaxis, de executors - antes de construir un solo objeto.
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

Lo comun del build vive en convention plugins en buildSrc, no en un subprojects {} del root.
Los 13 modulos repetian el mismo bloque de plugins, toolchain, dependencia de test y
useJUnitPlatform; ahora declaran un id y sus dependencias, y nada mas. La diferencia real no es
el ahorro de lineas: un subprojects {} configura hijos desde afuera, asi que hay que leer el root
para saber que le pasa a un modulo, y no hay forma de que un modulo elija. Un plugin se aplica,
y aplicarlo es una linea visible en el modulo. Por eso :app aplica kotlin-application y no
kotlin-module: la diferencia entre una libreria y un ejecutable pasa a estar declarada.
Descartado: un composite build con includeBuild("build-logic"), que es lo idiomatico a escala
pero agrega un settings y un build entero para 13 modulos chicos. Descartado tambien dejar el
subprojects {} y usar buildSrc solo para lo de los modulos: la configuracion quedaba partida en
dos lugares, que era el problema original.

El parser y el interprete despachan por registro, igual que el lexer. TokenRecognizers.DEFAULT ya
era eso: agregar un token es un archivo nuevo y una entrada, nunca editar un recognizer. Ahora
StatementSyntaxes.DEFAULT y StatementExecutors.DEFAULT tienen la misma forma, y el orden desempata
igual - CallSyntax va antes que AssignmentSyntax porque las dos reclaman un IdentifierToken.
En el parser el cambio no cuesta nada: el dispatch ya tenia un else, o sea que el conjunto de
sentencias siempre fue abierto.
En el interprete si cuesta. El when era exhaustivo sobre un sealed interface, asi que el compilador
era el que avisaba que faltaba cubrir una sentencia nueva; el registro cambia ese aviso por uno en
tiempo de test. Se paga a conciencia y se compensa con StatementExecutorsTest, que recorre
Statement::class.sealedSubclasses y falla si alguna no esta cubierta - incluida la variante de que
la sentencia sea tan nueva que el test no sepa construirla. Es el mismo trato que ya hace
ErrorRenderer al reves: ahi se eligio que el compilador obligue, aca que obligue un test, porque lo
que se compra es que agregar una sentencia deje de tocar codigo existente.
Un executor que reciba una sentencia que no es la suya devuelve UnsupportedStatement y no tira
ClassCastException: la primera invariante vale tambien para el codigo que hace de plumbing.
Descartado: hacer publico ParsingSupport para que se puedan escribir sintaxis desde otro modulo.
Congelaria los helpers como API sin un consumidor real. Las sintaxis viven en :parser igual que
los recognizers viven en :lexer.

ParsingContext y ExecutionContext son la excepcion a la regla del parrafo de arriba, y conviene
decir por que antes de que la encuentren. Las dos tienen una sola implementacion, asi que por el
Reused Abstractions Principle no deberian existir; la razon por la que existen igual no es abstraer
sino romper un ciclo *adentro* del modulo. Una sintaxis necesita evaluar expresiones y un executor
necesita el Environment, pero si los nombraran directo (ExpressionParser, Interpreter) el registro
dependeria de su propio despachador, que es quien construye el registro. El contexto corta eso.
La contra honesta: son interfaces de contexto, no declaraciones de necesidad, y una interfaz de
contexto tiende a crecer hasta ser un god object. Por eso las dos estan capadas a lo minimo -
ParsingContext expone dos miembros, ExecutionContext tres - y las implementa una inner class
privada del despachador, no el despachador mismo, para no volver publico lo que era privado.
Se paga tambien que Environment quede alcanzable a traves de ExecutionContext; se acepta porque los
tres executors necesitan la API completa de Environment igual, y no habia forma de darles menos.

El registro se prueba en los dos sentidos, no solo en el feliz. StatementExecutorsTest verifica que
el catalogo cubra todo el sealed hierarchy, y UnsupportedStatementTest verifica los dos caminos de
fallo que el registro estrena: un statement que nadie reclama, y un executor al que le entregan una
sentencia ajena. El segundo es el que sostiene la promesa de que narrow devuelve un Diagnostic en
vez de tirar ClassCastException - se comprobo rompiendolo a proposito, con un cast inseguro, y el
test falla con ClassCastException como corresponde.

Analyzing recorre sentencias adentro de Cli, igual que validation y execution, y no en un adapter
aparte. Antes tenia su propio loop en el composition root, y ese loop se tragaba los Failure: el
archivo con un error de sintaxis no reportaba nada y salia con codigo 0, en contra de la consigna.
Compartir overStatements arregla las tres cosas de una: corta en el primer error, lo reporta por el
renderer, y muestra el progreso del parseo. La leccion no es que faltaba un if - es que analyzing
era la unica de las cuatro operaciones sin un test end to end, y no lo tenia porque escribia por un
println de Kotlin en vez de por un sink inyectado. Lo no inyectable es lo no testeable.
Los hallazgos ahora salen por FindingRenderer, en :linter, que es a LintFinding lo que ErrorRenderer
es a Diagnostic: el unico lugar que abre la jerarquia y el unico que escribe prosa. Antes ese texto
vivia suelto en PrintScript.kt, que es el composition root y no tendria que redactar nada.
Cli declara Analyzer, la interfaz de lo que necesita - dame los hallazgos de esta sentencia - y no
conoce :linter. El sink de stdout paso a llamarse out porque ahora lleva dos cosas: el fuente
formateado y los hallazgos.

Una version del lenguaje es que entradas tiene cada catalogo, no un flag que alguien consulta.
Dialect, en :app, agrupa los siete que la definen: recognizers, sintaxis, parselets, corte de
sentencias, executors, funciones-valor y firmas. PrintScript arma el mismo grafo para las dos
versiones; lo unico que cambia es que hay adentro. La consecuencia verificable es que los strings
"1.0" y "1.1" aparecen unicamente en Dialect.kt: :lexer no sabe que existen las versiones, tiene
dos listas. Por eso `const` es un error de sintaxis en 1.0 sin que nadie escriba una comparacion -
V1_0 no tiene recognizer para la palabra, asi que sale como identificador y el parser la rechaza.
Descartado: pasarle la version a cada modulo. Un enum compartido obligaria a los siete a conocer el
concepto de version, que es mas acoplamiento del que saca.
La contra honesta: nada verifica que los siete catalogos de una version existan y sean coherentes.
Si alguien agrega V1_2 a seis de siete, no se entera hasta runtime. Dialect es el unico lugar que
los aparea, y lo hace a mano.

Que puede *abrir* una expresion es un registro; que operador liga mas fuerte es una tabla.
PrefixParselets es el tercer catalogo con la misma forma que los otros dos, y existe porque el
conjunto de cosas que abren una expresion crece con el lenguaje: un literal booleano en 1.1, una
llamada en 1.1. Los operadores infijos deliberadamente no son parselets - su precedencia la lee un
solo loop de PrecedenceTable, y no hay comportamiento por operador que registrar. El Pratt queda
partido en las dos mitades que la gramatica realmente tiene, no en dos mitades simetricas.

Donde termina una sentencia salio de StatementStream y paso a ser un catalogo. Es lo unico de la
gramatica que el pipeline no puede no saber, porque arma el lote antes de parsear, y con `if` dejo
de ser "hasta el primer ;": un `;` adentro de un bloque no corta, y el `}` que cierra el bloque
exterior corta salvo que siga un `else`. De ahi el unico token de lookahead que el stream sostiene.
StatementScan es un valor y no una maquina: take contesta con el scan que leyo un token mas, igual
que el Cursor del formatter, asi un boundary que cuenta llaves no se filtra a la sentencia siguiente.

El scope viaja como argumento y no como campo. Un bloque corre en un Environment hijo, pero el
interprete no tiene un "scope actual" que mover y volver a poner: cada bloque recibe su propio
Context. No hay nada que restaurar, y un fallo no puede dejar el scope equivocado puesto. Se hizo
asi despues de escribirlo con un var y un try/finally, que funcionaba pero rompia la invariante que
este documento venia sosteniendo desde 1.0 - el Environment es el unico componente mutable.

El tipo esperado baja en la evaluacion. evaluate recibe el tipo que el resultado tiene que llenar.
Casi toda expresion lo ignora, porque su tipo lo decide lo que es; readInput y readEnv son la
excepcion: lo que devuelven es texto hasta que algo dice que se suponia que era. Ese algo es la
declaracion, la asignacion, o println, que pasa string porque todo lo que imprime ya es texto.
El limite conocido: adentro de una expresion binaria la expectativa no baja, asi que
`let n: number = readInput("a") + 1;` lee la entrada como string. Es un caso que la consigna no
define y se deja escrito en vez de disimulado.

El prompt de readInput sale por el mismo emitter que println. Es salida, y el programa tiene una
sola; el valor entra por InputProvider, declarada por el consumidor igual que OutputEmitter, asi
que un test le pasa respuestas preparadas sin tocar el stdin del proceso. InputProvider.read no
recibe el prompt: lo imprime quien llama, y un parametro que ninguna implementacion usa es
generalidad especulativa.

validation valida semantica, y lo hace siendo la misma funcion que execution con otro Program.
Hasta la 1.1 solo parseaba, asi que `let x: number = "hola";` salia con codigo 0 pese a que la
consigna pide "un modo que solo valide la sintaxis y semantica del archivo". El :checker es el
gemelo del interprete a nivel de tipos: donde el interprete contesta que valor tiene una expresion,
el checker contesta que tipo, sin ejecutar nada. Cli perdio codigo en vez de ganarlo - validate y
execute eran el mismo loop.
Los dos comparten las reglas de alcance a traves de Environment<V>, que se parametrizo y se mudo a
:language. El interprete guarda un Value; el checker guarda Unit, porque a la hora de chequear el
unico hecho que sobrevive es *que* el nombre fue asignado - el tipo de cada slot ya esta en el slot.
El tipo se pasa en cada bind en vez de leerse del V, porque un store que sepa tipar su contenido no
podria guardar un Unit.
Descartado: un TypeScope aparte. Duplicaba child(), ownerOf() y el shadowing, que es exactamente la
logica que mas cuesta hacer bien dos veces.
El checker despacha con un when exhaustivo y no con un registro, a diferencia de las otras etapas:
no varia por version, porque el parser de cada version ya decide que sentencias pueden existir. Lo
que si varia son las firmas de las funciones, y eso si es un catalogo.
Limite conocido: readInput y readEnv no se pueden validar del todo sin ejecutar. Se valida que la
funcion exista, que su argumento sea un string y que el tipo a llenar sea uno que el lenguaje tiene;
un valor que no se pueda leer sigue siendo un error de ejecucion, que es lo que la consigna pide.

La condicion de un `if` tiene que ser una variable, y se eligio la lectura literal de la consigna.
La frase es "Solo con variables 'boolean' como argumento". La alternativa - aceptar cualquier
expresion booleana - es estrictamente mas util y no rompe ningun programa valido, pero la letra dice
variables y la unica razon para desviarse seria la comodidad. IfSyntax rechaza cualquier otra cosa
con NonVariableCondition, en el parser; que ademas sea boolean lo verifica el checker, en tipos, y
el interprete, en ejecucion. Las tres capas dicen lo mismo en el momento en que cada una puede.

La regla de argumentos del linter es lista blanca y no lista negra. Era
`expression !is BinaryExpression`, que aceptaba todo lo que no fuera una binaria - y cuando 1.1
sumo FunctionCall, `println(readInput("x"))` paso a colarse en silencio. Ahora enumera lo que si
vale (nombre, numero, string, booleano) en un when exhaustivo sobre la jerarquia sellada, asi que
una expresion nueva rompe la compilacion ahi y obliga a decidir de que lado cae. La leccion es la
de siempre en este codigo: lo que se enumera es lo que se permite, no lo que se prohibe.
