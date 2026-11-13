# Pendências herdadas do TODO de 13-11 — triagem feita

> Os 214 itens abertos (`- [ ]`) do `TODO.md` de 13-11, agrupados pela seção de origem. **Triados em 13-11:** cada um diz no fim se foi fechado (e por quê) ou movido para uma fase do `TODO.md` vivo. Nenhum ficou aberto aqui; o arquivo fica como registro da triagem e vai para `docs/archive/` no próximo ciclo. O TODO inteiro, com os feitos, está em `docs/archive/technical/TODO-ate-2026-11-13.md`.

## Regras novas do autor, 12-11 — escolhas feitas, nesta ordem

- [x] Mineiro sem ramal livre (solução 1 da reanálise de 12-11, pulada pelo autor): "every open branch is taken or — **fechado: duplicado de "Mineiro sem ramal livre" no `TODO.md`.**
  finished" em todas as versões recentes; no primeiro nível, antes do salão, ele só espera.

## Blocos que surgem sozinhos — buscar fluxo de fabricação (regra do autor, 04-11)

- [x] `prismarine_shard` ×16 e `prismarine_crystals` ×16: "drop ingredients appear on their own" — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**
- [x] `white_dye` ×12 e `yellow_dye` ×2: idem; caminho possível: flores do bioma, com o pastor — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**
- [x] trigo: `supplied` em `second_failure`, 3 vezes (corrigido na 0.4.18; conferir que parou) — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] terra sob caminho voando (`PathSupport`, linha "dirt under the path"): 0 no jogo da 0.4.24 (6 h) — **fechado: 0 ocorrências no jogo de 6 h da 0.4.24 e nenhuma depois.**

## Abertos do jogo da 0.4.28

- [x] Conferir em jogo a 0.4.29: "into the pen" no log, `VC_PENS` com animais, nós de corda vazios saindo das cercas — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Conferir `CLEARED_LEAF` no diário e o `FELLED_TREE` começando pelo centro; "a structure stands within" no log — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Fazendeiro preso na casa de x125–131, z-253…-247: 40 min sem colher; causa não achada (autor: não mexer agora) — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Vila sem obra: `project=none` em 10 de 14 relatórios, 23 camas para 18 aldeões; ócio de carpinteiro, pedreiro e construtor — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] MAE: conferir em jogo a limpeza (`VC_MAE_SITE result=clearing`, fase `clearing` no chat) e a obra seguinte; a prova por — **movido: F1, item MAE do `TODO.md`.**
  seção pode recusar por mudança natural: `changed_generation_evidence`. Na região, areia e cascalho podem cair e barrar
- [x] Mineiros: 0 de 16 tarefas de minério fechadas, `raw_iron` ×57 surgido — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**

## Abertos do jogo da 0.4.30

- [x] MAE: primeira obra em jogo na 0.4.31; durante a obra a prova ainda é por seção, e plantação ou aldeão na mesma seção pode — **movido: F1, item MAE do `TODO.md`.**
  parar com `world_changed_since_approval` ou `changed_prepared_terrain`
- [x] MAE: cada comando trava o servidor por cerca de 2,5 s (`MaeSites.start`, 22,6 s em 9 tentativas) — **movido: F2, custo do MAE do `TODO.md`.**
- [x] Pastor: 15 `dropped the herd in phase LEAD` e 3 em `ENTER` contra 12 entregas; 2 "did not walk into the pen" — **fechado: tratado pela Regra 269 (0.4.74), prazo pela distância; conferir no jogo.**
- [x] Fundidor: 24 soltas de `smooth_sandstone` e 11 de `terracotta`, 22% bloqueado; vila sem obra (`project=none`, `lots_ok=0`) — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Mineiros: 5 de 6 tarefas soltas — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**

## Testes que faltam (bateria de 2026-11-05: 1123 GameTests e 1745 unitários)

- [x] 2 regras sem teste (33 e 47, processo e comportamento do jogo) e 95 em que um teste só usa a classe indicada: [`regras-e-testes.md`](docs/testing/regras-e-testes.md) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
- [x] Teste só parcial: Regra 178 (falta o anel e o nivelamento do pedreiro), 174 (falta `DryLand` e a casa ligada a seco), 172 e 171 (falta o atributo em jogo) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md` (a Regra 174 foi revogada pela 264).**
- [x] `animalsWalkInThroughEachOfTheFourGates`: 2 falhas em 12 rodadas da bateria inteira, 0 em 7 isolado. Na falha pega, a vaca estava a 9 blocos do portão perto do prazo; causa não achada. Portão fechando foi descartado: fica aberto 600 tiques — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
- [x] `GroundPickupGameTest.workerCollectsWhileWalkingPast`: 1 falha em 12; suspeita, não provada: relógio global fora do expediente (Regra 194) — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**

## Abertos da vila viva (0.4.41)

- [x] Conferir em jogo: nascimentos por dia, obras repetidas, `UPKEEP` no diário e `VC_COST` com a rotina dos ociosos — **movido: F1, nascimentos do `TODO.md`.**
- [x] Conferir em jogo se a manutenção e o desgaste tiram tábua de obra; o trigo assado pode faltar a obra que peça fardo — **movido: F1, nascimentos do `TODO.md`.**
- [x] Sem teste próprio: quem não tem ofício tirando neve (Regra 220) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
- [x] Instáveis: `workerCollectsWhileWalkingPast` falhou 1 em 5 baterias depois de separar os dois testes em lotes (o item some sem chegar ao baú dele), e `animalsWalkInThroughEachOfTheFourGates` falhou 3 em 12 nesta série; a causa da vaca não foi investigada — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**

## Abertos da varredura de 11-06 (0.4.40)

- [x] Conferir em jogo: rua e lote novos com caminho só pedido; casas depois do lago ligadas com ponte; pedreiro com `PAVED` e `LEVELLED`; `/vc mae obra` mais aceita — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Save antigo: nunca jogado com a releitura ligada — **fechado: duplicado de "Saves de 2 ou 3 versões publicadas" no `TODO.md`.**
- [x] `Corridor` e `StripLot` ainda planejam forma própria de caminho antes de pedir ao grafo — **movido: F1, grafo do `TODO.md`.**
- [x] Sem teste próprio: a desistência por espera longa da obra pedida, `MaeSites.move` e a busca de lote pela rede do grafo — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**

## Abertos do jogo da 0.4.38

- [x] Conferir em jogo: marcador branco em toda estrutura e no portão da mina, nenhum `traced a path`, pedreiro com `PAVED`, e o grafo sem queda como a de 14:14 (132 para 114 nós) — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] O plano do caminho vive só na memória: reiniciar o jogo traça de novo, um por releitura — **movido: F2, estado só em memória do `TODO.md`.**
- [x] `/vc mae obra` recusada mostra só o motivo do último lote testado (`overlapping_request`, `protected_volume`) — **movido: F1, item MAE do `TODO.md`.**
- [x] Mineiro: 7 desistências em `the way down got no closer`; pastor: 12 vezes `fenced in` — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Surgidos, `VC_SUPPLY`: `raw_iron` ×16 (`WAITED_TOO_LONG`), `honeycomb` ×1, enchimento de lote ×37 — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**

## Abertos do jogo da 0.4.36

- [x] Conferir em jogo a 0.4.37: entrada branca em toda estrutura e no portão da mina, casa do mineiro ao lado da boca em uso, nenhuma tocha nova no chão, lote aceito com vela e tocha — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Ócio de vila parada no jogo da 0.4.36 (`time_ledger`): `MASON` 81%, `CARPENTER` 78%, `SMELTER` 74% e `SHEPHERD` 83%, com `project=none lots_ok=10707`. Falta achar por que nenhuma obra abre — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Mineiro: 141 `NO_PATH` e 46 `UNCLASSIFIED`; `VC_MINE_BRANCH` falhou 50 de 50 antes de voltar à boca — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Surgidos, `VC_SUPPLY`, para buscar fabricação: `AUTOMATIC_DROP` `prismarine_shard` ×596, `honeycomb` ×417, `string` ×298, `magma_cream` ×64, `black_dye` ×51; `WAITED_TOO_LONG` `raw_iron` ×151; `MISSING_PIECE` `clay` ×8, `decorated_pot` ×4, `anvil` ×3 — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**
- [x] O carpinteiro faz 16 velas de cada uma das 17 cores para o estoque, e nada as usa; ver se o estoque precisa delas — **movido: F1, velas do `TODO.md`.**

## Abertos do jogo da 0.4.35

- [x] Casas de x=45 a 65, depois do lago: 7 portas térreas com frente certa, mas a parte não liga à rede (`isolated`=4 a 5, de 54 a 87 blocos do grafo). Falta o diagnóstico "MAE e lago" — **fechado: com a Regra 264 (13-11), parte separada por água fica sem ligação por decisão do autor.**
- [x] Nenhuma obra nova: `project=none lots_ok=0`, construtor `NOT_IN_GAME` por `HousePlans.plansForNext` vazio, 23 camas para 19 aldeões; saturação não verificada — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Profissões menos ativas: pedido do autor, ainda sem medir — **fechado: medido na análise `docs/audit/2026-11-11-atividade-das-profissoes.md`.**
- [x] Surgidos, `VC_SUPPLY`: `grass_block` ×64 (`FOUNDATION_GROUND`), `prismarine_shard` ×9, `honeycomb` ×2 — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**
- [x] `LINKS_PER_READ`=1 e a ligação que falha gasta a vez: as partes isoladas atrasam o ramal das outras — **movido: F1, grafo do `TODO.md`.**

## Abertos do jogo da 0.4.32

- [x] MAE: conferir em jogo a 0.4.34 (Regras 204 e 205); no save atual, rodar `/vc mae soltar` uma vez para as 2 regiões já falhas — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Grafo: conferir em jogo a 0.4.36 (Regras 206 a 209): até 4 azuis e nenhum no centro; plantação sem anel e com uma entrada; anel antigo de volta ao solo; ferreiro de armas (121,66,-162) com entrada — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Surgidos, `VC_SUPPLY`: `prismarine_crystals` ×80, `prismarine_shard` ×76, `grass_block` ×10 (`FOUNDATION_GROUND`), `honeycomb` ×7, `terracotta` ×2 (`WAITED_TOO_LONG`), corantes de 1 a 2 — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**

## Jogo da 0.4.42 (Spark 7d0PErq0RJ, 2 janelas de 5 min e 9,7 min de diário: amostra curta)

- [x] Mineiro 61% parado: 9 `GAVE_UP` "has not moved a block", 21 `NO_PROGRESS` e 3 `STRANDED` "frozen twice" — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Fundidor 83% parado: 35 tarefas soltas porque a argila está presa pela reserva; 14 sem `clay_ball` no raio; `VC_SUPPLY_ERROR` de `clay_ball` 2 vezes — **fechado: tratado pela Regra 257 (0.4.63), areia e argila sem reserva de cru.**
- [x] Pastor 67% parado, e 8 vezes "fenced in", quase todas no curral `105, -231`. Só a 0.4.43 tira de lá o nó do poste; pode haver outra causa — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Construtor 38% parado; `VC_SUPPLY_ERROR` de `bricks`: "no route delivered it" — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Sem ofício: 94% ocioso, 3017 s. A rotina da Regra 219 conta como ócio; a Regra 51 os deixa de fora — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Fazendeiro: 54% andando, 42% esperando e 2% trabalhando; tarefa aberta 580 s sem progresso — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Carpinteiro e pedreiro: 33% e 34% esperando; três carpinteiros 100 s sem progresso em `oak_planks`, com `OAK_PLANKS` parado em 51 — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] `COBBLESTONE` no log de estoque foi de 554 a 13.579 em 16 min, enquanto 4 mineiros cavam 73 blocos por minuto. Conferir se é leitura de baús ao carregar ou sobra real, e se falta teto de produção — **movido: F2, estoque sem teto do `TODO.md`.**
- [x] `VC_REACH`: 3 de 103 lugares fora de alcance, os baús `108,68,-287`, `107,68,-293` e `50,58,-118` — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Grafo: `isolated` entre 7 e 11 em todo o jogo, e `toPave` subiu de 26 a 64 no fim — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] `VC_GATHER`: 13 de 25 na reunião do dia — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Custo: ciclo de 115 ms em média, com `workers` 49 ms, `detect` 32 ms, `population` 13 ms e `planner` 13 ms; pico de 182 ms no tique. Sem pressa: 20 TPS e 13,3 ms por tique — **fechado: duplicado de "Ciclo da colônia repartido entre tiques" no `TODO.md`.**
- [x] Biblioteca com duas portas a 2 blocos: dois brancos colados, por serem duas portas reais; decidir se viram um — **movido: Decisões do autor do `TODO.md`.**

## Jogo da 0.4.43 (Spark u6XC34ZC0k, 63 min, vila nova em 1270, 91)

- [x] Quina de plantação em cima da rua segue branca: `1242, 63, 66`. `RoadGraph.hasLineNear` ignora toda aresta que termina no ponto, e na rua as duas terminam nele — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Branco sem porta em `plains_accessory_1`: `1286, 72, 102`. Decidir se essa peça é enfeite (Regra 223) — **fechado: resolvido pela Regra 252 (0.4.58), acessório sem entrada.**
- [x] Socorro cedo (Regra 222): disparou às 10:09:52 depois de falhar pedreiro e armeiro, e a busca normal abriu obra 29 s depois. O jogador parou a obra às 10:10:09 — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] 230 colunas de rua cobertas (`VC_ROAD_COVERED`), 218 na primeira leitura: grama um bloco acima do caminho antigo. Conferir se o solo suave da geração enterra rua — **fechado: tratado pela Regra 250 (0.4.57), rua no chão novo.**
- [x] Madeira: 93 `SET_ASIDE` do construtor (`oak_log` e `stripped_oak_log` 29 cada, `oak_planks` 17), com lenhador 40% ocioso e árvore a 38 blocos na mediana, 171 no máximo — **fechado: tratado pela Regra 268 (0.4.73), bosque em qualquer altura; conferir no jogo.**
- [x] Nenhum nascimento em 63 min: 19 aldeões em todos os `VC_STATE`, com 28 camas. Nenhum `UPKEEP` no diário — **movido: F1, nascimentos do `TODO.md`.**
- [x] Fundidor: 1742 `smooth_stone` fundidos e 538 em estoque; outro preso em `terracotta` 285 s (badge `STUCK`) — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Pastor 8 vezes "fenced in", e 45% parado — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] 344 de 795 colunas de caminho fora da linha, e 118 colunas da linha ainda sem caminho — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Blocos que surgiram, `VC_SUPPLY`: `prismarine_crystals` ×80, `prismarine_shard` ×64, `white_terracotta` ×62, `honeycomb` ×29, `oak_log` ×39, `stripped_oak_log` ×16, `glass_pane` ×7, `white_wool` ×7, `string` ×4 — **fechado: duplicado de "Recurso que surge" no `TODO.md`.**

## Jogo da 0.4.53 (deserto, 2026-11-11, Spark Lk0DyOz8CK)

- [x] Pastor 34% bloqueado (o curral não é a causa: sai pelo portão em 4–39 tiques; O4a, Regra 196, já existe): "fenced in" 4 vezes, "no sheep with wool within the radius" 4 vezes — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] 11 trabalhadores registrados fora do mundo; carpinteiro 9467a1fc e pedreiro 3aab7d8a "not loaded" o jogo inteiro, com tarefa aberta — **fechado: tratado pela Regra 244 (0.4.55), fora da simulação não congela; conferir no jogo.**
- [x] Bateria instável em 12-11: `workerCollectsWhileWalkingPast` (aldeão para a 1 bloco do item) e `eachSpeciesGetsItsOwnPen` (40 tiques) falham só na bateria inteira, um ou outro por rodada, também sem a mudança da 0.4.56; isolados passam 2 de 2. Achar a interferência — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
- [x] O3b: no deserto o bosque do lenhador (`lumberjackInTheGrove`) não tem muda; plantar pede muda de fora. Esperando escolha do autor — **movido: Decisões do autor do `TODO.md`.**

## Jogo da 0.4.52 (deserto, 2026-11-11, Spark CverydmXKv)

- [x] `time_ledger` conta como ócio quem arruma o chão (pedreiro, carpinteiro e fundidor): o `LEVELLED` deles não marca trabalho — **fechado: tratado pela Regra 245 (0.4.55), chão é trabalho.**
- [x] Pastor 48 vezes "fenced in"; fazendeiro 29% encalhado — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] MAE ainda sem prova: nenhum `/vc mae obra` no jogo — **movido: F1, item MAE do `TODO.md`.**
- [x] Spark CverydmXKv não lido — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**

## Pastor, lã e plantação grande (0.4.51, Regras 239 a 241)

- [x] Conferir em jogo: coelhos no deserto, pastor procriando outras espécies, tapetes e lã de cor no baú, plantação grande pedida e semeada — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Sem corante nos baús a lã só vira tapete da cor que o rebanho deu: decidir uma fonte de corante (flor, cacto) se o autor quiser cores sempre — **movido: Decisões do autor do `TODO.md`.**
- [x] Melancia, abóbora e jarro fora da plantação grande até o fazendeiro saber colher fruto de caule — **movido: Depois da 1.0, frutos de caule do `TODO.md`.**
- [x] Investigar: `animalsWalkInThroughEachOfTheFourGates` caiu nas 4 baterias desde a 0.4.50 e passa isolado; ver se as entidades dos GameTests novos atrasam o passo das vacas — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
- [x] Conferir em jogo a Regra 242 (0.4.52): caminho coberto e caminho sem marca voltando ao solo pelas mãos de pedreiro, carpinteiro e fundidor; `offLine` caindo no `VC_ROAD_GRAPH`; rampa entre as bases sem degrau maior que 1 — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Sem teste próprio: o fundidor nas ajudas de chão (`VillageHelpers.helps`) e o aterro da rampa entre bases em mundo — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**

## Profissões mais ativas (0.4.50, Regras 235 a 238)

- [x] Conferir em jogo com `time_ledger.py` e `action_report.py`: mineiro do deserto trabalhando na pedreira; fazendeiro compostando; pastores e rebanho — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Sem teste próprio: a ligação da pedreira em `MineDigging` (a busca está testada) e o teto de pastor gravado por `ShepherdWork` — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
- [x] M2 (segundo ramal cedo) e C1 (adiantar a próxima obra) esperam decisão do autor: a escada é o único acesso ao nível, e a próxima obra só é escolhida quando o lote aparece — **movido: Decisões do autor do `TODO.md`.**
- [x] Instável: `animalsWalkInThroughEachOfTheFourGates` caiu duas vezes seguidas na bateria inteira e passou 3 de 3 isolado — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**

## Trecho travado em verde (0.4.49, Regra 234)

- [x] Conferir em jogo longo (10 dias de jogo, cerca de 3 h 20 min): trecho verde no `/vc mae mostrar ligar` e `fixed` acima de 0 no `VC_ROAD_GRAPH` — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Decisão em aberto: tirar o trecho travado do recálculo (afinamento e rota), conferindo só se o bloco de caminho existe. Muda a ADR-050; decidir depois de medir no Spark o custo da releitura do grafo — **movido: Decisões do autor do `TODO.md`.**

## Caminho sob laje e ponta na extremidade (0.4.48, Regra 231)

- [x] Conferir em jogo: caminho debaixo de laje e telhado no grafo; nenhuma ponta azul entre linhas; cada ponta na borda do grafo, apontando para fora — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Jogo da 0.4.47 (deserto, 2026-11-10, Spark BVayJOTalV), `time_ledger`: fundidor 59% bloqueado; mineiro 0% trabalhando e 38% esperando; fazendeiro 47% esperando; carpinteiro 68%, pedreiro 62% e pastor 69% ociosos. Diagnóstico não feito — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] `VC_ROAD_GRAPH` da 0.4.47: `offLine` de 276 a 517 e `toPave` de 142 a 236; `fixed` 0 (a trava de 10 dias não chegou) — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**

## Equilíbrio das profissões (0.4.47, Regras 232 e 233)

- [x] Conferir em jogo com `time_ledger.py` e `action_report.py`: menos `SET_ASIDE no material`, fundidor e carpinteiro menos ociosos, lenhador com menos `made no progress`, um segundo fazendeiro com comida baixa, e nascimentos acima de 1 por hora — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Sem teste: o diário com o produto feito (`CraftingWork.finish` com produto) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
- [x] Instável novo: `eachSpeciesGetsItsOwnPen` esgotou os 40 tiques uma vez na bateria inteira (0.4.47) e passou 3 de 3 isolado — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
- [x] Não feito desta vez: plantação nova a até 32 blocos e o pastor abatendo mais para comida (o abate acima de 6 animais já existe, Regra do `FlockHarvest`) — **fechado: a plantação grande em todo bioma (Regra 267) e o abate do `FlockHarvest` cobrem o pedido.**

## Pontas que levam a vila (0.4.46, ADR-052)

- [x] Conferir em jogo: 4 pontas azuis, uma por lado; cada uma andando 1 bloco por dia para fora; `VC_ROAD_TIP ... result=claimed` quando a rua sai 7 blocos — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Medir o custo da caixa maior: releitura do grafo, busca de lote e baús — **fechado: duplicado de "Perfil "carga" no Sentinela" no `TODO.md`.**
- [x] Sem teste: a zona só cresce com bloco de caminho de verdade na ponta (a conferência existe, mas nenhum teste a derruba) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**

## Grafo estável (0.4.44, ADR-050)

- [x] Conferir em jogo: `offLine`, `capped` e `fixed` no `VC_ROAD_GRAPH`; `isolated` perto de 0; o grafo parando de oscilar — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Passo 4 da pesquisa: devolver ao solo todo caminho fora do grafo. Decidir depois de medir `offLine` num jogo — **fechado: feito pela Regra 242 (0.4.52), caminho sem marca volta ao solo.**
- [x] A trava de 10 dias só aparece em jogo longo: 240.000 tiques, 3 h 20 min corridos — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Obra demolida não solta o ramal fixo enquanto o bloco de caminho existir — **movido: F1, grafo do `TODO.md`.**

## MAE: o que falta depois da 0.4.26

- [x] Medir em jogo a travada de `/vc mae obra`: `ms=` em `VC_MAE_SITE ... result=queued` e `result=refused` — **movido: F2, custo do MAE do `TODO.md`.**
- [x] Ruínas de saves que já abriram a 0.4.38 ou depois: as regiões antigas sumiram do save, e o mod não as acha — **fechado: saves de antes da 0.4.38; a F2 cobre saves de versões publicadas.**
- [x] Rodar o PIT: o core mudou na 0.4.43 (`RetiredSpurs`, `RoadSquares`, `RoadGraph`) — **fechado: o PIT roda no CI; 85,2% no `core` em 13-11.**
- [x] Primeira obra pedida em jogo pela fila do construtor (0.4.38, Regra 213): `/vc mae obra <modelo>`, Spark junto, `VC_MAE_SITE` de `queued` até `done`; conferir porta ligada pelo grafo e retomada depois de reiniciar — **movido: F1, item MAE do `TODO.md`.**
- [x] Obra longe da vila (`fora`): construtor chega lá? Só provado que a obra não é largada; caminhar até lá depende do jogo — **movido: F1, item MAE do `TODO.md`.**
- [x] Conferir em jogo a Regra 223 (0.4.43): branco na porta, poste sem branco, ramal voltando a grama, plantação sem quina ao lado da rua, porta com linha até a rua — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Chão de pedra em `x 114–119, z -175 a -166` (save Novo mundo): 6×10, uma camada sobre terra. Nenhum código do mod nem o diário explicam; a linha deixa de cruzá-lo, mas ele fica — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Cercas tiradas ao lado do portão `105, -231`, entre 00:14 e 00:49 do jogo da 0.4.42: sem registro no diário; saber se foi o autor — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Conferir em jogo o socorro do MAE (Regra 222): `VC_MAE_SITE result=rescue`, custo em ms, porta ligada pelo grafo e desenho da vila — **movido: F1, item MAE do `TODO.md`.**
- [x] Sem teste próprio: a volta que acha lote zerando a conta dentro de `LotSeeking` (a conta em si tem teste; a soma da volta sem lote tem `MaePatienceGameTest`) — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**

## Varredura dos ofícios (jogo da 0.4.24)

- [x] Mineiro: 54 desistências por distância e nenhuma tarefa fechada; conferir no próximo jogo com a 0.4.25 e a 0.4.28 — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Conferir `VC_LOOP` e `VC_GATHER` no próximo jogo: carpinteiro e pedreiro acima de 50% sem trabalhar — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] O guarda de laço cobre oficina e fornalha; coleta (lenhador, superfície) segue só com os prazos de sempre — **movido: F1, guarda de laço do `TODO.md`.**
- [x] Quando os testes forem liberados: `WorkClockTest`, `WorkHoursGameTest`, `OffHoursReleaseGameTest` e `BuildProgressGameTest` usam o expediente antigo; `TaskLoopGuard` e `MineSideWork` sem teste — **movido: F2, testes do expediente do `TODO.md`.**
- [x] Frase "none of N colony chests had" do fundidor mente quando o cru existe e a reserva impede — **movido: F4, mensagem do fundidor do `TODO.md`.**

## Abertos do jogo da 0.4.24

- [x] Pastor 86% ocioso: ler `VC_PENS` no próximo jogo e decidir o raio e o teto do `FencedIn` para o cercado grande — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
- [x] Fundidor travado em `minecraft:clay`: 76 `VC_SUPPLY_ERROR` "after four searches of the village's reach found none" — **fechado: tratado pela Regra 257 (0.4.63).**
- [x] Bocas antigas da mina não são lembradas depois de reiniciar o jogo: `MineEdge.PAST` só vale na sessão — **movido: F2, estado só em memória do `TODO.md`.**
- [x] Grafo em retângulos: hoje vem do anel do chafariz, das retas e da ponta que cresce; sem busca própria que feche quarteirão — **movido: Depois da 1.0, quarteirões do `TODO.md`.**
- [x] Conferir em jogo a 0.4.25: subida da escada da casa principal, ramal da mina, anel, quina, ponta e rua de um bloco — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**

## Abertos do jogo da 0.4.18

- [x] Spark `btHEJgIYWb`: `MaeGenerationEvidence.onChunkGenerate` custa 6,4% do tempo do servidor — **movido: F2, custo do MAE do `TODO.md`.**
- [x] `/vc mae obra` recusada duas vezes dentro da vila: `no_lot:original_structure` e `original_structure` — **movido: F1, item MAE do `TODO.md`.**
- [x] Caminho voando: origem do vão não achada (`PathSupport` corrige o sintoma); 8 blocos de caminho isolados no save — **fechado: o sintoma é tratado pelo `PathSupport`; 0 no jogo de 6 h da 0.4.24.**
- [x] Conferir em jogo a 0.4.19 e a 0.4.20: pulo, água, ponte, pastor, sinalizador, casa do mineiro, velas — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**

## Playtest de 03-11 — fila de construção

- [x] Validar 0.4.15: conservação da escada, reparo local de apoios e redução dos 21% de bloqueio/encalhe dos mineiros. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Trechos antigos sem geometria ativa ou rastro salvo não têm prova suficiente para reparo automático. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Confirmar no próximo playtest ausência das falsas derivas e progresso de outras obras. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Quando os testes forem liberados, cobrir busca parcelada, preservação do índice/cursor — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
      e liberação da fila após volta sem lote no Sentinela Local.
- [x] Retomar navegação e profissões após o recorte MAE priorizado pelo autor em 03-11; os diagnósticos anteriores permanecem pendentes. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**

## MAE — Fases 1 e 2

- [x] Medir custo de captura e escrita em carga real antes de ativar orquestração. — **movido: F1, item MAE do `TODO.md`.**
- [x] Validar carga real e playtest antes de qualquer orquestração automática MAE. — **movido: F1, item MAE do `TODO.md`.**

## MAE — Fase 3

- [x] Medir no playtest o tempo do pastor com a caminhada (`time_ledger`, motivos "gave up"). — **fechado: tratado pela Regra 269 (0.4.74).**
- [x] Exercitar `/vc mae obra` em jogo e ler `VC_MAE_SITE` no próximo playtest. — **movido: F1, item MAE do `TODO.md`.**

## Playtest de 02-11 — escolhas A aprovadas

- [x] Diagnosticar custo de geração e recalculo; processar lotes por tick. — **fechado: duplicado de "Perfil "carga" no Sentinela" no `TODO.md`.**
- [x] Conectar pórtico ao grafo, smoothing local e casa do mineiro com tocha laranja. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Garantir reserva exclusiva por bloco e frente exclusiva por mineiro. — **movido: F1, reserva da mina do `TODO.md`.**
- [x] Preferir terra e usar salto maior somente em recuperação. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Ao liberar testes, cobrir a suspensão no despacho `VillageHelpers.helps`; — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
      os GameTests legados chamam rotinas auxiliares diretamente e não provam essa restrição.
- [x] Restringir alternativas ao ofício; fazendeiro atende todas as fazendas, — **fechado: feito pela regra temporária de 02-11 (nenhuma profissão faz atividade de outra).**
      lenhador planta nas bordas sem árvore madura disponível.
- [x] Validar a separação dos ofícios no próximo playtest e concluir os demais itens do plano. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**

## Sentinela local de testes — 01-11-2026

- [x] Validar uma futura camada visual de cliente/GPU em spike Fabric isolada, — **movido: Depois da 1.0, Sentinela futuro do `TODO.md`.**
      no perfil `village-max`; não entra no fluxo diário sem prova reproduzível.
- [x] Avaliar publicação opcional Allure entre máquinas após necessidade real; — **movido: Depois da 1.0, Sentinela futuro do `TODO.md`.**
      plano em `docs/superpowers/plans/2026-11-02-sentinela-futuro.md`.
- [x] Avaliar laboratório serial de carga/JFR após definir orçamento de máquina; — **movido: Depois da 1.0, Sentinela futuro do `TODO.md`.**
      plano em `docs/superpowers/plans/2026-11-02-sentinela-futuro.md`.

## Rede e currais — implementação 0.4.3 em 02-11-2026

- [x] Explicar a falha intermitente de `PenEscape` antes da integração da rede e da condução. — **fechado: sem falha registrada de `PenEscape` desde então; os instáveis de curral estão em KF-012 e KF-013.**
- [x] Explicar o desvio intermitente pela água em `ColonyPathing`; o controle A/B/A não reproduziu a rota defeituosa. — **fechado: não reproduzido no controle A/B/A; reabre se aparecer.**
- [x] Revalidar ruas e migração no mundo novo antes de declarar o planejamento hierárquico concluído. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Provar condução física de pastor e animal; atualizar modelo, cultivos e crescimento local do curral. — **fechado: tratado pelas Regras 255 (0.4.61) e 269 (0.4.74).**

## Diagnóstico de terreno e trabalho — 01-11-2026

- [x] Reproduzir giro do mineiro e retorno repetido do lenhador com navegação concorrente, — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
      acesso obstruído e retomada após areia. Testes de descida e colheita completa reforçados
      em 01-11 passam; não demonstram a causa do defeito observado no save.
- [x] Confirmar no playtest a correção candidata: mineiro só libera alvo após falha de quebra; — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      fundidor retorna à área construída; ajuda comum espera 1.200 ticks de ociosidade própria;
      folhas e troncos não servem de piso; e lote novo limpa vegetação e árvores na margem de um bloco.
- [x] Investigar o reprodutor forense preservado em patch diagnóstico: caminho calculado não se traduz em chegada — **movido: F1, chegada física do `TODO.md`.**
      física em 140 ticks, inclusive com WorkTargets ativo no expediente. Separar geometria,
      tolerância de chegada e interferência do Brain antes de alterar o código de produção.
- [x] Investigar F19: condução da vaca ao cercado falhou no CI de push 41254270066, — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
      mas passou no CI de PR do mesmo código e nos 955 GameTests locais de 01-11.
- [x] Investigar a intermitência de `GroundPickupGameTest.aPassingWorkerPicksUpAllGroundItems`: — **fechado: registrado como instável em `docs/behavioral-tests/known-failures.md` (KF-011 a KF-014) e na tabela do `STATE.md`.**
      a bateria da 0.4.1 falhou em 1/979, mas a repetição isolada passou. Não houve
      alteração de produção para este sintoma.
      O CI 552 encontrou terra zero no baú original. Depósito em segundo baú reproduz
      o erro, mas o evento original ainda não foi rastreado. O teste agora registra
      remoção dos itens e estoques; conferir a próxima execução completa do CI.
- [x] Confirmar no CI completo a fixture hidratada de `BuilderGameTest.groundBlocksAreShapedWithoutTheChest`; — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      desidratação reproduziu a falha e os 35 testes isolados passaram após a correção.
- [x] Jogar o novo JAR em mundo novo e no save `Novo mundo`: medir grama flutuante, — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      transição do lote em degraus de 1 bloco e apoio sob o piso; conferir cortes em dunas e estruturas.
- [x] Observar pastor junto do animal ao alimentar e abater, reserva de 6 adultos por espécie, — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      currais intercalados de 15×15 e cercados existentes aproveitados.
- [x] Confirmar limpeza de árvores e copas nos novos lotes, folhas removidas durante a passagem, — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      itens recolhidos a 5 blocos e reposição de 16 produtos por tipo nos três ofícios.
- [x] Comparar o novo diário de profissões e Spark com o jogo de 31-10: pastor 3%, carpinteiro 11%, — **fechado: medida de ócio ou de tarefa de jogo antigo; a F1 mede de novo cada ofício contra o portão Beta.**
      pedreiro 15%, construtor 46% trabalhando; investigar esperas sem matéria-prima ou caminho.
- [x] Validar entrada da mina e ruas desconectadas: caminho natural quando o trecho da rede não for alcançável. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] Conferir produção contínua e reposição de 16 itens por tipo nos três ofícios, incluindo produtos de fornalha; — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
      manter prioridade das obras e baús nomeados reservados.
- [x] Repetir diário de trabalho: distinguir espera real de trabalho auxiliar sem marcação e medir NO_PATH/NO_PROGRESS. — **movido: F1, medida do diário do `TODO.md`.**

## Produção futura (estudo "Regras v2", 2026-10-22)

- [x] 🟠 **Configuração** (R-153/154) — desenho com decisões do autor em — **fechado: duplicado de "/vc config com interruptor por vila" no `TODO.md` (o desenho está em `docs/research/2026-10-22-desenho-configuracao.md`).**
      `docs/research/2026-10-22-desenho-configuracao.md` §8: por mundo, sobreposição por colônia
      (colônia → mundo → padrão), arquivo `saves/<mundo>/villagecolony/config.json` + `/vc config
      list|get|set|reset|reload`, operador nível 2, `VC_CONFIG` a cada mudança. F1 = tabela da §2
      inteira (14 ajustes) até o autor dizer outra coisa. Falta implementar.
- [x] 🟡 **Ausência com invalidação por evento** (R-021/R-027): `KnownAbsent` só expira pelo relógio — **movido: Depois da 1.0, contratos de tarefa do `TODO.md`.**
      (1 dia); invalidar quando o alcance cresce, aparece rota nova ou o terreno muda.
- [x] 🟡 **Contrato de tarefa** (R-047): prazo de progresso e critério de sucesso declarados na `Task`; — **movido: Depois da 1.0, contratos de tarefa do `TODO.md`.**
      centralizar os prazos hoje espalhados por ofício (R-050).
- [x] 🟡 **Estados de disponibilidade** (R-019): disponível, inacessível, não encontrado, — **movido: Depois da 1.0, contratos de tarefa do `TODO.md`.**
      temporariamente indisponível, não examinado — hoje espalhados.
- [x] 🟡 **Bateria de escassez** (R-158): falta total de madeira, água, semente, animal, combustível, — **movido: F2, bateria de escassez do `TODO.md`.**
      ferramenta, baú livre e lote; esperado: alternativa válida ou bloqueio explícito.
- [x] 🟡 **Animal inacessível não conta como plantel** (R-107). — **movido: Depois da 1.0, contratos de tarefa do `TODO.md`.**
- [x] 🟡 **Cobrir com teste o registro de cada suprimento** (Regra 111): só a fundação tem asserção. — **movido: Depois da 1.0, regras provadas só por classe do `TODO.md`.**
- [x] 🟢 **Área protegida pelo jogador** (R-035) — sistema novo; sem decisão. — **fechado: duplicado de "doMobGriefing ou gamerule própria" no `TODO.md`.**
- [x] 🟢 **Transporte físico de itens** (R-140 a R-142) — hoje a transferência entre baús é — **movido: Depois da 1.0, sistemas novos do `TODO.md`.**
      instantânea; sistema novo e grande; sem decisão.
- [x] 🟢 **Itens de outros mods** (R-026) — sem decisão. — **movido: Depois da 1.0, sistemas novos do `TODO.md`.**
- [x] 🟢 **Adaptador para versões futuras do Minecraft** (R-003/R-163) — o Core já não depende do jogo; — **movido: Depois da 1.0, sistemas novos do `TODO.md`.**
      só com ambiente da versão.

## 🔴 Biomas — o mod em todos os biomas (bancada de 20-10, 0.3.69–0.3.76)

- [x] 🟠 **Casa presa no último bloco — falta jogo.** Taiga: 44 ciclos em "1 blocks left". A causa — **movido: F1, biomas do `TODO.md`.**
      vista na bancada era a copa de pinheiro de fora do lote: 29 peças de parede puladas por
      "spruce_leaves is in the way" e a tocha da parede sem apoio. 0.3.77: folha natural dá lugar à
      peça; a peça inalcançável surge depois de 3 vezes de lado (`UnreachablePieces`,
      `villagecolony/unreachable-pieces.jsonl`, `VC_UNREACHABLE`). Bancada: taiga 0 → 2 casas
      concluídas, nota 7,6. Pendente: tronco natural no volume ainda é pulado ("spruce_log is in the
      way", 3×); peça de lado por apoio (`UNSUPPORTED`) que nunca ganha apoio ainda segura a obra.
- [x] 🟠 **Deserto jogado (0.3.77, 20-10, Spark IgpFCEon8m): 0 obras em 2,8 dias.** 0.3.78 corrige três — **movido: F1, biomas do `TODO.md` (pendências do deserto) e "Decisões do autor" (quem mora embaixo).**
      causas medidas na bancada com a semente do autor (`--near=-460,601`): a rua de arenito liso da
      vila gerada não era rua (só no índice, que só se enche de rua), a busca de rua só via +2 do
      centro, e a duna acima da rua não era cortada (corte até 5). Bancada: 0 → 4 casas, nota 5,1 → 8,8.
      0.3.84 (deserto jogado na 0.3.83, nota 7,8, Spark 5tus3Etvsq): fundidor atende já a peça de
      fornalha que a obra espera (arenito liso 67× de lado); ferro descansa um dia onde a mina não o tem
      (IronRest); rede da mina menor por nível (1º raio 2, demais 3; antes 6) para descer mais rápido;
      chão firme da vila de areia (2 camadas abaixo da superfície de areia viram arenito, DesertGround).
      0.3.85 (autor, opção 1): o lote aplainado ganha um corredor de 2 blocos em volta da casa, cortado
      até a base (só chão do bioma; nunca rua, peça da vila, obra ou beira d'água; não aterra). Bancada:
      9,4 / 9,1 / 8,9, a casa do buraco (148,78,-507) fecha; desistências de lado 570 → 63–107.
      0.3.83 (autor, 21-10: "mudar de casa"): quem desiste do trabalho morando 2+ abaixo dele passa a
      dormir numa cama livre da vila na altura do trabalho, ou troca de casa com um aldeão sem ofício
      que mora lá (HomeMove); à noite o jogo já o leva para a cama. Bancada: 9,3 / 9,3 / 5,9 — na de
      5,9 todas as camas de cima eram de aldeões com ofício e o construtor não tinha para onde ir.
      Pendente 🟠: esse caso (decisão do autor: trocar com quem tem ofício? construir casa em cima?).
      0.3.93 (avaliação do "Regras v2"): 74 transacional, 110 (itens não somem), 21/32 (peças funcionais),
      111 (lista de suprimentos), 112 (códigos de bloqueio). **Falta jogo.**
      Pendente 🟠: decisões do autor para a configuração (escopo, por colônia, edição, lista da F1).
      Pendente 🟢: área protegida (R-035), transporte físico (R-140–142), outros mods (R-026) — futuro.
      0.3.92: Regra 74 — cama mais perto do trabalho (BedPlan/BedChoice, 1×/dia ao amanhecer, troca se os
      dois ganham). **Falta jogo.** Pendente 🟡: o carpinteiro, o pedreiro e o fundidor não marcam lugar no
      diário (CRAFTED sem posição), então ficam onde estão.
      0.3.91 (deserto jogado na 0.3.90, nota 7,6, Spark LYMx4yHjKk): a faixa deu lote (curral, 486 blocos
      cortados, caminho de areia). Corrigido: a busca com corte travava 3 s (memória por passo, teto de
      50 ms); a bateria agora começa com o mundo limpo.
      Pendente 🟠: os mineiros andam 36% do expediente da vila alta até a mina do lago (9 desistências a
      32–61 blocos).
      Pendente 🟡: o `storage_majest` pede deepslate, calcita, folha de acácia e madeira de pinheiro no
      deserto (55 peças deixadas de lado).
      Pendente 🟡: espera de 42% do fazendeiro e de 41% do fundidor.
      0.3.89 (deserto jogado na 0.3.88, nota 4,0, Spark YFIroI1U1x; escolhas do autor de 22-10):
      - alcance do fazendeiro com a borda;
      - mineiro nada ao ar livre;
      - corredor corta degraus na duna (vale de 3, até 12 de fundo);
      - rebanho 8/6, lotação pelo tamanho, curral novo com todos cheios;
      - caminho de areia (Regra 73).
      **Falta jogo.** Pendente 🟡: KF-009 (MineReturn 1 em 4 baterias); o `PenPlans` não tem GameTest.
      0.3.88 (deserto jogado na 0.3.87, nota 4,9, Spark CnNU79zqZ5): Regra 71 (mineiro não constrói
      fora da montanha, desce antes de ir de lado, acesso cavado), Regra 70 (corredor de caminho), a faixa
      continua esperando lote. **Falta jogo.** Pendente 🟡: caminhos em mancha já calçados não foram
      desfeitos; a extensão de rua e o recuo de 3 não passam pelo `Corridor` (já são retos).
      0.3.87 (deserto jogado na 0.3.86, nota 3,7, Spark j7jTrVIaPQ; decisões do autor de 22-10): encalhe
      sem vaivém (BedTeleport 4,5), trigo pela semente mais rara e reserva de semente, mina insiste em 5
      pontos e só muda para boca alcançável, lote no fundo da faixa com caminho pelo terreno (A5, Regra 56).
      **Falta jogo.** Pendente 🟡: o fundidor larga a fornalha a cada ~30 s com o pedido ao pedreiro aberto
      (~7% do tempo dele; cooldown por tarefa); D2 ainda não exercido (a sessão não teve ceia).
      0.3.86 (deserto jogado na 0.3.85, nota 6,1, Spark IAXTyE0LXM; estudo em
      `docs/research/2026-10-21-deserto-ociosidade-alternativas.md`): a vila ficou 34 de 57 min sem
      obra porque **não havia lote** (a busca termina as voltas; não é falta de prazo). Feito: A1 (a
      rua em crescimento que para registra o motivo — a da faixa anexada parava em silêncio), C1 (o
      mineiro que chega ao lugar de trabalho alcança a pedra; antes ficava a 4,1 com alcance 4), B1
      (sem arenito, o pedreiro o faz da areia da vila para o fundidor), D2 (a ceia deixa um par da
      comida do rebanho). **Falta jogo.** Pendente 🟠: com o motivo da rua em mãos, A2 (rua da faixa
      com alternativas) ou A3 (aterro além de 2); decisões do autor A5 (lote e rua juntos na faixa),
      F2/G1 (lenhador e sem ofício ajudando a obra); D1 (o trato do rebanho contar como trabalho).
      0.3.82 (autor, 21-10, depois de jogar a 0.3.81): a escada fixa e a subida por pilar saíram —
      "quebra o visual", "a movimentação precisa ser a mais natural possível". Só o mineiro cava
      (degrau, túnel) para subir; os outros, só degrau natural (regra de 15-10 de volta). Pendente 🔴:
      quem mora na parte baixa da vila do deserto volta a não chegar à obra de cima (bancada: 0 obras
      em 2 rodadas com o construtor morando em y63) — falta uma saída natural, decisão do autor.
      0.3.81 (deserto jogado na 0.3.80, nota 8,2, Spark ETMetpWVS9; a vila tem duas alturas, y63 e y80+):
      toda profissão que desiste abaixo do destino sobe até a altura dele (pondo bloco ou cavando — a
      regra de 15-10 "só o mineiro" vale fora desse caso), com prazo de 2 min e descanso de 5; escada
      fixa de arenito entre as alturas (VillageStairs); viveiro do anel do baú aceita areia da vila a
      céu aberto, só na vila em duas alturas e dentro da caixa; canteiro novo na areia além da borda da
      roça (mais trigo para o pastor cruzar os bichos). Pendente: na bancada a nota variou de 4,4 a 9,0
      (sem ganho claro sobre a 0.3.80); escada parou em 4 de 18 degraus por peça da vila; KF-006.
      0.3.80 (deserto jogado na 0.3.79, nota 9,1, Spark jiRJWBtgDi): encalhado abaixo do caminho sobe
      até a altura dele (pilar/escada rumo ao destino — o mineiro da casa baixa saía "after 0 steps" 9×);
      grama também no chão do curral; viveiro primeiro em volta do baú do lenhador, na altura dele;
      terracota, feno e argila aparecem quando a obra pede (AppearingPieces, sem pedido nem falta).
      Pendente: KF-005 (teste do carvão do mineiro cai na bateria inteira, 3/7).
      0.3.79: fazendeiro vai ao lugar de pé ao lado da planta (FarmerStand) e, como o construtor, anda
      por etapas quando longe — desistências 7 → 0 na bancada; grama ao pé do poste da ovelha amarrada
      (PostGrass, pedido do autor). Pendente: a grama do poste não foi exercida na bancada (o pastor não
      amarrou ovelha nenhuma) — só GameTest; pastor segue 75% ocioso; lenhador e carpinteiro parados
      (40–60% ócio); cacto e cana contam como chão do lote; a bancada varia muito entre rodadas (nota
      3,9 a 8,8 com o mesmo código) — comparar com 2+ rodadas.
- [x] 🔴 **Jogar 0.3.77 pelo roteiro** (`docs/testing/roteiro-por-bioma.md`): deserto e uma floresta, — **fechado: duplicado de "Ciclos de playtest até todo ofício" no `TODO.md`; o roteiro `docs/testing/roteiro-por-bioma.md` entra nele.**
      com `/vc bioma nota`; relatório com `scripts/biome_report.py`. A bancada não vê casa torta,
      aldeão preso nem rua estranha.
- [x] 🟠 **Rotas que travam na execução (`STALLED`).** Deserto: arenito liso (7×), cortado (3×) e comum — — **movido: F1, biomas do `TODO.md`.**
      fundidor e pedreiro não pegam a tarefa; deserto também tem fundidor 100% parado. Neve: bloco e
      camada de neve agora têm tarefa e travam (6× e 2×). Savana: vidraça, terracota colorida.
- [x] 🟠 **Terraplanagem fase 2** (a vila inteira em subida de 1 em 1, plano de 15-10): ermos e taiga — **movido: F1, biomas do `TODO.md`.**
      nevada têm 69% e 28% das recusas "sem chão na janela", relevo acima de 2.
- [x] 🟠 **Lavoura e viveiro na neve e no deserto.** `FarmerTill` e `TreeNursery` exigem ar em cima do — **movido: F1, biomas do `TODO.md`.**
      chão: a camada de neve barra. No deserto, roça nova exige grama/terra (aterrar com terra da mina) e
      o fazendeiro "could not reach the crop" (9–11×). Fazendeiro 75–81% parado no deserto.
- [x] 🟠 **Pastor sem ovelha.** Deserto e neve: 84–96% parado, "no sheep with wool within the radius"; — **fechado: tratado pela Regra 239 (0.4.51), 15 espécies e coelho no deserto.**
      lã branca é a falta `NOT_FOUND` mais comum (todas as vilas). Decisão: o pastor traz/cria animais?
- [x] 🟡 **Estilo e madeira do bioma vizinho** para biomas fora da tabela (ermos → deserto, picos de — **movido: Depois da 1.0, estilo do bioma vizinho do `TODO.md`.**
      gelo → neve; selva, floresta escura, cerejeira com a madeira local). Hoje caem em planície de carvalho.
- [x] 🟡 **Reserva de madeira** onde não há árvore (lenhador 85–88% bloqueado no deserto). — **movido: F1, biomas do `TODO.md`.**
- [x] 🟡 **Mineiro: alvo alto demais e sem onde pisar** (TOO_HIGH/NO_STANDING_POSITION em todo bioma) — — **movido: F1, biomas do `TODO.md`.**
      a mina 2ª fase.
- [x] 🟡 **Ofício sem trabalho ajuda os outros** (cadeia da Regra 51 para pastor e fazendeiro) — mexe — **fechado: decidido pelo autor em 02-11, nenhuma profissão faz atividade de outra (exceções nas Regras 215 e 220).**
      na Regra 59, **decisão do autor**.
- [x] 🟡 `NO_TASK` novos: tronco e madeira de acácia na savana. Melancia (`melon`) sem rota: o — **movido: F1, biomas do `TODO.md`.**
      fazendeiro não planta melancia.
- [x] 🟡 **Bancada mais fiel:** 3 sementes por bioma; vila posta por `/place structure` não fica — **movido: Depois da 1.0, Sentinela futuro do `TODO.md`.**
      registrada como estrutura (mede o pior caso); medir o custo da limpeza de mata (`cost_ledger`).
- [x] 🟢 `MinerDirectPathGameTest` intermitente (~1/3, já no 690dee98) — card de tarefa aberto. — **fechado: registrado como KF-007 em `docs/behavioral-tests/known-failures.md`.**
- [x] 🟢 PR e merge na `main` das branches `fix/varredura-g1-g3` e `feat/biomas-simulacao` (ADR-035). — **fechado: duplicado de "PR #45 e as branches locais antigas" no `TODO.md`.**

## 🔴 Auditoria técnica 10-10 (fila AUD, P0→🔴 P1→🟠 P2→🟡 P3→🟢)

- [x] 🔴 **AUD-01** Regra 51 reprovada nas duas sessões de 10-10 (0.3.23: SMELTER 91%, BUILDER 92%, MINER 17%; — **fechado: duplicado de "Ciclos de playtest até todo ofício" no `TODO.md`.**
      fundidor 311/311 soltas). Raiz (diagnóstico de 11-10): o dono do único ramal vai e volta numa descida até
      um patamar de pedra maciça sem nenhum prazo correr; a rota vinha do rumo da escada girada, não da cavada.
      RC1 (prazo da descida) e RC2 (rota pela escada cavada) feitas; falta o playtest.
      Roteiro: `docs/technical/Roteiro-Correcoes-2026-10-10.md`, 2.2.
- [x] 🟡 **AUD-09** Ciclo raiz ↔ `fabric.*` por `VillageColonyMod` como localizador global. — **fechado: duplicado de "Contexto por servidor" no `TODO.md`.**
- [x] 🟡 **AUD-10** 181 estáticos mutáveis; config estática não-final (`MineLease`, `TreeChoice`, `MinerDetours`, `FarmerWork`). A regra ArchUnit já existe congelada: esvaziar o congelado. — **fechado: duplicado de "Contexto por servidor" no `TODO.md`.**
- [x] 🟡 **AUD-11** `fabric.network` com 67% de linhas no unitário (meta de 60% batida em 11-10): codec, os três — **movido: Depois da 1.0, cobertura do `TODO.md`.**
      pacotes pelo fio (`ProfessionPolicyPayloadsTest`), save da política e porta de permissão, com mutação conferida.
      Falta: `fabric.command` em 27% (bateria) e o cliente em 0%; receptores por lambda e o resto do `/vc log`.
- [x] 🟡 **AUD-04** Ciclo p95 67 ms na 0.3.22, 27,7 ms na 0.3.23: medir de novo (≥ 20 amostras) antes de otimizar. — **fechado: duplicado de "Ciclo da colônia repartido entre tiques" no `TODO.md`.**
- [x] 🟡 **AUD-13** Alternância de obras: `docs/behavioral-tests/README.md` atualizado (11-10); falta provar em jogo. — **fechado: conferência de versão antiga; o que vale agora é o jogo da 0.4.74 (`STATE.md`, "O que o próximo jogo precisa mostrar"); sintoma que voltar reabre pela análise do jogo.**
- [x] 🟢 **AUD-15** 848 imports não usados (`RemoveUnusedImports` no Error Prone). — **movido: Depois da 1.0, cobertura do `TODO.md`.**
- [x] 🟢 **AUD-16** Cache de `CraftingLookup.listAllOfType`, se o Spark mostrar. — **fechado: duplicado de "Ciclo da colônia repartido entre tiques" no `TODO.md` (só se o Spark mostrar).**
- [x] 🟢 **AUD-18** Tag por JAR publicado (0.3.4–0.3.23 sem tag). — **fechado: duplicado de "Versão semântica" no `TODO.md` (tags por versão publicada).**
- [x] 🟢 **AUD-26** **Escolha do autor:** número da regra do ócio (colide com a Regra 51 do `docs/RULES.md`). — **fechado: duplicado de "Número próprio para o teto de 15%" no `TODO.md`.**
- [x] 🟢 **AUD-20** Dividir as 15 classes em 488–500 linhas antes da próxima mudança nelas. — **fechado: duplicado de "Dívida conhecida" no `STATE.md`.**
- [x] 🟢 **AUD-24** **Escolha do autor:** apagar as 7 branches já contidas na `main`. — **fechado: duplicado de "PR #45 e as branches locais antigas" no `TODO.md`.**

## 🟠 Fluxo físico de materiais da obra

- [x] Medir no save real a entrega de `CALCITE` pelo mineiro e comparar contra as 63 inserções — **movido: F1, entrega física da obra do `TODO.md`.**
      instantâneas registradas; rota declarada e meta nominal não substituem a confirmação de entrega.
- [x] Para cada bloco sem catálogo, criar primeiro uma cadeia executável de profissão ou mantê-lo no — **movido: F1, entrega física da obra do `TODO.md`.**
      fallback terminal. Não voltar a declarar uma rota genérica sem trabalhador que a cumpra; folhas
      permanecem fora da coleta do lenhador pela regra de preservar a copa.
- [x] Conciliar demanda da obra, reserva de tarefa, estoque em baú e entrega para que uma reserva — **movido: F1, entrega física da obra do `TODO.md`.**
      sem retirada não impeça outra profissão de atender a peça.

## 🟠 Erros abertos

- [x] **KF-002** (aldeão de coleta some no tique 1): não reapareceu em ~27 baterias de 07-10. — **fechado: registrado como KF-002 em `docs/behavioral-tests/known-failures.md`.**
      Correção recomendada: montar o cenário dentro da própria arena, com o raio protegido encurtado
      por gancho de teste (como `MineDigging.shortenSurfaceRadiusTo`), em vez de 65–97 blocos fora.
- [x] **KF-003**: causa achada e corrigida (fusão de colônias no tique de teste). Fecha com — **fechado: registrado como KF-003 em `docs/behavioral-tests/known-failures.md`.**
      `python scripts/gametest_battery.py 30` limpo; até agora 7 + 6 limpas.
- [x] **`lumber_stall_guard`** caiu 1 vez em ~6 baterias de 07-10 (tarde), sem relação com a mudança — **fechado: registrado como KF-004.**
      da hora. Medir com o script antes de investigar.

## 🟡 Estrutura

- [x] Outros seis arquivos de produção estão em 497–500 linhas (`SmelterWork`, — **fechado: duplicado de "Dívida conhecida" no `STATE.md`.**
      `Mine`, `ClimbOut`, `SurfaceGatheringWork`, `Worker`, `BuilderWork`) e devem ser divididos
      antes da próxima mudança neles. `ColonyGoals` foi reduzido a 500 linhas com a extração de
      `AdvanceConstructionStock`; `FileSizeRuleTest` voltou a passar.
- [x] Branch local do worktree do Codex (`codex/village-visuals-logistics-mine-sweep`): já toda na — **fechado: duplicado de "PR #45 e as branches locais antigas" no `TODO.md`.**
      `main`; remover o worktree.

## Decisões do autor que faltam

- [x] Aldeões sem ofício 100% parados (cota da ADR-011): vagas para todos, ou ajudantes? — **fechado: duplicado de "Decisões do autor que faltam" no `TODO.md`.**
- [x] Dois construtores na mesma obra. — **fechado: duplicado de "Decisões do autor que faltam" no `TODO.md`.**
- [x] E5/E6: limites da felicidade e do rebanho (números do agente). E7: texto da Regra 33. — **fechado: duplicado de "Decisões do autor que faltam" no `TODO.md`.**
- [x] Números do agente na ADR-039 (carvão < 32, ferro < 16, caminho 4/ciclo até 24). — **fechado: duplicado de "Decisões do autor que faltam" no `TODO.md`.**
- [x] `docs/workers-analysis/`: mover para `docs/research/`, arquivar ou apagar. — **fechado: duplicado de "Decisões do autor que faltam" no `TODO.md`.**
