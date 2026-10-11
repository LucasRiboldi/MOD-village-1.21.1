#!/usr/bin/env bash
# Utilitários comuns dos scripts de scripts/audit/. Não é executado sozinho.
#
#   source "$(dirname "$0")/_common.sh"
#
# Define ROOT (raiz do repositório), OUT (build/audit), GRADLEW e JAVA_HOME,
# e as funções usage_from_header e py.

set -u
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
OUT="$ROOT/build/audit"
mkdir -p "$OUT"
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/jdk-21.0.12}"
if [ "${OS:-}" = "Windows_NT" ]; then GRADLEW="$ROOT/gradlew.bat"; else GRADLEW="$ROOT/gradlew"; fi
cd "$ROOT" || exit 2

# Imprime o bloco de comentário do topo do script chamador (linhas 2 até a
# primeira linha sem '#') como ajuda.
usage_from_header() {
  awk 'NR==1{next} /^#/{sub(/^# ?/,""); print; next} {exit}' "$1"
}

# Python isolado e em UTF-8 (o console do Windows é cp1252).
py() { python -I -X utf8 "$@"; }

gradle() { "$GRADLEW" "$@" --no-daemon --console=plain; }
