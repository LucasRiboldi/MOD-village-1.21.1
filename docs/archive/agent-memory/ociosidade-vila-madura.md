---
name: ociosidade-vila-madura
description: 0.3.58 — a ociosidade geral é a vila madura sem obra; expansão acelerada (FULL_AGAIN_AFTER)
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T20:33:19.223Z
---

Playtest 0.3.57 (Spark iALIIoOwEK, 42 min). A horta destravou (obra avança, 201 peças, TPS 20),
mas ociosidade geral alta: lenhador 98% ocioso, pedreiro 71%, pastor 80%, fundidor 60× "no task
open — able to smelt". **Raiz medida (não é bug solto):** a vila está MADURA — 5 casas, 1 obra,
21× "no empty plot", e estoque CHEIO (837 cobblestone, 322 oak_log, 220 stone). Os produtores param
porque não há obra pedindo e o baú transborda, NÃO por falta de material. Produzir mais seria o P0
antigo "a vila fabrica o que ninguém pediu". A manutenção do ofício funciona (lenhador plantou 21
mudas) mas esgota rápido. Único ócio por defeito real: o mineiro (172× NO_STANDING_POSITION, 44%
bloqueado) — aberto.

Decisão do autor: crescer a vila (dar demanda). Dois limites de expansão no código:
- `VillageExpansion.FULL_AGAIN_AFTER` era `3*FULL_AFTER` — após anexar uma faixa, esperava 3 voltas
  de varredura (dezenas de min) para re-anexar. **Corrigido 0.3.58: = FULL_AFTER** (anexa na volta
  seguinte que confirma falta de lote). Teste `anAnnexedStripThatGaveNoLotAnnexesAgainOnTheNextFullSweep`
  amarra via `assertEquals(FULL_AFTER, FULL_AGAIN_AFTER)`.
- `VillageExpansionSurvey.ANNEX_EVERY = 36000` (30 min) — teto de 1 anexo por meia hora. **NÃO
  mexido**: é a proteção contra o abandono da 0.3.42 (4 anexos em 5 min moveram o centro 68 blocos
  e soltaram 27 aldeões, [[abandono-por-deriva-do-centro]]). Neste playtest "is full" só 1×/42min, 1
  anexo — o ANNEX_EVERY não foi o gargalo ativo; o FULL_AGAIN_AFTER foi. Num playtest mais longo o
  ANNEX_EVERY de 30 min vai limitar — mas baixá-lo exige decisão do autor (risco de abandono).

Veredito honesto: a ociosidade da vila madura é em boa parte estrutural (vila pronta). A correção
acelera o crescimento sem forçar produção que enche baú. test 1515/0, runGametest 769/769. JAR
0.3.58 = 429511EA… Relacionado: [[regra-51-tempo-ocioso]], [[emenda-7-crescimento-sem-teto]],
[[processo-main-e-acoes-continuas]].
