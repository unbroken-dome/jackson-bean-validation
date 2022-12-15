plugins {
    `java-library`
    kotlin("jvm") version "1.5.0"
    signing
    `maven-publish`
    id("io.github.gradle-nexus.publish-plugin") version "1.1.0"
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


val jacksonVersion = "2.11.4"


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
    api("jakarta.validation:jakarta.validation-api:3.0.2")

    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    compileOnly(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))

    testImplementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("reflect"))

    testImplementation(platform("org.junit:junit-bom:5.7.1"))

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.24")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")
    testImplementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    testImplementation("org.hibernate.validator:hibernate-validator:7.0.2.Final")

    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.2.3")

    "javaOnlyTestImplementation"("org.junit.jupiter:junit-jupiter-api")
    "javaOnlyTestImplementation"("org.hibernate.validator:hibernate-validator:7.0.2.Final")
}


java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
    withJavadocJar()
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



publishing {
    publications {
        create("mavenJava", MavenPublication::class) {
            from(components["java"])
            pom {
                val githubRepo = providers.gradleProperty("githubRepo")
                val githubUrl = githubRepo.map { "https://github.com/$it" }

                name.set(providers.gradleProperty("projectName"))
                description.set(providers.gradleProperty("projectDescription"))
                url.set(providers.gradleProperty("projectUrl"))
                licenses {
                    license {
                        name.set(providers.gradleProperty("projectLicenseName"))
                        url.set(providers.gradleProperty("projectLicenseUrl"))
                    }
                }
                developers {
                    developer {
                        name.set(providers.gradleProperty("developerName"))
                        email.set(providers.gradleProperty("developerEmail"))
                        url.set(providers.gradleProperty("developerUrl"))
                    }
                }
                scm {
                    url.set(githubUrl.map { "$it/tree/master" })
                    connection.set(githubRepo.map { "scm:git:git://github.com/$it.git" })
                    developerConnection.set(githubRepo.map { "scm:git:ssh://github.com:$it.git" })
                }
                issueManagement {
                    url.set(githubUrl.map { "$it/issues" })
                    system.set("GitHub")
                }
            }
        }
    }

    repositories {
        maven {
            name = "local"
            url = uri("$buildDir/repos/releases")
        }
    }
}


signing {
    sign(publishing.publications["mavenJava"])
}


nexusPublishing {
    repositories {
        sonatype()
    }
}
