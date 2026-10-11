#!/usr/bin/env bash
# check-docs-drift.sh — números dos documentos contra os números medidos.
#
# Lê as contagens do README (unitários, Python, GameTests, PIT), a versão do
# badge, a linha "JAR atual" do STATE.md, o topo do CHANGELOG e o teto de
# 150 linhas do STATE.md, e compara com build/test-results, os logs de
# GameTest em build/audit, o mutations.xml do PIT e o gradle.properties.
# Rode antes check-tests-green.sh e check-mutation-threshold.sh --run. Um
# `test --tests` filtrado (check-architecture.sh, check-nbt-migration.sh)
# deixa em build/test-results só as classes filtradas: rode o drift depois
# de um `test` completo.
#
# Uso: scripts/audit/check-docs-drift.sh [--help]
# Relatório: build/audit/docs-drift.txt e build/audit/docs-drift.json.
# Saída 1 se houver qualquer divergência.
source "$(dirname "$0")/_common.sh"
case "${1:-}" in -h|--help) usage_from_header "$0"; exit 0 ;; esac
py scripts/audit/docs_drift.py --out "$OUT/docs-drift.json" > "$OUT/docs-drift.txt" 2>&1
status=$?
cat "$OUT/docs-drift.txt"
echo "saída: $status"
exit $status
