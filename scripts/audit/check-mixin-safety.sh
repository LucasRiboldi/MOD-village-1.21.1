#!/usr/bin/env bash
# check-mixin-safety.sh — mixins listados, sem @Overwrite/@Redirect/@ModifyConstant,
# handlers e campos com prefixo villagecolony$ e @Unique, defaultRequire >= 1,
# e alvos remapeados (ou refmap) no JAR mais recente.
#
# Se o skill modpack-modsmith estiver instalado, roda também o nível 1 do
# probar.py (o JAR contra si mesmo) e, com --pack <pasta>, o nível 2 (o JAR
# contra uma instalação real: loader, versão do MC, dependências, duplicatas).
#
# Uso: scripts/audit/check-mixin-safety.sh [--pack <pasta .minecraft>] [--help]
# Relatório: build/audit/mixin-safety.txt.
# Saída 1 se qualquer regra ou nível do probar.py falhar.
source "$(dirname "$0")/_common.sh"
PACK=""
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage_from_header "$0"; exit 0 ;;
    --pack) PACK="$2"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
status=0
{
  echo "== regras de mixin"
  py scripts/audit/mixin_safety.py || status=1
  PROBAR=.claude/skills/modpack-modsmith/tools/probar.py
  JAR=$(ls -t build/libs/*.jar downloads/*.jar 2>/dev/null | grep -v -e '-sources' -e 'Backup' | head -1)
  if [ -f "$PROBAR" ] && [ -n "$JAR" ]; then
    echo; echo "== modpack-modsmith probar.py sobre $JAR"
    if [ -n "$PACK" ]; then py "$PROBAR" "$JAR" --pack "$PACK" || status=1
    else py "$PROBAR" "$JAR" || status=1; fi
  else
    echo; echo "probar.py ou JAR ausente: níveis 1-2 do modsmith não rodaram"
  fi
} > "$OUT/mixin-safety.txt" 2>&1
cat "$OUT/mixin-safety.txt"
echo "saída: $status"
exit $status
