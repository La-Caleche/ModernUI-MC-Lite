plugins {
    id("fabric-loom")
    `java-library`
    `maven-publish`
}

version = providers.gradleProperty("modVersion").get()
group = providers.gradleProperty("mavenGroup").get()

base {
    archivesName.set(providers.gradleProperty("archiveBaseName").get())
}

repositories {
    mavenCentral()
    maven("https://maven.izzel.io/releases/")
}

val testmod: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().compileClasspath
    runtimeClasspath += sourceSets.main.get().runtimeClasspath
}

loom {
    runs {
        register("testmodClient") {
            client()
            name("Testmod Client")
            source(testmod)
        }
    }
    createRemapConfigurations(testmod)
}

dependencies {
    minecraft(libs.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)

    api(libs.modernui.core) {
        exclude(group = "it.unimi.dsi", module = "fastutil")
        exclude(group = "com.google.code.findbugs", module = "jsr305")
    }
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    "testmodImplementation"(sourceSets.main.get().output)

    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(testmod.output)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
        from("LICENSES") {
            into("META-INF/licenses/modernui-mc-lite")
        }
    }

    named<ProcessResources>("processTestmodResources") {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    remapJar {
        archiveBaseName.set(providers.gradleProperty("archiveBaseName").get())
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
