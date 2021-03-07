plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("io.rsocket:rsocket-core:1.1.0")
    implementation("io.rsocket:rsocket-transport-netty:1.1.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.2")

    testImplementation("junit", "junit", "4.12")
}
