# PrintScript

Un lexer, parser, intérprete, formateador y analizador estático para PrintScript 1.0, un
subconjunto de TypeScript. Escrito en Kotlin sobre un build multi-módulo de Gradle.

```bash
./gradlew build                                  # compila, testea, ktlint, detekt, cobertura
./gradlew :app:installDist                       # deja el ejecutable listo
./psc execution programa.ps                      # correr
./psc validation programa.ps                     # solo validar
./psc formatting programa.ps --config reglas.json
./psc analyzing  programa.ps --config reglas.json
```

Las operaciones son `validation`, `execution`, `formatting` y `analyzing`. La versión del
lenguaje es un argumento opcional y por ahora sólo existe `1.0`.

## Cómo está dividido

Las dependencias apuntan siempre hacia abajo: nada de lo de abajo conoce lo de arriba.

```
app ──▶ cli, pipeline, lexer, parser, interpreter, formatter, linter   (el composition root)

cli         ──▶ ast, report, common
pipeline    ──▶ ast, token, report
formatter   ──▶ token, report
linter      ──▶ ast, common
lexer       ──▶ token, report, common
parser      ──▶ ast, token, language, report, common
interpreter ──▶ ast, language, report, common
language    ──▶ ast, report, common
report      ──▶ ast, common
token, ast  ──▶ common
```

| Módulo | Responsabilidad |
|---|---|
| **common** | Dónde está algo en el fuente: `Position`, `Span`, `Located`. Módulo hoja, no depende de nadie. |
| **token** / **ast** | Los dos vocabularios de datos: lo que produce el lexer y lo que produce el parser. |
| **report** | Cómo viaja un fallo. `Result<T>` (`Success`/`Failure`), el catálogo cerrado de errores `Diagnostic`, y `ErrorRenderer`, el único lugar donde un error se convierte en una frase. |
| **language** | Qué *significa* el lenguaje: `OperatorRules` (qué tipos acepta y devuelve cada operador) y `NumberCodec` (texto ⇄ `Double`, y cómo se imprime un número). Lo comparten parser e intérprete para que la semántica viva en un solo lugar. |
| **lexer** | De caracteres a tokens. `SourceReader` → `Lexer` → `Sequence<Result<Token>>`, con maximal munch. |
| **parser** | De tokens a sentencias. `Parser` guarda el catálogo de sintaxis, `StatementParser` despacha y `ExpressionParser` resuelve expresiones por precedencia (Pratt). |
| **interpreter** | Ejecuta una sentencia por vez contra un `Environment`, y emite la salida por `OutputEmitter`. |
| **formatter** | Reescribe el fuente token por token según un archivo de reglas JSON. Trabaja sobre tokens, no sobre el AST. |
| **linter** | Recorre el AST y reporta incumplimientos de convención con su posición. Las reglas se configuran por JSON. |
| **pipeline** | Entrega una sentencia por vez: agrupa tokens hasta el `;` y parsea ese lote. |
| **cli** | `Cli`, el único componente con efectos: imprime, reporta errores y decide cuándo cortar. |
| **app** | El composition root. `PrintScript` arma el grafo entero y es el único archivo que nombra implementaciones concretas. |

El diagrama de componentes y el de clases están en
[`docs/architecture-diagram.puml`](docs/architecture-diagram.puml).

## Cómo se conectan

Un módulo nunca conoce a quien lo va a usar, pero tampoco conoce a quien lo implementa: **cada
interfaz vive en el módulo que la consume**, y el composition root le da una implementación.
`pipeline` declara `TokenSource` y `StatementParser`; `cli` declara `Program`, `StatementSource`,
`Formatting` y `Analyzer`; `interpreter` declara `OutputEmitter`. Por eso `:cli` y `:pipeline`
**no** dependen de `:lexer`, `:parser` ni `:interpreter` — y eso lo hace cumplir Gradle, no una
convención.

Tres reglas atraviesan todo el código, y están explicadas en [`DECISIONS.md`](DECISIONS.md):

1. **Los errores son datos**, nunca excepciones ni texto. Todo lo que puede fallar devuelve
   `Result<T>`, y un fallo lleva un `Diagnostic` con los hechos más el `Span` que culpa.
2. **Las interfaces viven con su consumidor**, no con su implementación.
3. **Nada bufferea el fuente entero.** El lexer es una `Sequence` perezosa, el `StatementStream`
   arma de a una sentencia, y el `Cli` recorre el stream de a una: un archivo de cualquier tamaño
   nunca entra completo en memoria.

## Cómo se extiende

- **Un token nuevo** = un `TokenRecognizer` más una entrada en `TokenRecognizers.DEFAULT`.
- **Una sentencia nueva** = un `StatementSyntax` en `:parser` y un `StatementExecutor` en
  `:interpreter`, más una entrada en cada catálogo `DEFAULT`.
- **Un error nuevo** = un caso de `Diagnostic` más la rama que el compilador va a exigir en
  `ErrorRenderer`.

En los tres casos se agrega un archivo y una línea: no se edita nada que ya funcione.
