# STATE — 2026-09-15

> Arquivo de estado vivo. Sobrescreve, não acumula.
> Se passar de 150 linhas, algo está errado — P0 não está fechando.

---

## P0 — bloqueadores

Um por vez, teste antes de seguir. Nada mais entra antes de fechar.

| Item | Descrição | Estado |
|---|---|---|
| P0.1 | O planejador não acha lote | ✅ entregue 09-11, **visto em jogo** |
| P0.1-b | O caminho de terra não sai de baú | ✅ entregue 09-11, **espera sessão** |
| P0.1-c | A recusa de lote diz por quê | ✅ entregue 09-11, **espera sessão** |
| P0.3 | Mineiro → armazenamento → fundidor | ✅ conserto entregue 09-11, **espera sessão** |
| P0.5 | Perda de item por inventário cheio (E3) | ✅ entregue 09-11, **espera sessão** |
| P0.6 | A enxurrada da areia calou | ✅ entregue 09-11, **espera sessão** |
| P0.7 | Elegibilidade simplificada de lotes | ✅ entregue 09-15, **espera playtest** |

**P0.7 — politica aplicada em 2026-09-15.** Piso solido disponivel e candidato a lote; pedra, gravilha e terracota nao sao recusadas pela composicao. Estrada exige material oficial mais `ROAD_AREA`; gravilha ou terracota fora da reserva continuam elegiveis. O scanner nao terraplana nem muda o mundo. Ver ADR-017.

**Lote 2 — continuidade do mineiro, primeira fatia integrada em 2026-09-15.** Ao encerrar a tarefa, `MinerWork.tick` agora libera a claim do ramal no mesmo tique em que remove o job. `MinerWorkLifecycleTest.aClosedJobReleasesItsMineClaimOnTheNextTick` prova que nem o job nem a claim sobrevivem. Isto corrige apenas a limpeza de claim; alvo inalcançavel, ramo bloqueado, veio exaurido, fluido e retomada apos backoff continuam na matriz aberta de recuperacao.

**JAR atual 0.3.0:** `build`, 847 unitarios e 330/330 GameTests passaram. O artefato inclui P0.7, a limpeza imediata da claim, as duas correcoes do playtest de 09-15 (toco orfao e minerio recusado), a retirada do bau da boca da mina e as duas otimizacoes do planejador. Copiado para `downloads/` e `%APPDATA%/.minecraft/mods/`; SHA-256 nas tres copias: `79D9774C4DCA09209742BAFC856ADDD5BA340BD71BE9DF25EE3F7FA328E04E8D`. Falta apenas playtest.

---

## Playtest de 2026-09-15, 21:50 — "nenhuma casa crescendo"

Log: `latest.log`, 21:30-21:50, colonia `111d6ee5`, JAR `1c362b8b`.

**A correcao da obra orfa funcionou:** as 21:42:41 o acougue foi liberado com
a linha nova (`lets go of ... outside the 64-block radius`).

**DIAGNOSTICO CORRIGIDO NO MEIO DO CAMINHO.** A primeira leitura do log foi
**errada** e esta registrada aqui porque custou uma tentativa de patch. Eu
concluira "impasse fechado: a rua nao cresce", e escrevi um patch que largava
o indice de ruas ao fim da volta vazia. **Onze GameTests quebraram** e
mostraram o porque: a Regra 15 (crescer a rua) roda exatamente nesse ponto — o
fim da volta sem lote — e o patch consumia o momento dela. Tudo foi revertido.

**O que o log diz de verdade:** a rua **cresce**. Tres extensoes em 20
minutos, as 21:45 (2 blocos oeste), 21:46 (3 norte) e 21:47 (5 sul). O sistema
funciona; esta lento demais para se ver.

**A causa da lentidao e a fila que eu criei na vespera.** A colonia teve vez do
planejador **16 vezes em 20 minutos**, porque `PlannerTurns` reparte 29
colonias em oito por ciclo. E **28 daquelas 29 estavam dormentes** — o log
registra uma unica colonia reportando atividade. A fila gastava a vez com quem
nao tinha o que fazer, enquanto a colonia observada esperava quatro ciclos.

**Correcao 1 — a colonia que o jogador ve fura a fila.** `PlannerTurns` ganhou
a sobrecarga `chooseFrom(active, watched)`, e `VillageDetectionHandler`
calcula `watched` pelo mesmo `SEARCH_RADIUS` da vila. **A cota nao muda**: as
observadas ocupam vagas dela, nao vagas a mais — alargar o orcamento
devolveria o pico de 214 ms pela porta dos fundos. O cursor do rodizio avanca
so pelas que entraram por rodizio, entao ninguem fica para tras.

**Correcao 2 — obra abandonada deixa de contar como casa.** Defeito **meu**, da
vespera: `smallestFirst` perguntava se `BUILDINGS.ofColony()` estava vazio, e
`WaitingWork.giveUp` registra a obra largada como `Building` para o lote nao
parecer livre. O save do mundo tem **56 buildings e zero** `house is up`, entao
a preferencia pela planta pequena **nunca dispararia**. `Building` ganhou o
campo `finished`, e so `BuilderWork.complete` o marca verdadeiro.

**Save retrocompativel:** a chave `finished` ausente vale **terminada**. O save
anterior a 09-15 nao a tem, e nele quase toda construcao e casa de verdade —
o registro so passou a receber obra abandonada em 09-12. Ler as antigas como
inacabadas faria toda vila ja construida voltar a preferir a planta pequena. O
preco e a obra abandonada de um save antigo contar como casa naquela colonia,
e isso se apaga na primeira casa que ela terminar.

**Verificacao:** `build` verde com **847 unitarios** (7 novos) e **330/330**
GameTests, bateria repetida **tres vezes, todas limpas**. **Pendente: validacao
em jogo.**

**Proxima frente decidida pelo autor:** menu do mod comecando por comando
(`/vc log`, `/vc watch <profissao>`), nao por tela. Ver a pesquisa na secao
seguinte.

---

## Playtest de 2026-09-15, 21:02 — nenhuma construcao nascendo

Log: `%APPDATA%/.minecraft/logs/latest.log`, 20:44-21:02, colonia `111d6ee5`.

**As otimizacoes do planejador funcionaram.** A Regra 3 caiu de 126.315 para
28.333 recusas, e o aviso `Colony cycle took` saiu de varios por minuto para
**um** na sessao inteira (214 ms as 20:45:35, no arranque). O gargalo do tique
deixou de ser o problema.

**A causa de nada nascer eram outras duas, e ambas foram corrigidas.**

**1. A obra ficou fora do raio quando o centro da vila derivou.** As 20:54:35 a
colonia abriu `plains_butcher_shop_2` em `638,65,-2793` — 382 blocos — e sete
minutos e meio depois continuava em *"382 blocks left"*, sem **um unico bloco
assentado**, segurando a vaga unica: *"no building work: one is already open"*.
Nao era falta de material: o estoque tinha 2.743 tabuas e 621 pedregulhos.

A aritmetica esta no proprio log. O centro da vila **nao e estavel** — a mesma
sessao registrou oito centros, de `616,-2863` a `640,-2891`, porque ele e
recalculado das camas vistas e camas entram e saem de chunk carregado. A obra
nasceu com o centro em `625,-2854`, a **62,4** blocos: dentro do raio de 64. O
centro que prevaleceu, `637,-2871`, a deixa a **78** — fora do raio, e portanto
fora do alcance de qualquer trabalhador.

Ninguem reclamava porque o relogio de paciencia so conta para
`WAITING_RESOURCES`, e esta obra estava em `BUILDING`; o passo do construtor sai
em silencio quando o aldeao nao esta em chunk carregado. Obra viva,
inalcancavel, calada e ocupando a vaga unica, as quatro coisas ao mesmo tempo.

`ConstructionProject.isOutOfReach` (Core, sem mundo) responde a pergunta, e o
planejador larga a obra antes do relogio de paciencia. **A planta nao leva a
culpa:** `WaitingWork.giveUp` ganhou o parametro `blamePlan`, porque marcar a
casa pelo primeiro material restante acusaria um item inocente — nao faltou
material nenhum. A casa pela metade e o lote continuam ocupados, como no
abandono por paciencia.

**2. A primeira casa da colonia passa a ser a menor.** Decisao do autor:
*"dar preferencia para a primeira ser uma casa pequena"*. Era a terceira sessao
seguida em que a maior planta do catalogo trava a vila antes de a primeira casa
existir. `HousePlans.smallestFirst` poe a menor na frente **enquanto a colonia
nao tem nenhuma construcao de pe**; levantada a primeira, a Regra 25 volta
inteira. A lista e **reordenada, nao encurtada**: se a pequena nao couber
naquele lote, a varredura desce para a seguinte.

A Regra 25 (2026-08-20) continua valendo e o motivo dela segue real — exigir a
casa grande em toda parte fez a vila parar de crescer. O que entrou e uma
excecao de **arranque**, e nao uma inversao: inverter de vez faria a vila virar
um bairro de cabanas.

**Verificacao:** `build` verde com **840 unitarios** (7 novos) e **330/330**
GameTests. Bateria repetida **tres vezes**: duas limpas e uma com o AUD-001
conhecido (`smeltergathers...` / *"fundidor criado no setor nao esta registrado
no ServerWorld"*), que nao tem relacao com construcao. **Pendente: validacao em
jogo das duas.**

---

## Playtest de 2026-09-15 — dois defeitos vistos e corrigidos

Log: `%APPDATA%/.minecraft/logs/latest.log`, 19:41–19:49, colonia `111d6ee5`.

**1. Lenhador deixava toco de 3 a 4 troncos de pe.** Relato do autor:
*"lenhador esta deixando 3 a 4 blocos de troncos sem cortar, todos troncos
devem ser cortados e replantar"*. O log mediu: **114** recusas `Not a tree`,
com a moda exata em **4 troncos** (34 dos 114). O lenhador **nao** abandonava
arvore no meio — as 18 que comecou, terminou, e o guarda de imobilidade de
09-12 segurou (stall 0–4). Os tocos nunca foram cortados: a regra da copa
viva (`TreeHarvester.plan`) recusa tronco sem folha `PERSISTENT=false` ao
alcance, e o tronco que **perdeu** a copa para o decaimento — o que sobra ao
lado de uma arvore derrubada — virava recusa permanente, com o castigo
crescendo a cada volta. Sem plano, tambem nunca era replantado.
**Decisao do autor (09-15):** toco sem copa cai quando o grupo e pequeno e
nao e protegido. `BARE_TRUNK_LIMIT = 8`, e o numero saiu do log: os 114
grupos se separam num vale limpo entre 7 e 10 — 93 tem de 1 a 7 troncos, os
21 restantes vao de 10 a 57. Grupo acima do limite continua intocado, que e a
Regra 3. Nao se usou `BlockProtection` como criterio porque ela nao responde
por tronco: o Vanilla marca quem pos uma **folha**, nao um tronco, entao a
cabana que o jogador ergueu em terreno livre ela nao ve. **Limite conhecido:**
cabana de ate 8 troncos, a mao, em terreno livre, entra no corte.

**2. Mineiro parado no fundo da mina.** Relato do autor: *"os mineiros
estavam parados no fundo da mina em local que nao chegaram escavando"*. O log
mostrou `c8c33662` recebendo **a mesma pedra tres vezes** — cobre em
665,32,-2866 — com 6.000 tiques de castigo ja escritos na primeira
desistencia, e o trabalhador demitido do oficio na terceira
(`gave up COLLECT_STONE once too often`). O furo era de **porta, nao de
marca**: a guarda do E44 em `MineDigging.nextCut` pergunta
`MineMarks.isOutOfReach` para a posicao do **tunel**; o minerio **colado** na
parede e servido por outra linha, passando so pelo `nowhereToStand`. O
`giveUp` escrevia a marca e ninguem a lia. Corrigido: o minerio da parede
passa pela mesma marca, e a passagem devolve a posicao do tunel — abrir a
parede e o que da ao minerio um lado de onde se alcance.

Os dois tiveram GameTest **vermelho antes do patch**, com a mensagem de
falha citando o relato. `build` verde (828 unitarios) e 329/329 GameTests.
**Pendente: validacao em jogo dos dois.**

**3. Regra 30 revogada — a boca da mina nao ganha mais bau.** Decisao do
autor, 2026-09-15: *"retire o bau da boca da mina, use so o bau de cada
mineiro"*. Era: minerio que nao fosse carvao ia para o bau da boca
(`MinerHaul.treasureChestFor`), o resto para o bau do mineiro. Passa a ser um
destino so. `MineMouth.furnish` nao poe mais bau; a lanterna fica, porque e
peca do arco e nunca dependeu do bau — a linha de log *"the mouth lantern
waits on the chest"* descrevia uma dependencia que ja nao existia, e a queixa
*"has no chest and none could be placed"* que o autor viu no log sumiu com a
causa.

**O que NAO mudou, de proposito:** `ColonyChests.addMineMouth` continua
**lendo** o bau da boca. O bau que a colonia ja pos em saves anteriores fica
de pe com todo o minerio que a Regra 30 mandou para la; parar de le-lo
apagaria esse estoque da contabilidade e devolveria o defeito que o P0.3
corrigiu em 09-11 — o fundidor dizendo `nothing in the colony chests to
smelt` com o ferro a dez blocos. Ele vira fonte que so drena. Quando o
jogador o quebrar, `MineMouth.chestAt` deixa de acha-lo e a leitura fica
silenciosa sozinha.

**4. Planejador: as duas otimizacoes que o autor escolheu (09-15).** O log
mediu ciclos de 98, 57 e 54 ms contra o orcamento de 50 ms do tique, com o
planejador levando 72 ms do pior deles, 29 colonias e 192.448 recusas de lote
num ciclo.

*Opcao 1 — a recusa barata responde antes da cara.* Das 192.448 recusas,
126.315 eram a Regra 3, a pergunta mais cara da fila
(`BlockProtection.isVillageOriginal` consulta o `StructureAccessor`), e ela
rodava em **segundo** de sete; a comparacao de dois inteiros da Regra 19, com
24.350 recusas, rodava em **sexto**. Toda coluna reprovada pela regua da rua
pagava a consulta de estrutura antes de chegar a comparacao que a reprovaria
de graca. A guarda barata do `isVillageOriginal` nao salvava o caso: ela sai
cedo quando o bloco nao tem referencia de estrutura, e dentro de uma vila os
blocos tem — que e onde a colonia procura lote.

Foram movidos **so os dois extremos**: `OFF_ROAD_LEVEL` para o topo e
`isVillageOriginal` para o fim. As tres do meio ficaram na ordem original de
proposito — custam o mesmo (uma consulta em memoria, duas leituras de bloco),
entao trocar nao compra desempenho e estraga o diagnostico: a primeira
tentativa pos `isLotGround` na frente e o `dirt_path` reservado como estrada
passou a ser recusado por "nao e solo natural", porque caminho de terra nao e
cubo inteiro. A recusa continuava certa e o log passava a mentir sobre o
motivo; quem pegou foi o
`everyReservedRoadMaterialBlocksTheWholeFootprint`.

*Opcao 3 — a vez de planejar e repartida.* Nada limitava quantas colonias
decidiam obra por ciclo: o laco percorria as 29, e o custo era a soma delas. A
varredura ja tinha teto **por colonia** (1.024 colunas por passagem) e faltava
o teto **global**. `PlannerTurns` da a vez a `PER_CYCLE = 8` colonias por
ciclo, em rodizio que retoma de onde parou. O numero saiu da medicao: 72 ms
para 29 colonias sao ~2,5 ms cada, e oito devolvem a fase para perto de 20 ms.

**O preco, dito por inteiro:** num mundo de 29 colonias cada uma passa a
decidir obra a cada quatro ciclos — dois minutos, e nao trinta segundos. A
construcao fica mais lenta onde ha muitas colonias, que e onde o tique
estourava. Com oito colonias ou menos nada muda. So o planejamento espera a
vez; trabalhador, bau e tarefa continuam andando todo ciclo para todas.

**Ao ler o log depois disto:** os numeros de recusa mudam de caixa sem que
nada de comportamento tenha mudado — espere a Regra 3 cair muito e a regua da
rua subir. O sinal de sucesso e a linha `Colony cycle took N ms` parar de
aparecer.

**Ainda nao verificado em jogo.** O ganho foi deduzido da medicao do log e da
ordem dos predicados; nenhum dos dois foi cronometrado em partida. Gametest
nao mede milissegundos de forma confiavel — o teste novo
(`theCheapRefusalAnswersBeforeTheExpensiveOne`) afirma a **ordem**, que e a
otimizacao, pela atribuicao da recusa.

Tres GameTests que afirmavam a Regra 30 foram convertidos:
`theMineMouthGetsALanternAndAChest` virou
`theMineMouthGetsALanternAndNoChest`, e a cauda do bau saiu de
`anArchTheOwnerBrokeIsNotRaisedAgain` e de
`theMouthFurnitureStaysOutOfTheStaircase`.

**Verificacao desta sessao:** `build` verde com **833 unitarios** (5 novos do
`PlannerTurnsTest`) e **330/330 GameTests**. A bateria foi repetida **tres
vezes** por causa da instabilidade conhecida: na primeira rodada caiu
`smeltergathersgrassoutsidetheprotectedvillageradius` com *"fundidor criado no
setor nao esta registrado no ServerWorld"* — que e o AUD-001 ja registrado —, e
as duas rodadas seguintes passaram limpas. Nao e regressao desta sessao, e o
AUD-001 continua aberto.

---

## Sessão de 2026-09-14

**Distribuição após a emenda ADR-012:** o JAR 0.3.0 foi reconstruído e
copiado para `downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256 nas três
cópias (`build/libs/`, distribuição e launcher):
`EF0138BE7180FC47FB905C42EF7F64A8A31FE68CFCFCBCE4C07CAF72DB467231`.
`build` e 324/324 GameTests passaram; falta a validação visual das obras e da
mineração no mundo do jogador.

**Correção local de continuidade do mineiro:** quando `MinerWork.tick` remove
um job cuja tarefa foi encerrada, ele agora libera a claim do ramal no mesmo
tique. O teste foi escrito vermelho antes do patch; depois, `build` e
324/324 GameTests passaram. O JAR em `downloads/` e no launcher não foi
atualizado nesta sessão; a observação em jogo continua pendente.

**AUD-001 — falha de CI reavaliada, ainda aberta:** o artefato do run Linux
`34814235426` no commit `2a0a4b7` registra a asserção
`fundidor criado no setor não está registrado no ServerWorld` no tick 1. A
suíte local fresca passa 324/324; desligar temporariamente a descoberta de
terra faz falhar somente `o fundidor não removeu a terra do setor externo
escolhido`, provando que a coleta é exercitada. A troca experimental para
`TestContext.spawnEntity` também falhou na asserção de registro e foi
revertida. Sem causa determinística, não houve aumento de timeout nem patch
de comportamento; falta executar o job Linux em uma revisão publicada.

## Sessão de 2026-09-13

**Distribuição desta sessão:** JAR 0.3.0 copiado de `build/libs/` para
`downloads/` e `%APPDATA%/.minecraft/mods/`. SHA-256:
`E41063395BFC20A8D5D3182A9732B6FAF1E7265E96446E8909F32A623B1BE1D9`.
`build` e 316/316 GameTests passaram; validação visual continua pendente.

**Jar atualizado após E45 em 2026-09-13:** SHA-256
`D8548BAD17A87FED10E0EFBBA92ADC129ED36047E1FC451AAE27248AE3B930F3` — em
`downloads/` e `%APPDATA%/.minecraft/mods/`.

**O que o autor viu no jogo:**

- Casa parada no meio.
- Mineiro invisível/parado.
- Lenhadores e fazendeiros funcionando.

**Quatro correções já entraram, para a próxima sessão:**

1. Mina desce quando o poço partilhado fica fechado antes da bifurcação.
2. Lenhador mira um ponto de pé ao lado da árvore em vez do tronco.
3. `SweepLog` deixou de acusar ciclos que saíram antes de pedir lote.
4. `ABANDONED` precisa de duas leituras positivas seguidas para voltar a `STABLE` — reduz o E9.

**Correção da casa, do playtest de 09-12:** a obra ficou com **1 bloco faltando**, em `WAITING_RESOURCES`, esperando `minecraft:composter`. A carpintaria fabricava composteiras, mas usava a porta de consumo (`ColonySupply.take`) e retirava do baú a peça que acabou de produzir. Corrigido em 09-13: `CraftingWork` passa a chamar `ColonySupply.stock`.

**Auditoria técnica de 09-13:** a casa média do log parou por uma exigência falsa de `minecraft:structure_void`; o leitor de blueprint agora ignora esse marcador. O teste novo falhou antes da correção e passou depois. Foram aprovados 798 unitários, 312 GameTests e 74 testes Python. A pequena fazenda antiga do log retomou com 45 blocos, mas não há identidade suficiente para afirmar que era a casa do relato. A casa média corrigida ainda aguarda validação em jogo.

**Reinvestigação após o playtest:** um GameTest reproduziu a colocação de
`minecraft:dirt` sem estoque: `BuilderWork` tratava toda a tag `DIRT` como
material moldado no local. A exceção foi limitada a farmland, água,
`dirt_path` e cultivos. Os 313 GameTests e `build` passaram; a nova regra
aguarda validação em jogo. O log anterior carregou o JAR antigo e não
registra bloco/posição da terra observada, então a causa está confirmada
no código, mas não pode ser atribuída com certeza àquela posição específica.

## Sessão de 2026-09-14 — obra esperando terra

O log mais recente mostra que o planejador **abriu** `plains_butcher_shop_2`
com 382 blocos às 00:50:15. A obra assentou dois blocos e então ficou em
`WAITING_RESOURCES`, com 380 restantes, aguardando `minecraft:dirt` até
01:01:15. Os 18 baús lidos tinham `GRASS_BLOCK=33`, mas nenhum `DIRT`; o
fundidor não tinha rota para coletar terra. Portanto, a ausência visual de
construções neste teste decorre de uma obra ativa bloqueada por insumo, não
de falha de seleção do planejador. Um aviso posterior de alcance do construtor
é secundário e precisa de nova medição depois que a terra chegar.

`DIRT` agora é recurso `SURFACE_GATHERED`, tem conversão do item Vanilla e
entra na mesma coleta externa e protegida por setor usada para `grass_block`.
GameTests cobrem catálogo, atribuição ao coletor, coleta real de terra fora do
raio protegido e depósito no baú pessoal. `build` e 320/320 GameTests passaram.
**Pendente:** instalar o JAR atualizado e validar em jogo se o fundidor coleta
terra, se a obra retoma além dos 380 blocos e se o aviso de alcance reaparece.

**Reanálise do playtest de 09-14:** o JAR do launcher tinha SHA diferente do
`build/libs/`; o log veio do artefato anterior à correção de terra. Nele,
construções abriram mas pararam esperando `dirt`; três mineiros estavam aptos,
mas sem tarefa, porque havia 3 carvões e não existiam metas de carvão/ferro
sem obra. Lenhadores também ignoravam `BlockProtection` durante a derrubada.
Correções atuais: piso de 64 carvão + 64 minério bruto (obra soma ao piso) e
proteção de árvores no plano e em cada quebra. Construções Vanilla e da colônia
são cobertas; troncos manuais sem marca não têm autoria recuperável pelo jogo.
`build` passou, 322/322 GameTests passaram, e o JAR foi copiado para
`downloads/` e o launcher. SHA-256 nas três cópias:
`9783536ED2B357FA0EA89EA8F5C36385297FBD512EA57C21AD13B684523114DB`.
**Pendente apenas validação em jogo** da construção, mineração e preservação
estrutural; troncos manuais sem marca seguem como limite conhecido.

**ADR-016 / Lote 1 em andamento (09-14):** `ResourceTally`,
`ColonyResources` e a leitura de baús agora preservam contagens por `ResourceId`
para itens fora de `ResourceType`, mantendo a visão tipada existente. `build`
verde e 317/317 GameTests; ainda não há metas/tarefas nem executor genérico para
esses IDs, portanto a cobertura de materiais continua pendente. Validação em
mundo de desenvolvimento ainda não realizada. JAR 0.3.0 distribuído em
`downloads/` e `%APPDATA%/.minecraft/mods/`, com cliente fechado; SHA-256 nas
três cópias: `F41920D7DA1FBEADA94D4F886F5047F7A3242F850011DE7DFA2F4A9CAE28DB77`.

O mineiro fechava a frente sem espaço para ficar e reabria a mesma hélice no
fundo. E45 foi resolvido em 2026-09-13 pela ADR-013: no limite, a rota gira
sem mover a boca; saves v4 reiniciam os cursores e mantêm o arco. `build`
passou com 808 testes unitários e `runGametest` com 314/314; confirmação
visual ainda pendente.

**Playtest de 2026-09-13, após o JAR anterior:** nenhuma construção visível;
mineiros sem atividade percebida; baús de profissões misturando produção.
No log, a obra assentou só três peças e parou por falta de grama/pedregulho;
mineiros ficaram sem espaço para ficar em pé e produziram zero; lenhadores
e agricultores registraram colheitas, e fundidores pararam repetidamente
por falta de areia. Revisão dos depósitos confirmou saídas em baús de
colegas: agora a produção de mineiro, lenhador, fundidor, carpinteiro e
pedreiro vai ao baú pessoal; insumos continuam compartilhados. Se não há
espaço, transformações devolvem o insumo e drops de mineração/derrubada
permanecem no mundo. `build` e 313 GameTests passaram. **Aguardar validação
em jogo.** Próximo lote: causa da mina sem espaço para ficar em pé e
construção bloqueada por estoque/retomada.

**Releitura após alterações do jogador (lote aplicado em 2026-09-13):**
interações que realmente mudam blocos e quebras invalidam os índices/cursor
de construção das colônias próximas; a varredura limitada relê estradas e
terreno no mundo. Abrir espaço sobre trecho já percorrido da mina reabre o
braço desde o primeiro ponto afetado e limpa seu bloqueio transitório.
`build` e 314 GameTests passaram, inclusive a reindexação após estrada nova.
Pendente confirmar em jogo que casa e mina retomam no mundo do autor. Isto
não cria novas galerias nem corrige geometria E45. Baús registrados e o da
boca da mina já são consultados ao vivo; baú arbitrário continua dependendo
de vínculo de armazenamento.

**ADR-011 — profissões e crescimento:** implementadas as sete funções
produtoras (Mineiro, Lenhador, Pedreiro, Fundidor, Carpinteiro, Agricultor,
Criador) e as vagas por população adulta: 1 de cada aos 15, 2 de cada aos
30, 3º Mineiro aos 31 e 3º Lenhador aos 32. Nitwits contam para a população,
mas não são contratados; bebês só contam quando adultos. `BUILDER` e
`SHEPHERD` permanecem compatíveis com saves, e Pastor legado conta na cota
de Criador. Qualquer produtor pode construir temporariamente sem trocar de
profissão. Build, testes unitários e 313 GameTests passaram; **aguarda
verificação em jogo**.

---

## P1 — em fila (depende de P0 verde)

| Item | Descrição | Decisão |
|---|---|---|
| E44 | Recusa de alvos inalcançáveis | ✅ escada em `MineMarks`; **aguarda validação em jogo** |
| E43 | O descanso de 4 ciclos é anulado no ciclo seguinte | **autor** |
| E41 | Nada mede degradação ao longo de muitos ciclos | — |
| KF-001 | Instabilidade de `aFrozenMinerGivesUpLongBeforeTheStallGuard` | ✅ causa no teste corrigida; quota global é risco separado de vazão |

---

## Última verificação em jogo

| Data | O que foi visto |
|---|---|
| **2026-09-13** | casa parada, mineiro parado, lenhadores e fazendeiros trabalhando |
| **2026-09-12** | casa morreu esperando `smooth_stone_slab`; mineiro não desceu |
| **2026-09-11** | casa aberta (P0.1 visto); obra travou em `dirt_path` |

**Pendente de ver em jogo:**

- P0.1-b, P0.1-c, P0.3, P0.5, P0.6 — entregues em 09-11, **nunca vistos**.
- As quatro correções de 09-13 — **nunca vistas**.
- Arco da mina não volta depois de quebrado (`Mine.archRaised`).
- Lenhador corta todo o tronco sem deixar sobra (E39, fechado 09-12).

---

## Bateria

Última medição: **810 unitários**, **314 GameTests** e **74 testes Python**;
zero falhas nos dois primeiros nesta sessão. Python não foi reexecutado.

**Diagnóstico do playtest/log de 09-13:** uma tarefa de areia foi criada para
vidro, mas `MinerWork` gravava no Job a pedra da paleta da vila. O log mostra
`0 of 3` apesar de areia transportada; corrigido para o Job acompanhar o
recurso da tarefa. Antes disso, ele completou 64/64 pedregulhos; depois a
busca de areia não encontrou bloco num raio de 48. `MinerWork.tick` remove
Jobs concluídos sem uma etapa explícita de volta à boca da mina; retorno
segue aberto. A obra esperou `minecraft:grass_block` com 382 blocos restantes
e desistiu mantendo o lote ocupado; nenhuma profissão fornece esse recurso
no catálogo atual. Agricultores não acharam plantio maduro/lote vazio em 32
blocos. Carpinteiro, fundidor, pedreiro e pastor reportaram sem tarefa aberta;
lenhadores foram vistos cortando e aumentando a produção. `build`: 810
unitários; GameTests: 314/314.

---

## Decisões que esperam o autor

1. **E43 — o descanso de 4 ciclos é anulado no ciclo seguinte.** A 2ª passagem do `takeOneTask` devolve a mesma tarefa ao mesmo trabalhador quando a colônia não tem outro trabalho da profissão dele.

---

## Onde o projeto está

MVP previamente verificado em jogo; o playtest de 09-13 contradiz o estado da mina: o minerador repete uma frente bloqueada no limite. O falso material `structure_void` da casa média foi corrigido e ainda precisa de verificação visual. O log mostra uma obra antiga retomada, mas não prova que era a casa do relato. Lenhadores e fazendeiros foram vistos funcionando pelo autor.

**O gargalo recorrente é verificação em jogo e decisões do autor**, embora
o playtest ainda revele defeitos de código, como a fome de buscas da mina
corrigida nesta sessão. Cada item entregue acumula dívida de "não visto em
jogo", e a fila cresce mais rápido do que drena.

**Próximo passo natural:** sessao de jogo para validar o portal da mina em
vila nova, a rota E45, a casa e a politica P0.7 no mundo do autor. E43 segue
como decisao do autor; E44 aguarda validacao em jogo.

**Nova vila sem portal da mina (09-13):** o log mostra mineiros das vilas
novas repetindo `looking for stone, 0 of 64`, sem linha de abertura. A causa
no código era a cota global de uma busca por tique presa ao primeiro
trabalho sem alvo. O rodízio foi corrigido e passou em `build` (809
unitários) e 314 GameTests; portal visível e início da escavação **aguardam
validação em jogo**.

**Coleta superficial do fundidor (09-13):** pá de ferro com Toque Suave I;
areia para a cadeia do vidro e `grass_block` apenas quando uma obra aberta
precisa dele. A grama é buscada estritamente além de 64 blocos, no setor
cardinal mais distante das peças de estruturas de vila conhecidas em chunks
carregados; nenhum chunk é forçado e o raio de busca de trabalho continua 48.
`build` e 315/315 GameTests passaram. **Aguardam validação em jogo** a coleta,
o baú pessoal do fundidor e a preservação visual das estruturas. JAR de
`downloads/` e launcher não atualizado nesta tarefa.

**Distribuição híbrida e pedra lisa (09-13):** faltas da obra aberta recebem
`CONSTRUCTION_MATERIAL` antes das tarefas de estoque; trabalhadores restantes
mantêm `PRODUCTION`. O pedreiro reconhece `smooth_stone_slab`, e a demanda
deriva a quantidade de `smooth_stone` da receita Vanilla. `build` e 316/316
GameTests passaram; aguarda validação visual. ADR-015 registra a escolha C,
sem teto numérico arbitrário de reserva. **Ainda não implementado:** tarefas,
contagem e estoque de blocos arbitrários, nem o fallback genérico para
fundidor/criador; o catálogo de recursos atual é enum fechado. Ver TODO.

**Plano de continuidade e construção (09-14):** plano por lotes em
`docs/superpowers/plans/2026-09-14-worker-continuity-and-construction.md`.
Lote 1 aplicado: ADR-012 reconcilia a coluna editada pelo jogador, preserva
a varredura parcial e reinicia apenas o cursor de consulta de ruas. `build` e
324/324 GameTests passaram; **sem confirmação em jogo**. Próximo lote para
revisão: continuidade do mineiro; depois diversidade de estruturas entre
construtores, estratégias distintas para avaliar lotes e revalidação após
falhas repetidas.
