---
name: roteiro-ociosidade-pendente
description: Roteiro dos pontos de ociosidade a atacar depois (pedido do autor 15-10). 6 das 7 profissões ociosas têm raiz na ALTURA; só o SMELTER é acionável sem a terraplanagem. Ordem e dependências registradas.
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T19:21:12.519Z
---

**Pedido do autor (15-10): registrar os pontos de ociosidade para atacar depois.** Do playtest 0.3.64
([[playtest-0364-ociosidade-altura]], 60 min, vila montanhosa y59-106).

**Os pontos, por profissão, com a causa e a dependência:**

| Profissão | Ocioso+bloq | Causa | Depende de |
|---|---|---|---|
| MASON | 93% | sem obra — lote plano não nasce na montanha | **TERRAPLANAGEM** ([[plano-terraplanagem-vila]]) |
| LUMBERJACK | 71% | sem obra pedindo madeira (cascata) | **TERRAPLANAGEM** |
| BUILDER | 36% | 25% blocked — obra em desnível | **TERRAPLANAGEM** |
| FARMER | 22% | sem lote de roça na montanha | **TERRAPLANAGEM** |
| SHEPHERD | 63% | terreno (34% blocked) | 0.3.64 (só-mineiro-remodela) já ataca — **FALTA JOGAR** |
| MINER | 52% | parede de montanha (TOO_HIGH/NO_STANDING) | A3 0.3.61 parcial — **FALTA JOGAR**; 2ª fase |
| SMELTER | 25% + espera 41% | espera clay/recurso inexistente no raio | **ACIONÁVEL AGORA** (C4) — NÃO feito ainda |

**Ordem recomendada quando voltar:**
1. **Jogar 0.3.64 + 0.3.66** na vila montanhosa — medir quanto escalada+bancada já resolveram do
   SHEPHERD/MINER e do deslocamento, antes de empilhar. (As duas não foram jogadas.)
2. **SMELTER / C4** (acionável sem terraplanagem): quando o recurso não existe no raio por N buscas, o
   fundidor troca de tarefa (funde outro material disponível / ajuda / recolhe) em vez de esperar. Vem
   de `SurfaceGatheringWork` (a mensagem "no clay/nothing in the whole radius"). Cuidado:
   [[castigo-por-oficio-vira-rodizio]] — punir o item sem frear a taxa empurra pro próximo; e
   [[took-zero-e-bau-cheio]] — "took 0" pode ser baú cheio, não falta de recurso.
3. **TERRAPLANAGEM** (a raiz de MASON/LUMBERJACK/BUILDER/FARMER): a vila nivela a montanha para subida
   uniforme ≤1 bloco, sem afrouxar o critério de obra. Feature grande, spec própria. Resolve 4 dos 7 de
   uma vez. Ver [[plano-terraplanagem-vila]].
4. **MINA 2ª fase** (MINER): o A3 é parcial; a parede de montanha ainda gera TOO_HIGH/NO_STANDING.

**Também pendente (fora de ociosidade):** fluxo de bancada feito (0.3.66) mas não jogado; faces dos
blocos (0.3.65) não jogadas.

Relacionado: [[playtest-0364-ociosidade-altura]], [[plano-terraplanagem-vila]],
[[castigo-por-oficio-vira-rodizio]], [[regra-51-tempo-ocioso]].
