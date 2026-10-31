plugins {
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
    // Tests read ../scenarios; print the pacing report lines into the log.
    testLogging { showStandardStreams = true }
}

// Regenerates LaunchTable.kt from the skeleton (README, "Changing the skeleton").
tasks.register<JavaExec>("solveTable") {
    group = "singularity"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.fanukas.singularity.engine.SolveTable")
    workingDir = rootDir
}
