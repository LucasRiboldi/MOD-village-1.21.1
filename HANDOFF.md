# Handoff — Village Colony

Ponto de retomada para Codex ou Claude Code. Confira a branch, o estado do Git e os PRs abertos antes de trabalhar. O handoff da Fase 3 do MAE está no histórico do Git.

## Entrega — 0.4.135 (2026-11-24)

- **Branch e PR:** `fix/pulo-grande-todo-tique`, PR #111 (junto com a 0.4.134).
- **Escolha 2 do autor (diagnóstico no jogo):** `HomeChestMoves.move` devolve o motivo da desistência, e `repair` registra `VC_HOME_CHEST_STAY` quando o baú perdeu a parede e não mudou, uma vez por mudança de motivo de cada morador. `ChestPlacer.repairSlot` anota a recusa de cada lugar (`occupied`, `unsafe(above=...)`, `unsafe(chest_beside)` e outros). O comportamento do baú não muda.
- **Causa da instabilidade do `ChestPlacerGameTest.repairsAProvenChestAcrossAChunkBorder`:** a casa deslocada até 7 blocos para cruzar a borda caía às vezes sob a barreira da arena; o destino era recusado por `unsafe(above=minecraft:barrier)`. **Escolha 1 do autor:** o teste esvazia o volume da casa antes de montar. Antes, 1 falha em 5 isolado; depois, 15 de 15.
- **Verificação:** Sentinela `fast` `20261125T010329Z` 1.886 unitários, propriedades e 1.265/1.265 GameTests. Na bateria, `VC_HOME_CHEST_STAY` saiu 5 vezes, uma por morador, com `no_placement_receipt`.
- **Instalado:** `village-colony-0.4.135.jar` em `build/libs/`, `downloads/` e TLauncher, SHA-256 `51167A1024B8E645BA0CCCC5282204FF948611573BA4817F26FF9EFEBCF86FA4`; a 0.4.134 ficou `.jar.disabled`.
- **Fusão de colônias (escolha 🅲 do autor):** o rastro mostrou o caminho de 29 passos trocado por outro de 14 no tique 40, sem o teste pedir: o teste empurrava a navegação por fora do Brain. Agora usa `WorkTargets.set` no expediente, chão firme em volta da rua e rastro `VC_TEST_MERGE_WALK` (posição, `WALK_TARGET`, atividade, caminho). Original 2/5 falhas; chão firme 1/10; com `WorkTargets` 10/10. Mutação: sem `RoadNetwork.merge` cai em "a rota deve entrar na rede". Sentinela `20261125T022823Z`: 1.264/1.265, falha só N05.
- **Próximo:** N05, tosquia (ver TODO).

## Entrega para outro agente — 0.4.134 (2026-11-24)

- **Branch:** `fix/pulo-grande-todo-tique`, criada da `main` em `f8f2c459` (0.4.133 já integrada pelo PR #105).
- **Causa da falha fixa do `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`:** desde a 0.4.125 (`bd7abd57`, pulo padrão primeiro) o teste falhou em 15 de 15 execuções do CI de 24-11 e em 5 de 5 isolado. O `PEN_DIAG` mostrou dois pulos padrão perdidos na cerca (t=9 e t=22) e o maior só em t=35: `VillagerMoves.needsHighJump` era conferido a cada 2 tiques e devolvia falso sem caminho, e a navegação larga o caminho na queda.
- **Correção (escolha 1 do autor):** colisão conferida a todo tique e com ou sem caminho. O teste do pulo (`VillagerRulesGameTest.theHighJumpIsOnlyForObstacles`) ganhou o caso sem caminho; falhou antes e com a produção antiga (mutação), passa depois.
- **Teste do curral (escolha 1 do autor):** com o pulo padrão primeiro, a saída leva 41 tiques e não cabe na janela de 39 da inspeção. O teste cobra a saída sozinha em até 60 tiques, sem o `PenEscape` conduzir. Com a produção antiga ele também passa (54 tiques): sozinho, não prova a melhoria.
- **Verificação:** 27 GameTests focados (pulo, curral, guia, condução). Sentinela `fast` `20261124T234447Z`: 1.886 unitários, propriedades e 1.264 de 1.265 GameTests; a falha foi `ChestPlacerGameTest.repairsAProvenChestAcrossAChunkBorder`, com os dois chunks carregados e o baú sem migrar.
- **Instalado:** `village-colony-0.4.134.jar` em `build/libs/`, `downloads/` e no TLauncher, SHA-256 `F8ADE9D409AEC4D58512FAF787BC5844A3FECA732B20F4359E109B85F28453A4` nas três cópias; a 0.4.133 ficou `.jar.disabled`. Nada enviado ao GitHub.
- **Outros instáveis medidos no CI de 24-11 (15 execuções):** baú na borda 5×, travessia após fusão 4×, N26 4×, N05 3×, `MineStepDownGameTest` 1× (5 de 5 verdes isolado).

## Entrega para outro agente — 0.4.133 (2026-11-24)

- **Branch:** `claude/resgate-telhado-seguro`, criada de `codex/piso-curral-grande` e com o PR #106 (baú na borda do chunk) já mesclado nela. **PR #107** foi mesclado pelo autor em `codex/piso-curral-grande` (merge `c76aadc5`, 2026-11-24 22:09 UTC); este bloco de handoff entrou depois, pelo PR #109. O PR #105 (`codex/piso-curral-grande` → `main`) também segue aberto.
- **O que a 0.4.133 traz:** resgate de telhado sem queda perigosa (`RoofDescent`, commit `c8b32ace`) e trecho da rede de ruas que só para em chão de rua, caminho ou chão natural (`ColonyPathing.standingOn`), Regras 151 e 160. Inclui o baú residencial na borda do chunk (PR #106).
- **Instalado:** `village-colony-0.4.133.jar` no TLauncher e em `downloads/`, SHA-256 `FAC43A023956DF59C615BD3039DD9281113AAC6D31C2DBE14547682B1AAB3998` nas três cópias; as anteriores ficaram `.jar.disabled`.
- **Testes:** `StreetRouteGameTest.aStreetColumnUnderAChimneyIsNotALeg` falhou antes e passa depois. Sentinela `fast` `20261124T204300Z`: 1.886 unitários, propriedades e 1.264 de 1.265 GameTests (falha conhecida do `PenEscapeGameTest`). CI do PR #107, duas tentativas, com falhas que mudam de rodada: `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`, N05 (`ShepherdGameTest.theShepherdOnlyShearsAfterReachingTheSheep`), N26 (`PenTetherGameTest.theAnimalWalksFromASideLedgeThroughTheGate`) e `ChestPlacerGameTest.repairsAProvenChestAcrossAChunkBorder`. Este último segue instável mesmo depois do PR #108 (ver a seção do PR #108 abaixo).
- **Pré-release `v0.4.133-alpha`:** adiada por escolha do autor até os testes instáveis ficarem verdes. O workflow Release exige CI verde no mesmo commit.
- **A confirmar em jogo:** nenhum fazendeiro morre de queda nem é resgatado de novo perto de `338,86,1414`; `338,87,1412` não aparece como destino no diário.
- **Hipótese não provada:** a coluna `338,1412` pertence à rede de ruas; o GameTest só reproduz a geometria. **Risco:** coluna da rede sobre piso de construção (tábuas) deixa de servir de trecho, e o aldeão vai direto ao destino.
- **Próximos passos:** (1) estabilizar os GameTests instáveis acima; (2) CI verde e pré-release `v0.4.133-alpha`; (3) PR #105 para a `main`; (4) confirmar em jogo a correção do telhado.
- **Logs de playtest:** `%APPDATA%\.minecraft\logs` (`latest.log` e `.log.gz`) e `%APPDATA%\.minecraft\saves\Novo mundo\villagecolony\analytics` (diário de ações, relatórios `VC_*`).
- **Ao falar com o autor:** linguagem simples, sem jargão; ele prefere saber o que está pronto e o que falta.

## Teste do baú na borda do chunk — PR #108 (2026-11-24)

- O PR #106 (já em `codex/piso-curral-grande`) deixa o baú residencial migrar para o chunk vizinho, desde que carregado. Antes, `HomeChestMoves.repair` exigia o mesmo chunk, e `ChestPlacerGameTest.repairsAProvenChestWithoutDroppingContents` falhava quando o GameTest caía com a borda entre os dois lados da cama.
- O teste novo `repairsAProvenChestAcrossAChunkBorder` falhou depois em 1 de 6 execuções do CI (45489203898, 2ª rodada), com origem e destino já em chunks vizinhos. A causa **não está provada**. Hipótese: o deslocamento de até 8 blocos tira a casa da área que o GameTest mantém carregada, e `repair` exige a casa inteira carregada (`Homes.isLoaded`).
- O PR #108 (branch `claude/project-thread-02gx49`, só teste e documentação) força os chunks da casa, roda o reparo no tick seguinte e registra os que estavam descarregados. O CI 45495565158 passou neste teste, mas o 45498542870 **falhou de novo** sem nenhuma linha `VC_TEST_CHEST_BORDER_UNLOADED`: todos os chunks já estavam carregados. A hipótese dos chunks está, portanto, enfraquecida e o PR #108 **não corrige** a instabilidade; ele só acrescenta o registro.
- Como a falha agora ocorre dentro de `waitAndRun`, a mensagem da asserção não sai no log do job; ela fica em `build/gametest-report.xml`, no artefato `ci-reports-<run>` (o ambiente de nuvem não baixa artefatos; baixe pelo navegador ou no PC do autor).
- Próximo passo: ler a mensagem no `gametest-report.xml` da execução 45498542870 e investigar as outras condições de `HomeChestMoves.repair` e `ChestPlacer.repairSlot` (parede, porta, escada, `world.isSpaceEmpty`). O ambiente de nuvem não compila o mod; leia os logs com `get_job_logs` e `grep`.
- Em nenhuma execução que passou apareceu `VC_HOME_CHEST_MOVED` para este teste (o teste original registra a linha normalmente). Motivo desconhecido; conferir ao investigar.

## Entrega preparada para outro agente — 0.4.131 (2026-11-24)

- Branch de origem: `codex/piso-curral-grande`. Código, testes focados e documentação estão reunidos nesta linha; `.serena/` local não faz parte da entrega.
- Próximo fluxo: `git fetch origin`, conferir PR contra `main`, aguardar CI verde do mesmo commit, fazer merge e disparar `Release` em `main`. O workflow publica `v0.4.131-alpha` e recompila o JAR no commit mesclado.
- Não declarar item 9 como confirmado: o código tem prova automatizada, mas o jogo ainda precisa confirmar o curral grande no deserto e a retomada de obra após recarga.
- Pendências ativas e ordem de trabalho estão no topo de [`TODO.md`](TODO.md). A falha conhecida de `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan` não foi atribuída ao item 9.
- Para retomar: leia este bloco, depois [`STATE.md`](STATE.md), o topo de [`TODO.md`](TODO.md) e [`docs/releases/v0.4.131.md`](docs/releases/v0.4.131.md). Não use históricos antigos como estado atual.

## Candidata local 0.4.131 (2026-11-24)

- A branch `codex/piso-curral-grande` inclui a candidata 0.4.130. No item 9, a opção 1 molda grama sobre o terreno natural nas posições de circulação da planta. Água e canteiros seguem o modelo.
- O preparo funciona na construção normal, em peças adiadas por alcance e no fechamento da obra. Ao reabrir o mundo, o curral grande com obra iniciada continua apto à retomada.
- As regressões reproduziram o defeito. Os 22 GameTests focados passaram. No Sentinela `fast` (`20261124T185435Z`), passaram 1.886 testes unitários, a suíte de propriedades e 1.262 dos 1.263 GameTests. A única falha repetiu `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`.
- O build passou. O JAR para `downloads/` e TLauncher é `village-colony-0.4.131.jar`, com SHA-256 `87B20C3C46E222A788171427DFA7772AEFADB075FF3301C654443AC5077FCE5E`.
- Construa um curral grande no deserto. Confira grama na circulação, os 12 blocos de canteiro e a água da planta. Se quiser testar a retomada, saia depois de uma porteira ou cerca ser assentada e confira a conclusão ao reabrir. Currais concluídos ficam fora do escopo da mudança. O item segue no [TODO](TODO.md#pendências-do-playtest-do-deserto-04127).

## Candidata local 0.4.130 (2026-11-24)

- A branch `codex/moradia-placas-e-baus` parte da candidata 0.4.129. A placa da fachada lista os aldeões pelas camas atribuídas. O quadro interno da casa pequena prefere a face com passagem voltada ao centro.
- O recibo persistente marca baús colocados pelo mod a partir desta versão. A migração exige outro ponto livre junto à parede no mesmo cômodo; o destino pode estar no chunk vizinho, desde que carregado. O baú conserva os itens e o vínculo com o aldeão. Baús anteriores de origem indeterminada ficam no lugar.
- Build aprovado e 38 GameTests focados passaram. No Sentinela `fast` (`20261124T182652Z`), passaram 1.886 unitários, a suíte de propriedades e 1.260 de 1.261 GameTests. A única falha foi `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`, também presente na 0.4.129.
- O `village-colony-0.4.130.jar` foi copiado para `downloads/` e instalado no TLauncher. As três cópias tinham SHA-256 `1B6B59F895BA73D2EDD798FAFAC55FF9E139E18564209AD7F3147C50684BBEE0`. A 0.4.129 ficou desativada para retorno.
- No deserto, confira a placa da casa perto de `847,64,298` e o quadro do baú perto de `878,63,245`. Verifique uma troca de cama e reabra a partida. Para o teste de migração, use uma cópia do mundo, um baú criado nesta versão e um ponto seguro afastado de porta e escada.
- Procure `VC_HOME_SIGN` e `VC_HOME_CHEST_MOVED` no próximo log. Os itens do playtest continuam no [TODO](TODO.md#pendências-do-playtest-do-deserto-04127).

## Candidata local 0.4.129 (2026-11-24)

- A branch `codex/pastor-rota-expediente` inclui as candidatas 0.4.127 e 0.4.128. No item 6 do deserto, a opção 2 evita portas no trajeto com animal na guia e aplica a regra social aos dois participantes da conversa.
- Ao prender a guia, o pastor descarta a rota anterior. O filtro de portas abertas ou fechadas vale durante a condução, incluindo a aproximação da porteira. O acesso ao curral segue as regras de entrega vigentes.
- Adultos com profissão na colônia ficam fora das conversas de aproximação, encontro no sino e troca de itens durante o expediente. Conversas em andamento são reavaliadas no início do expediente. A [ADR-004](docs/decisions/ADR-004-Mixin-Policy.md) registra as injeções.
- Os 14 GameTests focados passaram. As regressões reproduziram o atalho pela casa e a aproximação social indevida antes da correção.
- Build aprovado. No Sentinela `fast` (`20261124T175259Z`), passaram 1.886 unitários, a suíte de propriedades e 1.258 de 1.259 GameTests. A única falha foi `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`, já registrada desde a 0.4.125.
- Candidata `village-colony-0.4.129.jar` com SHA-256 `A8BF59A2A628147BED3F52A6B7E048B07CDAE5874D7DCE38BF122FCBB52A04DA`. O JAR está em `downloads/` e ativo no TLauncher. As três cópias têm o mesmo hash. A 0.4.128 ficou desativada para retorno.
- No próximo jogo, volte à vila do deserto perto de `874,64,275`. Observe o pastor buscar uma vaca com uma casa entre ela e o curral, primeiro com a porta aberta e depois fechada. Confira o desvio pela rua, a travessia do animal pela porteira e o fechamento antes de soltar a guia. Compare as posições em `VC_FLOCK_EVENT`.
- Observe as conversas entre aldeões antes e depois do tique 10.000. Durante o expediente, um aldeão livre também deve ignorar um trabalhador como parceiro de conversa. O comportamento em mundo real segue no [TODO](TODO.md#pendências-do-playtest-do-deserto-04127).

## Candidata local 0.4.128 (2026-11-24)

- A branch `codex/currais-tematicos-04127` reúne os currais temáticos e a correção do grafo (`6638482f`, `604c8b5e`). O arenito de uma peça de rua Vanilla entra no grafo desde a primeira leitura. O registro da BigHouseMOD e da casa do mineiro pede releitura.
- A regressão reproduziu a rua de arenito ignorada com o índice vazio. Os 57 GameTests de grafo e fundação e os seis testes de arquitetura passaram na branch da correção. O cenário da BigHouseMOD verifica a saída pelo piso de areia da porta e a ligação à rede em volta de um desnível.
- Na revisão entregue, os unitários e propriedades passaram no Sentinela (`20261124T172635Z`) e passaram 57 GameTests focados. A última bateria completa (`20261124T172158Z`), anterior ao ajuste de conversão de coordenadas, passou em 1.255 de 1.257 GameTests. Repetiram as falhas da saída do curral entre varreduras e da tosquia N05.
- O JAR `village-colony-0.4.128.jar` está em `downloads/` e ativo no TLauncher. As três cópias têm SHA-256 `13FCA22D112B85AAAC04624B8CA0A55AB895A17C6179C7F061FAF55EB9DC1CAF`. A 0.4.127 ficou desativada para retorno.
- No TLauncher, use a vila do deserto da 0.4.127. Confira a linha ligada aos nós ao lado de `845,63,319` e `885,64,272`. Saia e reabra o mundo. Em uma vila nova no deserto, confira se a primeira leitura inclui as ruas de arenito e se as entradas das fundações ganham ramal. Compare `VC_ROAD_GRAPH`, em especial `edges=` e `loose=`.
- A mina, o trajeto do pastor com guia, as placas, os baús e o piso do curral grande estão nas [pendências do playtest do deserto](TODO.md#pendências-do-playtest-do-deserto-04127).

## Candidata local 0.4.127 (2026-11-24)

- A branch `codex/currais-tematicos-04127` parte da 0.4.126 (`1961a279`). Os seis NBTs em `data/villagecolony/structure/colony/` entram no rodízio de obras de vilas com pastor. O interior é plano, com 100 blocos de grama, faixa livre junto à cerca e uma porteira ligada à rede de caminhos.
- O curral grande aceita todas as espécies manejadas e tem prioridade sem limite de lotação. Sem acesso ao grande, o pastor tenta o médio compatível ou segue as regras do cercado comum. A contagem lê todos os currais. O abate preserva dois adultos da espécie no curral da vítima e continua exigindo excedente na vila.
- Os 50 GameTests focados e os três testes de NBT passaram. Nos currais novos do mod, a abertura ganha espaço durante a entrega de animais largos. O fechamento reconecta a cerca antes de soltar a guia.
- Sentinela `fast` final (`20261124T090817Z`): unitários e propriedades passaram. Passaram 1.254 de 1.255 GameTests. A única falha, `PenEscapeGameTest.normalFenceNavigationCanLeaveBeforeTheScheduledScan`, também ocorreu na 0.4.125 e na 0.4.126.
- O playtest da 0.4.126 registrou, na colônia `4dac0976`, três animais no grande e nove num cercado sem classificação. Houve seis rejeições de acesso externo e `BREED_WAIT ... no_reachable_ready_pair`. A sessão deixou a causa da baixa acumulação em aberto.
- O JAR `village-colony-0.4.127.jar` gerado por `remapJar` está em `downloads/` e ativo no TLauncher. As três cópias têm SHA-256 `5099153313BEFAF9F602F83735F28C785C0E05DEE5B4CB53EDA940C8E102E67C`. O JAR da 0.4.126 ficou desativado para retorno.
- No próximo jogo, confirme a ligação das porteiras à rua e observe a entrega ao curral grande. Bloqueie seus acessos num mundo de teste para verificar o destino temático. Observe o pastor pular a cerca, o animal caminhar pela porteira e a porteira fechar antes de soltar a guia. Acompanhe contagem, nascimentos e abates com `VC_PENS`, `VC_ROUND`, `VC_FLOCK_EVENT` e `VC_FLOCK_HARVEST`. Registre as pendências no [TODO](TODO.md).

## Candidata local 0.4.126 (2026-11-23)

- A branch `codex/playtest-04126` reúne a 0.4.125 e os ajustes N10 e N14. A porta da moradia nova prioriza o centro se a mesma pegada permite acesso pela rua. A varredura usa o limite de dois para baús criados.
- As regressões de abertura da obra, passagem da porta recuada e criação do baú falharam antes dos ajustes e passaram na versão corrigida. A rodada focada da candidata passou em 32 dos 33 GameTests. A saída espontânea do curral antes da varredura foi a exceção. O mesmo teste falhou na 0.4.125 (`bd7abd57`), antes de N10 e N14.
- O Sentinela `fast` (`20261123T184500Z`) passou em unitários e propriedades. Passaram 1.249 de 1.251 GameTests. As falhas foram a saída do curral e a tosquia N05 já registrada no TODO.
- JAR gerado por `remapJar`, copiado para `downloads/` e conferido nas três cópias. SHA-256 `C9B887359298C407B336A2C0BD77AAB5724078B6228F8786D679ED01830065F1`. Cópia no TLauncher preparada como `village-colony-0.4.126.jar.disabled`. Feche o jogo antes de substituir os JARs ativos 0.4.124 e 0.4.125.
- Use o [roteiro da moradia](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md#n10-e-n14--roteiro-da-moradia) para verificar acesso, recarga da obra, cama, baú e placa. As outras pendências continuam no [TODO](TODO.md).

## Candidata local 0.4.125 (2026-11-23)

- Na branch `codex/fix-villager-jumps-04125`, `VillagerMoves` mantém o pulo padrão diante de obstáculo previsto. O pulo maior só ativa após colisão horizontal confirmada; encalhe e saída de curral continuam exceções.
- A regressão de degrau de dois blocos e cerca passou com 6/6 GameTests focados. `build` final passou; PMD mantém 177 avisos preexistentes.
- JAR 0.4.125 copiado para `downloads/` e TLauncher. SHA-256: `D26EA2D70579B0117C599E28649471704892CB4055E75BF14E5D5B3092C08A74`. O 0.4.124 do TLauncher segue ativo enquanto o processo Java o mantém bloqueado; desative-o após fechar o jogo.
- Aguarde playtest em degraus e cercas. Confirme que o aldeão tenta o salto normal antes do maior e não fica preso.

## Candidata local 0.4.124 (2026-11-23)

- A branch `codex/playtest-04124` reúne o resgate de telhado (`c8b32ace`) e a correção do rebanho (`07afdcdc` na branch `codex/pastor-rebanho-nos-currais`, incorporado como `cd6924f9`).
- A coleta e a entrega registram os animais por UUID. A reprodução de corte usa pares do mesmo curral. O abate usa a meta vigente no total da vila e a reserva do curral da vítima.
- JAR instalado em `downloads/` do checkout do Desktop e na pasta `mods/` do TLauncher. As cópias e o build têm SHA-256 `53E341158D3963E0D1A7724A1B0AA0394593E906CA4C190E3AE07E128AC43FF8`. A 0.4.123 ficou como cópia desativada.
- No Sentinela `fast` (`20261123T170632Z`), unitários e propriedades passaram. Dos 1.247 GameTests, 1.244 passaram. Falharam a tosquia N05 e dois cenários de coleta de `ShepherdRoundGameTest`. Na repetição focada de rebanho, telhado, ronda e tosquia, os 20 passaram. O log confirmou nascimento e morte por queda do mesmo filhote, e abate com dois adultos preservados no curral da vítima.
- Retome pelo [roteiro do rebanho](docs/technical/Playtest-0.4.122-2026-11-23.md#próximo-playtest-do-rebanho) e pelo [TODO](TODO.md). A sessão anterior não tinha informação suficiente para explicar toda a perda de animais.

## Candidata local 0.4.123 (2026-11-23)

- Na branch `codex/resgate-telhado-04123`, a opção 1 protege o resgate de telhado. Um salto curto usa a distância segura de queda do aldeão. Para o desnível alto, o resgate o coloca em um piso natural ou rua com espaço livre, zerando a velocidade e a distância de queda.
- A regressão de oito blocos falhou antes da mudança. Os oito GameTests focados passaram após a correção, e o Sentinela `fast` passou nas três suítes (`20261123T152335Z`, 1.244 GameTests).
- Use o [roteiro do telhado](docs/technical/Playtest-0.4.122-2026-11-23.md) para conferir a chegada ao chão e a volta ao trabalho. O [TODO](TODO.md) mantém a investigação da subida e do retorno.

## Candidata 0.4.122-alpha (2026-11-23)

- A entrega reúne a correção do grafo e as mudanças do pastor da série 0.4.119–0.4.121. Os 17 GameTests do grafo passaram na candidata. Consulte as [notas da entrega](docs/releases/v0.4.122.md) e o [TODO](TODO.md) para a próxima rodada.
- Confirme as três colunas observadas na 0.4.118 e os casos originais da mina e da casa do mineiro. Siga o [roteiro de jogo](docs/technical/Playtest-0.4.118-2026-11-22.md).
- O [CI do PR #104](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/runs/45101176577) reproduziu N26 no desnível lateral. Após reposicionar o pastor, o teste passou na bateria local completa. O N26 passou nas duas rodadas do CI de `1a841653`. A primeira rodada falhou na tosquia (N05) e a segunda na travessia entre redes viárias após fusão de colônias. Confira o [PR #104](https://github.com/LucasRiboldi/MOD-village-1.21.1/pull/104) para o estado da integração.
- O JAR de `1a841653` foi instalado no TLauncher e em `downloads/` com SHA-256 `2CFB3FD629A42E03C19AF44064313CD1A815BD9B54D55123D07195BD7B85396B`. O [playtest de 23-11](docs/technical/Playtest-0.4.122-2026-11-23.md) usou o JAR anterior. Prioridade em jogo é impedir as mortes no resgate do telhado. O [TODO](TODO.md) mantém os demais pontos.

## Histórico da candidata local 0.4.121 (2026-11-22)

Na branch `codex/pastor-curral-04119`, `FlockHarvest` exige três adultos da espécie no curral da vítima e preserva dois. A reserva de seis na área da vila continua valendo. `FlockHarvestGameTest` cobre dois currais, animais soltos, outra espécie e um filhote. O [roteiro da candidata](docs/technical/Playtest-0.4.118-2026-11-22.md) cobre a validação em jogo e a investigação da queda na contagem.

Os testes da reserva passaram. A rodada focada confirmou na 0.4.120 a falha de guia longe da porteira. O cenário da vaca no desnível lateral falhou na bateria completa e passou isolado. Consulte o `STATE.md` antes de mexer na condução.

## Histórico da candidata 0.4.120 (2026-11-22)

A opção 3 do autor está em `ShepherdDelivery` na branch `codex/pastor-curral-04119`. A física do salto reutiliza `PenMoves`. A medição do curral continua em `ShepherdPens`. A confirmação no mundo do autor está pendente. O roteiro do relatório da 0.4.118 segue na candidata atual.

## Histórico da candidata 0.4.119 (2026-11-22)

- A branch `codex/pastor-curral-04119` parte de `606eb0f6`. A coleta, a separação e o abate respeitam
  recintos cobertos com porta. A navegação do animal alinha a entrada da porteira enquanto a guia continua presa.
- Sentinela `fast` passou com 1.886 testes unitários, uma suíte de propriedades e 1.241 GameTests (`20261123T004749Z`).
- O [relatório do jogo da 0.4.118](docs/technical/Playtest-0.4.118-2026-11-22.md) contém as seis notas e o roteiro
  da próxima sessão. Priorize confirmar a condução e cruzar a contagem do curral grande com `VC_FLOCK_HARVEST`.
  As entradas de casa e a autoria de blocos na obra continuam na fila.

## Histórico da entrega 0.4.118 (2026-11-22)

- No Claude Code, atualize a `main` antes de retomar. O PR #102 integrou o Lote A
  (`eb222eba`, N12/N28) e a orientação da placa do baú (`a9999d09`, N14).
  A correção do pastor (N05/N26/N27) já estava na `main` pelo PR #101.
- `remapJar` passou em `91241a66`. O JAR da 0.4.118 foi instalado como único ativo no
  TLauncher. Os arquivos em `build/libs/`, `downloads/` do checkout do
  desktop e `mods/` conferem com SHA-256
  `ED7D96FEC95A398C08E7E8DECDA881713E07CC5003D09995BC7D5651E4E6C4F5`.
- O autor dispensou novos testes locais nessa entrega. As
  [notas da 0.4.118](docs/releases/v0.4.118.md) registram as evidências e o roteiro.
- Continue pelo [plano do playtest](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md)
  e pelo [TODO](TODO.md). Confirme N05/N12/N14/N26/N27/N28 no jogo da 0.4.118.
  O atraso do baú segue aberto. N23 (cores das placas) segue pendente.

## Histórico do N14

- A branch local `codex/n14-placa-bau` contém a correção da placa e saiu da `main` em
  `6aa5cacf`. O atraso na criação do baú continua aberto. N10 aguarda o Lote A e N24
  requer decisão separada.
- O roteiro está no [plano do playtest](docs/technical/Plano-Playtest-0.4.115-2026-11-21.md#n14--roteiro-da-placa-do-baú).
  Os focados `ChestMarkerGameTest` e `BedChoiceGameTest` passaram (17 casos).
  Sentinela `fast`: 1.886 unitários, uma suíte de propriedades e 1.238 GameTests
  (`reports/test-runs/20261122T002039Z/`, relatório local).
- Para montar a candidata local 0.4.118, acrescente à revisão desta branch apenas o delta
  de `6aa5cacf..eb222eba` sob `src/` em uma área de build temporária. Isso inclui a
  reconexão da 0.4.117 (Lote A, N12/N28) e seu teste. As duas branches seguem separadas.
- A combinação passou em `build`, `remapJar` e 25 GameTests focados. A 0.4.118 é o
  único JAR ativo do mod no TLauncher. A 0.4.117 fica como `.jar.disabled`.
  O SHA-256 em `build/libs/`, `downloads/` e `mods/` é
  `ed7d96fec95a398c08e7e8decda881713e07cc5003d09995bc7d5651e4e6c4f5`.

## Histórico da reconexão 0.4.117 e do pastor 0.4.116

- **Prioridade atual:** validar em jogo a reconexão da mina e da casa do mineiro na 0.4.117 (Lote A, N12/N28).
  A candidata inclui a correção do pastor (N05/N26/N27) da 0.4.116.
  As 28 notas, seis imagens e demais pendências estão em `docs/technical/Plano-Playtest-0.4.115-2026-11-21.md`;
  o relatório é `docs/testing/relatorios/jogo-0.4.115-2026-11-21.md`. Não descartar os outros lotes.
- **Branch de retomada:** `codex/lote-a-reconexao`, criada sobre a `main` com a 0.4.116 integrada no PR #101.
  A cópia usada para gerar o JAR fica em `C:/Users/lucas/Desktop/MOD-village-1.21.1`.
- **Prova da 0.4.117:** a recusa antiga bloqueou a única passagem seca em GameTest. Com a correção,
  os 16 casos focados passaram. `fast` PASS em `reports/test-runs/20261121T233603Z/`.
- **Prova da 0.4.116:** `fast` PASS em `reports/test-runs/20261121T191357Z/`: 1.885 unitários,
  uma suite de propriedades e 1.237 GameTests. Build passou; 196 testes Python passaram.
  Mutação para a produção 0.4.115: cinco regressões falharam em `20261121T191211Z`; 23 focados verdes em `20261121T191032Z`.
  A revisão foi concluída, inclusive cleanup antecipado da arena. A análise histórica está em
  `reports/sentinel-analysis/20261121T191903Z/`; inclui falhas antigas e mutações, não é um relatório sem achados.
- **Artefato da 0.4.117:** use `village-colony-0.4.117.jar` para o próximo jogo. A entrega usa `remapJar`,
  `downloads/` e a pasta `mods` do TLauncher, com comparação de SHA-256.
- **Próximo passo:** seguir o roteiro do plano para mina, casa e pastor. Ao final do jogo, use `/vc salvar`.
  Desnível entre casas, autoria do corte no arco, demais itens do Lote A e lotes C, D e E seguem no plano.
- **Regras do autor:** relato de que entrou no jogo/testou sempre usa `vc-log-driven-fix`.
  Toda versão nova deve atualizar `downloads/` e `%APPDATA%/.minecraft/mods`, com SHA-256 igual ao build
  e apenas um `village-colony-*.jar` ativo; anteriores ficam `.jar.disabled`.
- **Candidata 0.4.115:** resgate de telhado limpa a memória de caminhada anterior. O teste da memória falhou antes
  e passou depois; Sentinela `fast` passou em `reports/test-runs/20261121T143122Z/`. Confirmar em jogo se o aldeão
  deixa de retornar ao telhado; a causa completa do retorno ainda não está comprovada.
- **Candidata local 0.4.114:** diário conserva os motivos de entrada e de saída, e `/vc salvar` mostra
  as mudanças recentes, sono e destino. A mina espera no corte que segura líquido, e a união de
  colônias reaproveita as duas redes. Sentinela `fast` passou com 1.232 GameTests
  (`reports/test-runs/20261121T125319Z/`); análise em `reports/sentinel-analysis/20261121T125835Z/`.
  `build` e `remapJar` passaram. Cópias em `build/libs/`, `downloads/` e `mods/` com SHA-256
  `10ebc1116c15bb8fda82bd23feaf876dbb3848fa7961930b769ce765ab17dfce`. A 0.4.113 foi desativada. Falta o jogo do autor.
- **Plano do ciclo:** `docs/technical/Proximo-ciclo-0113.md` — as nove escolhas (A8, A7, A9, A6, A2, A3a, A4, A5, R1) estão implementadas. Próxima verificação em mundo novo, com `/vc salvar` antes de sair.
- **R1, candidato (Regra 346):** relatório da sessão com a tabela completa de blocos, mina, obras, último grafo, janelas de tempo e eventos da colônia. GameTest focado passou e detectou a restauração do limite de 15 linhas.
- **A5, candidato:** desistência do pastor registra posições da ovelha e do pastor e imobilidade ou prazo esgotado. Teste falhou antes da mudança e sob mutação; quatro GameTests focados e Sentinela `fast` passaram. Relatório em `reports/test-runs/20261121T064752Z/`. Confirmar o diagnóstico no próximo jogo.
- **A4, candidato:** desistência do fazendeiro registra canteiro, posição do aldeão e imobilidade ou prazo esgotado. Teste falhou antes da mudança e sob mutação; três GameTests focados passaram. Sentinela `fast` passou em `reports/test-runs/20261121T063830Z/`. Confirmar o diagnóstico no próximo jogo.
- **Histórico da 0.4.113:** `fix/miolo-pelas-pecas` reunia também a 0.4.111 (Regra 347) e a 0.4.112 (Regra 348).
- **Entrega local:** candidata 0.4.113 em `downloads/` e no TLauncher, com a 0.4.112 desativada. SHA-256 igual nas três cópias, `928eaaf4e1a2a63525a7e14b1def77fa584be01ac27487476a1e523ea5afae99`. Sentinela `fast`, análise e build passaram. O Gradle foi encerrado.
- **A8, candidata (Regra 349):** quatro reservas reduzidas, `ingot` removido só da medição e `wool` preservado. Cinco regressões falharam com a mutação, e os 21 GameTests focados passaram com a regra restaurada.
- **Sentinela `fast` após o A9:** unitários, propriedade e 1.225 GameTests passaram na terceira execução. Relatório local em `reports/test-runs/20261121T045420Z/`. As duas primeiras execuções estão registradas no plano.
- **A7, candidata (Regra 350):** o cenário registra dois baús públicos com 256 terras no total. Sem o filtro, o aldeão recolhe terra com o estoque no teto. Com o filtro, os três GameTests focados passaram, cobrindo também a coleta de pedregulho e a retomada da coleta de terra abaixo de 256.
- **Análise do último jogo:** `docs/audit/2026-11-21-analise-jogo-0111.md` (achados A1 a A9, R1).
- **Linha de base restabelecida em 21-11:** `gradlew.bat build` passou com JDK 21 após isolar `earthAboveTheCeilingStaysOnTheGround` no lote `ground_pickup_earth_ceiling`.
- **A9, candidata (Regra 351):** os oito GameTests focados e os 196 testes Python passaram. Grama e caminho sem autor declarado saem do conflito. Entorno de obra e piso da mina têm autoria explícita.
- **A6, candidata (Regra 352):** sem argila disponível, a obra pede tijolos de pedra e o estoque exclui terracota. A retomada preserva peças assentadas. Os dois testes novos falharam sob mutação e os 18 focados passaram.
- **A2, candidata (Regra 353):** a escada, os patamares e o salão recebem tochas da colônia sem custo. As duas regressões falharam antes da correção e os 6 GameTests focados passaram depois.
- **Sentinela `fast` após o A2:** unitários, propriedade e 1.228 GameTests passaram após isolar o teste que limpa o cache. Relatório em `reports/test-runs/20261121T055553Z/`. O build final passou.
- **A3a, candidata (Regra 354):** cinco mudas no bosque por lenhador, primeiro do estoque da espécie escolhida. Adubação usa excedente da reserva de quatro ou lotes de 16 declarados no `VC_SUPPLY`. O GameTest reproduziu o estoque intacto antes da correção e sob mutação. Os 9 GameTests focados e 196 testes Python passaram.
- **Sentinela `fast` após o A3a:** unitários, propriedade e 1.228 GameTests passaram na primeira execução. Relatório em `reports/test-runs/20261121T061045Z/`. O build final passou.
- **Sentinela `fast` após o A6:** unitários, propriedade e 1.227 GameTests passaram na terceira execução. Relatório em `reports/test-runs/20261121T052007Z/`. O build final passou. As duas primeiras execuções repetiram falhas conhecidas de pastoreio.
- **Regras do modo:** `CLAUDE.md` §0.2.2 (escolha do autor antes de código, teste que falha primeiro, Sentinela, um JAR por ciclo, push só se o autor pedir). Responder em português. Build: `JAVA_HOME` do JDK 21, `./gradlew.bat build`; GameTests focados por `python scripts/gametest_focus.py <classe…>` (+ `restore`); em Bash no Windows, escreva scripts Python em arquivo em vez de heredoc com aspas.
- **Armadilhas já vistas:** ErrorProne com `-Werror` (parênteses, campos não lidos); `FileSizeRuleTest` (500 linhas); `ArchitectureRulesTest` (sem ciclo de pacote); `MineMarks.dug` apaga marcas a 8 blocos; a escada planejada da mina começa na altura da boca.
- **Avisar o autor:** a trava `wool` foi mantida contra o "saem" dele (é lógica, protege a lã branca); a tocha da colônia na mina abre exceção à Regra 212.

## Histórico até a 0.4.93

- **Versão:** 0.4.93 na `main` (branch `fix/jogo-0490` integrada por PR), **não jogada**; publicada como [`v0.4.93-alpha`](https://github.com/LucasRiboldi/MOD-village-1.21.1/releases/tag/v0.4.93-alpha). A 0.4.79 foi jogada em 2026-11-13 (taiga, Spark `oujZp2IxmS`); a triagem dos oito sintomas virou as Regras 275 a 282.
- **Modo:** correção por playtest (`CLAUDE.md` §0.2.2): log primeiro, opções ao autor, ele escolhe, teste que falha, correção, Sentinela, commit, JAR.
- **Entregue desde a 0.4.53 (todas candidatas):**
  - 0.4.54 a 0.4.56, Regras 243 a 249: plantação grande sobre areia, baú de qualquer lugar, aldeão fora da simulação não congela, cama longe vem para perto, chão nivelado é trabalho, um terço da madeira em tora, fundição sem cru chama o pedreiro.
  - 0.4.57 a 0.4.59, Regras 250 a 253: rua no chão novo, flor não segura coluna, acessório sem entrada, curral com terra no solo.
  - 0.4.60 a 0.4.62, Regras 254 a 256: patamar nas dobras da mina (ADR-053), pastor não amarra com curral, arco de pinheiro e lenhador só com folha.
  - 0.4.63 e 0.4.64, Regras 257 e 258 (reanálise dos logs): areia e argila sem reserva de cru; adiantamento lê todas as estruturas.
  - 0.4.65 a 0.4.68, Regras 259 a 262 (regras novas do autor de 12-11): oito ofícios na fundação, casa do mineiro na fundação, poste de luz, estruturas por bioma.
  - 0.4.69 a 0.4.74, Regras 263 a 269 (jogo da 0.4.68): fundidor funde onde está, grafo nunca sobre a água (174 revogada), linha planejada sai da construção, uma casa do mineiro basta, plantação grande em todo bioma, bosque em qualquer altura, prazo do pastor pela distância.
  - 0.4.75 a 0.4.79, Regras 270 a 274 (pedidos do autor de 13-11): sem ofício fora das análises, muda do bosque surge, corante das flores, um construtor por vila, obra conta como trabalho do construtor.
  - 0.4.80 a 0.4.82, Regras 275 a 282 (jogo da 0.4.79): soleira não é curral, alçapão em pé, lenhador encosta, água só sem caminho seco, solto só fora do lugar, árvore no caminho sai, log de tronco posto, só a rede reserva chão contra lote.
  - Varredura de falhas de 12-11 (`docs/audit/2026-11-12-varredura-de-falhas.md`): VF-001, VF-007 e VF-010 instrumentados; GameTest que mexe na hora ou na memória do mundo roda no próprio lote.
  - 0.4.93, Regras 315 a 325 e ADR-057 (simulações de 3 dias da 0.4.92 e escolhas de 18-11): fazendeiro semeia a terra comum, rua da vila gerada fica, `changes=` no grafo, três modos do grafo, porta do jogador é entrada, só aresta estável é calçada, terreno da vila inteira por fila (`VC_TERRAIN`), lenhador tira a folha, plantação grande inteira pelo molde. Sentinela com perfil longo.
  - 0.4.92, Regras 313 e 314 (jogo da 0.4.91): entrada no chão depois do degrau da porta; casa do mod com caminho na porta.
  - 0.4.91, Regras 305 a 312 (jogo da 0.4.90): entrada da casa no piso, aresta fixa só de rosa a rosa, telhado fora do chão do grafo, fundição na quantidade pedida, pastor sem prazo rumo ao curral, pó de osso do fazendeiro, areia moída antes, mineiro preso sobe rumo à borda.
  - 0.4.90, Regras 299 a 304 (jogo da 0.4.89): pastor leva o animal até o curral, porta e partes soltas no grafo, argila na hora, nó da mina sob o arco, frente da porta no piso, casa com solo. Falta o ponto 2 (nivelar ao longo do grafo).
  - 0.4.89, Regras 295 a 298 (jogo da 0.4.88 em montanha): ligação do grafo que não some, peças da vila no solo suavizado, chão suavizado em volta da obra nova, vila nova com um adulto por ofício e sobra sem ofício. Falta o ponto 5 (atividade por profissão).
  - 0.4.88, Regras 289 a 294 (ADR-056, escolhas G1–G7 do autor de 15-11): plantação grande semeada com 2 fazendeiros, tronco inteiro e folha no caminho, grafo na tábua e na frente da porta, sem torre de minério, pátio da mina, sobra tirada pelos ociosos. Falta a G8 (casa do mineiro medieval).
  - 0.4.87, Regra 288 (jogo da 0.4.86): nenhuma estrutura nasce sobre água; a água dentro da vila vira chão na geração (mundo novo).
  - 0.4.86, Regra 287 (jogo da 0.4.85): a tora deitada é chão; o fazendeiro entra na plantação grande.
  - 0.4.85, Regras 285 e 286 (jogo da 0.4.84): água com margem e gelo sob o pulo; mineiro novo na casa junto da mina; diagnósticos do grafo e do terreno.
  - 0.4.84, Regra 284 (VF-010): o animal atravessa o portão amarrado; relógio, vaca solta e canal de água estáveis nos GameTests.
  - 0.4.83, Regra 283 (GameTests instáveis de 14-11): o pastor só recolhe dentro da caixa da vila; testes do pastor, da tosquia e da taiga sem depender dos vizinhos (os lotes rodam juntos num mundo só).

### Roteiro histórico da 0.4.93

**O autor precisa jogar a 0.4.93 num mundo novo e mandar o Spark,** num jogo longo (o da 0.4.91 durou 6 min e não gerou `VC_TIME`). Confira `VC_TERRAIN` com nivelados todo dia (Regra 323), `CLEARED_LEAVES` do lenhador (324) e a plantação grande entregue inteira semeada (325); o modo do grafo é `atual` até o autor trocar com `/vc grafo modo`. Da 0.4.92: Confira a frente das portas no nível do chão (Regra 313) e o caminho na porta da BigHouseMOD (Regra 314). Da 0.4.91: Confira `doors` acima de 0 no `VC_GROUND` (Regra 305), `fixed=` e `loose=` no `VC_ROAD_GRAPH` (306 e 307), `CLAY_ON_REQUEST` e `BONE_MEAL_FOR_THE_FARM` no `VC_SUPPLY` (308 e 310), animais no curral grande (309), `MISSING_PIECE` de areia (311) e o mineiro sem voltar a encalhar no mesmo ponto (312). Da 0.4.90: As Regras 288, 296, 298, 303 e 304 (geração) só valem em chunk e vila gerados depois delas; confira também `doors` no `VC_GROUND`, `VC_PENS` com animais no curral grande e `CLAY_ON_REQUEST` no `VC_SUPPLY`; confira `refused=` no `VC_ROAD_GRAPH`, `moved`/`most` no `VC_GROUND` e `maxStep` no `VC_SITE_EDGE`. As triagens dos jogos da 0.4.85 e da 0.4.86 deixaram achados abertos no `TODO.md`. Use as notas `/vc bioma nota` e a lista `docs/testing/checklist-jogo-avaliacao.md`. Ao receber o log, siga `CLAUDE.md` §0.2.2 e confira:

- Lote (Regra 282): `lots_ok` acima de 0 no `VC_BIOME` e obra aberta; o overlay da obra volta com ela.
- Troncos (Regra 281): ler as linhas `VC_LOG_PLACED` e achar quem pôs os troncos que o autor viu na vila.
- Encalhe (Regra 279): nenhum "out at ... after 0 steps" seguido de novo encalhe no mesmo ponto.
- Ramal da mina (Regra 280) e água (Regra 278): o mineiro chega à mina pela rua; nenhum aldeão nadando longe; nenhum afogado.
- Pastor e alçapões (Regras 275 e 276): nenhum "fenced in" em soleira de casa nem em canteiro de alçapões.
- Pastor (Regra 283): o animal solto perto do curral ainda é recolhido; nenhum animal de fora da caixa da vila.
- Curral (Regra 284): o animal entra pelo portão sem fugir para longe; nenhum "gave up: the cow did not walk into the pen".
- Água (Regra 285): `VC_WATER_RESCUE` e `VC_WATER_ICE`; ninguém preso na água.
- Mineiro (Regra 286): dorme na casa junto da mina desde o primeiro dia (no jogo da 0.4.85 não houve linha de log que provasse).
- Fazendeiro (Regra 287): trabalho acima de 1% no `time_ledger`; nenhum "could not reach the crop" na plantação grande.
- Ler `loose=` no `VC_ROAD_GRAPH`, `VC_SMOOTH` e `VC_LOT_EDGE` para escolher as correções das notas 1, 3, 7 e 8 do jogo da 0.4.84.
- Varredura: a linha `VC_COST_PHASES` (VF-001) e o fantasma com `lastSeen` (VF-007).

Em aberto no `TODO.md`: VF-001 a VF-003 da varredura e os itens das fases F0 a F5.

## Ordem de leitura

1. Este arquivo.
2. `AGENTS.md`, inteiro no topo; `CLAUDE.md` §0 a §2.1. `CLAUDE.md` vence em conflito.
3. `STATE.md` (cabe em 150 linhas).
4. `docs/audit/2026-11-13-avaliacao-qualidade.md`: notas, portões e cronograma até a 1.0. O `TODO.md` segue as fases dele;
   os abertos antigos foram triados em `docs/technical/TODO-herdado-2026-11-13.md` (cada um diz se fechou ou para onde foi).
5. `docs/RULES.md`: procure a regra por número. ADRs ficam no índice `docs/decisions/README.md`.

## Regras do autor que não estão nos outros documentos

Elas viviam só na memória local do Claude Code. O que está em `CLAUDE.md` e `AGENTS.md` continua valendo.

- **Idioma:** sempre português do Brasil. Ao perguntar ou oferecer opções, frases completas, de modo leigo.
- **Formato (autor, 2026-11-24):** resposta visual e colorida (emojis de cor, tabelas curtas), com a etapa atual sempre visível. Toda decisão de correção traz **3 opções** — a mais simples, uma maior e uma com melhorias —, com efeito, risco e a recomendada marcada. Espera do autor bem marcada ("aguarde" contra "sua vez"). Ao fechar uma correção, buscar sozinho a próxima correção ou melhoria e trazê-la já diagnosticada; editar comportamento continua exigindo a escolha dele.
- **"commit push":** commite, envie e responda só `commit push realizado`. Se algo falhar, não diga a frase: diga o que houve.
- **"relatorio commit":** ciclo completo: estado atual, revisão, testes de verdade, README, `TODO.md` por prioridade, higiene, commit, push e relatório `✅ CICLO ENCERRADO`. Separe "tem teste" de "foi verificado rodando de verdade".
- **Merge na `main`:** autorizado sem perguntar quando é o próximo passo, com CI verde e sem conflito (`gh pr merge N --merge`). **Release e tag exigem pedido.** Nunca `--force`.
- **Sistema velho:** autor em 2026-11-06: "ignora a antiga, esquece o que é velho e passa sempre a usar o sistema mais atual".
- **Não marque como feito o que não rodou:** diga o que foi verificado, o que não foi e o que depende do jogo. Correção é candidata até o próximo playtest.
- **Imagens do autor valem mais que a leitura do texto dele:** o desenho da 0.4.43 derrubou uma recomendação minha de juntar encontros próximos.
- **Limpeza:** ao fim de cada etapa, `./gradlew.bat --stop` e mate o que ficou ocioso.
- **CI:** só altere os workflows com pedido do autor (o de 13-11 autorizou: cobertura da bateria guardada, checagem de espaços contra a base, release só com CI verde do mesmo commit).

## Ambiente

- **Máquina do autor:** Windows 11, projeto em `C:\Users\lucas\Desktop\MOD-village-1.21.1`.
- **JDK 21:** o `PATH` não basta. Em Git Bash: `export JAVA_HOME=/c/Users/lucas/.jdks/jdk-21.0.12+8`.
- **Compilar:** `./gradlew.bat compileJava compileTestJava compileGametestJava -q`. ErrorProne roda com `-Werror`, então aviso reprova.
- **Sentinela:** `python scripts/test_lab.py fast`, depois `python scripts/sentinel_analysis.py`, com o Minecraft fechado. Mudou regra ou teste: `python scripts/rule_coverage.py`. Mudou `docs/RULES.md`: `python scripts/rules_to_readme.py`.
- **Instalar para o autor jogar:** `./gradlew.bat remapJar`, copie `build/libs/village-colony-<versão>.jar` para `downloads/` e para `%APPDATA%\.minecraft\mods` (TLauncher), renomeie o anterior para `.jar.disabled` e compare o SHA-256 das três cópias.
- **Publicar:** com `docs/releases/v<versão>.md` na `main`, `gh workflow run Release --ref main`. O workflow compila outro JAR, com outro SHA-256.

### Só existe na máquina do autor, fora do GitHub

`%APPDATA%\.minecraft\logs\latest.log` (sobrescrito a cada jogo), o diário `logs\villagecolony-actions-*.jsonl`, o save `saves\Novo mundo`, `reports/`, `graphify-out/`, `downloads/` e os checkpoints `.handoff/`. Sem acesso a eles, peça o log ao autor: os relatórios e o `scripts/save_read.py` dependem deles.

O hook do Claude Code que manda rodar `graphify` antes de ler arquivos não se aplica se `graphify-out/` não existir.

## Ferramentas

| Script | Para quê |
|---|---|
| `scripts/time_ledger.py <log>` | tempo por ofício (Regras 50 e 51) |
| `scripts/action_report.py` | diário e motivos de recusa; toda análise de Spark o mostra ao autor |
| `scripts/cost_ledger.py <log>` | custo por fase do ciclo |
| `scripts/spark_decode.py` | resumo de um Spark baixado |
| `scripts/save_read.py` | grafo e mapa do save, sem abrir o jogo |
| `scripts/gametest_focus.py` | roda só algumas classes de GameTest; `runGametest` não aceita filtro |
| `scripts/gametest_summary.py` | total e falhas do relatório de GameTest |

Triagem no log: `VC_BIOME_NOTE` primeiro, depois `VC_MAE_SITE`, `VC_ROAD_GRAPH`, `VC_ROAD_COVERED`, `VC_SUPPLY`, `VC_SUPPLY_ERROR`, `VC_PENS`, `VC_DAY`, `VC_STATE`.

**Prova com sabotagem:** teste verde de primeira não prova nada. Depois de implementar, troque um comportamento de cada vez e confira que algum teste falha. Na 0.4.43 isso expôs um teste que passava de qualquer jeito.

## Verificado e não verificado na 0.4.53

- **Verificado:** Sentinela com 1085 GameTests e 1787 unitários, PIT local de 85%, CI da PR 65 e da `main` verdes, sabotagens pegas em cada versão.
- **Não verificado:** nada das 0.4.48 a 0.4.52 foi jogado; os efeitos em jogo das Regras 231 a 242 dependem do próximo jogo.
- **Testes instáveis:** `animalsWalkInThroughEachOfTheFourGates` e `workerCollectsWhileWalkingPast`, sem causa achada. Veja o `TODO.md`. Repita a bateria antes de culpar a mudança.

## O que falta no MAE

Lista viva em `TODO.md`, seção "MAE: o que falta depois da 0.4.26". Resumo:

- **Depende de jogo:** `/vc mae obra <modelo>` do `queued` ao `done`, retomada depois de reiniciar, obra `fora`, socorro até o fim (só disparou, e a obra foi parada), carga real.
- **Depende de código:** testes de `MaeSites.move` e da volta que zera a conta em `LotSeeking`; medir de novo o custo de `MaeGenerationEvidence`. Os testes de `WaitingWork` e da soma para o socorro entraram (`MaePatienceGameTest`).
- **Limitação conhecida:** ruínas de saves que já abriram a 0.4.38 ou depois não são achadas, porque as regiões antigas sumiram do save.

## Git

- **PR aberta antiga:** [#45](https://github.com/LucasRiboldi/MOD-village-1.21.1/pull/45), `codex/local-test-sentinel`, sem atividade desde 2026-11-02. Pergunte ao autor antes de mexer.
- **Só locais e fora da `main`:** `docs/regra-166-ferramentas` e `fix/miner-direct-path-gametest`, 1 commit cada, de outubro. Não foram enviados: são velhos e não fazem falta ao ponto atual.
- **Muitas branches `origin/claude/*` e `origin/codex/*`:** históricas. A existência de uma branch não prova que está integrada nem concluída.
- **Atribuição:** o rodapé `Co-Authored-By: Claude` dos commits antigos é do Claude Code. Use o seu.

## Memória das sessões anteriores

[`docs/archive/agent-memory/`](docs/archive/agent-memory/MEMORY.md) traz a cópia da memória do Claude Code: uma lição por arquivo, com o índice em `MEMORY.md`. É histórico: procure por tema (`rg`), não leia inteiro, e confira no código antes de confiar, porque cada nota reflete o dia em que foi escrita. Vale ler quando for mexer no assunto: gametest, mina, pastor, grafo, MAE.

As armadilhas que mais custaram:

- Classe de GameTest fora de `src/gametest/resources/fabric.mod.json` some da bateria em silêncio.
- Relatório de GameTest velho engana: apague `build/gametest-report.xml` antes de rodar.
- GameTests do mesmo lote interferem entre si: use `batchId` próprio e coordenada longe.
- `findPathTo` logo depois de `spawnEntity` dá `null`: espere pousar.
- Heredoc grande no shell quebra: escreva o script em arquivo.
- Rodar `gradlew.bat` de dentro de um `subprocess` do Python com `shell=True` no Windows falha: use Git Bash.
