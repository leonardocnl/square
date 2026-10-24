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
  `com.eteks.sweethome3d.viewcontroller.ViewFactory` (uma `SquareViewFactory`; no 7.5 `ViewFactory` é uma interface).
  Os controllers originais ficam intactos.
- Stack: Kotlin, Compose Multiplatform Desktop (interface e planta 2D),
  jMonkeyEngine 3 / LWJGL3 (3D), Gradle com Kotlin DSL, JDK 25.
- **Interface sem Material** (nem 2 nem 3): o Square terá componentes próprios
  no estilo Braun, sobre `compose.foundation`. Não crie design system sem
  decisão do Leonardo.
- **Diferença intencional em relação ao SH3D** (planta): a roda sozinha dá zoom
  centrado no cursor (10% por clique, constante `ZOOM_STEP`); pan com botão do
  meio ou espaço + arraste; F ou Ctrl+0 enquadram o projeto. No SH3D original a
  roda rola e Ctrl+roda dá zoom de 5%.
- **Diferenças intencionais na vista 3D:** V alterna vista aérea/visitante e Q/E
  giram (extras). Os atalhos do SH3D prevalecem em conflito: A/D giram (não
  deslizam) e as setas esquerda/direita giram (não andam); Alt + seta desliza.
  Valores e teclas vêm de `swing/HomeComponent3D.java` (ver `Home3DInput`).
- Licença: GPL v2 ou posterior (herdada do SH3D). Preserve os avisos originais.
- Ambiente principal: Fedora Linux.

## Estilo

- Indentação de **4 espaços** em todo código (Kotlin, Java, Gradle, scripts).
- Nas respostas: explique termos técnicos e deixe as suposições explícitas.
- Decisões: pergunte ao Leonardo as de gosto, prioridade ou produto; as técnicas
  com resposta clara, resolva e explique.
- Código minimalista: antes de criar algo, verifique se o SH3D já tem.

## Comandos

- Verificar o núcleo sem Gradle e sem internet: `./scripts/verify-core.sh`
- Compilar o núcleo: `./gradlew :sh3d-core:jar`
- Rodar o app: `./gradlew :app:run`
- **Testes e medições de 3D (jME/Skia) SEMPRE por `./scripts/run-limited.sh <tarefas gradle>`**:
  limite de 3 GB sem swap (`systemd-run --user --scope`), sem daemon e com timeout (padrão 300 s;
  `MEM_MAX`, `TIMEOUT` ajustáveis). A memória nativa do Skia/jME não respeita `-Xmx`; sem o limite,
  duas medições esgotaram a RAM e travaram o computador. Medições: avisar o Leonardo para fechar
  navegador/IDE e esperar o ok antes de rodar; sempre com timeout, fechando as janelas ao terminar.

- Arquivos `.sh3d` e `samples/` não vão para o git. Gerar o de teste:
  `samples/teste-simples.sh3d` via `scripts/CoreSmoke.java`.

## Detalhes que já custaram tempo

- Os fontes do SH3D estão em **ISO-8859-1**, não UTF-8 (já configurado no Gradle).
- O núcleo precisa dos `.properties` dele no classpath (textos traduzidos).
  Sem eles, `OperatingSystem` falha ao iniciar.
- `tools/OperatingSystem.java` importa `com.apple.eio`; ele é compilado com
  `vendor/sh3d/lib-compile/AppleJavaExtensions.jar` como `compileOnly`.
- **GPU/OpenGL (Fedora do Leonardo):** Intel Iris Xe (Raptor Lake-P), Mesa 26.2,
  OpenGL 4.6 core com aceleração; sessão Wayland (AWT roda via XWayland).
  Testes com jME usam o contexto offscreen do LWJGL3 direto na GPU; não é
  preciso `LIBGL_ALWAYS_SOFTWARE`. Se uma máquina sem GPU rodar os testes,
  use Mesa llvmpipe (`LIBGL_ALWAYS_SOFTWARE=1`). `glxinfo` vem de `glx-utils`.
- **jME e Compose convivem** no mesmo processo (testado em `JmeSmokeTest`:
  janela Compose aberta + jME offscreen + jME em janela). Em testes, use
  `application(exitProcessOnExit = false)`, senão o Compose encerra a JVM do teste.
- **jME no Wayland:** o GLFW roda nativo em Wayland e **não informa a posição da janela**
  (log `SEVERE: Wayland: The platform does not provide the window position`); a janela
  3D destacada só guarda tamanho nesse caso (`Square3DApp.destroy`).
- **Unidades do jME = cm** (como o SH3D): os parâmetros padrão do `SSAOFilter` são para
  metros; `scale` precisa ser ~0,02 (com 30 o efeito some).
- Para render offscreen do jME: `FrameBuffer` próprio como saída da `ViewPort` +
  `SceneProcessor.postFrame` lendo com `renderer.readFrameBuffer` (veja
  `app/src/test/.../JmeSmokeTest.kt`); a leitura vem de baixo para cima.

## Documentos vivos (mantenha atualizados)

- `docs/ARCHITECTURE.md`: como Square, ViewFactory e controllers se ligam
- `docs/PROGRESS.md`: o que está pronto, o que falta, problemas conhecidos
- `docs/VIEWFACTORY_COVERAGE.md`: status de cada método da ViewFactory
