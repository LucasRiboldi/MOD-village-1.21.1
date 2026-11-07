---
name: playtest-0359-spark-1nwglutitv
description: "Playtest 0.3.59 (15-10, Spark 1nwGlUtitv, ~37min) CONFIRMA que o conserto do falso caminho à superfície funcionou — MINED saltou de 17/64 para 297/295 por mineiro, 1448 minerações. Resta o 2º defeito (approach 4-5bl) para o P1."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T03:13:58.257Z
---

Playtest da **0.3.59** (15-10 madrugada, Spark `1nwGlUtitv`, ~37 min; log dividido em
`2026-10-14-6.log.gz` + `-7.log.gz`, action log `villagecolony-actions-20261014-232713.jsonl`).
TPS 20 estável; vila menor que a da 0.3.58 (166-318 entidades).

**O conserto do DirectPath (0.3.59) FUNCIONOU — 1º defeito do mineiro fechado:**
| métrica | 0.3.58 (100min) | 0.3.59 (~37min) |
|---|---|---|
| MINED por mineiro | 17, 64 | **297, 295** |
| MINED/min | 0,8 | **~19** (aceite >0,8 batido folgado) |
| mineiros com tarefa feita | 0/2 | 2/2 |
| TARGET_REJECTED NO_STANDING | 72× | 22× (−70%) |
| "not moved a block" | 22× | 9× |
| settle (minerações) | bursts raros | **1448** |

**Resta o 2º defeito em série (o P1 da Fase 1):** `time_ledger` do jogo completo (46 janelas) dá
MINER 50% idle + 6% blocked (Regra 51: 57%), e `went one level deeper` só 1× — o mineiro minera muito
no nível atual mas o approach **encala a 4-5 bl** quando chega perto: 22× NO_STANDING_POSITION + 9×
TOO_HIGH, conclusão 11% (15 soltas). É exatamente o alvo do P1 (alternativa A1+A3: roomBeside antes de
passOver + contornar).

**O resto da vila (MASON 78%, BUILDER 63% idle)** é o ócio de vila madura sem obra já diagnosticado na
0.3.58 — outro problema, fases depois (B1/B2 do plano). FARMER/SMELTER reprovam por ESPERA (52-58%),
o C4/P3.

**P1 FEITO (0.3.60, 15-10, commit 34294d97, branch fix/miner-directpath-superficie, sem push):**
`MinerHands.passOver` tenta `MineVein.roomBeside` antes de largar quando `worthRerouting`(só
NO_STANDING) e redireciona o alvo para a pedra vizinha que abre o pé. TOO_HIGH/NO_PATH contornam.
test 1521/0; runGametest 769/769; mutação derruba `tooHighAndNoPathAreNotReroutable`. JAR 0.3.60 =
F164319032E6DE6342027DE336B89B7AD34A5CC42527A1A51E0A2AF37295C169. **Falta playtest de dia** (aceite:
NO_STANDING caindo, MINER Regra 51 <15%, went one level deeper >1).

Relacionado: [[mineiro-directpath-falso-caminho]], [[playtest-0358-spark-YOT1oqyhIv]],
[[ociosidade-vila-madura]].
