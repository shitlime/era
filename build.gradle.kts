plugins {
	java
	id("org.springframework.boot") version "3.2.5"
	id("io.spring.dependency-management") version "1.1.4"
}

group = "com.shitlime"
version = "0.0.3-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_21
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.3")
	// bot框架
	implementation("com.mikuac:shiro:2.2.5")
	// sqlite数据库
	implementation("org.xerial:sqlite-jdbc:3.45.3.0")
	// yaml
	implementation("org.yaml:snakeyaml:2.2")
	// rome
	implementation("com.rometools:rome:2.1.0")
	// jsoup
	implementation("org.jsoup:jsoup:1.17.2")
	// playwright
	implementation("com.microsoft.playwright:playwright:1.41.2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Delete>("cleanRuntimeLibraries") {
	delete(layout.buildDirectory.dir("libs/lib"))
}

tasks.register<Copy>("copyRuntimeLibraries") {
	from(configurations.compileClasspath)
	into(layout.buildDirectory.dir("libs/lib"))
	dependsOn("cleanRuntimeLibraries")
}

tasks.bootJar {
	exclude("*.jar")
	dependsOn("cleanRuntimeLibraries", "copyRuntimeLibraries")

	manifest {
		attributes(
			"Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "lib/${it.name}" }
		)
	}
}
