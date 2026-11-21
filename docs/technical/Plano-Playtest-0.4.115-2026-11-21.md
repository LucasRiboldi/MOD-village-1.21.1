# Plano de correção — jogo da 0.4.115 em 21-11-2026

Status: triagem concluída; 23 pontos ainda abertos. Este documento separa correções não implementadas de correções implementadas, mas ainda não confirmadas em jogo.

## Atualização de estado — 2026-11-21

O `latest.log` mais recente continua carregando a versão **0.4.115**. Portanto, ele não valida nem invalida as candidatas 0.4.116 e 0.4.117.

### Ainda sem correção implementada — 18 pontos

- **P1:** N07, N08, N09, N17, N22 e N25.
- **P2:** N10, N11, N13, N14, N15, N16, N18, N19, N20, N21 e N24.
- **P3:** N23.

### Código corrigido, aguardando confirmação em jogo — 5 pontos

- **0.4.116:** N05, N26 e N27.
- **0.4.117:** N12 e N28.

Evidência decisiva do log 0.4.115: `VC_ROAD_GRAPH isolated=5`, `loose=door@29,63,-31`, `VC_STATE` do Pastor com `BLOCKED=53` e Mineiro sem evento claro `MINED`, `CUT` ou `BREAK`. Não atribuir o corte do arco ao Mineiro sem autoria registrada.

Próxima validação: jogar com 0.4.117 e recolher novo `latest.log`. Próxima correção de código: N07 + N08, integridade e acesso ao arco da mina.

## Evidência e limites

- `C:/Users/lucas/AppData/Roaming/.minecraft/logs/latest.log`: linha 95 confirma `villagecolony 0.4.115`; 28 linhas `VC_BIOME_NOTE`, entre 14:26:54 e 15:17:21. Encerramento normal às 15:21:50. Cenário: `Novo mundo`, planície, colônia `c5cd8b44`.
- Diário explicitamente selecionado: `logs/villagecolony-actions-20261121-142458.jsonl` da mesma instalação.
- Imagens do autor: `C:/Users/lucas/Desktop/2.jpg` até `7.jpg`, comparadas pelos nomes originais abaixo. Os arquivos continuam fora do Git; as descrições permitem retomar a triagem sem os anexos.
- [Relatório único gerado](../testing/relatorios/jogo-0.4.115-2026-11-21.md), por `scripts/game_report.py`, com log e diário explícitos. Também executado `scripts/cost_ledger.py` sobre o mesmo log.
- Esta worktree está na base 0.4.110. A worktree `C:/Users/lucas/Desktop/MOD-village-1.21.1` está em `fix/miolo-pelas-pecas`, `ae2ed2ab`, com `mod_version=0.4.115`. Não aplicar correções de runtime nesta base antiga nem sobrescrever o JAR atual. Revalidar Git/PRs e coordenar sobreposição antes da implementação.
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
| N14 · 14:44:00 · 2706 · P2 | “bau ... depois ... nasceu”; “placa ... apontando para a porta”; 5.jpg | Separar investigação do atraso de criação do baú e correção da orientação da placa. Testar momento de colocação após atribuição da casa e placa legível voltada para espaço livre em direção à porta, sem ocupar cama/passagem. |
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

## Escolha para a próxima etapa

**Opção 1 — acesso primeiro (recomendada).** Evidência: N08/N09/N25/N28, imagens 2/4/6 e nós isolados. Investigar e corrigir o Lote A, mantendo todos os outros itens na fila. Verificação: GameTests de transição de altura e conectividade após mudança/recarga, seguidos de playtest. Risco médio: terreno, proteção e grafo interagem.

**Opção 2 — pastor primeiro.** Evidência: N05/N26/N27, imagem 7 e 70% caminhada. Investigar e corrigir o Lote B. Verificação: GameTests de alcance de tosquia e animal atravessando porteira em terreno irregular. Risco médio: navegação e comportamento do animal.

As duas opções preservam o inventário completo; escolher uma determina a primeira intervenção, não descarta as outras. Nenhuma autoriza implicitamente as decisões arquiteturais ou a exceção temporária de profissão.
