#!/usr/bin/env bash
# check-tick-budget.sh — custo do ciclo da colônia e Regra 51 num log de jogo.
#
# Lê um latest.log de playtest (padrão: %APPDATA%/.minecraft/logs/latest.log)
# e roda scripts/cost_ledger.py (linhas VC_COST: média, p95 e máximo por
# fase) e scripts/time_ledger.py (linhas VC_TIME: proporção do expediente).
# Reprova se o p95 do ciclo total passar do tique (50 ms) em qualquer faixa
# de colônias, ou se algum ofício passar da Regra 51 (ocioso + bloqueado +
# encalhado > 15%). Com --journal <jsonl>, junta o action_report.py.
#
# Não abre o jogo: um perfil do Spark exige sessão com jogador e fica fora
# daqui (ver o skill minecraft-performance-profiling).
#
# Uso: scripts/audit/check-tick-budget.sh [--log <latest.log>] [--journal <actions.jsonl>] [--help]
# Relatório: build/audit/tick-budget.txt.
# Saída 1 se o orçamento ou a Regra 51 forem violados; 2 se o log não tiver VC_COST.
source "$(dirname "$0")/_common.sh"
LOG="${APPDATA:-}/.minecraft/logs/latest.log"; JOURNAL=""
while [ $# -gt 0 ]; do
  case "$1" in
    -h|--help) usage_from_header "$0"; exit 0 ;;
    --log) LOG="$2"; shift 2 ;;
    --journal) JOURNAL="$2"; shift 2 ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done
[ -f "$LOG" ] || { echo "sem log: $LOG" >&2; exit 2; }
REPORT="$OUT/tick-budget.txt"
{
  echo "log: $LOG"
  grep -a -m1 -oE 'villagecolony [0-9.]+' "$LOG" | sed 's/^/mod: /'
  echo; echo "== cost_ledger (ms)"; py scripts/cost_ledger.py "$LOG"
  echo; echo "== time_ledger"; py scripts/time_ledger.py "$LOG"
  if [ -n "$JOURNAL" ]; then echo; echo "== action_report"; py scripts/action_report.py "$JOURNAL"; fi
} > "$REPORT" 2>&1
cat "$REPORT"
py - "$REPORT" <<'EOF'
import re, sys
text = open(sys.argv[1], encoding='utf-8', errors='replace').read()
totals = [float(m.group(2)) for m in re.finditer(r'^\s*total\s+([\d.]+)\s+([\d.]+)\s+([\d.]+)', text, re.M)]
r51 = re.findall(r'^(\w+)\s.*REGRA 51 \((\d+)%', text, re.M)
if not totals:
    print('\nveredito: sem linhas VC_COST no log'); sys.exit(2)
bad = [t for t in totals if t > 50]
print(f'\nveredito: p95 do ciclo {max(totals):.1f} ms (tique = 50 ms) '
      f'{"ESTOURA" if bad else "ok"}; Regra 51 violada por {len(r51)} ofício(s)'
      + (': ' + ', '.join(f'{p} {v}%' for p, v in r51) if r51 else ''))
sys.exit(1 if bad or r51 else 0)
EOF
status=$?
echo "saída: $status"
exit $status
