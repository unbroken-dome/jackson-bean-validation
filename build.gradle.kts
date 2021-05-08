plugins {
    `java-library`
    kotlin("jvm") version "1.5.0"
    `maven-publish`
    id("org.unbroken-dome.test-sets") version "4.0.0"
}

repositories {
    mavenCentral()
}

testSets {
    create("javaOnlyTest") {
        dirName = "java-only-test"
    }
}


val jacksonVersion = "2.10.0"


configurations {
    "javaOnlyTestImplementation" {
        setExtendsFrom(extendsFrom - setOf(testImplementation.get()))
        extendsFrom(implementation.get())
    }
}
tasks.named("check") {
    dependsOn("javaOnlyTest")
}


dependencies {
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("javax.validation:validation-api:2.0.1.Final")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:6.0.13.Final")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.3.2")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")

    "javaOnlyTestImplementation"("org.junit.jupiter:junit-jupiter-api:5.3.2")
    "javaOnlyTestImplementation"("org.hibernate.validator:hibernate-validator:6.0.13.Final")
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=enable")
    }
}


tasks.withType<Test> {
    useJUnitPlatform()
}

val jacksonCompatVersions = mapOf(
    "2.9" to (0..10), "2.10" to (0..5), "2.11" to (0..4), "2.12" to (0..3)
).flatMap { (majorMinor, patchVersions) -> patchVersions.map { "$majorMinor.$it" } }

for (compatVersion in jacksonCompatVersions) {
    val testConfiguration: Configuration = project.configurations.create("jacksonCompatTest_$compatVersion")
    testConfiguration.extendsFrom(configurations["testRuntimeClasspath"])
    testConfiguration.resolutionStrategy.eachDependency {
        if (requested.group.startsWith("com.fasterxml.jackson.") && requested.name != "jackson-annotations") {
            useVersion(compatVersion)
        }
    }

    val testTask = project.tasks.register("jacksonCompatTest_$compatVersion", Test::class) {
        group = JavaBasePlugin.VERIFICATION_GROUP
        shouldRunAfter("test")
        classpath = sourceSets["test"].output + sourceSets["main"].output + testConfiguration
        exclude("**/KotlinValidationTest.class")
    }

    tasks.named("check") {
        dependsOn(testTask)
    }
}


val sourcesJar by tasks.creating(Jar::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Assembles a JAR archive containing the sources."
    from(sourceSets["main"].allSource)
    archiveClassifier.set("sources")
}

val javadocJar by tasks.creating(Jar::class) {
    group = BasePlugin.BUILD_GROUP
    description = "Assembles a JAR archive containing the Javadocs."
    from(tasks.named("javadoc"))
    archiveClassifier.set("javadoc")
}


publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
}
