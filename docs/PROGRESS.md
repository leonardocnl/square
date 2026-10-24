# Progresso

## Pronto

- Fonte do SH3D 7.5 importado em `vendor/sh3d` (sem binários de `lib/`).
- `sh3d-core` compila com Gradle (`./gradlew :sh3d-core:jar`) e com
  `./scripts/verify-core.sh` (só JDK).
- Teste de ida e volta passou em modo headless: cria 2 paredes unidas e
  1 cômodo, salva `.sh3d`, reabre. As junções vêm calculadas pelo código
  original (ex.: canto em (405, -5) / (395, 5)) e a área sai correta.

## Marco 0 — pendente (fazer no Fedora)

- [ ] Conferir e atualizar versões em `gradle/libs.versions.toml`
      (Kotlin, Compose, jMonkeyEngine; não verificadas).
- [ ] `./gradlew :app:run` compilando; o esqueleto de `Main.kt` nunca foi
      compilado.
- [ ] Abrir um `.sh3d` real e ver paredes e cômodos no canvas Compose.
- [ ] Pan e zoom suaves.
- [ ] Janela 3D separada (jMonkeyEngine): paredes e pisos, PBR, sombras, SSAO,
      câmera orbital.
- [ ] Atualização das vistas pelos listeners originais do SH3D.

## Problemas conhecidos

- Nenhum ainda.
