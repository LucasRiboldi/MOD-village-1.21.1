---
name: sessao-2026-09-06-07-lenhador-e-barrier
description: Logs de 06-07/09 — lenhador gasta 95% do tempo recusando as mesmas 140 árvores mortas, e o TEST BARRIER risca stripped_oak_log com 50 oak_log no baú
metadata:
  type: project
---

Medido nos logs de `%APPDATA%\.minecraft\logs` (jar conferido por md5 = commit 4a9dd6b).

**E1 — o lenhador reavalia para sempre as mesmas árvores mortas.**
Dia 06: 925 linhas `Not a tree` sobre **140 coordenadas distintas** (~7 reavaliações
cada), 273 `made no progress`, e apenas **48 árvores derrubadas**. Amostras de estado:
749 `looking for a tree` / 539 `walking` / 93 `chopping` — 7% do tempo cortando.
A causa não é bug: `TreeMarks.REJECTED_MEMORY` = 10 × `CYCLE_TICKS` (600) = 6000 ticks,
e os timestamps das rejeições repetidas ficam exatamente 5 minutos apart. É a Regra 23
(*o que já foi analisado pode ser analisado de novo*) funcionando como escrita.
O que não foi previsto: um pilar de tronco **sem copa nunca vira árvore** — reavaliar
a mesma parede a cada 5 min é custo puro. A Regra 23 vale para o que o mundo muda
(muda plantada, copa que cresce); não vale para construção do jogador.
Ver [[gametest-copia-do-predicado]].

**E2 — TEST BARRIER risca `stripped_oak_log` com a matéria-prima no baú.**
24 riscos no dia 06. Amostra: colônia `1760a4b3` com **OAK_LOG=50** no baú no momento
do risco. `ManufacturerWork.strip()` está correto (corrigido 05/09, usa
`MaterialChoice` para a espécie que a colônia tem) e roda **antes** da guarda de
`convertOne` — mas descascou **1 tronco na sessão inteira**. O barrier em
`BuilderWork.java:384` risca a peça na primeira falta, sem esperar o fabricante.

**O que NÃO é defeito (verificado):** a roça de 05/09 está corrigida — no dia 07 ela
nasceu a 5 blocos e o fazendeiro semeou (`sowed potato`). Zero exceptions nos dois logs.

**Anomalia menor não investigada:** miner reporta `105 of 32 so far` (meta ultrapassada
sem parar), 496 ocorrências no dia 06.
