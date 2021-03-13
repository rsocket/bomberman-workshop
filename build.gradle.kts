plugins {
    id("org.springframework.boot").version("2.4.2")
    id("io.spring.dependency-management").version("1.0.11.RELEASE")
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-rsocket")

    // implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("io.rsocket:rsocket-core:1.1.0")
    implementation("io.rsocket:rsocket-transport-netty:1.1.0")

    testImplementation("junit", "junit", "4.12")
}
