# Plano de correção — jogo da 0.4.115 em 21-11-2026

Status: a candidata local 0.4.118 aguarda playtest da orientação da placa (N14), da reconexão da 0.4.117
(N12/N28) e do pastor da 0.4.116 (N05/N26/N27). O atraso na criação do baú continua em investigação.

## Evidência e limites

- `C:/Users/lucas/AppData/Roaming/.minecraft/logs/latest.log`: linha 95 confirma `villagecolony 0.4.115`; 28 linhas `VC_BIOME_NOTE`, entre 14:26:54 e 15:17:21. Encerramento normal às 15:21:50. Cenário: `Novo mundo`, planície, colônia `c5cd8b44`.
- Diário explicitamente selecionado: `logs/villagecolony-actions-20261121-142458.jsonl` da mesma instalação.
- Imagens do autor: `C:/Users/lucas/Desktop/2.jpg` até `7.jpg`, comparadas pelos nomes originais abaixo. Os arquivos continuam fora do Git; as descrições permitem retomar a triagem sem os anexos.
- [Relatório único gerado](../testing/relatorios/jogo-0.4.115-2026-11-21.md), por `scripts/game_report.py`, com log e diário explícitos. Também executado `scripts/cost_ledger.py` sobre o mesmo log.
- A triagem começou na worktree `70b9`, base 0.4.110. Após a escolha do autor e sua delegação sobre a linha de trabalho, a correção segue em `C:/Users/lucas/Desktop/MOD-village-1.21.1`, branch `fix/miolo-pelas-pecas`, partindo de `ae2ed2ab` (0.4.115). A worktree antiga conserva seus documentos locais; não gerar nem instalar um JAR dela. Esta continuação preserva a linha existente, sem abrir outra implementação concorrente.
- Foto isolada mostra geometria e estado instantâneo; não prova duração, autor da edição de um bloco ou causa no código. As localizações técnicas abaixo são pontos de investigação, não causas confirmadas.

## Comparação das seis imagens

| Arquivo | O que a imagem sustenta | Notas relacionadas |
|---|---|---|
| 2.jpg | Degraus e parede de terra entre casas, com desnível de aproximadamente dois blocos; falta de transição caminhável no trecho mostrado. | N09 |
| 3.jpg | Horta delimitada por troncos e ramal amarelo entre canto e grafo; base visual para escolher o canto mais próximo. Não mede qual canto é ótimo. | N11 |
| 4.jpg | Traçados amarelos próximos e paralelos ao redor de casas/caminhos. Sustenta revisar conexão e evitar ramais redundantes. | N13 |
| 5.jpg | Baú ao lado da cama e placa vista lateralmente entre cama/parede. Compatível com a nota sobre orientação; não demonstra quando o baú apareceu. | N14 |
| 6.jpg | Casa com piso/entrada rebaixados em relação à faixa de terra vizinha; transição abrupta no centro da vila. | N25, N09 |
| 7.jpg | Pastor com nome vermelho, junto à cerca, preso ao animal por uma guia; vaca em terreno elevado do lado externo. Não permite concluir sozinho se a porteira estava aberta. | N26, N27 |

## Inventário integral das notas

Horários são os do log. P1: acesso, integridade e fluxo bloqueado. P2: comportamento ou funcionalidade. P3: apresentação. R: confirmação positiva a preservar. Cada critério é uma prova futura, ainda não executada.

| ID / hora / linha | Evidência da nota e imagem | Ação proposta e verificação de aceite |
|---|---|---|
| N01 · 14:26:54 · 740 · R | “nenhuma terra voando acima das estruturas” | Preservar limpeza superior. Regressão de geração em planície e montanha; nenhum bloco residual suspenso sobre casas. |
| N02 · 14:27:13 · 789 · R | “bighousem em altura semelhante” | Preservar escolha da altura da BigHouseMOD; testar novamente em relevo acidentado, sem generalizar este sucesso de planície para montanha. |
| N03 · 14:27:44 · 866 · R | “mina acessivel mineiro entra e cava” | Preservar acesso e mineração durante mudanças no grafo, arco e escada. GameTest de entrada e progresso real de escavação. |
| N04 · 14:28:25 · 1118 · R | “mineiro entra e dorma na casa junto da vila” | Preservar cama/casa acessíveis. Observar ciclo noturno e retorno à mina, inclusive após recarga. A nota diz vila, não comprova distância exata à mina. |
| N05 · 14:29:49 · 1566 · P1 | “la retirada sem o pastor encostar” | Investigar alcance e linha de visão da tosquia. Testar ovelha distante e atrás de obstáculo: não tosquiar; aproximar e só então obter lã. Log atual registra tosquias, mas não distância no instante: EVIDÊNCIA_INSUFICIENTE para cravar a causa. |
| N06 · 14:30:10 · 1731 · R | “caminhos chegam nas portas terreas” | Preservar seleção de entrada térrea durante a revisão do grafo, com casa de dois andares como regressão. |
| N07 · 14:30:50 · 1842 · P1 | “um bloco do portal da mina foi cortado” | Rastrear edição no diário por posição e agente; conferir proteção do arco contra escavação, limpeza e terreno. Teste preserva todos os blocos estruturais. EVIDÊNCIA_INSUFICIENTE sobre quem cortou e qual bloco: não atribuir ao mineiro sem prova. |
| N08 · 14:31:36 · 2046 · P1 | “terreno em volta do arco ... paredes intranspoviveis” | Preparar acesso ao redor da boca da mina por smoothing do Zelador, preservando arco, fundações e alterações do jogador. Testar percurso transitável dos dois lados, sem saltos de dois blocos. |
| N09 · 14:33:35 · 2355 · P1 | “entre uma construção e outra ... dois bloco ... no maximo 1 bloco”; 2.jpg | Nivelar as transições entre construções, não apenas o piso do lote. Prova: rota entre portas com diferença de altura de no máximo um bloco por passo e navegação real. |
| N10 · 14:34:28 · 2402 · P2 | “porta da casa do MOD ... virada para o centro” | Requisito novo de orientação dos modelos. Testar rotações do NBT e alinhamento da entrada ao centro, mantendo acesso; registrar deliberação se conflitar com orientação vigente. |
| N11 · 14:36:23 · 2462 · P2 | “entrada ... fazenda ... canto mai proximo da linha do grafo”; 3.jpg | Selecionar canto acessível mais próximo de um segmento do grafo, com ramal curto. Testar as rotações da fazenda e distância ao segmento, não só ao nó. |
| N12 · 14:38:32 · 2521 · P1 | “no de grafo do arco ... nao se conecta” | Investigar geração/registro/reconexão da entrada da mina. Testar caminho no grafo desde o arco até a rede principal, antes e depois da recarga. |
| N13 · 14:41:03 · 2575 · P2 | “conectar a aresta mais proxima ... evitando arestas paralelas”; 4.jpg | Revisar inserção de ramais para aproveitar segmento existente e evitar duplicação próxima; preservar acesso de todos os nós. Testar geometria com duas ruas próximas e obstáculos. |
| N14 · 14:44:00 · 2706 · P2 | “bau ... depois ... nasceu”; “placa ... apontando para a porta”; 5.jpg | Orientação corrigida em código, com GameTest de reposicionamento. Aguardar playtest. O atraso na criação do baú segue aberto e precisa ser reproduzido após a atribuição da cama. |
| N15 · 14:47:30 · 2957 · P2 | “caracol ... descendo mais uns 7 blocos ... plato de 3x3x3 ... mais 7 para direita” | Mudança de geometria da mina: revisar `MineStairPoints`/`MineArm` e ADR do patamar antes de alterar. Provar largura, altura livre, patamares e rotação sem cortar arco/casa. Antes da implementação, fixar se “7 blocos” é avanço ou queda vertical; a nota não define isso inequivocamente. |
| N16 · 14:51:04 · 3838 · P2 | “lista inicial ... 1° fazenda grande ... 2° curral grande ... 3° ... casas ... .nbt cada” | Planejar fila experimental fazenda → curral → casas específicas com NBT por profissão. Definir relação com estruturas obrigatórias de fundação, material físico, retomada e moradia. Testar ordem sem pular por falta de material e não duplicar após reinício. Exige revisão das regras/ADR antes do comportamento. |
| N17 · 14:51:43 · 3940 · P1 | “zelador caminha muito ... não ... fazendo nada” | Correlacionar ação física e contador de trabalho com deslocamento; distinguir pavimentação e nivelamento. Relatório: 65% trabalho, 33% caminhada; não concluir ócio com base apenas na impressão. Teste de cenário com desnível deve registrar bloco alterado e queda de defeitos. |
| N18 · 14:52:39 · 4153 · P2 | “não ... gerar corações fora ... reunião ... final do dia” | Requisito de agenda para cortejo/reprodução. Investigar Vanilla e decisão arquitetural antes de integrar; nenhum mixin novo sem ADR. Testar horário/local permitidos, negativas fora deles e preservação de necessidades normais. |
| N19 · 14:54:22 · 4570 · P2 | “golens ... boiar ... pular para sair ... agua” | Pedido novo, não falha de afogamento comprovada aqui. Investigar movimento Vanilla; testar água com borda alcançável, subida e limites sem voo/teleporte. Decisão arquitetural se exigir novo ponto de integração. |
| N20 · 14:57:14 · 4676 · P2 | “casa do pastor ... proxima do curral grande” | Preferir lote caminhável junto ao curral grande. Testar seleção com candidatos próximos e distantes; definir fallback quando não existe lote válido. |
| N21 · 15:01:49 · 4776 · P2 | “casa do lenhador ... ate 10 blocos fora ... londe da agua ... expandir ... 10 blocos ... rebentos” | Planejar busca da casa até dez blocos além da vila, preferência por área seca, expansão após conclusão e mudas no raio de dez blocos. Definir distância à água e origem das mudas nas regras vigentes; testar limites e recarga sem duplicação. |
| N22 · 15:05:37 · 4941 · P1 | “zelador ... todo as arestas ... corrigindo ... adjascentes” | Ronda periódica de manutenção ao longo de todas as arestas e adjacências; integrar à fila de terreno existente. Provar cobertura, edição física e retomada sem recomeçar sempre pela primeira aresta. Definir cadência e largura de adjacência antes do código. |
| N23 · 15:06:52 · 5056 · P3 | “revisar as cores ... mais cores ... possibilidades” | Inventariar estados realmente existentes e mapear em cores/legenda coerentes em `WorkerNameplate`. Testar prioridade de estados e transições; não criar estados sem evidência só para adicionar cores. |
| N24 · 15:09:56 · 5774 · P2 | “vanilla ... nao sao do mod ... teste ... conforme ... zelador ... voltar ... normal” | Pedido de experimento reversível. Colide com a regra temporária de exclusividade por profissão: registrar exceção e duração antes de implementar. Delimitar população elegível e ativação; desligar restaura IA/ofício Vanilla, sem conversão permanente ou perda de trades. |
| N25 · 15:12:10 · 6298 · P1 | “estrutura abaixo ... 2 blocos ... nivelado pelo zelador”; 6.jpg | Mesmo eixo de N09, caso independente de casa nova rebaixada: testar pós-construção e transição para terreno/casa vizinha, sem enterrar porta nem cortar fundação. |
| N26 · 15:14:07 · 6840 · P1 | “pastor travado, segurando animal, animal girando”; 7.jpg | Reproduzir cerca, desnível e animal na guia. Investigar `ShepherdPens`, `PenTether`, `PenGate` e replanejamento. GameTest deve levar animal pelo portão e encerrar tarefa, sem repetição indefinida. |
| N27 · 15:16:06 · 7102 · P1 | “talvez ... abrir ... primeiro ... depois ... pular ... animal ... porteira” | Tratar como proposta do autor para N26, ainda não causa provada. Testar abrir antes de atravessar e manter aberto até animal passar. Avaliar salto do pastor como alternativa condicionada; animal continua atravessando a porteira. Não tornar teleporte uma solução implícita. |
| N28 · 15:17:21 · 7137 · P1 | “nó da casa do mineiro ... conectada ... deixou ... todos nos ... sempre ... conectados” | Regressão de conectividade após terreno/obra/reconstrução do grafo. Verificar todos os nós de entrada válidos, incluindo mina e casa, com reconexão após mudanças e recarga. Se não houver rota física, registrar impedimento, sem desenhar ligação fictícia. |

## N14 — Roteiro da placa do baú

1. Reinicie o jogo e confirme `villagecolony 0.4.118` no `latest.log`.
   Abra uma cópia da vila do teste anterior. Entre pela porta da casa do carpinteiro e confira
   se a placa do baú fica visível pela passagem. Aguarde um ciclo da colônia se o baú já tinha placa.
2. Confira se há um só quadro do mod, com o mesmo ícone e nome. Salve, reabra o mundo e confira de novo.
3. Repita numa casa recém-atribuída. Se o baú demorar, anote o horário, as coordenadas, o ofício e
   os instantes em que a cama foi atribuída, o baú apareceu e a placa ficou visível.
4. Se a face voltada para a porta já tiver um quadro seu, confira se a decoração é preservada.

O GameTest liberou a passagem de um baú com placa legada, verificou o respeito ao quadro do jogador
e confirmou a troca para a face norte sem duplicar a placa nem perder a profissão. A busca da porta
usa piso livre no mesmo nível, até seis blocos de distância do baú. Sem uma rota reconhecida,
o quadro existente é mantido ou, para um baú sem placa, vale a primeira face lateral livre.

## Evidência adicional e hipóteses de trabalho

1. **Terreno/grafo — P1:** às 15:14:56, `VC_TERRAIN ... day=2 ... steps=217 levelled=0`; às 15:13:02, `VC_ROAD_GRAPH ... isolated=5`. A casa do mineiro aparece em `loose=door@29,63,-31`, inclusive às 15:12:00. Há também `VC_SMOOTH ... planned=5 done=5` às 15:11:55: smoothing parcial existe; não afirmar que nunca roda. Hipóteses a distinguir: cobertura insuficiente da fila; pontos recusados/protegidos; tarefas não executadas; geometria de conexão sem rota; reconexão invalidada depois de edições. Investigar `TerrainQueue`, `WorldTerrain`, `VillageGround` e os componentes `Road*`, sem escolher uma causa antes da reprodução.
2. **Pastor — P1:** 3% trabalho, 70% caminhada, 10% bloqueado e 16% ócio; soma arredondada de ócio+bloqueio = 26%. `VC_PENS` registra zero animais às 15:09:07 e um às 15:14:52: existe progresso parcial, mas isso não elimina o travamento observado na foto. Separar entrada no curral, alcance da tosquia e ronda.
3. **Fluxo de produção — P2:** construtor 37% ocioso+bloqueado e 13% bloqueado; carpinteiro 46% ócio; pedreiro 41% ócio; fundidor 29% ocioso+bloqueado. Lenhador: 73% caminhada e 13% trabalho. Investigar demanda, recursos, acesso e medição, sem usar trabalho de outra profissão como alternativa automática.
4. **Estoque/baús — P2:** 207 recusas de cenoura, 430 de sementes de trigo, 58 de lã branca, 45 de terra e três de carpete branco. São eventos de recusa, não prova de perda de itens; correlacionar limites e origem das transferências antes de mudar capacidades. Carpinteiro registra seis segundos sem cama; conferir se é transição ou falha persistente.
5. **Custo — investigação P2:** 11 amostras, uma colônia: total médio 56,42 ms, p95/máximo 429,71 ms; fase `population` p95/máximo 423,06 ms. Confirmar janela e repetibilidade antes de otimizar; não interpretar como medida de FPS nem custo constante de todo tick. Não foi fornecido Spark nesta rodada.

## Ordem proposta e gates

1. **Lote A — acesso e integridade:** N07–N09, N12–N13, N17, N22, N25, N28. Primeira reprodução pequena: duas casas em alturas diferentes, nó da mina/casa ligado e depois terreno alterado. Preservar N01–N04 e N06. Inspecionar autoria do corte do arco antes de qualquer patch.
2. **Lote B — pastor:** N05, N26–N27. Separar regressões de tosquia e travessia; só promover a regra de abertura/salto após reproduzir o bloqueio.
3. **Lote C — implantação e moradia:** N10–N11, N14, N16, N20–N21. Deliberar fila inicial e modelos por profissão; proteger compatibilidade de saves. Baú e placa podem formar correção atômica separada da fila.
4. **Lote D — regras e experiência:** N15, N18–N19, N23–N24. Geometria da mina, reprodução, golem e aldeões Vanilla são decisões separadas, não uma refatoração geral. A exceção de N24 precisa ficar explícita e reversível.
5. **Lote E — achados complementares:** investigar fluxo, recusas e custo do relatório, com reprodução e prioridade própria; não alterar reservas por recomendação automática isolada.

Cada lote será dividido em uma tarefa por commit. Antes de código: confirmar branch/PR concorrentes e regras atuais na base 0.4.115 ou posterior, baseline `build`, GameTest que falhe pela causa correta, menor correção escolhida, teste verde, revisão e Sentinela. Atualizar regra/ADR quando aplicável. Após os gates, gerar candidata, instalar no TLauncher e em `downloads/`, conferir hashes. Resultado de jogo só pode ser declarado depois do próximo playtest.

## Opções apresentadas no início do ciclo

**Opção 1 — acesso primeiro (recomendada).** Evidência: N08/N09/N25/N28, imagens 2/4/6 e nós isolados. Investigar e corrigir o Lote A, mantendo todos os outros itens na fila. Verificação: GameTests de transição de altura e conectividade após mudança/recarga, seguidos de playtest. Risco médio: terreno, proteção e grafo interagem.

**Opção 2 — pastor primeiro.** Evidência: N05/N26/N27, imagem 7 e 70% caminhada. Investigar e corrigir o Lote B. Verificação: GameTests de alcance de tosquia e animal atravessando porteira em terreno irregular. Risco médio: navegação e comportamento do animal.

As duas opções preservam o inventário completo; escolher uma determina a primeira intervenção, não descarta as outras. Nenhuma autoriza implicitamente as decisões arquiteturais ou a exceção temporária de profissão.

## Lote B — candidata 0.4.116

- **N05, causa reproduzida:** a tosquia admitia alcance de 4,5 blocos. Agora a caminhada mira a ovelha com chegada zero e a ação exige contato entre caixas de colisão, com tolerância de 0,25 bloco. A lã continua vindo da tosquia física e entrando no baú.
- **N26/N27, causa reproduzida:** a condução soltava a corda com o animal ainda lateral à entrada e conservava o destino de busca. A porteira podia continuar fechada no ramo de espera. Agora abre antes, aproxima pela célula externa e entra mais fundo mantendo a corda. Solta somente com o animal alinhado ao centro externo (0,75 bloco) ou já dentro; limpa destino e navegação anteriores na passagem para a amarra do curral. O fechamento existente espera o vão ficar livre.
- **Escopo:** `ShepherdWork` e `ShepherdHerding`; sem novo mixin, sistema de salto ou tarefa de outra profissão. O salto de cercas já existia em `ColonyPathing`/`VillagerMoves`; a tosquia verifica aproximação física, sem impor uma rota única ao aldeão. O animal continua atravessando o portão.
- **Prova da regressão:** com somente os dois arquivos de produção devolvidos à 0.4.115, cinco dos nove GameTests falharam pelas causas esperadas (`reports/test-runs/20261121T191211Z/`). A vaca continuou na fase LEAD com idade crescente, comprovando simulação física ativa. Com a correção, os 23 testes focados passaram (`20261121T191032Z`).
- **Montagem dos testes:** arenas físicas usam `Arena.forceChunks` e conferem idade das entidades, pois uma origem aleatória podia deixar o animal sem ticks. A cadência da condução é de cinco ticks e a do fechamento é de vinte, como na produção. Não há teleporte no teste físico de travessia.
- **O que foi descartado durante a investigação:** a distância em coordenadas de bloco deixava um caso lateral na fronteira sem avançar. Medir a posição real do animal resolveu essa transição no cenário reproduzido. A hipótese de precisar de um salto novo perdeu força ao verificar o salto de cerca existente e a tosquia física no GameTest. As primeiras falhas da arena sem ticks foram descartadas como prova da regressão.
- **Pendente de jogo:** repetir a tosquia com ovelha distante e dentro de curral fechado; levar vaca do desnível lateral e conferir entrada, soltura da corda, porteira fechada e continuidade do trabalho. Gerar `/vc salvar` antes de sair. A melhora do tempo bloqueado/ocioso só pode ser medida no próximo playtest.

### Verificação e artefato

- `python scripts/test_lab.py fast`: PASS nas três suites, `reports/test-runs/20261121T191357Z/`: 1.885 unitários, uma suite de propriedades e 1.237 GameTests. Os 196 testes Python passaram; README gerado e escopo conferidos. `./gradlew.bat build`: PASS.
- Revisão encerrada sem bloqueios após corrigir o cleanup da checagem antecipada. Análise histórica em `reports/sentinel-analysis/20261121T191903Z/`; inclui execuções de diagnóstico e mutação, com resultados antigos preservados.
- JAR candidato: `village-colony-0.4.116.jar`, gerado com `remapJar` após o commit `7a07b38f`. Versão no `fabric.mod.json` conferida. As cópias em `build/libs/`, `downloads/` e `mods/` têm SHA-256 `702fda50d734e8a8c77baab8a809c83a489955d37a866829fdf4ac97426ce01a`. A 0.4.115 foi desativada no TLauncher.

### Decisão após o Lote B

O autor escolheu avançar para N12/N28 enquanto a validação do pastor seguia pendente. A autoria do corte no arco, os desníveis entre casas, os ramais redundantes e a cobertura da manutenção pelo Zelador continuam abertos.

### Roteiro do pastor — 0.4.116

1. Reinicie o Minecraft após a instalação e confirme `villagecolony 0.4.116` no `latest.log`. Comece em mundo novo, em planície, para comparar com o jogo anterior; repita depois em terreno irregular.
2. Com pastor e demanda por lã, observe ovelha a aproximadamente quatro blocos e separada pela cerca. Ela deve conservar a lã até o pastor chegar junto; a tosquia deve gerar lã no baú.
3. Observe uma coleta com vaca do lado externo, em desnível lateral de um bloco, e porteira inicialmente fechada. O pastor deve abrir, aproximar o animal da frente e entrar mantendo a corda enquanto necessário. O animal deve cruzar pela porteira.
4. Confira se a corda solta depois do alinhamento/entrada, o vão fecha quando livre e o pastor segue para outra atividade. Registre se continuar girando, preso à cerca ou repetindo o mesmo destino; não considere somente a cor do nome como prova de travamento.
5. Registre cada resultado com `/vc bioma nota` e gere `/vc salvar` antes de sair. Informe qual cenário falhou e anexe a imagem correspondente. O novo relatório permitirá comparar bloqueio e ócio com a sessão 0.4.115.

## Lote A — candidata 0.4.117 (N12/N28)

- **Causa reproduzida:** uma recusa do Zelador mantinha a coluna fora da busca até a sessão ou o modo do grafo mudar. O GameTest montou a mina e a casa do mineiro do outro lado de uma única passagem. Registrou a recusa e confirmou que os nós permaneceram isolados mesmo após secar a passagem.
- **Correção:** o evento de bloco reaproveita a região já monitorada e libera a recusa quando muda o piso, o apoio ou a passagem na coluna ou em uma vizinha direta. A releitura usa a verificação de terreno existente. A regressão também confirma que uma edição acima da passagem preserva a recusa e que trocar o apoio libera a busca enquanto a água ainda impede a conexão.
- **Prova:** a regressão falhou pela recusa antiga. Passaram 16 GameTests focados e as três suites do Sentinela `fast` (`20261121T233603Z`). A causa exata da desconexão registrada no playtest segue sem confirmação.

### Roteiro da mina e da casa — 0.4.117

1. Reinicie o jogo e confira `villagecolony 0.4.117` no `latest.log`. Use um mundo de teste ou uma cópia da vila. Em `/vc grafo modo`, confira o modo `atual`. Ative o mesmo desenho do grafo do playtest anterior.
2. Localize o arco da mina e a casa do mineiro. Confira se ambos têm ligação com a rua e se você consegue caminhar da rua até cada entrada.
3. Alague um trecho da única passagem. Registre se os nós ficam isolados e se o log `VC_ROAD_GRAPH` mostra uma recusa. Retire a água e recoloque o apoio firme. Aguarde a releitura com a vila carregada. A varredura normal começa a cada 6.000 tiques, cerca de cinco minutos a 20 TPS. O arco e a casa devem voltar à rede principal. Se a água ainda bloquear o trecho e não houver desvio, os nós devem continuar isolados.
4. Caminhe até as duas entradas e confira portas, escadas e fundações. Salve e reabra o mundo. Confirme se as conexões aparecem de novo e anote quanto tempo demoraram.
5. Na mesma candidata, repita os passos de tosquia e condução do roteiro do pastor. Use `/vc bioma nota` para anotar resultados. Execute `/vc salvar` antes de salvar e sair, e de novo ao concluir o teste após reabrir.


## Lote A — altura das entradas na candidata 0.4.122

- **Evidência da 0.4.118.** O `VC_ROAD_GRAPH` mostrou três nós isolados em `(351,86,675)`, `(339,82,697)` e `(339,82,699)`. Na cópia do mundo, as três colunas têm caminho no piso inferior (Y79, Y78 e Y78) e tábuas no nível dos nós isolados. O índice de ruas registra cada coluna por XZ.
- **Correção.** Quando a leitura da porta retorna um piso válido, o grafo adota essa altura na coluna, mesmo com uma rua cadastrada acima. Se duas entradas válidas ocuparem a mesma coluna, prevalece a mais baixa. Mantém-se um nível por coluna.
- **Verificação.** O GameTest falhou antes da correção porque o piso térreo ficou isolado. Depois, o nó da entrada integrou a rede principal em duas publicações do grafo. Os 17 GameTests focados passaram na branch da correção.
- **Pendente.** Aplicar o [roteiro da 0.4.122](Playtest-0.4.118-2026-11-22.md) às três colunas e aos casos originais do arco da mina (N12) e da casa do mineiro (N28). A evidência das três colunas ainda não explica os dois casos originais.
