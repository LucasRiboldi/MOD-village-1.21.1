# Erros, inconsistências, conflitos e decisões pendentes — 2026-10-07

Fotografia depois das ADR-036, 037 e 038 (JAR 0.3.3). Cada item diz o que é, onde está e o
que se pede ao autor. Nada da 037 e da 038 foi visto em jogo.

## A. Erros conhecidos

| # | O quê | Evidência | Pede |
|---|---|---|---|
| A1 | **Testes intermitentes da bateria** (KF-003): carpintaria `craft_stock` e `craft_family`, fundidor da boca da mina, `FarmPlanGameTest` | **07-10 manhã:** os de fabricação e o do fundidor mediam num tique fixo cedo demais; passaram a medir perto do limite e 9 baterias seguidas deram 638/638. Ainda não prova (9 limpas por acaso ≈ 35%); `FarmPlan` sem causa | repetir até ~30 baterias limpas antes de fechar |
| A2 | **A ordem da bateria derruba testes de outras classes**: criar um lote (`batchId`) novo mudou a disposição das arenas | virou regra no `known-failures.md`: teste novo vai para lote existente | — |
| A3 | ~~`walk` = 0% no `time_ledger`~~ **corrigido 07-10:** o servidor nunca baixa o braço do aldeão, então um golpe contava trabalho para sempre | `WorkTime.swungSinceLastSample`, provado por mutação | conferir no próximo playtest que `walk` aparece |
| A4 | Picos de 100 ms por tique do playtest de 07-10 | corrigidos (F1–F3), **não confirmados em jogo** | playtest |

## B. O que foi feito sem teste que prove

| # | O quê | Onde |
|---|---|---|
| B1 | O mineiro que raspa a superfície não invade o poço (a mutação sobreviveu) | `MineDigging` → `MineVein.exposedStone` |
| B2 | ~~Limite de 6 blocos para fechar portas~~ **coberto 07-10** por `HouseDoorsGameTest` | `HouseDoors` |
| B3 | Artesão em volta do sino quando não há bancada | `CraftStation` |
| B4 | Ofício parado cedendo vaga (só a regra tem teste; o gancho na janela de 5 min não) | `WorkTime.yieldIdleTrades` |
| B5 | O pastor **andando** com o animal na corda (o teste põe o pastor no lugar) | `ShepherdHerding` |
| B6 | Boca da mina: preferência de morro separada da de água | `MineEdge.inside` |

## C. Memórias que se perdem ao fechar o mundo

`VillageTrees` (árvores e mudas), `SurfaceHits` (onde achou recurso), rastro do veio, cursores
do adiantamento, coletas do pastor em andamento. Todas se reconstroem jogando, mas custam
busca de novo a cada sessão. **Pede:** decidir se alguma vai para o save.

## D. Ficou pela metade (decidido, não feito)

| # | O quê | Por quê |
|---|---|---|
| D1 | Carpinteiro adiantando peças | escada e laje de madeira não são recurso acompanhado; tora descascada gastaria a tora da obra |
| D2 | Construtor calçando o caminho até a obra | traçar caminho no terreno (desnível, água, peças da vila) pede desenho próprio |
| D3 | Ramais rumo ao minério de que a vila precisa | a geometria dos ramais é fixa; pede decisão de como escolher o rumo |
| D4 | Galerias e veio no save | ver C |

## E. Conflitos entre regras

| # | Conflito | Como está | Pede |
|---|---|---|---|
| E1 | P7 pediu 5 min de espera depois de "nada no raio" × decisão de 03-10 (1, 2 e 4 min, porque o material aparece no baú na 4ª busca) | ficou 1/2/4 | escolher |
| E2 | P2c: o pastor só cria animal amarrado ou em curral × antes qualquer ovelha perto procriava | ovelha solta não procria | confirmar |
| E3 | R1: a ceia doa comida mesmo sem cama sobrando × a comida fica no bolso de quem não pode ter filho | doa sempre, até 12 pontos por adulto | confirmar |
| E4 | P3b: boca da mina até 5 blocos **dentro** da vila × a escada desce para fora, mas o buraco fica no chão da vila | dentro, com a Regra 3 protegendo as peças | confirmar |
| E5 | Felicidade: os limiares (12/4 pontos, 2 camas, 1 obra, convívio) ainda são do agente | ADR-036 C7 + ADR-037 R1 | revisar com o próximo playtest |
| E6 | Até 8 animais por cerca, 12 por espécie: números do agente | `ShepherdHerding.MAX_TIED`, `ShepherdFlock.FLOCK_TARGET` | revisar |
| E7 | Regra 33 (o JAR vai para `downloads/`) × ADR-036 26 (o JAR saiu do git) | `downloads/` virou cópia local; a versão vai para o GitHub | confirmar o texto da Regra 33 |

## F. Decisões abertas antigas

| # | Pergunta | Onde |
|---|---|---|
| F1 | P0.7 — aceitar pedra como solo de lote? | `docs/RULES.md` |
| F2 | O que fazer com a branch `codex/bighousemod` (o `Barn_Majest.nbt` dela difere) | GitHub |
| F3 | A branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`), já toda na `main` | máquina do autor |
| F4 | `TODO.md` com 1.771 linhas, boa parte histórica: arquivar o que está feito? | auditoria P1-05 |
| F5 | `docs/workers-analysis/` órfã (ninguém aponta) | auditoria |
