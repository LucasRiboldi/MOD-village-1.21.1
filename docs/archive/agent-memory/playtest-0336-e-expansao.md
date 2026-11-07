---
name: playtest-0336-e-expansao
description: "Playtest 0.3.36 (Spark nTgYjiciKr): impasse do enchimento da mina e sílex sem rota; 0.3.37 corrige e aplica a expansão B+C+D (Regra 56); JAR 4032F148, não jogado"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-12T18:24:51.545Z
---

O playtest da 0.3.36 (12-10, 32 min) teve TPS 19,5 e mod 1,4%, mas o mineiro ficou em 0%. O túnel pronto esperava pedregulho para o piso: o log teve 37.297 linhas de "waits for fill". Os baús tinham zero pedregulho; só havia smooth stone (196) e lajes (468 + 348). Sem alvo, o mineiro não cavava e, por isso, não produzia o pedregulho que faltava.

A obra também parou a sessão inteira esperando fletching_table. O sílex não tem rota, e `renewForCycle` só contava ciclo com tarefa aberta, então a regra da 4ª tentativa nunca disparava.

**Correção na 0.3.37 (d3e0a8ed):**
- `NetworkSafety.quarryFor` faz o mineiro cavar a parede do túnel para tirar enchimento.
- O enchimento sai primeiro do baú do próprio mineiro.
- `SmelterAdvance` guarda 64 de pedregulho (MINE_FILL).
- A rota marcada como indisponível passa a contar ciclo.

**Expansão B+C+D (e81f83d5)**, em `VillageExpansion` (core) e `VillageExpansionSurvey`:
- casa reivindica 16 em volta, oficina e roça 24;
- depois de 2 varreduras sem lote, a vila anexa 16 blocos no melhor lado;
- a rua cresce primeiro por esse lado.

O autor não respondeu às perguntas 2–5 do doc; ficaram os valores propostos.

**Why:** a mina dependia só dos baús para o enchimento, e outras profissões consomem o pedregulho.

**How to apply:**
- No próximo playtest, contar `digging the wall for them`, `is full — annexes` e `stocked … after`.
- O lado anexado vive só na memória; a preferência da rua (D) não tem GameTest.
- Ver [[versao-0336-mina-e-planejamento]] e [[obra-esperando-tijolo]].
