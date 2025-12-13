plugins {
    kotlin("jvm") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.sourceforge.javacsv:javacsv:2.0")
    implementation("junit:junit:4.13.1")
    implementation("com.github.curious-odd-man:rgxgen:1.4")
    implementation("org.hamcrest:hamcrest-core:1.3")
    implementation("com.google.guava:guava:33.5.0-jre")
    implementation("com.alibaba:fastjson:1.2.83")
    implementation("commons-cli:commons-cli:1.4")
    implementation("org.xerial:sqlite-jdbc:3.41.2.2")
    implementation("org.soot-oss:soot:4.6.0")
    implementation("net.amygdalum:regexparser:0.2.5")
}