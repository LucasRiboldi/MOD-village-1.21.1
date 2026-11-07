---
name: playtest-2026-10-01-noite
description: Playtest 01-10 23:12–00:17 (Spark jPsGP2hsPo) — curral e pilar funcionaram; pilar travou 41 min sob a grama protegida da vila; corrigido em 8789190 (mayDigOut)
metadata:
  type: project
---

Logs: `.minecraft/logs/2026-10-01-4.log.gz` (23:12–23:59) + `latest.log`
(00:00–00:17). Spark jPsGP2hsPo: TPS 20 todos os 61 min, MSPT mediano 7–12,
mod 1,7% (lenhador 0,7%, coleta de superfície/areia 0,6%). Pico ~100 ms a cada
5 min = fase "population" do ciclo (VillageForest procurando lugar, F13).

- Curral: 26 saídas em 12–67 tiques. Faixa de 1 bloco entre cercas (z=430)
  pula PARA o curral (regra "lado mais largo") e sai pelo portão — não trava.
  `862b0a6b` falhou 7 min sem causa no log → log "could not use" ganhou fase,
  posição fracionária, chão e passo do caminho.
- Pilar: 199ad062 subiu 81 níveis (y −13→68) em 2 min 20 s, parou 41 min a 2
  da superfície: "may not break grass_block" 388x — grama dentro da caixa de
  peça gerada. Corrigido: `BlockProtection.mayDigOut` (exceção do chão do
  bioma, só para a fuga); túnel ganhou TUNNEL_PASSES=11.
- Construção normal (403→75 blocos, esperas de material). Colônia medida:
  X −324..−159, Z 349..488; o centro (−241,67,419) fica debaixo da vila.

**Why:** a exceção do chão do bioma já valia para rua e lote; faltava na fuga.
**How to apply:** JAR 17ceb450 em mods. Próximo log: o 199ad062 (ou outro sob
a vila) deve passar de `climbed to … 68` para `is out at`; contar "could not
use … in APPROACH phase" para o caso 862b0a6b. Ler blocos do save com o
script `blocks.py` (nbtlib, mesmo formato do world_survey). Ver
[[climb-out-nunca-desiste]], [[e52-curral-prende-aldeoes]].
