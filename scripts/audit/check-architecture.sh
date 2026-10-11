#!/usr/bin/env bash
# check-architecture.sh — isolamento das camadas e ciclos entre pacotes.
#
# 1. compila e roda o ArchitectureRulesTest (regras ArchUnit do projeto);
# 2. passa o bytecode pelo jdeps e por scripts/audit/arch_graph.py, que acha
#    dependência do core para fora dele, do core para Minecraft/Fabric e
#    ciclos entre pacotes (componentes fortemente conexos).
#
# Uso: scripts/audit/check-architecture.sh [--help]
# Relatório: build/audit/architecture.txt e build/audit/arch-graph.json.
# Saída 1 se o ArchUnit falhar ou o core depender de fora dele.
# Ciclos são relatados mas não reprovam (dívida, não quebra de ADR).
source "$(dirname "$0")/_common.sh"
case "${1:-}" in -h|--help) usage_from_header "$0"; exit 0 ;; esac

REPORT="$OUT/architecture.txt"
status=0
gradle test --tests 'com.villagecolony.architecture.*' > "$OUT/architecture-gradle.log" 2>&1 || status=1
"$JAVA_HOME/bin/jdeps" -verbose:package -filter:none build/classes/java/main > "$OUT/jdeps-full.txt" 2>&1
{
  echo "ArchUnit: $([ $status = 0 ] && echo passou || echo FALHOU) (ver architecture-gradle.log)"
  py scripts/audit/arch_graph.py "$OUT/jdeps-full.txt" "$OUT/arch-graph.json"
} > "$REPORT" 2>&1
graph=$?
[ $graph -ne 0 ] && status=1
cat "$REPORT"
echo "saída: $status"
exit $status
