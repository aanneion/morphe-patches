import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.net.URI
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("jvm") version "2.4.21" }
group = "local.capcut.experimental"
version = "0.2.0-experimental"
repositories { mavenLocal(); mavenCentral(); google(); maven("https://jitpack.io") }
val d8 by configurations.creating

dependencies {
    implementation("app.morphe:morphe-patcher:2.0.1")
    implementation("com.github.MorpheApp.smali:smali:ac700e1942")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("com.google.code.gson:gson:2.14.0")
    d8("com.android.tools:r8:9.1.57")
}
kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_11); freeCompilerArgs.add("-Xcontext-parameters") } }
java { sourceCompatibility = JavaVersion.VERSION_11; targetCompatibility = JavaVersion.VERSION_11 }

tasks.jar {
    exclude("util/**")
    archiveExtension.set("mpp")
    manifest {
        attributes(mapOf("Name" to "CapCut Combined Experimental", "Description" to "CapCut 19.9.0 combined community patches plus experimental interceptor candidate", "Version" to project.version.toString(), "Patcher-Version" to "2.0.1", "Author" to "kveld9 contributors; local experimental modifications", "License" to "GPL-3.0", "Source" to "https://github.com/kveld9/kveld-extra-morphe-patches"))
    }
    from("LICENSE"); from("NOTICE")
}
val dexBundle by tasks.registering(JavaExec::class) {
    dependsOn(tasks.jar)
    classpath = d8
    mainClass.set("com.android.tools.r8.D8")
    maxHeapSize = "512m"
    doFirst {
        val output = layout.buildDirectory.dir("dex").get().asFile
        output.mkdirs()
        args("--release", "--min-api", "26", "--output", output.absolutePath)
        configurations.runtimeClasspath.get().files.forEach { args("--classpath", it.absolutePath) }
        val inputJar = layout.buildDirectory.file("d8-input.jar").get().asFile
        tasks.jar.get().archiveFile.get().asFile.copyTo(inputJar, overwrite = true)
        args(inputJar.absolutePath)
    }
}
tasks.register("buildAndroid") {
    dependsOn(dexBundle)
    doLast {
        val bundle = tasks.jar.get().archiveFile.get().asFile
        FileSystems.newFileSystem(URI.create("jar:" + bundle.toURI()), mapOf("create" to "false")).use { fs ->
            layout.buildDirectory.dir("dex").get().asFile.listFiles()!!.filter { it.extension == "dex" }.forEach {
                Files.copy(it.toPath(), fs.getPath("/" + it.name), StandardCopyOption.REPLACE_EXISTING)
            }
        }
    }
}
tasks.register("writeClasspath") {
    dependsOn(tasks.classes)
    doLast { file("build/runtime-classpath.txt").writeText(sourceSets.main.get().runtimeClasspath.asPath) }
}
