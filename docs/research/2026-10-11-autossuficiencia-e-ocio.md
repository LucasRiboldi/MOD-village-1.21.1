# Autossuficiência e ócio das profissões — análise e alternativas (2026-10-11)

> Pedido do autor: *"busque opções de melhorias, visando que as profissões não fiquem ociosas ou
> bloqueadas, para evitar ao máximo que itens precisem surgir automaticamente, que alguma rota de
> profissão o produza ou recolha"*. Fonte: playtest 0.3.24 de 11-10 (10:04–11:44, mundo novo),
> `time_ledger.py`, `action_report.py`, `cost_ledger.py`, Spark `3Q6g9MrO1o`, log e código.
> **Isto é análise e proposta; nada aqui foi implementado.** Continua
> [`2026-10-08-tempo-ocioso.md`](2026-10-08-tempo-ocioso.md).

## 1. O que o playtest mostrou

| Ofício | ocioso + bloqueado | espera | O que o log diz |
|---|---|---|---|
| Construtor | **81%** | 0% | obra parada em `WAITING_RESOURCES` por `glass_pane` e `cobblestone` |
| Pastor | **68%** (44% bloqueado) | 7% | 27× "no sheep with wool within the radius" |
| Mineiro | **43%** | 1% | 31× "got no closer than N blocks" (preso na boca), 1 dos 3 minerou 2 blocos |
| Lenhador | **29%** (27% bloqueado) | 0% | 65% andando; árvores longe |
| Pedreiro | 22% | 35% | ajudando a obra: 128× `SET_ASIDE cobblestone` |
| Carpinteiro | 16% | **46%** | 72× "half of the colony's wood stays in logs" (tarefa de 0 peça) |
| Fundidor | 1% | **51%** | nunca fez vidro: nenhuma areia entrou na colônia |
| Fazendeiro | 10% | 36% | 36× "nothing ripe and no empty plot" |

**Itens que surgiram sozinhos** (regra da 4ª tentativa, `BiomeConstructionSupply.stock`, 1 unidade
por vez): **58 pedregulhos** e **16 vidraças**.

## 2. A descoberta principal: a vila fabrica o que ninguém pediu e falta o que a obra pede

Estoque no fim da sessão (`stores {...}` do ciclo):

| Peça | Fim | | Matéria-prima | Fim |
|---|---|---|---|---|
| `cobblestone_slab` | **318** | | `cobblestone` | **15** |
| `smooth_stone_slab` | **282** | | `stone` | 21 |
| `oak_stairs` | **356** | | `oak_log` | 50 |
| `oak_planks` | 151 | | | |

A obra deixou pedregulho de lado **252 vezes** (pedreiro, mineiro e pastor ajudando) enquanto 318
lajes estavam nos baús.

**Causa (código):** `AdvanceStock.addTo` (ADR-036 item 8, ADR-038 P5b) dá a cada peça antecipada a
meta `estoque atual + 5`. Quando a peça a alcança, o cursor passa à seguinte com `estoque dela + 5`,
e assim em roda, **sem teto**. Três raias giram ao mesmo tempo: pedreiro (laje de pedregulho, tijolo
de pedra), fundidor (vidro, pedra lisa) e carpinteiro (escada, laje e cerca de carvalho). O que
amplifica:

- A receita da laje rende 6 e a tarefa pede 1. O `TaskChain` reabre o pedido assim que um termina,
  lendo o `LastShortage` do ciclo anterior (até 30 s velho): 58× "chained ... the shortage remains".
- `STONE` e `SMOOTH_STONE` estão no grupo `NONE`. O pedregulho que o fundidor assa sai do grupo que
  a meta de pedra conta, e a meta pede mais pedregulho.
- A reserva do fundidor (`StockRules.rawToKeep`) guarda metade do cru, não o que a obra pede.

É a raiz comum do construtor parado, do pedreiro com `SET_ASIDE` e do pedregulho que surge sozinho.

## 3. Demais causas, por ponto

**Areia e vidro.** Só o fundidor coleta areia (`ProfessionRegistry:44-45`), fora da vila, num
setor só, com raio de 48 (`SurfaceGatheringWork`). A rota é aprovada por `sandNearWater`, que não
olha a Regra 3, o ar em cima nem o setor. O `SandPatch` recusa tudo isso: a rota existe "no papel".
Nesta sessão nenhuma tarefa de areia chegou a ser pega (0 no diário; a única linha é "no surface
gathering work: no task open for it", às 10:09). **A confirmar com uma linha de diagnóstico:** se a
meta `SAND` nasceu e foi filtrada, ou se `WorkMaterials.glass` devolveu 0 com as vidraças
adiadas. O `SandGathering` do mineiro está ligado só a tarefas de grupo `SAND` que o ciclo nunca
gera, então é caminho morto.

**Pedra.** A mina cavou 1022 blocos, mas só a tarefa conta o recurso pedido (`MinerHands:135-144`).
Granito, andesito e diorito vão ao baú e não entram na meta, embora `EquivalentPieces` os aceite
na peça. Um mineiro por ramal (`MineClaims`, 4 ramais). Quem fica sem ramal cai na pedra exposta.
Hipótese para o mineiro preso: `MineEntrance.legDown` devolve vazio sem cadeia de entradas até o
nível do alvo, e ele anda em linha reta até a boca.

**Construtor.** A obra é única (`ConstructionService.register:133`). Em `WAITING_RESOURCES`, o
`ensureTask` não abre tarefa e o `BuildHelper` só ajuda em `BUILDING`. A regra do ócio
(`IdleYield`, `MIN_WORKERS=2`) nunca solta um construtor sozinho.

**Pastor.** Tosquia só por pedido. Sem ovelha lanuda no raio, solta a tarefa em 30 s e o ciclo a
reabre. Procria a cada 6000 tiques até 12 adultos.

**Lenhador.** O corte conta como trabalho (`TreeFelling` balança o braço a cada 5 tiques). O
"bloqueado" é a perna da caminhada parada (`WalkLegs`) até árvores longe.

**Carpinteiro.** O portão "metade da madeira fica em tora" fecha. `CraftingWork.finish` marca a
tarefa como **concluída com 0 peça** e o `TaskChain` reabre outra igual (72 vezes).

**Fazendeiro.** Não ara canteiro novo nem usa farinha de osso. A semente vem só do baú dele
(`FarmerChores:88`), não dos baús da colônia.

**Desempenho (bônus).** 72% do custo do mod no Spark é `SmelterWork.mayStillSmelt`, chamado dentro
do laço de baús: baús² × crus leituras por peça.

## 4. Alternativas por ponto

### P1 — Produção antecipada sem teto (raiz)
- **1a. Teto fixo por peça:** adianta até N (ex.: 32) e para. Simples, mas não sabe o que vem.
- **1b. Teto pela próxima obra (recomendada):** adianta só o que a planta seguinte vai pedir (a obra
  aberta + a próxima do sorteio), e só com cru acima da reserva da obra aberta.
- **1c. Adiantar só no ócio:** sem meta numérica. O ofício adianta quando não há pedido da obra, e
  nunca com cru que a obra aberta espera.
- **1d. Tirar a produção antecipada:** só sob pedido da obra. Volta a espera de 30 s entre pedidos.

### P2 — Areia e vidro
- **2a. Consertar a rota que existe (recomendada):** alinhar `sandNearWater` ao `SandPatch`
  (Regra 3, ar, setor), buscar em mais de um setor e por pernas, e coletar no ócio por estoque.
- **2b. Mineiro traz areia e cascalho:** ligar o `SandGathering` aos bolsões que a mina encontra
  e às margens de rio.
- **2c. Britador (como o MineColonies):** o pedreiro mói pedregulho em cascalho e cascalho em areia
  (2:1). Fecha o ciclo em qualquer bioma, mas é uma mecânica fora do Vanilla.
- **2d. Janela equivalente:** sem areia no bioma, a vidraça vira peça Vanilla equivalente
  (`EquivalentPieces`), por exemplo alçapão ou cerca.

### P3 — Pedra para a obra
- **3a. Rochas da mina contam (recomendada):** granito, andesito e diorito entram na meta e na
  entrega, como já entram na peça.
- **3b. Reserva da obra antes:** fundidor e pedreiro só usam cru acima do que a obra aberta pede (hoje
  a reserva é metade).
- **3c. Mais frentes de mina:** um ramal ou nível por mineiro. Quem fica sem ramal tem prioridade
  na pedra exposta da superfície.
- **3d. Corrigir o mineiro preso na boca** (a perna da escada até o nível do alvo). Complementa as
  outras.

### P4 — Construtor ocioso
- **4a. Segunda obra quando a primeira espera** (muda a arquitetura da vaga única).
- **4b. Prepara o próximo lote** enquanto espera: limpar, terraplanar, fundação.
- **4c. Busca o próprio material:** pega do chão, carrega dos baús profissionais e, como coletor,
  busca a areia ou a pedra que falta.
- **4d. Manutenção da vila:** reparo, ruas (já calça), tochas, cercas.

### P5 — Pastor
- **5a. Rebanho ativo:** procria até o teto com trigo do fazendeiro, traz ovelhas soltas ao curral e
  tosquia por estoque.
- **5b. Pastor ajudante:** sem lã a fazer, colhe para o fazendeiro ou carrega itens.
- **5c. Vaga por demanda:** a regra do ócio solta também o titular único quando há falta noutro
  ofício.

### P6 — Mineiro
- **6a.** Corrigir a perna da escada (igual à 3d).
- **6b.** Um ramal ou nível por mineiro.
- **6c.** O mineiro sem ramal vira carregador da mina (do baú da boca ao armazém) ou trabalha na
  pedreira de superfície.

### P7 — Lenhador e carpinteiro (madeira)
- **7a. Bosque-fazenda:** mais mudas perto (o `VillageGrove` tem 6), replantio sistemático, corte
  só do bosque quando ele está maduro.
- **7b. Perna travada com desvio:** `WalkLegs` com o `DetourPlanner` do mineiro, e a árvore cuja
  rota falhou fica marcada.
- **7c. Reserva de tora pela obra:** o portão de metade vira o que a obra pede em tora. A tarefa de
  0 peça não conta como feita nem encadeia.

### P8 — Fazendeiro
- **8a.** Arar e semear canteiro novo ao lado da roça, até um teto.
- **8b. Compostor Vanilla:** semente e planta sobrando viram farinha de osso, que acelera a lavoura e
  o bosque. Fecha o ciclo sem item do nada.
- **8c.** Semente de qualquer baú da colônia (correção).

### P9 — A regra da 4ª tentativa (item que surge)
- **9a.** Manter como último recurso, com prazo maior e só quando nenhuma rota tem executor.
- **9b.** Manter, mas contar no `time_ledger`/`action_report` como falha de rota (régua: 0 por
  sessão).
- **9c.** Remover depois que P1–P3 estiverem medidos (sem ela, falta vira espera visível).

## Escolha do autor (2026-10-11)

| Ponto | Escolhido |
|---|---|
| P1 | 1b, teto pela próxima obra |
| P2 | 2a, consertar a rota atual; 2c, britador do pedreiro; 2d, janela equivalente |
| P3 | 3a, rochas da mina contam; 3b, reserva da obra primeiro; 3d, mineiro preso na boca |
| P4 | 4b, prepara o próximo lote |
| P5 | 5a, rebanho ativo; 5c, vaga por demanda |
| P6 | 6b, ramal ou nível por mineiro; pedreira de superfície |
| P7 | 7a, bosque-fazenda; 7b, perna com desvio; 7c, reserva de tora pela obra |
| P8 | 8b, compostor Vanilla; 8c, semente de qualquer baú |
| P9 | 9a, último recurso com prazo maior |
| Ordem | tudo implementado antes do playtest, que mede tudo de uma vez |

## 5. Correções sem escolha (defeitos)
`SmelterWork.mayStillSmelt` fora do laço de baús; tarefa de 0 peça do carpinteiro; semente só do
baú próprio; linha de diagnóstico da meta de areia.

## Fontes externas
- MineColonies, Crusher's Hut (pedregulho → cascalho → areia, 2:1): https://minecolonies.com/wiki/buildings/crusher
- MineColonies, Glassblower e Courier (vidro sob pedido; carregador):
  https://minecolonies.com/wiki/buildings/glassblower, https://minecolonies.com/wiki/buildings/deliveryman
- RimWorld, piso de estoque e prioridade de transporte (fórum do desenvolvedor):
  https://ludeon.com/forums/index.php?msg=17802, https://ludeon.com/forums/index.php?msg=299991
