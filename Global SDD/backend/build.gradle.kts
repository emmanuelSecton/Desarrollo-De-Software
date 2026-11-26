plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.5.16"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "ar.edu.utn"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-security")
	// Rate limiting por IP (C-03, D10). Caffeine (version del BOM) acota el mapa de buckets.
	implementation("com.bucket4j:bucket4j_jdk17-core:8.14.0")
	implementation("com.github.ben-manes.caffeine:caffeine")
	implementation("org.flywaydb:flyway-core")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:postgresql")
	testImplementation("org.testcontainers:junit-jupiter")
	// El BOM de Spring Boot no gestiona ArchUnit: unica version explicita (D4).
	testImplementation("com.tngtech.archunit:archunit-junit5:1.4.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
	// La JVM corre en UTC para que la sesion de base quede en UTC (D7, RN-GL-02).
	jvmArgs("-Duser.timezone=UTC")
}

// Una sola imagen: el jar ejecutable tiene nombre fijo y no se genera el jar "plain".
tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	archiveFileName = "app.jar"
}

tasks.named<Jar>("jar") {
	enabled = false
}

// Cobertura: reporte siempre y umbral de 80 % de lineas sobre src/main (D14).
// OdontoApplication (solo main()) no aporta logica testeable y se excluye explicitamente.
val exclusionesDeCobertura = listOf("ar/edu/utn/odonto/OdontoApplication*")

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	classDirectories.setFrom(files(classDirectories.files.map {
		fileTree(it) { exclude(exclusionesDeCobertura) }
	}))
	reports {
		xml.required = true
		html.required = true
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(files(classDirectories.files.map {
		fileTree(it) { exclude(exclusionesDeCobertura) }
	}))
	violationRules {
		rule {
			limit {
				counter = "LINE"
				value = "COVEREDRATIO"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}

tasks.test {
	finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
	dependsOn(tasks.jacocoTestCoverageVerification)
}
