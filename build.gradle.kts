plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21" apply false
}
val buildRoot = providers.environmentVariable("DOLMIS_BUILD_ROOT").orNull?.let { file(it) } ?: rootDir.resolve(".build")
layout.buildDirectory.set(buildRoot.resolve("root"))
subprojects { layout.buildDirectory.set(buildRoot.resolve(name)) }
