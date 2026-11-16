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

    testImplementation("junit:junit:4.13.2")
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

tasks.named<Test>("test") {
    systemProperty("java.awt.headless", "true")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes["Main-Class"] = "com.eteks.sweethome3d.SweetHome3D"
    }
}

val jpackageInputDir = layout.buildDirectory.dir("jpackage/input")
val distDir = layout.buildDirectory.dir("dist")

val prepareJPackageInput = tasks.register<Copy>("prepareJPackageInput") {
    dependsOn(":sh3d-core:jar", "jar")
    into(jpackageInputDir)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Jars principais
    from(tasks.named("jar"))
    from(project(":sh3d-core").tasks.named("jar"))

    // Dependencias de runtime
    from(configurations.named("runtimeClasspath").map { config ->
        config.filter { it.extension == "jar" && it.name != "app.jar" && it.name != "sh3d-core.jar" }
    })

    // Bibliotecas nativas do Java3D da plataforma
    val osName = System.getProperty("os.name").lowercase()
    if (osName.contains("linux")) {
        from(rootProject.file("vendor/sh3d/lib/java3d-1.6/linux/amd64")) {
            include("*.so")
        }
    } else if (osName.contains("win")) {
        from(rootProject.file("vendor/sh3d/lib/java3d-1.6/windows/amd64")) {
            include("*.dll")
        }
    } else if (osName.contains("mac")) {
        from(rootProject.file("vendor/sh3d/lib/java3d-1.6/macosx")) {
            include("*.dylib", "*.jnilib")
        }
    }
}

val packageAppImage = tasks.register<Exec>("packageAppImage") {
    dependsOn(prepareJPackageInput)
    val inputDir = jpackageInputDir.get().asFile
    val outputDir = distDir.get().asFile
    val squareDir = File(outputDir, "square")

    doFirst {
        if (squareDir.exists()) {
            squareDir.deleteRecursively()
        }
        outputDir.mkdirs()
    }

    executable = "jpackage"
    val javaHome = System.getProperty("java.home")
    val jpackageArgs = mutableListOf(
        "--type", "app-image",
        "--name", "square",
        "--app-version", "1.0.0",
        "--input", inputDir.absolutePath,
        "--dest", outputDir.absolutePath,
        "--main-jar", "app.jar",
        "--main-class", "com.eteks.sweethome3d.SweetHome3D",
        "--icon", rootProject.file("vendor/sh3d/src/com/eteks/sweethome3d/resources/frameIcon128x128.png").absolutePath,
        "--java-options", "-Djava.library.path=\$APPDIR",
        "--java-options", "-Djogamp.gluegen.UseTempJarCache=false",
        "--java-options", "-Dcom.eteks.sweethome3d.singleInstance=false",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "-Xmx1024m"
    )
    if (javaHome != null && File(javaHome).exists()) {
        jpackageArgs.addAll(listOf("--runtime-image", javaHome))
    }
    args(jpackageArgs)
}

val packageArchive = tasks.register<Tar>("packageArchive") {
    dependsOn(packageAppImage)
    archiveFileName.set("square-1.0.0-linux-x64.tar.gz")
    destinationDirectory.set(distDir)
    compression = Compression.GZIP

    from(distDir.get().dir("square")) {
        into("square")
    }
}

val packageRpm = tasks.register<Exec>("packageRpm") {
    dependsOn(prepareJPackageInput)
    val inputDir = jpackageInputDir.get().asFile
    val outputDir = distDir.get().asFile

    doFirst {
        outputDir.mkdirs()
    }

    executable = "jpackage"
    val javaHome = System.getProperty("java.home")
    val jpackageArgs = mutableListOf(
        "--type", "rpm",
        "--name", "square",
        "--app-version", "1.0.0",
        "--input", inputDir.absolutePath,
        "--dest", outputDir.absolutePath,
        "--main-jar", "app.jar",
        "--main-class", "com.eteks.sweethome3d.SweetHome3D",
        "--icon", rootProject.file("vendor/sh3d/src/com/eteks/sweethome3d/resources/frameIcon128x128.png").absolutePath,
        "--linux-package-name", "square",
        "--linux-app-category", "Graphics",
        "--linux-menu-group", "Graphics",
        "--linux-shortcut",
        "--java-options", "-Djava.library.path=\$APPDIR",
        "--java-options", "-Djogamp.gluegen.UseTempJarCache=false",
        "--java-options", "-Dcom.eteks.sweethome3d.singleInstance=false",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "-Xmx1024m"
    )
    if (javaHome != null && File(javaHome).exists()) {
        jpackageArgs.addAll(listOf("--runtime-image", javaHome))
    }
    args(jpackageArgs)
}

val packageDeb = tasks.register<Exec>("packageDeb") {
    dependsOn(prepareJPackageInput)
    val inputDir = jpackageInputDir.get().asFile
    val outputDir = distDir.get().asFile

    doFirst {
        outputDir.mkdirs()
    }

    executable = "jpackage"
    val javaHome = System.getProperty("java.home")
    val jpackageArgs = mutableListOf(
        "--type", "deb",
        "--name", "square",
        "--app-version", "1.0.0",
        "--input", inputDir.absolutePath,
        "--dest", outputDir.absolutePath,
        "--main-jar", "app.jar",
        "--main-class", "com.eteks.sweethome3d.SweetHome3D",
        "--icon", rootProject.file("vendor/sh3d/src/com/eteks/sweethome3d/resources/frameIcon128x128.png").absolutePath,
        "--linux-package-name", "square",
        "--linux-app-category", "graphics",
        "--linux-menu-group", "Graphics",
        "--linux-shortcut",
        "--java-options", "-Djava.library.path=\$APPDIR",
        "--java-options", "-Djogamp.gluegen.UseTempJarCache=false",
        "--java-options", "-Dcom.eteks.sweethome3d.singleInstance=false",
        "--java-options", "--enable-native-access=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/java.awt=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/javax.swing=ALL-UNNAMED",
        "--java-options", "--add-opens=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.awt.X11=ALL-UNNAMED",
        "--java-options", "--add-exports=java.desktop/sun.java2d.opengl=ALL-UNNAMED",
        "--java-options", "-Xmx1024m"
    )
    if (javaHome != null && File(javaHome).exists()) {
        jpackageArgs.addAll(listOf("--runtime-image", javaHome))
    }
    args(jpackageArgs)
}
