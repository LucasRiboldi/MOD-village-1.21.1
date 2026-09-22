# STATE — 2026-09-22

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Este arquivo chegou a **2.277 linhas** antes de
> 09-19: ele passou a atrasar a sessão que devia orientar.

---

## Entrega operacional atual - 2026-09-22

Esta e a fotografia que vale para a proxima sessao. O pacote contem a regra
de suprimento sem rota local, a reserva integral da `BigHouseMOD`, a migracao
de Criador para Pastor e o analisador versionado de travamentos em
`scripts/analyze_village_log.py`.

Verificacao desta entrega: `test --rerun-tasks` passou com 966 testes Java;
`python -m unittest discover -s tests` passou com 76 testes.

**A bateria de GameTest fechou em 22-09, e o gate P0 do rodizio caiu.**
`FarmPlanGameTest.thenextturnafterahouseisnonresidential` passou: o defeito
era do cenario, nao de `HousePlans`. O teste registrava a casa anterior so no
registro de construcoes, e desde o reparo ciclico (P0.10) o
`BuildingRepairPlanner` roda antes da alternancia — ele adotava essa casa sem
bloco nenhum de pe e a devolvia como obra nova, que a asserção lia como "abriu
outra casa". O cenario agora assenta a planta no mundo antes de planejar; a
regra de alternancia nao foi tocada.

Tres rodadas de `runGametest --rerun-tasks` em sequencia: **409/410** antes do
conserto (so esta falha), **408/410** depois (o teste verde, e as duas de
`SurfaceGatheringGameTest` caindo juntas) e **410/410** na terceira, sem
nenhuma alteracao de codigo entre a segunda e a terceira. As falhas de
`SurfaceGatheringGameTest` ficam, portanto, como instabilidade de fixture, e
nao como gate: elas caem e passam sozinhas. Cairem **as duas juntas** aponta
raiz unica — os dois cenarios leem `FarthestVillageSector.farthestLoadedSector`,
que depende de quais chunks a bateria inteira deixou carregados. Nao alterar
coleta nem timeout antes de uma reproducao deterministica dessa fixture.

O playtest analisado tem candidatos de repeticao em frente de mina (6.337),
espera de recurso (123), caminho do construtor (7), falta de profissao (11) e
orcamento de varredura (40). As contagens e a fila priorizada estao em
`docs/technical/Log-Stall-Statistics.md` e
`docs/technical/Operational-Status-2026-09-22.md`; elas nao substituem um
GameTest que demonstre falta de progresso.

O JAR sincronizado em `build/libs/`, `downloads/` e
`%APPDATA%/.minecraft/mods/` tem SHA-256
`A417310243A6818D4AED39FABB0A45AD0B6880E66A8C53393C5750AB55191C39`.

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
