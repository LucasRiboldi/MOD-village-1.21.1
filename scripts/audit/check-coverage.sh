#!/usr/bin/env bash
# check-coverage.sh — cobertura de linha por pacote, nos dois instrumentos.
#
# Unitário (build/reports/jacoco/test): pacotes críticos core e data.
# Bateria de jogo (build/reports/jacoco/gametest, se existir): pacotes
# críticos fabric.work, fabric.integration e fabric.event, que só o
# GameTest exercita. Os dois relatórios vêm de `gradle test` e
# `gradle runGametest`; rode-os antes (ou use --run).
#
# Uso: scripts/audit/check-coverage.sh [--run] [--min N] [--help]   (N padrão: 60)
# Relatório: build/audit/coverage.txt, coverage-unit.json, coverage-gametest.json.
# Saída 1 se algum pacote crítico ficar abaixo do mínimo.
source "$(dirname "$0")/_common.sh"
RUN=0; MIN=60
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage_from_header "$0"; exit 0 ;;
    --run) RUN=1; shift ;;
    --min) MIN="$2"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
[ $RUN = 1 ] && { gradle test jacocoTestReport > "$OUT/coverage-gradle.log" 2>&1; gradle runGametest >> "$OUT/coverage-gradle.log" 2>&1; }
UNIT=build/reports/jacoco/test/jacocoTestReport.xml
GAME=build/reports/jacoco/gametest/jacocoGametestReport.xml
status=0
{
  if [ -f "$UNIT" ]; then
    echo "== unitário (críticos: core, data)"
    py scripts/audit/coverage_report.py "$UNIT" --min "$MIN" --critical core,data --out "$OUT/coverage-unit.json" || status=1
  else
    echo "sem $UNIT"; status=1
  fi
  if [ -f "$GAME" ]; then
    echo; echo "== bateria de jogo (críticos: fabric.work, fabric.integration, fabric.event)"
    py scripts/audit/coverage_report.py "$GAME" --min "$MIN" --critical fabric.work,fabric.integration,fabric.event \
       --out "$OUT/coverage-gametest.json" || status=1
  else
    echo; echo "sem $GAME (rode runGametest)"
  fi
} > "$OUT/coverage.txt" 2>&1
cat "$OUT/coverage.txt"
echo "saída: $status"
exit $status
