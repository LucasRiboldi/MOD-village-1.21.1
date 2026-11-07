---
name: cadeia-de-producao-recursiva
description: Perguntar "e fabricavel?" num degrau so esconde a obra travada; a cobertura tem de descer a receita ate a folha
metadata:
  type: project
---

O levantamento de cobertura de material **tem de ser recursivo**. Perguntar
*"a estante é fabricável?"* devolve **sim** e a colônia ainda assim não a
levanta: a estante pede tábua **e livro**, o livro pede papel **e couro**, e
o couro não nasce de baú nenhum.

É o P1.1 generalizado — lá a obra esperou por `cobblestone_stairs` que
ninguém fabricava, com 69 pedregulhos no baú. A causa foi sempre a mesma:
**um degrau da cadeia sem dono**.

`StructureCoverageGameTest` faz isso: lê as plantas das cinco vilas, desce
cada receita por `CraftingLookup.Bill.ingredients` até `CHAIN_DEPTH`, e
**falha** se aparecer peça sem produtor. São 189 itens distintos. O
relatório sai em `build/gametest/structure-coverage.txt` e é a fonte da
tabela do README.

**O falso positivo que quase sujou a lista:** variante de parede.
`wall_torch`, `spruce_wall_sign`, `brown_wall_banner` apareceram como órfãos
e **não são peça nova** — é o mesmo item colocado de outro lado, a mesma
conta que `WorkMaterials.coal` já fazia. Normalizadas em `standing()`. Uma
lista de órfãos com falso positivo é pior que nenhuma: ensina a ignorar a
lista.

**A distribuição real, medida em 2026-09-18:** carpinteiro 104, pedreiro 28,
o mundo 16, fundidor 12, lenhador 10, superfície 8, fazendeiro 7, pastor 3,
mineiro 1. O carpinteiro concentra mais de metade da cadeia — se um dia
alguém for dividir carga, é lá que está.

Relacionado: [[sorteio-sem-nada-para-sortear]] — o outro caso de estrutura
que existe e não é alcançada.
