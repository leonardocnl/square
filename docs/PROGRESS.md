# Progresso

## Pronto

- Fonte do SH3D 7.5 importado em `vendor/sh3d` (sem binários de `lib/`).
- `sh3d-core` compila com Gradle (`./gradlew :sh3d-core:jar`) e com
  `./scripts/verify-core.sh` (só JDK).
- Teste de ida e volta passou em modo headless: cria 2 paredes unidas e
  1 cômodo, salva `.sh3d`, reabre. As junções vêm calculadas pelo código
  original (ex.: canto em (405, -5) / (395, 5)) e a área sai correta.

- JDK 25 (antes 21), Gradle 9.8.2, Kotlin 2.4.21, Compose 1.12.2, jME 3.9.0-stable.
  Por quê: só há JDK 25/27 no Fedora; o 21 do CLAUDE.md não tinha motivo técnico
  e o Gradle 8.14 não roda em Java 25. `verify-core.sh` passa com `--release 25`.
- `./gradlew :app:run --args="samples/teste-simples.sh3d"` abre direto, sem
  diálogo. `./gradlew :app:test` renderiza o PlanCanvas sem janela e salva
  `app/build/screenshots/plan.png` (verificação visual padrão). O teste usa o
  `samples/teste-simples.sh3d`: gere-o antes com o CoreSmoke (veja CLAUDE.md).
- `:app:run` compila e abre a janela; botão feito só com `foundation`.

- `SquareViewFactory` + `SquarePlanView` (parcial) ligadas ao `PlanController`
  original; o zoom passa por `PlanController.zoom`/`setScale`. `:app:test`
  dirige a entrada real e salva `1-fit`, `2-zoom`, `3-pan` em
  `app/build/screenshots/`.

- `HomeChangeWatcher`: CollectionListener por coleção do Home + PropertyChangeListener
  por item; `close()` remove tudo (o Home troca a lista interna a cada add/delete,
  então os itens vigiados são rastreados pelo watcher). A view sobe um contador
  `version` lido no desenho: o Compose junta mudanças seguidas num redesenho por frame.
  Limite: ainda não observa câmera, bússola, preferências nem itens dentro de grupos de móveis.

- 3D (jME): `Geometry3D.kt` (malhas sem GL: paredes com topo inclinado/redondas e
  pisos/tetos com furos por sobreposição de cômodos, triangulados com earcut4j),
  `Square3DApp` (cena, câmera do `Home`, captura offscreen), `Home3DInput`
  (mouse/teclas → `HomeController3D` original). Testes: `Home3DTest` (PNGs
  `3d-aerea`, `3d-orbita`, `3d-modelo-alterado`) e `Home3DInputTest`.

- 3D visual provisório: PBR simples, luz direcional com sombra (3 cascatas, 8 m), ambiente e SSAO
  (`Square3DApp.addShadowsAndSsao`). Conhecido: faixa escura de ~3 px no topo da imagem (borda do SSAO);
  sem light probe, a PBR sai mais escura que o SH3D. O visual final será decidido com o Leonardo.

## Paridade com o SH3D (regra para o que vem a seguir)

Quando chegarmos a portas/janelas, rodapés e materiais/texturas por lado, a
geometria deve **seguir `vendor/sh3d/src/com/eteks/sweethome3d/j3d/Wall3D.java`**
(e `Room3D.java` para pisos e tetos), não uma lógica nova. Já alinhado com eles:
elevações (nível, espessura do piso, `LEVEL_ELEVATION_SHIFT`), topo inclinado pela
projeção no eixo da parede (vale também para paredes redondas), arestas sobre o
eixo ignoradas, cômodos posteriores do mesmo nível cobrindo os anteriores.
Ainda diferente: sem face de baixo nas paredes, sem espessura de laje entre
níveis, teto sempre em `elevação + altura do nível` (falta `getRoomHeightAt`:
teto plano/inclinado), sem furos de escada, uma cor por parede (a esquerda),
sem normais suaves em paredes redondas.

Janela destacada: o SH3D guarda `<classe da vista>.detachedView`, `.detachedViewX/Y/Width/Height`
e `.detachedViewDividerLocation` no `Home` (`HomePane`), com a chave
`com.eteks.sweethome3d.swing.HomeComponent3D`. Usar as mesmas chaves, para paridade
dos arquivos.

## Marco 0 — pendente (fazer no Fedora)

- [x] Conferir e atualizar versões em `gradle/libs.versions.toml`
      (Kotlin, Compose, jMonkeyEngine; não verificadas).
- [x] `./gradlew :app:run` compilando; o esqueleto de `Main.kt` nunca foi
      compilado.
- [x] Abrir `.sh3d` gerado pelo núcleo (CoreSmoke) no canvas Compose.
- [ ] Abrir um `.sh3d` real feito no SH3D (Leonardo vai fornecer).
- [x] Pan e zoom (roda no cursor, botão do meio/espaço, F/Ctrl+0); "suave" = escala exponencial, sem inércia/animação.
- [ ] Janela 3D separada (jMonkeyEngine): paredes e pisos, PBR, sombras, SSAO,
      câmera orbital.
- [x] Atualização da planta pelos listeners originais do SH3D (`HomeChangeWatcher`; a 3D vai reutilizá-lo).

## Problemas conhecidos

- 3D: a cena é reconstruída inteira a cada mudança do modelo (sem atualização incremental por item).
- 3D: o jME lê o modelo na própria thread enquanto a interface pode alterá-lo (listas do `Home` são copy-on-write; campos dos itens, não).
- Móveis dentro de grupos (`HomeFurnitureGroup`) não são observados pelo `HomeChangeWatcher`.
- Os listeners que o `PlanController` original registra no `Home` não têm remoção
  (a API dele não oferece); só vazam se o Home sobreviver ao controller.
- LWJGL avisa sobre `sun.misc.Unsafe` no JDK 25 (inofensivo por ora).
