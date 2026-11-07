---
name: mineiro-preso-na-descida
description: Raiz do P0 de 10-10: dono do único ramal vai e volta na descida até um patamar de pedra maciça; MinerSteps zera os prazos na descida e nenhum guarda dispara
metadata:
  type: project
---

Três sessões (0.3.22 e 0.3.23, 10/11-10): zero mineração. O mineiro `2f3facba` segura o
único ramal repartível (save `villagecolony_memory.dat`, `arms`) e mira o nível em y=16; a
rota de `MineEntrance.legDown` manda da boca lembrada (−768,66,−973) ao patamar (−763,56,−968),
que é pedra maciça no `r.-2.-2.mca` (os níveis guardados não existem — 5 níveis abaixo da
boca, marca do "desce 5 níveis num segundo" de 07-10). Ele vai e volta; `MinerSteps.java:153–161`
zera `lease` e `stalled` a cada tique de descida, `still` não pega quem anda → ramal preso
para sempre, os outros 3 barrados ("no mine mouth" é a busca de superfície do barrado).

**Why:** "no mine mouth" parecia falta de mina e era mineiro barrado; a frase mentia sobre a causa.
**How to apply:** ao ver mineração zero, ler `miners:` no log (quem segura o ramal e onde está),
e o save com `scratchpad/nbtread.py`/`mcablocks.py` (leitores NBT/Anvil só-leitura) antes de
supor geometria. Ver [[lento-nao-e-travado]] e [[frase-ausente-refuta-mecanismo]].

**Estado em 2026-10-11:** RC1 feita em `08b0c34f` (PR #24): `MineLease.outOfTimeOnDescent`, 60 s
sem chegar mais perto nem descer mais fundo → `stuck(...)`. RC2 feita em `36d3946b`: os níveis
EXISTIAM (cavados NORTH da boca real −768,66,−923); o giro da escada (ADR-037 M1) trocou o rumo para
EAST, e o `MineEntrance` deduzia a cadeia pelo rumo de agora. Lição: "nível fantasma" era rota
calculada por regra que o giro quebra; procure tochas no save (`mcablocks`) para achar a escada real.
