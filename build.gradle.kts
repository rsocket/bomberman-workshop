plugins {
    java
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.corundumstudio.socketio:netty-socketio:1.7.18")
    implementation("javax.activation:javax.activation-api:1.2.0")
    implementation("com.sun.activation:javax.activation:1.2.0")
    implementation("org.slf4j:slf4j-simple:1.7.30")

    testImplementation("junit", "junit", "4.12")
}
