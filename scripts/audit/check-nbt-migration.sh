#!/usr/bin/env bash
# check-nbt-migration.sh — migração BREEDER→SHEPHERD, save malformado e
# propriedades do planejador de caminho (auditoria de 2026-10-10, D5/D7/D9).
#
# Roda os testes de migração e de save malformado da bateria (SaveMigrationTest,
# ColonySavedDataTest, ColonySaveFuzzTest, WorkMemoryLoadTest) e a sonda de
# propriedades de scripts/audit/probes/, que é copiada para src/test só durante
# a execução e removida na saída (trap), mesmo em falha.
#
# Uso: scripts/audit/check-nbt-migration.sh [--help]
# Relatório: build/audit/nbt-migration.txt e build/audit/probes-results/
# (apagada a cada execução, para nunca relatar resultado velho).
# Saída 1 se algum teste ou sonda falhar.
source "$(dirname "$0")/_common.sh"
case "${1:-}" in -h|--help) usage_from_header "$0"; exit 0 ;; esac

RESULTS="$OUT/probes-results"
DEST=src/test/java/com/villagecolony/audit
REPORT="$OUT/nbt-migration.txt"
rm -rf "$RESULTS"; mkdir -p "$RESULTS"

cleanup() { rm -rf "$DEST"; }
trap cleanup EXIT
[ -e "$DEST" ] && { echo "ERRO: $DEST já existe; não sobrescrevo" >&2; exit 2; }
mkdir -p "$DEST" && cp scripts/audit/probes/*.java "$DEST/"

status=0
gradle test --tests 'com.villagecolony.data.save.SaveMigrationTest' \
    --tests 'com.villagecolony.data.save.ColonySavedDataTest' \
    --tests 'com.villagecolony.data.save.ColonySaveFuzzTest' \
    --tests 'com.villagecolony.fabric.work.WorkMemoryLoadTest' \
    > "$OUT/nbt-migration-gradle.log" 2>&1 || status=1
for t in data.save.SaveMigrationTest data.save.ColonySavedDataTest data.save.ColonySaveFuzzTest \
         fabric.work.WorkMemoryLoadTest; do
  cp "build/test-results/test/TEST-com.villagecolony.$t.xml" "$RESULTS/" 2>/dev/null || status=1
done

gradle propertyTest --tests 'com.villagecolony.audit.PathPlannerProperties' \
    > "$OUT/properties-gradle.log" 2>&1 || status=1
cp build/test-results/propertyTest/TEST-com.villagecolony.audit.*.xml "$RESULTS/" 2>/dev/null || status=1

py - "$RESULTS" > "$REPORT" <<'EOF'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + '/*.xml')):
    s = ET.parse(f).getroot()
    print(f"{s.get('name')}: {s.get('tests')} testes, {s.get('failures')} falhas, {s.get('errors')} erros")
    for c in s.findall('testcase'):
        bad = c.find('failure') if c.find('failure') is not None else c.find('error')
        message = (bad.get('message') or '') if bad is not None else ''
        detail = ' — ' + message.splitlines()[0][:140] if message else ''
        print(f"  [{'FALHA' if bad is not None else 'ok   '}] {c.get('name')}{detail}")
EOF
cat "$REPORT"
echo "saída: $status (relatório em $REPORT)"
exit $status
