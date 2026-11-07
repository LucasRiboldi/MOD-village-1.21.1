---
name: triagem-ler-notas-e-dados
description: "Regra do autor — toda triagem de playtest lê VC_BIOME_NOTE (/vc bioma nota) primeiro, e os dados de /vc dados, /vc bioma, /vc log"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-05T02:03:56.463Z
---

Em toda análise de playtest, ler sempre os dados gerados pelos comandos `/vc bioma nota`,
`/vc dados`, `/vc bioma` e `/vc log` — **principalmente as notas** (`VC_BIOME_NOTE` no log),
que são os defeitos que o autor viu e quer corrigidos (autor, 2026-11-04).

**Why:** o autor anota no jogo o que viu de errado; a triagem só pelos relatórios (`time_ledger`,
`action_report`, `cost_ledger`) perde o que ele observou.

**How to apply:** primeiro `grep VC_BIOME_NOTE` no `latest.log` e listar cada nota com horário,
depois `scripts/state_report.py` e `scripts/biome_report.py`, depois os três relatórios da
Regra 50. Cada nota vira achado com prioridade. Ver [[regra-blocos-surgidos-fabricacao]].
