# Obra esperando tijolo — playtest 0.3.32 (12-10, 09:08–09:34)

Fontes: `latest.log`, diário `villagecolony-actions-20261012-090943.jsonl`, `time_ledger`,
`action_report`, `cost_ledger`, Spark `ulRRxpi1ye` (TPS 20 em todas as janelas, MSPT mediano
6,5–8 ms, mod 0,8% do servidor; ciclo da colônia médio 16 ms, p95 38 ms — sem lag).

## O que aconteceu

A `plains_armorer_house_1` (−314, 63, −1046), planejada às 09:13, pôs as outras peças e parou com
**3 blocos de `bricks`** a partir de 09:23: `WAITING_RESOURCES … waiting for minecraft:bricks`, 28
leituras seguidas, até o fim da sessão. O `/vc log` às 09:34:

```
[CADEIA] bricks (bancada) ← brick (fundidor: sem tarefa) ← clay_ball (coleta de superfície: sem tarefa)
```

## As perguntas da regra

| Pergunta | Resposta (do log) |
|---|---|
| O tijolo (`bricks`) estava sendo fabricado por alguma profissão? | **Não.** A tentativa de fabricar respondeu "needs 4 minecraft:brick and has 0", e o pedido ao pedreiro não abriu porque `ColonySupply.canProvide` dá falso sem ingrediente. |
| E o ingrediente (`brick`, tijolo assado)? | **Não.** "it comes from the furnace, not the bench — the smelter owns it", e **nenhuma** tarefa `SMELT_MATERIAL brick` foi aberta na sessão (diário: só vidro, pedra lisa). |
| Os blocos para o tijolo estavam sendo recolhidos? | **Não.** Nenhuma tarefa de `clay_ball` nem de `clay` na sessão. |
| Quanto a vila já tinha e o que faltava? | 3 `bricks` = 12 `brick` = 12 `clay_ball`. Nos baús: `bricks` 0, `brick` 0, `clay_ball` 0; `CLAY=4` (bloco de argila — não volta a bola por receita) e `TERRACOTTA=8`. **Faltava tudo da cadeia.** |
| Qual profissão deveria resolver? | **Fundidor**: coleta a argila (`COLLECT_SURFACE_RESOURCE`) e assa o tijolo (`SMELT_ITEMS`). **Pedreiro**: junta 4 `brick` em `bricks` (`CRAFT_STONE`). |
| Por que não o fez? | Porque nenhuma tarefa nasceu. A demanda que desce a receita (`WorkMaterialRecipeNeeds.addSmeltedIngredients`) só descia quando o bloco da obra já era um recurso declarado (`red_terracotta`). `bricks` não é: a conta saía zero, e com zero nenhuma meta pede `brick` nem `clay_ball`. Sem tarefa aberta, o suprimento de último caso (aparecer depois de 4 ciclos) também nunca começou a contar (`ConstructionSupplyDispatches.renewForCycle` só conta ciclo com tarefa da rota aberta). Os três fundidores passaram a sessão soltando tarefa de vidro sem areia (20 vezes) — 53% em espera — com a argila por buscar. |

## Correção (0.3.33)

`WorkMaterialRecipeNeeds` desce a receita também da peça sem recurso declarado, até 3 degraus
(escada de tijolo → tijolos → tijolo assado), pela receita que o jogo usa. Lingote e vidro ficam de
fora porque já têm conta própria (`WorkMaterials.iron`, `.glass`). Com isso a obra de 3 `bricks`
pede 12 `brick` ao fundidor e 12 `clay_ball` à coleta; se a argila não estiver no alcance, a quarta
busca vazia a põe no baú (Regra 51), e a cadeia segue: fundidor assa, pedreiro junta, construtor põe.

Provado: `WorkMaterialsGameTest.bricksRequestTheSmelterAndTheClay` e
`brickStairsReachTheSmelterTwoRecipesDown` — com o código antigo os dois pedem **0** (mutação
conferida); com a correção, 741/741 GameTests.

## Outros números da sessão

`time_ledger`: construtor 72% ocioso (esperando o tijolo), pastor 25% bloqueado, fundidor 53% em
espera, mineiro 0% trabalhando (11 desistências "got no closer"). `action_report`: o fazendeiro
ajudou a obra (94 blocos postos), e "the builder left the colony" uma vez. Ficam no `TODO.md`.
