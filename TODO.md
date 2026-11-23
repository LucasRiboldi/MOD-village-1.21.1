# TODO

Organizado pelo cronograma da avaliação de qualidade (`docs/audit/2026-11-13-avaliacao-qualidade.md`, seção 5): uma
seção por fase, prioridade dentro dela (🔴 bloqueia o portão, 🟠 importante, 🟡 melhoria). Só o que está aberto: feito
sai daqui e vai para o commit e o `CHANGELOG.md`. Tamanho: P até 1 dia, M 2 a 4 dias, G uma semana ou mais.

- Os 214 abertos do TODO antigo foram triados em 13-11: 139 fechados com o motivo e 75 trazidos para cá; o registro
  está em `docs/technical/TODO-herdado-2026-11-13.md`.
- O TODO antigo inteiro, por jogo, está em `docs/archive/technical/TODO-ate-2026-11-13.md` (`grep`).

## F0 — Sinal limpo (1 semana)

- [ ] 🟠 Decisão do autor: a ADR-024 (índice físico do armazém) é aceita, mas o `WarehouseIndex` não é usado em produção;
  religar ou retirar (P)
- [ ] 🟡 Decisão do autor: o `CLAUDE.md` importa `.fabric/AGENTS.md`, que a ferramenta Fabric cria localmente (pasta no
  `.gitignore`) e não existe em nenhum checkout; manter, reinstalar a ferramenta ou tirar o import (P)

## Varredura de falhas 12-11

Registro, fila e método em [`docs/audit/2026-11-12-varredura-de-falhas.md`](docs/audit/2026-11-12-varredura-de-falhas.md).
Nenhum crítico. A VF-004 (jogar a 0.4.68) foi feita.

- [ ] 🟠 VF-001: primeiro ciclo depois de abrir o mundo leva 471 a 1.212 ms; a linha `VC_COST_PHASES` reparte a fase
  `planner` por colônia — falta ler no próximo jogo (P)
- [ ] 🟠 VF-002: `workerCollectsWhileWalkingPast` — a colônia de teste deixou de ser dada como abandonada (722f7ec5), mas o
  teste ainda caiu em baterias de 14-11 (P)
- [ ] 🟠 VF-003: PIT a 85,17% contra o limiar de 85%; `MineShaftWaypointsTest` cobre o caminho da mina — conferir no CI (P)
- [ ] 🟠 GameTests de aldeão e animal interferem entre si porque os lotes rodam juntos num mundo só (KF-011 a KF-015 e os
  oito casos corrigidos em 14-11: relógio, raio estático, vila gerada, arena vizinha, ponto de partida do A*, IA da vaca,
  bicho do vizinho, tosquia). Correção estrutural: lote sequencial próprio ou área que nenhum outro teste alcança.
  Sobe a nota de D5 (G)
- [ ] 🟡 A* do jogo e água (Regra 278): no cenário do canal, o aldeão que parte de (0, 2, 0) nada sempre e o que parte
  de (0, 2, 1) vai pelo seco (31 nós). O teste passou a partir de onde o aldeão nasce. Suspeita não provada:
  `PathNodeNavigator` reescreve `pathLength` antes de conferir melhora e poda a volta pelo alcance de 48. Investigar se
  aparecer em jogo (aldeão nadando com caminho seco ao lado) (M)
- 🟡 VF-005 a VF-014 e 🟢 VF-015 a VF-020: só no registro

## F1 — A vila trabalha (3 a 5 semanas) → portão Beta 0.5.0

### Pendências do playtest 0.4.122-alpha

- [ ] **Verificar em jogo.** Três colunas do grafo com piso sobreposto, arco da mina (N12) e casa do mineiro (N28), incluindo recarga do mundo.
- [ ] **Verificar em jogo.** Tosquia e condução (N05, N26, N27), reserva de abate por espécie e curral, e orientação da placa do baú (N14).
- [ ] **Confirmar N26 no desnível lateral.** A vaca parava na fase `LEAD` com a guia presa. O pastor agora usa o lado oposto da abertura para manter a condução. O GameTest passou na bateria local completa após o ajuste.
- [ ] **Investigar N05 se a tosquia falhar novamente.** Na bateria local da 0.4.122, `theShepherdOnlyShearsAfterReachingTheSheep` parou com o aldeão longe da ovelha. Registrar a posição, o destino e a duração da tarefa no próximo caso.
- [ ] **Investigar.** Queda da contagem no curral grande. Cruzar as contagens com `VC_FLOCK_HARVEST`. O teste da guia longe da porteira passou com a porteira instalada no cenário.
- [ ] **Investigar ou corrigir.** N10, atraso de criação do baú (N14), lenhador na obra e pendências de terreno e acesso do Lote A. Conferir as evidências no [plano de 28 notas](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md).
- [ ] **Deliberar.** Cores das placas (N23) e proposta de N24.

- [ ] 🔴 [Jogo da 0.4.118](docs/technical/Playtest-0.4.118-2026-11-22.md) (22-11). Validar na 0.4.122 a reserva de abate por curral, a proteção
  de animais em recintos, os dois saltos, o fechamento antes da soltura e o desvio dos demais aldeões. Investigar a queda na contagem do curral grande com o novo
  log de abate. Reproduzir a entrada da vaca a partir do desnível lateral. Confirmar as três colunas de entrada do grafo e rastrear a atividade do lenhador na obra.
- [ ] 🔴 Jogo da 0.4.115 (21-11): 28 notas e seis imagens no [plano integral](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md).
  Lote B implementado na candidata 0.4.116: tosquia junto da ovelha e condução pela porteira (N05, N26, N27); aguarda jogo.
  Lote A, N12/N28: a 0.4.117 trata a recusa após mudar a passagem. A 0.4.122 preserva a altura do piso da entrada. Confirmar reconexão da mina e da
  casa do mineiro em jogo e após reabrir o mundo. Os outros pontos do Lote A e os lotes C (implantação/moradia),
  D (regras/experiência) e E (fluxo/custo) continuam abertos. Preservar os sucessos N01–N04 e N06.
  Decisões arquiteturais pendentes exigem deliberação.
- [ ] 🟠 N14: validar em jogo a orientação da placa em moradias novas e existentes. Medir separadamente
  quando a cama é atribuída, quando o baú aparece e quando ganha placa para reproduzir o atraso relatado (P).
- [ ] N23: inventariar os estados que chegam ao overlay e revisar as cores das placas. Aguardar a escolha do autor.
- [ ] 🟠 Jogo da 0.4.114: conferir se a candidata 0.4.115 impede o retorno ao mesmo telhado após o resgate;
  a memória de caminhada persistente foi reproduzida em GameTest, mas sua relação com o retorno ainda depende do jogo (P)
- [ ] 🔴 Ciclos de playtest até todo ofício ficar abaixo de 40% parado em dois biomas, dois jogos seguidos (G)
- [ ] 🔴 Conferir em jogo as candidatas 0.4.69 a 0.4.74 (lista no `STATE.md`) (por jogo)
- [ ] 🔴 Jogo da 0.4.79 (13-11): no próximo jogo, conferir `lots_ok` acima de 0 (Regra 282) e ler as linhas
  `VC_LOG_PLACED` para achar a origem dos troncos que o autor viu (Regra 281) (por jogo)
- [ ] 🟠 Regra 283 (0.4.83): conferir no jogo que o pastor ainda recolhe o animal solto perto do curral; a folga de
  10 blocos em volta da caixa saiu (por jogo)
- [ ] 🔴 Jogo da 0.4.84 (14-11), à espera dos dados do próximo jogo (0.4.85):
  - grafo: ler `loose=` no `VC_ROAD_GRAPH` e escolher entre tábua de carvalho como caminho (nota 3), entrada da
    plantação no nível do bloco da plantação (nota 7) e entrada de estrutura em solo de pedra (nota 8) (M);
  - terreno: ler `VC_SMOOTH` (motivo de cada coluna recusada; suspeita: flor dentro de peça original da vila sai
    `protected:`) e `VC_LOT_EDGE` (degrau em volta do lote), e escolher a correção dos muros (nota 1) (M);
  - lagos isolados dentro da vila: a Regra 288 (0.4.87) aterra toda água dentro da vila na geração; conferir em
    mundo novo (por jogo);
  - mineiro (Regra 286): conferir se dorme na casa junto da mina; a água (Regra 285) resgatou 10 vezes no jogo da
    0.4.85 sem precisar de gelo (por jogo)
- [ ] 🔴 Jogo da 0.4.85 (15-11, planície, Spark `CxCKzJmQ5r`), achados da triagem ainda sem correção:
  - lenhador 46% parado (`has not moved a block` 18×) e tora de baixo deixada na árvore (nota em -238,68,212) (M);
  - degrau de 2 blocos no meio da vila (nota em -217,68,221); `VC_LOT_EDGE` com `maxStep` até 6 e
    `VC_SMOOTH` recusando por `protected:air` (M);
  - portas soltas do grafo: `loose=door@-242,68,210` e `door@-175,70,234` (casa do mineiro) em quase todo ciclo (M);
  - blocos sem origem: bloco solitário (-228,64,272) e tronco (-232,67,256) (P);
  - plantação grande: arar toda a estrutura e plantar sementes variadas dos baús, surgindo se faltar (nota) (M);
  - pedidos novos: arco da mina com piso de pedra de raio 6, baú ao pé e céu livre; casa do mineiro com luz
    externa e telhado medieval (M);
  - construtor 42% parado (24% bloqueado), pastor 25%, ferreiro 29% (Regra 51) (M)
- [ ] 🔴 Jogo da 0.4.88 (15-11, mundo novo em montanha, Spark `b28MY9TPEA`), escolhas 1A a 5A do autor:
  - grafo oscilando, peças em pilar, parede em volta da obra e fundação com 16 adultos: Regras 295 a 298 na 0.4.89
    (candidatas; o jogo da 0.4.89 mostrou 1 por ofício, `maxStep` ≤ 2 em volta das obras e 37 peças assentadas);
  - atividade por profissão: mineiro 39% e construtor 32% parados (61 `SET_ASIDE`); fazendeiro 1% sem plantação
    grande na vila nova (5A) (M)
- [ ] 🔴 Playtest da 0.4.113 em mundo novo — as nove escolhas de 21-11 (A8, A7, A9, A6, A2, A3a, A4, A5, R1)
  estão no [plano do ciclo](docs/technical/Proximo-ciclo-0113.md). Rodar `/vc salvar` antes de sair e conferir o arquivo (G)
  - Sessão de 21-11, 04:41–05:10, `VC_SUPPLY`: pedregulho ×127 (`MISSING_PIECE`, 3 entregas alternativas),
    pó de osso ×144 (9 lotes para o bosque), mudas ×8, fundação ×87 (79 de lote e 8 de talude) e caminho de terra ×1.
    Buscar fluxo físico de pedregulho e pó de osso. Distinguir os 52 drops declarados (16 corantes, 20 cristais e
    16 fragmentos de prismarinho) e os 48 animais iniciais das peças da obra.
- [ ] Jogo da 0.4.114: acompanhar o motivo da espera e a retomada por aldeão, a posição da pedra
  que a escada espera para cavar e o trânsito entre as duas redes após uma união. Salvar com `/vc salvar`.
- [ ] 🔴 Jogo da 0.4.111 (20-11, planície, colônia e8ee5291, 106 min), análise em
  `docs/audit/2026-11-21-analise-jogo-0111.md`:
  - mina com muitas aberturas e o laço do bloco solto: Regra 348 na 0.4.112 (candidata; conferir no próximo jogo);
  - o mineiro morreu para um zumbi no salão do primeiro nível (A2) (G);
  - cadeia da tora: 124 peças postas de lado, tora em estoque 0, carpinteiro e pedreiro esperando (A3) (G);
  - fazendeiro 0 colheitas entregues e pastor 6 de 6 tarefas soltas: diagnósticos A4 e A5 esperam jogo (M);
  - fundidor 71% parado: argila não achada 9× (A6, Regra 352 candidata), nenhum minério de ferro (A1) (M);
  - terra 868 no estoque, 314 recusadas (A7); travas `smelter_raw`, `bone_meal`, `value_raw`, `mine_fill` seguram
    demais, `ingot` e `wool` não agem (A8); grama espalhando conta como "o mod desfez o jogador" (A9) (P);
  - o `/vc salvar` não traz mina, grafo, obras, mortes, laços nem o jogo inteiro (R1) (M);
  - blocos que surgiram (`VC_SUPPLY`): pó de osso ×736 (`BONE_MEAL_FOR_THE_FARM`, declarado), lã branca ×117
    (`WAITED_TOO_LONG`), argila ×72 e tora ×64 (`MISSING_PIECE`), argila em bola ×32 (`CLAY_ON_REQUEST`), terracota
    branca ×10 (`APPEARING_PIECE`) — buscar o fluxo de fabricação da lã, da argila e da tora (M)
- [ ] 🔴 Jogo da 0.4.89 (15-11, mundo novo, Spark `hYkEcERV1A`, 39 min), triagem:
  - casa sem solo, frente da porta, nó da mina, curral vazio, porta solta e argila: Regras 299 a 304 na 0.4.90
    (candidatas; conferir no próximo jogo);
  - nivelamento ao longo do grafo: o pedreiro aplica a suavização da Regra 297 aos trechos do grafo (opção C) (M);
  - blocos que surgiram (`VC_SUPPLY`): argila ×240 e argila em bola ×8, cerca de carvalho ×40 e tora ×25
    (`MISSING_PIECE`); terracota branca ×47 (`APPEARING_PIECE`); prismarinho ×144 (`AUTOMATIC_DROP`) — buscar o
    fluxo de fabricação de cada um (M)
- [ ] 🔴 Jogo da 0.4.93 (19-11, montanha, colônia b66aecbf, Spark `0SMEEkMLGP`, ~35 min), triagem e escolhas do autor:
  - a vila foi medida em 175 × 152 com y de 82 a 173; o centro ficou em 195, 133, 57, no ar, e a BigHouseMOD em
    124, 149, 5, uns 60 blocos acima das casas (y 87 a 90). A boca da mina (151, 136, -18) nunca foi alcançada
    (`VC_REACH unreached`), a casa do mineiro ficou 41 min sem lote e saiu em 141, 160, -17; mineiro 0% de trabalho,
    10 `NO_PATH`. Escolha A refeita (B-1): a BigHouseMOD nasce na altura das casas, Regra 338 na 0.4.102 (candidata); o
    centro em x e z já caía no meio das casas, e o y dele quase não pesa (G);
  - blocos de terreno voando acima das casas (print do autor): o save ao sair não tem nenhum pedaço de terreno solto
    na caixa da vila; causa: a casa desce ao solo suavizado e a coluna dela não era cortada acima do piso. O autor trocou a D (linha
    `VC_FLOAT`) por limpar na geração: Regra 337 na 0.4.101 (candidata; conferir no próximo jogo em montanha);
  - Regra 51 em todos os ofícios: mineiro 73%, fundidor 55% esperando, fazendeiro 40%, construtor 31%, pastor 22%
    (12% bloqueado), pedreiro 20%, lenhador 19% (76% andando) (M);
  - blocos que surgiram (`VC_SUPPLY`): pó de osso ×672 (`BONE_MEAL_FOR_THE_FARM`, declarado), pedregulho ×36, tora
    descascada ×30 e tora ×10 (`MISSING_PIECE`), terracota branca ×31 (`APPEARING_PIECE`), pedra ×16
    (`WAITED_TOO_LONG`) — buscar o fluxo de fabricação do pedregulho, da tora descascada e da pedra; o pó de osso
    ×672 em 35 min pede teto (M); nenhuma nota `VC_BIOME_NOTE`
- [ ] 🔴 Regra 44 vazou (autor 20-11): o trabalhador do mod ganhou roupa de ofício Vanilla. Causa: a varredura
  limpava antes de contratar e só via 64 blocos do centro. Regra 341 na 0.4.105 (candidata); conferir no
  próximo jogo que `VC_VANILLA_JOB` não mostra `stripped` repetido do mesmo aldeão (M)
- [ ] 🟠 Telemetria da interferência do jogador (F1–F9) e a **reafirmação** (F3b: o jogador refaz o que o mod desfez,
  ponto de atenção que vira item a corrigir): plano em `docs/research/2026-11-20-telemetria-dos-aldeoes.md` §4 (M)
- [ ] 🟡 Próximas telemetrias A–E (movimento, fila, produção, mundo, a própria medida): mesmo plano, §4 (M)
- [ ] 🟢 Roupa própria das profissões do mod: análise em `docs/research/2026-11-20-skins-das-profissoes.md` (viável,
  caminho S1: camada no cliente com a profissão sincronizada; texturas por script Pillow com prévia) (G)
- [ ] 🔴 Jogo da 0.4.93 (19-11, planície, colônia ccac8347, Spark `AYGQ78wh44`, TPS 20, ~3 h), triagem:
  - o calçador sem ofício deixava tronco do carvalho grande no caminho: Regra 326 na 0.4.94 (candidata);
  - o pastor levava de novo o animal já no curral e ficava travado com nome vermelho: Regras 327 a 329 na 0.4.95
    (ADR-058, candidatas);
  - a terra tirada pelo jogador voltava: Regra 334 na 0.4.98 (candidata);
  - o nó da casa do mod fora do grafo (410, 65, -199): a Regra 335 (0.4.99) tira porta de cima, mas o teste com a
    BigHouseMOD real em chão plano liga a entrada; a causa no terreno da vila sai da porta solta no `VC_ROAD_GRAPH`
    (`door@...:g...[...]`) do próximo jogo (M);
  - fazendeiro 66% sem trabalhar (29× "could not reach the crop" no trigo do curral, 17× "fenced in"): Regras 332 e
    333 na 0.4.97 (ADR-060, candidatas); fardo de feno e culturas novas (opção C) ficam para depois (M);
  - Regra 51: mineiro 33% (27% era descanso de 13 desistências, 11 com o alvo da descida no pé ou na rocha): Regra 336
    na 0.4.100 (ADR-061, candidata; conferir no próximo jogo); fundidor 48% e carpinteiro 42% sem trabalhar: Regras 339, 340, 342 e 343 na 0.4.103 a 0.4.107 (as 4 fases do plano de
    ofícios e reservas: depois reserva por classe de bloco com a ADR-062, `VC_BLOCKS` e `/vc blocos`, e a medida
    das travas pequenas com `VC_KEEP` e `stock_report.py`) (M);
  - o caracol girado com degrau cavado (ADR-037 M1) recomeça a escada; com a Regra 336 o mineiro desce cavando, mas
    a escada antiga fica (G);
  - `dirt_path` sem receita e posto como `SHAPED_GROUND` (Regra 69, declarado)
- [ ] 🔴 Simulação de 3 dias da 0.4.92 (18-11, planície, modo `vizinhanca`, colônia 58cb0273), achados:
  - fila do terreno, lenhador encostado no tronco e plantação grande: Regras 323 a 325 na 0.4.93 (candidatas);
  - fila do terreno parada no dia 1 (`levelled=0`): ninguém chegou à fila (M);
  - lenhador 59% andando e 26% ocioso; `STRANDED` "fenced in" 10× num curral; espera "no task open for it" depois de
    cada soltura (M);
  - pastor 53% bloqueado (M)
- [ ] 🔴 Jogo da 0.4.91 (17-11, mundo novo, Spark `pkfBfAxYsv`, 6 min), triagem e escolhas do autor:
  - bloco de caminho alto na frente das portas (15 de 17 portas no save) e BigHouseMOD sem caminho na porta: Regras 313
    e 314 na 0.4.92 (candidatas; só em vila gerada depois);
  - jogo curto: sem `VC_TIME`; as Regras 305 a 312 seguem sem medida em jogo;
  - blocos que surgiram (`VC_SUPPLY`): pó de osso ×16 (`BONE_MEAL_FOR_THE_FARM`, declarado) e terra arada ×9
    (`SHAPED_GROUND`) — nenhum novo a buscar fabricação (P)
- [ ] 🔴 Jogo da 0.4.90 (16-11, Spark `91TxnT5c0X`, 248 min), triagem e escolhas do autor:
  - entrada da casa (`doors:0`), aresta fixa, telhado como chão, argila em laço, curral vazio, fazendeiro parado,
    construtor sem areia e mineiro encalhado 14 vezes: Regras 305 a 312 na 0.4.91 (candidatas; conferir no próximo jogo);
  - blocos que surgiram (`VC_SUPPLY`): areia ×127 (`MISSING_PIECE`, para vidro), tora ×18 e pedregulho ×6
    (`MISSING_PIECE`), argila ×225 e bola de argila ×149 (`CLAY_ON_REQUEST`), terracota branca ×47
    (`APPEARING_PIECE`), grama ×87 (`SHAPED_GROUND`) — buscar o fluxo de fabricação da tora e do pedregulho (M)
- [ ] 🔴 Jogo da 0.4.86 (15-11, planície, Spark `GHWvjlPaPs`, 37 min, 8 notas), achados da triagem:
  - casas e lampião sobre o lago (notas 10:15 e 10:24): Regra 288 na 0.4.87, candidata; conferir em mundo novo (por jogo);
  - fazendeiro 0%: faltava semente em baú; Regra 289 (candidata) — obra semeada, semente que surge, terra arada,
    2 fazendeiros por plantação grande;
  - grafo e tábua: Regra 291 (candidata); a casa do mineiro a cerca de 80 blocos do centro, fora da caixa, segue
    solta (`door@-278,67,-205`): o caminho fora da caixa só conta se encostar no de dentro (Regra 224) (M);
  - torres e `protected:air`: Regra 292 (candidata) — a torre era minério na coluna; conferir em mundo novo e com o
    pedreiro em mundo antigo;
  - arco da mina: Regra 293 (candidata) — pátio de pedra de raio 6, céu livre, nó de entrada à frente do arco;
  - lenhador 71% parado (`NO_PROGRESS` 7×), construtor 40% (`SET_ASIDE: no material` 132×) (M);
  - contagem dos blocos que surgiram sozinhos não feita na triagem (P)
- [ ] 🟠 Estado e implementação nas 81 regras de `docs/RULES.md` que não têm essas colunas (M)
- [ ] 🟠 Número próprio para o teto de 15% de ócio; "Regra 51" hoje tem dois sentidos (P)
- [ ] 🟠 Fechar os conflitos C-12, C-13 e C-15 de `docs/audit/RULE-RECONCILIATION.md` (P)
- [ ] 🟠 Estado "confirmada em jogo" nas regras e no `STATE.md`, com as já vistas marcadas (P)
- [ ] 🟡 "Recurso que surge" reduzido a exceções declaradas e medidas por jogo (M). No jogo da 0.4.68: trigo ×467,
  lã ×144 (`WAITED_TOO_LONG`); tora ×43, tábua ×20, pedra lisa ×8 e bancadas (`MISSING_PIECE`); terracota ×47
  (`APPEARING_PIECE`); prismarinho ×144, favo ×20 e corantes (`AUTOMATIC_DROP`)
- [ ] 🟠 ADR-055 item 12: segunda frente de mina (com 3 ou mais mineiros, desce três níveis pela mesma escada e cava lá); pede ADR nova antes do código (G)
- [ ] 🟡 **Item MAE:** primeira obra pedida em jogo pela fila do construtor (`/vc mae obra <modelo>`, `VC_MAE_SITE` de
  `queued` a `done`), obra longe da vila, socorro (Regra 222) e a recusa que só mostra o motivo do último lote
- [ ] 🟡 **Nascimentos:** 0 em 63 min na 0.4.43 com 28 camas para 19 aldeões; conferir nascimentos por dia e `UPKEEP`
  no diário, e se a manutenção tira tábua da obra
- [ ] 🟡 **Grafo:** `Corridor` e `StripLot` ainda traçam caminho próprio antes de pedir ao grafo (ADR-048);
  `LINKS_PER_READ`=1 e a ligação que falha gasta a vez das outras partes; obra demolida não solta o ramal fixo
- [ ] 🟡 **Velas:** o carpinteiro faz 16 velas de cada uma das 17 cores para o estoque, e nada as usa
- [ ] 🟡 **Guarda de laço:** cobre oficina e fornalha; a coleta (lenhador, superfície) segue só com os prazos de sempre
- [ ] 🟡 **Reserva da mina:** reserva exclusiva por bloco e frente exclusiva por mineiro (dois mineiros no mesmo túnel)
- [ ] 🟡 **Chegada física:** caminho calculado que não vira chegada em 140 tiques, mesmo com `WorkTargets` ativo;
  separar geometria, tolerância de chegada e interferência do Brain antes de mexer
- [ ] 🟡 **Medida do diário:** trabalho auxiliar sem marca ainda conta como espera no `time_ledger`; medir `NO_PATH` e
  `NO_PROGRESS` por ofício
- [ ] 🟡 **Entrega física da obra:** medir no save a entrega de `CALCITE` pelo mineiro contra as inserções instantâneas;
  bloco sem catálogo ganha cadeia de profissão ou fica no recurso terminal; reserva sem retirada não deve impedir
  outra profissão de atender a peça
- [ ] 🟡 **Biomas** (jogos de bioma do portão Beta):
  - taiga: casa presa no último bloco por tronco natural no volume e por peça sem apoio (`UNSUPPORTED`);
  - deserto: mineiros andando 36% da vila alta até a mina do lago; `storage_majest` pedindo deepslate, calcita, folha de
    acácia e pinheiro; fundidor e fazendeiro esperando 41–42%;
  - rotas que travam na execução (`STALLED`): arenito liso, cortado e comum no deserto; bloco e camada de neve;
    vidraça e terracota colorida na savana;
  - terraplanagem fase 2 (subida de 1 em 1) para ermos e taiga nevada;
  - lavoura e viveiro com camada de neve em cima do chão;
  - reserva de madeira onde não há árvore (lenhador 85–88% bloqueado no deserto);
  - mineiro com alvo alto demais e sem onde pisar (`TOO_HIGH`, `NO_STANDING_POSITION`);
  - `NO_TASK` de tronco e madeira de acácia na savana; melancia sem rota

## F2 — Save e desempenho (3 a 4 semanas, em paralelo com a F1)

- [ ] 🔴 Cópia de segurança do `villagecolony_colonies.dat` antes de gravar, ou leitura de recuperação que não grava o
  estado vazio quando o arquivo existia e falhou (M)
- [ ] 🔴 Saves de 2 ou 3 versões publicadas lidos num teste; GameTest de parar e reabrir o servidor (M)
- [ ] 🔴 Ciclo da colônia repartido entre tiques, com orçamento de tempo e retomada (hoje até 93 ms com 1 vila e
  458 ms com 4 ou mais) (G)
- [ ] 🟠 Versão de esquema nos outros 4 arquivos que o mod grava (P)
- [ ] 🟠 NullAway em `data/save` e `fabric/adapter` (P/M)
- [ ] 🟠 Overlay recortado pela vila do jogador ou pela distância (hoje manda todos os trabalhadores a todo jogador) (P)
- [ ] 🟠 Gravação dos ledgers e do diário fora da thread do servidor (P)
- [ ] 🟡 Perfil "carga" no Sentinela, com N vilas e teto de custo por tique (M)
- [ ] 🟠 **Estado só em memória:** o plano do caminho (`PlannedPaths`) e as bocas antigas da mina (`MineEdge.PAST`) se
  perdem ao reiniciar o jogo
- [ ] 🟡 **Custo do MAE:** cada `/vc mae obra` travava o servidor cerca de 2,5 s (`MaeSites.start`) e a
  `MaeGenerationEvidence.onChunkGenerate` custava 6,4% do servidor (Spark `btHEJgIYWb`); medir de novo
- [ ] 🟡 **Estoque sem teto:** pedregulho de 554 a 13.579 em 16 min na 0.4.42 e 2.256 na 0.4.68; ver se falta teto de
  produção ou se é leitura de baús
- [ ] 🟡 **Testes do expediente:** `WorkClockTest`, `WorkHoursGameTest`, `OffHoursReleaseGameTest` e
  `BuildProgressGameTest` usavam o expediente antigo; `TaskLoopGuard` e `MineSideWork` sem teste
- [ ] 🟡 **Bateria de escassez** (R-158): falta total de madeira, água, semente, animal, combustível, ferramenta, baú
  livre e lote; esperado: alternativa válida ou bloqueio explícito

## F3 — Servidor e compatibilidade (2 semanas) → portão versão candidata 0.9.0

- [ ] 🔴 `ColonyTorches` fora da geração de mundo ou seguro entre threads (`HostileSpawnMixin` em `CHUNK_GENERATION`
  usa `HashMap` estático nas threads `Worker-Main`) (P)
- [ ] 🔴 Servidor dedicado com 2 clientes por 30 minutos, com Spark e `cost_ledger` (M)
- [ ] 🔴 Matriz mínima com Lithium, FerriteCore, ModernFix e um mod de aldeão: o jogo sobe e o mundo abre (M)
- [ ] 🟠 **Flowing Fluids** (instalado no jogo do autor, `flowing_fluids 1.0.8`): água finita com deslocamento. No jogo de 13-11 o bloco posto num buraco de água empurrou a água para cima ("enableDisplacement": true); não é defeito do Village Colony, mas ele muda a água que a horta, a plantação grande e a mina esperam. Desativado no jogo do autor em 13-11 (JAR renomeado para `.disabled`, decisão dele); volta na matriz de compatibilidade (M)
- [ ] 🟠 Permissão de op em `/vc bioma nota` e `/vc dados`, com limite no texto (P)
- [ ] 🟠 Teto ou rotação do diário e dos ledgers; interruptor das linhas `VC_*` em produção (P)

## F4 — Experiência do jogador (2 a 3 semanas)

- [ ] 🔴 Todas as mensagens em chaves de idioma, com inglês (hoje 49 em `Text.literal` e 25 traduzíveis) (M)
- [ ] 🔴 `/vc config` com interruptor por vila e pausa da colônia (M)
- [ ] 🔴 Guia curto do jogador em inglês e português: o que acontece, o que dá para fazer, como parar (M)
- [ ] 🟠 README de jogador separado do de desenvolvimento; capturas e GIF (P)
- [ ] 🟡 **Mensagem do fundidor:** "none of N colony chests had" mente quando o cru existe e a reserva impede

## F5 — Publicação (1 a 2 semanas) → portão 1.0.0

- [ ] 🔴 Declaração de uso de IA no README e nas páginas (o Modrinth exige desde 08-2026) (P)
- [ ] 🔴 Procedência do ícone, das texturas e das estruturas; `NOTICE` com a `big_house_mod.nbt` (cópia editada da
  Mojang); decidir refazer ou declarar (P/M)
- [ ] 🔴 Páginas no Modrinth e no CurseForge com descrição em inglês, ícone, galeria e changelog (M)
- [ ] 🔴 Release que roda os testes e publica o mesmo JAR jogado, amarrada ao CI do mesmo SHA (M)
- [ ] 🟠 Versão semântica a partir do corte de funcionalidades; canais beta e estável (P)
- [ ] 🔴 Fechamento: dois playtests limpos na candidata e Regra 51 (≤ 15%) em três biomas (G)

## Depois da 1.0 (dívida declarada)

- [ ] Contexto por servidor no lugar dos serviços estáticos de `VillageColonyMod` (G)
- [ ] `fabric/work` em grupos por profissão com dependência num só sentido (G)
- [ ] NullAway em toda a camada fabric (G)
- [ ] Nenhum método acima de complexidade cognitiva 25; teto por complexidade no lugar do de 500 linhas (M)
- [ ] Comentários datados abaixo de 300 (contínuo, ADR-035 §6)
- [ ] PIT estendido a pacotes puros de `fabric/work`; 10 a 20 propriedades jqwik nos sistemas com mais sobreviventes (M)
- [ ] Migração da fronteira da mina entre formas, em vez de descartá-la (G)
- [ ] `doMobGriefing` ou gamerule própria para o trabalho que altera terreno (M)
- [ ] **Regras provadas só por classe:** subir as ~95 regras em que o teste só usa a classe indicada para um teste que
  cita a regra (`docs/testing/regras-e-testes.md`); provas parciais das Regras 178, 172 e 171; sem teste próprio as
  Regras 220, a desistência da obra pedida, `MaeSites.move`, a busca de lote pela rede, o fundidor nas ajudas de chão, a
  pedreira em `MineDigging`, o teto de pastor, o diário com produto, a zona que cresce na ponta e o registro de cada
  suprimento (Regra 111)
- [ ] **Frutos de caule:** melancia, abóbora e jarro fora da plantação grande até o fazendeiro saber colher
- [ ] **Quarteirões:** o grafo só fecha retângulo pelo anel, pelas retas e pela ponta; sem busca própria
- [ ] **Sentinela futuro:** camada visual de cliente em spike isolado, publicação Allure entre máquinas, laboratório
  serial de carga com JFR (`docs/superpowers/plans/2026-11-02-sentinela-futuro.md`); bancada de bioma mais fiel
- [ ] **Contratos de tarefa:** ausência com invalidação por evento (R-021/R-027), prazo e critério de sucesso na `Task`
  (R-047/R-050), estados de disponibilidade (R-019), animal inacessível fora do plantel (R-107)
- [ ] **Sistemas novos, sem decisão:** transporte físico de itens (R-140 a R-142), itens de outros mods (R-026),
  adaptador para versões futuras do Minecraft (R-003/R-163)
- [ ] **Estilo do bioma vizinho** para biomas fora da tabela (ermos, picos de gelo, selva, floresta escura, cerejeira)
- [ ] **Cobertura:** `fabric.command` em 27% e o cliente em 0% (AUD-11); imports não usados (AUD-15)

## Regra de teste

Todo teste novo ou alterado: **o commit diz a mutação feita e que o teste caiu com ela**. Teste que passa com a regra
desligada não entra como prova. Cenário fora da arena de 8x8 bate na barreira da borda.

## Decisões do autor que faltam

Nenhuma: as 14 de 13-11 estão na ADR-055.

## Decidido, não fazer

- Obra antiga fica como está: mudanças valem daqui para frente (casas antigas sem piso não são refeitas).
