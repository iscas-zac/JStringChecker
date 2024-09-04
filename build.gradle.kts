plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}


dependencies {
    implementation("org.soot-oss:soot:4.5.0")
    implementation("net.amygdalum:regexparser:0.2.5")
}