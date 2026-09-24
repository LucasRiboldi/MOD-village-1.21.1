# STATE — 2026-09-24

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Este arquivo chegou a **2.277 linhas** antes de
> 09-19: ele passou a atrasar a sessão que devia orientar.

---

## 🟢 Rodada de qualidade — 24-09, tarde (itens 1 a 12 da pesquisa de métodos)

- CI nos branches `codex/**`; PIT no core (77% de mutações mortas);
  JaCoCo; Error Prone só com avisos; fabric-loader-junit; depuração de
  mixin nos runs; três GameTests intermitentes isolados.
- Analisador de log com 19 assinaturas: no log de 24-09, 196 ciclos
  passaram de um tique.
- spark instalado em `mods`, com procedimento em
  `docs/technical/Profiling-spark.md`.
- Hooks prontos em `scripts/hooks/`; **o autor liga** no `.claude/settings.json`.
- **Nenhum arquivo de produção acima de 500 linhas**: 17 divididos, 33
  classes novas, movimento puro.

Estado final: `gradlew build` 1025/1025, `runGametest` 433/433 (duas
rodadas verdes no último recorte). Detalhe em `TODO.md`, seção "Qualidade
e verificação".

---

## 🟢 Revisão de naturalidade aplicada — 24-09, decisão do autor (espera jogo)

Seis itens entraram em código, com commit e testes por marco. O N5 já estava
feito desde o P0.7. Ver `docs/technical/Revisao-Naturalidade-2026-09-24.md`.

- **N1** (`fc9d9b8`): fundação só na criação ou quando a BigHouseMOD sobe,
  com 1 aldeão por cama. Morto não é reposto. `VillageMeals` dá comida ao
  fim do expediente para a vila crescer por procriação. Emenda na ADR-018.
- **N7** (`8e78fa7`): placa do lote em topo da planta + 5.
- **N10** (`553faaa`): escada da fuga tampada com o que saiu dela.
- **N11** (`74a273a`): alcance de coleta = 24 + 2 por cama, até o teto do ofício.
- **N9** (`4590e4c`): casa → oficina do ofício sem oficina → o que falta.
- **N3** (`bd7072e`): equivalente antes da peça pronta (119 → 102 automáticas).

Verificado: `gradlew build` 1022/1022, `runGametest` 433/433 (duas rodadas
verdes seguidas depois do N9). **Não verificado em jogo:** a procriação
acontecer de fato, a leitura da placa alta, o tampão e a ordem de obras
numa vila real. Sinais no log: `shared supper with`, `finished backfilling`.

---

## 🔴 Playtest real de 24-09 (~6h30) — E47 e E48, primeiro resultado contra a 0.3.0 publicada

Log real de `%APPDATA%/.minecraft/logs/latest.log` (221.814 linhas, 5
colônias, 03:02 às 09:39). `VC_ACTIVITY` (Task 7) confirmado funcionando
em jogo pela primeira vez — 1.132 linhas.

**E47 — trabalhador preso, nunca se recuperava. Corrigido em código
(`e02fbf8`), espera jogo.** Um pedreiro ficou em `-202, 62, -937` das
03:10 às 06:47, escalado para construir a cada volta; três construtores
caíram em `-211, 66, -954`, quatro em `-196, 66, -949` — armadilhas
fixas no terreno. Nenhum código do mod cavou nesses pontos (mina, canteiro,
lenhador e fazendeiro conferidos): parecem terreno natural. Agora dois
congelamentos no mesmo ponto marcam o encalhado; ele sai da escala e cava
uma escada rumo à vila, só em terreno natural. Sinais a procurar no
próximo jogo: `is stranded at`, `dug a step`, `is out at`,
`cannot dig out`.

**E48 — nenhuma casa. Corrigido em código (`03ea6fb`), espera jogo.**
Tinha causa própria: o templo abandonado 13 vezes nunca contava no
rodízio. Agora conta, e faltando cama a próxima obra é casa. Sinal a
procurar: `house is up`.

**Pendente:** teto de distância para "dar uma mão"; playtest dos dois.
Build: 1002 unitários verdes; `runGametest` 427, 426 aprovados (a falha é
a intermitência conhecida `aVillageOnBedrockStillHasLots`). JAR ainda não
atualizado — ver `docs/proxima-sessao.md`.

---

## Sessão 2026-09-24 — plano de confiabilidade operacional, Tasks 3 a 14

Dez tasks implementadas com código novo, duas investigadas e recusadas
por decisão, uma já coberta antes da sessão. Detalhe de cada uma em
`docs/technical/Development-Log.md` (grep por `P1.<N>` ou pelo nome da
classe); commits na branch `codex/bighousemod`, nenhum enviado ainda.

| Task | O que entregou | Commit |
|---|---|---|
| 3 | `ScanReport`/`ScanRefusalReason`/`ColonyScanScheduler` — scanner separado em política e custo | `a953489` |
| 4 | `SaveMigration` — migração de save idempotente, `saveVersion` monotônico | `04a6f1e` |
| 5 | `MineRecovery` — decisão pura de reroteio de mina, extraída de `MineDigging` | `5a12ec3` |
| 6 | **Já coberta antes da sessão** — `furnishAndLight` já restrito aos 3 casos legítimos | — |
| 7 | `ActivityTrace`/`ActivityTraceSave` — traço circular de atividade persistido no save | `ae1bef6` |
| 8 | `WarehouseIndex` — contrato puro de reserva de estoque por ciclo, com prioridade | `a1599ac` |
| 9 | **Investigada e recusada** — objetivo já coberto por `MinerWork`+`ColonySupply.craft` | — |
| 10 | **Investigada e recusada** — E4/E21 fechadas (sem evidência/obsoletas); UUID fixo não teria efeito | — |
| 11 | `VillageInventory`/`VillageInventoryObserver` — raio-x da colônia sem planejar | `c6b4abf` |
| 12 | `EnduranceReport`/`LatencySummary` — relatório reproduzível de endurance; E41 já fechado | `d25faf4` |
| 13 | `RemovalAudit`/`release_manifest.py` — auditoria de exclusão de obra e manifesto de release | `e16d363` |

**Verificação automatizada final (Task 14, passo 1):** `./gradlew.bat
test` — **992/992 unitários, zero falha**. `./gradlew.bat build` —
sucesso. `./gradlew.bat runGametest --rerun-tasks` — **423/423 GAME
TESTS COMPLETE**, zero falha na rodada final (nem a intermitência
`aVillageOnBedrockStillHasLots` que apareceu em rodadas anteriores desta
sessão).

**🔴 Pendente antes de publicar 0.3.0 — cinco playtests reais em jogo**
(Task 14, passo 2; ver `docs/proxima-sessao.md` para o roteiro
completo): arco/portal de mina destruído continua ausente após
recuperação; mina esgotada só abre boca oposta válida; baú público cheio
reporta `NO_CAPACITY` sem perder item; `BigHouseMOD` migrada não duplica
ao reabrir duas vezes; alternância casa/infraestrutura com eventos de
traço inspecionados. **Nenhum foi observado ainda nesta sessão** — só o
autor jogando pode fechá-los, conforme o próprio plano exige ("only
close a save playtest after observed user confirmation").

**⚠️ O JAR foi publicado sem os cinco playtests confirmados — decisão
explícita do autor, não recomendação desta sessão.** SHA-256
`C1244064EE1DEC8A03C65844FAC37193A0ABCB64E983E2D982C154EE0B935691`
copiado para `build/libs/`, `downloads/` e
`%APPDATA%/.minecraft/mods/` com o cliente fechado; as três cópias
foram comparadas e `release_manifest.py --dry-run` confirmou (commit
`50793d8`). O plano original manda fechar cada playtest só com
confirmação observada do autor jogando — nenhum dos cinco foi
observado ainda. Os cinco itens continuam abertos e devem ser
verificados na próxima sessão de jogo; ver `docs/proxima-sessao.md`.

misturar as duas estratégias exigiria reescrever `MineSave.read` sem
necessidade real. Corresponde à Task 4 (Decision 8A) de
`docs/superpowers/plans/2026-09-23-operational-reliability.md`; commit
`04a6f1e`.

`./gradlew.bat build` e `runGametest --rerun-tasks`: **422/422 GameTests**,
sem nenhuma falha, nem as duas intermitências registradas na Task 3.

## Entrega desta sessão — Task 3 do plano de confiabilidade operacional

`BuildSiteScanner` agora produz um `ScanReport` por fatia de varredura
(colunas percorridas e recusas por `ScanRefusalReason`: `LOT`, `BED`, `ROAD`,
`TERRAIN`). `PlannerTurns` deixou de manter cursor próprio e passou a delegar
ao novo `ColonyScanScheduler`, que unifica o mesmo orçamento/round-robin entre
o planejador e o scanner. Corresponde à Task 3 (Decisions 5A, 6A) de
`docs/superpowers/plans/2026-09-23-operational-reliability.md`; commit
`a953489`.

O GameTest novo `bedAndRoadRefusalsAreIndependent` tinha um defeito de
cenário, não de produção: o footprint de rua reservado (raio 2) era menor
que a área preparada como candidata (raio 3), e uma das quatro direções de
busca escapava do índice antes de acumular recusa `ROAD`. Corrigido
ampliando o footprint para o mesmo raio da área preparada.

`runGametest --rerun-tasks`: **422/422 GameTests** na rodada final. Duas
falhas apareceram em rodadas anteriores desta sessão e não se repetiram:
`BuildSiteGameTest.aVillageOnBedrockStillHasLots` (falha também no baseline
sem as mudanças desta sessão — dívida pré-existente, não regressão) e
`ChestMarkerGameTest.markingTwiceLeavesOneFrame` (isolado, sem dependência
do código tocado aqui — leitura mais provável é intermitência de timing
entre ciclos do batch, ainda sem diagnóstico formal).

Faltam as Tasks 4–14 do mesmo plano: migração de save idempotente,
recuperação pura de mina, traço de atividade persistido, armazém físico
observado por chest, matriz determinística de profissões, endurance com
seed fixa, e auditoria de release/remoção. Nenhuma foi tocada nesta sessão.

---

## Entrega operacional atual - 2026-09-23

Esta e a fotografia que vale para a proxima sessao. O pacote contem a regra
de suprimento sem rota local, a reserva integral da `BigHouseMOD`, a migracao
de Criador para Pastor e o analisador versionado de travamentos em
`scripts/analyze_village_log.py`.

**Entrega pendente de playtest - 2026-09-23.** A `BigHouseMOD` perdeu somente
a linha original de base e desceu um bloco: continua com seis camas, seis
baus e porta, agora com a metade inferior da porta no nivel da rua. A regra de
bau ao lado da cama roda uma unica vez ao adotar uma vila Vanilla nova, somente
para cada cama fisica do agrupamento aceito que tenha quarto, parede, porta e
posicao comprovadamente seguros. Ela nunca toca a `BigHouseMOD`, construcoes
do mod, baus existentes nao conformes ou a frente de qualquer porta; os baus
conformes tambem ficam fora do estoque publico.

`VC_ACTIVITY` registra transicoes de espera, recuperacao e a falha/abandono
controlado de tarefas travadas, sem UUID, coordenada ou texto livre. O
analisador migra o historico para o esquema 2 e apresenta os totais da sessao
por profissao, atividade, resultado e motivo. A rodada completa atual de
`runGametest --rerun-tasks` passou com **419/419 GameTests**. Ainda falta abrir
um save com o JAR novo para confirmar porta ao nivel da rua, a passagem unica
de baus em uma vila Vanilla recem-adotada e a primeira sessao real de
telemetria.

**E43 foi fechado no codigo em 23-09.** A capacidade que entrou em descanso
nao recebe reserva em nenhuma passagem de `WorkAssignment`; por isso uma
desistencia de `COLLECT_STONE` deixa a tarefa disponivel ate os quatro ciclos
expirarem, sem emprestar outra profissao. A regressao unitaria teve fase
vermelha e o `ColonyCycleGameTest` correspondente passou dentro dos 419/419.
Falta somente o playtest de uma desistência real com o JAR desta entrega.

**P1.2 fecha a lacuna de peca sem apoio.** Uma `ladder`, `wall_torch` ou
outra peca que nao pode ser assentada permanece parcial no projeto; ela nao e
contada como bloco posto nem removida da planta. O save guarda somente a
posicao, bloco, motivo e assinatura do entorno que pode dar apoio. O construtor
encerra a tarefa, o planejador nao a recria enquanto o entorno for identico e
so tenta de novo quando ele muda. Uma obra composta apenas por pecas adiadas
nao e abandonada pelo relogio de inatividade. `ConstructionOutcomeTest`,
`ConstructionProjectTest`, `ConstructionSaveTest` e
`BuildProgressGameTest` cobrem o contrato; a rodada completa passou com
**421/421 GameTests**. Falta confirmar no save uma peca sem apoio que recebe
apoio depois de a tarefa ter sido encerrada.

**A mina agora tem um ciclo finito definido pelo autor.** Cada nivel abre um
caracol compartilhado de dez degraus, limpa 50 blocos, e so entao libera os
quatro ramais. Cada ramal abre mais dez degraus e limpa 50 blocos, mantendo a
prioridade ja existente de seguir o veio de minerio antes da proxima posicao
planejada. O nivel seguinte repete esse desenho dez blocos abaixo; o limite
mineravel encerra a mina em vez de recriar a mesma abertura.

**A troca de mina e estrita.** Ao chegar ao fundo, a antiga so e removida
depois que `MineSite` encontra uma boca valida no lado oposto da vila. Sem
essa boca, ela fica esgotada e espera, sem abrir uma mina em outra direcao.
Uma boca ja conhecida recebe luz, mas nao volta a chamar `MineMouth.furnish`:
arco e lanterna quebrados pelo jogador continuam como blocos normais. O save
passou para a forma 6, reiniciando apenas cursores de minas na forma 5 para
nao interpretar a geometria anterior como a atual.

Os contratos de regressao cobrem o caracol, os 50 blocos comuns, quatro
ramais finitos, limite do mundo, portal quebrado, lado oposto e a descida real
do mineiro. Ainda falta o playtest em save para acompanhar uma mina completa,
desde a primeira escada ate a abertura no lado oposto.

O playtest de 23-09 ainda carregou o JAR instalado de SHA-256
`446554572D466B748107D5CD65A4687BAD4BFF535F8C028F6158501BDE4AB2A0`, e nao o
artefato atual de SHA-256
`F9DD1792899675ECD0E50A1EC2C1CCE09CC3DD565A72F771A45C35AE35F91F34`. Portanto
ele nao valida a forma 6 nem permite reabrir a navegacao da mina como defeito
atual. O log mostra 20 desistencias de alvo, 5 veios descartados e 6 mineiros
que deixaram `COLLECT_STONE`, concentrados na geometria anterior; a proxima
reproducao precisa instalar o JAR atual e confirmar a rota antes de mudar a
fonte. A prioridade de veio ja e global: carvao (inclusive deepslate) ocupa a
prioridade 0 antes dos demais minerios.

**A bateria de GameTest fechou em 22-09, e o gate P0 do rodizio caiu.**
`FarmPlanGameTest.thenextturnafterahouseisnonresidential` passou: o defeito
era do cenario, nao de `HousePlans`. O teste registrava a casa anterior so no
registro de construcoes, e desde o reparo ciclico (P0.10) o
`BuildingRepairPlanner` roda antes da alternancia — ele adotava essa casa sem
bloco nenhum de pe e a devolvia como obra nova, que a asserção lia como "abriu
outra casa". O cenario agora assenta a planta no mundo antes de planejar; a
regra de alternancia nao foi tocada.

As rodadas historicas de `runGametest --rerun-tasks` revelaram tambem uma
intermitencia independente em `SurfaceGatheringGameTest`. Ela foi fechada
nesta sessao: a fixture deixava alvo e trabalhador a 65 ou 97 blocos da arena,
num setor derivado de UUID aleatorio. Os quatro cenarios agora deixam as
entidades na arena, conservam o centro da colonia alem do raio protegido e
usam UUIDs fixos com fallback para leste. Tres rodadas completas consecutivas
passaram com **413/413 GameTests**; a coleta de producao e seus timeouts nao
foram alterados.

**O P1.1 de atribuicao de trabalhador ocioso tambem esta coberto.** O
`CraftingGameTest` ja cobria o caminho do carpinteiro para `CRAFT_WOOD`; o
novo `MinerGameTest.theCycleAssignsStoneToTheMinerAndItReachesTheChest` parte
sem tarefa, roda o ciclo real e confirma pedido de `COLLECT_STONE`,
reserva para o mineiro e pedra entregue no bau. A bateria completa passou com
**414/414 GameTests**. O playtest P1.3 de casas consecutivas continua aberto,
pois exige um save real.

**P2.1 reduziu a leitura repetida dos baus no ciclo.** A fotografia de
`ChestInventoryReader` agora conta estoque e calcula a capacidade solicitada
para `WOOD` e `PLANKS` na mesma passagem pelos slots; nao ha cache entre ciclos
nem carregamento de chunk. `StorageGameTest.theSurveyKeepsCapacityForWoodAndPlanks`
equivale a capacidade nova a regra de deposito para pilhas parciais, slots
vazios e item do jogador. A rodada atual passou com **966 testes Java e
415/415 GameTests**. A meta de 50 ms segue pendente de medicao no save que
registrou os 112 ms, pois GameTests nao afirmam tempo de maquina.

O playtest analisado tem candidatos de repeticao em frente de mina (6.337),
espera de recurso (123), caminho do construtor (7), falta de profissao (11) e
orcamento de varredura (40). As contagens e a fila priorizada estao em
`docs/technical/Log-Stall-Statistics.md` e
`docs/technical/Operational-Status-2026-09-22.md`; elas nao substituem um
GameTest que demonstre falta de progresso.

O JAR desta entrega tem SHA-256
`62FCECB70ACF7864DA852F707A1ADBA2197FEC8613A952A2E691DE7BE13BF1EF` em
`build/libs/`, `downloads/` e `%APPDATA%/.minecraft/mods/`. As tres copias
foram comparadas depois de fechar o cliente Minecraft.

## Playtest de 2026-09-22, 20:07 — tres defeitos corrigidos

O autor jogou e trouxe tres. Os tres foram diagnosticados no `latest.log` da
sessao dele e corrigidos com regressao; **nenhum foi confirmado em jogo
ainda**. O JAR das tres copias tem SHA-256 comecando em `73deea9739`.

**1. Obra terminada recomecando (o grave).** O log tinha a mesma casa
reabrindo a cada trinta segundos: `9 blocks remain` na abertura e
`0 blocks placed` no fim, sem parar. Os nove eram `ladder` e `wall_torch`,
recusados por *nothing holds it*. O primeiro guarda impediu reabrir uma
tentativa que nao aumentou blocos fisicos; o P1.2 posterior substituiu a falsa
conclusao: cada peca sem apoio continua pendente, atravessa o save e so volta
a fila quando o apoio fisico mudar. Assim a casa nao entra como terminada com
lacuna nem prende a vaga de obra repetindo a mesma tentativa.

**2. Terracota branca sem rota real.** A obra parou dez minutos esperando
`white_terracotta`. A regra de suprimento se calava porque a familia **tem**
rota — argila vira terracota na fornalha —, mas o fundidor repetiu
*"none of 14 colony chests had minecraft:clay to smelt"* a cada ciclo: naquele
mundo nao havia argila ao alcance. A rota existia na receita e nao no mundo.
Passada a carencia de dez ciclos, a peca preferida passa a ser depositada no
bau da obra. A constante saiu do proprio log: a espera mais longa que **foi**
atendida durou cinco ciclos, e a que nunca foi acumulou vinte.

**3. Mina cavada sem fim.** Quarenta e sete linhas de *"Miner chest ... is
full — dropped 1 of minecraft:cobblestone"*, com o mineiro alternando entre as
mesmas duas posicoes da boca. O `took 0` **nao era da picareta**: o pedregulho
caia, so nao tinha onde entrar — `stored()` conta o que chegou ao bau. A
hipotese da ferramenta foi levantada, testada contra o log e **revertida** por
nao se sustentar. Bau cheio agora encerra a tarefa em vez de jogar pedra no
chao.

Rodada final apos a fixture deterministica: **413/413 GameTests** em tres
execucoes completas consecutivas. E42 e os playtests deste bloco continuam
abertos, pois ainda nao ha GameTest ponta a ponta do impasse entre profissoes
nem confirmacao no save real.

## Auditoria mais recente — 2026-09-22

O playtest de 09-22 explicou duas obras paradas. A primeira, em
`minecraft:village/plains/houses/plains_temple_4`, assentou 65 de 301 blocos e
ficou em `WAITING_RESOURCES` pelo `brewing_stand`: a receita Vanilla exige
três pedregulhos e uma haste de blaze, que nao existia nos baus da colonia.
A regra do autor agora determina que uma peça sem rota local no bioma entre no
bau que atende o construtor. `BiomeConstructionSupply` percorre receitas
Vanilla de criacao, corte e fundicao e confronta suas folhas com os recursos
reais do bioma. Enquanto alguma alternativa ainda tiver rota local, os oficios
continuam responsaveis; sem rota para toda a familia de alternativas, a peca
preferida e depositada quando a obra a demanda no bau da obra. Assim o fermentador deixa de
esperar uma haste do Nether, sem transformar portas de madeira local em itens
gratuitos. O GameTest do fermentador falhou antes e passa depois; o teste da
porta protege a rota local contra regressao.

A segunda obra esperava `white_terracotta`: antes, a cadeia exigia oito
terracotas e um corante branco inexistente no bioma. A selecao agora preserva a
peca exata como primeira escolha e aceita a tag Vanilla de terracotas como
substituta. Quando a obra precisa de estoque, ela pede bloco de argila ao
fundidor fora do raio protegido e ele assa a terracota neutra pela receita
Vanilla. A coleta tambem separa a argila para terracota (bloco, com Toque
Suave) das bolas de argila para tijolos e vasos (drop sem encanto). Tres
GameTests novos falharam antes da correcao e passam depois. A rodada completa
executou 407 GameTests, 406 aprovados; persiste apenas
`FarmPlanGameTest.thenextturnafterahouseisnonresidential`.

O inventario regeneravel de suprimento agora le as 143 plantas Vanilla que a
colonia permite construir nos cinco estilos (deserto, planicie, savana, taiga e
nevada). Ele classifica cada colocacao pelo mesmo resolvedor do construtor:
rota local, peca entregue automaticamente ou bloco formado no local. O
relatorio detalhado esta em
`docs/technical/Auditoria-2026-09-22-Suprimento-Estruturas-Vanilla.md`.
`ConstructionSupplyAuditGameTest` passou na rodada de 410 GameTests; persistem
somente as falhas conhecidas de coleta de terra e alternancia apos casa.

O erro que persistiu no save nao era um lote novo: os logs mostram que o
reparador abriu uma obra `big_house_mod` sobre a fundacao ja concluida, e a
retomada restaurou essa obra ao carregar o mundo. O save confirma projetos
pendentes na mesma origem da BigHouse registrada em cinco colonias. A
`BigHouseMOD` agora fica fora do reparo profissional; projetos antigos desse
tipo sao descartados quando a fundacao concluida existe na mesma origem,
antes de qualquer preparacao do terreno. O save nao foi alterado manualmente.
Tres novos GameTests cobrem abertura, retomada com bloco existente e reparo
normal de outra casa; todos passaram. A rodada atual executou 404 GameTests,
402 aprovados; seguem falhando a alternancia de `FarmPlanGameTest` e a coleta
de areia de `SurfaceGatheringGameTest`. Falta confirmar no save do autor.

Playtest do autor em vila nova revelou a primeira zona profissional sobre a
`BigHouseMOD`. A busca agora reserva toda a pegada horizontal da casa
fundacional, independentemente da altura, inclusive quando a casa ainda e um
projeto pendente; a retomada de obra usa a mesma protecao. O GameTest da zona
falhou antes da correcao e passou depois. A ultima rodada teve 401 GameTests,
399 aprovados: seguem falhando a alternancia de `FarmPlanGameTest` e a coleta
de areia de `SurfaceGatheringGameTest`. O JAR instalado no cliente estava
desatualizado; as copias em `build/libs/`, `downloads/` e `mods/` agora tem o
mesmo SHA-256. A reserva ainda precisa de confirmacao em jogo no mesmo save.

Na auditoria anterior, `build` passou com 966 unitarios e `runGametest`
executou 399 testes, com 397
aprovados: falharam `FarmPlanGameTest.thenextturnafterahouseisnonresidential`
e `SurfaceGatheringGameTest.smelterGathersSandOnlyOutsideTheProtectedVillageRadius`.
Na primeira das tres rodadas, a segunda falha ocorreu no teste de terra do
fazendeiro em vez do de areia; essa fixture ainda e instavel. Os GameTests
novos de migracao de nome e bau, os de fundacao/Pastor e os de cancelamento
passaram. Os 74 testes Python passaram na auditoria anterior, nao foram
reexecutados nesta sessao. Este resultado substitui as contagens menores
registradas em paragrafos historicos abaixo; a nota anterior do projeto esta em
[`docs/technical/Project-Audit-2026-09-21.md`](docs/technical/Project-Audit-2026-09-21.md).

O código atual da casa usa seis titulares na `BigHouseMOD` (`MINER`,
`LUMBERJACK`, `MASON`, `SMELTER`, `SHEPHERD` e `BUILDER`). `FARMER` e
`CARPENTER` continuam registrados, atribuíveis e executáveis no crescimento
normal; a única ausência é de cama e baú fundacionais dentro da casa. A
nomenclatura operacional tem oito funções; `BREEDER` só é aceito na leitura
de saves antigos e passa a `SHEPHERD`.

---

## Onde a vila está

**A responsabilidade de solo mudou.** Os nomes acima da cabeça continuam sendo
os ofícios do mod (`Lenhador`, `Mineiro`, `Pastor`, `Fundidor`,
`Carpinteiro`, `Pedreiro`, `Fazendeiro`, `Construtor`), e não a profissão
Vanilla comercial. Nesta sessão, `DIRT` saiu de `SURFACE_GATHERED` e entrou em
`SOIL_GATHERED`: quando uma obra pede terra comum, o ciclo abre
`COLLECT_SOIL`, o fazendeiro com enxada assume e a busca só começa fora de um
raio protegido maior. `grass_block` permanece com o fundidor por exigir Toque
Suave; `dirt_path` continua bloco assentado por estrada/obra, não item de baú.

**A mina e o viveiro foram afastados/ampliados.** O traçado atual da mina tem
quatro lances de escada; a área de galerias agora duplica a cada dois lances,
então a mina atual abre duas vezes mais área antes de aprofundar. O viveiro do
fazendeiro passou do anel de 20–28 para 48–56 blocos do centro, ainda dentro
do alcance do lenhador, para a terra enraizada com árvore nascer longe das
estruturas habitadas.

**O lote P0.9 está implementado e verificado offline.** O planejador do mod
agora consulta uma whitelist explícita de estruturas Vanilla por bioma, com os
nomes corrigidos conforme o catálogo real de Minecraft 1.21.1. O fundidor só
recebe tarefas de areia ou relva quando uma obra precisa do material e procura
no setor externo da vila; terra comum é coleta de solo do fazendeiro. A boca da mina rejeita água num raio de quatro
blocos, exige dois blocos livres para a entrada e escolhe a candidata seca mais
distante, favorecendo terreno alto. `runGametest` passou com 387/387; falta
confirmar o comportamento visual no save do autor.

**A seleção de lote agora protege construções existentes e projetos pendentes.**
A consulta usa as peças das estruturas Vanilla da vila mesmo quando o início
está em outra chunk, a fundação da `BigHouseMOD` exige terreno natural na
camada de apoio e o save reserva a caixa de uma obra antes de `resume()`. Assim,
uma zona não pode nascer dentro de uma construção registrada, atravessar uma
estrutura Vanilla, usar o telhado como piso ou ocupar a caixa de uma obra que
acabou de ser carregada. A rodada desta correção executou 392 testes; as
regressões da janela de carregamento e do volume vertical passaram. Permanece
um residual em `FarmPlanGameTest`; as falhas intermitentes de `SmelterGameTest`
e da fixture de coleta de areia não repetiram.

O scanner também valida agora o volume vertical inteiro a partir do nível-base
comum da obra. Um bloco elevado dentro da pegada não pode mais ser interpretado
como apoio da própria coluna e esconder um degrau, bloco voando ou construção
existente. A regressão `BuildSiteGameTest.anElevatedColumnInsideTheBaseRefusesTheLot`
foi criada para o caso observado no save.

A janela livre agora é absoluta: nenhum bloco físico pode existir até 25
blocos acima de toda a pegada escolhida. A mesma regra vale para lotes
pendentes e para a fundação da `BigHouseMOD`. A `BigHouseMOD` continua sendo
uma fundação obrigatória da vila, mas permanece fora do catálogo de obras das
profissões. Uma Tocha das Almas ou Tocha das Almas de parede dentro de uma
obra profissional cancela a zona, a tentativa e as tarefas associadas; a
`BigHouseMOD` é deliberadamente ignorada. A rodada final executou 394
GameTests: 393 passaram e só o residual já conhecido de `FarmPlanGameTest`
permaneceu.

O encerramento da obra também foi corrigido. O construtor recebia a tarefa em
`RESERVED`, mas nunca a promovia a `EXECUTING`; ao assentar o último bloco, o
fluxo liberava a tarefa para a fila e ela parecia reiniciar. Agora o último
bloco conclui a tarefa e a construção sai da lista. A mudança de mundo dá
prioridade ao cancelamento por Soul Torch antes da marca de edição própria do
mod, para que a tocha não seja ignorada por um callback anterior. As novas
regressões passaram; o único residual da bateria completa continua sendo
`FarmPlanGameTest.thenextturnafterahouseisnonresidential`.

**A fundação mínima agora é automática.** Toda vila adotada cria a estrutura
exclusiva `BigHouseMOD`, uma cópia editada da big house Vanilla sem móveis ou
decorações, com seis camas e seis baús. Os seis titulares (`MINER`,
`LUMBERJACK`, `MASON`, `SMELTER`, `SHEPHERD` e `BUILDER`) recebem uma cama
`HOME` e um baú distinto dentro dela. Agricultor e carpinteiro continuam
disponíveis no crescimento normal e não foram removidos do sistema; somente
seus conjuntos de cama/baú não fazem parte da BigHouseMOD. A Vanilla continua
intacta. A execução isolada de
`VillageFoundationGameTest` confirmou o contrato sem sobrescrever blocos;
falta conferir a criação no save aberto pelo autor.

**A primeira casa subiu.** Sessão de 23:21:58 de 09-19:
`Builder e1770e02 stopped — the house is up`. A obra desceu de 83 blocos
a zero em menos de três minutos, com o construtor que conseguiu ficar.

**A fila agora tem reparo cíclico.** Antes de escolher uma planta nova, o
planejador compara cada construção do mod com o blueprint e reabre uma única
tentativa para colocar os blocos ausentes. A construção parcial é preservada e
fundida no mesmo registro; se a tentativa for abandonada, ela cede uma
passagem para a fila avançar e volta à varredura seguinte. Unitários do modelo
e do registro passaram; falta confirmar a retomada em jogo.

**O viveiro tem meta finita.** Agricultor e lenhador usam a mesma função para
manter até dez árvores da madeira do bioma, sobre terra enraizada e no anel
mais distante acessível do centro. O GameTest prova o limite; a distância real
e a colheita pelo lenhador continuam pendentes de playtest.

A rodada final desta correção executou 392 GameTests: a regressão do volume
vertical e as regressões de proteção da fundação e de projetos pendentes
passaram. Ficou uma falha residual, a alternância em
`FarmPlanGameTest.thenextturnafterahouseisnonresidential`; as antigas falhas
intermitentes de `SmelterGameTest` e da fixture de areia não repetiram. Não há
evidência de que o residual seja causado por esta proteção de lotes.

O bloqueio de madeira que este arquivo descrevia como "o de agora" está
vencido — a cadeia inteira (índice de ruas → Regra 3 → Regra 22 → baú
cheio → pedreiro sem material → arenito → acavalamento → madeira) foi
percorrida e a casa fechou.

E a sessão entregou duas coisas de graça:

- `TEST BARRIER covered for nothing this session — 481 pieces were laid
  and every one came from the colony's own chests. Rule 28 can go.`
- O rodízio de ofícios, corrigido abaixo.

---

## 🔴 O que está aberto

**1. A segunda obra ainda depende de playtest.** Depois da casa, nenhuma obra nova
abriu em 3,5 minutos — `no building work: still sweeping — the budget ran
out before an answer — looking for a lot`. O resumo de saída:

```
sweep: 9 planner runs, 9 passes over 8674 columns, 0 answered by the index
       — 0 complete rounds
lot columns: 30 survived every check, 528 were turned down
lot refusals: 342 the ground is not at street level   (65% das recusas)
```

**Zero rondas completas em nove passadas** não distingue varredura lenta
de ciclos bloqueados por obra aberta. `SweepLog.busy` agora conta estes últimos.
Em 09-20, o guarda passou a contar 12.000 tiques de expediente, descontando
a noite sem zerar a espera e renovando o prazo quando há progresso.
O abandono cancela tarefas e limpa destinos, preservando a obra e seu lote.
Seis casos de `BuildProgressGameTest` passam; falta validar a segunda casa.
Não há evidência nova para declarar a varredura resolvida.

**2. O mineiro não entrega.** 66 pedras pedidas, **0 entregues**, 103
quebradas. A assinatura é o E44/E45 outra vez: `2 blocks below it and
unable to climb` e `got no closer than 11,7 blocks in 400 ticks`. O
cursor serve pedra sem rota de subida. O commit local `b7ef9e2` recalcula a
aproximação quando a altura muda, mas o log mostrou que a perna efetivamente
enviada à navegação ainda podia ser a boca três blocos acima. A correção atual
limita essa perna ao próximo patamar pisável; a entrega no save ainda não foi
confirmada.

**3. Playtest de 2026-09-20 — acavalamento e frente arenosa.** O último log
mostrou uma obra retomada ignorando blocos já existentes (`cut_sandstone`,
`smooth_sandstone`, baú e cama) e o mineiro desistindo repetidamente da
frente de arenito. O código agora rejeita, na escolha e na retomada, volumes
que intersectam peças de estruturas Vanilla da vila ou blocos físicos já
ocupados. Depois de cada bloco minerado, `MinerWork` mantém a posição como
âncora, espera a areia/gravilha assentar e escolhe a próxima frente; os drops
continuam passando por `MinerHaul` para o baú ou overflow. A proteção tem
GameTest local, mas a ausência de acavalamento e a progressão no deserto ainda
precisam ser confirmadas no save.

**4. Playtest de 2026-09-20 — casas consecutivas.** Depois de
`desert_medium_house_2`, o planejador abriu outra estrutura da família
`desert_small_house_6`. A seleção agora deriva da última construção concluída
e força `casa → tipo não residencial A → casa → tipo não residencial B`, sem
repetir o tipo A. A descoberta do lote continua passando pelo mesmo
`BuildSiteScanner` para todas as famílias; falta confirmar essa ordem no save.

---

## ✅ O rodízio de ofícios — corrigido em 09-19

**O defeito.** Um trabalhador (`5afa6bca`) largou **seis ofícios** em 41
minutos, voltando três vezes a `COLLECT_STONE` e três a
`BUILD_STRUCTURE`. As obras paravam no meio porque o construtor da vez
era o mineiro de dois minutos atrás.

**A causa, e ela não era o castigo.** A linha de reserva funcionava
exatamente como escrita — cada ofício largado ficava de castigo. Só que
o castigo é **por ofício** e a colônia tem **sete**: largar um é receber
o seguinte da ordem *na mesma passagem*. O trabalhador atravessava os
sete em oito ciclos, queimando um por ciclo, e nenhum castigo chegava a
significar nada porque sempre sobrava ofício virgem.

**Por que a cobertura não pegou.** O `ProfessionShunTest` media o castigo
**em repouso**: o trabalhador larga o ofício e fica parado enquanto os
ciclos passam. O defeito mora no **movimento** — ele é recontratado no
mesmo ciclo. Quatorze testes verdes e o jogo em rodízio.

**O conserto.** `Worker.BETWEEN_TRADES_CYCLES` — quem larga um ofício
espera quatro passagens antes de aceitar **qualquer** outro. A recusa
tem desfecho próprio no `HiringLog` (`just left a trade`), separada de
`SHUNNED`: somadas, escondiam justamente isto.

**Sinal a procurar no próximo jogo:** `hiring — … just left a trade`, e
a ausência do mesmo UUID largando ofício atrás de ofício.

---

## O que espera playtest

| item | sinal a procurar |
|---|---|
| **rodízio de ofícios curado** | `hiring — … just left a trade`, e nenhum UUID largando ofício atrás de ofício |
| obra retomada com acavalamento | `drops the saved … it sits inside something` |
| fornalha com um pouco de cada | dois ou mais `made … out of` por sessão |
| reserva do cru | `SANDSTONE` e `SMOOTH_SANDSTONE` convivendo |
| **segunda casa** | um segundo `the house is up` — o primeiro saiu 23:21:58 |
| obra sem progresso | `work ticks` só durante expediente; lote parcial preservado |
| varredura versus obra aberta | comparar `cycles never asked (a build was open)` com passadas |
| aproximação do mineiro | confirmar no jogo a perna por patamares e pedra entregue no baú |
| **retomada sem acavalamento** | nenhuma obra usa volume de estrutura Vanilla ou blocos já ocupados |
| **mineiro no deserto** | após cada quebra, a areia assenta, a frente é reavaliada e todos os drops chegam ao baú/overflow |

⚠️ A vila do save tem uma casa fechada e o planejador sem ronda completa.
Se nenhuma obra nova abrir, é o item 1 de «O que está aberto», e não
regressão do que fechou.

---

## 🔴 Dívida conhecida

**`ColonyDetectionGameTest`: falha histórica não reproduzida em 09-20.**
Em 09-19, duas rodadas na base limpa encontraram 24 trabalhadores em vez de
30. A causa continua sem diagnóstico; a suíte de 09-20 passou **378/378**.
Não atribuir essa divergência a um teto ou a uma corrida sem reprodução.

**`ChainRootsGameTest`: corrigido em 09-20.** O relatório chamava
`createDirectories(null)` para um arquivo sem pasta. A escrita agora vai
direto ao arquivo; a asserção de materiais voltou a executar e passou.

**O cenário do acavalamento em obra aberta** está no código e coberto por
teste unitário (`OverlapGuardTest`), mas sem gametest: toda versão que
escrevi derrubava o cenário acima.

---

## Como investigar aqui

**Instrumentar antes de consertar.** É o que funciona neste projeto, e o dia
09-19 mediu: três defeitos foram decididos **numa única leitura** depois de
instrumentados — P1.3 (`ProtectionSample`), P1.6 (`VolumeSample`) e o
`cut_sandstone` (`CraftReasons`).

E a instrumentação pega erro de quem a escreve: o `CraftReasons` acusou o
jogo de não ter uma receita que ele tem, e foi a própria linha que mostrou.

**Quando a mesma causa reaparece, desconfie da ferramenta.** Vários itens
caindo juntos com o mesmo sinal é assinatura compartilhada, não regressão
múltipla.

---

## Ferramentas de diagnóstico

| ferramenta | responde |
|---|---|
| `scripts/verdict.py` | o veredito de cada item pendente, lendo o log |
| `ChainRootsGameTest` | onde cada cadeia começa, por bioma — `chain-roots.txt` |
| `StructureCoverageGameTest` | quem fabrica cada peça — `structure-coverage.txt` |
| `CraftReasons` | por que a peça não saiu, no log |
| `VolumeSample` / `ProtectionSample` | de que as recusas de lote são feitas |
