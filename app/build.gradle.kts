import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":sh3d-core"))
    implementation(compose.desktop.currentOs)
    implementation(libs.jme.core)
    implementation(libs.jme.desktop)
    implementation(libs.jme.lwjgl3)
    implementation(libs.jme.effects)

    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "br.com.square.MainKt"
        jvmArgs += "--enable-native-access=ALL-UNNAMED"
        nativeDistributions {
            targetFormats(TargetFormat.Rpm, TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "Square"
            packageVersion = "0.1.0"
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    // Caminhos relativos em --args valem a partir da raiz do repositório.
    workingDir = rootDir
}

tasks.test {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    workingDir = rootDir
}
