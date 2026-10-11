#!/usr/bin/env bash
# check-nbt-migration.sh — migração BREEDER→SHEPHERD, fuzzing de NBT e
# propriedades do planejador de caminho (auditoria de 2026-10-10, D5/D7/D9).
#
# Roda os testes de migração que já existem e as sondas de
# scripts/audit/probes/. As sondas são copiadas para src/test só durante a
# execução e removidas na saída (trap), mesmo em falha: nunca ficam na árvore.
#
# Relatório: build/audit/nbt-migration.txt e build/audit/probes-results/.
# Saída 1 se algum teste ou sonda falhar.
set -u
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
case "${1:-}" in
  -h|--help)
    sed -n '2,11p' "$0" | sed 's/^# \{0,1\}//'
    exit 0 ;;
esac
cd "$ROOT" || exit 2
export JAVA_HOME="${JAVA_HOME:-/c/Program Files/Java/jdk-21.0.12}"
GRADLEW=./gradlew; [ -f gradlew.bat ] && [ "${OS:-}" = "Windows_NT" ] && GRADLEW=./gradlew.bat
OUT=build/audit; mkdir -p "$OUT/probes-results"
DEST=src/test/java/com/villagecolony/audit
REPORT="$OUT/nbt-migration.txt"

cleanup() { rm -rf "$DEST"; }
trap cleanup EXIT
[ -e "$DEST" ] && { echo "ERRO: $DEST já existe; não sobrescrevo" >&2; exit 2; }
mkdir -p "$DEST" && cp scripts/audit/probes/*.java "$DEST/"

status=0
{
  echo "== migração existente (ColonySavedDataTest, SaveMigrationTest)"
  "$GRADLEW" test --tests 'com.villagecolony.data.save.SaveMigrationTest' \
      --tests 'com.villagecolony.data.save.ColonySavedDataTest' \
      --tests 'com.villagecolony.audit.NbtFuzzProbeTest' --no-daemon --console=plain
} > "$OUT/nbt-migration-gradle.log" 2>&1 || status=1
cp build/test-results/test/TEST-com.villagecolony.audit.*.xml build/test-results/test/TEST-com.villagecolony.data.save.*Migration*.xml \
   build/test-results/test/TEST-com.villagecolony.data.save.ColonySavedDataTest.xml "$OUT/probes-results/" 2>/dev/null

"$GRADLEW" propertyTest --tests 'com.villagecolony.audit.PathPlannerProperties' --no-daemon --console=plain \
    > "$OUT/properties-gradle.log" 2>&1 || status=1
cp build/test-results/propertyTest/TEST-com.villagecolony.audit.*.xml "$OUT/probes-results/" 2>/dev/null

python -I -X utf8 - "$OUT/probes-results" > "$REPORT" <<'EOF'
import glob, sys, xml.etree.ElementTree as ET
for f in sorted(glob.glob(sys.argv[1] + '/*.xml')):
    s = ET.parse(f).getroot()
    print(f"{s.get('name')}: {s.get('tests')} testes, {s.get('failures')} falhas, {s.get('errors')} erros")
    for c in s.findall('testcase'):
        bad = c.find('failure') if c.find('failure') is not None else c.find('error')
        mark = 'FALHA' if bad is not None else 'ok   '
        msg = '' if bad is None else ' — ' + (bad.get('message') or '').splitlines()[0][:140] if (bad.get('message') or '') else ''
        print(f"  [{mark}] {c.get('name')}{msg}")
EOF
cat "$REPORT"
echo "saída: $status (relatório em $REPORT)"
exit $status
