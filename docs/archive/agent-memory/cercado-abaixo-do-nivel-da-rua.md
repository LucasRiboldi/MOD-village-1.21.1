---
name: cercado-abaixo-do-nivel-da-rua
description: O animal_pen tem camada de solo em y=0 e assentava 1 bloco abaixo da rua; StreetLayer.soilBase corrige
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T01:19:20.955Z
---

Playtest 0.3.52 (13-10): o autor viu o cercado construído abaixo do nível da rua. Causa, provada
pelo log + NBT: `plains_animal_pen_1` tem a camada y=0 = **solo** (27 grass_block + 1 dirt) e a
cerca/portão em y=1. Log: `planned ... at ColonyPos[x=-328, y=64,...]` com a rua medida em y=65 —
origem 1 abaixo da rua, o chão de dentro do recinto virou uma cova.

`StreetLayer.of` decide qual camada do blueprint vai no nível da rua (porta-1, senão encaixe de
rua, senão `sunkenBase`). O `sunkenBase` só pega base ESPARSA (`bottom*2 < above`, feito para o
`barn_majest`: 5 embaixo, 85 em cima). O pen tem camada de solo DENSA (30 vs 21), então não era
pego — ficava sem street layer e afundava.

Correção 0.3.53 (decisão do autor: nivelar pela camada útil): `StreetLayer.soilBase` — se a camada
0 é SÓ solo natural (grass/dirt/sand/gravel/snow/podzol/.../dirt_path, set SOIL), ela é a camada
da rua (retorna 0). Entra em `of()` antes do `sunkenBase`. Testes em `StreetLayerTest`
(`aSoilBottomLayerIsTheStreetLevel`, `aBuiltFloorIsNotSoil`, `aMixedBottomLayerIsNotPureSoil`);
mutação (inverter soilBase) derruba. test 1506/0, runGametest 765/765. JAR 0.3.53 = F6C85111…,
**não jogado** (falta ver o pen assentar no nível da rua em jogo).

Como diagnosticar estrutura por camada: decodificar o NBT (gzip) e contar blocos por y / palette —
casa tem cobblestone/planks em y=0 (piso construído), pen/roça têm grass/dirt (solo). Relacionado:
[[validar-antes-de-transformar]], [[riscado-nao-e-assentado]], [[bancadas-do-mod]].
