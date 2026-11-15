plugins {
    application
    java
}

val sh3dSrc = rootProject.file("vendor/sh3d/src")
val sh3dLib = rootProject.file("vendor/sh3d/lib")
val sh3dLibTest = rootProject.file("vendor/sh3d/libtest")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        java {
            srcDirs(file("src/main/java"), sh3dSrc)
            include("**/*.java")
            exclude("com/eteks/sweethome3d/applet/**")
            exclude("com/eteks/sweethome3d/model/**")
            exclude("com/eteks/sweethome3d/viewcontroller/**")
            exclude("com/eteks/sweethome3d/io/**")
            exclude("com/eteks/sweethome3d/tools/**")
        }
        resources {
            setSrcDirs(listOf(sh3dSrc))
            include("com/eteks/sweethome3d/*")
            include("com/eteks/sweethome3d/swing/**")
            include("com/eteks/sweethome3d/j3d/**")
            include("com/eteks/sweethome3d/plugin/**")
            include("com/eteks/sweethome3d/resources/**")
            exclude("**/*.java", "**/*.html")
        }
    }
}

dependencies {
    implementation(project(":sh3d-core"))

    // FlatLaf moderno com suporte a Light / Dark Mode e HiDPI
    implementation("com.formdev:flatlaf:3.5.4")
    implementation("com.formdev:flatlaf-extras:3.5.4")

    // Bibliotecas do Java3D 1.6 e componentes auxiliares (iText, Batik, Sunflow, JMF, etc.)
    implementation(fileTree(mapOf(
        "dir" to sh3dLib,
        "include" to listOf(
            "*.jar",
            "java3d-1.6/*.jar"
        )
    )))

    // JNLP runtime e compile stubs
    implementation(files(File(sh3dLibTest, "jnlp.jar")))
    compileOnly(files(File(sh3dLibTest, "AppleJavaExtensions.jar")))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "ISO-8859-1"
    options.compilerArgs.add("-nowarn")
}

application {
    mainClass.set("com.eteks.sweethome3d.SweetHome3D")
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    val nativeLibPath = rootProject.file("vendor/sh3d/lib/java3d-1.6/linux/amd64").absolutePath
    jvmArgs(
        "-Djava.library.path=$nativeLibPath",
        "-Djogamp.gluegen.UseTempJarCache=false",
        "-Dcom.eteks.sweethome3d.singleInstance=false",
        "--enable-native-access=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--add-exports=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "-Xmx1024m"
    )
}
