plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "2.2.10"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "pl.marcinwieczorek"
version = "0.1.0"

springBoot {
    mainClass.set("pl.marcinwieczorek.investmentmonitor.InvestmentMonitorApplicationKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jsoup:jsoup:1.21.1")
    implementation("com.microsoft.playwright:playwright:1.52.0")
    implementation("org.xerial:sqlite-jdbc:3.50.3.0")
    implementation("org.flywaydb:flyway-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-runner-junit5:6.0.3")
    testImplementation("io.kotest:kotest-assertions-core:6.0.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
    }
}

tasks.register<JavaExec>("verifySources") {
    group = "verification"
    description = "Runs live developer source verification without updating trusted snapshots."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("pl.marcinwieczorek.investmentmonitor.tools.SourceVerificationCliKt")
}

tasks.register<JavaExec>("captureFixtures") {
    group = "verification"
    description = "Captures configured developer pages as reviewed parser fixtures."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("pl.marcinwieczorek.investmentmonitor.tools.FixtureCaptureCliKt")
}
