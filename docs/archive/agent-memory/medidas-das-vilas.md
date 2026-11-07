---
name: medidas-das-vilas
description: Vila Vanilla de planície mede ~110-160 blocos de lado; vizinhas a 238-636 de centro; scripts/world_survey.py mede o save fechado
metadata:
  type: reference
---

`python scripts/world_survey.py [pasta do mundo]` (precisa de nbtlib) lê
region/ (structures/starts), poi/ (home, meeting) e o .dat do mod. Numera
vilas de norte para sul. Medido em 2026-09-30 (save "Novo mundo"): 35 vilas,
caixas 86-161 x 96-158 (Y 8-55); vizinha mais próxima 238-636 de centro
(mediana 408); menor vão entre caixas 37, nenhum ≤32. Vila do autor = #4,
138x23x157, 27 camas originais + 39 de BigHouseMOD. Vila #20 tem 2 colônias.

**Armadilha:** POI de cama só existe em chunk já carregado — 14 vilas aparecem
com 0 camas; o mod não enxerga vila nunca visitada. Ver [[e51-identidade-da-vila]].
