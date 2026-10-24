#!/usr/bin/env bash
# Compila o núcleo do SH3D (model, viewcontroller, io, tools) só com o JDK e
# roda um teste de ida e volta: cria paredes e um cômodo, salva um .sh3d,
# reabre e imprime o resultado. Não depende de Gradle nem de internet.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
out="$(mktemp -d)"
src="$root/vendor/sh3d/src"

cd "$src"
find com/eteks/sweethome3d/{model,viewcontroller,io,tools} -name '*.java' > "$out/files.txt"
javac --release 21 -nowarn -encoding ISO-8859-1 \
    -cp "$root/vendor/sh3d/lib-compile/AppleJavaExtensions.jar" \
    -d "$out/classes" @"$out/files.txt"
find com/eteks/sweethome3d/{model,viewcontroller,io,tools} -type f \
    ! -name '*.java' ! -name '*.html' -exec cp --parents {} "$out/classes/" \;

javac -cp "$out/classes" -d "$out/smoke" "$root/scripts/CoreSmoke.java"
java -Djava.awt.headless=true -cp "$out/classes:$out/smoke" CoreSmoke "$out/test.sh3d"
echo "OK — núcleo compilado em $out/classes"
