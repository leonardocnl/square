// Compila, SEM MODIFICAR, os pacotes de lógica do Sweet Home 3D 7.5 que ficam
// em vendor/sh3d/src. Os pacotes swing e j3d (interface e 3D antigos) ficam de
// fora de propósito: o Square os substitui.
plugins {
    `java-library`
}

val sh3dSrc = rootProject.file("vendor/sh3d/src")
val corePackages = listOf("model", "viewcontroller", "io", "tools")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf(sh3dSrc))
            corePackages.forEach { include("com/eteks/sweethome3d/$it/**/*.java") }
        }
        resources {
            // Textos traduzidos (.properties), ícones e modelos usados pelos controllers.
            setSrcDirs(listOf(sh3dSrc))
            corePackages.forEach { include("com/eteks/sweethome3d/$it/**") }
            exclude("**/*.java", "**/*.html")
        }
    }
}

dependencies {
    // Só para compilar OperatingSystem.java (código específico de macOS).
    // Não vai para o runtime.
    compileOnly(files(rootProject.file("vendor/sh3d/lib-compile/AppleJavaExtensions.jar")))
}

tasks.withType<JavaCompile>().configureEach {
    // Os fontes do SH3D estão em ISO-8859-1 (Latin-1), não UTF-8.
    options.encoding = "ISO-8859-1"
    // O código é antigo e gera muitos avisos (deprecações, raw types);
    // não são problemas nossos.
    options.compilerArgs.add("-nowarn")
}
