---
name: bancadas-do-mod
description: Blocos próprios do mod (carpinteiro/pedreiro/ferreiro) que dobram a produção; primeira infra de conteúdo registrado
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T01:19:05.010Z
---

0.3.52 (13-10). Pedido do autor: blocos exclusivos do mod, réplicas das bancadas de cada
profissão, com luz, que **dobram a produção** se o aldeão da profissão as usar; só o **player**
as fabrica (receita), a colônia nunca.

**Escopo decidido com o autor** (brainstorming): só as profissões que já trabalham diante de
estação — carpinteiro (`carpenter_bench`/crafting_table), pedreiro (`mason_cutter`/stonecutter) —
**+ ferreiro/fundidor** (`blacksmith_forge`/blast_furnace, bloco simples sem BlockEntity). Luz
**10**. Textura **dourada** (tint sobre a Vanilla, remete ao ouro da receita). Dobra a
**quantidade por ciclo** (+1 cópia no baú), não a velocidade. Receita = estação Vanilla + 4× ouro,
sempre disponível. Vínculo = **POI próprio do mod** (`PointOfInterestHelper.register`, sem
`VillagerProfession` — Vanilla não emprega nele, detecção de vila intacta).

**Arquitetura (abordagem A):** `ModContent` (fabric/content) registra 3 blocos + BlockItem + 3
POIs; mapa profissão→bloco/POI. `CraftStation.modBench(world, profession, near)` busca o POI do
mod (preferido sobre crafting_table/stonecutter/sino); `bench()` o chama primeiro; SMELTER ganhou
caso no CraftStation (antes não tinha spot). `ModBenchBonus.grantExtra(world, workerId, item, near)`
é o ÚNICO ponto do dobro: se `usesModBench`, deposita +1 do item (baú da profissão, senão da vila).
Chamado nos call sites com workerId: `CraftingSteps.produceForWork`/`produceForGoal` (após
`stockFor`) e `SmelterWork.convert` (após o depósito). NÃO dobra no fundo recursivo do
`ColonySupply.craft` (não tem workerId) — por isso o +1 extra no call site, não ×2 lá dentro.

**Infra nova:** primeira vez que o mod registra bloco/item. Registro em
`VillageColonyMod.onInitialize` (cedo). Resources: blockstates/models/textures (geradas com PIL,
tint dourado sobre a Vanilla do merged.jar), 3 receitas, tag `#villagecolony:profession_bench`,
lang pt_br/en_us/pt_pt. GameTest registrado no `src/gametest/resources/fabric.mod.json`
(senão some da bateria, [[gametest-registro-silencioso]]).

**Armadilhas vividas:** (1) assert de mutação tem que ser contra o literal `2`, não contra
`ModBenchBonus.FACTOR` — senão a mutação FACTOR=1 vira 1==1 e não derruba. (2) `SmelterWork`
passou de 500 linhas (FileSizeRuleTest); enxuguei 2 linhas no próprio método tocado.

Verificado: test 1503/0; runGametest 765/765 (flake conhecido [[bateria-gametest-instavel]] à
parte); mutação FACTOR=1 derruba `thecarpenterdoublesatthemodbench` e `thesmelterdoublesatthemodforge`.
JAR 0.3.52 = E4B7DDDC…. Spec: `docs/superpowers/specs/2026-10-13-bancadas-do-mod-design.md`.

**0.3.53:** BlockItem registrado sem ItemGroup NÃO aparece em aba nenhuma do criativo (só receita
no livro e `/give`) — gap da 0.3.52. Corrigido: aba própria do mod `ModContent.GROUP`
(`FabricItemGroup.builder()`, Registries.ITEM_GROUP, lang `itemGroup.villagecolony.benches`).
Jogado na 0.3.52: as bancadas funcionam, mas o autor não as tinha achado no criativo — era isso.
Relacionado: [[regra-45-baus-da-vila]], [[cercado-abaixo-do-nivel-da-rua]].
