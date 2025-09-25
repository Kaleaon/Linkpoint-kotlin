pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Linkpoint-Android"
include(":app")

// Include our core viewer modules
include(":core")
project(":core").projectDir = file("../core")

include(":protocol")
project(":protocol").projectDir = file("../protocol")

include(":graphics")
project(":graphics").projectDir = file("../graphics")

include(":ui")
project(":ui").projectDir = file("../ui")

include(":audio")
project(":audio").projectDir = file("../audio")

include(":assets")
project(":assets").projectDir = file("../assets")