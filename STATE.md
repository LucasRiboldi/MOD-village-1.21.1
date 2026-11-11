# STATE — 2026-11-10

> **Retomada por outro agente:** comece por `HANDOFF.md`. A 0.4.43 está publicada (`v0.4.43-alpha`), e a análise do jogo dela espera a escolha do autor em `docs/audit/2026-11-07-jogo-0443.md`.

> **Candidata 0.4.50:** opções da análise `docs/audit/2026-11-11-atividade-das-profissoes.md` escolhidas pelo autor (Regras 235 a 238):
> pastor por rebanho (16 animais por pastor), pedreira de areia e arenito no deserto, composteira pela roça, rebanho até 16 com comida
> sobrando. M2 e C1 não foram feitos: pedem decisão de arquitetura e voltaram ao autor. Não jogada.
> Sentinela `fast`: 1079 GameTests e 1784 unitários; em cada bateria caiu um instável conhecido (`workerCollectsWhileWalkingPast`, depois
> `animalsWalkInThroughEachOfTheFourGates` duas vezes, que passou 3 de 3 isolado). 6 sabotagens pegas.

> **Candidata 0.4.49:** pedido do autor em 11-11 (Regra 234): o sinalizador dos caminhos pinta de verde o trecho já travado pela
> Regra 225, coluna a coluna. Só desenho; o trecho travado segue na releitura. Não jogada.
> Sentinela `fast` verde de primeira: 1076 GameTests e 1783 unitários. 3 sabotagens pegas.

> **Candidata 0.4.48:** pedido do autor sobre o jogo da 0.4.47 (Regra 231): o grafo lê o caminho debaixo de laje e telhado quando
> restam 2 livres em cima; com menos, vira solo. A ponta azul fica só na extremidade do grafo no lado dela e aponta para fora. Não jogada.
> Sentinela `fast` verde de primeira: 1076 GameTests e 1783 unitários. 4 sabotagens pegas.

> **Candidata 0.4.47:** equilíbrio das profissões, sobre a análise de 11-10 (Regras 232 e 233): a obra manda no artesão; estoque de
> enfeite espera a obra parada; lenhador não larga a árvore enquanto se aproxima; fazendeiro primeiro com comida em falta; mineiro sem
> vaga nova com pedra sobrando; diário grava o produto. Não jogada.
> Sentinela `fast`: 1075 GameTests e 1781 unitários; nas duas baterias caiu um teste instável cada (`workerCollectsWhileWalkingPast`, e
> `eachSpeciesGetsItsOwnPen` por tempo, que passou 3 de 3 isolado). 5 sabotagens pegas.

> **Candidata 0.4.46:** as pontas levam a vila (ADR-052, Regras 229 e 230): uma ponta azul por lado, 1 bloco por dia para fora, até 128
> blocos; a caixa alcança a ponta calçada que sai 7 blocos. Não jogada.
> Sentinela `fast` verde de primeira: 1073 GameTests e 1778 unitários. 5 sabotagens das pontas pegas.

> **Candidata 0.4.45:** fechamento do MAE (ADR-051, Regras 226 a 228): socorro só quando nem casa cabe, em bairro; `/vc mae obra` com
> todos os motivos de recusa e tempo em `ms`; a obra do jogador aterra água interna da vila, não mar nem rio aberto; ruína de save antigo
> sai, só o bloco igual ao da planta. Não jogada. O que falta do MAE é prova em jogo: `TODO.md`, seção do MAE.
> Sentinela `fast`: 1070 GameTests e 1777 unitários, verde na terceira bateria; nas duas primeiras falhou um instável conhecido em cada (`workerCollectsWhileWalkingPast`, `animalsWalkInThroughEachOfTheFourGates`). PIT local: 85% (3314 de 3897). 9 sabotagens do MAE pegas.

> **Candidata 0.4.44:** grafo estável, pedido do autor em 11-10 (ADR-050, Regras 224 e 225): toda parte solta liga em reta ao ponto mais
> perto da linha; entra o caminho de fora da caixa que encosta no de dentro; o teto de 6.000 colunas não cala mais o grafo; trecho com 10 dias
> sem mudar fica fixo. Passo 4 da pesquisa (solo para todo caminho fora do grafo) espera medir `offLine` em jogo. Não jogada.
> Sentinela `fast` verde: 1062 GameTests e 1777 unitários. 10 sabotagens pegas pelos testes. PIT não rodou; o core mudou.

> **Candidata 0.4.43:** conceito desenhado pelo autor sobre o jogo da 0.4.42 (Spark 7d0PErq0RJ), Regra 223: branco colado na porta, só onde se entra,
> sem ramal para quem não tem entrada. Publicada, e jogada em 2026-11-07 (Spark u6XC34ZC0k): 0 quadrados de nós e 0 brancos soltos. A 0.4.42 foi jogada sem disparar o socorro: nenhum `VC_MAE_SITE`.
> Sentinela `fast`: 1060 GameTests e 1768 unitários; a primeira bateria falhou só no teste instável da vaca, a segunda ficou verde. PIT não rodou nesta versão, e o core mudou (`RetiredSpurs`, `RoadSquares`, `RoadGraph`).

> **Candidata 0.4.42:** Fase 6 do MAE no recorte escolhido pelo autor (ADR-049, Regra 222): sem lote depois de 2 voltas, a vila pede sozinha
> uma obra a até 24 blocos da caixa, longe da água, com aviso no chat. O bairro novo do plano mestre fica fora. Não jogada, e a 0.4.41 também não.
> Sentinela `fast` verde: 1052 GameTests e 1758 unitários. PIT não rodou de novo: nenhuma classe do core mudou.

> **Candidata 0.4.41:** "vila mais viva", pedido do autor sobre o jogo da 0.4.40 (Spark WCWRBHTKlX). Causa dos poucos nascimentos:
> 201 trigos e 35 comestíveis nos baús, porque só o baú do fazendeiro assava; a ceia assa de qualquer baú (Regra 217). Com um de cada
> tipo de pé, cada 5 adultos dão uma obra repetida (Regra 218). Rotina à vista de quem está sem tarefa, ainda contada como ócio (Regra 219).
> Quem não tem ofício calça e tira neve (Regra 220). A vila gasta comida, ferramenta e manutenção (Regra 221). Não jogada: nascimentos,
> falta de material e custo por tique só se medem no próximo jogo.
> Sentinela `fast` verde: 1045 GameTests e 1758 unitários. PIT local: 85,8% (3231 de 3766). Instáveis em aberto no `TODO.md`.

> **Candidata 0.4.40:** varredura de conflitos aplicada (ADR-048): todo caminho e toda rede pelo grafo, em toda vila; só aldeão calça; pedreiro ocioso
> nivela; uma régua de lote, um leitor de porta e uma lista de livres; a escrita própria do MAE saiu do código. Não jogada, e o efeito em save
> antigo nunca foi visto. A prova de geração saiu a pedido do autor.
> Sentinela `fast` verde: 1032 GameTests e 1754 unitários. PIT local: 85,8% (3231 de 3764).

> **Candidata 0.4.39:** toda entrada aparece no grafo na hora, só o grafo traça caminho em vila nova, o caminho traçado não é esquecido
> (Regra 214) e o pedreiro ocioso ajuda a calçar (Regra 215). Jogo da 0.4.38 (Spark uoFhAtB15p): primeira obra MAE concluída, 151 de 151 peças
> pelo construtor, e casa do mineiro na boca em uso. Não jogada: a queda de 132 para 114 nós do grafo não teve a causa provada.

> **Candidata 0.4.38:** a obra MAE entra na fila do construtor (Regra 213, ADR-047): o aldeão a levanta, o mundo vivo não a para, e ela
> só acaba pronta ou cancelada pelo jogador. A parada da 0.4.35 foi reproduzida em GameTest: grama coberta vira terra e muda a impressão da
> seção. Sem obra pedida em jogo ainda.

> **Candidata 0.4.37:** jogo da 0.4.36 (Spark CnNQltY9kb, sem nota do autor no log). Toda estrutura e o portão da mina têm entrada de
> porta no grafo, sem porta no mundo (Regra 210). Casa do mineiro na boca em uso (Regra 211). As "velas" do chão eram a tocha da colônia do
> carpinteiro: 13 no chão e 11 em parede no save, e nenhuma vela de verdade na superfície. Ninguém as põe mais, e vela e tocha não recusam
> lote (Regra 212). Não jogada. Abertos: nenhuma obra nova, mineiro em `NO_PATH` (`TODO.md`).
> O grafo também parou de mudar sem a rua mudar: a ordem dos nós vinha da identidade do objeto. Sentinela `fast`: 1161 GameTests e 1777 unitários verdes.
> PIT local: 85,6% contra o limite de 85, parte pelo registro de traçado `RoadGeometryPinTest`, que fixa o resultado sem dizer que está certo.

> **Candidata 0.4.36:** jogo da 0.4.35: os 2 azuis centrais eram quinas fixas de anel. Plantação agora não tem anel e fica com uma
> entrada; o anel antigo sai do grafo se a rede continua inteira (Regra 208). Porta de quintal fechado só conta se for a única da casa
> (Regra 209). Conta sobre o save: restariam 4 azuis, um por quadrante. Não jogada. Abertos: lago, MAE, falta de obra (`TODO.md`).

> **Candidata 0.4.35:** grafo com no máximo 4 pontas, uma por quadrante (Regra 206), e entrada em toda porta térrea, pelo lado de
> fora (Regra 207). Rua sem saída sai do grafo. Não jogada.

> **Candidata 0.4.34:** jogo da 0.4.32: a obra MAE limpou o lote e parou em `occupied_structure_target`, e a região falha congelou
> o grafo. Agora a peça escreve por cima de planta baixa (Regra 204) e a obra falha devolve a região (Regra 205). Não jogada.

> **Candidata 0.4.33:** o tanque da obra MAE nasce fechado (Regra 203): água e vizinhas numa escrita só. Provado no teste com a obra
> atrasada de propósito, que antes falhava igual ao jogo. Não jogada.

> **Candidata 0.4.32:** testes aplicados a pedido do autor: 1123 GameTests verdes em 2 rodadas seguidas e 1745 unitários. Os testes
> novos das Regras 196 a 202 falharam com as regras desligadas (10 de 10). Sem teste ainda: Regras 181 a 190, 192 a 194, e o andar da
> reunião (Regra 195, só o relógio). Teste verde não é jogo: o MAE continua sem nenhuma obra em jogo.

> **Candidata 0.4.31:** jogo da 0.4.30: 6 de 9 obras MAE recusadas por `changed_generation_evidence`. Medido no save: seção com
> água corrente diverge do recibo em 100% e com folha em 92%, sem ninguém mexer. A prova agora começa na reserva (Regra 202).
> Curral confirmado em jogo: 12 entregas (Regra 196). O MAE continua sem nenhuma obra em jogo. Sem testes; compilado.

> **Candidata 0.4.30:** a obra MAE limpa árvore, folha e mato da região antes de reservar (Regra 200) e aceita chão natural e
> minério no lote (Regra 201). Emendas na ADR-044. O MAE continua sem nenhuma obra em jogo. Sem testes; compilado.

> **Candidata 0.4.29:** jogo da 0.4.28 (70 min): 0 animais levados ao curral. Portão com rua volta a contar e o poste fica só para
> vila sem curral ou lotada (Regra 196); aldeão corta folha que trava o caminho (Regra 197); lenhador no interior, sem muda a
> até 3 blocos de estrutura (Regras 198 e 199). Abertos: fazendeiro preso numa casa e recusa `no_lot:protected_volume` do MAE.
> Sem testes; compilado.

> **Candidata 0.4.28:** Regras 191 a 195: espera para pedido sem resultado (`VC_LOOP`), só carpinteiro converte tora, mineiro alarga
> o nível, coleta só no expediente e reunião de 10000 a 11000 (`VC_GATHER`). O expediente ficou 9% menor. Sem testes; compilado.

> **Candidata 0.4.27:** varredura dos ofícios no jogo da 0.4.24: pedido de peça só do que falta (Regra 188), árvore mais perto
> do lenhador e machado para quem está sem ofício (Regra 189), fundidor larga o pedido com o cru na reserva (Regra 190). Abertos
> no `TODO.md`. Sem testes; compilado.

> **Candidata 0.4.26:** adiantamento das Fases 4 a 6 do MAE: obra pronta entrega a região (Regra 187, emenda da ADR-045),
> porta pelo grafo novo em vila nova, retomada depois de reiniciar, `applied` no `VC_MAE_SITE` e `/vc mae soltar`. O MAE
> ainda não construiu em jogo; falta a obra, a medida de carga e o gate da água. Sem testes; compilado.

> **Candidata 0.4.25:** Regras 181 a 186: teto no pulo do caminho (causa dos 36 teletransportes da casa principal no jogo
> da 0.4.24), ramal em vez de boca nova, anel de caminho, quina como entrada, ponta que cresce e rua de um bloco. `VC_PENS`
> mede o cercado grande antes de corrigir o pastor. Fase 4 do MAE segue aberta, nada novo nela. Sem testes; compilado.

> **Candidata 0.4.24:** caminho coberto vira terra (Regra 180) e `/vc mae obra <modelo> fora`, primeira forma de o MAE
> construir em jogo: no jogo da 0.4.23 as 5 tentativas deram `original_structure`, porque o lote caía na caixa da vila.
> Medido nesse jogo: ciclo médio 37 ms, `mspt` 9 a 11, grafo em uma peça, calçada de 313 para 103. Fase 4 segue aberta:
> falta o MAE construir em jogo, medir a carga e decidir o gate da água. Sem testes; compilado.

> **Candidata 0.4.23:** tocha na casa principal, saída em caminho, portões no grafo, rota que corta (`canCut`), zona
> limpa (`clearZone`) e `BuildingGaps` do pedreiro. Modo temporário: sem testes; compilado e nada visto em jogo.

> **Candidata 0.4.22:** grafo de caminhos da vila nova como mapa de trilhos (`RoadLines`), releitura a cada 5 min
> (`RoadGraphRefresh`, `/vc mae reler`) e calçada do que falta. Rodado fora do jogo sobre o caminho salvo da vila taiga
> (1.243 blocos): 6 releituras até uma peça só, degrau máximo 1, dois pares de nós a até 2 blocos. Portas lidas ali por script,
> não pelo `PathLinks`. Modo temporário: sem testes; candidata até o próximo playtest.

> **Candidata 0.4.21:** as capacidades novas da 0.4.19/0.4.20 (terra sob caminho, ponte, mina na rede, limpeza ampliada
> de árvores) só valem em vila nova (`NewVillage`). O plano mestre do MAE não está concluído: Fases 1 a 3 entregues,
> 4 e 5 parciais, 6 sem fila automática e nenhum critério de playtest cumprido; "fim do plano MAE" nas notas de 0.4.17
> vale só para a Fase 3.

> **Candidata 0.4.20 (branch `fix/encalhado-falso-solto`):** sinalizador `/vc mae mostrar ligar|desligar`, casa do
> mineiro ao lado da boca da mina com lote sem exigir rua, boca da mina na rede de caminhos, velas na casa principal
> e curva mais cara no traçado. Modo temporário: sem testes; candidata até o próximo playtest, junto com a 0.4.19.

> **Candidata 0.4.19, notas do jogo da 0.4.18 (branch `fix/encalhado-falso-solto`):** pulo padrão com o maior
> só no obstáculo, água 5× com mais de 5 blocos à frente, limpeza da zona em todas as alturas (`TreeClearing`),
> terra sob caminho voando (`PathSupport`), pastor com prioridade para trazer animais, casa principal em lote
> ligado a seco (`DryLand`) e ponte (`BridgeDeck`). Modo temporário: sem testes; candidata até o próximo
> playtest. A 0.4.20 traz o sinalizador dos caminhos MAE, a casa do mineiro na boca da mina, as velas da casa
> principal e o registro de blocos surgidos. Spark `btHEJgIYWb`: `MaeGenerationEvidence.onChunkGenerate` é 6,4%
> do tempo do servidor, ainda sem ação.

> **Candidata 0.4.18, encalhado falso solto (branch `fix/encalhado-falso-solto`):**
> playtest da 0.4.17 (68 min, 20 TPS) achou o construtor preso 50 min no mesmo bloco,
> solto pela fuga "after 0 steps" e marcado de novo a cada 2 min. Quem é marcado de novo
> onde foi solto sem se mover fica na fuga até sair dali (`StrandedWorkers.mayLeaveAt`), e
> `toSafety` troca a cama colada ao encalhe pelo centro da vila. Também: colheita do
> fazendeiro conta como progresso; chão do curral bloqueado a não-pastor de fora e saída por
> portão sem largar a tarefa (Regra 122); linhas por bloco do mineiro em DEBUG. Lenhador 0/15
> era o mesmo falso solto; lote 41 abaixo veio de save antigo. Modo temporário: sem testes
> (PenFloorsGameTest ajustado, não executado); candidata até o próximo playtest.

> **MAE — candidata 0.4.17, comando supervisionado (branch `claude/mae-obra-comando`):**
> `/vc mae obra <modelo>` (operador, nível 2) dispara uma obra supervisionada:
> pegada centrada no jogador e rotação pela direção do olhar; `<rotação>` 0–3
> fixa a rotação; `busca` procura lote nivelado (desnível até 2) num raio de 24.
> `/vc mae estado` mostra as obras em andamento e `/vc mae parar` pausa os jobs
> aprovados, sem desfazer nada. Quem pediu recebe o resultado no chat; o log
> registra `VC_MAE_SITE`.
> A obra liga a entrada à rua MAE (`road_v2`): semente perto do centro ou ligação
> ao grafo; a obra seguinte reaproveita a região. Passaram 1104 GameTests da
> bateria completa e a suíte unitária. Ainda não exercitado em jogo.
> Fim do plano MAE na mesma branch: vaso com planta, curral grande (água, solo
> arado e trigo moldados), placa/baú/barril/fornalha vazios, depósito grande
> recriado com porta térrea, pastor atravessando o portão andando e mobília
> funcional provada. Passaram 1112 GameTests da bateria completa e a suíte
> unitária. PR #53; JAR 0.4.17 com SHA-256 idêntico em build, downloads e TLauncher:
> `DA497943FBE45EDD03E64E0F19B952C2BA0EEDEC9C9506B6472A3D3848A13DC5` (refeito após a correção
> do CI: depósito gerado pelo script e pastor no rumo); 0.4.16 preservada
> como `.disabled`. Falta playtest, carga real e a fila automática.

> **MAE Fase 3 — candidata 0.4.16:** branch `codex/mae-structure-access`,
> baseada na `main` `9dc8b4f2`. Contrato `structure_v3` na ADR-046, P1–P4
> aprovadas pelo autor: envelope `maeWork` v3, material exato dos baús, cursor
> estrito, retirada antes da escrita. Retomada com escrita pendente bloqueia com
> `materials_uncertain` ou `partial_parts`, nunca repete. Obra conclui só com a
> prova de acesso nos blocos reais e entra uma vez no registro de edifícios.
> `MaeStructureAccess.beginFor` deriva circulação e destino; `MaeStructureSite`
> liga terreno → acesso → obra; terreno plano entra por prova sem mudança
> (escolha do autor). Casa, oficina, curral e depósito reais concluem nas quatro
> rotações. Passaram 105 GameTests `-PgametestOnly=Mae` e 23 unitários
> (arquitetura e `MaeWorkStoreTest`); bateria completa não executada.
> Sem comando em jogo nem fila automática; sem playtest. Ativação automática
> segue bloqueada por carga real e playtest da Fase 2. Travessia física dos
> animais pelos portões continua pendente.
> JAR 0.4.16 com SHA-256 idêntico em build, downloads e TLauncher:
> `9C983881043F6180D05A53F77A21D1958C243D11645D998454B22B3BD785EA26`;
> 0.4.15 preservada como `.disabled`. Push de `codex/mae-structure-access`; sem PR.

> **Playtest — candidata 0.4.15:** conservação da rota da mina, opção A aprovada.
> O playtest 0.4.14 confirmou origem na rua e até 30.289 células, com 23/23
> destinos alcançados. Mineiros ainda somaram 21% de bloqueio/encalhe; o log
> não prova destruição da escadaria. Duas regressões reproduziram falhas de
> proteção do piso natural e reparo de passagem aberta antes da correção.
> Pisos da geometria ativa e apoios do rastro salvo ficam protegidos na seleção,
> quebra e desvios. O reparo reutiliza MineFloor: um apoio por chamada, próximo
> do trabalhador, somente vazio, passagem livre e cursor já avançado.
> Não substitui água ou blocos do jogador; não redesenha minas nem certifica
> níveis antigos sem geometria ativa. Nenhum novo formato de save.
> Passaram 21 unitários de geometria/retorno e 16 GameTests de acesso, retorno,
> entrada, piso e proteção aquática. A classe já integra o Sentinela Local.
> `remapJar`, revisão do diff e README `--check` passaram. Manifesto confirma
> 0.4.15, Minecraft 1.21.1 e Java 21. SHA-256 das cópias locais:
> `518524FF551536EC10701F540599751E9279BBBA26F67E2FF49B6EE9C4CFEC3F`.
> Entrega local nesta linha, sem push ou merge e sem bateria completa.
> Conservação da escada e retorno real continuam pendentes do próximo playtest.

> **Playtest — candidata 0.4.14:** correção da origem do mapa de alcance.
> Opções A+B+C aprovadas. O playtest da 0.4.13 repetiu quatro vezes origem
> `(-1554,115,3553)` sobre escada de acácia: 38 posições, nenhum destino alcançado.
> Inspeção somente leitura encontrou rua em `(-1554,109,3552)`, com pés em Y=110,
> abaixo do telhado e perto do centro Y=111. A busca agora prefere rua indexada
> fisicamente válida; depois procura chão até quatro blocos do centro e ±2 em Y.
> O heightmap permanece como último recurso. Não escreve terreno, índice ou save.
> Quatro regressões cobrem rua, chão sob telhado, índice inválido e fallback.
> Testes focados liberados: seis ReachMap e quatro BedChoice passaram (10/10).
> Antes da correção, três regressões falharam pela seleção do telhado.
> `remapJar`, revisão do diff e README `--check` passaram. Manifesto confirma
> 0.4.14, Minecraft 1.21.1 e Java 21. SHA-256 idêntico em build, downloads e TLauncher:
> `9BC402F9D3F7AB8ADAB21E775D0833003C86CCE611A2D452D4D6E28A6682BC6C`.
> Só 0.4.14 permanece ativa em mods; 0.4.13 foi preservada como `.disabled`.
> Commit local nesta linha; sem push ou merge. Bateria completa não executada.
> Acesso real à mina, comportamento das profissões e efeito em jogo continuam pendentes.

> **Playtest — candidata 0.4.13:** instrumentação da busca periódica de alcance.
> `VC_REACH_DETAIL` registra centro, origem, piso, contagens de verificações fora
> da área, sem passagem e sem posição válida, além de uma amostra dos primeiros
> 64 bloqueios. `VC_REACH_START` informa ausência de origem no máximo uma vez por
> minuto. A hipótese de origem sobre telhado continua sem confirmação. A análise
> depende do próximo playtest; não altera a escolha de origem nem a navegação.
> `remapJar` compilou; SHA-256 do JAR:
> `42F7E6F55F7BF138C8BE5B7FA3364D23BC0BBE3B463453628BFB5B657C16CCD3`.
> Testes locais suspensos; a instrumentação aguarda validação em jogo.

> **Playtest — candidata 0.4.12:** branch `codex/mine-home-search`, baseada na
> `main` `adb7630c` (PR #50). Opção A aprovada: busca local da casa do mineiro
> com cursor independente, sem recentrar o índice nem apagar o progresso geral.
> Uma volta completa sem lote permite tentar outras obras; a casa continua pendente.
> Busca parcial retoma respeitando teto de 1.024 colunas e prazo do planejador.
> O log de 03-11 mostrou 103 falsas derivas: centro da vila comparado à boca da mina.
> O marcador `VC_MINE_HOME` informa volta local sem lote e liberação da fila.
> `remapJar` passou; manifesto confirma 0.4.12, Minecraft 1.21.1 e Java 21.
> SHA-256 idêntico em `build/libs/`, `downloads/` e TLauncher:
> `4EBBB6C35AB112280570A9936E522C57B237327F9E64FAC363271C98AA4C0DFD`.
> O JAR anterior foi preservado como `.disabled`. Testes locais suspensos nesta
> entrega; o efeito na fila ainda depende do próximo playtest. Sem push ou merge.
> Não inclui acesso/smoothing da mina, navegação, profissões nem integração automática MAE.

> **MAE Fase 3 em andamento, base 0.4.11 integrada pelo PR #50:**
> auditoria e limites em `docs/technical/MAE-Fase-3.md`.
> O contrato experimental deriva o ponto de entrada da planta da obra; ainda
> não existe integração automática com construção ou caminhos. Carga real e
> playtest da Fase 2 continuam necessários antes da orquestração automática.

> **MAE Fase 2 — implementação local com validação focada concluída:** branch `codex/mae-terrain`,
> criada sobre `main` `b57063162165b7d78c4657cb571ced734b862fea`, após integração
> da Fase 1 pelo PR #48. Modelo e testes focados liberados pelo autor.
> Planejamento puro limitado, levantamento incremental, vegetação baixa explícita,
> corte/aterro até dois, duas camadas de apoio e transição até seis. Operações
> tipadas de terreno/rua usam o writer e diário existentes; só publicam rua confirmada.
> Os sete pontos legados recusam efeitos em seções supervisionadas. Redescoberta
> legada de metadados é adiada na dimensão com reserva MAE; contratos desconhecidos
> conservam o bloqueio de seções após recarga. Não há certificação retroativa.
> Passaram 37 testes unitários/estáticos e 48 GameTests focados (45 MAE e três
> dos helpers físicos), sem falhas ou ignorados. Arquitetura passou sem ampliar exceções.
> A bateria completa passou com 1.023 GameTests após isolar as fixtures MAE de
> colônias criadas por outros testes; o isolamento mantém a proteção entre colônias.
> A fixture mediu 520 leituras incrementais e dez escritas; máximos por fatia:
> 256 leituras, uma escrita, 2,557 ms no levantamento e 4,967 ms no writer.
> Hashes/inspeções síncronas não entram nas 520 leituras; não é medição de carga ou SLA.
> A revisão reproduziu e corrigiu adoção entre colônias, liberação por contrato
> desconhecido, envelope excessivo de corte e contagem de plantas sobrepostas ao aterro.
> ADR-045, relatório `docs/technical/MAE-Fase-2.md` e tutorial Sentinela registram o contrato.
> Não ativa o MAE nas obras do jogo e não comprova correção dos sintomas históricos.
> Nenhum save do jogador foi aberto. Versão candidata 0.4.10; integração e
> distribuição dependem do resultado do PR e do CI.
> Os registros antigos de suspensão de testes abaixo descrevem suas respectivas entregas.

> **MAE Fase 1 integrada:** PR #48, base `b5706316`. Contrato conservador preservado;
> detalhes em `docs/technical/MAE-Fase-1.md` e ADR-044. A entrega 0.4.9 anterior
> permanece distinta deste desenvolvimento ainda não distribuído.

> **PR #47 integrado:** merge `8a8ae09844761566f9b7e2e9fc06c67bac1db29d`;
> CI 553 (`41513439513`) passou, incluindo 977 GameTests e PIT. `main` local
> atualizada antes da abertura desta branch. As fixtures de cultivo e métricas
> foram corrigidas; causa histórica do episódio intermitente de coleta continua
> sem confirmação. O CI verde não demonstra sua causa.

> **0.4.8 — primeiro lote do plano aprovado:** o despacho de `VillageHelpers`
> deixa de enviar pedreiro para terraplanagem e fundidor para transporte/luz.
> Construtor mantém reparos de terreno, pastor mantém alimentação e adultos
> sem profissão mantêm transporte. Testes seguem suspensos. `remapJar` passou
> em 3min25s; manifesto interno 0.4.8 e SHA-256 idêntico no build, `downloads/`
> e TLauncher: `ABDD780E8B026F9C9BFE1CC6C0018E4ABE60ED465F2861E1C1BF81A861D20C8B`.
> O JAR anterior foi preservado como `.disabled`. Efeito depende de playtest.
>
> **Planejamento aprovado, em desenvolvimento:** seis opções A para
> orçamento por tick, acesso/casa da mina, reservas por bloco, frentes exclusivas,
> terra/salto normal e tarefas restritas ao ofício. Nenhuma profissão deve
> executar atividade de outra durante a regra temporária. Detalhes e evidências:
> `docs/technical/Plano-Playtest-2026-11-02.md`. Demais correções ainda pendentes.
> Modelos atualizados em `docs/technical/Modelos-Codex.md`; testes suspensos.

> **0.4.7 — análise local do Sentinela pendente:** `scripts/sentinel_analysis.py`
> lê apenas execuções preservadas, agrupa causas, indica evidência incompleta e
> cria JSON, Markdown e HTML em `reports/sentinel-analysis/`. A janela oferece
> a ação manual correspondente. Cada suite futura registra CPU, RAM, memória
> Java e GPU opcional em `metrics.json`; GPU ausente é metadado, não falha.
> Baterias Java seguem suspensas. Foram executados testes Python e compilação
> Python, sem Gradle. O JAR 0.4.7 ainda não foi gerado nem copiado.

> **0.4.6 — Observatório local do Sentinela pendente:** o mesmo
> `Sentinela/Abrir-Sentinela.cmd` agora escolhe suites, repetição e orçamento
> seguro em minutos. `scripts/test_lab.py` preserva `dashboard.html` local com
> casos XML, falhas agrupadas, lentidão, logs e até 50 execuções; `INTERRUPTED`
> nunca mata a suite corrente e pode ser retomado. Não há dependência nova,
> telemetria, execução em login ou mudança de CI. Baterias Java continuam
> suspensas por ordem do autor; foram validadas sintaxe/importação Python e
> simulações secas. JAR local 0.4.6 em `build/libs`, `downloads/` e TLauncher:
> SHA-256 `FD5BDA43D752E9E70AB0CC7701E7A9CB8D4913791C16097B985F996E47C9D37B`.

> **0.4.5 — aplicativo local do Sentinela pendente:** `Sentinela/Abrir-Sentinela.cmd`
> abre uma janela Windows que chama o orquestrador oficial sem IA. A tela expõe
> perfil, repetição, simulação e retomada, transmite a saída e abre os relatórios
> existentes. Não agenda tarefas, não muda CI e não duplica a lógica de testes.
> A validação fica restrita a importação/sintaxe e `--dry-run`, pois o modo
> temporário de correção por playtest ainda suspende as baterias Java. JAR local
> 0.4.5 em `build/libs`, `downloads/` e TLauncher: SHA-256
> `6D180AE9701125CE26B39CAA10B37A6BA1588664AE58C8D055A785A6DA23FA53`.

> **0.4.4 — fechamento rastreável pendente:** `FECHAR-RAPIDO` confere os
> documentos vivos antes do PR; `REGISTRO-RELEASE` transforma o registro humano
> versionado em evidência de commit e SHA-256 para a publicação GitHub. Push de
> branches deixa de repetir a bateria completa, que continua obrigatória no PR
> e na `main`. Esta alteração não muda o mod no jogo e permanece sem bateria
> Java pelo modo temporário de correção por playtest. JAR local 0.4.4 em
> `build/libs`, `downloads/` e TLauncher: SHA-256
> `5DFAB711B431FB9A693F991ABA4A3434235528E60DBC1C96E34EB205A1D64891`.

> **0.4.3 — fechamento local em 02-11:** watchdog de progresso útil, reposição física
> na segunda falha diária, metas ociosas de mina e fundição, casa do mineiro,
> curral grande único e ativação compatível do grafo de ruas foram implementados.
> A correção da fixture de lingotes dividiu a pilha inválida de 96 itens em duas
> pilhas aceitas pelo baú Vanilla; não mudou produção. `runGametest` passou
> 977/977 depois da correção. O **Sentinela Local full** passou em
> `reports/test-runs/20261102T125155Z`: unitários 36,8 s, propriedades 2,6 s,
> GameTests 145,9 s, auditoria 169,0 s e mutação 2,6 s. Vilas existentes
> preservam rua e currais legados; o playtest em mundo novo ainda é necessário.

> **Sentinela Local — 02-11:** nome oficial da varredura manual. O comando
> conversacional `rodar Sentinela Local` solicita o perfil `full`. O
> `scripts/test_lab.py` cria uma
> varredura manual com perfis `quick`, `fast` e `full`, repetição, retomada e
> relatórios isolados em `reports/test-runs/`. A cópia compara arquivos internos
> dos relatórios, não somente o horário da pasta. Não altera CI nem inicia ao
> abrir o projeto.
> O modo temporário de correção por playtest permanece ativo: esta entrega foi
> verificada apenas com `--dry-run`; nenhum JUnit, GameTest ou PIT foi iniciado.
> Dois testes Python específicos para cópia e retomada passaram em 02-11 sem
> iniciar Gradle. A liberação não abrange baterias Java, GameTests ou PIT.
> Camada visual/GPU do cliente continua pendente de prova Fabric no perfil
> `village-max`.
> O empacotamento 0.4.3 executou somente `remapJar -x test -x runGametest`; não
> houve nova bateria de testes por decisão do autor. JAR 0.4.3 em `build/libs`,
> `downloads/` e TLauncher: SHA-256
> `5A95A448D74152A17646CB0F0B7E1299DDDCC6A1FFC672E6595DD31057256BAF`.

> **Fechamento local 0.4.1 — 01-11:** testes foram liberados para esta rodada.
> Lotes novos limpam vegetação e árvores naturais na margem externa de um bloco;
> mineiro só avança após `removeBlock` bem-sucedido; folhas e troncos não viram
> piso; ajuda fora do ofício espera 1.200 ticks; e fundidor fora de área construída
> recebe retorno ao baú pessoal antes de fundir. O GameTest agora registra a oficina
> exigida pelo cenário de fundição e cobre explicitamente o retorno.

> `clean build remapJar` passou em 2m20. A bateria completa local rodou 979 GameTests:
> 978 passaram e `GroundPickupGameTest.aPassingWorkerPicksUpAllGroundItems` falhou;
> a repetição isolada passou. O CI de push também falhou, em
> `BuilderGameTest.groundBlocksAreShapedWithoutTheChest`; a intermitência fica aberta, sem alteração de produção,
> conforme a instrução do autor de não bloquear esta entrega por testes. Ainda falta
> playtest em mundo novo e no save; não declarar os sintomas corrigidos antes dele.
> JAR 0.4.1 em `build/libs`, `downloads/` e TLauncher: SHA-256
> `FD72245C3BDF5E4B1DA816CC37E69C9B4CBE725E0A62254D0F184320D731606E`.

> **Rede e currais, desenho aprovado em 01-11 — entrega parcial autorizada:**
> grafo puro e codec paralelo versionado implementados; sete testes de grafo,
> treze de persistência nova e testes legados/planejador passam. Duas regressões do codec
> foram reproduzidas e corrigidas: lista NBT com tipo errado e circuito de 6.000 colunas.
> Rotas sobre os trechos contraídos passaram em quatro testes; componentes desconectados ficam fora das distâncias ao centro.
> Revalidação física e origem passam em quinze GameTests, incluindo o hook real de geração e descoberta tardia.
> Ainda faltam migração operacional, planejador integrado
> e mudanças de currais. Última rodada completa: 1.691 testes JUnit, zero falhas;
> 977/977 GameTests, incluindo migração, diário e controles de terreno externo.
> Fuga: 4/4 isolados e combinados; bateria 15/16, falha no desvio de água ainda investigada.
> Entrada elevada, piso sem apoio e contagem do diário tiveram falhas reproduzidas; PathNetwork passou 7/7 depois.
> Migração passou 8/8 após reproduzir defeitos de teto, pisos ambíguos, janela insuficiente e carga indireta de chunk.
> Delegação dos mixins passou após mover VillageOrigin para integration, sem ampliar a whitelist arquitetural.
> Quatro casos novos do codec falharam antes da correção; a rodada focada passou 18/18 depois.
> Outros dois casos reproduziram nó interno sem divisão da aresta; os treze testes do codec passaram após a correção.
> A rodada combinada passou, mas ainda não explica a intermitência do desvio de água.
> Autor confirmou manter o bloqueio da integração até concluir esse diagnóstico, mesmo com a bateria completa verde.
> Controle A/B/A manteve a mesma rota seca nas três consultas; esse trecho de terreno externo não explicou o defeito nessa montagem.
> Correções locais de entrada, apoio e diário estão testadas; o novo planejador e os novos currais permanecem desativados.
> Após a bateria completa, três controles focados de água passaram, sem reproduzir a falha histórica.
> Autor encerrou os testes e autorizou commit, push, merge e atualização do JAR em 01-11.
> JAR local de desenvolvimento 0.4.0 atualizado; build, downloads e instalação têm SHA-256
> `2F37C545B8A205EABBC8F80FFE230F8B3067D65382761D46CC3BAEC418CF1032`.
> `remapJar` aprovado sem reabrir testes; efeito no mundo do autor ainda exige playtest.
> CI do push aprovado; CI do PR #43 falhou em 1/978 GameTests: translação atravessou água
> na origem 8977781, -60, 13733688. Autor pediu encerrar os testes à força e seguir a integração.
> Essa falha permanece aberta; não foi corrigida nem tratada como teste aprovado.
> [Desenho aprovado](docs/superpowers/specs/2026-11-01-hierarchical-roads-and-pens-design.md)
> e [ADR-043](docs/decisions/ADR-043-hierarchical-road-graph.md).

> **Integração e diagnóstico de 01-11:** [19 pontos priorizados](docs/audit/2026-11-01-falhas-priorizadas.md),
> backlog histórico e [plano aprovado](docs/technical/Plano-playtest-2026-11-01.md).
> `clean build` e `test runGametest` aprovados: 1.667 unitários, uma propriedade jqwik,
> 955/955 GameTests; Python 155/155 e `rules_to_readme.py --check` aprovados.
> JAR local de desenvolvimento 0.4.0: SHA-256
> `F787497B76CAB6AFD09EC80B817CE64D888702E3F90C390F6F42ACD66B3C92B7`.
> Binário igual ao anterior: esta etapa muda testes/documentação, sem nova lógica de produção.
> Permanecem abertas as causas do mineiro girando, aldeões presos e terreno incompleto;
> a integração não as declara corrigidas. CI anterior registrou intermitência de condução da vaca pelo pastor.

> **01-11 — reforço dos testes de mineiro e lenhador:** corrigida consulta absoluta/relativa
> no teste de descida; agora exige degrau inicialmente sólido, escavação real e descida física.
> Colheita exige todas as toras no baú; cenário adicional cobre árvore de oito toras sem
> abandonar a base durante o corte. Os 135 GameTests focados passaram. Falhas artificiais
> de degrau fechado e tora abandonada derrubaram os três testes esperados; foram retiradas.
> Isso valida a sensibilidade dos testes, mas ainda não reproduz o giro e as idas e voltas
> observados no save. Esta etapa reforça testes; o comportamento de produção vem da linha anterior.
> **Diagnóstico aberto, preservado em patch:** após 147/147 aprovados, o cenário forense passou a exigir
> chegada física. Falhou tanto com navegação direta quanto com destino de trabalho e
> horário correto. Existe caminho calculado, mas o aldeão não alcança a posição exigida
> em 140 ticks. Causa ainda em investigação; não equivale à reprodução do save atual.
> O teste obrigatório original cobre planejamento; o reprodutor de chegada está no
> [relatório priorizado](docs/audit/2026-11-01-falhas-priorizadas.md), com instruções de aplicação e retirada.

> **Jogo de 31-10, `villagecolony 0.4.0`, mundo `Novo mundo`:** log sem exceção fatal e
> encerramento normal. Spark [NuQRCK1bmh](https://spark.lucko.me/NuQRCK1bmh): TPS 20,
> MSPT médio de 21,62 ms no último minuto (p95 29,02 ms), pico de 97,44 ms; 828 entidades,
> 136 itens soltos e 18 blocos em queda. O diário aponta construtor 46% trabalhando,
> carpinteiro 11%, pastor 3%, pedreiro 15% e fundidor 57%; não há prova de travamento contínuo.
> Nesta linha de desenvolvimento, a obra ganha transição em degraus de 1 bloco, preservando solo
> e apoio; o pastor precisa estar a até 5 blocos para alimentar, cruzar e abater, com reserva
> de 6 adultos por espécie; currais usam um modelo de 15×15 para todos os biomas, intercalados
> até haver um por espécie; folhas naturais no trajeto são removidas aos poucos, todo item
> solto a até 5 blocos de trabalhador é recolhido, e árvores naturais no lote saem inteiras.
> A produção auxiliar repõe até 16 por tipo conforme o consumo. Tudo exige novo playtest;
> GameTests e build validam o código, não o comportamento no save do autor.
> Validação desta correção: `clean build` aprovado, 954/954 GameTests aprovados,
> `rules_to_readme.py --check` aprovado. JAR de desenvolvimento SHA-256
> `F787497B76CAB6AFD09EC80B817CE64D888702E3F90C390F6F42ACD66B3C92B7`.

> **Linha `codex/terrain-stock-repair` em validação, sem versão publicada:**
> proteção prévia de cortes/aterros do alisamento e da geração, apoio de vazios secos de até 3 blocos fora de minas e construções,
> construtor ajudando o terreno da vila sem obra, catálogo de estoque com pedra e metal, fundidor repondo blocos
> de receitas de fornalha até 16 por tipo e trecho de rua sujeito a caminho alcançável.
> Baús nomeados pelo jogador seguem reservados. A 0.4.0 foi jogada em 31-10; testar as mudanças desta linha em mundo novo e no save de diagnóstico
> antes de concluir que as paredes, os acessos à mina e a ociosidade foram resolvidos.
> Verificação da linha: `clean build` aprovado, `build` final aprovado e 949/949 GameTests aprovados.
> JAR de desenvolvimento desta linha instalado no TLauncher: `village-colony-0.4.0.jar`,
> SHA-256 `F787497B76CAB6AFD09EC80B817CE64D888702E3F90C390F6F42ACD66B3C92B7`; ainda não publicado.

> **0.4.0** (31-10, promovida de `fix/estruturas-0110`): depois da análise do
> jogo da 0.3.110 (12,3 h, nenhuma roça, 58 camas para 19 adultos, armazém grande 63× sem lote). A vez da roça abria
> casa (`HousePlans.plansForNext` pedia a lista de casas) — corrigido; roças pela maior das contas, 1 a cada 15
> adultos ou 1 por fazendeiro (Regra 163); casa de reserva respeita o teto de camas (Regra 164); galpão
> `storage_small` 7×7 antes do `storage_majest` (Regra 165). Unitários 1667/1667, bateria 940/940, Python 155/155.
> JAR `village-colony-0.4.0.jar`, SHA-256 `9E011A3F256C22B1695D2A2AA44366CB4A38AE03DD4C356F138B958B86BF040D`,
> cópia da release preservada como `village-colony-0.4.0-release.jar.disabled` no TLauncher. A 0.4.0 foi jogada em 31-10.
> Para o jogo: **mundo novo**; ver se a roça sai com fazendeiro sem roça, se as casas param perto de adultos + 4, se o
> galpão fica de pé com porta, tocha e 8 baús. Mina, caminhos, curral duplo e flecheiro seguem abertos
> (análise na memória `playtest-0110-mina-caminhos-estruturas`).

> **31-10 — ambiente Codex registrado:** `minecraft-modding`, `jvmsrc` e
> `fabric` foram configurados, `token-saviour` foi instalado globalmente e o
> `skill-freeze` ficou em 11 hot/98 cold (90% de economia). Stonecutter não faz
> parte deste estado; detalhes reproduzíveis em `docs/technical/Codex-Tooling.md`.

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
