---
name: emenda-7-crescimento-sem-teto
description: ADR-003 Emenda 7 (6db813a): busca de lote = ½ diagonal da caixa + 12, sem teto; o autor escolheu isso ciente do custo quadrático da varredura
metadata:
  type: project
---

2026-10-01, o autor: "a vila deve crescer infinitamente para qualquer lado". O
teto real era `ConstructionPlanner.searchRadius` = 64 fixo, que alimenta lote,
índice de ruas (`rebasedTo`) e ponta de rua. Agora `searchRadius(colony)` =
max(64, ½ diagonal da caixa + 12); não medida = 64; testes via shortenSearchTo.

**Why:** eu estimei errado o custo ("alguns ciclos") e corrigi antes de aplicar:
~1.024 colunas por ciclo de 30 s → volta de 8,5 min (64), ~26 min (caixa 144),
~1 h 40 (300); a rua só cresce após volta sem lote. Ofereci "crescer pelas ruas"
(varredura fixa em 64, índice de ruas pela caixa) e "faixa da borda"; o autor
manteve o raio pela caixa. Não há GameTest de vila passando de 64 (50+ passagens).

**How to apply:** se o playtest mostrar vila grande parada em `SWEEP_INCOMPLETE`
/ "looking for a lot", a 1ª opção é a CADÊNCIA: o planejador roda 1x por ciclo
de 30 s com 1 passagem — rodar passagens por segundo para a colônia em foco
(estudo docs/research/2026-10-01-solucoes-das-pendencias.md §7-A); crescer
pelas ruas é a 2ª.
JAR md5 d9598bd5 em mods (anterior 4a2b31f3 no scratchpad). Ver
[[regra-45-baus-da-vila]], [[quadrado-da-vila-proposta]].
