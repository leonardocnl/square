# Square — regras do projeto

Square é uma modernização desktop do Sweet Home 3D (SH3D) 7.5: mesma lógica e
mesmas funcionalidades, interface e 3D novos, mais rápidos e mais bonitos.

## Decisões fixas (não reabrir sem perguntar ao Leonardo)

- **A lógica do SH3D não é traduzida nem reescrita.** Os pacotes
  `com.eteks.sweethome3d.model`, `.viewcontroller`, `.io` e `.tools` são
  compilados como estão, a partir de `vendor/sh3d/src`, no módulo `sh3d-core`.
- **`vendor/sh3d` é somente leitura.** Nunca edite esse código. Se parecer
  necessário, pare e explique o motivo e as alternativas.
- Os pacotes `swing` e `j3d` do SH3D **não entram no app**; servem só de
  referência de comportamento.
- A interface nova implementa a classe abstrata
  `com.eteks.sweethome3d.viewcontroller.ViewFactory` (uma `SquareViewFactory`).
  Os controllers originais ficam intactos.
- Stack: Kotlin, Compose Multiplatform Desktop (interface e planta 2D),
  jMonkeyEngine 3 / LWJGL3 (3D), Gradle com Kotlin DSL, JDK 21.
- Licença: GPL v2 ou posterior (herdada do SH3D). Preserve os avisos originais.
- Ambiente principal: Fedora Linux.

## Estilo

- Indentação de **4 espaços** em todo código (Kotlin, Java, Gradle, scripts).
- Nas respostas: explique termos técnicos e deixe as suposições explícitas.
- Código minimalista: antes de criar algo, verifique se o SH3D já tem.

## Comandos

- Verificar o núcleo sem Gradle e sem internet: `./scripts/verify-core.sh`
- Compilar o núcleo: `./gradlew :sh3d-core:jar`
- Rodar o app: `./gradlew :app:run`

## Detalhes que já custaram tempo

- Os fontes do SH3D estão em **ISO-8859-1**, não UTF-8 (já configurado no Gradle).
- O núcleo precisa dos `.properties` dele no classpath (textos traduzidos).
  Sem eles, `OperatingSystem` falha ao iniciar.
- `tools/OperatingSystem.java` importa `com.apple.eio`; ele é compilado com
  `vendor/sh3d/lib-compile/AppleJavaExtensions.jar` como `compileOnly`.

## Documentos vivos (mantenha atualizados)

- `docs/ARCHITECTURE.md`: como Square, ViewFactory e controllers se ligam
- `docs/PROGRESS.md`: o que está pronto, o que falta, problemas conhecidos
- `docs/VIEWFACTORY_COVERAGE.md`: status de cada método da ViewFactory
