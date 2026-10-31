# STATE — 2026-10-29

> **31-10 — a 0.3.110 foi jogada** por longo tempo (relato do autor; logs ainda não analisados com
> `time_ledger.py`/`action_report.py`). A corrigir: estrutura e entrada da mina; criação e ligação dos caminhos;
> escolha das estruturas a construir e as que faltam. Ambiente: 11 skills fora do escopo foram para
> `docs/archive/skills/`; MCP `minecraft-modding` (`@adhisang/minecraft-modding-mcp@8.0.0`) em `.mcp.json`.
> Diagnóstico: `docs/audit/2026-10-31-auditoria-controle.md`.

> **0.3.110** (29-10, branch `fix/playtest-0102`, commits `878d209` e `e92e1fe`): pedidos do autor depois dos jogos da
> 0.3.109 e de um JAR 0.3.110 sem testes — o aldeão da colônia pula 2 blocos e por cima da cerca (Regra 159); o
> construtor trabalha do chão e quem fica num telhado se joga rumo ao chão, posto no chão depois de 3 pulos (Regra 160);
> a mina recomeça na mesma boca, virando de rumo (emenda da Regra 119); comer enche a vida (Regra 161); o alisamento não
> mexe dentro de construção ou obra e o pedreiro limpa o miolo dos cercados (Regra 162). Unitários 1663/1663, bateria
> 939/939 na rodada final (`ShepherdHerding`, `ShepherdFeeding` e `Builder ground` falharam uma vez cada antes),
> Python 153. Regras 161 e 162 sem teste próprio. **A versão testada não foi jogada.** Para o jogo: **mundo novo**; o
> pastor no curral sem portão, o construtor no telhado, o miolo do cercado.
> Publicada como pré-lançamento `v0.3.110-alpha` (workflow `Release`, só `remapJar`, commit `29b9ca4`): JAR
> `village-colony-0.3.110.jar`, SHA-256 `A23105CF54A35A852E9FD9677114FCA7A48D965B689743F738266151D41EE2D1`.
>
> **0.3.109** (29-10, branch `fix/playtest-0102`): depois do jogo da 0.3.108 (`docs/research/2026-10-29-jogo-0108.md`):
> o chão em volta da vila é a mistura suave com o terreno numa faixa de 4× o desnível, de 16 a 64 (Regra 152 refeita,
> `VillageBlend`; a vila referencia os chunks até 64 além das peças); a árvore sobre chão cortado sai inteira; o mineiro
> não quebra o que segura cascalho, água ou lava pelo vão (Regra 157); ninguém dorme no andar de baixo da casa de escada
> de mão (Regra 158). A mistura parte do topo de verdade e a margem da água fica natural (bancada de 6 cenários: chão
> sobre vão cai em todos, maior degrau igual, paredões +8 a 25 % perto da vila). Unitários 1663/1663, bateria 936/936,
> Python 153, PIT 85 %. **Não jogado.** Falta: portão da mina na entrada
> real, argila, fazendeiro, construtor sem material. Para o jogo: **mundo novo**; a encosta em volta da vila.
>
> **0.3.108** (29-10, branch `fix/playtest-0102`): o pedreiro sem tarefa alisa o chão da vila aos poucos (Regra 156 —
> janela de 24×24, buraco tampado, calombo vira rampa de degrau 1, nunca em construção, roça, água ou mina); o
> construtor com a obra parada nivela o lote (O2); o portão do autor no lugar do arco da mina, pistas no centro do vão
> (Regra 155); bancada de solo em 100×100 em todos os biomas. Unitários 1643/1643, bateria 933/933, Python 150.
> **Não jogado.** Falta: O1 (ajudar a assentar), O7 (canteiro antigo). Para o jogo: **mundo novo**; o portão sobre a
> boca da mina; `LEVELLED` no diário do pedreiro e do construtor.
>
> **0.3.107** (29-10, branch `fix/playtest-0102`): o solo da vila nasce suave na geração (Regra 152 — degrau > 1
> na área: deserto 4,29 → 0,19 %, savana 2,11 → 0,10 %, neve 1,95 → 0,42 %, medido em mundo novo); neve e argila
> aparecem, o fundidor faz vidraça, o lenhador derruba a espécie pedida (T1, T5); carregador, pastor cuidando do
> rebanho, madeira e pedra adiantadas, árvore fora um dia, bosque perto, areia mais longe, composto (Regras 153–154).
> Bateria 931/931, unitários verdes. **Não jogado.** Falta: O2, o pedreiro ajudar a assentar (O1) e aplainar o
> canteiro antigo (O7). Para o jogo: **mundo novo**.
>
> **0.3.106** (28-10, branch `fix/playtest-0102`): o trajeto longo do aldeão da colônia vai pela rede de ruas
> (Regra 151, `StreetRoute` + `MoveToTargetTaskMixin`), e o mineiro não recusa mais o primeiro degrau do ramal novo
> (`MiningTarget.judge`; no jogo da 0.3.104 foram 11 ramais encerrados). Bateria 923/923, unitários verdes.
> **Não jogado.** Para o jogo: mundo novo, a §4 e a §5 de `docs/research/2026-10-28-jogo-0104-savana.md`.
>
> **0.3.105** (28-10, branch `fix/playtest-0102`): os pedidos do jogo da 0.3.104 na savana
> (`docs/research/2026-10-28-jogo-0104-savana.md`) — rua só de caminho (Regra 138), vila do Minecraft gerada em
> terreno plano (`beard_box`, só chunks novos), aldeão da colônia evita a água e prefere a rua (Regra 151), porta
> de toda casa ligada à rua por caminho de 1 (Regra 123), canteiro novo 9×9 com água no meio (Regra 31).
> Bateria 921/921, unitários verdes, Python 145. **Não jogado.** Falta: trajeto longo pela rede de ruas. Para o
> jogo: **mundo novo**, e a §4 do documento do jogo.
>
> **0.3.104** (28-10, branch `fix/playtest-0102`): decisões do autor sobre o diagnóstico da 0.3.103
> (`docs/research/2026-10-28-diagnostico-e-decisoes-0103.md`) — gelo surge na obra (T1, parte), pastor tosquia
> por cima da cerca (T2, Regra 150), área forçada da bancada cresce com a caixa (D4), mapa de encalhe
> `scripts/stuck_map.py` (T3), mineiro pula pedra alta demais ou sem caminho na escolha (T4). Unitários
> 1625/1625, bateria 915/915, Python 144. **Não jogado.** Para o jogo: a §4 do documento de decisões.
>
> **0.3.103** (28-10, branch `fix/playtest-0102`): ruas naturais (Regras 138–140), centro fixo (141), noite
> só para o preso (142), baú público inteiro (143), alvo inalcançável esquecido (144), camas da caixa (145),
> produtos de valor (146), diário da vila `VC_EVENT`/`VC_DAY`/`VC_AUDIT` (147), obra nunca largada meio
> pronta (148), rua sem falha e ajudantes (149), mina com salão de 9 e forma 9; correções da auditoria
> (`PROJECT_AUDIT.md`, T1–T5). **Não jogado.** Para o jogo: `python scripts/state_report.py` lê o diário.
>
> **0.3.102** (28-10): tocha nova (referência do autor) e o retrato da vila `VC_STATE`, no log e em
> `<mundo>/villagecolony/analytics/*.jsonl`. Também `/vc dados`, `/vc log` e `/vc bioma` com o retrato, e
> `scripts/state_report.py`. Bateria 900/900. **Não jogado.** Para o jogo: `/vc log`, `/vc dados` e
> `python scripts/state_report.py` depois de jogar.
>
> **0.3.101** (28-10): corredor do lote com serpentina primeiro e corte raso (até 3) com talude, no lugar
> do cânion de até 12 (Regra 70, opção 3 do autor). Bateria 899/899. **Não jogado.** Para o jogo:
> `cut N blocks of the slope` no log e a aparência do corte nas obras de encosta.
>
> **0.3.100** (28-10, branch `fix/playtest-0397`, PR #37): os ociosos e travados do jogo da 0.3.99 (Spark
> eVkPhNEtZT, 76 min, 20 TPS), Regras 133 a 137 com as escolhas do autor (memória
> `playtest-0399-ociosos-decisoes`).
> - **Verificado:** testes de unidade OK; bateria 898/898 (`IdleFixesGameTest`). **Não jogado.**
> - **Para o jogo:** rodar `time_ledger` e `action_report`; conferir no log:
>   - `chests are N% full` e o armazém aberto;
>   - `restarts its stair` do mineiro;
>   - `value goods` no diário;
>   - quedas de "could not reach the crop" e "no sheep with wool".
>

> **0.3.99** (28-10, branch `fix/playtest-0397`, PR #37): caminho rente ao chão (Regra 123 emendada) depois
> do jogo da 0.3.98 (Spark S3FcIdzPmh: aterros de até 4 blocos e travão de 12 s no traçado). Memória
> `caminho-rente-ao-chao`.
> - **Verificado:** testes de unidade OK; bateria 892/892 (o GameTest da rede confere que nada nasce
>   acima da superfície e que o poço ao lado não é aterrado). **Não jogado.**
> - **Para o jogo:**
>   - os caminhos novos rentes ao chão;
>   - o tempo de cada traçado no log (`traced a path … in N ms`, `no path … (N ms)`);
>   - nenhum "Can't keep up" vindo do traçado.
> - **Fica como estava, por escolha do autor:** os aterros que a 0.3.98 já fez.
> - **Para decidir:** o corredor do lote (Regras 70/110) ainda corta encosta até 12 de fundo ao abrir uma
>   obra. Ele só tira bloco, mas não segue o "rente ao chão".
>

> **0.3.98** (27-10, branch `fix/playtest-0397`): o playtest da 0.3.97 (savana, Spark n4DVevJAmw) e os
> pedidos do autor, cada um com as opções que ele escolheu (memória `playtest-0397-e-decisoes-2026-10-27`).
> - Defeitos: base com a horta da planta, horta pedida como material, fundidor sem a cadeia da pedra,
>   porta recuada sem caminho, tocha invisível, tocha da mina na parede, escada fechada pela água.
> - Regras 128 (5 min), 129 (cor do nome), 130 (curral), 131 (nivelamento), 132 (bancadas); árvore rara;
>   receitas.
> - **Verificado:** testes de unidade e arquitetura OK; bateria 892/892. **Não jogado.**
> - **Para o jogo:**
>   - a tocha com os pacotes de textura e na parede;
>   - as cores dos nomes;
>   - `Regra 128` no log e o `time_ledger` abaixo de 15% de ócio;
>   - `levelled` em volta das obras;
>   - `path network —` e `traced a path`;
>   - o painel das bancadas;
>   - o custo das passivas no Spark.
> - **Limites conhecidos:** a escada em caracol da mina não leva tocha (só os salões); a proteção do caminho
>   cobre a entrada do nível atual da mina; o encantamento vale para o que o jogador fabrica na vila.
>

> **0.3.97** (27-10, branch `fix/obra-pecas-de-lado`, PR #36): os sete pontos abertos da 0.3.96, cada um com
> as opções escolhidas pelo autor (memória `decisoes-autor-2026-10-27-sete-itens`).
> - Cama pela soma (16) e tarefa a quem mora perto do lugar do ofício (Regra 74).
> - Ajudas (127): pastor na roça, lenhador no bosque, carpinteiro com a tocha da colônia, pedreiro com a pá
>   na neve.
> - Atalho ao chafariz a partir de 2× (D5); veio que não sobe (119).
> - Rede de corredores removida; Regra 127 de lote pela centralidade **não feita**, por escolha do autor.
> - **Verificado:** testes de unidade e arquitetura OK; bateria 876/876; cada ajuda e a tocha com GameTest,
>   menos o lenhador e a escolha do lugar da tocha (dependem do expediente). **Não jogado.**
> - **Para o jogo:**
>   - a cara da tocha e a área clara;
>   - "lit the dark path", "tended the grove", "harvested … for the farmer", "shoveled";
>   - se a vila faz bloco e camada de neve com as bolas;
>   - um save antigo com a rede de túneis carregando sem erro.
> - **Veio:** a régua é "nunca sobe acima de onde começou" (e não "até 2 acima do piso" ao pé da letra, que
>   quebrava 5 testes do veio) — confirmada pelo autor em 27-10.
>

> **0.3.96** (27-10, branch `fix/obra-pecas-de-lado`, PR #36): mina em caracol (119), rede de caminhos
> (123–125, estudo `docs/research/2026-10-27-rede-de-caminhos.md`), fazendeiro (121), lenhador (120),
> cercados (122), 3 blocos entre construções (124), peça colorida branca (126) e códigos do diário.
> - **Verificado:** testes de unidade OK; bateria 874/874. Cada regra tem teste, e as provas de mutação da
>   mina, da roça, do custo de colar e do aterro foram pegas. **Não jogado.**
> - **Para o jogo:**
>   - a mina nova num save novo (num save antigo ela segue da mesma boca);
>   - "traced a path", "finished a path" e "PAVED … path network" no log;
>   - recusas `TOO_CLOSE` no `LotRefusals`;
>   - "closed the wood task"; "still sweeping" deve cair.
> - **Ainda abertos:**
>   - item 3 (trabalho perto da cama, troca forçada);
>   - item 8 (ajudante da cadeia);
>   - D5 (atalho ao chafariz);
>   - Regra 127 (lote pela centralidade);
>   - neve (snow_block da pá, diorito da mina);
>   - remover o código parado da rede de túneis;
>   - o veio que mira pedra alta no mineiro.
>

> **0.3.95** (27-10, mesma branch `fix/obra-pecas-de-lado`, PR #36): correções feitas a partir dos cinco
> logs da 0.3.93 (savana, planície ×3, neve, taiga, deserto). Análise na memória `analise-biomas-logs-0393`.
> - **Regra 114:** o teleporte pousa onde o corpo cabe (14 sufocados: 12 na neve, 2 no deserto); o preso sem
>   cama vai ao centro da vila; quem fica 3 rodadas cercado sai por cima da cerca (`SafeLanding`, `PenLift`).
> - **Regra 115:** o armazém pisa no nível do chão, com lote nivelado; há um por bioma, sem folhas, alçapões
>   nem teias (`scripts/storage_variants.py`).
> - **Regra 116:** a argila surge na 3ª busca vazia. **Regra 117:** ração do pastor sem comida do fazendeiro.
>   **Regra 118:** no deserto, bosque com o dobro de mudas, nas bordas sorteadas.
> - Animais da fundação (2 de cada espécie): já existiam (Regra 102); os logs mostram 8 em toda vila.
> - **Ainda sem correção:**
>   - savana: o lenhador não termina tarefa e há 94 acacia_log de lado. As tarefas do lenhador terminam soltas
>     em todo bioma, então falta medir se é a acácia ou a cota de 128 toras;
>   - fazendeiro: "could not reach the crop" (até 31×) e, na planície 1bd4fbb9, "budget ran out" (266×), com a
>     população parada em 16 e 41 camas;
>   - neve: snow, snow_block e diorite surgindo; deserto: smooth_sandstone (146);
>   - mineiro: NO_STANDING e TOO_HIGH; Regra 74 sem troca; códigos UNCLASSIFIED.
>
> Verificado: test OK; Python 124 OK; bateria 871/871 depois de cada marco (×2 nos três primeiros).
> Não jogado.
>
> **Próximo:** jogar e procurar "lifted from", "to the village center", "animal feed", morte de aldeão
> "sufocou"; ver o armazém num terreno inclinado.
>

> **0.3.94** (27-10, branch `fix/obra-pecas-de-lado`): **Regra 113**, decisão do autor. No jogo da 0.3.93
> no deserto (22-10, ~3 h), a vila parou às 18:57: o armazém (`storage_majest`, 112 89 -430) chegou a
> "5 blocks left" com as cinco peças de lado; o construtor soltou a obra depois dos 5 minutos do
> `BuildWait`, o planejador não reabre tarefa sem peça a pôr, o `UnsupportedRelease` só fecha até 4
> peças sem apoio, e nenhuma obra nova abria (343 ciclos "a build was open"). Construtor 77% ocioso,
> lenhador 98%, pedreiro 93%.
> - **Agora:** a cada ciclo, toda peça de lado volta à fila (qualquer motivo) e a tarefa reabre; depois de
>   4 voltas a peça surge (`VC_SUPPLY` `STUCK_PIECE`) e a obra fecha (`PieceRescue`, `StuckPieces`).
> - **Também visto nesse jogo, sem correção ainda:** 115 aldeões levados à cama à noite por falta de
>   caminho; fazendeiro 15× "could not reach the crop"; mineiros 59× sem lugar de pé e 22× sem
>   caminho; nenhuma troca de cama da Regra 74 (não verificado se o diário tinha ações bastantes);
>   12 códigos `UNCLASSIFIED` no mineiro.
> - CHANGELOG reconstruído de 0.3.51 a 0.3.85 e corrigido de 0.3.86 a 0.3.93 a partir dos commits.
>
> **Próximo:** jogar e ler `brings back … try N of 4` e `the last pieces set aside came up in place`.
>

> **0.3.93** (22-10, branch `feat/regras-v2-melhorias`): avaliação do "Regras Consolidadas v2" (proposta do
> autor) contra o código — `docs/research/2026-10-22-avaliacao-regras-v2.md` — e as melhorias escolhidas.
> Numeração 1–109 mantida.
> - **Regra 74 transacional:** a cama nova só com baú ao lado; na troca cada um herda o baú do outro, remarcado;
>   se falta algo ninguém muda; distância pela caminhada real (`Walks`). O risco era real: sem baú, o
>   trabalhador perdia o ofício.
> - **Regra 110:** o que a terraplanagem e o corredor cortam vai aos baús públicos; o que não cabe cai no chão
>   (`Cuttings`).
> - **C-02, Regras 21/32:** cama, porta e baú são obrigatórios; sem apoio ganham o chão
>   (`FunctionalPieces`); `VC_FUNCTIONAL_MISSING` quando o apoio é impossível.
> - **Regra 111:** lista declarada de suprimentos (`SupplyException`, linha `VC_SUPPLY`, `supply_report.py`).
> - **Regra 112:** códigos únicos de bloqueio no diário (`BlockCode`; o `action_report.py` agrupa).
> - **Configuração:** desenhada, sem código (`docs/research/2026-10-22-desenho-configuracao.md`); faltam 4
>   decisões do autor.
>
> Verificado: test 1594/0; pitest 85,3%; Python OK; bateria 862/862 ×2; mutação em cada item. Não jogado.
>

> **0.3.92** (22-10, mesma branch da 0.3.91, PR #33): **Regra 74**, pedido do autor — cada aldeão dorme na
> cama mais perto de onde trabalha.
>
> Escolhas do autor (todas as recomendadas):
> - **Trabalho:** onde trabalhou de fato, a mediana das últimas ações com lugar do diário.
> - **Cama ocupada:** troca se os dois andam menos; o sem ofício sempre cede.
> - **Frequência:** uma vez por dia, ao amanhecer, com ganho mínimo de 16 blocos.
> - **Baú:** acompanha a cama.
>
> A cama livre só vale se alcançada a pé da vila, e quem dorme não muda (`BedPlan` no Core, `BedChoice`).
> No deserto (sessão 0.3.90), os mineiros dormiam a 70 e 97 blocos da mina.
>
> Verificado:
> - `BedPlanTest` 11 casos e 3 GameTests (troca, cama livre só com trabalho medido, cama cercada não);
> - duas mutações reprovam;
> - test 1586/0, pitest 85,2%, bateria 857/857 ×2.
>
> **Próximo:** jogar e ler *"moves home from … — its work is at"* no amanhecer.
>

> **0.3.91** (22-10, branch `fix/corredor-teto-de-tempo`): o jogo da 0.3.90 (Spark `LYMx4yHjKk`) teve **nota
> 7,6** (era 4,0). A faixa sul deu o primeiro lote: um curral no platô, com 486 blocos cortados e caminho de
> areia num vale de 3. A busca com corte, porém, travou o servidor 7 vezes, de 2,6 a 3,7 s.
>
> Feito na 0.3.91:
> - **Busca:** guarda a leitura de cada passo, para em 50 ms (`Corridor.OUT_OF_TIME`, que aparece no log).
> - **`StripLot`:** para entre tamanhos de casa e retoma dali.
> - **Bateria:** o `runGametest` apaga o mundo de teste antes de rodar. As colônias guardadas de rodadas
>   anteriores derrubavam ColonyDetection, Crafting e MinerDirectPath.
>
> Verificado: test 1575/0; pitest 85,2%; bateria 854/854 ×2 limpa; a mutação sem o teto reprova.
>
> Abertos: o percurso dos mineiros até a mina do lago (36% andando) e as peças de ardósia do armazém no
> deserto.
>

> **0.3.90** (22-10, branch `fix/corredor-crash-pit`): corrige o **crash da 0.3.89**, jogada às 10:41.
> - **O crash:** `NullPointerException` em `Corridor.plan`, na busca do corredor que corta degraus na faixa.
> - **A causa:** o estado da busca era `Comparable` só pela prioridade, e a igualdade a ignorava. Com muitos
>   estados de hash igual, o `HashMap` vira árvore, procura pela ordem, e o custo sumia.
> - **A correção:** o estado deixou de ser `Comparable`, e a fila ordena com um `Comparator`. O
>   `CorridorStateTest` reproduz a falha (reprova sem a correção).
> - **CI da main:** a etapa de mutação (PIT, Core) estava em 84%, abaixo dos 85%, porque o Core novo da
>   branch não tinha teste de unidade. Agora está em 85,2% (2107/2473), com testes para a faixa do
>   `VillageExpansion`, o `MineStairPoints` e os custos do mineiro.
>
> Verificado: test 1575/0; `pitest` passa; bateria 853/853 em 1 de 3. Nas outras caiu a KF-006 (Crafting),
> que falha igual na 0.3.89 com o filtro `ColonyDetection,Crafting`.
>

> **0.3.89** (22-10, branch `fix/deserto-a1-c1-b1-d2`, **sem push, JAR instalado, não jogado** —
> `2F8E0DDD…B21C`): resposta ao deserto jogado na 0.3.88 (Spark `YFIroI1U1x`, nota 4,0; §12 do estudo).
> Itens:
> - **Fazendeiro:** a 3 blocos alcança a planta, com a borda incluída (falhava 10×).
> - **Mineiro, escolha do autor:** nada ao ar livre, e a água continua proibida debaixo da terra (`MinerCaution`).
>   Ele ficou 12 min numa margem de lago cercada.
> - **Regra 70, degrau cortado, escolha do autor:** sem caminho pelo terreno, o corredor da faixa corta a
>   encosta até 12 de fundo, num vale de 3 (`Corridor` maxCut, `CorridorCut`). A faixa sul é uma duna 23
>   acima da rua.
> - **Pastor, pedidos do autor:** procria até 8 e abate acima de 6; o curral é lotado pelo tamanho (1 a
>   cada 4 de chão); com todos cheios, um curral novo entra na fila (`PenPlans`).
> - **Regra 73:** caminho de areia, exclusivo do deserto (`SandPathBlock`).
>
> Verificado:
> - test 1564/0;
> - runGametest 853/853 em 3 de 4 baterias. Na outra caiu o `MineReturnGameTest`, que passa isolado 4/4
>   (KF-009);
> - a mutação derruba o teste em cada item.
>
> Sem GameTest: o caminho `PenPlans` → fila de obras, e a textura vista em jogo.
>
> **Próximo:** jogar ≥30 min e ler *"cut … blocks of the slope for the path"*, *"finds a lot inside the
> annexed strip"*, o caminho de areia nas ruas novas e a separação dos currais.
>

> **0.3.88** (22-10, branch `fix/deserto-a1-c1-b1-d2`, **sem push, JAR instalado, não jogado** —
> `FBB7FB55…9680`): resposta ao deserto jogado na 0.3.87 (Spark `CnNU79zqZ5`, nota 4,9; §11 do estudo).
> **Regra 71** (autor): o mineiro não constrói para fora da montanha (céu aberto não recebe bloco no desvio)
> e, se precisar construir, desce antes de ir de lado (`MinerTerrain`, `DetourMoves.Costs.MINER`); com a
> entrada da mina inalcançável a pé, cava um acesso só cavando, da vila até o nível (`MineAccess`, escolha
> do autor). **Regra 70** (autor): caminho é corredor de um bloco que sai da porta em reta, vira só a 90° e
> para no primeiro caminho (`Corridor`, usado pelo `WorkPath` e pelo `StripLot`); o construtor só fecha
> buraco no meio de uma reta (acabou a regra dos 3 lados, que espalhava caminho). **Faixa:** um lote achado
> em outro lugar não encerra mais a faixa (`STRIP_AWAITS`), e a volta sem lote diz por quê. Verificado: test
> 1562/0; runGametest 846/846 e 845/846 (KF-008, pastoreio, intocado, isolado 4/4); mutação derruba teste em
> cada item. Ligação do acesso no `MinerSteps` sem teste próprio. **Próximo:** jogar ≥30 min e ler
> *"digs the mine access"*, *"has a dug access"*, *"found no lot inside the annexed strip … path stopped
> by"* e os caminhos novos.
>

> **0.3.87** (22-10, branch `fix/deserto-a1-c1-b1-d2`, **sem push, JAR instalado, não jogado** —
> `5B4DC077…62DD`): resposta ao deserto jogado na 0.3.86 (Spark `j7jTrVIaPQ`, nota 3,7; análise no §10
> de `docs/research/2026-10-21-deserto-ociosidade-alternativas.md`). Decisões do autor de 22-10:
> **encalhe** — o vaivém degrau/túnel de 3 blocos zerava a conta da cama; agora conta como o mesmo lugar
> (`BedTeleport.SAME_PLACE` 4,5; os mineiros ficaram 12 e 17 min presos); **trigo** — o fazendeiro
> semeia a lavoura que a vila tem menos e o pastor deixa 8 sementes para o plantio (semeava beterraba
> 27× contra 4 de trigo); **mina** — a escada insiste em 5 pontos do nível (4 rumos cada, 20 partidas,
> `MineStairPoints`) e a boca nova só vale se alcançável a pé (`ReachMap.reachesOnFoot`); **A5** — com a
> rua da faixa barrada pela duna, o lote nasce no fundo da faixa com um caminho que segue o terreno
> (degrau ≤1, até 32, calçado na abertura; `StripLot`; Regra 56 emendada). Mantido: rua não passa por
> arenito na vila gerada (Regra 3). Verificado: test 1560/0; runGametest 840/840 ×2; mutação derruba
> teste em cada item. **Próximo:** jogar o deserto ≥30 min com um anoitecer (para o D2) e conferir as
> linhas *"finds a lot inside the annexed strip"*, *"keeps its place"* e o tempo de encalhe dos mineiros.
>

> **0.3.86** (21-10, branch `fix/deserto-a1-c1-b1-d2`, **sem push, JAR instalado, não jogado** —
> `4A56148A…C93C`): resposta ao deserto jogado na 0.3.85 (Spark `IAXTyE0LXM`, nota 6,1; relatório
> `docs/testing/relatorios/deserto-2026-10-21.md`, estudo `docs/research/2026-10-21-deserto-ociosidade-alternativas.md`).
> A vila ficou 34 de 57 min sem obra porque **não havia lote** (a busca termina as voltas; não é falta
> de prazo). Escolha do autor: **A1** a rua em crescimento que para registra o motivo (*"stopped growing
> the road … — motivos"*; a da faixa anexada parava em silêncio); **C1** o mineiro que chega ao lugar de
> trabalho alcança a pedra (`MinerReach.isWithinReachStanding`; ficava a 4,1 com alcance 4);
> **B1** sem arenito, o pedreiro o faz da areia da vila para o fundidor (`FurnaceFeed`); **D2** com
> pastor, a ceia deixa um par de cada comida do rebanho (emenda na ADR-037 R1). Verificado: test 1557/0;
> mutação derruba teste em cada um (C1 1, A1 1, B1 4, D2 2). Bateria inteira 832: só KF-006 e KF-007
> (nova, `MinerDirectPathGameTest`, navegação do jogo; bisseção em 22 baterias mostra a queda já sem o
> D2 e no arranjo de 830+ testes). **Próximo:** jogar o deserto na 0.3.86 e ler a linha *"stopped growing
> the road"* — ela decide entre A2 (rua da faixa com alternativas) e A3 (aterro além de 2).
>

> **0.3.75–0.3.77** (20-10, mesma branch, push feito, **JAR 0.3.77 instalado, não jogado** — `EF825151…`):
> terraplanagem fase 1 (lote ±2 da rua aplainado), rotas próprias (madeira/argila com prova, descascar),
> folha natural dá lugar à peça e a peça inalcançável surge depois de 3 vezes de lado
> (`villagecolony/unreachable-pieces.jsonl`, `VC_UNREACHABLE`). Taiga na bancada: 0 → 2 casas, nota 7,6.
> test 1552/0, runGametest 802/802. **Próximo:** o autor joga deserto e floresta pelo roteiro; eu gero o
> relatório com `scripts/biome_report.py`.
>
> **0.3.69–0.3.74** (20-10, branch `feat/biomas-simulacao`, **não jogado**): **o mod em todos os
> biomas.** Bancada de simulação sem jogador (`scripts/biome_sim.py`, chave só de teste
> `-Dvillagecolony.simulate`) achou que 3 das 5 vilas Vanilla nunca viravam colônia; corrigido:
> camas da mesma vila gerada se juntam, o estilo vem da vila gerada (ADR-003 Emenda 9), a casa
> fundadora procura até a borda da vila, aplaina até 2 e limpa árvore natural (G1). **G2:** cada peça que
> trava a obra tem contador próprio; 2 ciclos sem progresso e tudo aparece no baú do construtor, com
> registro permanente em `villagecolony/supply-gaps.jsonl` (ADR-040 emenda 20-10). **Caminho com o bloco
> natural do bioma** (arenito liso na areia etc.; só é rua onde a colônia assentou). **Mata fechada:** a
> obra comum limpa o lote (florestas, taigas, selvas, cerejeira, mangue); viveiro e bosque protegidos
> (`ColonyTrees`). **Mineiro no deserto:** escora o teto de areia em arenito antes de quebrar; enchimento
> da mina com a pedra do bioma. **Teste por bioma:** `/vc bioma`, `/vc bioma nota`, linha `VC_BIOME`,
> `scripts/biome_report.py`, auditoria `BiomeCoverageGameTest` (53 biomas) e o roteiro
> [`docs/testing/roteiro-por-bioma.md`](docs/testing/roteiro-por-bioma.md). Notas na bancada:
> planície 9,9, savana 9,2, taiga 8,8, neve 7,1–8,3, deserto 5,2–5,8, mata e ermos 3,0 (o relevo barra o
> lote: terraplanagem). Verificado: test 1552/0, runGametest 788/788 (`MinerDirectPathGameTest` falha
> ~1/3 já no 690dee98, instabilidade anterior). Detalhes: `docs/research/2026-10-20-biomas-previsao.md`.
> **Próximo:** jogar um mundo de bioma único pelo roteiro; terraplanagem (o maior bloqueio restante).

> **0.3.68** (20-10): **as bancadas funcionam de verdade para o jogador.** Defeito achado: as telas
> Vanilla (mesa, cortador, tear) só ficam abertas diante do bloco Vanilla exato — com o bloco do mod abriam
> e fechavam no mesmo tique, então o "o jogador usa as bancadas" da 0.3.63 **nunca funcionou**. Agora cada
> bancada tem tela própria e funciona como a estação Vanilla do ofício (decisão do autor): mesa
> (carpinteiro, lenhador, construtor, mineiro), cortador de pedra (pedreiro), **fornalha de verdade**
> (fundidor, BlockEntity próprio), tear (pastor), composteira (fazendeiro). Todas cubos cheios. **Também:**
> os blocos não tinham tabela de drop (quebrar não devolvia o bloco) — corrigido, com tags de ferramenta.
> **Texturas:** recorte novo pela grade real dos mockups (as faces sul/fundo da 0.3.65 pegavam a tarja da
> legenda) com 6 painéis diferentes; fazendeiro e mineiro ganharam arte nova do ofício
> (`scripts/assets/art_farmer_miner.py`). **Fundidor:** a argila ausente aparece no baú dele
> (`KnownAbsent`) em vez de 4 buscas inúteis por lote. **Não jogado.** Verificado: test 1534/0;
> runGametest 773/773 (`BenchUseGameTest`: telas com controle Vanilla, forja funde, composteira composta,
> todo bloco cai ao quebrar); mutações da tela e do drop derrubam os testes. JAR `village-colony-0.3.68.jar`
> = `CDD55F4F07D588B096C00C6401FDCA936B1DA8C057C0E0354E1A5BD3D1DF4819`, instalado. **Próximo:** simulação
> por bioma (plano em memória `bancadas-telas-e-biomas-plano`).

> **0.3.67** (20-10): **corrige os 3 defeitos graves da varredura de 20-10**
> ([`docs/audit/2026-10-20-varredura.md`](docs/audit/2026-10-20-varredura.md)), todos em código não
> jogado das 0.3.64/0.3.66. **G1:** a proteção da bancada zerava o prazo ao desistir e o artesão voltava
> a caminhar a cada 200 tiques (produção ~200× menor com bancada inalcançável) — agora, estourado o
> prazo, fabrica no lugar até o fim da tarefa. **G2:** a bancada era procurada todo tique (~9.800
> leituras de bloco por carpinteiro) — agora uma vez por tarefa (`CraftStation.Approach`). **G3:** o
> não-mineiro que desistia de escalar seguia marcado como encalhado, fora da fila, repetindo busca e log
> para sempre — agora volta à vila pelo teleporte da cama (`BedTeleport.toBed`); sem cama, é liberado.
> **Não jogado.** Verificado: test 1529/0; runGametest 769/769; mutação do G1 derruba o teste. JAR
> `village-colony-0.3.67.jar` = `A0594C3F1C24F75D9BEF3A6CBC3D15352114FBA59DC7C4DDEFCA4851E3E37E23`,
> instalado e conferido. **Próximo:** jogar ≥30 min na vila montanhosa (pastor fora do laço, artesão sem
> queda de produção, MSPT estável); depois o resto da ordem de ataque da varredura.

> **0.3.62→0.3.66** (15-10, branch `fix/climb-nao-mineiro-so-natural`): sessão de bancadas + terreno
> montanhoso. **0.3.62:** bancada do mod para as 8 profissões. **0.3.64:** só o mineiro remodela o
> terreno para escalar; as outras sobem por degrau natural (raio 6) ou desistem sem tocar o mundo
> (vila montanhosa — pastor em laço de encalhe). **0.3.65:** cada bloco do mod com os 6 lados
> diferentes (HD 64px recortadas dos mockups 1254px). **0.3.66:** o artesão vai até a bancada antes de
> fabricar (mod→Vanilla→normal) com salvaguarda do `APPROACH_BUDGET` — reconcilia com a regra de 10-11
> ("fabrica onde está"), bancada inalcançável não trava. **Nenhuma jogada** (0.3.66 instalada,
> FB13…→ver install-mod). Verificado: test 1530/0; runGametest 769/769.
> **Pendente (ociosidade, roteiro em memória):** 6 dos 7 ócios têm raiz na ALTURA → a **terraplanagem
> da vila** (nivelar a montanha para subida ≤1 bloco, sem afrouxar o critério de obra) é o plano futuro
> que resolve 4 deles; só o SMELTER (espera clay inexistente, C4) é acionável sem ela. Próximo: jogar
> 0.3.64/66, depois SMELTER, depois terraplanagem.

> **0.3.61** (15-10): **o desvio da mina respeita o beco** (A3, fecha o P1). O playtest da 0.3.60
> (Spark `HNi26AzwWD`, 44 min, **versão confirmada no log**) mostrou o reroute **executando** (10×) mas
> **sem atingir o aceite**: NO_STANDING subiu a 35×, MINER Regra 51 = 60%. Causa (leitura das coords):
> as recusas são uma **parede/teto de caverna** (50 de 59 em y67-68, cluster apertado), não pedras
> isoladas — o `roomBeside` escolhia um vizinho **na mesma região sem pé**, e o desvio trocava
> NO_STANDING por TOO_HIGH. **Correção:** o desvio não prossegue quando o alvo está num **beco**
> (`MineMarks.isInADeadEndAt`, ≥3 recusas na vizinhança), e o `roomBeside` passa a respeitar
> `isUnreachableAround` (não só `isOutOfReach`) — numa parede, deixa o contorno/curva agir em vez de
> desviar para dentro dela. O desvio segue valendo para a pedra **isolada**. **Não jogado** (0.3.61
> instalada e conferida, sha256 542A761B…). Verificado: test 1523/0 (2 novos); runGametest 769/769
> exit 0; mutação (ignorar o beco) derruba `aStoneInsideADeadEndDoesNotReroute`.
> **O próximo jogo precisa mostrar:** NO_STANDING caindo numa vila com caverna, MINER Regra 51 < 15%.

> **0.3.60** (15-10): **o mineiro desvia para a pedra ao lado antes de largar** (P1 do plano, mina).
> ⚠️ **Os consertos do mineiro (0.3.59 DirectPath, 0.3.60 reroute) ainda NÃO foram jogados:** os JARs
> não estavam na pasta de mods — os 3 últimos playtests rodaram **0.3.58** (o `SendUserFile` entrega ao
> chat, não instala; ver memória `jars-nao-instalados-deslocamento`). O salto MINED 0,8→~19/min foi de
> **0.3.57→0.3.58**, não do conserto do DirectPath. A 0.3.60 foi instalada e conferida (sha256
> F164319032…) em 15-10; o próximo jogo é o **primeiro** com o conserto do mineiro. Restava o 2º defeito
> em série, alvo deste ciclo: o approach **encala a 4-5 bl** quando o
> mineiro chega perto (22× NO_STANDING_POSITION + 9× TOO_HIGH, conclusão 11%). **Correção:** antes de
> `passOver` marcar e largar, se faltava só o pé (`worthRerouting` = só NO_STANDING), tenta
> `MineVein.roomBeside` (ADR-038 P6) e redireciona o alvo para a pedra vizinha que abre o pé; TOO_HIGH/
> sem-caminho seguem o contorno de sempre. **Não jogado.** Verificado: test 1521/0 (3 novos em
> `MinerRerouteTest`); runGametest 769/769 exit 0; mutação (desviar para qualquer rejeição) derruba
> `tooHighAndNoPathAreNotReroutable`. JAR `village-colony-0.3.60.jar` =
> `F164319032E6DE6342027DE336B89B7AD34A5CC42527A1A51E0A2AF37295C169`.
> **O próximo jogo precisa mostrar:** NO_STANDING_POSITION caindo (desvio funciona), MINER idle+blocked
> < 15% no expediente (Regra 51), `went one level deeper` > 1. Playtest **de dia**.

> **0.3.59** (14-10): **o mineiro recusa o falso caminho à superfície** (P0, mina). Playtest da 0.3.58
> (Spark `YOT1oqyhIv`, 100 min) mostrou TPS 20 estável, mas o mineiro passava **janelas longas**
> "walking to ‹pedra›" sem chegar — parado em y89-93 sobre alvo em y80, `went one level deeper` 1× em
> 100 min, só bursts intermitentes. **Causa (spike):** `MinerDirectPath.reaches` aceitava
> `path.reachesTarget()` sem conferir onde o caminho termina; o `findPathTo` sobe o alvo sólido/distante
> até a superfície (E32) e reporta "chegado" com o fim dezenas de blocos acima no Y, curto-circuitando
> o `MineApproachPath`/perna. **Correção:** `endReachesApproach` exige o fim do caminho a ≤2 Manhattan
> nos **três eixos** (Y incluído); `reachesTarget()` deixa de bastar sozinho. **Não jogado.** Verificado:
> test 1518/0 (3 novos); runGametest 769/769 exit 0 (1ª rodada foi flake ~25%); mutação (ignorar a
> distância) derruba `endOnTheSurfaceAboveTheStoneDoesNotReach`. JAR `village-colony-0.3.59.jar` =
> `FE20DDC6437E519A6158267CFF9FB7C199C96D6615FB2E81F605477E2CBBAF68`.
> **Aberto (2º defeito em série, próximo ciclo):** mesmo quando chega perto, o approach encala a **4-5
> blocos** (fora do REACH 4) — o NO_STANDING_POSITION/roomBeside (A1/A3), não medido nesta correção.
> **O próximo jogo precisa mostrar:** o mineiro chegar à frente e descer (`went one level deeper` > 1,
> MINED/min sobe), playtest **de dia**.

> **0.3.58** (14-10): playtest da 0.3.57 (Spark `iALIIoOwEK`, 42 min) — a horta destravou (obra
> avança, 201 peças, TPS 20), mas **ociosidade geral alta**: lenhador 98% ocioso, pedreiro 71%,
> pastor 80%, fundidor 60× "no task open". Raiz medida: a **vila está madura e sem obra** — 21×
> "no empty plot", estoque cheio (837 cobble, 322 toras); os produtores param porque não há obra
> pedindo, não por falta de material. E a **expansão era lenta**: esperava 3 voltas de varredura
> (`FULL_AGAIN_AFTER=3*FULL_AFTER`, dezenas de min) entre anexos. **Correção (decisão do autor):**
> `FULL_AGAIN_AFTER = FULL_AFTER` — a vila anexa a faixa já na volta que confirma a falta de lote,
> crescendo no ritmo em que fica ociosa; a salvaguarda contra inchaço (só lado plano/seco/sem outra
> colônia) permanece. **Não jogado.** Verificado: test 1515/0; runGametest 769/769. JAR
> `village-colony-0.3.58.jar` = `429511EA11985FA09724E6A69F5E4C77A222DBB4DD6824709ABF0FDBD39BA659`.
> **Aberto:** o mineiro ainda tem 172× NO_STANDING_POSITION (44% bloqueado) — o único ócio por
> defeito real, não por vila madura.

> Arquivo de estado vivo. **Sobrescreve, não acumula.** Se passar de 150 linhas, algo
> está errado: P0 não está fechando. O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md)
> (o STATE de 10-10 inteiro, 709 linhas, sem edição) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.

---

## 🔴 P0 — As profissões não trabalham de forma contínua; a mina é a raiz

Reavaliação 14-10 ([`docs/audit/2026-10-14-reavaliacao-pos-0359.md`](docs/audit/2026-10-14-reavaliacao-pos-0359.md)),
plano de execução ([`docs/technical/Plano-Melhoria-2026-10-14.md`](docs/technical/Plano-Melhoria-2026-10-14.md)).
O fio comum: **ação interrompida** (parada, bloqueada, esperando), e a **mina** concentra o P0.

**O que a mina é hoje (correção da varredura de 14-10):** o PMD atual mostra o pior método em
complexidade **62** (`MinerSteps.step`), seguido de `MineEdge.inside` 48 e `MineCuts.nextCut` 46 — a
mina concentra a complexidade (o A1 da varredura estava certo). O problema da mina é **complexidade +
tamanho de subsistema** (31 classes) e
**geografia** (boca a ~77–95 blocos numa vila grande), não um método-monstro. A 0.3.59 fechou o
mecanismo do falso caminho à superfície; **falta o 2º defeito em série (approach encalha a 4–5 bl) e a
geografia**. Em volta: cadeia de material que trava a obra (C2), profissões periféricas em laço
improdutivo (C4: fundidor pede clay inexistente, fazendeiro sem lote).

**Plano em execução:** Fase 0 higiene (H1 feito — este STATE; H2 time_ledger dia/noite; H3 TODO) →
Fase 1 produto (P1 approach 4–5 bl; P2 geografia da boca; P3 troca de tarefa) → Fase 2 forma (F1
subpacotes `work/`; F2 teto de 500; F3 complexidade PMD).

**Aceite do P0:** playtest de dia, ≥ 30 min; Regra 51 (ocioso+bloqueado+encalhado) ≤ 15% por ofício no
expediente; `MINED`/min > 0,8; nenhuma profissão em laço de recusa visível no `action_report`.

## Branches e processo

**`main` é a linha oficial** (ADR-035 §2). B1 resolvido: a `claude/autossuficiencia` foi integrada
(0.3.25→0.3.55) e os ciclos seguintes (0.3.56→0.3.59) são branches curtas por PR. Ciclo atual:
`fix/miner-directpath-superficie` (0.3.59 + reavaliação + plano), ainda sem push/merge.

## O que o próximo jogo precisa mostrar

JAR **0.3.59** instalado (o 0.3.58 foi jogado: Spark `YOT1oqyhIv`, 100 min; é dele a reavaliação).
Playtest **de dia** (CLAUDE.md §0.4):

| # | Item | Sinal |
|---|---|---|
| 1 | Mineiro chega à boca e desce | `MINED`/min > 0,8; sem "walking to nowhere" prolongado; telemetria do percurso (a implementar) |
| 2 | Profissões contínuas | `time_ledger`: Regra 51 ≤ 15% por ofício no expediente |
| 3 | Sem laço de recusa | `action_report`: soltas/pegas < 10%, sem o mesmo motivo repetido por profissão |
| 4 | Obra não trava | `grep VC_SUPPLY_ERROR` perto de zero; "opened a build task" avança entre obras |

Depois de jogar: `scripts/analyze_village_log.py`, `time_ledger.py`, `action_report.py`,
`cost_ledger.py` (CLAUDE.md §0.4).

## 🔴 Aberto

A fila completa e priorizada está no `TODO.md` (seção AUD) e no roteiro
(`docs/technical/Roteiro-Correcoes-2026-10-10.md`, tabela "Estado da execução").

- **2.4, fundidor e despacho:** o código da linha Codex está integrado; falta medir em jogo.
- **AUD-11 (parcial):** `fabric.network` já passa dos 60% (67%); faltam `fabric.command` (27%) e o cliente (0%).
- **AUD-04 e AUD-16:** só com gatilho (Fase 4). Sem número, sem otimização (ADR-035 §5).
- **Fase 5 (dívida):** AUD-15, 20, 10 e 09; o AUD-09 pede ADR.
- **Decisões do autor:** apagar as 7 branches já contidas na `main` (AUD-24); o número da
  regra do ócio, que divide o "Regra 51" com a da peça na 4ª tentativa (AUD-26); a ADR do AUD-09.
- **AUD-18:** tag por JAR publicado. Não foi feito: criar tags é publicação.

## Dívida conhecida

- Teto de 500 linhas virou colete (A3): **4 arquivos exatamente em 500** (`TreeChoice`, `SmelterWork`,
  `BuilderWork`, `ColonyGoals`), ~20 entre 470–500. Tratar 500 como gatilho de extração (ciclo F2).
- `runGametest` não filtra por teste dentro da bateria completa; `-PgametestOnly=Classe` roda
  uma classe isolada. Testes rodam juntos num mundo só: quem conta entidades num raio grande
  afirma sobre as suas (o `FreeChestCountGameTest` falhava 2 em 8 por isso).
- Cobertura da bateria: `fabric.command` e `fabric.network` baixas; cliente 0% (sem cliente no teste).
- Hooks do Claude Code em `scripts/hooks/`; ligá-los no `.claude/settings.json` é do autor.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/` (auditoria de 10-10, régua v2 na
  `METODOLOGIA.md`); `python scripts/assess/assess_project.py --run --gametest-runs 2`.
- **Verificações reprodutíveis:** `scripts/audit/check-*.sh` (README na pasta).
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse.
- **Ler o save quando o log não basta:** a RC2 só se decidiu lendo as tochas do mundo.

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest`, `StructureCoverageGameTest` | onde cada cadeia começa; quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
