plugins {
    kotlin("jvm") version "1.9.21"
}

group = "net.marvk"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    implementation("org.jetbrains.lets-plot", "lets-plot-common", "2.5.1")
    implementation("org.jetbrains.lets-plot", "lets-plot-kotlin-jvm", "4.1.1")
    implementation("org.jetbrains.lets-plot", "lets-plot-image-export", "2.5.1")
    implementation("com.google.code.gson", "gson", "2.10.1")

}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(18)
}
