import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.gradle.api.JavaVersion.VERSION_11
import org.jetbrains.changelog.closure
import org.jetbrains.intellij.tasks.PrepareSandboxTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask

buildscript {
    dependencies {
        classpath("org.commonmark:commonmark:0.17.2")
    }
}

plugins {
    idea
    id("org.jetbrains.intellij") version "1.5.2"
    id("org.jetbrains.changelog") version "1.1.2"
}

val pluginVersion = prop("pluginVersion")
group = "appland.appmap"
version = pluginVersion

repositories {
    mavenCentral()
}

dependencies {
    // This dependency is used for telemetry
    implementation("com.azure:azure-monitor-opentelemetry-exporter:1.0.0-beta.5")

    // http://wiremock.org, Apache 2 license
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.28.0")
}

intellij {
    version.set(prop("ideVersion"))
    downloadSources.set(true)
    updateSinceUntilBuild.set(true)
    instrumentCode.set(true)
}

changelog {
    version = pluginVersion
    path = "${project.projectDir}/CHANGELOG.md"
    header = closure { "[$version]" }
    itemPrefix = "-"
    keepUnreleasedSection = true
    unreleasedTerm = "[Unreleased]"
    groups = listOf("Changes")
}

configure<JavaPluginConvention> {
    sourceCompatibility = VERSION_11
    targetCompatibility = VERSION_11
}

tasks {
    buildSearchableOptions.get().enabled = false

    buildPlugin {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        copyPluginAssets("")
    }

    patchPluginXml {
        sinceBuild.set(prop("sinceBuild"))
        untilBuild.set(prop("untilBuild"))
        pluginDescription.set(file("${rootDir}/description.md").readText().renderMarkdown())

        changeNotes.set(provider {
            if (pluginVersion.endsWith("-SNAPSHOT")) {
                changelog.getUnreleased().toHTML()
            } else {
                changelog.get(pluginVersion).toHTML()
            }
        })
    }

    processTestResources {
        copyPluginAssets("")
    }

    withType<RunPluginVerifierTask> {
        ideVersions.set(prop("ideVersionVerifier").split(","))
    }

    withType(PrepareSandboxTask::class.java).all {
        copyPluginAssets(intellij.pluginName.get())
    }

    withType(Test::class.java).all {
        systemProperty("idea.test.execution.policy", "appland.AppLandTestExecutionPolicy")
        systemProperty("appland.testDataPath", file("src/test/data").path)
    }
}

fun AbstractCopyTask.copyPluginAssets(rootDir: String) {
    val rootPath = if (rootDir.isEmpty()) "" else "${rootDir.removeSuffix("/")}/"
    from("${project.rootDir}/appland") {
        into("${rootPath}appmap")
        include("index.html")
        include("dist/**")
    }
    from("${project.rootDir}/appland-user-milestones") {
        into("${rootPath}appland-user-milestones")
        include("welcome.html")
        include("agent.html")
        include("record.html")
        include("appmaps.html")
        include("dist/**")
    }
}

// https://github.com/commonmark/commonmark-java
fun String.renderMarkdown(): String {
    val parser = Parser.builder().build()
    val document = parser.parse(this)
    val renderer = HtmlRenderer.builder().build()
    return renderer.render(document).trim()
}

fun prop(name: String): String {
    return extra.properties[name] as? String ?: error("Property `$name` is not defined in gradle.properties")
}