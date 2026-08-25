plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "PrintScript"

include(":app")
include(":ast")
include(":cli")
include(":common")
include(":interpreter")
include(":language")
include(":lexer")
include(":parser")
include(":pipeline")
include(":report")
include(":token")