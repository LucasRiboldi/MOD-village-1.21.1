---
name: pendencias-0354-mina-folhas-esperando
description: "Diagnóstico pronto das 4 frentes que ficaram da rodada 0.3.54 — mina, folhas, profissões esperando"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T02:41:30.865Z
---

Playtest 0.3.53 (Spark vN5T2AtQNZ). A rodada 0.3.54 fez 3 de 7 pedidos (aba funcional, obra
parada [[cercado-abaixo-do-nivel-da-rua]] não — obra parada via shoveOutOf; plantação Regra 67).
Faltam 4, com o diagnóstico já feito:

**Mina (3 sub-pedidos do autor, "tudo numa rodada"):** o problema no log é a **fase serial
inicial** — `waiting for a branch — the shaft is one branch until the gallery starts, e5c88ea5
has it`. Antes de `NetworkDigging.galleryPhase` (exige `arm(0).cut() >= MineShaft.SHARED_BLOCKS`),
só o ramal 0 é repartível (`Mine.branchesOpenNow`=1); os outros 2 mineiros caem em
`MinerSteps.startNextStone`→`exposedStoneFallback`, que não acha pedra exposta → ociosos. Na fase
de rede o paralelismo já existe (N nós, N mineiros via `MineNetwork.claim`). (a) Paralelismo: abrir
os outros mineiros mais cedo na fase da escada, ou dar-lhes nós da rede assim que o salão começa.
(b) Descer mais agressivo: `NetworkDigging.nextOrDescend` só desce com `network.exhausted()`.
(c) Mineiro preso: `e5c88ea5 ... 9 blocks below it and unable to climb, walking to the mine mouth`
— acesso à frente de trabalho na descida (ligado a [[mineiro-preso-na-descida]]).

**Folhas (lenhador recolhe se a obra pedir):** a raiz é que `_leaves` NÃO é um `ResourceType`/
`ResourceGroup` do mod (só WOOD/PLANKS/STRIPPED existem), então obra que pede `oak_leaves` não
mapeia a profissão nenhuma e fica sem suprimento. `TreeHarvester` já QUEBRA as folhas no abate
(não guarda o bloco). `LumberjackWork.isWoodTask` exige `group()==WOOD`. Caminho: declarar folha
como `HARVESTED` (lenhador) + fazer o lenhador guardar o bloco de folha quando pedido; `isVillageWood`
em `BiomeConstructionSupply` já casaria `oak_leaves` por `startsWith(species+"_")`. Atravessa
ResourceType/Production/ColonyCycle.typeFor/LumberjackWork — não-trivial.

**Profissões esperando:** no playtest, MASON 68% espera, FARMER 66%, SMELTER 67% ocioso/bloqueado,
LUMBERJACK 38% blocked — mas a maior parte era **cascata da obra parada** (white_terracotta/kept
standing), que a 0.3.54 destravou (shoveOutOf). Remedir no próximo playtest ANTES de caçar causa
por profissão: muito do "esperando" pode sumir com a obra andando. Pastor tem laço próprio de lã
("no sheep with wool", 9-25 soltas por playtest) — provável causa real, não cascata.

0.3.54 = 4BDCF491…, instalado, não jogado. Relacionado: [[mineiro-mina-em-rede]],
[[deadlock-no-de-seguranca-sem-enchimento]], [[regra-51-tempo-ocioso]].
