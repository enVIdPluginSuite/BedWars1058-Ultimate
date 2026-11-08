import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.2.21"
    id("com.gradleup.shadow") version "9.0.0"
}

group = "dev.etran"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // Oldest supported API: anything that compiles here exists on 1.8.8. Newer-only
    // features are reached through the compat layer (reflection).
    compileOnly("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
    // Spigot 1.8.8's POM points at a dead bungeecord-chat snapshot, so its real
    // compile-time dependencies are listed here by hand.
    compileOnly("net.md-5:bungeecord-chat:1.16-R0.4")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("com.google.code.gson:gson:2.2.4")
    compileOnly("org.yaml:snakeyaml:1.15")
    compileOnly("commons-lang:commons-lang:2.6")
    compileOnly(files("libs/bedwars-plugin-23.3.jar"))
    implementation(kotlin("stdlib"))

    testImplementation(kotlin("test"))
    testImplementation("org.spigotmc:spigot-api:1.8.8-R0.1-SNAPSHOT") { isTransitive = false }
    testImplementation("net.md-5:bungeecord-chat:1.16-R0.4")
    testImplementation("com.google.guava:guava:17.0")
    testImplementation("com.google.code.gson:gson:2.2.4")
    testImplementation("org.yaml:snakeyaml:1.15")
    testImplementation("commons-lang:commons-lang:2.6")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    filter { excludeTestsMatching("*ApiCompat*") } // needs the API jars below; run it with `gradle compatTest`
}

// Minecraft versions the plugin is verified against (Spigot API jars; Paper adds nothing the plugin relies on).
val compatVersions = listOf(
    "1.8.8", "1.9.4", "1.10.2", "1.12.2", "1.13.2", "1.14.4", "1.16.5", "1.17.1", "1.18.2", "1.19.4",
    "1.20.4", "1.20.6", "1.21.1", "1.21.4", "1.21.8", "1.21.11", "26.1.2", "26.2", "26.3"
)

val compatTest by tasks.registering(Test::class) {
    group = "verification"
    description = "Checks the compiled plugin against the Bukkit API of every version in compatVersions."
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    filter { includeTestsMatching("*ApiCompat*") }
    testLogging { showStandardStreams = true }
    dependsOn(tasks.classes, tasks.testClasses)
    doFirst {
        val jars = compatVersions.map { version ->
            val cfg = configurations.detachedConfiguration(dependencies.create("org.spigotmc:spigot-api:$version-R0.1-SNAPSHOT"))
            cfg.isTransitive = false
            "$version=${cfg.resolve().single().absolutePath}"
        }
        systemProperty("compat.jars", jars.joinToString(File.pathSeparator))
        systemProperty("compat.classes", sourceSets.main.get().output.classesDirs.asPath)
        systemProperty("compat.testClasses", sourceSets.test.get().output.classesDirs.asPath)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("plugin.yml") { expand(props) }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll("-Xjdk-release=8")
    }
}

tasks.shadowJar {
    archiveBaseName.set("BedWars1058-Ultimate")
    archiveClassifier.set("")

    // Relocate the Kotlin runtime so this plugin can't clash with any other Kotlin-based plugin on the server
    // (each plugin ships its own stdlib copy; unrelocated copies of different versions break each other).
    relocate("kotlin", "dev.bw1058.ultimate.libs.kotlin")
    relocate("org.jetbrains", "dev.bw1058.ultimate.libs.jetbrains")
    relocate("org.intellij", "dev.bw1058.ultimate.libs.intellij")

    exclude("META-INF/*.kotlin_module", "META-INF/maven/**", "META-INF/versions/**", "META-INF/proguard/**", "DebugProbesKt.bin", "**/module-info.class")
}

tasks.jar {
    archiveClassifier.set("plain")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
