# TODO

**Atualizado:** 2026-09-22, suprimento de construcao sem rota no bioma.

**Auditoria técnica:** [`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md).
Nesta sessao: uma obra que pede uma peca sem rota fisica no bioma recebe a peca
final no bau que atende o construtor. A verificacao percorre receitas Vanilla;
alternativas e cadeias locais, como `argila -> fornalha -> terracota`, continuam
sob responsabilidade dos oficios. A rodada atual de `runGametest` executou 410
testes, 408 aprovados; restam duas falhas independentes: alternancia de
`FarmPlanGameTest` e a fixture de terra de `SurfaceGatheringGameTest`. Os 76
testes Python da auditoria passaram nesta sessao.

## Fila operacional 2026-09-22

O relatorio atual, incluindo responsabilidades, fluxo de obra, suprimento por
bioma e estatistica do `latest.log`, esta em
[`docs/technical/Operational-Status-2026-09-22.md`](docs/technical/Operational-Status-2026-09-22.md).
Ordem canonica desta sessao: P0 fechar os GameTests residuais; P1 reproduzir as
repeticoes medidas de mina/obra/atribuicao; P2 interpretar varredura e reduzir
complexidade; P3 endurance. Nao tratar contagem de log como defeito confirmado
sem contexto e regressao automatizada.

## Próximas atividades corrigíveis sem acessar o jogo

Esta fila separa falhas reproduzíveis ou coberturas que podem ser tratadas
com código e testes locais das validações que continuam dependendo de um save.

- [x] **Criador encerrado:** `SHEPHERD` assume vagas, fundação, tarefas, nome e baú do antigo `BREEDER`; saves antigos são migrados na leitura. Unitarios e GameTests de nome, bau, fundacao e trabalho do Pastor passaram.
- [x] **Cadeia da terracota da obra:** a peca exata continua preferida, mas a tag Vanilla de terracotas pode substitui-la; `CLAY` alimenta a fornalha para terracota neutra. O `CARPENTER` ainda fabrica o fermentador pela receita Vanilla quando houver haste e pedregulho; sem rota local para a haste, a politica de suprimento da obra entrega o fermentador final. Os tres GameTests de substituicao, cadeia de argila e bolas de argila falharam antes da correcao e passam depois.
- [x] **Suprimento de obra sem rota no bioma:** a construcao consulta sua familia de alternativas e a arvore de receitas Vanilla. Se nenhuma rota local existir, a peca preferida aparece no bau da obra quando demandada; se qualquer rota existir, ela permanece tarefa dos oficios. `BuilderGameTest` cobre fermentador sem haste de blaze e porta de carvalho em planicie.
- [x] **Inventario por bioma das plantas construtiveis:** `ConstructionSupplyAuditGameTest` le todos os 143 NBTs permitidos e registra, por estilo, as estruturas, recursos por rota local, itens automaticos e blocos formados no local. O resultado versionado esta em `docs/technical/Auditoria-2026-09-22-Suprimento-Estruturas-Vanilla.md`.
- [ ] Playtest da migração: abrir save antigo, confirmar Pastor sobre a cabeça, tesoura no baú e continuidade das tarefas; testar Tocha das Almas dentro de uma obra real e observar liberação da fila, sem esperar demolição.
- [x] **P0.8/P1.2 — pegada da `BigHouseMOD` reservada:** a busca e a retomada recusam qualquer lote que use uma coluna horizontal da fundacao, mesmo em outra altura ou com projeto pendente. `BuildSiteGameTest.noProfessionLotCanUseTheBigHouseFootprint` falhou antes da correcao e passou depois.
- [x] **P0.8/P1.2 — BigHouse fundacional nao vira reparo profissional:** o save real tinha projetos `big_house_mod` na mesma origem de casas concluidas. O reparador ignora essa estrutura exclusiva, e a retomada descarta o reparo antigo antes de tocar no terreno. Dois GameTests falharam antes da correcao e passaram depois; um terceiro preserva o reparo de casas profissionais.
- [ ] Playtest da pegada e do reparo: reabrir o mesmo save e confirmar que o projeto `big_house_mod` antigo desaparece, a casa permanece intacta e o proximo lote profissional nao usa nenhuma coluna de sua pegada. Nao cancelar manualmente a fundacao.

- [x] 🔴 **P0.9 — catálogo e terreno seguro:** as construções do mod agora usam somente a whitelist explícita de ids Vanilla reais por bioma; areia e relva são coletadas pelo fundidor quando uma obra pede o recurso, enquanto terra comum virou solo do fazendeiro com raio protegido ampliado; bocas de mina rejeitam água, exigem entrada livre e preferem a posição seca mais distante/elevada. `VillageStructuresGameTest`, `SurfaceGatheringGameTest` e `MinerGameTest` passaram.
- [x] 🔴 **P0.10 — reparo cíclico de obras:** depois de concluir, abandonar ou liberar uma obra parada, o planejador varre construções registradas pelo mod, recompõe a planta a partir dos blocos que ainda existem e tenta fechar a incompleta antes de abrir outra. Uma tentativa sem avanço cede uma passagem e é repetida depois, sem travar a vila. `ConstructionProjectTest` e `BuildingRegistryTest` passaram.
- [x] 🟠 **P0.11 — reserva de árvores da vila:** agricultor e lenhador compartilham o viveiro do bioma, plantam em terra enraizada no anel distante de 48 a 56 blocos do centro e param ao atingir dez árvores marcadas. `TreeNurseryGameTest.theNurseryStopsAtTenTrees` passou.
- [ ] Playtest P0.10/P0.11: confirmar no save que uma obra abandonada é retomada sem duplicar blocos, que uma tentativa impossível não congela a fila e que cada vila mantém dez árvores fora do centro.
- [ ] Residual de `FarmPlanGameTest.thenextturnafterahouseisnonresidential`: a rodada completa de 21-09 ainda abriu uma casa consecutiva; reproduzir a alternância com estado de vila isolado antes de alterar `HousePlans`.
- [x] Residual de `SmelterGameTest.theOreInTheMineMouthChestIsCountedAndSmelted`: não repetiu na rodada completa de 21-09; manter a fixture em observação antes de alterar a coleta do fundidor.
- [x] 🔴 **P0.8 — fundação absoluta da vila:** toda vila detectada cria a `BigHouseMOD`, cópia editada da big house Vanilla sem móveis/decorações, com seis camas e seis baús distintos. Os seis titulares (`MINER`, `LUMBERJACK`, `MASON`, `SMELTER`, `SHEPHERD` e `BUILDER`) recebem adulto, cama `HOME` e baú dentro dela; `FARMER` e `CARPENTER` continuam profissões ativas, mas sem cama/baú fundacionais na casa, e entram normalmente no crescimento. A Vanilla permanece intacta. `VillageFoundationGameTest` passou.
- [ ] Playtest P0.8: entrar em um save com vila recém-detectada e confirmar a `BigHouseMOD`, os seis aldeões, suas camas, seus baús e a ausência de sobreposição com estruturas existentes.
- [ ] 🔴 **E42 — impasse entre profissões:** criar o GameTest da roça fora do alcance do fazendeiro, com duas passagens do planejador, e corrigir a fila se a segunda passagem não abrir o projeto de casa.
- [ ] 🟠 **E43 — descanso ignorado:** decidir se o descanso de quatro ciclos deve impedir a reatribuição na segunda passagem de `WorkAssignment`, depois registrar a decisão em teste e corrigir o fluxo escolhido.
- [ ] 🟠 **P1.1 — trabalhador ocioso sem `COLLECT_STONE` ou `CRAFT_WOOD`:** adicionar uma regressão ponta a ponta para criação do pedido, atribuição ao ofício correto e execução; investigar a mesma raiz do caso de peça de construção já corrigido.
- [ ] 🟠 **Intermitencia de `SurfaceGatheringGameTest`:** reproduzida em 21-09. Na primeira de tres rodadas o fazendeiro criado fora da arena nao apareceu no `ServerWorld`; nas duas seguintes, o fundidor nao removeu a areia externa. Capturar uma reproducao deterministica e diagnosticar a fixture/execucao antes de alterar coleta ou timeout.
- [ ] ⚙️ **E38 — resíduos no inventário pessoal:** definir o destino sustentável de varas, maçãs e mudas antes de alterar armazenamento ou descarte.
- [ ] 🟠 **E41 — endurance:** criar uma verificação de muitos ciclos para detectar degradação, tarefas acumuladas ou custo crescente; ainda é lacuna de cobertura, não defeito reproduzido.
- [x] 🟠 **P1.3 — casas consecutivas:** o log mostrou `house → house`. `HousePlans` agora alterna `casa → tipo não residencial A → casa → tipo não residencial B`, exclui o tipo A anterior e mantém todas as famílias sob o mesmo `BuildSiteScanner`; `HousePlansTest` e o GameTest de rotação passaram.
- [ ] Playtest P1.3: confirmar no save a sequência casa, infraestrutura A, casa e infraestrutura B diferente de A.

- [x] 🔴 **E44 residual — perna da boca acima do degrau:** o log confirmou que o recálculo de `job.approach` não limitava a perna efetivamente entregue por `WorkTargets`. `MinerWork` agora escolhe um patamar pisável dentro de `CLIMB`; `MinerApproachGameTest` cobre a queda abaixo da boca e a suíte passou com **379/379 GameTests**. Falta confirmar no mundo que o mineiro volta a entregar pedra.
- [x] 🔴 **P1.2 — obra retomada sobre estrutura existente:** `BuildSiteScanner` agora rejeita interseção com peças de estruturas Vanilla da vila, blocos físicos ocupados e volumes já registrados, inclusive durante a retomada de projeto salvo. `BuildSiteGameTest.theResumedProjectRejectsAnOccupiedVolume` reproduz o caso físico.
- [x] 🔴 **P1.2 — zona sobre construção existente:** a seleção consulta peças Vanilla referenciadas pela `StructureAccessor`, inclusive quando o início está em outra chunk, e a `BigHouseMOD` rejeita telhados ou outros blocos de construção como camada de apoio. `BigHouseFoundationGameTest.theFoundationDoesNotStartOnTopOfAnExistingBuilding` reproduz a falha que aceitava o lote.
- [x] 🔴 **P1.2 — projeto pendente reserva o lote no carregamento:** o save agora participa da consulta de sobreposição antes de `ConstructionPlanner.resume`, e `BigHouseFoundation.ensure` não duplica uma `BigHouseMOD` ainda pendente. `BigHouseFoundationGameTest.aPendingProjectStillReservesItsSavedBox` e `aPendingBigHouseIsNotPlacedAgain` cobrem a janela observada no log de 21-09.
- [x] 🔴 **P1.2 — volume vertical do lote:** a varredura agora testa todas as colunas a partir do nível-base comum da obra; degraus, blocos voando e restos de outra construção dentro da caixa real recusam o lote antes de iniciar a casa. `BuildSiteGameTest.anElevatedColumnInsideTheBaseRefusesTheLot` reproduz a falha e passa após a correção.
- [x] 🔴 **P1.2 — folga vertical absoluta de 25 blocos:** nenhum lote, projeto pendente ou fundação da `BigHouseMOD` aceita um bloco físico na janela completa acima da pegada. `BuildSiteGameTest.aBlockTwentyFiveAboveTheLotRefusesTheLot` e `BigHouseFoundationGameTest.theFoundationRejectsABlockTwentyFiveAboveTheFloor` cobrem o limite.
- [x] 🟠 **P1.2 — cancelamento manual por Tocha das Almas:** colocar uma Soul Torch dentro de uma obra profissional cancela a zona, o projeto, as tarefas e o registro parcial, liberando o ciclo; a `BigHouseMOD` não é cancelada. `ConstructionCancellationGameTest` cobre os dois contratos.
- [x] 🔴 **P1.2 — obra não desaparecia ao colocar o último bloco:** a tarefa reservada agora entra em execução antes de concluir, então o último bloco marca `BUILD` como `COMPLETED` em vez de liberá-la para o próximo ciclo. O evento de mudança também prioriza a Soul Torch antes de filtrar edições do próprio mod; `BuilderGameTest` e `ConstructionCancellationGameTest` reproduzem os dois sintomas.
- [x] 🟠 **P1.2 — `BigHouseMOD` fora do catálogo profissional:** a fundação obrigatória continua nascendo com a vila e não pode ser escolhida como casa/oficina por profissões. `HousePlansTest.theModBigHouseIsNeverAProfessionBuild` fixa a regra.
- [x] 🟠 **P1.2 — frente arenosa cíclica:** `MinerWork` mantém a posição após cada quebra, espera a queda de areia/gravilha assentar e só então reavalia a frente. A coleta integral continua em `MinerHaul.deposit`, inclusive para overflow no chão. Falta confirmar a progressão e a entrega no save.
- [ ] Playtest P1.2: iniciar uma obra perto de construções existentes e acompanhar o mineiro no deserto até confirmar que não há acavalamento, que a frente avança após os assentamentos e que os drops chegam ao baú/overflow.
- [ ] Playtest P1.2: reabrir o save com projeto pendente e confirmar que a zona antiga permanece reservada, a `BigHouseMOD` não é duplicada e nenhuma obra nova é salva sobre ela.
- [ ] Playtest P0.9: criar vilas nos cinco biomas e confirmar no save que somente as estruturas listadas aparecem, que areia/relva são buscadas longe da vila quando solicitadas e que a entrada da mina permanece seca e acessível.

## Prioridade atual — obra aberta impede a próxima construção

- [x] Revisar os dois commits locais: espera entre ofícios (`e37708c`) e recálculo de aproximação do mineiro (`b7ef9e2`). `fetch` e `pull --ff-only`: nenhuma novidade remota; alterações locais preservadas.
- [x] Completar o guarda de obra em `BUILDING` sem progresso: liberar a vaga após 12.000 tiques de expediente, preservando a construção parcial e seu lote. Descontar a noite sem zerar a espera; renovar o prazo quando uma peça é assentada.
- [x] Corrigir e registrar `BuildProgressGameTest`: API de marcação, relógio de idade do mundo, conclusão dos testes e quatro cenários (parada, progresso, noite e mudança de horário). Regressão noturna reproduzida antes da correção.
- [x] Corrigir a escrita do relatório de `ChainRootsGameTest`, que falhava em `createDirectories(null)`. Suíte final: **376/376 GameTests**.
- [x] Encerrar tarefas `BUILD` no abandono da obra e limpar job/destino do construtor. Limpar o destino também quando uma tarefa encerrada é vista no tick. Dois novos GameTests reproduziram as falhas antes da correção; `WorkerLossGameTest` distingue morte (devolve tarefa) de abandono (cancela). Suíte posterior: **378/378 GameTests**.
- [ ] Playtest: confirmar recuperação da fila, preservação do lote e segunda casa. Comparar `SweepLog.busy` com passadas; não declarar a varredura resolvida pelo contador antigo.
- [ ] Playtest dos commits locais: estabilidade de ofício e entrega de pedra depois de limitar também a perna real da boca ao próximo patamar.
- [ ] Investigar a falha histórica de `ColonyDetectionGameTest` (24/30), não reproduzida na execução de 09-20.

---

## 🔴 P1.0 — Nenhum lote aprovado: a vila não chega a planejar obra

Playtest de 2026-09-17, 00:09. O autor: *"não vi as zonas de construção
marcadas, não vi casa crescendo"*. E o log concorda: **`planned` = 0**,
`opened a build task` = 0 em 6 minutos, com 8 pedidos de lote.

```text
lot refusals: 174912 candidates turned down
  92972 (53%) the ground is inside a reserved road area
  33597 (19%) village-original or player-placed, Rule 3 protects it
sweep: 8 planner runs, 2055 columns, 0 complete rounds
```

**É anterior ao E46:** aquele conserta a obra que morre depois de nascer, e
aqui nenhuma nasceu. A causa suspeita é `BuildSiteScanner.isRoadArea`, que
trata **todo calçamento original da vila** como reserva de estrada — com a
Regra 3, 72% do território fica bloqueado.

⚠️ **Duas leituras, e a sessão não separou:** `0 complete rounds` também
explica tudo (a varredura precisa de ~17 ciclos e teve 6). Ver
`lento-nao-e-travado` — este diagnóstico já foi errado antes.

- [x] ✅ **Instrumentar** quantas colunas sobrevivem a todas as recusas — `LotRefusals.accepted`, com linha própria no relatório. **Verificado:** build + 902 unitários + 336 GameTests em 5/5. Gametest `theSurvivingColumnsAreCounted` conferido no XML.
- [x] ✅ **Playtest de 42 min** — a resposta foi **zero**: `0 survived every check, 960672 were turned down`. Não é orçamento de varredura; a vila não tem chão que sirva. Distribuição idêntica à da sessão curta, então não é amostra pequena.
- [x] ✅ **Decidido pelo autor (opção 1):** a reserva vale só para a rua que a colônia calçou. `isReservedAgainstLots` é nova e substitui `isRoadArea` **só na recusa de lote** — a função antiga continua servindo aos três usos positivos, senão a colônia deixaria de reconhecer as ruas Vanilla. **Verificado:** build + 902 unitários + 337 GameTests em 5/5.
- [x] ✅ **Playtest do JAR novo (08:23)** — **abriu chão: 313 colunas aprovadas**, contra zero antes, e **uma obra foi planejada**. A reserva caiu de 54,8% para 49,6% das recusas.

---

## 🔴 P1.1 — A obra espera uma peça que ninguém fabrica (playtest 08:23)

A biblioteca nasceu às 08:43 e passou **8 minutos em `WAITING_RESOURCES` com
628 de 628 blocos**, esperando `cobblestone_stairs`. A colônia tinha **69
`COBBLESTONE`** no baú.

```text
Builder … stopped — no minecraft:cobblestone_stairs in the colony chests
no collect_stone work: no worker in the village can do it — COBBLESTONE needs COLLECT_STONE
no craft_wood_material work: no worker in the village can do it — OAK_PLANKS needs CRAFT_WOOD
```

**Não é falta de material — é falta de quem transforme.** Estoque: 4.258
tábuas, 1.686 toras, 69 pedregulhos. É o laço fechado que
`analise-plano-crescimento.md` já descrevia em 09-12.

- [x] ✅ **Corrigido em 09-17** (`4cee104`). A causa: `ColonyCycle.requestMissing` itera `Map<ResourceType,Integer>`, e `cobblestone_stairs` **não é um `ResourceType`** — escada, porta e cerca são peças da planta, não recursos contados. Agora a obra abre a tarefa **pela peça que espera**, e quem decide se dá para fazer é o jogo (`ColonySupply.canProvide` → `CraftingLookup`), não uma lista. **Verificado:** build + 902 unitários + 338 GameTests em 5/5. ⚠️ sem playtest.
- [ ] 🔴 Confirmar em jogo que o P1.1 destrava o pedreiro e o construtor
- [ ] 🟠 `no worker in the village can do it` para COLLECT_STONE e CRAFT_WOOD com trabalhadores ociosos — pode ter a mesma raiz, não foi investigado

---

## 🟠 Revisão completa de 2026-09-17 — o que ficou sinalizado

Inventário em
[`docs/technical/Revisao-2026-09-17.md`](docs/technical/Revisao-2026-09-17.md).
**391 arquivos `.md`, 40.838 linhas, 23 documentos parados desde 08-08.**

**Decisões do autor, pendentes** (o pedido foi sinalizar para decidir depois):

- [ ] 🔴 Destino de `Class-Architecture.md`, `Fabric-Implementation-Plan.md`, `Data-Model.md` — descrevem `ColonyManager`, `TaskManager`, `BuildingStatus` e outras classes que **nunca existiram**. Recomendado: `docs/historical/`.
- [ ] 🟠 Destino de `MVP.md`, `MVP-Tasks.md`, `START_PROJECT.md`, `Development-Roadmap.md` — planos concluídos ou superados
- [ ] 🟠 **`STATE.md` tem 1.193 linhas** contra o teto de 150 que ele mesmo declara. Precisa de poda.
- [ ] 🟠 `ConstructionService.forget` sem chamador em `src/main` — confirmar se é gancho ou resto
- [ ] 🟠 **Pastor tem 2 gametests; mineiro tem 79.** As profissões calmas são as menos protegidas, não as mais sólidas.
- [ ] 🟠 Playtest que exercite **fundidor** e **pastor**

---

## 🔴 P1.2 — Água trava o mineiro (pedido do autor + confirmado no log)

O autor: *"quando aldeão encontra água ou lava ele se perde e trava, ele
deve tentar fechar o bloco que gerou a água ou lava rapidamente e trocar de
caminho"*.

**Confirmado:** 15 desistências com `the place to stand is …, which is
flooded`, e `flooded` 30 vezes no log de 28 min. O aldeão fica parado até o
guarda de imobilidade expirar (300 tiques) e devolve a tarefa.

**O que já existe:** `MineDigging.flooded` vira o ramal
(`The branch turns away from the water`, 5×). **O que falta:** tapar o bloco
que verte e seguir, em vez de esperar o guarda. Lava não apareceu
(`lava` = 0).

- [x] ✅ **Corrigido em 09-17.** A pesquisa mostrou que *tapar a fonte e virar o ramal* **já existia** desde 2026-09-03 (`MineFlooding.seal` + `MineDigging.flooded`) e funciona — agiu 5 vezes no log. O que faltava era o **outro caso**: a água que já estava lá antes de a picareta bater, com o aldeão a 11 blocos andando para um `place to stand` alagado. `BuilderApproach.isDry` recusa na escolha. **Verificado:** 340 GameTests, 4/5 rodadas.
- [x] ✅ **Lava entra pela mesma porta** — `getFluidState` não distingue.
- [ ] 🟠 Confirmar em jogo que `which is flooded` caiu dos 30 medidos

---

## 🔴 P1.4 — A escolha de área: a varredura nunca fecha a volta

Varredura completa do subsistema em
[`docs/technical/Escolha-de-area-varredura-2026-09-17.md`](docs/technical/Escolha-de-area-varredura-2026-09-17.md).

**O achado que manda:** a vila varreu **22.116 colunas** em 27 passagens —
mais que os **16.641** de uma volta inteira — e ainda marcou
`0 complete rounds`. O reinício por deriva de centro custou a volta toda.

- [x] ✅ **S5** — `LotRefusals.clearAll` entrou no ciclo de vida. Era o único dos cinco vizinhos que ficava de fora, e a assimetria piorou quando o `ACCEPTED` entrou nele hoje.
- [x] ✅ **S3 verificado, já funciona** — `coloniesNearPlayers` faz a vila observada furar a fila do rodízio. Derruba metade do custo estimado: 32 `planner runs` em 32 min é vez quase todo ciclo. **Nada a fazer.**
- [x] 🟢 ~~**S1** — transladar o cursor em vez de descartar~~ — **descartado pela aritmética**, e o motivo está no §4.1: com deriva de 40, o anel seguro no centro novo é **0**. Translação não recupera nada.
- [x] ✅ **A deriva do centro: investigada, e NÃO é defeito.** A colônia foi de **37 para 69 camas** — a sonda descobriu quase o dobro da vila e o centro se mudou para o meio verdadeiro, uma vez só. Só a sonda move o centro (ADR-003 Emenda 4), e ela parte do centro e a ele volta. **O movimento estava certo; o preço é que estava errado.**
- [x] ✅ **Conserto aplicado: o cursor cai, o índice fica.** `ColonyRoads.rebasedTo` reancora o índice de ruas no centro novo e descarta só as colunas fora do raio. O cursor é anel **relativo** e vira lixo; o índice guarda **coluna absoluta**, e rua não deixa de ser rua. Custava **16 de 32** consultas. **Verificado:** 905 unitários (+3), 340 GameTests em **5/5**.
- [ ] 🟠 **C3 do E46 fica aberto por outro motivo:** o `colony.center()` fica a 77 blocos do aglomerado de camas que a detecção **vê e recusa adotar** (`view not provably complete`, 50×). Isso é diferente da deriva — é o centro que **não** se move quando deveria.
- [ ] 🟠 **S6 → S4** — duas réguas de chão: a **estrada** usa `isNaturalGround` (5 blocos) e o **lote** usa `isLotGround` (qualquer sólido). A estrada é mais exigente que a casa. Medir antes de unificar.
- [ ] 🟡 `BuildSiteScanner` tem **1.509 linhas** contra o teto de 500 do `CLAUDE.md`, com 36 métodos e 5 mapas de estado

⚠️ **Correção de afirmação minha:** na sessão anterior escrevi que *"a vila
não consegue estender estrada"*. **Errado** — `extended/grew the road` = 10
contra 3 recusas. A estrada cresce.

---

## 🟢 B1 — Cortador de pedra (melhoria de economia, 09-17)

`CraftingLookup` não lia `RecipeType.STONECUTTING`, e o pedreiro recebe
`Items.STONECUTTER` do `ChestMarker`. A escada de pedregulho custava **6→4**
na bancada; no cortador é **1→1**.

- [x] ✅ `cutFor` + `cheaperOf`: os dois caminhos são consultados e vence o que gasta menos **por peça**. Empate fica com a bancada.

---

## 🟠 P1.3 — Pedidos de design do autor sobre a mina (09-17)

Duas ideias que **não são defeito** e precisam de decisão antes de código:

- [ ] 🟠 *"a zona de cada camada da mina deve ser mais explorada"* — hoje o nível fecha quando os quatro ramais acabam (`ARM = 16`, `RINGS`). Explorar mais significa aumentar o raio do ramal ou adensar a galeria; muda o custo por nível e o tempo até descer.
- [ ] 🟠 *"quando o mineiro encontrar um veio de minério o foco deve ser o veio todo"* — já existe `MineArm.followVein`/`veinExhausted` e `OreVein.beside`. Verificar o que falta: seguir o veio **até acabar** antes de voltar à ordem de cavar.

⚠️ **Expectativa honesta:** `dirt_path` não é bloco sólido cheio e continuará
caindo em `NOT_NATURAL_GROUND` adiante. Quem passa a poder virar lote é o
calçamento de **gravel e terracotta** — quanto disso existe na vila do
playtest, ninguém mediu ainda. **O efeito pode ser bem menor que os 54,8%.**

- [ ] 🟠 **A vila também não estende estrada** — `none of the road ends this colony can see may be paved`, 23 tentativas na sessão de 42 min. Sem lote a regra manda crescer a rua, e a rua não cresce: é um círculo fechado, e vale investigação própria.

---

## ✅ P0.8 — E45: a mina presa na boca (CONFIRMADO EM JOGO 09-17)

**O conserto funcionou.** Playtest de 00:09: `Miner … took` = **37** (era
**0**), `hit stone with nowhere to stand` = **15** (era 17.518), e
`turning the helix` = **4** — o guarda novo girou a hélice em vez de travar.
Primeira sessão desde 09-15 com pedra saindo do mundo.

Reproduzido no playtest de 21:41–22:12: 17.518 recusas em 31 min, 10/s,
**zero pedra quebrada** (em 03:44 foram 166.559 em 4h40 — mesma assinatura).

⚠️ **A causa mudou na revisão de 22:30.** Não é o reinício por
`deepenIfEveryOpenArmIsDone`/`restartAt` — o log prova que esse caminho
**nunca é alcançado**. O laço é a **mesma passagem repetida**: o braço fecha
em 8 recusas dentro de um orçamento de 64, `claimArm` o solta, e a passagem
seguinte o reocupa no mesmo `cut`. Detalhe em
[`docs/technical/E45-mina-presa-na-boca.md`](docs/technical/E45-mina-presa-na-boca.md).
**Nada foi alterado no código.** Ordem revisada:

- [x] ✅ **C4 + C2** — **corrigidos em 09-16. Decisão do autor: (b) com (a) como escalada.** `Mine.turnsWithoutAPickaxe` conta as voltas sem pedra, mora na mina (o braço é solto a cada passagem) e **só a picareta zera**. Cheias 3 voltas → gira a hélice com o `rerouted()` existente; esgotadas as 4 → boca nova; sem boca melhor → `exposedStone`. **6 testes novos**, e o principal fecha e reinicia os ramais exigindo que a conta sobreviva. **Verificado:** build + 902 unitários (0 falhas) + 335 GameTests em 4/5. ⚠️ sem playtest.
- [x] ✅ **C1** — resolvido junto: o giro e a escalada emitem linha própria (`turning the helix from … to …`, `tried all 4 helices …`), e o laço deixou de ser mudo.
- [x] 🟢 ~~**C3** — `everyOpenArmIsDone`~~ — **retirado:** premissa refutada pelo log, e mexer ali arriscaria o limbo de 09-04

**Defeito de fundo a tratar junto:** `stall`, `still` e `adrift` ficaram
zerados nas duas sessões — o caminho de falha zera os contadores no mesmo
tique. Terceira volta do mesmo laço; os consertos anteriores fecharam portas
específicas, e o guarda genérico nunca disparou em nenhuma das três.

---

## ✅ P0.9 — E46: a obra nasce condenada (C4 corrigido 09-16, espera playtest)

Playtest de 21:41. A biblioteca foi planejada **duas vezes** e as duas
morreram em ~30 s com **628 blocos restantes de 628**, sem um bloco posto.

**O C2 mudou a causa raiz.** Não é a divergência quadrado/círculo — as obras
estavam fora das **duas** réguas. É o **índice de ruas sem teto de raio**:
`findAmongRoads` percorre o índice inteiro sem consultar o raio, e
`remember` acrescenta qualquer rua nova. A vila calçou estrada 31 vezes, e o
lote passou a nascer de onde a estrada chegou. Prova: `40 answered by the
index — 0 by drift`, do `SweepLog`. Detalhe em
[`docs/technical/E46-obra-nasce-condenada.md`](docs/technical/E46-obra-nasce-condenada.md).

**É independente do E45** — corrigir o mineiro sozinho não faz a vila
construir.

- [x] ✅ **C2** — instrumentado: a linha `planned …` diz o centro e as duas distâncias; o scanner emite `WARN` ao servir lote de fora do raio. **Verificado:** build + 886 testes, 0 falhas (XML conferido). ⚠️ sem gametest e sem jogo.
- [x] ✅ **C4** — **corrigido em 09-16. Decisão do autor: (c)** — o scanner estava certo, o guarda errado. A estrada é projetada para sair do raio (`RoadExtension` ordena as pontas da mais distante para a mais perto), e o raio de 64 é da **detecção de vila**, não um limite de crescimento. O guarda passou a medir da **rede de ruas** (`ColonyRoads.blocksToTheNearestRoad`), larga a obra a mais de 16 blocos de qualquer rua, e **sem índice cai na régua antiga**. **Verificado:** build + 896 unitários (0 falhas) + 335 GameTests em 4/5 rodadas. ⚠️ sem playtest.
- [ ] 🟡 **C1** — **rebaixado pelo C4.** O círculo só governa agora o caso **sem índice de ruas**; no caminho normal quem decide é a distância à rua, em quadrado. A divergência continua no caso residual, mas os 21% já não descrevem a vila em operação.
- [ ] 🟠 **C3** — o centro que a detecção recusa mover (59× `view not provably complete`, centro a 77 blocos do aglomerado real). **Não causou o E46.** ⚠️ investigação própria — afrouxar a regra de completude ressuscita o E2.
- [x] 🟢 ~~`isSupersededBy` do planejador~~ — **descartado:** `drops the untouched` = 0
- [x] 🟢 ~~`PatienceClock` / obra sem material~~ — **descartado:** `WAITING_RESOURCES` não aparece no log, e a paciência é de 10 min contra 29 s observados
- [x] 🟢 ~~`ConstructionService.forget` chamado de outro lugar~~ — **descartado:** único chamador é `WaitingWork:190`
- [x] 🟢 ~~o scanner partir de outro centro~~ — **descartado pelo C2:** parte de `colony.center()`, e `0 by drift`

---

## 🟠 P1 — Village Growth Planner (especificado 09-16, nada implementado)

A vila que **nunca termina**: diagnóstico do que já existe → `NEED_SCORE`
por estrutura → estrutura → lote → construção, em ciclo permanente. Não uma
fila fixa que acaba na vigésima casa. Especificação inteira do autor em
[`docs/technical/village-growth-planner.md`](docs/technical/village-growth-planner.md).

A [ADR-009](docs/decisions/ADR-009-Autonomous-Village-Evolution.md) já
estabelece o princípio desde 08-22 e **não é contradita** por isto. A lacuna
real é concreta: `HousePlans.houseFor` escolhe a planta **pelo tamanho** (a
maior que cabe), e não por necessidade da vila.

- [ ] 🟠 **Playtest primeiro** — confirmar E45 e E46 antes de construir por cima
- [ ] 🟠 ADR do Growth Planner: onde mora, como se liga ao `ConstructionPlanner`, o que acontece com a Regra 25
- [ ] 🟠 `VillageInventory` (Estágio 0) — o diagnóstico do que a vila já tem. É `core` puro e testável sem servidor
- [ ] 🟡 `NEED_SCORE` — substitui `houseFor` como quem escolhe o alvo
- [ ] 🟢 Estágios de desbloqueio e expansão permanente, em fatias

---

## 🟠 Achados de fundo do playtest de 21:41

- [ ] 🟠 **36 colônias ativas**, `Colony cycle took` até **486 ms** — 40 ciclos acima de um tique do servidor. O custo do ciclo cresce com o número de colônias, e o playtest carrega muito mais que os cenários de teste.
- [ ] 🟠 **Só uma colônia planeja** (`9da5460c`); as outras 35 dão `assigned 0 tasks (0 open)`. Conferir se é esperado — é o padrão de `roca-sem-lote-trava-a-vila`.

---

> **Este arquivo é a lista viva.** Só o que está aberto agora.
> O histórico — sessões por data, ciclos fechados, erros resolvidos — está
> em [`TODO-archive.md`](TODO-archive.md).
>
> Onde este arquivo e o `Backlog.md` discordarem, **vale este**.
> Onde este arquivo e o `Plano-de-Correcao.md` discordarem sobre *o que já
> foi feito*, vale este. Sobre *o que fazer e em que ordem*, vale o plano.

---

## Plano de trabalho por lotes — 2026-09-14

Plano solicitado para manter mineiros em atividade, variar as estruturas
tentadas pelos construtores, ampliar a avaliação de espaços e revalidar após
falhas repetidas: [plano completo](docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md).

**Lote 1 concluído em código:** emenda da ADR-012 e reconciliação local de
alterações do jogador no scanner; 324/324 GameTests e `build` passaram. Ainda
aguarda validação em jogo. **Lote 2 teve a primeira correção integrada:** job
fechado libera imediatamente a claim do ramal do mineiro; o novo
`MinerWorkLifecycleTest` prova a limpeza no mesmo tique. A
continuidade/recuperação restante segue sujeita a revisão do autor; lotes
seguintes cobrem variedade dos construtores, estratégias de avaliação de
espaço e revalidação escalonada.
“Continuar trabalhando” respeitará expediente, recursos, perigo e chunks
carregados, sem criar recursos ou tarefas fisicamente impossíveis.

**AUD-001 continua aberto:** a asserção Linux histórica foi recuperada e a
contraprova confirmou que o teste acusa a ausência real de coleta quando a
regra de terra é desligada. A suíte local fresca passa 324/324; porém a perda
do aldeao entre `spawnEntity` e o tick 1 ainda nao tem causa deterministica,
e a execucao Linux da revisao ainda nao existe. Nao fechar o Lote 0 nem
aumentar timeout por este achado; P0.7 e uma decisao independente ja entregue.

---

## ⏭️ Por onde começar

**P0.7 entregue em codigo e no JAR, espera playtest.** A politica aceita qualquer piso
solido disponivel, sem taxonomia geologica e sem terraplanagem. Os unicos
materiais de estrada sao `dirt_path`, `gravel` e `terracotta`, e so bloqueiam
o lote dentro de `ROAD_AREA`. A decisao e a verificacao de 327/327 GameTests
estao na ADR-017. O JAR 0.3.0 atual tambem inclui a liberacao imediata de
claim do job mineiro encerrado; foi distribuido em 2026-09-15 com SHA-256
`C5D0790F996082CE3B7D2AA55CED93936DF04063568A03B0B521F50245A0BA1A`.

**E a varredura não era a culpada — o instrumento do projeto disse isso por
escrito.** O `SweepLog` gravou no encerramento:
`47 planner runs, 0 passes over 0 columns, 0 complete rounds`, com
`46 of 47 planner runs gave up before reaching the sweep` e o aviso
*"the sweep is not why: most cycles gave up before reaching it. Look at
what the planner refused, not at the sweep."* **Zero passagens em 47
tentativas.** A hipótese de orçamento de varredura foi descartada por
medição, não por leitura.

✅ **Resolvido em 2026-09-12:** o defeito latente do `CropPatch.survey` foi
confirmado com GameTest e corrigido. O canteiro vazio achado numa fatia
pausada agora é lembrado por colônia enquanto o `RingSweep` não fecha a
volta; a lembrança é validada contra o mundo antes de ser reaproveitada e
limpa junto com a varredura do fazendeiro.

✅ **Auditoria 1.9 registrada em 2026-09-12:** nenhum CRÍTICO novo foi
confirmado. O ALTO era de release: não havia workflow de CI/CD, e o
manifesto publicado aceitava qualquer Fabric API com `"*"`. Entrou
`.github/workflows/ci.yml` com actions pinados por SHA, Java 21,
unitários, Python, `build`, `runGametest`, artefato do jar e relatórios
em falha. O `fabric.mod.json` agora exige a Fabric API da matriz por
expansão do Gradle, e `ModMetadataTest` impede o curinga de voltar.
Registro completo em
[`docs/technical/Auditoria-2026-09-12.md`](docs/technical/Auditoria-2026-09-12.md).

---

## Playtest de 2026-09-14 — casa sem avanço visível

O log registra seleção e abertura de `plains_butcher_shop_2` às 00:50:15,
com 382 blocos. Dois foram assentados; a obra ficou com 380 em
`WAITING_RESOURCES` esperando `minecraft:dirt` por mais de seis minutos. Os
18 baús tinham 33 `grass_block` e nenhum `dirt`. A causa imediata era uma
lacuna na cadeia de produção: o catálogo reconhecia areia e grama, mas não
terra, então nenhum trabalho de superfície do fundidor atendia essa demanda.

**Corrigido e testado:** `DIRT` entrou no catálogo de recursos e no conversor
Vanilla; o fundidor busca terra exposta fora do raio protegido, no setor
cardinal definido para coleta externa, sem carregar chunks. GameTests cobrem
tipo/tarefa e o ciclo físico de quebrar e guardar terra. `build` passou e
`runGametest` passou com 320/320. **Ainda requer JAR atualizado e confirmação
em jogo:** coleta de terra, retomada dos 380 blocos e eventual bloqueio de
alcance observado depois da espera por material. Não foi alterada a escolha
da construção: o log prova que ela já foi aberta.

---

## 🎮 Sessão de 2026-09-13

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3`, em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu:**

- Casa parada no meio.
- Mineiro invisível / parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a
   `STABLE`, reduzindo o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco
faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A
carpintaria fabricava composteiras, mas usava a porta de consumo
(`ColonySupply.take`) e retirava do baú a peça que acabou de produzir.
Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`,
que fabrica e deixa a peça no estoque para o construtor assentar.

**Reinvestigação após o playtest:** novo GameTest reproduziu `minecraft:dirt`
sendo colocado sem estoque porque `BlockTags.DIRT` era tratado como terra
moldada no local. A exceção agora só cobre farmland, água, `dirt_path` e
cultivos. `runGametest` (313) e `build` passaram. **Pendente:** confirmar
em jogo com o JAR atualizado se a obra deixa de assentar terra sem material
e continua construindo; o log anterior veio de um JAR antigo e não registra
o bloco/posição exata da terra vista.

## Reanálise do playtest de 2026-09-14

O log usado pelo launcher veio de JAR anterior à correção de terra. As casas
foram selecionadas, mas aguardaram `dirt`; o mineiro estava sem tarefa, não
preso na mina: havia apenas 3 carvões e nenhuma reserva de carvão/ferro definida.
Lenhadores cortavam sem consultar a proteção de estruturas e podiam confundir
troncos de casas com árvore de copa compartilhada.

**Implementado neste ciclo:** metas de 64 carvões e 64 minérios de ferro brutos,
somadas à demanda da obra; proteção da árvore no planejamento e revalidação de
cada bloco antes de quebrar. Testes de jogo cobrem estrutura da colônia
registrada antes/depois do plano. Troncos manuais sem marca ou folha persistente
continuam indistinguíveis de árvores; proteção adicional exige decisão sobre
marcação/persistência.

**Verificado e distribuído:** `build` passou, 322/322 GameTests passaram, e
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/` têm SHA-256
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.

**Próximo passo: validar em jogo.** A casa deve avançar além de 380 blocos; o
mineiro deve abrir tarefas e manter os pisos minerais; o lenhador deve cortar
árvores naturais por inteiro e preservar estruturas Vanilla e da colônia.
Fazenda e demais profissões não têm falha comprovada neste log.

**Playtest seguinte, 2026-09-13:** nenhuma construção visível e baús de
profissões recebendo produção cruzada. O log associado mostra somente três
peças assentadas; construtor encerrou por falta de `grass_block` e
`cobblestone`; mineiros repetiram falta de espaço para ficar em pé e
produziram zero; lenhadores e agricultores colheram; fundidores pararam por
falta de areia. A produção agora vai ao baú pessoal do profissional, com
insumos compartilhados. Sem espaço, transformações devolvem o insumo e
drops ficam no mundo. `build` e 313 GameTests verdes; **aguarda validação em
jogo**. A investigação da construção e da mineração segue aberta.

**Diagnóstico do log de 09-13:** o mineiro completou 64/64 pedregulhos e
ficou sem tarefa; depois recebeu areia para vidro, mas `MinerWork.Job` usava
a pedra da paleta e mantinha o progresso em `0 of 3`. Corrigido e coberto
por teste; `build`: 810 unitários, `runGametest`: 314/314. Depois da correção,
a tarefa ainda depende de encontrar areia: o log diz repetidamente que não
há areia num raio de 48 blocos. **Aberto:** implementar/validar o retorno
explícito do mineiro à boca quando a tarefa termina; `MinerWork.tick` hoje
descarta o Job encerrado sem criar uma rota de volta.

A obra `plains_butcher_shop_2` esperou `grass_block` (382 blocos restantes),
desistiu e manteve lote/obra parcial. Nenhuma profissão do catálogo produz
`grass_block`. **Aberto:** impedir seleção/reserva de blueprint sem cadeia de
materiais possível e definir recuperação da obra parcial, respeitando a
propriedade do lote. Fazenda: sem cultivo maduro ou lote vazio em 32 blocos.
Lenhadores cortaram e produziram durante a mesma sessão, embora às vezes
tenham ficado sem árvore próxima. Carpinteiros, fundidores, pedreiros e
pastor registraram "no task open"; o carpinteiro também ficou no baú em
`0/20` ticks fora do expediente. Isso aponta falta de demanda/insumos no
recorte observado, não falha de execução confirmada dessas profissões.

**Alterações do jogador, aplicadas em 2026-09-13 (ADR-012):** quebras e
interações que mudam blocos agora invalidam a busca local de lotes; trechos
abertos pelo jogador reabrem braços já percorridos da mina. A busca continua
limitada por tick e sem forçar chunks. `build` e 314 GameTests verdes. **Na
próxima sessão, verificar retomada da casa e reabertura da mina.** ADR-013
resolveu E45 (geometria/ramais), sem cadastrar baús arbitrários: o estoque
é vivo nos baús já vinculados ao trabalhador e na boca da mina.

**E45 resolvido em 2026-09-13 (ADR-013):** no fundo da mina, quando todos os
braços terminam, a hélice muda de orientação sem mover a boca. A galeria
continua usando `turned()` para seus quatro braços. A geometria agora é
`SHAPE_VERSION` 5; saves v4 zeram os cursores e preservam entrada, rumo e
arco. Testes focados reproduziram as duas falhas antes da correção; `build`
passou com 808 unitários e `runGametest` com 314/314. Falta confirmar no
mundo do autor se o mineiro percorre e trabalha na nova hélice.

---

## 🔴 P0 — bloqueadores

Um por vez, teste antes de seguir.

| item | o quê | estado |
|---|---|---|
| **P0.1** | O planejador não acha lote | ✅ entregue 09-11 · **visto em jogo** |
| **P0.1-b** | O caminho de terra não sai de baú | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.1-c** | A recusa de lote diz por quê | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.3** | Mineiro → armazenamento → fundidor | ✅ conserto entregue 09-11 · ⬜ **espera sessão** |
| **P0.5** | Perda de item por inventário cheio (E3) | ✅ corrigido e testado · ⬜ **espera sessão**; depósito pessoal revisto em 09-13 |
| **P0.6** | A enxurrada da areia calou | ✅ entregue 09-11 · ⬜ **espera sessão** |
| **P0.7** | Elegibilidade simplificada de lotes | ✅ entregue 09-15 · ⬜ **espera playtest** |

---

## 🔴 Erros abertos

| | erro | estado |
|---|---|---|
| **E44** | A escada de recusas já existe em `MineMarks` e é consultada pela mineração e pela fronteira da galeria; há unitários e GameTests. O playtest ainda observou o mineiro parado, então a integração completa segue **sem validação em jogo**. Não reabrir a decisão original sem reproduzir um defeito residual. | ⬜ validar em jogo |
| **E43** | O descanso de quatro ciclos é anulado no ciclo seguinte. O `giveUp` do mineiro marca `worker.rest(COLLECT_STONE)`, e a 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador sempre que a colônia não tem outro trabalho da profissão dele. **Decisão de projeto, e é do autor** | 🟠 aberto |
| **E41** | Nada mede degradação ao longo de muitos ciclos. O teste mais longo do projeto tem centenas de tiques. **Maior lacuna de cobertura depois do E37-b.** | 🟠 aberto |
| **E42** | Nenhum teste de impasse entre profissões. Os dois casos reais — a roça que travava toda a construção, e o fabricante que nunca descascava — foram achados **em jogo**, não pela bateria. **A tentativa de 09-09 à noite foi retirada pelo gauntlet-verifier** — os três casos escritos eram a invariante de vários trabalhadores, com outro nome. **O trabalho de verdade é outro gametest:** lote de roça fora do alcance do fazendeiro, planta de casa disponível, duas passagens do planejador, e a segunda tem de abrir projeto de CASA | 🔴 aberto |
| **E38** | O baú pessoal pode assorear com vara, maçã e muda sem consumidor; a colheita não transborda para outra profissão e itens sem espaço viram drops no mundo. Definir tratamento sustentável dos resíduos sem misturar depósitos | ⚙️ aberto |
| **KF-001** | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard`. **Fechado em 09-09:** a afirmação lia o estado da tarefa depois que o ciclo podia reservá-la novamente; o teste passou a registrar o instante da devolução e força a fase do ciclo. O orçamento global de uma busca/tique continua sendo um risco separado de vazão, não a causa provada da falha. | ✅ teste corrigido; medir vazão se houver evidência |
| **E21** | `theStoneLeavesTheWorldAndReachesTheChest` disse "a pedra não chegou ao baú" uma vez. Suspeita: custo de ler estrutura no tique. **Suspeita, não diagnóstico** | 🟡 aberto |
| **E4** | `path held: no` e o aldeão chega assim mesmo. Provável, nunca verificado | 🟡 aberto |
| **E3** | Sobra de colheita é perda de item. **Metade fechada em 09-04** — o lenhador deixou de destruir; **o mineiro continua sem teto de inventário** | ⚙️ metade fechada |
| **E9** | Colônia `ABANDONED` desmarcada no ciclo seguinte. **Mitigado em 09-13** — precisa de duas leituras positivas seguidas | ⚙️ mitigado |

---

## 🟠 Pendências abertas

| | o quê |
|---|---|
| **Mina de vila nova sem portal visível** | Confirmado no escalonador: uma única busca global por tique ficava presa no primeiro trabalho sem alvo, impedindo os mineiros seguintes de iniciar a busca. O rodízio foi corrigido sem aumentar a cota; `build` e 314 GameTests verdes. **Aguardar validação em jogo**: portal aparece e o mineiro inicia a escavação. |
| **Terra comum em blueprint** | Corrigido localmente: agora exige estoque. 313 GameTests e build verdes; **aguarda validação em jogo**. |
| **P0.7 — elegibilidade simplificada de lotes** | ✅ entregue 09-15; piso solido e elegivel, estrada exige `ROAD_AREA`; 327/327 GameTests e JAR atualizado. Espera playtest. |
| **A casa ainda sobe com a barreira de teste** | Última medição: 47 de 169 peças em 09-04. É o item que fecha a Fase 2 de verdade |
| **O lenhador rejeita as paredes da própria vila** | 118 rejeições sobre 28 posições. O castigo escalona (6.000→48.000) e funciona, mas ele redescobre a mesma parede seis vezes. **Nenhuma recusa veio com o número 24** |
| **O fundidor não tem o que fundir** | `nothing in the colony chests to smelt`, 34× na sessão de 09-04. Deve seguir o E44 |
| **Cobertura de qualquer material de construção** | ADR-015: fallback para fundidor e criador e estoque limitado pela capacidade física. Em 09-14, contagem e localização por `ResourceId` entraram em `ResourceTally`/`ColonyResources` e no leitor de baús (317 GameTests verdes). Metas, tarefas e execução profissional para IDs arbitrários continuam pendentes; não declarar cobertura funcional ainda. ADR-016, Lote 1. |
| **Mineiro longe do corredor não tem resgate** | Sem posição da ordem a uma perna dele, não há passo a dar e a boca continua sendo a resposta. Decisão de projeto: caminhar em linha reta, ou devolver a tarefa |
| **A arena de gametest não hospeda a galeria** | Ela assenta no fundo do mundo. Toda a bateria de mineração exercita **só o poço**. A divergência dos ramais é provada por unitário, não de ponta a ponta |
| **Os 49 `assign()` que criam trabalhador de mãos vazias** | Passam hoje por folga no `tickLimit`, não por estarem certos |
| **A proteção não consulta o registro de construções em todos os caminhos** | `BlockProtection` passou a consultar (`isColonyBuilt`), mas o caminho que decide o que pode ser quebrado ainda tem furos |
| **`Colony cycle took 122 ms`** | Contra o limite de 50. Medido em 09-11. **Instrumentar antes de otimizar** — ver P2.1 do plano |

---

## 📋 Pendências por nível de progressão lógica

A ordem é de dependência: cada nível precisa do anterior de pé.

### Nível 0 — o que já roda em jogo

Detecção · identidade estável · aldeões e profissões · baús · lenhador **em cinco espécies** · fabricante, **inclusive descascando e fazendo o que a obra pede dois degraus abaixo** · construtor **chegando ao bloco** · centro parado pela sonda · **a rua crescendo** · **a obra parada saindo da frente** · **a mina abrindo e sendo mobiliada** · **o mineiro cavando** · **o pastor tosquiando** · casa terminada.

### Nível 1 — a raiz do material *(aberta, e ela se fecha sozinha)*

- **A mina abriu em 2026-08-26** — a busca acertou na primeira camada.
- **O mineiro cavou** — 43 blocos numa tarefa só, descendo até y≈44.
- **A galeria o engoliu, e o E30 fechou em 08-27**.
- **Continuar ainda não está provado em jogo**. O conserto tem teste e **nenhuma sessão o viu rodar**. E o **E32** ficou de pé.
- **A pedra de superfície continua sem prova** — nunca foi exercitada.

### Nível 2 — material processado *(feito, e ainda passando fome)*

- **E18 fechado em 08-22**, pelo caminho genérico que a ADR pediu.
- **Nenhuma sessão viu isso rodar** — a linha a procurar é `Smelter ... made minecraft:smooth_sandstone`.
- O fundidor espera **a areia**, e a cadeia da areia começou em 08-20.

### Nível 3 — a obra termina sem o jogador

- **A casa terminou sozinha em 2026-08-26** — 149 blocos planejados, **127 assentados**, em 4 min 57 s.
- **Mas dezenove peças foram da barreira**, não da colônia. *Casa feita inteira com material da própria colônia* **continua sem prova**.
- **A barreira risca antes de a cadeia ter chance**. Enquanto a Regra 28 valer, a soma da sessão superestima o que está quebrado.

### Nível 4 — a vila não fica presa

- O planejador persegue **uma** obra e não sabe desistir.
- ✅ **A varredura recomeçar a cada obra fechada** — resolvido em 08-27, o cursor fica onde achou o lote.
- ✅ **Perguntar só às ruas** — resolvido em 08-27 pelo índice.
- ✅ **O cursor da varredura** — resolvido em 08-27, e **confirmado em jogo**.
- ✅ **O veio que desce** — resolvido em 08-27.
- 🟠 **O mineiro cavando de verdade ainda não foi visto em jogo.** **Falta a sessão que confirme.**
- 🟠 **Mineiro longe demais não caminha até a mina.** Não investigado.

### Nível 5 — o motor da ADR-009

`VillageProfile` · inventário de território · escassez e distância · orçamento de recursos · detecção de dependência circular · reserva mínima de sobrevivência · objetivos graduais. **Nada disso existe.**

### Nível 6 — o que nem modelo tem

Comida · água · o fazendeiro (tem enxada e baú desde a Fase 4 e nunca teve código) · população por capacidade · defesa · especialização · comércio entre vilas.

### Fora dos níveis — dívida que não bloqueia

- **13 arquivos de código acima de 500 linhas**, e 11 de teste. `VillageDetectionHandler` é o pior com **1.107**, e o corte dele é o próximo.
- **ADR-008** (orientação) e **ADR-007** (fusão), decididas e por escrever.
- **Regra 16** — distância mínima e máxima entre construções.
- **O ícone** — 1,95 MB num jar de 2,29 MB.
- **Cenário de teste por bioma.** A planície escondeu **duas vezes** que o deserto estava quebrado.
- **O `Development-Log`** está atualizado até 09-15. Cada lote futuro deve registrar ali a evidência, o escopo e o artefato distribuído.

---

## ⚠️ Incompatibilidades — o que se contradiz hoje

| | o quê |
|---|---|
| 🔴 | **Regra 28 vs ADR-009 §3.6.** A barreira é o remendo do problema que a ADR quer resolver: ela esconde o travamento em vez de a vila mudar de objetivo |
| 🟠 | **Regra 25 inerte** enquanto a 28 valer: "a maior planta que couber" precisa de mais de uma planta |
| 🟠 | **`furniture()` do `BlueprintBlock` sem dono** desde a morte da Regra 21 |

---

## 👤 Decisões que faltam, na ordem em que travam

| | decisão | trava |
|---|---|---|
| 1 | **E43 — o descanso de 4 ciclos deve valer sempre?** | Anulado pela 2ª passagem do `takeOneTask`. Decisão de projeto |
| 2 | **TASK-048 — o que uma colônia ABANDONED deixa de fazer?** | Hoje nada. Ela é marcada e continua sendo simulada |
| 3 | **TASK-044 — a fusão de vilas** | ADR-007 escrita em 08-21, não implementada |
| 4 | **TASK-046 — a orientação dos blocos** | ADR-008 escrita em 08-21, forma (a). Metade do E8 fechou em 08-15; a orientação fica |
| 6 | **E38 — o baú do trabalhador assoreia** | Dar consumidor ou descarte a vara, maçã e muda. **Decisão de projeto** |
| 7 | **E45 — como a mina troca de rota no fundo?** | Geometria, boca estável, migração do save e novo GameTest; não há ADR atual |

---

## 🧪 O que falta ver em jogo

Em ordem do que mais precisa ser visto.

| | o quê | a linha que prova |
|---|---|---|
| **1** | **Portal da mina em vila recém-descoberta** | log `opens a mine at ...`; confirmar entrada visível e mineiro iniciando trabalho |
| **1** | **A mina velha destravando** | `Mine ... finished every branch and went one level deeper` |
| **1** | **A varredura acabando num ciclo** | `no building work: still sweeping` aparecendo **uma vez** e não a sessão inteira |
| **2** | **P0.1-b, P0.1-c, P0.3, P0.5, P0.6** | entregues em 09-11, **nunca vistos** |
| **2** | **As quatro correções de 09-13** | sessão do autor |
| **2** | **O mineiro parando à noite** | o contador de `stall` **congelado** com `off hours` |
| **2** | **A ferramenta de ferro na mão** | ferro, e não madeira nem diamante |
| **2** | **Profissões e crescimento (ADR-011)** | sete funções produtoras, ordem de vagas nos adultos 15/16/30/31/32 e produtor construindo sem perder o ofício |
| **2** | **O nome colorido** | sete cores distintas para as profissões ativas, e o nome do jogador **sem** cor |
| **2** | **O arco da boca** | dois pilares, verga e lanterna — **junto** com o baú do mineiro |
| **2** | **Coleta de superfície do fundidor (ADR-014)** | pá com Toque Suave I; areia no baú pessoal e `grass_block` só com obra solicitando, além de 64 blocos e longe das estruturas |
| **3** | **A cadeia da areia inteira** | meta de vidro → fundidor busca areia → vidro → vidraça |
| **4** | **A casa inteira sem a barreira** | `TEST BARRIER covered for nothing` |
| **5** | **A rua crescendo e a casa nascendo junto** | `extended the road N blocks ...` seguido de `planned ... at ...` no mesmo ciclo |
| **6** | **A casa de deserto subindo** | `blocks left` caindo de 113 |
| **7** | **Fechar e reabrir o mundo com mina aberta** | a **galeria retomada** |

**Limites conhecidos:** a arena da bateria tem bioma fixo de planície — taiga, savana, nevada e deserto nunca rodaram, e todas as sessões até hoje foram em planície.

---

## ⚙️ Ciclo de 2026-09-11 — recusados

**Três itens do plano não se fazem**, e os três pelo mesmo motivo: o plano foi escrito a partir deste arquivo, e as linhas que ele copiou **já estavam vencidas**.

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto**: é o primeiro critério de ordenação da obra, e é o que põe mobília depois da casa inteira |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |
| **P1.7** | Separar `WOOD` por espécie **refaria o defeito de 09-10** |

---

## 📌 Notas do ciclo de 09-12

### ✅ 2026-09-12 — duas sessões de jogo, dois defeitos de "uma vez só"

**O arco da mina voltava depois de quebrado.** `Mine.archRaised` entrou no save sob a chave `arch`, **fora do `SHAPE_VERSION` de propósito**. O critério levou três reprovações do `gauntlet-verifier`, e as três viraram gametest.

**O lenhador deixava tronco de pé — e era o E39.** A causa não era alcance. **E o ciclo vicioso que ninguém havia medido:** expulsão deixa tora de pé → `markUnreachable` pula a árvore por 6.000 tiques → a copa decai nesse tempo → o resto vira `N logs without a living canopy`, que é recusa **definitiva**.

**Verificado:** 780 unitários e 304 gametests. Cada um dos quatro testes novos foi visto falhando contra a versão que acusa.

---

## ⚙️ Ciclo de 2026-09-10 — recusados

| item | por quê |
|---|---|
| **P1.8** | O `furniture()` **não está morto** |
| **P1.12** (parte) | A asserção defensiva no `assign()` **quebraria a contratação** |

---

## 📎 Referências rápidas

- **Plano de correção:** [`docs/technical/Plano-de-Correcao.md`](docs/technical/Plano-de-Correcao.md)
- **Histórico por data:** [`TODO-archive.md`](TODO-archive.md)
- **Estado vivo:** [`STATE.md`](STATE.md)
- **Assinaturas de defeito:** [`docs/PATTERNS.md`](docs/PATTERNS.md)
- **Regras do autor:** [`docs/RULES.md`](docs/RULES.md)
- **Estado detalhado (histórico):** [`docs/technical/Project-State.md`](docs/technical/Project-State.md)
- **Próxima sessão de jogo:** [`docs/proxima-sessao.md`](docs/proxima-sessao.md)
- **Responsabilidade das profissões:** [`docs/technical/Profession-Responsibility.md`](docs/technical/Profession-Responsibility.md)
- **Regressões catalogadas:** [`docs/behavioral-tests/REGRESSION-HISTORY.md`](docs/behavioral-tests/REGRESSION-HISTORY.md)
- **Falhas conhecidas:** [`docs/behavioral-tests/known-failures.md`](docs/behavioral-tests/known-failures.md)

---

## Nota sobre este arquivo

**Regra de manutenção:** quando um item fecha, ele **sai** daqui e vai para o `TODO-archive.md`, na seção `✅ Resolvido` do mês correspondente. Quando um item novo abre, ele entra na seção apropriada — **nunca** em bloco de "ciclo".

**Meta de tamanho:** 150 linhas. Se passar, algo está sendo arquivado no ritmo errado.

**O que este arquivo não é:** não é log de sessão, não é histórico de decisão, não é lugar de guardar "como se chegou aqui". Isso vai para `Development-Log.md` e `TODO-archive.md`.
