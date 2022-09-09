import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.report.ReportMergeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.net.URI
import java.time.LocalDateTime

val artifactoryUrl: String? by project
val artifactoryRepository: String? by project
val artifactoryUsername: String? by project
val artifactoryApiKey: String? by project

plugins {
    `maven-publish`
    kotlin("jvm") apply true
    id("com.jfrog.artifactory") version "4.28.0" apply true
    id("io.gitlab.arturbosch.detekt")
}

val reportMerge by tasks.registering(ReportMergeTask::class) {
    output.set(rootProject.buildDir.resolve("reports/detekt/exposed.xml"))
}

allprojects {
    apply(from = rootProject.file("buildScripts/gradle/checkstyle.gradle.kts"))

    if (this != rootProject && this.name != "exposed-tests") {
        apply(plugin = "maven-publish")
        apply(plugin = "com.jfrog.artifactory")

        if (this.name != "exposed-bom") {
            apply(from = rootProject.file("buildScripts/gradle/publishing.gradle.kts"))
        }
    }
}

subprojects {
    tasks.withType<Detekt>().configureEach detekt@{
        finalizedBy(reportMerge)
        reportMerge.configure {
            input.from(this@detekt.xmlReportFile)
        }
    }

    tasks.withType<KotlinJvmCompile>().configureEach {
        kotlinOptions {
            jvmTarget = "1.8"
            apiVersion = "1.6"
            languageVersion = "1.6"
        }
    }
}

artifactory {
    clientConfig.isIncludeEnvVars = true
    clientConfig.info.addEnvironmentProperty("createdAt", LocalDateTime.now().toString())

    setContextUrl(artifactoryUrl)

    publish {
        repository {
            setRepoKey(artifactoryRepository)
            setUsername(artifactoryUsername)
            setPassword(artifactoryApiKey)
        }

        defaults {
            publications("ExposedJars", "bom")
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = URI("$artifactoryUrl/$artifactoryRepository")
        credentials {
            this.username = artifactoryUsername
            this.password = artifactoryApiKey
        }
    }
}
