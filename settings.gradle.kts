plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "WorldTasker"

include(":RoosterUI")
project(":RoosterUI").projectDir = file("../rooster-ui")

include(":RoosterCore")
project(":RoosterCore").projectDir = file("../rooster-core")

include(":RoosterLocalization")
project(":RoosterLocalization").projectDir = file("../rooster-localization")

include(":RoosterSql")
project(":RoosterSql").projectDir = file("../rooster-sql")
