#!/usr/bin/env bash
# Roda o Gradle (app, testes, medições de 3D) dentro de um limite de memória do sistema.
# A memória nativa do Skia/jME não respeita -Xmx; sem este limite, uma medição já esgotou a
# RAM da máquina duas vezes. Estourar o limite mata só este grupo, não o resto do desktop.
#
# Uso:    ./scripts/run-limited.sh :app:test --tests '*Home3DTest*'
# Opções (variáveis de ambiente): MEM_MAX=3G  TIMEOUT=300 (segundos)
set -euo pipefail

cd "$(dirname "$0")/.."
# --no-daemon: o daemon do Gradle fica fora do grupo e escaparia do limite.
exec timeout --foreground "${TIMEOUT:-300}" \
    systemd-run --user --scope --quiet \
    -p MemoryMax="${MEM_MAX:-3G}" -p MemorySwapMax=0 \
    ./gradlew --no-daemon "$@"
