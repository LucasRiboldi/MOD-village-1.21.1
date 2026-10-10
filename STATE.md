# STATE — 2026-10-10

> **JAR atual: `village-colony-0.3.9.jar` = `D2ED5E94…A907754`** em `build/libs/`, `downloads/` (local) e
> `%APPDATA%/.minecraft/mods`. **ADR-036 a 039 na `main`.** Nada da 037, 038 e 039 visto em jogo.
> Pendências: `docs/technical/Decisoes-Pendentes-2026-10-07.md` e a ADR-039.

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto três vezes (2.277 linhas
> em 09-19; 659 em 09-24; 348 em 09-30): o texto antigo foi arquivado lá, sem
> edição. O texto completo de cada correção abaixo está na seção
> "Arquivado do STATE.md em 2026-09-30".

---

## 🟡 10-10 — painel de profissão dentro da moldura

A imagem do playtest mostrou dois overlays separados: o painel pixelado sem o texto e, abaixo dele,
o nameplate Vanilla vermelho (`Fundidor`) fora do background. A causa não estava no log; era a
combinação de `OverlayDrawing` com `WorkerNameplate`: o cliente desenhava a moldura própria, mas o
servidor ainda forçava `setCustomNameVisible(true)` para a profissão.

Correção 0.3.9: `PixelPanelLayout` voltou a montar uma placa única, com texto à esquerda e ícone à
direita dentro da mesma moldura, no estilo do modelo `Carpinteiro` enviado como referência.
`WorkerNameplate` continua armazenando o nome da profissão no aldeão para reconhecimento/fallback,
mas não força mais o nameplate Vanilla visível; saves antigos que já tinham a profissão gravada
também são regravados sem visibilidade quando o ciclo passar pelo trabalhador.

Verificado com regressão primeiro: `PixelPanelLayoutTest` falhou com o layout antigo e
passou depois; `WorkerEquipmentGameTest.colonyProfessionNameIsStoredButNotForcedVisible` falhou
com o nameplate Vanilla ativo e passou depois em `runGametest -PgametestOnly=WorkerEquipmentGameTest`
com 24/24. Depois passaram `test --tests com.villagecolony.client.*`, `build --no-daemon` e
`runGametest --rerun-tasks --no-daemon` **650/650 em 30,80 s**. JAR
`D2ED5E9447C42F49DF2ADB6C3C4BEDFF3E8D3235CB62AC43EB6186F43A907754` copiado para
`downloads/` e `%APPDATA%/.minecraft/mods/`; a 0.3.8 foi arquivada fora da pasta de mods. Falta o
playtest visual no cliente com a 0.3.9.

## 🟡 10-10 — log curto e painel de profissão

Playtest em `latest.log` (22:13:38–22:16:01): 900 linhas; sem erro do overlay. O analisador marcou
6 linhas de `builder_pathing_stalled`, 2 ciclos acima de um tique (pior 388 ms), 3 `ERROR` de mods
de cliente/resource pack e nenhum `VC_TIME`/`VC_COST` (sessão curta ou sem janela suficiente).

Correção 0.3.8: `OverlayDrawing` voltou a usar um layout explícito para o painel pixelado: a moldura
é dimensionada pelo texto, o nome fica dentro do background e o ícone da profissão fica centralizado
acima. `PixelPanelLayoutTest` cobre nome de uma linha, construção de duas linhas e o modo sem texto.
Verificado com `test --tests com.villagecolony.client.*`, `build --no-daemon`,
`runGametest --rerun-tasks --no-daemon` **649/649 em 33,02 s** e JAR
`317647772FDCC555C948578F775BA41B60D9FC916C5381031D68249ECD0944ED` copiado para `downloads/`
e `%APPDATA%/.minecraft/mods/`. **Ainda falta playtest visual** no cliente com a 0.3.8 para
confirmar o desenho em jogo.

## 🟡 09-10 — playtest Spark GqK0MLwvTw e descida do mineiro

Spark `GqK0MLwvTw`: TPS estável em 20; custo do ciclo da colônia saudável (`cost_ledger` ~10,6 ms
médio, p95 ~12,9 ms). O gargalo visto no save foi de fluxo: construtor esperando `stonecutter`,
fundidor sem `iron_ore`/`raw_iron`, e mineiros presos na abertura da mina. O log mostrou o mineiro
com alvo de `raw_iron` recebendo caminhada de `y=46` para `y=34`, segurando o único ramal enquanto
os outros esperavam.

Correção 0.3.7: `MinerApproach.climbableWalkTarget` agora quebra descida longa em patamar seguro,
simétrica ao limite de subida. Regressão nova falhou antes do patch e passou depois. Verificado com
`runGametest --rerun-tasks --no-daemon` **649/649** e `build --no-daemon`. **Ainda falta playtest
no save real** para confirmar queda do tempo ocioso do fundidor e retomada da cadeia do
`stonecutter`.

## 🟡 09-10 — fluxo de profissão, suprimento e caminho (branch local)

Implementadas as melhorias 3, 4 e 6 da pesquisa na branch `codex/fluxo-grafo-waypoints`: varredura
vazia com respiro para o mineiro, grafo explícito de rota de suprimento da obra e waypoint de rua
mais próxima para calçar caminho até lote afastado. Verificado localmente com build de linha de base,
unitários focados, GameTests 648/648 e build final. **Ainda falta playtest no save real.**

## 🟡 07-10, tarde — ADR-039 aplicada (aguarda playtest)

**0.3.6 (segunda rodada da ADR-039):** produto no baú da profissão; adiantamento segue as peças das
casas do bioma (pinheiro, acácia, arenito); mineiro não perde o ofício. `TODO.md` reescrito só com o
aberto (histórico arquivado). `scripts/gametest_battery.py N` para a bateria repetida.

**0.3.5:** as memórias escolhidas (ADR-039 C) vão ao save `villagecolony_memory`: árvores e mudas,
colunas de coleta, veio e rastro, ramais reservados, vez do adiantamento, coletas do pastor.
Provado o vai-e-volta do save (`WorkMemoryGameTest`); **fechar e abrir o mundo ainda não**.

Respostas às pendências (`docs/decisions/ADR-039-*`, tabela "Estado"): E1 espera de 1 a 5 min
depois de busca vazia; E2 animal solto também procria; D1 carpinteiro adianta escada, laje e
cerca, e a tarefa de fabricação faz a peça pedida (o pedreiro fazia tábua no lugar do tijolo
adiantado); D2 caminho até a obra longe da rua; D3 ramal novo rumo ao minério que falta;
F1 pedra como solo (já valia); F2/F3 branches do Codex incorporadas.
**KF-003 com causa achada:** o tique de teste fundia colônias de cenários vizinhos.

**Verificado em 07-10:** `build` ok; **1.361 unitários**; GameTests **644/644** em 6 baterias
seguidas, 0 fusões. **Playtest pedido:** carpinteiro com escada/laje/cerca no baú, pedreiro com
tijolo sem obra pedindo, caminho até obra afastada, mineiro abrindo ramal com minério,
`time_ledger.py` (o `walk` deve aparecer depois do A3).

## 🟡 07-10, tarde — ADR-038 aplicada (aguarda playtest)

Rotinas e trajetos (`docs/decisions/ADR-038-*`): P5 todo baú cheio alivia e adiantamento de 5 em 5
por peça; P3b boca da mina até 5 dentro da vila, no morro e longe da água; P3a memória de árvores
e mudas do lenhador; P3c artesão diante da bancada ou do sino; P2b/P2c pastor coleta animais na
corda e cuida dos amarrados e cercados; P1 ofício parado cede vaga; P2a fundidor adianta vidro e
pedra lisa; P6 mina abre lugar de pé ao lado da pedra emparedada; P7 coleta começa onde já achou.
**Pela metade:** carpinteiro adiantando, construtor calçando caminho, ramais rumo ao minério,
galerias no save.

**Verificado em 07-10:** `clean build` ok; **1.361 unitários**, 0 falhas; 95 Python; PIT acima de
85% (força 92%); GameTests **637/637** nas duas últimas rodadas de cada item.
**Playtest pedido:** pastor trazendo animais (precisa de cerca perto da cama), lenhador sem ir a
muda, mina nova dentro da vila, artesãos na bancada, `time_ledger.py` (ver P4: `walk` = 0%).

## 🟡 07-10 — playtest do 0.3.1 e ADR-037 aplicada (aguarda playtest)

**Playtest de 06-10 23:41 → 07-10 00:43** (Spark `QK8BBjPGXr`: TPS 20, mod 2,7%): as duas vilas
nasceram com 0 árvores (capim barrava o plantio — provado por GameTest); lenhador vivendo de
7 replantios; a mina desceu 5 níveis num segundo; comida 1 ponto por adulto; picos de 100 ms
da busca de lugar para árvore.

**ADR-037, um commit por decisão:** F1–F3 floresta (5 por dezena, árvore natural para a obra,
capim não barra), C1 só `storage_majest`, C2 teto 3×64 também no mineiro, B1 construtor põe de
qualquer lugar da zona da obra, L1 lenhador até borda + 20 sem árvore, M1 cascata da mina
corrigida e mineiro sem ramal raspa a superfície, R1/C7 reunião doa a comida e o trigo conta,
V1 desempregado e bebê seguem o Vanilla (o mod apagava a caminhada deles à noite), V2/C3 porta
fechada ao dormir.

**Verificado em 07-10:** `clean build` ok; **1.355 unitários**, 0 falhas; 95 Python; GameTests
**631/631** nas duas últimas rodadas (em ~20 baterias do dia, KF-003 caiu 2 vezes).
**Não provado por teste:** a exclusão das células do poço na raspagem de superfície; o limite
de 6 blocos das portas.
**Plano para o ócio:** `docs/technical/Plano-Atividades-2026-10-07.md` (P1–P7, espera o autor).
**Playtest pedido:** vila nova (contar árvores na fundação), mina por ≥ 20 min, obra com tora,
noite (portas, desempregados indo para a cama), `/vc log` Felicidade, `time_ledger.py` e
`cost_ledger.py`.

## 🟡 06-10, noite — ADR-036 aplicada (aguarda playtest)

As decisões do autor de 06-10 (`docs/decisions/ADR-036-*`, tabela "Estado" com commit e
verificação de cada uma): 4 baú de cama, 5 Regra 25 desfeita, 6 peça na 4ª tentativa,
8 estoque do pedreiro, **9** teto de 3 compartimentos por item e `storage_majest` sem baú livre,
10 vila abandonada para, 11 foco com 1 minuto, **15** preso vai para a cama, **17** rampa no fundo
da mina, **18** busca até 10 além da borda (centro/borda), **19** eixo/metade/formato da planta,
**20** felicidade decide os filhos, **23** veio inteiro, 24–27 arrumação (docs em `docs/archive/`,
`agent/` apagado, site e JAR fora do git, testes de texto apagados), 28 E43 fechado.

**Verificado em 06-10 (22:51):** `build --rerun-tasks` ok; **1.348 unitários**, 0 falhas (XML);
95 Python; PIT 86% (limiar 85); GameTests **627** — a última rodada de cada item passou, e em ~30
baterias completas da noite caíram testes já intermitentes — KF-003 (escadas) 3 vezes, fundidor
e FarmPlan 1 vez cada; ver `docs/behavioral-tests/known-failures.md`.
**Limiares que o agente escolheu e o autor revisa:** felicidade (item 20) e teto da rampa.
**Playtest obrigatório:** baú cheio → 10 últimos para baú livre e `storage_majest`; aldeão preso
indo para a cama à noite; mina chegando ao fundo; casa com tronco deitado e laje de cima;
`/vc log` com a linha Felicidade; `time_ledger.py` e `cost_ledger.py` com 1 e 4 colônias.

## 🟡 06-10, tarde — ADR-035 aplicada (aguarda playtest)

Os sete pontos da avaliação técnica (`docs/decisions/ADR-035-*`), um commit cada:

1. **Save durante o jogo:** registro copiado em todo `BEFORE_SAVE`; nada depois do fechamento.
2. **Uma linha por vez:** regra em `CLAUDE.md` §0.2.1 / `AGENTS.md`; CI também em `claude/**` e `integra/**`.
3. **Pedido de material (fase 1):** `/vc log` diz o que a obra espera, de onde e por quê. Fase 2 espera playtest.
4. **Decisão no core:** `ProfessionPolicy.searchRadiusOr`, `ReservationGate`.
5. **Custo por fase:** linha `VC_COST` a cada 10 ciclos; `python scripts/cost_ledger.py`.
6. **Comentários:** regra em `CLAUDE.md` §0.5; 4 textos que mentiam corrigidos.
7. **Testes:** `-PgametestOnly=X`, auditoria fora da bateria comum (`-PgametestAudit=only`),
   PIT só em PR/`main`, `runGametestServer` com `/test`, fixture que monta, `GameTestRegistryTest`.

**Verificado em 06-10 (14:54–15:00):** `build --rerun-tasks` ok; 1.338 unitários + 1 propriedade,
0 falhas (XML); 95 Python; **613/613 GameTests em duas rodadas `--rerun-tasks`** (27 s de servidor cada);
auditoria 1/1; PIT 87% (limiar 85). JAR `downloads/village-colony-0.3.0.jar` = `20090AF3…8F95`.
Intermitentes conhecidas: KF-003 (porta e escadas, 1/10 cada).
**Playtest obrigatório:** matar o processo Java e reabrir (save); `/vc log` com obra esperando
peça; `time_ledger.py` e `cost_ledger.py` depois de ≥ 5 min com 1 e com 4 colônias; mina com save
antigo (SHAPE_VERSION 7→8); painéis com e sem Iris; argila no lago.

## 🟡 06-10 — linhas Claude e Codex integradas (aguarda playtest)

Branch `integra/linhas-2026-10-06`: `claude/corrigiveis-sem-jogo` + `codex/village-visuals-logistics-mine-sweep`,
com as decisões da auditoria (`docs/audit/`, ADR-034):

- **D-01 mina:** salões 10x10x3 do Codex (`SHAPE_VERSION 8`); `MineMouthArchTest` (arco do Claude) removido.
- **D-02 varredura:** `VillageSpiralSweep` + `VillageFluidIndex` (ADR-031); do Claude ficam `LocateFallback`
  e a exceção da argila (não pula água — o motor do Codex pulava). `FluidColumns` removido; `CropPatch` e
  `SandGathering` voltam ao comportamento da `main` (sem pular fluidos). **Pendente:** ligar o índice a eles
  exige tirar `VillageFluidIndex` de `fabric/work` (senão fecha ciclo de pacote).
- **D-03 painel:** `OverlayDrawing`/`OverlaySprites` do Claude + `OverlayPreferences` do Codex (esconder texto);
  `PixelPanelLayout`/`WorldPixelPanelRenderer` removidos.
- `closePlan` só em `TreeFelling`; import em `VillageFocusPlayerGameTest`; comentários "seis" → "sete".
- **`MineOverflowStorageGameTest` não estava no `fabric.mod.json` do Codex** — registrado, roda pela 1ª vez.

Verificado em 06-10: `build --rerun-tasks` ok; 1.325 unitários + 1 propriedade, 0 falhas (XML);
92 Python ok; **610/610 GameTests em duas rodadas `--rerun-tasks`** (564 + 24 + 21 + 1). PIT não rodado
localmente. JAR `downloads/village-colony-0.3.0.jar` = `CA54DA54…3128`. **Nada visto em jogo.**
Playtest obrigatório: mina com save antigo (SHAPE_VERSION 7→8), painéis com e sem Iris, argila no lago.

## 🟡 04-10 — painéis, perímetro, mina e varredura (aguarda playtest)

- Vila nova cria até oito árvores maduras no anel de 48–56 blocos, alternando
  as espécies do bioma. Folhas naturais não persistentes podem ceder somente
  dentro da copa gerada; folhas do jogador, blocos de vila, entidades e
  construções continuam protegidos. A descida da mina recupera um degrau
  transitável próximo quando o mineiro é deslocado para fora do corredor sob a
  boca, antes de desistir da tarefa.
- Corrigido o formato de vértices dos overlays: moldura e ícone não enviam
  mais atributos incompatíveis com `RenderLayer.getTextSeeThrough`.
- Construção esperando madeira por 20 passagens solicita ao fazendeiro o
  rebento da espécie; ele só planta após retirar fisicamente a muda de um baú,
  em área segura a 48–56 blocos e alcançável pelo lenhador. Essa retirada e
  plantio agora respeitam o expediente: à noite a muda permanece no baú.
- Aldeões largam navegação e alvos de trabalho fora do horário, preservando
  casa e tarefa. Inventários de baú duplo são tratados como uma unidade.
- Mina subterrânea: dois lances de cinco degraus, salões 10x10x3, corredor 3x3
  e dez tentativas antes de abandonar a boca; formato de save 8.
- Baús das camas vanilla voltam a ser garantidos de forma idempotente em cada
  observação, recuperando chunks ausentes na adoção.
- Painéis nativos de pixel art sobre trabalhadores e canteiros: moldura fina
  com centro transparente, texto dentro dela e ícone centralizado acima. A
  obra usa uma moldura única e responsiva para nome e itens faltantes. O Mod
  Menu alterna o texto da profissão durante a sessão, preservando o ícone. A
  associação usa a profissão e o estado reais do payload, não a posição na
  lista.
- `SiteMarker` agora desenha, com partículas de fogo azul, também o perímetro
  inteiro da caixa atual da vila. A boca da mina virou arco 5x4, dois lampiões
  e passagem central 3x3; a escada e o túnel já eram 3 blocos de largura.
- O caminho de retirada de material do construtor já usa `ColonySupply`, que
  percorre os baús válidos da vila e retira o item físico. A causa observada
  para espera excessiva era falta de estoque, não uma segunda rota ausente.
- O preparo converte somente `grass_block` sob a base planejada de obra não
  agrícola em terra; plantações e os demais pisos ficam intactos.
- A coleta de superfície agora começa na borda da caixa, converge até o centro
  e segue por anéis externos sem reler colunas. Um índice transitório e
  incremental ignora colunas de água/lava já medidas e é invalidado quando a
  caixa cresce (ADR-031).
- Baú profissional cheio libera seus dez slots finais para baús comunitários
  da mesma vila, priorizando um baú vazio. Pilhas e componentes são preservados;
  sem espaço comunitário, o excedente usa um baú físico no salão completo da
  mina, sem carregar chunk ou usar baú de profissão (ADR-033).
- A linha de base voltou a compilar sem ciclos Fabric: foco da vila, leitura e
  orientação de blueprint e adaptação de bioma ficaram em `integration`; eventos
  aplicam a política de moradia a partir do resultado da varredura. A regra de
  arquitetura não encontrou ciclos e o teto de 500 linhas voltou a ser atendido
  após mover o fechamento de corte para `TreeFelling`. A verificação final
  passou em `build` e em 585/585 GameTests.
- Spark `8VskZd9AOD` confirmou 20 TPS e mostrou o mineiro alcançando a frente,
  mas abandonando a coleta porque seu baú estava cheio. O transbordo agora é
  tentado no instante do depósito; sem destino, a tarefa volta à fila em vez de
  concluir com zero itens e o mineiro pode apoiar obra durante o descanso curto.
  Lenhador sem árvore recompõe o viveiro do anel 48–56 antes do apoio, com meta
  `max(10, 5 por lenhador)`.
- Depois de três faltas, terracota colorida não fica presa a uma rota teórica de
  recoloração: a peça preferida entra fisicamente no baú do construtor. A
  terceira tentativa continua sendo necessária e as duas primeiras deixam a
  coleta/fabricação local trabalhar.
- Terracota vermelha agora é peça do pedreiro, não saída direta da fornalha:
  a demanda abre `argila -> terracota` para fundidor e coleta. Todo corante
  Vanilla é ingrediente automático físico no baú de serviço, sem tarefa de
  coleta ou craft. O fundidor passa a antecipar a cadeia da obra após duas
  faltas; o lote já usa o índice incremental da rua e aceita lacunas rasas que
  a fundação pode preencher, sem uma segunda cache concorrente.
- Colunas de terreno cujo chunk estava descarregado deixam de ser perdidas pelo
  índice de água/lava: ficam pendentes e são revisitadas incrementalmente quando
  o chunk carregar. A varredura integral de chunks segue rejeitada por custo;
  o estudo compara cursor, heightmap e índice por eventos.
- O Spark `t80rKW8u6q` mostrou que uma roça sem lote ao alcance deixava a vila
  sem projeto por vinte ciclos, embora houvesse materiais e trabalhadores. O
  recuo agora dura um ciclo: a casa ou oficina seguinte pode abrir, e a roça
  continua proibida fora do alcance do fazendeiro.
- O Spark `G7eI22eQt0` manteve 20 TPS (MSPT mediano 9,93; p95 14), mas mostrou
  `TreeScanner.findNearestLog` em 62% do custo do mod. A busca agora retoma na
  coluna exata e encerra a passagem ao indexar 16 árvores. A carpintaria
  fabrica até cobrir a demanda restante da obra, ainda com uma receita por
  ação; o analisador só chama baú de mineiro cheio diante do aviso real.
- A rodada de 04-10 confirmou 583/583 GameTests. Uma execução anterior teve
  duas falhas opostas no batch concorrente `craft_family`; a repetição imediata
  passou integralmente, portanto a instabilidade ficou registrada para
  investigação, sem reduzir timeout nem enfraquecer a cobertura.
- Roças agora tratam a sua camada física mais baixa como fundação: o construtor
  assenta o bloco pedido sobre a grama natural, sem mudar a altura especial que
  acomoda os canteiros e a água. A matriz auditável de 161 blocos dos templates
  de todas as vilas Vanilla 1.21.1 está em
  `docs/reports/blocos-vilas-vanilla-1.21.1.xlsx`; 67 ainda não têm rota direta
  de uma profissão e estão marcados sem inventar suprimento.
- `barn_majest.nbt` foi retirado do sorteio de obras; ele não pertence mais ao
  pastor nem entra em `VillageStructures.buildableFor`. `storage_majest.nbt`
  permanece no sorteio do construtor. O celeiro segue como arquivo de recurso,
  mas não é uma possibilidade de construção automática.
- Falta o playtest visual e de desempenho no save, inclusive marcador em vila
  grande e coleta atravessando a borda, fluxo real dos baús, nova medição do
  lenhador/carpintaria e o sorteio das duas oficinas em uma vila real.

## 🟡 02-10 — consolidação na `main` (aguarda playtest)

- Mod Menu opcional (ADR-030), overlays no cliente, Regra 48 (`GroundPickup`),
  B-4 parcial e B-5. Playtest pendente: lista no topo do `TODO.md`.

## 🟡 03-10, fim da tarde — playtest da manhã (aguarda playtest)

- Ícones com nomes trocados (era o arquivo), placa com fundo, área da vila em
  partículas, base só de grama, gargalos da obra, mina 3 × 3, água e lava
  marcadas (sem teste). Análise da varredura das bordas em
  `docs/research/2026-10-03-varredura-das-bordas.md`, aguarda decisão.

## 🟡 03-10 — medida da vila, Emenda 8 (aguarda playtest)

- Peças da vila gerada (142 × 116 → 143 × 117), 15 em volta das obras, só o
  lado que passa avança, centro num bloco; rua com 10 caminhos fora. Saves
  antigos são medidos de novo.

## 🟡 03-10, manhã — revisão das profissões (aguarda playtest)

- Regra nova do autor: material não achado em **3 buscas** aparece no baú de
  quem o usa (natureza inclusive); achado no alcance, o aldeão vai buscar.
- Pastor sem ovelha solta a tarefa; carpinteiro lê os mesmos baús da meta;
  arte em pixel desenhada no `LAST`; fundidor sem spam.
- Duas decisões abertas: ociosos sem ofício (ADR-011) e pedreiro ocioso.
  Relatório: `docs/research/2026-10-03-paradas-por-profissao.md`.

## 🟡 03-10, 01:02–01:32 — playtest (Spark `r6nErbWNSL`)

- **Desempenho ok:** TPS 20, MSPT mediano 8–12 ms, o mod é 1,9% do tick do
  servidor. As duas janelas de TPS 13–14 são a recarga de pacote de textura e
  a pausa (01:07–01:09), não o mod. Nenhuma exceção do mod no log.
- **Travamento era de fluxo, na obra:** a casa do pastor (`-292, 65, 386`)
  levou 26 min para 172 blocos. O construtor escolhia o lugar de pé dentro da
  casa fechada e ficava parado fora até o guarda de 300 tiques (6×) ou o
  `sets … aside` de 200 (dezenas). **Corrigido sem jogo:** `UnreachableSpots`
  lê o `CANT_REACH_WALK_TARGET_SINCE` do Vanilla e troca de lado em 1 s.
- **Vidraça sem areia:** a vila disse `has no sand anywhere in the radius` às
  01:05:54 e a barreira só riscou a vidraça às 01:12:02. Resolvido pela regra
  das 3 buscas (a areia aparece), não mais riscando a vidraça.
- Sinais para o próximo jogo: `gives up standing at`, `no place to stand
  within reach can be walked to`; menos `has not moved a block` de construtor.

## Em uma linha

Integração de 06-10 (branch `integra/linhas-2026-10-06`): as duas linhas abaixo
foram juntadas com as decisões D-01..D-05 da auditoria (`docs/audit/`).

**Linha Claude (03-10):**
03-10: corrigíveis sem jogo em `claude/corrigiveis-sem-jogo`, depois o
playtest da madrugada e as duas correções dele — 1.295 unitários, 579/579
GameTests, três mutações mortas. As correções do playtest ainda não foram
vistas em jogo.

**Linha Codex (04-10):**
Entrega de 04-10 publicada no commit `43f13978`: 1.314 testes unitários e
585/585 GameTests; `build` verde. O JAR em `build/libs/`, `downloads/` e
`%APPDATA%/.minecraft/mods/` é
`EEE5A695C00281E129145E924F9DC3A6E63E40227397F90052FF5E7F5C56DD39` nas
três cópias. A branch de desenvolvimento está alinhada ao remoto.
Próximo: fazer o playtest dos painéis, perímetro, mina, varredura, baús e
ritmo de construção no save real.

## 30-09, noite — playtests 18h e 21h (Spark `YUm45D9Sw4`, `qI5h6MXtDA`)

- **E51 (`2895d71`):** a ponta da vila nascia colônia nova com BigHouseMOD e
  7 adultos — `44cd9e5a` tem 7 BigHouseMOD. Análise e o que falta em
  `docs/technical/Identidade-da-Vila-2026-09-30.md`. Não visto em jogo.
- **Caixa da vila (ADR-003 Emenda 6), 01-10:** busca de camas em coluna com
  janela de altura; caixa que cresce (construção/lote +12, rua inclui o bloco);
  identidade e fusão pela caixa; só trabalha com jogador dentro (+5 min). Aberto: limpar o save.
- **E52 (01-10):** `PenEscape` abre o portão (fecha atrás) ou pula a cerca, por rota
  própria. **E47 revisto:** o encalhado larga o ofício e `ClimbOut` sobe por escada,
  pilar ou túnel, sem desistir. Os dois vistos em jogo em 01-10.
- **01-10:** Regra 45 (baús da caixa); Emenda 7 (lote: ½ diagonal + 12, sem teto).
- **Playtest 01-10 23:12–00:17 (Spark `jPsGP2hsPo`):** TPS 20, mod 1,7%. Curral ok; pilar parou sob a grama da vila → `mayDigOut`.
- **Playtest 02-10 00:43–01:04 (Spark `LhqqBh973A`):** TPS 20, mod 1,7%; 2 casas prontas. Piso da obra nunca assentado (proteção da própria obra) → `mayBuildOver`; mineiro preso sob pedregulho → entulho; `/vc log` "travado" antigo → `WorkerStrikes.worked`. Pedidos: golem no curral, base construída, escada de madeira, roça acima da rua. Análise: `docs/research/2026-10-02-travamentos-e-tentativas.md`. **01:36 (Spark `f0wlFJg1kL`):** golens pularam a cerca; 3 mineiros encalhados 7x em y=10 saíam furando a vila → `MineReturn` (volta pelo rastro, casca de 4 blocos). **F-1/F-2 (02-10):** `MineDescent`, `EmptySweeps`, tora de casa no teto, `TaskChain`. Regras 2-e1, 49, 50 e F-3 (02-10).

## Git

- 02-10: PRs #8 e #3 mesclados com aval do autor; `main` e `codex/bighousemod`
  no mesmo commit. Funcionalidade nova: branch a partir da `main`.

## Corrigido e testado, pendente de playtest

Cada item tem teste que falhou antes da correção. Nenhum foi visto em jogo.

| Data | Correção | O que confirmar no save |
|---|---|---|
| 30-09 | Revisão das profissões: lava nunca, carpinteiro titular, viveiro do lenhador, pão, rebanho, drops automáticos, teto 256, ofício Vanilla bloqueado | `baked`, `fed two sheep`, `leaves the vanilla trade`, `lava is never placed`, `drop ingredients appear`; BigHouseMOD nova com 7 camas |
| 30-09 | Rebento não nasce no espaço de obra (viveiro e replantio) | nenhum `planted … sapling` dentro de lote com obra aberta |
| 30-09 | Busca de lote limitada pelo prazo; motivo da ponta recusada | `sweep:` com 1–2 passagens por volta; `no road end … — N motivo`; `Planner turns` e `Colony cycle took` |
| 30-09 | Placa órfã (PR #4) | nenhuma placa sobre lote sem obra; obra aberta com placa |
| 30-09 | Obra anda longe do jogador (PR #6) | `blocks left` caindo entre 64 blocos e a distância de simulação; vila pausa depois dela; `Colony cycle took` com várias vilas |
| 28-09 | Obra pede a próxima peça artesanal; troncos brutos reservados | obra com escadas ou `oak_log` sem ficar sem peça |
| 28-09 | Levantamento de baús em rodadas de 8; baú inalcançável degrada sem travar | ciclo sem 76–122 ms no levantamento; baú descarregado ao lado de baú compartilhado |
| 28-09 | Obra só reservada com ponto de apoio; fundação completa lacunas de 1 camada | fim do `WORK_STALLED` do construtor; trabalhador saindo de ponto preso |
| 27-09 | Boca seca exige degraus em solo firme; vila na água tenta escada selada | mina seca 3x3 e mina aquática |
| 27-09 | Lote validado na orientação final da planta | obra retangular ao lado de lavoura original |
| 27-09 | Construtor mede alcance pela posição física | fim da falsa caminhada a 5,1 blocos |
| 27-09 | Mina esgotada sem boca oposta espera 600 tiques | um único estado de espera por ciclo |
| 27-09 | Viveiro sem ponto livre espera 6.000 tiques | lenhador sem árvore sem custo no Spark |
| 27-09 | Desvio do mineiro mira a perna intermediária | os dois alvos antes abandonados avançam até a galeria |
| 27-09 | Rua fechada abre ramo perpendicular | vila sem ponta acha lote sem invadir rua, lavoura ou estrutura |
| 27-09 | Trabalhador legado sem casa migra para cama livre | aviso sem repetir a cada varredura |
| 27-09 | Construtor na evolução por população; déficit de camas prioriza moradia | vila com mais adultos que camas; mensagem do `/vc log` |
| 27-09 | Mineiro volta depois de cair dois blocos (pedregulho do baú) | retorno num poço real |
| 27-09 | `WaitingWork` conserva a obra com rota no bioma; `BuilderApproach` escolhe ponto livre | retomada de obra já marcada como abandonada |
| 25–26-09 | Oito correções mais antigas, ainda sem playtest | `Historico-2026-09.md`, "Arquivado do STATE.md em 2026-10-02" |

## O que o próximo jogo precisa mostrar

O roteiro completo está em [`docs/archive/technical/proxima-sessao-2026-10-04.md`](docs/archive/technical/proxima-sessao-2026-10-04.md).
Em ordem:

| # | Item | Sinal no log |
|---|---|---|
| 1 | Perfil de desempenho (spark) | link do `/spark profiler stop`; ver `docs/technical/Profiling-spark.md`. O de 30-09 (`hUQeDXo9U6`) não foi lido: o ambiente remoto bloqueia `lucko.me` |
| 2 | Colônia presente e ciclo mais leve | `Planner turns`, **menos** `Colony cycle took` |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |
| 8 | Bosque fundacional | duas árvores maduras distintas a 48–56 blocos |
| 9 | Placa órfã e obra longe (PR #4 e #6) | ver a tabela acima |

Depois de jogar: `python scripts/analyze_village_log.py` (assinaturas) e
`python scripts/time_ledger.py` (tempo por profissão — Regra 50, critério de toda verificação).

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`.

- **R1, desempenho.** Prazo de 15 ms e cota ajustável no código; falta medir
  em jogo, agora também com a execução em toda colônia `ACTIVE`. É o único
  critério da avaliação com nota 1.
- **`surface_worker_unreachable`** (7 no log de 28-09): reproduzir em
  GameTest antes de mudar a coleta.
- **Estado global (R2).** `ServerMemory` garante a limpeza, mas os 89 campos
  estáticos mutáveis continuam; consolidá-los é o que falta para o C05.
- **Itens 9 e 10 (ciclo de tarefa comum aos ofícios; regras de decisão
  para o `core`).** Pedem ADR antes do código.
- **ADR-025 fase 3** (veios por valor), depois do playtest das fases 1 e 2.
  Ver `docs/research/2026-09-25-mineiro-autonomo.md` §8-§11.
- **Decisões do autor** (ver `TODO.md`, "Decisões que faltam", e
  `docs/research/2026-09-25-decisoes-simples.md`): E43, TASK-048, TASK-044
  (fusão; a ADR-007 está aceita e não implementada), TASK-046, E38, E45.
- **Sessão longa de 26-09** (`docs/research/2026-09-26-sessao-longa.md`):
  lote que não cresce (§7.1; o motivo da rua sai no próximo log), obra
  largada sem blocos prendendo o lote (decisão) e aldeão ocioso preso.
- **Mineiro que não entrega (E44/E45)** e **segunda obra que não abre:**
  sem playtest novo desde as correções; detalhe no `Historico`.

## Dívida conhecida

- **Nenhum arquivo de produção acima de 500 linhas** desde 02-10 (os oito
  foram divididos); a lista congelada do `FileSizeRuleTest` está vazia.
- **Hooks do Claude Code:** os scripts estão em `scripts/hooks/`; quem liga
  no `.claude/settings.json` é o autor.
- **Bateria de jogo:** 3 testes intermitentes isolados em 24-09; a taxa
  histórica era ~1 falha a cada 8 rodadas. `runGametest` não filtra teste.
  O timeout isolado do construtor alcançando o topo (27-09) não se repetiu.
- **`ColonyDetectionGameTest`:** a falha de 09-19 (24 trabalhadores em vez
  de 30) nunca foi reproduzida.
- **Cobertura da bateria de jogo** medida desde 02-10 (`runGametest` →
  `build/reports/jacoco/gametest`): `fabric/work` 83%, `integration` 84%;
  furos em `command` (3%), `network` (20%) e cliente (0%, sem cliente no teste).
- **GameTest com jogador** desde 02-10 (`FakePlayer` no mundo): o foco e a
  passagem extra da busca de lote já são testados com jogador dentro e longe.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/METODOLOGIA.md`, rodada
  por `python scripts/assess/assess_project.py --run --gametest-runs 2`. A
  última deu B, 3,21 de 4.
- **Instrumentar antes de consertar.** Três defeitos de 09-19 se decidiram
  numa única leitura depois de instrumentados.
- **Quando a mesma causa reaparece em vários itens, desconfie da
  ferramenta.**
- **Ferramentas:**

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest` | onde cada cadeia de produção começa |
  | `StructureCoverageGameTest` | quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
