---
name: time-ledger-ja-so-expediente
description: "H2 do plano resolvido por verificação, não código — WorkTime.sample já pula aldeões fora do expediente (WorkHours.isWorkTime), desde o commit da Regra 50. A linha VC_TIME é expediente-apenas por construção; não há viés dia/noite a corrigir."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T02:19:59.077Z
---

**Ciclo H2 do plano de melhoria (14-10): resolvido por verificação.** A premissa estava errada.

O plano (e a varredura de 14-10, C3) supunha que o `time_ledger` contava espera noturna como bloqueio
e pedia separar dia/noite. **Falso.** `WorkTime.sample` (src/main/java/.../fabric/work/WorkTime.java:93-95)
pula o aldeão quando `!WorkHours.isWorkTime(world, villager)` → `continue`. A linha `VC_TIME` **só conta
o expediente**, por construção, **desde o commit original da Regra 50** (`5ce71b0f`). `WorkHours.isWorkTime`
→ `WorkClock.isWorkTime(getTimeOfDay())`.

O `time_ledger.py` também **já pondera certo** por `s=` (linha 74-77: `total * share/100`, dividido pelo
`total` real), não faz média simples das janelas. As janelas noturnas aparecem com `s=` baixo (ex.: MINER
com 12 janelas s=60 vs 13 s=600 no playtest 0.3.58) e entram ponderadas — correto.

**O que realmente houve na leitura de 11-10:** o "BUILDER 93% bloqueado era a noite" foi interpretação da
época; o filtro já existia. Não há conserto de código no H2.

**Decisão:** H2 fechado sem alterar código. O que falta de verdade para medir bem é **jogar de dia**
(já é a regra do aceite do P0), não mexer no ledger.

Relacionado: [[regra-51-tempo-ocioso]], [[playtest-0358-spark-YOT1oqyhIv]], [[lento-nao-e-travado]].
