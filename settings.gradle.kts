plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "PrintScript"

include(":app")
include(":ast")
include(":cli")
include(":common")
include(":formatter")
include(":interpreter")
include(":language")
include(":lexer")
include(":linter")
include(":parser")
include(":pipeline")
include(":report")
include(":token")
