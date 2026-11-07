---
name: playtest-2026-10-08-noite
description: Spark D66XrVOZl4 (0.3.19): celeiro 1 acima do chão, pastor fora do curral, funil sem rota; 0.3.20 commitada local, sem push
metadata:
  type: project
---

0.3.20 (commits 6e8cd5d3..90da1e68 em claude/obra-lote-e-trava, **não enviados**): StreetLayer.sunkenBase (planta sem porta/encaixe com camada 0 esparsa assenta no chão — só o barn_majest), ShepherdFlock.shearingArea (busca = raio + caixa da vila +10), HOPPER/RAIL/FLOWER_POT no STOCKED_AFTER_MISSES. Verificado: 1.387 unitários, 683/683, mutações pegas. JAR em mods. Não visto em jogo.

Ferramenta útil: ler o save com nbtlib (rodar python SEM -I, senão some o site-packages) — blocos por seção, entidades em entities/*.mca, colônias em data/villagecolony_colonies.dat. Foi o que provou o vão do celeiro e as ovelhas lanudas fora do raio.

**Aberto:** mina não cruza ferro (0 em 1.136 blocos cavados; decisão de desenho), mineiro cc99a53b na pedra −751 31 −942, lenhador parado pune a árvore, A1, ADR-036 item 6. Celeiro já aberto no save continua suspenso.

Ver [[regra-51-tempo-ocioso]], [[spark-com-action-report]], [[mineiro-em-cima-da-mina]].
