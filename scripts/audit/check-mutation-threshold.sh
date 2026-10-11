#!/usr/bin/env bash
# check-mutation-threshold.sh — placar do PIT contra o limiar (padrão 85%).
#
# Com --run, roda `gradle pitest` antes; sem ele, lê o mutations.xml que já
# existe. Usa scripts/audit/pit_report.py: placar, folga em mutantes até o
# limiar e os 20 sobreviventes mais relevantes (pacotes críticos primeiro).
# XML sem mutação nenhuma reprova: PIT que não rodou não passa.
#
# Uso: scripts/audit/check-mutation-threshold.sh [--run] [--threshold N] [--help]
# Relatório: build/audit/mutation.txt e build/audit/pit.json.
# Saída 1 abaixo do limiar; 2 se não houver mutações.
source "$(dirname "$0")/_common.sh"
RUN=0; TH=85
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage_from_header "$0"; exit 0 ;;
    --run) RUN=1; shift ;;
    --threshold) TH="$2"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
if [ $RUN = 1 ]; then
  gradle pitest > "$OUT/pitest-gradle.log" 2>&1
fi
XML=build/reports/pitest/mutations.xml
[ -f "$XML" ] || { echo "sem $XML: rode com --run" | tee "$OUT/mutation.txt"; exit 2; }
py scripts/audit/pit_report.py "$XML" --threshold "$TH" --out "$OUT/pit.json" > "$OUT/mutation.txt"
status=$?
cat "$OUT/mutation.txt"
echo "saída: $status"
exit $status
