#!/usr/bin/env bash
# check-tests-green.sh — testes Python, unitários Java e a bateria de jogo.
#
# Roda, nesta ordem: `python -m unittest discover -s tests`,
# `gradle test --rerun-tasks` e N rodadas de `runGametest --rerun-tasks`.
# Conta os unitários pelos XML de build/test-results/test (não pelo log, que
# só lista o que falhou) e os GameTests pela linha "All N required tests
# passed" — sem ela a rodada conta como falha, mesmo com BUILD SUCCESSFUL.
#
# Uso: scripts/audit/check-tests-green.sh [--runs N] [--help]   (N padrão: 1)
# Relatório: build/audit/tests-green.txt; logs em build/audit/tests-green/.
# Saída 1 se qualquer etapa falhar.
source "$(dirname "$0")/_common.sh"
RUNS=1
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage_from_header "$0"; exit 0 ;;
    --runs) RUNS="$2"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
LOGS="$OUT/tests-green"; mkdir -p "$LOGS"
REPORT="$OUT/tests-green.txt"
status=0
: > "$REPORT"

py -m unittest discover -s tests > "$LOGS/python.log" 2>&1 || status=1
echo "python: $(grep -aE '^Ran [0-9]+ tests' "$LOGS/python.log") — $(tail -1 "$LOGS/python.log")" >> "$REPORT"

gradle test --rerun-tasks > "$LOGS/unit.log" 2>&1 || status=1
py - >> "$REPORT" <<'EOF'
import glob, xml.etree.ElementTree as ET
t = f = 0
for p in glob.glob('build/test-results/test/*.xml'):
    r = ET.parse(p).getroot()
    t += int(r.get('tests')); f += int(r.get('failures')) + int(r.get('errors'))
print(f'unitários: {t} testes, {f} falhas/erros')
EOF

for i in $(seq 1 "$RUNS"); do
  gradle runGametest --rerun-tasks > "$LOGS/gametest-$i.log" 2>&1 || status=1
  line=$(grep -aoE 'All [0-9]+ required tests passed|[0-9]+ required tests failed' "$LOGS/gametest-$i.log" | head -1)
  [ -z "$line" ] && { line="sem linha de resultado (rodada não conta)"; status=1; }
  case "$line" in *failed*) status=1 ;; esac
  echo "gametest rodada $i: $line" >> "$REPORT"
done
cat "$REPORT"
echo "saída: $status"
exit $status
