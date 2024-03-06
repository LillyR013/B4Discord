plugins {
	java
	id("org.springframework.boot") version "3.2.3"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "com.b4discord"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.session:spring-session-core")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	implementation("net.dv8tion:JDA:5.0.0-beta.20")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
