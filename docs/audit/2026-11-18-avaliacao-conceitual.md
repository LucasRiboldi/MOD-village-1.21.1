# Avaliação conceitual do Village Colony — 2026-11-18

Pedido do autor em 2026-11-18:
- buscar alternativas e soluções para os pontos abertos, com pesquisa em outros mods, jogos e projetos;
- fazer uma avaliação conceitual do projeto, para pesar as correções desses pontos e de outras regras e decisões;
- fazer um estudo à parte do grafo.

**Só avaliação: nenhum código foi alterado.** A escolha das correções é do autor (`CLAUDE.md` §0.2.2).

- **Base:** `fix/jogo-0490` em `3f164a3f` (0.4.92, 314 regras, 56 ADRs, cerca de 66 mil linhas em `src/main`, 1.187 GameTests).
- **Documentos lidos:** a avaliação ISO de 13-11 (`2026-11-13-avaliacao-qualidade.md`) e a reconciliação de regras
  (`RULE-RECONCILIATION.md`).
- **Logs:** dos jogos da 0.4.85 à 0.4.91.
- **Estudo do grafo:** [`docs/research/2026-11-18-estudo-do-grafo.md`](../research/2026-11-18-estudo-do-grafo.md).

Marcas: **CONFIRMADO** (visto no código, no log ou no git), **PROVÁVEL** (mecanismo visto, efeito não medido), **SUSPEITO**.

## 1. O conceito, em seis promessas

Do `CLAUDE.md` e das ADRs, o mod promete:

| # | Promessa | Fonte |
|---|---|---|
| P1 | A vila trabalha, produz e constrói sozinha; o jogador acha a vila e vai embora | `CLAUDE.md` §0 |
| P2 | O mundo é a fonte da verdade; nada surge sem origem física, salvo exceção declarada | ADR-028, ADR-034, Regra 111 |
| P3 | Movimento natural: sem teleporte, sem pilar, sem escada fixa | E47, decisão de 21-10 |
| P4 | Cada ofício faz o seu trabalho; nenhum faz o do outro (regra temporária de 02-11) | `RULES.md`, topo |
| P5 | Os aldeões decidem e constroem a rede de ruas | ADR-043, ADR-048 |
| P6 | Ócio + bloqueio + encalhe ≤ 15% por ofício (Regra 51), e menos de 40% parado como meta | `CLAUDE.md` §0.4 |

## 2. Onde as promessas se chocam

### 2.1 P1 × a vila só trabalha com o jogador perto (CONFIRMADO)

`VillageFocus.PERIMETER = 48`: a vila só trabalha com o jogador dentro da caixa ou a até 48 blocos (Regra 62). Fora disso o
chunk descarrega e a vila para. "O jogador vai embora" hoje quer dizer "a vila congela".

**Opções:**
- **A.** Recuperar o tempo perdido na volta: ao recarregar, simular em resumo o tempo fora, por exemplo produção por taxa
  medida e obras com uma quota de blocos. Não carrega chunk, mas pede um modelo de taxa por ofício.
- **B.** Manter carregada a área da vila (forceload). É simples, mas custa tique o tempo todo.
- **C.** Reescrever a promessa: "a vila trabalha enquanto você está por perto". É honesto e sem custo.
- **Recomendo C já** (README e guia) **e A como meta da 1.0**. O MineColonies também congela a colônia descarregada.

### 2.2 P2 × P1: a economia física não fecha sozinha (CONFIRMADO)

`SupplyException` já tem 18 exceções declaradas, por exemplo:
- argila na hora pedida (301);
- semente que surge (289);
- muda do bosque (271);
- corante (272);
- peça que falta (51);
- pó de osso (310).

Cada uma nasceu de um jogo em que uma cadeia de produção não fechou. As exceções não têm teto nem medida de custo comum. No
jogo da 0.4.90, a areia surgiu 127 vezes e a argila 374. A Regra 308 conteve o laço.

O padrão: a vila **reage à falta**. Quatro buscas vazias levam a surgir; o britador só abre pedido depois da falta. Ela não
**planeja o estoque**.

**Opções:**
- **A. Previsão de estoque geral (recomendada).** A Regra 311 fez isso para a areia: calcular a lista de materiais recursiva
  (`CraftingLookup`) da obra aberta e da próxima, e abrir antes a coleta ou a fabricação do que falta. É o "estoque mínimo"
  do armazém do MineColonies, onde o pedido passa por baú, armazém, artesão e coletor nessa ordem.
- **B. Uma só porta de exceção:** um mecanismo único de "surgir", com espera crescente e teto por dia e por colônia, no lugar
  de uma classe por item (`ClayOnRequest`, `LargeFarmSeeds`, `BoneMealRound`...).
- **C. Fonte física garantida por bioma:** toda vila nova ganha a fonte do que o bioma não dá, por exemplo uma barreira de
  argila ou um poço de areia como estrutura. É o que o britador já é para a areia.
- **Recomendo A + B:** A tira a maior parte das faltas, B põe teto no resto.

### 2.3 P4 × P6: ofício isolado não chega a 15% de ócio (PROVÁVEL)

Com um aldeão por ofício (Regra 298) e nenhum ajudando o outro (regra temporária de 02-11, há 16 dias "temporária"), o tempo
parado é sobretudo **falta de demanda** ou **espera de insumo**, e não comportamento errado. O ferreiro sem minério e o
pastor com curral cheio não têm o que fazer no próprio ofício.

Medidas:
- 0.4.85: lenhador 46%, construtor 42%, ferreiro 29% parados;
- 0.4.88: mineiro 39%, construtor 32%.

**Opções:**
- **A.** Separar no `time_ledger` o parado "sem demanda" (`NO_TASK`) do parado "bloqueado" (`NO_PATH`, `NO_RESOURCE`). A
  Regra 51 passaria a contar só o bloqueado e o encalhe; o "sem demanda" vira sinal para a economia (§2.2), não defeito de
  ofício.
- **B.** Tornar permanente a regra de 02-11, com as exceções 215 e 220 escritas como regra, e baixar a meta para o que um
  ofício isolado alcança.
- **C.** Voltar a permitir ajuda entre ofícios por demanda, como a "mão de obra comum" do MineColonies.
- **Recomendo A**, porque decide B ou C com número, e não com impressão.

### 2.4 P3 × a realidade do terreno: 21 classes de fuga (CONFIRMADO)

Existem 21 classes para tirar aldeão preso:
- escada e túnel: `ClimbOut`, `ClimbOutward`, `ClimbReshape`, `ClimbTerrain`, `ClimbPolicy`;
- fuga e encalhe: `StrandedEscape`, `StrandedWorkers`, `StrandedNotice`, `EscapeBackfill`;
- mina: `MineReturn`, `MineDescent`;
- teleporte e volta para casa: `BedTeleport`, `NightHome`, `HomeMove`;
- cercados, telhado e água: `PenEscape`, `PenLift`, `FencedIn`, `RoofDescent`, `WaterRescue`;
- obra e trabalho parados: `StuckPieces`, `WorkStall`.

Elas resgatam **depois** do encalhe, porque a navegação Vanilla não planeja cavar nem pôr bloco. Cada resgate novo pede outro.
A Regra 312, de ontem, é a mais recente. O teleporte voltou como último recurso (ADR-036 item 15).

**Opções:**
- **A. Prevenir em vez de resgatar (recomendada para o mineiro).** Um planejador com custo explícito de quebrar e pôr bloco,
  como o Baritone: A* que inclui cavar, subir em pilar e fazer ponte, cada um com o seu custo. O mineiro escolhe um trajeto
  que sai da mina antes de entrar.
- **B. Uma máquina de estados única de encalhe:** detectar, tentar o retorno pelo rastro, cavar e subir, ir para a cama, cada
  passo com prazo, numa classe só. Substitui as 21 entradas.
- **C.** Aceitar o teleporte como regra de jogo, como o MineColonies, e apagar a maior parte das fugas.
- **Recomendo B agora** (consolidação) **e A para o mineiro**, onde o encalhe mais acontece.

### 2.5 P5: o grafo (ver o estudo à parte)

Resumo:
- **A ideia está certa:** a vila decide e os aldeões calçam.
- **A origem está errada:** o grafo afina os blocos de caminho que ele mesmo manda pôr e apagar. É um laço fechado e instável:
  202 mudanças em 2 h 27, `isolated=15`, `offLine=2033`.
- **O `PathTrim` apaga a rua da vila gerada,** contra a ADR-043 item 6 e a Regra 3.
- **Recomendação do estudo:** cortar o laço agora (importação única, histerese, `PathTrim` só no que a colônia pôs) e partir
  depois para "plano primeiro": a rede nasce das entradas, ligada por A* com custo de terreno, como no GDMC e nos mods de
  estrada.

## 3. Como as regras nascem: o problema de processo

**Medido no `RULES.md` e no git:**
- **Ritmo:** da Regra 200 (11-05) à 314 (11-17) são 114 regras em 12 dias, quase 10 por dia; 401 commits desde 01-11.
- **Revisões rápidas:** 35 menções de "muda a Regra", "emenda" ou "revoga", e algumas no mesmo dia:
  - entrada da porta: 303 (16-11), depois 305 (17-11), depois 313 (17-11);
  - trava do grafo: 225, depois 306;
  - grafo sobre a água: 174, revogada pela 264.
- **Sem confirmação em jogo:** 98 regras marcadas "Candidata" e **nenhuma** marcada como confirmada no `RULES.md`. A avaliação
  de 13-11 contou 57 versões candidatas.
- **Jogos curtos demais:** o da 0.4.91 durou 6 min e não gerou nenhuma linha `VC_TIME`.

**Leitura:** cada sintoma de jogo vira uma regra local, com teste que prova o mecanismo, mas sem a medida que prova o efeito.
Como o efeito não é medido, o próximo jogo mostra outro sintoma no mesmo sistema, e nasce outra regra. O grafo (cerca de 50
regras) e a fuga (21 classes) são os dois casos mais visíveis.

**Opções:**
- **A. Teste de longa duração automático (recomendada).** `biome_sim.py` e `/vc bioma` já existem (memória "Bancada de
  simulação por bioma"). Um servidor sem jogador, com o foco da vila forçado, roda 3 dias de jogo em 3 sementes fixas e grava
  `VC_TIME`, `VC_SUPPLY`, `VC_ROAD_GRAPH` e `VC_COST`. A regra só passa de "candidata" a "confirmada" quando o número
  melhora. Tira o playtest manual do caminho crítico.
- **B. Contrato por subsistema:** um documento curto de invariantes por sistema (entrada da porta, grafo, fuga, economia),
  com desenho, que vira teste de propriedade. Regra nova só emenda o contrato. O teste da Regra 313 sobre 14 casas da
  planície é o modelo: estender a todas as casas de todos os biomas.
- **C. Estado de confirmação no `RULES.md`:** a coluna "confirmada em jogo" (já no `TODO.md`) e um teto de regras candidatas
  abertas por sistema; acima do teto, só corrige com medida.
- **Recomendo A + B.** C sai de graça com A.

## 4. Outras decisões a rever

| Decisão | Situação | Proposta |
|---|---|---|
| Regra temporária de 02-11 (nenhum ofício faz o do outro) | 16 dias "temporária", com 2 exceções | Decidir com a medida de §2.3 A: tornar permanente ou retirar |
| Teto de 500 linhas por arquivo | Força cortes por linha: na Regra 310 comentários do `FarmerWork` foram enxugados para caber | Teto por responsabilidade, com a exceção justificada que o `AGENTS.md` já permite; `F1` (subpacotes) segue vetado pelo autor |
| `PathTrim` sem conferir origem | Apaga rua da vila gerada (estudo do grafo, §3.4) | Só o que a colônia calçou |
| Trava do grafo em dias de jogo | Quase nunca dispara num playtest | Estabilidade por evento ou por releitura (estudo, §6 B) |
| Ponta cresce 1 bloco por dia | Cresce sem necessidade e oscila com o esqueleto | Crescer quando a busca de lote falha naquele lado |
| Exceções de suprimento sem teto | 18 e crescendo | §2.2 B |
| C-12: "a regra está certa" (linha 15) × "a tabela está errada" (linha 463) | Aberta | Uma frase: "a intenção do autor vale; a tabela e o código se corrigem para ela" |
| C-15: corpo das regras em `Project-State §18` | Aberta (linhas 19 e 461); o `Project-State` é histórico | Tirar a remissão; o corpo já está em cada linha da tabela |
| C-13: ponteiros para classe inexistente | Em parte corrigida (`ColonyHut` aposentada, `LotLevel.flatGroundAt`) | Rodar `rule_coverage.py` com verificação de símbolo |
| ADR-024 no `TODO.md` | **Item velho:** a ADR já foi retirada em 13-11 e o código saiu | Fechar o item do `TODO.md` |

## 5. Os pontos abertos, um a um

Para cada ponto: a causa provável, de duas a três opções e a recomendação. Nada implementado.

### 5.1 Lenhador 46% parado (`has not moved a block` 18×, jogo da 0.4.85)

- **Causa provável:** o guarda de imobilidade conta como parado o tempo de cortar, como o fazendeiro antes do E36 (memória
  "Lenhador expulso pelo guarda"), ou o lugar de pé da árvore está fora de alcance.
- **Opções:**
  - **A.** O tempo de corte zera o `WorkStall`, como no ramo de trabalho do fazendeiro.
  - **B.** Lugar de pé calculado por árvore, como o `FarmerStand`.
  - **C.** O lenhador do MineColonies: área própria, derruba o tronco inteiro de baixo e recolhe o que caiu num raio.
- **Recomendo:** diagnosticar com `action_report` no próximo jogo longo, depois A ou B.

### 5.2 Degrau de 2 blocos no meio da vila e nivelar ao longo do grafo (ponto 2 do jogo da 0.4.89)

- **Causa:** a suavização da Regra 297 só roda em volta de obra nova; a da geração (Regras 296 e 305) só em vila nova.
- **Opções:**
  - **A.** O pedreiro ocioso aplica a `SiteSmoothing` numa faixa de 3 ao longo de cada aresta calçada, uma janela por vez. É a
    escolha C do autor de 15-11.
  - **B.** Fila de terraplanagem com orçamento por dia, alimentada por `VC_SITE_EDGE` com `maxStep` maior que 1.
- **Recomendo A,** já escolhida, depois do corte do laço do grafo (estudo, §6 B), para não nivelar uma rua que vai mudar.

### 5.3 Plantação grande: arar a estrutura inteira e semear variado

- **Opções:**
  - **A.** Ler do molde da peça as colunas de terra arada e semear pela semente mais escassa (já existe em `scarcestSeed`).
  - **B.** Planta fixa por bioma, com a mistura de sementes declarada no molde.
- **Recomendo A.**

### 5.4 Arco da mina com piso de pedra de raio 6 e baú; casa do mineiro com luz e telhado medieval

- É conteúdo (molde `.nbt` e posição), sem lógica nova.
- **Opções:** A. editar `mine_entrance.nbt` e o molde da casa; B. gerar o piso por código em volta do arco (raio 6),
  respeitando lote e caminho.
- **Recomendo A** para o telhado e a luz, **B** para o piso, que depende do terreno.

### 5.5 Blocos que surgiram sem fabricação (tora ×18, pedregulho ×6, jogo da 0.4.90)

- **Causa:** `MISSING_PIECE` na quarta busca vazia; o lenhador e o mineiro não receberam o pedido a tempo.
- **Recomendo §2.2 A:** a previsão de estoque abre a coleta de tora e de pedregulho antes, como a Regra 311 faz com a areia.

### 5.6 Meta de 40% parado por ofício, em dois biomas, dois jogos seguidos

- Depende de medida, não de mais uma regra.
- **Recomendo §3 A** (teste de longa duração em sementes fixas, planície e deserto) **com §2.3 A** (parado por causa).

### 5.7 Save: cópia de segurança e leitura de versões publicadas

- **Opções:**
  - **A.** Gravar em arquivo temporário, depois renomear (atômico), guardando o anterior como `.bak`, como o `level.dat_old`
    do Vanilla. Na leitura com erro, ler o `.bak` e **não** gravar o estado vazio (memória "PersistentState engole exceção").
  - **B.** Arquivos `.dat` reais da `v0.4.74-alpha` e da `v0.4.90-alpha` em `src/test/resources`, lidos num teste de unidade
    (ida e volta do codec), sem precisar reabrir o servidor.
- **Recomendo A + B.**

### 5.8 Ciclo da colônia num tique só (até 93 ms com 1 vila, 458 ms com 4+)

- **Opções:**
  - **A.** Máquina de fases por colônia, com orçamento de tempo por tique (por exemplo 5 ms) e retomada. O `RingSweep` já é
    assim para a varredura.
  - **B.** Revezar as colônias: uma por tique.
- **Recomendo A.** B sozinho não resolve 1 vila de 93 ms.

### 5.9 GameTests instáveis (pastor, curral da taiga e vizinhos)

- **Causa** (memórias "Interferência entre gametests" e "Aldeão de teste invade o vizinho"): estado global estático e
  entidades vivas em arenas vizinhas do mesmo mundo.
- **Opções:**
  - **A.** Teste com aldeão ou animal vivo em `batchId` próprio e arena longe (o padrão das coordenadas de milhões já usado),
    com IA desligada quando o teste não precisa dela.
  - **B.** Limpeza de estado global por teste, via `ServerMemory`, num auxiliar comum.
  - **C.** Repetição só para a lista de instáveis declarados: conferir se o `@GameTest` da 1.21.1 tem `maxAttempts` e
    `requiredSuccesses`.
- **Recomendo A + B.** C só como quarentena declarada, nunca como padrão.

### 5.10 Segunda frente de mina (ADR-055 item 12)

- **Opções:**
  - **A.** Com 3 ou mais mineiros, um nível novo três abaixo pela mesma escada.
  - **B.** Frente exclusiva por mineiro no mesmo nível, com reserva de bloco (dá conta do "dois mineiros no mesmo túnel").
  - **C.** Pedreira de superfície como segunda frente (já existe no deserto, Regra 236).
- **Recomendo B antes de A:** resolve o defeito que existe hoje com 2 mineiros, e A vem depois, com a ADR que o autor pediu.

### 5.11 ADR-024 e os conflitos C-12, C-13 e C-15

Ver §4.
- **ADR-024:** só fechar o item do `TODO.md`, porque a ADR já foi retirada.
- **C-12 e C-15:** uma frase e uma remissão a tirar.
- **C-13:** conferência automática de símbolo.

## 6. Ordem sugerida, do que destrava mais para o que destrava menos

| # | O quê | Por quê | Tamanho |
|---|---|---|---|
| 1 | Teste de longa duração em sementes fixas (§3 A) e parado por causa (§2.3 A) | Toda decisão seguinte passa a ter número | M |
| 2 | Cortar o laço do grafo (estudo, §6 B e passo 1 da §9) | Para a oscilação e para de apagar a rua da vila | M |
| 3 | Previsão de estoque geral e porta única de exceção (§2.2 A + B) | Tira a maior parte do "surgir" e da espera | G |
| 4 | Save atômico com `.bak` e teste de `.dat` publicado (§5.7) | Bloqueia a 1.0 | P/M |
| 5 | Máquina única de encalhe (§2.4 B) | Substitui 21 entradas por uma | G |
| 6 | Ciclo da colônia com orçamento por tique (§5.8) | Bloqueia a 1.0 | M |
| 7 | ADR do grafo "plano primeiro" (estudo, §6 A) | Destino do grafo | G |
| 8 | Conteúdo: arco, casa do mineiro, plantação (§5.3, §5.4) | Visual, sem risco | P |

## Fontes externas

Além das listadas no estudo do grafo:
- [MineColonies, sistema de pedidos](https://minecolonies.com/wiki/systems/request);
- [MineColonies, mensageiro](https://minecolonies.com/wiki/workers/courier);
- [MineColonies, britador](https://minecolonies.com/wiki/buildings/crusher);
- [MineColonies, caminhos e aldeão preso](https://minecolonies.com/wiki/systems/pathing);
- [Pathfinding Edition for MineColonies](https://modrinth.com/mod/UFGyxCZm);
- [Baritone, recursos](https://raw.githubusercontent.com/cabaletta/baritone/1.19.4/FEATURES.md);
- [Foundation, ruas que nascem do passo](https://pcgamer.com/foundation-is-a-freeform-medieval-city-builder-with-no-grids-to-follow);
- [GDMC](https://cog2025.inesc-id.pt/gdmc/).
