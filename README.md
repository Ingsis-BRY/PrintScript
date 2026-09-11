# PrintScript

Un lexer, parser, intérprete, formateador y analizador estático para PrintScript, un
subconjunto de TypeScript. Escrito en Kotlin sobre un build multi-módulo de Gradle.

```bash
./gradlew build                                  # compila, testea, ktlint, detekt, cobertura
./gradlew :app:installDist                       # deja el ejecutable listo
./psc execution programa.ps                      # correr
./psc validation programa.ps                     # sintaxis y semántica, sin ejecutar
./psc execution programa.ps 1.1                  # elegir la versión del lenguaje
./psc formatting programa.ps --config reglas.json
./psc analyzing  programa.ps --config reglas.json
```

Las operaciones son `validation`, `execution`, `formatting` y `analyzing`. La versión del
lenguaje es un argumento opcional; existen `1.0` y `1.1`, y por defecto se usa `1.0`.

## El lenguaje

**1.0** — `let` con tipo explícito, `number` y `string`, aritmética binaria, concatenación con
`+`, y `println`.

**1.1** extiende la 1.0 con:

| | |
|---|---|
| `const` | constantes; una segunda asignación es un error |
| `boolean` | tipo nuevo, con `true` y `false` como literales |
| `if` / `else` | bloques con llaves obligatorias, sin `else if`. La condición es una variable `boolean` |
| `readInput(mensaje)` | imprime el mensaje y lee un valor |
| `readEnv(nombre)` | lee una variable de ambiente |

`readInput` y `readEnv` devuelven texto, y ese texto se lee como el tipo que tenga que llenar:
en `let n: number = readInput("¿Cuántos?")` el resultado es un número, y dentro de un `println`
es un string. Un valor que no se pueda leer como ese tipo corta la ejecución.

Un programa que usa algo que su versión no tiene falla con la posición exacta del problema.

## Cómo está dividido

Las dependencias apuntan siempre hacia abajo: nada de lo de abajo conoce lo de arriba.

```
app ──▶ cli, pipeline, lexer, parser, interpreter, checker, formatter, linter   (composition root)

cli         ──▶ ast, report, common
pipeline    ──▶ ast, token, report
formatter   ──▶ token, report
linter      ──▶ ast, common
lexer       ──▶ token, report, common
parser      ──▶ ast, token, language, report, common
interpreter ──▶ ast, language, report, common
checker     ──▶ ast, language, report, common
language    ──▶ ast, report, common
report      ──▶ ast, common
token, ast  ──▶ common
```

| Módulo | Responsabilidad |
|---|---|
| **common** | Dónde está algo en el fuente: `Position`, `Span`, `Located`. Módulo hoja, no depende de nadie. |
| **token** / **ast** | Los dos vocabularios de datos: lo que produce el lexer y lo que produce el parser. |
| **report** | Cómo viaja un fallo. `Result<T>` (`Success`/`Failure`), el catálogo cerrado de errores `Diagnostic`, y `ErrorRenderer`, el único lugar donde un error se convierte en una frase. |
| **language** | Qué *significa* el lenguaje: `OperatorRules` (qué tipos acepta y devuelve cada operador), `NumberCodec` (texto ⇄ `Double`) y `Environment`, el store de variables con sus reglas de alcance. Lo comparten parser, intérprete y checker para que la semántica viva en un solo lugar. |
| **lexer** | De caracteres a tokens. `SourceReader` → `Lexer` → `Sequence<Result<Token>>`, con maximal munch. |
| **parser** | De tokens a sentencias. `Parser` guarda los catálogos de sintaxis y de parselets, `StatementParser` despacha y `ExpressionParser` resuelve expresiones por precedencia (Pratt). |
| **interpreter** | Ejecuta una sentencia por vez contra un `Environment<Value>`, y emite la salida por `OutputEmitter`. |
| **checker** | El gemelo del intérprete a nivel de tipos: contesta *qué tipo* tiene una expresión sin ejecutar nada. Es lo que hace que `validation` valide semántica. |
| **formatter** | Reescribe el fuente token por token según un archivo de reglas JSON. Trabaja sobre tokens, no sobre el AST. |
| **linter** | Recorre el AST y reporta incumplimientos de convención con su posición. Las reglas se configuran por JSON. |
| **pipeline** | Entrega una sentencia por vez: agrupa tokens hasta donde termina la sentencia y parsea ese lote. |
| **cli** | `Cli`, el único componente con efectos: imprime, reporta errores y decide cuándo cortar. |
| **app** | El composition root. `PrintScript` arma el grafo entero y `Dialect` elige la versión; es el único módulo que nombra implementaciones concretas. |

El diagrama de componentes y el de clases están en
[`docs/architecture-diagram.puml`](docs/architecture-diagram.puml).

## Cómo se elige una versión

**Una versión del lenguaje no es un flag que alguien consulta en runtime: es qué entradas tiene
cada catálogo.** `Dialect`, en `:app`, agrupa los siete que definen una versión:

| Etapa | Catálogo | Qué decide |
|---|---|---|
| `:lexer` | `TokenRecognizers` | qué palabras existen |
| `:parser` | `StatementSyntaxes` | qué sentencias hay |
| `:parser` | `PrefixParselets` | qué puede abrir una expresión |
| `:pipeline` | `StatementBoundaries` | dónde termina una sentencia |
| `:interpreter` | `StatementExecutors` | qué se sabe ejecutar |
| `:interpreter` | `ValueFunctions` | qué funciones dan un valor |
| `:checker` | `FunctionSignatures` | qué tipos piden esas funciones |

Los strings `"1.0"` y `"1.1"` aparecen **únicamente** en `Dialect.kt`. Ningún otro módulo sabe
que existen las versiones: `:lexer` tiene dos listas y nada más. Por eso `const a: string = "x";`
falla en 1.0 sin que nadie haya escrito una comparación — `TokenRecognizers.V1_0` no tiene
recognizer para `const`, así que la palabra sale como identificador y el parser la rechaza.

## Cómo se conectan

Un módulo nunca conoce a quien lo va a usar, pero tampoco conoce a quien lo implementa: **cada
interfaz vive en el módulo que la consume**, y el composition root le da una implementación.
`pipeline` declara `TokenSource`, `StatementParser` y `StatementBoundary`; `cli` declara
`Program`, `StatementSource`, `Formatting` y `Analyzer`; `interpreter` declara `OutputEmitter`,
`InputProvider` y `EnvironmentSource`. Por eso `:cli` y `:pipeline` **no** dependen de `:lexer`,
`:parser` ni `:interpreter` — y eso lo hace cumplir Gradle, no una convención.

`validation` y `execution` son el mismo recorrido con un `Program` distinto: uno chequea tipos y
el otro ejecuta. `Cli` no sabe cuál le tocó.

Tres reglas atraviesan todo el código, y están explicadas en [`DECISIONS.md`](DECISIONS.md):

1. **Los errores son datos**, nunca excepciones ni texto. Todo lo que puede fallar devuelve
   `Result<T>`, y un fallo lleva un `Diagnostic` con los hechos más el `Span` que culpa.
2. **Las interfaces viven con su consumidor**, no con su implementación.
3. **Nada bufferea el fuente entero.** El lexer es una `Sequence` perezosa, el `StatementStream`
   arma de a una sentencia, y el `Cli` recorre el stream de a una: un archivo de cualquier tamaño
   nunca entra completo en memoria.

## Cómo se extiende

- **Un token nuevo** = un `TokenRecognizer` más una entrada en el catálogo de su versión.
- **Una sentencia nueva** = un `StatementSyntax` en `:parser` y un `StatementExecutor` en
  `:interpreter`, más una entrada en cada catálogo, más la rama que el compilador va a exigir en
  el `when` del `Checker`.
- **Una expresión nueva** = un `PrefixParselet` más su entrada.
- **Un error nuevo** = un caso de `Diagnostic` más la rama que el compilador va a exigir en
  `ErrorRenderer`.
- **Una versión nueva** = un `Dialect` con sus siete catálogos.

En casi todos los casos se agrega un archivo y una línea. Donde hay que editar algo que ya
funciona es porque una jerarquía sellada creció, y ahí el compilador señala cada lugar.
