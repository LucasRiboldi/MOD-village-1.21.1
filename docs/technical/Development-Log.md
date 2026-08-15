# Development-Log.md

# Village Colony — Development Log

Arquivo cronológico do desenvolvimento. Uma entrada por sessão, na ordem
em que aconteceram, da primeira à última.

Saiu de `Project-State.md` em 2026-08-12, quando aquele documento passou
de 5800 linhas e o log respondia por quatro quintos dele. A regra do §16
de lá — "quem abre este projeto hoje sabe onde estamos e qual é o próximo
passo?" — deixava de ser respondida pelo tamanho.

O que ficou lá é o estado: onde o projeto está, o que falta, o que está
quebrado, o que espera decisão. O que está aqui é como se chegou nele.

**As referências a "§15" espalhadas pelo código apontam para cá.** O §15
do Project-State virou o ponteiro para este arquivo, e as entradas
continuam identificadas pela data.

Não se reescreve entrada antiga. Quando uma conclusão se revela errada, a
correção entra como entrada nova e a antiga fica onde está, com o
apontamento — o histórico de um erro é a parte que ensina.

Uma consequência disso: entradas anteriores a 2026-08-12 citam "§16" ao
falar das duas regras do autor. Na consolidação daquele dia elas viraram
o §18 do Project-State. O texto das entradas fica como estava.

---

## Entry 001

Data:

Initial setup.

Estado:

Toda documentação base criada.

Resultado:

Projeto pronto para iniciar implementação Fabric.

---

## Entry 002

Data:

2026-08-06

Ação:

Auditoria técnica completa dos 24 documentos.

Estado:

Documentação coerente em visão e filosofia.

Encontrado:

* 5 bloqueadores arquiteturais;
* 4 problemas de severidade alta;
* 5 de severidade média;
* 6 de severidade baixa.

Correções aplicadas:

* extensões `.md.txt` renomeadas para `.md`;
* repositório Git remoto registrado.

Resultado:

Implementação bloqueada até conclusão do Stage 0.

O próximo passo NÃO é criar o projeto Fabric.

O próximo passo é ADR-002.

---

## Entry 003

Data:

2026-08-06

Ação:

Redação das decisões do Stage 0.

Criado:

```text
ADR-002-Chunk-Loading-Strategy.md

ADR-003-Village-Detection.md

ADR-004-Mixin-Policy.md

ADR-005-Core-Type-Isolation.md

ADR-006-Package-Layout.md

docs/technical/Vanilla-Integration.md

LICENSE

.gitignore
```

Atualizado:

```text
README.md          — licença MIT

Fabric-Version.md  — Java 21, Yarn, Version Matrix
```

Estado:

Todas as ADRs em `Proposed`.

Nenhuma aceita.

Resultado:

Implementação permanece bloqueada.

Aguardando aprovação.

---

## 2026-08-06 — Stage 0 concluído

Aceito:

```text
ADR-002  Hibernação com estado persistente e retomada

ADR-003  Detecção por cluster de POIs de cama ocupados

ADR-004  Mixin com superfície mínima e declarada

ADR-005  Tipos de valor próprios no Core (records imutáveis)

ADR-006  Domínio dentro da camada
```

Atualizado:

```text
PROJECT_CONSTITUTION.md   §3 alinhado com ADR-002

Fabric-Version.md         Version Matrix fixada
```

Versões fixadas:

```text
Gradle 9.6.1

Loom 1.17.18

Yarn 1.21.1+build.3

Loader 0.19.3

Fabric API 0.116.15+1.21.1
```

Não verificado:

```text
A matriz não foi executada.

Nenhum build rodou.

A validação acontece na TASK-001.
```

Resultado:

```text
Implementação desbloqueada.

Próximo passo: TASK-001.
```

---

## 2026-08-06 — TASK-001 concluída

Criado:

```text
build.gradle

settings.gradle

gradle.properties

gradle/wrapper/      (Gradle 9.6.1)

gradlew / gradlew.bat

.gitattributes

src/main/java/com/villagecolony/VillageColonyMod.java

src/main/resources/fabric.mod.json
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → mod carregado, sem exceções
```

Descoberto:

```text
Loom 1.17.18 exige JVM 21 para rodar o Gradle.

O foojay-resolver cobre a toolchain, não o JVM do Gradle.

Temurin 21.0.12 instalado em ~/.jdks
```

Corrigido:

```text
Initial-Setup-Checklist.md §6

  layout antigo substituído por referência à ADR-006
```

Resultado:

```text
Version Matrix validada na prática.

Nenhum valor precisou de correção.
```

---

## 2026-08-06 — TASK-002 e TASK-003 concluídas

TASK-002 — Identidade do mod:

```text
fabric.mod.json completo

description, contact.homepage, sources, issues

license MIT
```

TASK-003 — Estrutura de pacotes:

```text
28 pacotes conforme ADR-006 §3

cada um com package-info.java
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → villagecolony 0.1.0 na lista de mods

                       sem exceções
```

Corrigido — ADR-006 §7 exigia fonte única:

```text
README.md §11

Class-Architecture.md

Fabric-Implementation-Plan.md
```

Os três repetiam layouts divergentes.

Agora apontam para a ADR-006.

Divergências encontradas:

```text
core/profession/  →  core/worker/

camada manager    →  não existe mais

core/type/        →  estava ausente
```

Pendente:

```text
icon do mod — decisão do autor
```

---

## 2026-08-06 — TASK-004 concluída

Criado:

```text
fabric/event/ServerLifecycleHandler.java
```

Alterado:

```text
VillageColonyMod  → LOGGER compartilhado, chama register()
```

Verificado:

```text
./gradlew build      → BUILD SUCCESSFUL

./gradlew runClient  → mod carregado, sem exceções
```

Não verificado:

```text
Os handlers de SERVER_STARTED e SERVER_STOPPING nunca executaram.
```

`runServer` para no EULA:

```text
run/eula.txt  →  eula=false
```

Aceitar o EULA é decisão do autor, não do agente.

Pendência aberta até que um servidor rode.

---

## 2026-08-06 — TASK-005 concluída

Criado:

```text
core/type/ColonyPos                  record, ADR-005

core/colony/model/ColonyState        STABLE, PRODUCTION, EXPANSION

core/colony/model/ColonyLifecycle    ACTIVE, DORMANT

core/colony/model/Colony
```

Infraestrutura:

```text
JUnit 5.12.2 configurado em build.gradle
```

Verificado:

```text
13 unit tests passando

Core sem net.minecraft

Nenhum domínio do core importa outro
```

Conflito de documentos resolvido:

```text
Data-Model.md    state = STABLE | PRODUCTION | EXPANSION

ADR-002          state = ACTIVE | DORMANT
```

Não era conflito. São dois eixos independentes:

```text
ColonyState      o que a colônia faz

ColonyLifecycle  se a colônia é simulada
```

Uma colônia DORMANT conserva seu ColonyState e retoma nele.

Coberto pelo teste `goingDormantPreservesState`.

Corrigido em `Data-Model.md`:

```text
centerPosition   BlockPos → ColonyPos   (ADR-005)

lifecycle        campo adicionado
```

---

## 2026-08-06 — TASK-006 concluída

Criado:

```text
core/colony/service/ColonyService
```

Nome divergente de `MVP-Tasks.md`:

```text
MVP-Tasks.md   ColonyManager

ADR-006 §5     manager não existe como camada
```

Vale a ADR. A classe é `ColonyService`.

Operações:

```text
createColony    detecção encontrou vila nova

register        recolocação vinda do save

find            por id

findNearest     por posição, com raio explícito

all             leitura, ordem de registro

remove / clear / count
```

Verificado:

```text
35 unit tests passando

Core sem net.minecraft
```

Decisões registradas no código (TASK-006):

```text
register duplicado lança em vez de sobrescrever

  sobrescrever esconderia save corrompido

raio de findNearest é parâmetro, não constante

  quem chama conhece o contexto (ADR-003)

LinkedHashMap para ordem de iteração estável

  ordem instável dificulta depurar simulação

sem thread safety — thread única do servidor

  documentado na classe
```

Fase 1 encerrada.

---

## 2026-08-06 — TASK-007 escrita, não verificada

Criado:

```text
data/save/ColonySavedData
```

Alterado:

```text
VillageColonyMod        campo COLONIES (ADR-006 §5)

ServerLifecycleHandler  carrega no start, grava no stop
```

API confirmada contra o jar mapeado, não de memória:

```text
PersistentState.Type<T>(Supplier, BiFunction, DataFixTypes)

writeNbt(NbtCompound, RegistryWrapper.WrapperLookup)

server.getOverworld().getPersistentStateManager().getOrCreate(TYPE, key)
```

Decisão — `lifecycle` não é persistido:

```text
É estado derivado do carregamento de chunk.

Ao abrir o mundo nada está carregado.

Toda colônia volta DORMANT.
```

Persistir `ACTIVE` marcaria como simulável uma colônia cujo chunk não
existe em memória.

Contraria o que eu havia planejado na entrada anterior deste log, onde
`lifecycle` constava entre os campos a salvar. A leitura da ADR-002
mostrou que estava errado.

Decisão — estado desconhecido no save:

```text
Cai para STABLE em vez de lançar.

Não impedir o jogador de abrir o mundo.
```

Verificado:

```text
43 tests passando

runServer: Loaded 0 / Saved 0 colonies

villagecolony_colonies.dat gravado no mundo
```

O EULA foi aceito pelo autor, desbloqueando também a verificação
pendente da TASK-004.

---

## 2026-08-06 — Ordem do MVP-Tasks tem uma inversão

`TASK-008 — Testar Carregamento` exige:

```text
Criar mundo → Encontrar vila → Salvar → Fechar → Abrir
```

"Encontrar vila" é `TASK-009`.

A TASK-008 depende de uma tarefa que vem depois dela.

Consequência prática: o round-trip com dados reais só pode ser
provado após a detecção existir.

Enquanto isso, a serialização foi coberta por teste direto de NBT,
sem servidor.

---

## 2026-08-06 — TASK-009 concluída

Criado:

```text
core/colony/model/VillageCandidate

core/colony/service/VillageDetector

fabric/adapter/MinecraftTypeAdapter

fabric/integration/VillageScanner
```

Divisão adotada:

```text
Core      decide o que é vila (puro, testável)

Fabric    lê POI, conta aldeões, checa bioma
```

APIs confirmadas com `javap` no jar mapeado:

```text
PointOfInterestStorage.getInCircle(Predicate, BlockPos, int, OccupationStatus)

PointOfInterestTypes.HOME / MEETING  são RegistryKey

WorldView.getBiome(BlockPos) → RegistryEntry<Biome>

EntityView.getEntitiesByClass(Class, Box, Predicate)
```

---

## Conflito entre ADRs — resolvido com desvio

ADR-003 §7 manda `ColonyState` ganhar `DORMANT`, dizendo estar
"alinhado com ADR-002".

Não está:

```text
ADR-002   DORMANT = chunk descarregado

ADR-003   DORMANT = vila sem população
```

Condições diferentes. Uma vila abandonada com o jogador ao lado
atende a segunda e não a primeira.

`ColonyLifecycle` já usa `DORMANT` no sentido da ADR-002.

Decisão: o valor de `ColonyState` chama-se `ABANDONED`.

Motivo: dois `DORMANT` com significados distintos no mesmo objeto
seriam uma armadilha para quem lê o código depois.

```text
PENDENTE: emendar ADR-003 §7 registrando a troca.
```

Este é um desvio do texto de uma ADR aceita, e está aqui para ser
revisto pelo autor — não para passar despercebido.

---

## Escolhas de implementação

```text
Distância de cluster é horizontal

  a cama do sótão é da mesma casa
```

```text
Média das camas somada em long

  64 camas em coordenada extrema estouram int
```

```text
Camas com OccupationStatus.ANY

  vila que perdeu aldeões ainda é vila;

  quem decide isso é a validação
```

```text
Caixa de contagem derivada das camas

  buscar aldeões no mundo inteiro é proibido

  por Performance-Rules.md §5
```

---

## 2026-08-07 — Persistência de trabalhadores decidida

Decisão:

```text
Estender ColonySavedData
```

Recusado: `WorkerSavedData` em arquivo próprio.

Motivo: o `Worker` referencia a colônia por `colonyId`. Dois arquivos
separados permitiriam um trabalhador órfão apontando para uma colônia
que não foi gravada, e não há transação que mantenha os dois em
sincronia. Um só `PersistentState` também evita dobrar o custo de
versionamento futuro.

Registrado:

```text
MVP-Tasks.md   TASK-012b, entre a 012 e a 013
```

A numeração com sufixo evita renumerar da TASK-013 em diante, o que
invalidaria as referências já espalhadas pelos documentos.

Nenhum código foi escrito nesta entrada.

---

## 2026-08-07 — Verificação em jogo adiada a pedido do autor

A fila do §8 punha a verificação da TASK-012 antes de escrever mais
código. O autor não podia rodar o jogo no momento e pediu para seguir.

Consequência aceita: TASK-012b, 013 e 014 foram escritas sobre uma
fronteira nunca exercitada. Registrado aqui para que a origem da dívida
não se perca.

---

## 2026-08-07 — TASK-012b concluída

Criado:

```text
WorkerService.restore        recoloca trabalhador vindo do save
```

Alterado:

```text
ColonySavedData          lista "workers" ao lado de "colonies"

  sync(colonies, workers)  — assinatura mudou, os dois juntos

ServerLifecycleHandler   carrega e grava os trabalhadores
```

Decisão — órfão é descartado na leitura:

```text
Trabalhador cuja colônia não veio no mesmo arquivo é ignorado.
```

Ele não deveria existir, já que os dois são gravados juntos. Se
existir, mantê-lo seria pior que perdê-lo: nenhuma colônia o listaria e
a varredura não o recriaria, porque o `villagerId` já teria dono. Ficaria
invisível para sempre. Descartado, a varredura o reencontra e o reatribui
à colônia certa — ao custo da profissão que ele tinha.

Decisão — profissão ausente ou desconhecida vira "sem função":

```text
Mesmo princípio de readState: não derrubar o mundo.
```

Aqui o custo é menor que no estado da colônia — a TASK-014 dá uma função
nova no próximo ciclo.

Decisão — `restore` lança em duplicata, `register` não:

```text
register  idempotente   a varredura repete de propósito

restore   lança         villagerId repetido no save esconderia
                        qual profissão venceu
```

Verificado:

```text
116 testes passando

./gradlew build → BUILD SUCCESSFUL
```

Não verificado:

```text
O round-trip com mundo real. Nenhum save foi aberto e fechado.
```

---

## 2026-08-07 — TASK-013 concluída

Criado:

```text
core/worker/model/Capability          COLLECT_WOOD, CRAFT_ITEMS,
                                      MAINTAIN_FOOD, BUILD_STRUCTURE

core/worker/model/ToolType            NONE, WOODEN_AXE, WOODEN_HOE

core/worker/model/Profession          definição imutável

core/worker/service/ProfessionRegistry   catálogo das quatro
```

Decisão — `Capability` é tipo próprio, não método de `ProfessionType`:

```text
Duas profissões podem vir a compartilhar uma capacidade.
```

Profession-System.md exige que profissão nova não obrigue a mexer nas
antigas.

Decisão — `ToolType` em vez de `Item`:

```text
ADR-005: o Core não conhece net.minecraft.
```

A conversão para o item Vanilla entra no `MinecraftTypeAdapter` quando
houver quem entregue a ferramenta. Hoje não há.

Decisão — o registro é estático e não tem `clear`:

```text
As quatro profissões são fixas e não pertencem a um mundo.
```

Difere de `ColonyService` e `WorkerService`, que são estado de partida.

Decisão — `of` lança em vez de devolver `Optional`:

```text
Todo ProfessionType tem definição.
```

A falta de uma é profissão acrescentada ao enum sem entrada no catálogo
— erro de programação, não ausência legítima. Há teste que trava isso.

Não implementado:

```text
allowedTasks, previsto em Profession-System.md
```

Depende de `core/task`, hoje vazio. A ligação já é possível pelo outro
lado: a tarefa declara a `Capability` que exige e `canPerform` responde.

---

## 2026-08-07 — TASK-014 concluída

Criado:

```text
core/worker/service/ProfessionAssigner
```

Alterado:

```text
VillageDetectionHandler.registerVillagers

  atribui depois de registrar
```

Regra adotada — sempre a função mais escassa da colônia:

```text
Cobre as quatro antes de duplicar qualquer uma.
```

É a necessidade mínima do Profession-System.md: seis aldeões, um de cada.

Decisão — empate resolvido pela ordem de `ProfessionType`:

```text
LUMBERJACK, MANUFACTURER, FARMER, BUILDER
```

Numa colônia recém-detectada todas as contagens são zero, então é essa
ordem que decide as primeiras quatro atribuições. Ela é a cadeia
produtiva do MVP: começar pelo construtor, sem madeira nem material,
daria um trabalhador sem o que fazer.

Decisão — recontar a cada atribuição, não uma vez por lote:

```text
Quatro aldeões de uma colônia vazia veriam a mesma contagem
e virariam quatro lenhadores.
```

Há teste para isso.

Decisão — a atribuição roda mesmo sem aldeão novo:

```text
Save anterior à TASK-012b traz trabalhadores sem função.
```

Eles precisam receber uma sem depender de alguém nascer.

Verificado:

```text
135 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — TASK-015 e TASK-016 concluídas

Criado:

```text
core/storage/model/WorkerStorage        workerId + posição do baú

core/storage/service/StorageRegistry    quem tem baú, e onde

fabric/integration/ChestScanner         cama -> baú próximo
```

Alterado:

```text
VillagerScanner          procura o baú na mesma passagem

  scan devolve ScanResult, não int

VillageColonyMod         campo STORAGES

ServerLifecycleHandler   esvazia STORAGES junto com os demais

VillageDetectionHandler  loga os baús registrados
```

APIs confirmadas com `javap` no jar mapeado, não de memória:

```text
MemoryModuleType.HOME                     -> GlobalPos

Brain.getOptionalRegisteredMemory(...)

GlobalPos.dimension() / pos()

WorldChunk.getBlockEntities()             -> Map<BlockPos, BlockEntity>

ServerChunkManager.getWorldChunk(x, z)    null se não carregado
```

Decisão — a busca parte das block entities do chunk, não do cubo:

```text
Percorrer o cubo de raio 6 custaria 2197 getBlockEntity
por aldeão sem baú, a cada ciclo.
```

Contra Performance-Rules.md §6. Uma casa tem um punhado de block
entities, e é sobre esse punhado que se itera. O raio vira um filtro,
não um laço.

Decisão — a cama é a casa:

```text
MemoryModuleType.HOME já é a cama do aldeão.
```

É o mesmo POI que a ADR-003 usa para achar a vila, então não há uma
segunda noção de "casa" no código.

Decisão — baú já reivindicado é pulado:

```text
Storage-System.md §"Proteção".
```

Sem isso, dois aldeões do mesmo cômodo partilhariam um baú e cada um
contaria o estoque do outro como seu.

Decisão — `register` substitui, ao contrário de colônia e trabalhador:

```text
O baú registrado pode ter sido quebrado.
```

Reencontrar o dono com outro baú é a recuperação prevista em
Storage-System.md §"Falhas". Recusar prenderia o trabalhador a um baú
que não existe mais.

Decisão — o registro de baús não é persistido:

```text
A posição do baú existe no mundo e é redescoberta.
```

Difere da profissão, que só existe na cabeça do mod. Salvar manteria
uma segunda verdade que envelheceria assim que o jogador quebrasse o
baú.

Decisão — a busca não força carregamento de chunk:

```text
Chunk não carregado é pulado.
```

ADR-002. O baú lá será encontrado no ciclo em que o chunk estiver
carregado.

Não implementado:

```text
"Storage Missing" de Storage-System.md §"Falhas"
```

`StorageRegistry.remove` existe e nada o chama: falta detectar que o
baú registrado sumiu. Enquanto isso, um baú quebrado continua no
registro até o dono achar outro.

Verificado:

```text
148 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — TASK-017 concluída, Fase 5 encerrada

Criado:

```text
core/resource/model/ResourceCategory    NATURAL, PROCESSED, CONSTRUCTION

core/resource/model/ResourceType        OAK_LOG, OAK_PLANKS, COBBLESTONE

core/resource/model/ResourceTally       contagem imutável, somável

fabric/integration/ChestInventoryReader lê os baús
```

Alterado:

```text
MinecraftTypeAdapter      Item -> ResourceType

VillageDetectionHandler   loga o estoque quando um baú novo entra
```

APIs confirmadas com `javap` no jar mapeado:

```text
Inventory.size() / getStack(int)

ItemStack.getItem() / getCount() / isEmpty()

Items.OAK_LOG / OAK_PLANKS / COBBLESTONE
```

Decisão — o tipo do recurso é do Core, o item é da fronteira:

```text
ADR-005. ResourceType não conhece Item.
```

A conversão é uma comparação por identidade no `MinecraftTypeAdapter`:
`Items.OAK_LOG` é singleton do registro, a mesma instância para todo
stack de carvalho do servidor.

Decisão — `ResourceTally` é imutável:

```text
Uma contagem é a fotografia de um momento.
```

O baú muda o tempo todo. Um objeto que se atualizasse sozinho não teria
como dizer de quando é o número que carrega. Somar duas contagens
produz uma terceira, e é assim que os baús viram um total.

Decisão — zero e ausente são a mesma coisa:

```text
of() descarta os zeros.
```

Sem isso, duas contagens que dizem o mesmo não seriam iguais — uma com
`OAK_LOG=0`, outra sem a chave.

Decisão — o total é calculado, não guardado:

```text
O jogador pode esvaziar o baú a qualquer momento.
```

Um total em cache estaria errado sem que nada avisasse. Enquanto a
contagem for barata — um punhado de baús, dezenas de slots — vale
pagar por ela. Quando deixar de ser, o cache precisará de invalidação
por evento, não de um temporizador.

Decisão — baú duplo conta só a metade registrada:

```text
Cada metade é uma block entity com posição própria.
```

Foi uma delas que o trabalhador reivindicou. Contar as duas faria a
colônia enxergar o dobro quando o outro lado fosse reivindicado por
outro aldeão.

Decisão — o log sai só quando um baú novo entra no registro:

```text
O conteúdo muda a cada baú que o jogador abre.
```

Logar por ciclo encheria o arquivo sem dizer nada. Sem nenhuma linha, a
contagem seria invisível em jogo — e o §11 existe porque defeitos desta
camada só aparecem lá.

Nada aqui escreve no baú. O MVP lê; mover item é da Fase 6.

Verificado:

```text
161 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Tudo o que depende do jogo. Ver §7.
```

---

## 2026-08-07 — Duas correções de jogabilidade

Nenhuma das duas veio de tarefa do plano. Vieram de olhar o que já
existia como jogo, e não como código.

---

### Bebê e nitwit não recebem mais função

Sintoma:

```text
ProfessionAssigner dava função a todo trabalhador sem função.
```

Um bebê virava lenhador. Além de absurdo em jogo, ele ocupava a vaga:
`mostNeeded` contava a função como preenchida, e o adulto seguinte
virava fazendeiro numa colônia sem ninguém cortando madeira.

O nitwit tinha o mesmo problema com um agravante. O Vanilla nunca lhe
dá emprego, e o jogador que reconhece o casaco verde espera que ele
continue inútil — PROJECT_CONSTITUTION §4 manda respeitar o
comportamento Vanilla do aldeão.

Correção:

```text
VillagerScanner decide quem pode trabalhar

  !isBaby() && profissão != NITWIT

ScanResult carrega os aptos

assignMissing recebe o conjunto
```

A decisão fica na camada fabric porque é ela que enxerga a entidade. O
Core continua puro: recebe um conjunto de ids e não pergunta por quê.

A contagem de necessidade continua olhando a colônia inteira. Um
lenhador é um lenhador esteja ele à vista ou não — filtrar a contagem
pelos aptos faria a colônia recontratar funções que já tem toda vez que
alguém saísse do raio.

O bebê é registrado como antes. Ao crescer, torna-se elegível sozinho,
no ciclo seguinte, sem nada que trate o caso.

---

### Aldeão morto ou zumbificado deixa de ser trabalhador

Sintoma:

```text
WorkerService.remove existia e nada o chamava.
```

Uma colônia que perdesse o lenhador numa noite de zumbis continuaria
achando que tinha um, para sempre. A vaga nunca reabria. O baú do morto
ficava reservado para sempre, e nenhum outro aldeão podia usá-lo.

Correção:

```text
fabric/event/VillagerLifecycleHandler

  AFTER_DEATH       morreu

  MOB_CONVERSION    virou zumbi
```

Os dois eventos, não só a morte: aldeão mordido por zumbi é
<em>convertido</em>, não morto, então `AFTER_DEATH` nunca dispara — e é
justamente o caso mais comum de perder um trabalhador em jogo.

Remove o trabalhador e o baú dele juntos. Um baú reservado para quem
não existe mais é um baú perdido para a colônia.

Só o evento serve como prova. Ausência na varredura não serve, e é por
isso que `remove` tinha ficado sem quem o chamasse: um aldeão fora do
raio, ou num chunk descarregado, não está morto — apenas não foi visto.

Consequência aceita:

```text
Zumbi curado volta com identidade nova.
```

Será registrado do zero e receberá a função de que a colônia mais
precisar, não necessariamente a que tinha. Preservar a antiga exigiria
rastrear a conversão nos dois sentidos.

---

Não corrigido, e por quê:

```text
O baú do jogador pode ser reivindicado — ver §9.
```

Hoje é inofensivo, porque nada move item. Vira problema real na Fase 6,
e a saída depende de decisão do autor: não há sinal de propriedade no
Vanilla para o mod se apoiar.

Verificado:

```text
164 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

Não verificado:

```text
Ambas dependem do jogo para valer. Ver §7.
```

---

## 2026-08-07 — TASK-018 e TASK-019; TASK-020 bloqueada

Teste em jogo adiado outra vez a pedido do autor. O §7 foi reescrito
como roteiro de verificação — sete itens, V1 a V7, com o método e a
ordem — para que a sessão de teste não dependa de reconstruir contexto
de memória. Seguiu-se pelo que não precisa do jogo.

Criado:

```text
core/resource/model/ColonyResources     total + repartição por baú

core/resource/service/ResourceDemand    déficit
```

Alterado:

```text
ChestInventoryReader.readColony         agrega guardando a origem

VillageDetectionHandler                 loga o número de baús
```

Decisão — o nome não é `ResourceRegistry`, como em MVP-Tasks.md:

```text
ColonyResources é imutável.
```

Registro sugere algo que se mantém e se atualiza; isto é uma leitura
datada. Prometer atualidade que não se tem seria pior que o nome
diferente. Mesma precedência de `ColonyManager` → `ColonyService`
na TASK-006.

Decisão — a repartição por baú é guardada, não só o total:

```text
O trabalhador vai ao baú, não ao total.
```

Saber que a colônia tem 64 troncos não diz a ninguém para onde andar.

Decisão — baú vazio não entra na agregação:

```text
Senão "três baús com madeira" contaria baús sem madeira.
```

Decisão — déficit não lista o que não falta:

```text
Recurso em dia fica fora do mapa, não entra com zero.
```

Um mapa que lista o que não falta obriga todo chamador a filtrar, e o
primeiro que esquecer vai gerar trabalho para buscar nada.

Decisão — sobra é déficit zero, não negativo:

```text
Ter 100 com meta de 64 é déficit 0, não -36.
```

O excedente é outra pergunta. Misturá-lo faria uma soma de déficits
cancelar falta de um recurso com sobra de outro.

---

### TASK-020 não foi feita, e não por falta de tempo

`MVP-Tasks.md`: "A Colônia deve saber o que possui e o que falta."

Ela já pode responder às duas. O que falta é **onde perguntar**. O loop
de simulação da ADR-002 e do Simulation-Loop.md nunca foi escrito —
§9 registra isso desde a Fase 4 — então não existe ciclo de colônia em
que encaixar a consulta.

Pendurá-la no `VillageDetectionHandler` seria errado: aquilo é
detecção, não simulação. A colônia passaria a pensar sobre recursos
apenas quando um jogador passasse perto o bastante para disparar a
detecção.

Falta também de onde vêm as metas de estoque. Resource-System.md
§"Necessidade de Recursos" fala em "metas mínimas" e dá um exemplo,
mas nada diz quem as define. Provavelmente saem do que a expansão
pretende construir, que é a Fase 9.

É uma lacuna de plano do mesmo tipo da que produziu a TASK-012b, e está
em §10 aguardando decisão do autor.

Verificado:

```text
184 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft (grep)
```

---

## 2026-08-07 — TASK-021 e TASK-022; TASK-023 parcial

Criado:

```text
core/task/model/TaskState      AVAILABLE, RESERVED, EXECUTING,
                               COMPLETED, CANCELLED

core/task/model/TaskPriority   SURVIVAL, PRODUCTION, CONSTRUCTION

core/task/model/TaskType       COLLECT_WOOD, CRAFT_MATERIAL, BUILD

core/task/model/Task

core/task/service/TaskService
```

Movido — e este é o ponto que mais mexeu no que já existia:

```text
core/worker/model/Capability       -> core/type/Capability

core/resource/model/ResourceType   -> core/type/ResourceType

core/resource/model/ResourceCategory -> core/type/ResourceCategory
```

Motivo: a ADR-006 §6 proíbe um domínio do Core importar outro. A tarefa
precisa dizer de que capacidade precisa e sobre qual recurso age. Com
`Capability` dentro de `core.worker` e `ResourceType` dentro de
`core.resource`, o domínio task teria de importar os dois — violação
direta.

Os três são vocabulário compartilhado, não regra de um domínio: a
tarefa declara a capacidade, a profissão declara as que tem, e nenhuma
conhece a outra. É o mesmo papel de `ColonyPos`, que já morava ali.

Verificado por varredura: nenhum domínio do Core importa outro.

---

Decisão — `TaskService`, não `TaskManager` como em MVP-Tasks.md:

```text
ADR-006 §5 removeu manager como camada.
```

Mesma decisão da TASK-006, onde `ColonyManager` virou `ColonyService`.

Decisão — existe `CANCELLED`, que MVP-Tasks.md não lista:

```text
Simulation-Loop.md exige o caso.
```

Aldeão morreu, construção removida, recurso dispensado. Sem estado
próprio, a tarefa cancelada teria de ser apagada — e apagar perde a
diferença entre "foi feita" e "deixou de fazer sentido", que é o que a
colônia precisa saber ao reavaliar.

Decisão — `release` é diferente de `cancel`:

```text
release  perdeu o executor, a tarefa continua valendo

cancel   a tarefa deixou de valer
```

Quando o lenhador morre, a colônia ainda precisa de madeira. Cancelar
ali faria a demanda sumir junto com quem a atenderia.

Decisão — reservar o que já está reservado lança:

```text
Simulation-Loop.md: uma tarefa tem um executor só.
```

Substituir em silêncio poria dois aldeões a cortar a mesma árvore, cada
um contando a madeira do outro como sua.

Decisão — `availableFor` já vem ordenada:

```text
A prioridade é regra da colônia, não de quem chama.
```

Deixar cada chamador ordenar abriria espaço para dois pontos do código
escolherem tarefas em ordens diferentes. Empate mantém ordem de
criação, e é disso que depende `sort` ser estável.

Decisão — `purgeClosed` existe:

```text
Sem limpeza o registro cresce para sempre.
```

Uma colônia produzindo por horas acumula milhares de tarefas
concluídas que ninguém consulta.

Decisão — tarefas não são persistidas:

```text
Uma tarefa é intenção do momento.
```

Retomá-la numa sessão em que o mundo mudou faria o aldeão ir cortar
uma árvore que o jogador já derrubou.

---

### O registro de tarefas foi ligado, mesmo vazio

`VillageColonyMod.TASKS` existe e nada cria tarefas: a geração de
demanda é o passo 4 do Simulation-Loop.md, que depende do loop
não escrito.

Foi ligado assim mesmo ao `VillagerLifecycleHandler`, que agora devolve
à fila as tarefas de quem morreu, e ao `ServerLifecycleHandler`, que o
esvazia. Custou três linhas, e o precedente pesou: `WorkerService.remove`
ficou sem chamador até alguém notar que colônia nenhuma reabria vaga.

---

### TASK-023 ficou pela metade, e não por falta de tempo

Os dois lados da associação existem e estão testados:

```text
TaskType declara a Capability

ProfessionRegistry.withCapability diz quem a tem

TaskService.nextFor acha a tarefa de uma capacidade
```

Falta quem junte os dois em tempo de execução. Esse código importaria
`core.task` e `core.worker` juntos, e não há hoje lugar legítimo para
ele — a ADR-006 §6 fecha os domínios, `core/type` é para tipos de valor
e `fabric/` seria regra de colônia morando na camada de integração.

Não criei a camada por conta própria. É decisão de arquitetura, e as
seis ADRs existentes foram todas aprovadas antes de virar código. Está
em §10.

Verificado:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

Core continua sem net.minecraft

Nenhum domínio do Core importa outro (varredura)
```

---

## 2026-08-07 — Sessão encerrada, pendências registradas

Nenhum código nesta entrada. O §8 foi reescrito como ponto de retomada
e passou a separar três coisas que antes estavam misturadas numa fila
só:

```text
o que precisa do jogo         P1

o que precisa de decisão      P2, P3, P4

o que não precisa de nenhum   A, B, C
```

A separação importa porque o projeto passou três sessões seguidas
adiando o item que precisa do jogo, e a fila antiga não deixava óbvio
que havia trabalho disponível sem ele.

Levantadas e não iniciadas, por decisão do autor de encerrar a sessão:

```text
A  Fabric Game Test cobriria V1, V2, V4, V5, V6 e V7

   O fabric-gametest-api-v1 já vem no Fabric API do projeto —
   confirmado no cache de dependências. Falta configurar
   runGametest, que não existe entre as tasks do Gradle hoje.

   Exigiria uma costura no VillageDetectionHandler, porque a
   detecção só é disparada por chunk e por ciclo de ticks.
   Mexer em produção para viabilizar teste é decisão que não
   se toma sozinho.

B  A ADR-006 §6 vira teste em vez de grep manual

C  Este documento passou de 2662 linhas
```

Estado ao encerrar:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

Core sem net.minecraft; nenhum domínio importa outro

9 commits à frente de origin/main, agora empurrados
```

---

## 2026-08-07 — Primeiro defeito de fronteira encontrado em jogo

A sessão de verificação começou e não chegou ao roteiro: o jogo travou o
carregamento de terreno depois de alguns minutos de caminhada.

Antes disso, uma sessão foi perdida por método. O jar instalado em
`.minecraft/mods/` era das 09:41 e continha apenas até a TASK-012 —
conferido abrindo o jar, sem `Profession`, `ChestScanner` nem `Task`
dentro. Nada das Fases 4 e 5 podia aparecer no log porque nada das
Fases 4 e 5 estava rodando. A armadilha do §11 tem agora uma irmã:

```text
Jar velho na pasta de mods não testa nada.
Conferir o conteúdo do jar, não a data do arquivo.
```

O que aquela sessão provou, ainda assim, do V1:

```text
Colony created at ColonyPos[x=886, y=69, z=922] with 26 beds
Colony ... moved to ... with 27 beds
Registered 2 villagers in colony c18264c9 (8 total)
Registered 6 villagers in colony c18264c9 (14 total)
Colony c18264c9 is now DORMANT
Saved 2 colonies with 30 workers
```

Detecção, movimentação de centro, registro incremental sem repetir os
mesmos aldeões, DORMANT ao afastar e gravação ao sair. Falta conferir
se `N` bate com a contagem real da vila, que só se vê em jogo.

---

### O travamento

Com o jar correto, o terreno parou de carregar. Não houve crash, não
houve linha de log — o `latest.log` ficou em zero byte, com o buffer
presa na thread travada. O defeito não se denunciou por log nenhum.

Quem o expôs foi um thread dump do processo, com `jstack`:

```text
"Server thread" ... TIMED_WAITING (parking)
  at net.minecraft.class_1937.method_8321          getBlockEntity
  at ChestInventoryReader.read(ChestInventoryReader.java:49)
  at ChestInventoryReader.readColony(...:116)
  at VillageDetectionHandler.logResources(...:171)
  at VillageDetectionHandler.registerVillagers(...:143)
  at VillageDetectionHandler.detectAround(...:235)
  at VillageDetectionHandler.onChunkLoad(...:78)
  at ServerChunkEvents.lambda$static$0(ServerChunkEvents.java:44)
```

A pilha diz tudo: `onChunkLoad` roda dentro do pipeline de carga de
chunk, na própria thread do servidor. Dali, `World.getBlockEntity`
carrega o chunk que faltar, e a thread passa a esperar por um chunk que
só ela mesma poderia produzir. Ela para, e com ela para todo o
carregamento de terreno.

Correção: `ChestInventoryReader.read` passou a buscar o chunk por
`getChunkManager().getWorldChunk(...)` e a devolver vazio quando ele não
está carregado — exatamente o que o `ChestScanner.findFreeChest` já
fazia, pela ADR-002 §"o mod não segura chunk". Os dois lados da leitura
de baú agora seguem a mesma regra.

Consequência aceita: baú em chunk descarregado não entra na contagem.
É o comportamento correto — a colônia não enxerga o que não está
carregado — mas cai no risco que o V5 já apontava, o de um defeito que
aparece como número plausível em vez de ausência.

---

### O que isto custou e o que ensina

```text
o defeito não estava no código de domínio

  217 testes verdes, e nenhum deles poderia pegá-lo:
  o erro é a thread errada chamando o método certo
```

```text
"instrumentar antes de suspeitar" não bastou aqui

  a thread travou antes de escrever a linha; o log
  ficou em zero byte. Contra travamento, a ferramenta
  é o thread dump, não o log
```

Isto reforça o item A do §8 — o Fabric Game Test rodaria a detecção num
servidor de verdade e teria travado o build, e não a tarde do autor.

Estado ao registrar:

```text
217 testes passando

./gradlew build → BUILD SUCCESSFUL

roteiro V1 a V7 ainda por rodar; a sessão travou antes
```

---

## 2026-08-07 — Fases 4 e 5 verificadas em jogo

A dívida de três sessões foi paga. Seis dos sete pontos do roteiro V1 a
V7 têm agora evidência de jogo real, colhida em duas sessões seguidas
depois da correção do travamento.

---

### V3 — Persistência: CONFIRMADO

```text
[22:14:03] Loaded 2 colonies with 16 workers
```

As duas leituras anteriores diziam `0 workers` e quase foram tratadas
como defeito. Não eram: liam o save das 21:52, escrito pelo jar velho.
O primeiro save escrito pelo jar atual voltou inteiro.

Nenhuma linha `Assigned` seguiu o carregamento — as funções vieram do
save e a atribuição não as desfez, que é o contrato da TASK-012b.

---

### V2 — Atribuição: CONFIRMADO

Lido dentro do `villagecolony_colonies.dat`, não só pelo log:

```text
4 LUMBERJACK · 4 MANUFACTURER · 4 FARMER · 3 BUILDER
o primeiro é LUMBERJACK
```

Rodízio entre as quatro funções, uma linha por lote novo, silêncio ao
reencontrar os mesmos aldeões.

---

### V5 — Contagem de estoque: CONFIRMADO

```text
[22:18:23] stores {OAK_LOG=128} across 2 chests
[22:20:53] stores {OAK_LOG=448, OAK_PLANKS=64} across 8 chests
```

Antes disto, toda linha dizia `nothing tracked across 0 chests`. Isso
era o esperado e não defeito: a colônia acompanha três itens, e baú de
vila vanilla tem trigo, semente e esmeralda.

Ressalva encontrada aqui e corrigida logo depois: a linha
`nothing tracked across 0 chests` não distinguia "baú vazio" de "baú em
chunk que não consegui ler". Ver a entrada seguinte.

---

### V7 — Morte: CONFIRMADO

```text
[22:21:37] Villager ... died, message: 'Saqueador atingiu Aldeão'
[22:21:37] Worker fa196508 died — profession freed, storage released
```

Não foi provocado: um saqueador matou o aldeão durante a sessão. A
contagem seguiu correta — 22 trabalhadores, menos o morto, mais quatro
registrados depois, deu 25.

A zumbificação continua sem exercício, e é o caminho mais comum em
jogo. Exige dificuldade normal ou acima.

---

### V1 e V6 — Indício, não confirmação

O registro é incremental e não repete aldeão já conhecido, ao longo de
três sessões. Falta o único critério que o log não dá: se `N` bate com
a contagem real de aldeões da vila.

Do V6, o indício é bom e aparece várias vezes:

```text
Registered 4 villagers ... / Assigned 3 professions
```

Alguém foi registrado e não empregado, que é o comportamento correto
para bebê e nitwit. O save fecha com 25 trabalhadores e 23 profissões.
Falta esperar um bebê crescer e receber função sozinho.

---

### V4 — Não verificado

Se cada aldeão pegou o baú da própria casa e não o do vizinho, o log
não diz. É inspeção visual, e continua pendente.

---

Estado ao registrar:

```text
V2, V3, V5, V7(morte)   confirmados em jogo
V1, V6                  indício forte, critério final pendente
V4                      não verificado
V7(zumbi)               não exercido

217 testes passando; ./gradlew build → BUILD SUCCESSFUL
```

---

## 2026-08-07 — Duas melhorias que a verificação cobrou

Nenhuma tarefa do MVP. As duas saíram do que a sessão de verificação
mostrou, e as duas fecham buracos de observação, não de funcionalidade.

---

### A contagem de baús passou a dizer o que não conseguiu ler

A correção do travamento criou um caso que antes não existia: baú
registrado cujo chunk não está carregado. `ColonyResources` descarta baú
vazio na agregação, então esse baú e um baú vazio saíam pelo mesmo cano
— sumiam.

O log dizia a mesma coisa nos dois casos:

```text
Colony ... stores nothing tracked across 0 chests
```

"Nenhum baú tem madeira" e "não consegui ler baú nenhum" com o mesmo
texto. É o defeito-que-parece-número que o V5 do §7 já antecipava, e
agora com uma causa concreta atrás dele.

`ChestInventoryReader.survey` devolve `ChestSurvey`: o que foi lido,
quantos baús foram alcançados — vazios inclusive — e quantos ficaram
fora de alcance. A linha passou a ser:

```text
Colony ... stores {OAK_LOG=448} in 8 of 13 chests read
Colony ... stores nothing tracked in 0 of 4 chests read (9 unreachable, chunk unloaded)
```

`readColony` continua existindo e devolvendo só o agregado. O javadoc
de `survey` diz quando preferir uma à outra: decisão de colônia tomada
sobre contagem parcial mandaria um trabalhador buscar o que ela já tem.

Fica registrado que isto é observação, não correção do risco. A colônia
ainda não *usa* `isPartial()` para se recusar a decidir — quando o loop
de simulação existir (§10 item 2), é onde essa recusa mora.

---

### A regra de dependência da ADR-006 §6 virou teste

Era o item B do §8. A regra "nenhum domínio do core importa outro" era
conferida por `grep`, à mão — o que some no dia em que alguém esquecer
de rodar. Ela já tinha cobrado uma vez, obrigando a mover `Capability`
e `ResourceType` para `core/type` na Fase 7.

`DependencyRuleTest` lê os fontes de `core/` e trava três coisas:

```text
core não importa net.minecraft nem net.fabricmc

core não importa fabric nem data

nenhum domínio do core importa outro, exceto core/type
```

Lê fonte, e não bytecode, de propósito: a regra é sobre `import`, que é
o que o autor escreve e o que a ADR proíbe. O bytecode já perdeu a
diferença entre um import e um nome qualificado.

O teste foi verificado ao contrário antes de entrar. Um arquivo
temporário em `core/colony/model` importando `Worker` e `BlockPos` fez
os dois testes certos falharem, com a mensagem apontando arquivo e
import:

```text
ADR-006 §6 — nenhum domínio do core importa outro. Violações:
  .../TempViolation.java (colony) importa com.villagecolony.core.worker.model.Worker
```

O quarto teste, `theScanReachesTheSource`, existe porque uma varredura
que não acha arquivo nenhum passa sempre — e passaria calada se o
caminho relativo quebrasse.

---

Estado ao registrar:

```text
221 testes passando (eram 217)

./gradlew build → BUILD SUCCESSFUL
```

---

## 2026-08-07 — A colônia pode encolher

Decisão do autor, tomada a partir de uma pergunta levantada na sessão de
verificação. `observedBeds` só crescia: uma vila que perdesse camas —
zumbis destroem, o jogador derruba — ficaria com o centro congelado no
lugar antigo para sempre, porque nenhuma observação futura alcançaria a
marca antiga. A partir da Fase 8 isso mandaria trabalhador andar até um
centro que não existe mais.

---

### O que não podia ser desfeito junto

A regra que impedia o encolhimento é a mesma que impede a oscilação do
§11, e essa oscilação chegou a custar o UUID de uma vila. Baixar a
guarda por completo devolveria o defeito.

O que separa os dois casos é *autoridade*: uma observação que viu menos
camas ou viu menos da vila, ou a vila encolheu de fato, e o log não
distinguia as duas.

---

### A prova de completude

Uma observação é completa quando provadamente não cortou cama alguma:

```text
toda cama de um cluster está a no máximo CLUSTER_DISTANCE
de outra cama dele — é a definição de cluster

logo, se toda cama vista está a até
SEARCH_RADIUS - CLUSTER_DISTANCE do gatilho,
qualquer cama ligada a elas cairia dentro do raio
e teria sido coletada

64 - 32 = 32 blocos de margem
```

Dentro da margem, "vi menos camas" só pode significar que a vila
encolheu. Fora dela, a resposta é "não sei", e o seguro é continuar
recusando.

`VillageCandidate` ganhou `complete`, `Colony.observe` ganhou o terceiro
argumento, e quem prova é a detecção — o Core não sabe o que é raio de
busca. `VillageScanner` passa o gatilho, que já tinha em mãos.

Limite conhecido e aceito: a prova mede na horizontal, como a
clusterização. Uma cama muito acima ou abaixo das outras entra no
cluster e poderia cair fora da esfera de busca. Vila Vanilla é de
superfície, e o erro possível é a colônia deixar de encolher — nunca
encolher errado.

---

### Como foi verificado

Teste antes de código. Os testes novos não compilavam contra a API
antiga, que é o vermelho legítimo aqui.

Depois de verdes, a regra foi revertida à mão dentro de `observe` para
confirmar que os testes a sustentam:

```text
if (beds < observedBeds) {        ← sem o && !complete

  completeViewMayShrinkTheColony        FALHOU
  completeViewMayShrinkAndMoveTheCenter FALHOU
  observeReportsMovementWhenShrinking   FALHOU

  os 12 restantes seguiram passando
```

Os doze que continuaram verdes importam tanto quanto os três que
falharam: são eles que garantem que a oscilação não voltou junto.

---

Estado ao registrar:

```text
231 testes passando (eram 221)

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo: nenhuma vila encolheu ainda
```

---

## 2026-08-07 — A prova geométrica não servia, e o jogo mostrou

Camas foram destruídas em jogo e a colônia não encolheu. O
`observedBeds` seguiu em 38, conferido dentro do `.dat`.

O log não sabia dizer por quê: a linha `moved` só sai quando o centro
muda, e encolher sem mover o centro é silencioso. Duas causas possíveis,
com correções diferentes — a regra recusou a observação menor, ou
observação menor nenhuma chegou.

Instrumentar antes de suspeitar. A linha nova saiu na sessão seguinte:

```text
[23:13:21] saw 32 beds, keeping 38 — view not provably complete
[23:13:49] saw  3 beds, keeping 38 — view not provably complete
[23:13:49] saw 33 beds, keeping 38 — view not provably complete
[23:14:21] saw 33 beds, keeping 38 — view not provably complete
[23:15:58] saw 32 beds, keeping 38 — view not provably complete
```

Cinco observações da vila encolhida, cinco recusas. A de 3 camas está
correta — é visão de borda. As de 32 e 33 é que deviam ter passado.

---

### O defeito era do critério, não do jogo

A prova exige toda cama a até 32 blocos do gatilho. Esta vila é maior
que isso, e por isso nenhuma observação real ali jamais se prova
completa. A regra estava correta no papel e inalcançável na prática.

Os testes não pegaram porque usam clusters de três camas a dez blocos de
distância — passavam com folga. O critério só funcionava em vila
pequena, e não havia teste com vila do tamanho das de verdade.

---

### A mesma janela

Decisão do autor entre três mecanismos. Escolhido: a colônia guarda de
onde veio a melhor observação, e uma varredura do mesmo ponto vendo
menos camas tem autoridade para encolher.

```text
Colony.observedFrom      âncora da melhor observação

VillageCandidate.anchor  de onde a varredura partiu

ciclo longo varre também a partir do centro de
cada colônia ativa — âncora estável entre ciclos
```

A posição do jogador nunca se repete entre ciclos; o centro da colônia
sim. Sem essa segunda varredura a âncora nunca casaria e o mecanismo
não dispararia nunca — o mesmo erro da prova geométrica.

Roda depois de `updateLifecycles`, para não varrer colônia dormente:
sem chunk carregado a varredura não acharia cama alguma e a colônia se
veria vazia.

Custo: uma consulta de POI por colônia ativa a cada ciclo de 30s. O
limite de Performance-Rules.md §5 continua respeitado — busca por raio
em torno de um ponto, nunca pelo mundo.

A prova geométrica ficou. É rara, mas é a única que serve na primeira
observação, quando ainda não há âncora com que comparar.

---

### Como foi verificado

Teste antes de código, e o primeiro deles é o caso real: vila de 38
camas, observação de 33 da mesma âncora, encolhe.

Duas mutações confirmaram que os testes sustentam as duas decisões:

```text
sem a comparação de âncora

  sameAnchorSeeingFewerBedsShrinksTheColony  FALHOU
  aBetterViewMovesTheAnchorToo               FALHOU

âncora atualizada mesmo na recusa

  aWorseViewDoesNotBecomeTheNewAnchor        FALHOU
```

A segunda mutação é a que importa mais: ela é exatamente a deriva do
§11 voltando por outro caminho, e agora existe teste que a barra.

---

Estado ao registrar:

```text
238 testes passando (eram 231)

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo: a vila encolhida ainda não encolheu a colônia
```

---

## 2026-08-07 — A âncora que nunca nascia

Segunda tentativa, segunda recusa em jogo. A sonda do centro estava
rodando — o log passou a trazer duas linhas por ciclo, uma de cada
varredura — mas a colônia seguiu em 38:

```text
[23:26:03] saw 33 beds, keeping 38   ← jogador
[23:26:03] saw 33 beds, keeping 38   ← sonda
[23:26:33] saw 33 beds, keeping 38
[23:26:33] saw 33 beds, keeping 38
```

O impasse estava no meu código, e é do tipo que teste de unidade não
encontra porque depende do estado com que a colônia nasce:

```text
a colônia vem do save com observedBeds = 38
e âncora nula

a âncora só era gravada numa observação ACEITA

nenhuma observação é aceita enquanto 33 < 38

logo a âncora nunca nasce, e nada nunca encolhe
```

Os testes anteriores não pegaram porque todos partiam de uma colônia
criada na hora, cuja primeira observação é sempre aceita e já deixava a
âncora pronta. Nenhum partia de colônia carregada do save.

---

### A sonda passou a ter memória própria

`probeAnchor` e `probeBeds` são gravados a cada leitura da sonda, aceita
ou recusada. Deixaram de ser um efeito da observação aceita e viraram o
que sempre deveriam ter sido: o registro do que a sonda viu da última
vez.

A regra de encolhimento ficou:

```text
mesma âncora da leitura anterior
   e  a leitura de agora não é maior que aquela
   e  aquela já estava abaixo da contagem registrada
```

A terceira condição é a que exige repetição. Sem ela, a sonda que viu 38
e depois 33 confirmaria o 33 contra si mesma, e uma visão parcial
isolada encolheria a colônia — dois testes falharam exatamente nisso
antes de a condição existir.

---

### Só a sonda tem âncora

A varredura que parte do jogador e a que parte do chunk carregado passam
`anchor` nulo. Não é detalhe: um jogador parado na borda da vila repete
a mesma visão pobre ciclo após ciclo, e ela se confirmaria sozinha. A
deriva do §11 entraria pela porta que abrimos para o encolhimento.

`VillageScanner.scan` recebe `isProbe` e retira a âncora quando a
varredura não é sonda. A prova de completude continua valendo nas duas,
porque ela não depende de repetição.

---

Estado ao registrar:

```text
233 testes passando

  eram 238; sete testes da âncora antiga foram
  substituídos por oito da sonda, e o total caiu
  porque a regra antiga tinha caso que não existe mais

./gradlew build → BUILD SUCCESSFUL

falta ver em jogo, pela terceira vez
```

---

## 2026-08-07 — A colônia encolheu em jogo

Terceira tentativa, e desta vez funcionou. A vila tinha 38 camas
registradas e 33 reais desde que camas foram destruídas.

```text
[23:36:42] saw 33 beds, keeping 38    ← sonda registra
[23:36:42] saw 33 beds, keeping 38

           (a sonda confirmou 33 aqui)

[23:38:42] saw  3 beds, keeping 33    ← já é 33
[23:38:42] saw 13 beds, keeping 33
[23:39:12] saw 25 beds, keeping 33
```

Confirmado fora do log, dentro do `.dat`: `observedBeds` passou de 38
para 33 e sobreviveu ao salvamento.

As três linhas depois do encolhimento são a outra metade da prova.
Visões de 3, 13 e 25 camas foram recusadas contra os 33 — a guarda
contra a deriva do §11 continua de pé enquanto o encolhimento funciona.
As duas coisas conviviam mal em teoria e convivem bem em jogo.

---

### O que custou

Três sessões de jogo do autor e duas correções minhas, ambas do mesmo
tipo: código que passava em 230+ testes e não funcionava na primeira
vez que encontrou o mundo real.

```text
prova geométrica    correta e inalcançável
                    margem de 32 blocos, vila maior que isso

âncora da sonda     nascia só numa observação aceita,
                    e nenhuma vinha enquanto a colônia
                    estivesse grande demais
```

Nenhum dos dois é erro de lógica. Os dois são erro de *premissa sobre o
mundo* — o tamanho de uma vila real, e o estado com que uma colônia
começa a sessão.

---

Estado ao registrar:

```text
233 testes passando

./gradlew build → BUILD SUCCESSFUL

encolhimento verificado em jogo
```

---

## 2026-08-07 — V7 fechado; o V4 era inverificável

A zumbificação apareceu em jogo, e distinta da morte:

```text
Worker 0bdd1c8a was converted — profession freed
Worker fc3c2162 was converted — profession freed
Worker 457439b1 was converted — profession freed, storage released
```

Os dois desfechos convivem no mesmo ataque — na mesma investida
apareceram sete linhas de `died` e três de `was converted`. A diferença
entre "morreu" e "virou zumbi" é o caminho mais comum em jogo e o que o
§7 marcava como o mais fácil de deixar passar. Está exercido.

A terceira linha mostra a outra metade: `storage released` só sai para
quem tinha baú. Quem não tinha, não solta.

Com isto o V7 fecha inteiro. Restava o V4.

---

### O V4 não falhou: ele não podia ser respondido

"Cada aldeão pegou o baú da sua casa, não o do vizinho." O autor foi
verificar em jogo e a resposta foi que não dá para saber.

```text
Registered 7 storages in colony ... (7 total)
```

Quantos, e nada mais. Nem qual baú, nem de quem, nem onde. Não existe
UI, e o baú reivindicado é igual a qualquer outro. O critério estava no
roteiro desde o começo e nenhuma sessão poderia tê-lo respondido.

É o mesmo padrão da linha `nothing tracked across 0 chests` e da linha
que faltava no encolhimento: um critério de verificação escrito sem
que existisse a instrumentação para satisfazê-lo. Três vezes no mesmo
dia.

`ChestScanner` passou a registrar cada reivindicação:

```text
Storage claimed by <uuid>: bed 1109,68,730 chest 1112,68,731 (3,3 blocks apart)
```

São as coordenadas de ir até lá e abrir o baú, que é a verificação que
o V4 pede. Uma linha por baú reivindicado, e o baú de um aldeão é
procurado uma vez só enquanto ele o tiver.

---

Estado ao registrar:

```text
V2, V3, V5, V7      confirmados em jogo
encolhimento        confirmado em jogo
V1, V6              indício forte, critério final pendente
V4                  instrumentado, ainda por verificar

233 testes passando; build verde
```

---

## 2026-08-08 — V4 respondido: um baú estava noutro andar

A instrumentação da véspera respondeu na primeira sessão. Dezesseis
baús reivindicados, com cama, baú e distância:

```text
13 de 16    1,4 a 2,4 blocos    mesmo cômodo
 3 de 16    ~5 blocos           suspeitos
```

O autor foi conferir os três em jogo. Um estava errado, e de um jeito
que a distância não denunciava:

```text
7ae2b8d3   cama 1068,65,735   baú 1068,70,735   5,0 blocos
```

Mesmo x, mesmo z, cinco blocos acima. Distância no espaço não conhece
teto: o baú estava dentro do raio de seis e noutro andar.

---

### A regra

Decisão do autor: o baú tem de estar no mesmo nível da cama.

```text
MAX_LEVEL_DIFFERENCE = 1
```

Um bloco de folga, e não zero, porque chão de vila vanilla tem degrau —
casa com piso 68 de um lado e 69 do outro é comum. Os números da sessão
sustentam a escolha: quinze dos dezesseis baús estavam a zero ou um
bloco de altura da cama; só o do outro andar estava a cinco.

A vertical saiu do cubo de busca e virou limite próprio. O horizontal
continua em seis.

Nada a migrar: o registro de baús não é persistido, é refeito a cada
sessão. A reivindicação errada simplesmente não se repete.

---

### O que a regra não resolve

Os outros dois suspeitos — camas em z=714 com baús em z=719 — não são
de altura. Se estiverem errados, é parede no meio, e altura não
distingue parede.

```text
o baú do vizinho continua alcançável
```

Isso é o item P4 do §8, a propriedade do baú, que já estava na fila e
segue lá. A regra de hoje fecha o andar de cima; a casa do lado
continua aberta.

---

Estado ao registrar:

```text
V1 a V7             respondidos; V4 gerou correção
encolhimento        confirmado em jogo

233 testes passando; build verde

a regra de nível ainda não foi vista em jogo
```

---

## 2026-08-08 — P2 e P3 decididas; a colônia passou a pensar

Duas decisões do autor destravaram as TASK-020 e TASK-023, paradas
desde a Fase 6.

---

### P2 — `core/coordination`

O código que casa tarefa com profissão precisa de `core.task` e
`core.worker` na mesma linha, e a ADR-006 §6 proíbe domínio importar
domínio. As saídas eram três; a escolhida foi um pacote próprio no core,
acima dos domínios.

A emenda é estreita: só `core/coordination` pode importar domínios, e
domínio nenhum passa a importar outro. `DependencyRuleTest` ganhou a
exceção e um teste para a direção contrária — coordenação lê domínio,
domínio não lê coordenação, ou a regra viraria um ciclo com nome bonito.

A exceção foi verificada por mutação: sem ela, `WorkAssignment` aparece
como violação da regra.

Descartada a alternativa de deixar em `fabric/`, que não exigiria emenda
nenhuma: é a única camada sem um teste de unidade sequer, e é onde todos
os defeitos desta semana apareceram.

---

### TASK-023 — quem faz o quê

`WorkAssignment` percorre os trabalhadores ociosos, lê a profissão de
cada um e reserva a tarefa mais urgente que ele saiba fazer.

```text
uma tarefa por trabalhador, um trabalhador por tarefa

  a Fase 8 vai mandar o aldeão andar até o local,
  e quem tivesse duas andaria para dois lugares

ocioso é quem não tem tarefa aberta

  não quem está parado: a caminho da árvore
  continua ocupado
```

Percorre as capacidades da profissão, não só a primeira: pedreiro que
também carrega madeira pega madeira quando não há o que construir.

---

### P3 — o loop de simulação existe

`ColonyCycle` é o ciclo da ADR-002, e roda uma vez por colônia ACTIVE a
cada `CYCLE_TICKS`:

```text
comparar o que tem com o que quer
pedir o que falta, retirar o pedido sem motivo
entregar os pedidos a quem sabe atendê-los
```

Decisões que o ciclo carrega, cada uma com um teste:

```text
um pedido por recurso, não um por ciclo

  30s de ciclo com falta permanente encheria a fila
  sem limite

pedido cancelado quando a falta acaba

  o jogador enche o baú, e o pedido perde o motivo

tarefa já iniciada não é cancelada

  a contagem muda o tempo todo, e ninguém é
  interrompido a meio caminho da árvore
```

---

### O que o ciclo se recusa a fazer

A colônia não decide sobre contagem parcial:

```text
if (survey.isPartial()) return;
```

Baú em chunk descarregado sai da soma sem avisar. Uma colônia que
concluísse "falta madeira" com metade dos baús fora de alcance mandaria
um trabalhador buscar o que ela já tem. O `ChestSurvey` de ontem existia
para dar o aviso; hoje ele tem quem o ouça.

---

### O provisório assumido

`ColonyGoals` devolve meta fixa para toda colônia: 64 de madeira, 32 de
pedra. Resource-System.md fala em metas mínimas e não diz de onde vêm; a
resposta real é a Fase 9, quando a meta sair do que a expansão pretende
construir.

Está isolado numa classe só, e a assinatura já recebe a colônia — trocar
por meta de obra não toca em `ColonyCycle`.

---

Estado ao registrar:

```text
257 testes passando (eram 233)

./gradlew build → BUILD SUCCESSFUL

nada disto foi visto em jogo ainda
```

---

## 2026-08-08 — P4 decidida: parede define propriedade

A última das três decisões do §8. Não havia critério nenhum de
propriedade: o `ChestScanner` pegava o baú livre mais próximo da cama e
não tinha como saber de quem ele era.

Sobravam dois casos que a distância não separa, e eles são o mesmo caso:

```text
o baú do vizinho    casa geminada, cama de um lado da parede
                    e baú do outro

o baú do jogador    base construída encostada na vila, sem
                    sinal no Vanilla que diga que é sua
```

Escolha do autor entre três saídas: linha livre entre a cama e o baú.

```text
dá para ir da cama ao baú sem atravessar bloco?

  sim  → mesmo cômodo, o baú é da casa
  não  → parede no meio, não é da colônia
```

Uma regra resolve os dois, porque nenhum dos dois tem sinal próprio no
Vanilla e os dois têm parede.

Descartadas: marcar o baú à mão, que inventa regra de jogo que o MVP não
tinha; e adotar só o que já existia na vila, que quebra "o jogador
constrói para a colônia".

---

### Como

Um traço do centro da cama ao centro do baú,
`RaycastContext.ShapeType.COLLIDER`. Bater no próprio baú é chegar — ele
é sólido e é o alvo. Bater na própria cama é sair, porque o traço começa
dentro dela.

Custo: um traço por baú candidato, e só quando o aldeão não tem baú.
Para quem tem, a busca inteira nem começa.

---

### O que isto piora, e por que ainda assim vale

Um baú na volta de um corredor em L está no mesmo cômodo e não tem linha
livre. Ele deixa de ser reivindicado.

É o erro na direção certa: a colônia deixa de adotar um baú que era
dela, em vez de adotar um que não era. O primeiro é um aldeão sem baú; o
segundo é a colônia contando o estoque do jogador e, a partir de ontem,
gerando tarefa com base nele.

Também custa mais para o aldeão que não acha baú nenhum: a busca se
repete a cada ciclo, e agora com um traço por candidato. São poucos baús
num raio de seis.

---

### Urgência que apareceu ontem

Enquanto nada consumia a contagem, a contaminação era inerte. Com o
`ColonyCycle` ligado, o número errado passou a **gerar tarefa**: a
colônia que soma o baú do jogador conclui que não falta madeira e não
pede nada, ou o contrário.

O §9 descrevia esse primeiro estágio como "invisível para o jogador".
Deixou de ser.

---

Estado ao registrar:

```text
257 testes passando; build verde

as três decisões do §8 estão fechadas

P4 é código de fronteira e não tem teste — a camada
fabric segue sem nenhum. Precisa de jogo
```

---

## 2026-08-08 — A colônia pensou em jogo, e o P4 fechou

Uma sessão, duas verificações, nenhuma correção necessária. Primeira vez
na semana.

---

### O loop de simulação funcionou

Linha inédita, no primeiro ciclo:

```text
Colony 0c2771b0 assigned 1 tasks (0 open)
Colony 9a5afa23 assigned 2 tasks (0 open)
```

Os números fecham com a meta de `ColonyGoals` — 64 de madeira, 32 de
pedra — e é isso que prova que a cadeia inteira está ligada, não só que
o código rodou:

```text
0c2771b0   stores {OAK_LOG=192}     madeira satisfeita,
                                    falta pedra      → 1 tarefa

9a5afa23   stores nothing tracked   falta as duas    → 2 tarefas
```

`(0 open)` diz que toda tarefa criada foi reservada no mesmo ciclo por
um trabalhador com a capacidade certa. TASK-020 e TASK-023 confirmadas
em jogo.

---

### O P4 fez o que devia

Quinze baús reivindicados, contra dezesseis antes das duas regras:

```text
14 de 15    entre 1,4 e 2,8 blocos
 1 de 15    a 5,4 blocos
```

Os dois casos que sobravam foram tratados por regras diferentes, e dá
para ver cada uma agindo:

```text
1068,65,735 → 1068,70,735    sumiu pela regra de nível

1130,69,714 → 1130,69,719    sumiu pela regra de parede
```

O segundo é o que importa: era um dos dois pares geminados de z=714 para
z=719, e a parede o barrou enquanto deixava o vizinho passar. A regra
não é um filtro cego de distância.

Sobrou um a 5,4 blocos, `1130,69,714 → 1132,69,719`, e ele não era
denunciável pelo log — cinco blocos tanto pode ser sala aberta quanto
casa do lado. O autor foi ao lugar e confirmou que dá para andar de um
ao outro sem sair da casa.

A reivindicação está correta. O V4 do §7 — "cada aldeão pegou o baú da
sua casa, não o do vizinho" — está respondido, e afirmativamente.

---

### O que isto encerra

```text
Fases 4 a 7    escritas, verificadas em jogo

V1 a V7        respondidos

§8  P2, P3, P4 decididos e implementados
```

A Fase 8 não tem mais bloqueio. TASK-024 e TASK-025 são o lenhador que
anda até a árvore, quebra o bloco e traz a madeira — a primeira vez que
o mod vai escrever no mundo em vez de só lê-lo.

---

Estado ao registrar:

```text
257 testes passando; build verde

nada pendente sem verificação
```

---

## 2026-08-08 — A camada fabric ganhou o primeiro teste

Item A do §8, aprovado pelo autor. `./gradlew runGametest` sobe um
servidor sem cliente, monta a vila, afirma e falha o build.

```text
All 2 required tests passed :)
```

Dois testes, ambos cobrindo o caminho que nenhum teste de unidade
alcança:

```text
aVillageBecomesAColony              POI vira detecção vira colônia

villagersBecomeWorkersWithAProfession   e vira trabalhador com função
```

São carregados: com `MIN_VILLAGERS` mutado para 99, os dois falham.

---

### A costura

`VillageDetectionHandler.runCycleNow` é o único código do mod que existe
por causa de teste. Faz o que o ciclo longo faz, na mesma ordem —
detectar, atualizar lifecycle, sondar do centro, simular. Se divergir do
`onServerTick`, o teste passa a verificar um caminho que o jogo não
percorre, que é pior do que não ter teste.

Em jogo nada a chama.

---

### Três premissas erradas, todas descobertas rodando

O padrão da semana se repetiu, com a diferença de que desta vez o
retorno levou segundos em vez de uma sessão do autor.

```text
meia cama não vira POI

  o primeiro teste plantava um bloco de cama só. O POI
  HOME nasce da cabeceira, e sem POI não há vila

o bioma não era problema

  a suspeita era que o mundo de teste fosse void e a
  detecção recusasse. A diagnose respondeu:
  minecraft:plains

o mundo é partilhado

  o teste negativo plantou duas camas e a diagnose achou
  cinco POIs no raio: as estruturas dos outros testes
  ficam a menos de 64 blocos
```

A mensagem de falha só respondeu isso porque carrega uma diagnose — POIs
no raio, bioma e posição absoluta. Sem ela, "esperava 1, achei 0"
mandaria adivinhar entre as três.

---

### O que não cabe aqui

O caso negativo — "camas de menos não são vila" — foi tentado e
descartado com prova. "Não existe colônia" é propriedade global, e
nenhum teste pode afirmá-la num mundo que todos partilham. Separar em
batches não resolve: os blocos permanecem entre eles.

Está coberto onde cabe, em `VillageDetectorTest`, que é onde a regra
vive.

O V3 continua humano: persistência exige fechar e reabrir o mundo, e o
gametest roda um servidor só.

Baú e estoque ainda não têm teste de jogo. São o próximo alvo natural, e
agora custam minutos em vez de sessão.

---

Estado ao registrar:

```text
257 testes de unidade + 2 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 2 required tests passed
```

---

## 2026-08-08 — O teste de jogo pagou na primeira hora

Quatro testes novos cobrindo baú e estoque, e o primeiro defeito de
produção encontrado por máquina neste projeto.

```text
All 6 required tests passed :)
```

---

### A regra de parede era um nada

A regra do P4, escrita e dada como verificada horas antes, não filtrava
coisa alguma.

O traço partia do centro da cama. A cama é sólida: ele batia nela mesma
no primeiro passo, e a regra tratava esse acerto como "cheguei". O
resultado era `true` para qualquer baú, parede incluída.

O teste plantou cama, parede de pedra e baú atrás dela, e o baú foi
reivindicado. A sonda dentro do teste apontou o culpado:

```text
traço: BLOCK em -11912041, -59, -2021780   ← a própria cama
```

Correção: o traço parte de um bloco acima da cama, onde estaria a cabeça
de quem levanta. Chegar passou a ser bater no próprio baú, ou não bater
em nada.

---

### Uma conclusão de ontem estava errada

Na sessão em jogo eu afirmei que a regra de parede tinha barrado um dos
dois pares geminados — o `4b09f9cb`, de `1130,714` para `1130,719`, que
sumiu da lista.

Não foi a regra. A regra não filtrava nada. Aquele baú sumiu porque foi
reivindicado por uma cama mais perto: `1ef9c30b`, de `1130,717`, está a
dois blocos dele contra cinco.

A evidência era compatível com a explicação e não a sustentava. Fica
registrado porque o erro não foi de código: foi de leitura.

---

### O que os quatro testes cobrem

```text
aChestInTheSameRoomIsClaimed        o caminho feliz
aChestBehindAWallIsNotClaimed       a regra do P4
aChestOnAnotherLevelIsNotClaimed    a regra de nível
theColonyCountsWhatTheChestHolds    o V5, com número afirmado
```

Todas as afirmações são ancoradas na posição do baú plantado, via
`StorageRegistry.isTaken`. Contagem global não vale aqui, e é o mesmo
motivo de §15 mais acima: o mundo é um só e as estruturas ficam a menos
de 64 blocos. "Este baú tem dono" e "este baú não tem" são locais e
valem.

A memória `HOME` do aldeão é escrita à mão. Em jogo o cérebro dele
reivindica a cama sozinho, o que leva tempo e depende do ciclo dele;
o que estes testes verificam é o que o mod faz depois de existir casa.

---

### O que isto significa para o método

```text
o defeito estava em produção desde o commit e6ac113

a sessão de jogo que o "verificou" não podia pegá-lo:
o sintoma era um baú a mais reivindicado num canto da
vila, indistinguível de um baú legítimo

o teste pegou em segundos, e disse onde
```

É o argumento do item A por inteiro, e agora com um caso concreto.

---

Estado ao registrar:

```text
257 testes de unidade + 6 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 6 required tests passed

a correção da regra de parede não foi vista em jogo
```

---

## 2026-08-08 — Fecho da sessão

O que foi feito, na ordem em que aconteceu:

```text
paga a dívida de verificação das Fases 4 e 5
  V1 a V7 respondidos; V4 gerou correção

quatro defeitos de fronteira achados e corrigidos
  travamento da thread do servidor
  prova geométrica inalcançável
  âncora da sonda que nunca nascia
  baú noutro andar

decidido e implementado o encolhimento da colônia

decididas e implementadas as três do §8
  P2 core/coordination, ADR-006 emendada
  P3 ColonyCycle, o loop da ADR-002
  P4 linha livre entre cama e baú

item A do §8: runGametest existe, com seis casos

quinto defeito, achado por máquina
  a regra de parede não filtrava nada
```

Fases 4 a 7 fechadas e verificadas em jogo. §3, §5, §6, §7, §8 e §10
reescritos para descrever o projeto de hoje.

---

### Duas correções a coisas que eu afirmei

```text
"a regra de parede barrou o par 1130,714 → 1130,719"

  não barrou. A regra não filtrava nada. O baú foi para
  uma cama mais perto, a dois blocos contra cinco.

contagem de testes num commit

  informei 251; eram 246. Corrigido no commit seguinte.
```

A primeira é a que importa: a evidência era compatível com a explicação
e não a sustentava, e foi apresentada como se sustentasse.

---

### O que fica para a próxima sessão

```text
decisão   regras do lenhador — §7 e §10 item 1

trabalho  TASK-024 e TASK-025, a Fase 8

dívida    gametest para morte, zumbificação,
          encolhimento e geração de tarefa

dívida    consolidar este documento, item B do §8
```

---

Estado ao encerrar:

```text
257 testes de unidade + 6 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 6 required tests passed

nada escrito e não verificado, salvo a correção da
regra de parede, que tem gametest e não tem jogo
```

---

## 2026-08-08 — Fase 8: o mod passou a escrever no mundo

TASK-024 e TASK-025. A virada de natureza do projeto: até aqui tudo
lia, agora derruba árvore e guarda madeira.

---

### As regras, decididas pelo autor

```text
alvo     só oak_log. Folha, terra e qualquer outro
         bloco ficam. Ao terminar a árvore, planta
         muda na base

item     vai direto para o baú do trabalhador, sem
         passar por item no chão

alcance  64 blocos do centro da colônia
```

O item foi a decisão menos óbvia. Item no chão despawna em cinco
minutos, cai n'água, e outro mob o pega — a contagem da colônia passaria
a mentir sem nada avisar, que é a família de defeito que mais custou
nesta semana.

O alcance ficou em 64, e não nos 32 que eu recomendei: é o mesmo raio da
detecção de vila.

---

### O que foi escrito

```text
TreeScanner      acha o carvalho mais próximo do centro

TreeHarvester    derruba os troncos ligados e replanta

ChestDepositor   guarda no baú o que couber, devolve o resto

LumberjackWork   o passo de trabalho por ciclo: achar,
                 andar, derrubar, guardar
```

`TreeScanner` não varre volume. Raio 64 em três dimensões são milhões de
blocos, e Performance-Rules.md §5 e §6 proíbem esse caminho: ele percorre
colunas em espiral a partir do centro, usa o mapa de altura para saber
onde está a superfície, e para no teto de 4096 colunas. Parar no teto é
"não achei perto", e o ciclo seguinte tenta de novo.

`TreeHarvester` tem teto de 24 troncos por árvore. Carvalho comum tem
entre quatro e sete; o teto existe para o carvalho gigante e para a casa
de tronco que o jogador tenha encostado numa árvore.

---

### Seis testes de jogo, e os negativos importam mais

```text
fellingTakesTheWholeTrunk        derruba os quatro troncos
fellingReplantsASapling          muda no lugar da base
fellingLeavesTheLeavesAlone      folha fica
fellingIgnoresOtherWoods         bétula fica
theWoodGoesIntoTheChest          madeira entra e é contada
theSearchFindsATreeNearby        a varredura acha
```

Os dois do meio são os que protegem a construção de quem joga.
Verificados por mutação: com o `TreeHarvester` tornado guloso —
aceitando folha e bétula — os dois falham, e só eles.

---

### O limite conhecido

O movimento. O cérebro Vanilla do aldeão tem agenda própria e pode
sobrescrever o destino que `LumberjackWork` pede. O caminho correto é
uma task no `Brain`, que é mudança maior e mexe no comportamento dele
fora do trabalho.

Hoje o pedido é repetido a cada ciclo, e a derrubada só acontece a três
blocos da árvore. Se o aldeão não chegar, a linha `felled` não aparece —
é o que uma sessão de jogo vai dizer, e é a única parte da Fase 8 que os
testes não conseguem responder.

Está em §10 como decisão pendente.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

a derrubada nunca rodou em jogo
```

---

## 2026-08-08 — A Fase 8 quebrou o jogo, e o diagnóstico ficou incompleto

Primeira sessão com a Fase 8 instalada: o terreno quase não carregou e
os aldeões ficaram parados. O jogo foi fechado antes de eu poder tirar
um thread dump, e o `latest.log` parou às 03:32:23 com o buffer não
descarregado — a mesma assinatura do travamento de 2026-08-07.

```text
[03:32:23] Storage claimed by e8f56d2b ...
[03:32:23] Storage claimed by a60c4f43 ...
            (nada mais)
```

**Não sei qual dos três defeitos abaixo causou o quê.** O que se sabe é
que os três existiam, e que os três são do tipo que já travou a thread
antes.

---

### O que estava errado

```text
1  TreeHarvester lia e escrevia com world.getBlockState

   O mesmo erro de 2026-08-07, repetido no primeiro
   código que escreve no mundo. Um tronco na borda de
   chunk faz o vizinho cair em chunk descarregado, e a
   leitura o carrega à força — gerando terreno dentro do
   laço, na thread do servidor.

   Agora todo acesso passa por getWorldChunk com nulo
   checado, e o que não está carregado é pulado.

2  findVillager varria uma caixa de 128 blocos de lado

   Por tarefa, por ciclo. O servidor já indexa entidade
   por UUID: world.getEntity(uuid) responde direto.

3  a espiral iterava o miolo para descartá-lo

   Para olhar quatro mil colunas, o laço percorria mais
   de um milhão de posições. O salto agora pula o miolo,
   e o teto caiu de 4096 para 1024 colunas.
```

---

### O que isto custa admitir

Os doze testes de jogo passaram antes, durante e depois. Nenhum deles
mede tempo nem toca em chunk descarregado: a estrutura do gametest é
pequena e está inteiramente carregada.

```text
o gametest cobre comportamento, não custo

o gametest cobre o mundo montado, não o mundo real
```

É o mesmo limite que o §11 registra para o teste de unidade, um nível
acima. Uma bancada que não pode falhar por lentidão não protege contra
lentidão.

---

### O que fica em aberto

A correção não foi verificada em jogo, e o diagnóstico não foi
confirmado — foi deduzido do código. Se o travamento voltar, o caminho
é o mesmo que funcionou antes: manter o jogo aberto e tirar um thread
dump com `jstack`, que aponta a linha exata.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

a Fase 8 travou o jogo uma vez; a correção não foi vista
```

---

## 2026-08-08 — A correção do travamento funcionou; nome sobre a cabeça

Sessão de dois minutos com a Fase 8 corrigida:

```text
[03:38:44] assigned 1 tasks (0 open)
[03:39:14] assigned 1 tasks (0 open)
[03:39:44] assigned 1 tasks (0 open)
[03:40:12] Saved 3 colonies with 45 workers
```

Ciclos a cada trinta segundos, mundo salvo, saída limpa. O travamento
não voltou, e as três correções ficam confirmadas — sem saber, ainda,
qual das três era a causa.

Nenhuma linha `felled`. O lenhador não chegou à árvore, que é o limite
registrado em §10: o cérebro Vanilla tem agenda própria e sobrescreve o
destino pedido.

---

### Identificar o trabalhador em jogo

Pedido do autor: pôr uma skin no lenhador para reconhecê-lo. A skin
enviada era de jogador, no layout 64×64 de player, e aldeão usa modelo e
UV próprios — aplicá-la daria textura embaralhada.

Mais de fundo: a profissão é nossa e existe só no servidor. Textura por
profissão exigiria mixin de renderização no cliente, sincronização por
rede e ADR nova, já que a ADR-004 permite um mixin só. O mod deixaria de
funcionar em cliente Vanilla.

Escolha do autor entre três: nome sobre a cabeça.

```text
LUMBERJACK    → Lenhador
MANUFACTURER  → Fabricante
FARMER        → Fazendeiro
BUILDER       → Construtor
```

`WorkerNameplate` nunca sobrescreve nome existente: aldeão batizado com
etiqueta pelo jogador continua com o nome dele. Trabalhador sem
profissão fica sem nome — bebê e nitwit são o caso comum, e chamá-los de
trabalhador diria algo falso.

Texto literal e não chave de tradução: o mod roda no servidor e o
cliente pode ser Vanilla puro, que mostraria a chave crua.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo; build verde

travamento da Fase 8: corrigido e confirmado em jogo
derrubada de árvore: nunca aconteceu em jogo
nomes: escritos, não vistos
```

---

## 2026-08-08 — Os nomes apareceram; o que está pronto e o que falta

```text
[03:49:16] Named 1 workers in colony 0c2771b0
[03:49:16] Named 7 workers in colony 0c2771b0
[03:49:44] Named 7 workers in colony 9a5afa23
[03:49:44] Named 23 workers in colony 0c2771b0
```

Quarenta e quatro trabalhadores nomeados, confirmados em jogo pelo
autor. Vêm em lotes porque a nomeação acompanha a detecção, que enxerga
a vila por partes.

---

### O que está pronto e verificado em jogo

```text
detecção de vila, identidade e persistência
registro de aldeões como trabalhadores
atribuição de profissão, com rodízio
descoberta do baú da casa, com regra de nível e de parede
contagem de estoque, com aviso de leitura parcial
encolhimento da colônia por sonda repetida
ciclo de simulação: déficit vira tarefa
distribuição de tarefa por capacidade
nome da profissão sobre a cabeça
```

---

### O que está escrito e nunca aconteceu em jogo

```text
nada da Fase 8
```

A Fase 8 fechou em 2026-08-08 às 05:32:44, com a linha
`Worker e8f56d2b felled 6 logs at 1120, 64, 669`. A tarefa nasce, é
reservada, o aldeão anda até a árvore, derruba, e a madeira entra no
baú — tudo visto em jogo, não só em teste.

A Fase 8 está coberta por dez testes de jogo, e quatro deles existem
para provar o que o lenhador **não** quebra.

---

### O que falta, em ordem

```text
1  colher até o baú encher            FEITO em 2026-08-11

2  colher no tempo de um jogador      FEITO em 2026-08-11

3  a fila de tarefas não esvazia      FEITO em 2026-08-11, junto com a
                                      Regra 1 — ver §17, E1

4  gametest para o que falta          morte, zumbificação, encolhimento,
                                      e o ciclo gerando tarefa

5  consolidar este documento          passou de 4700 linhas

6  um lenhador de cada vez            FEITO em 2026-08-11
```

Os itens 1 e 2 são a mesma decisão vista de dois lados: quanto colher, e
em quanto tempo. Juntos eles tiram o lenhador do regime de "uma árvore
inteira por ciclo, para sempre" e o põem no regime de trabalho contínuo
com um teto real.

O item 4 tem uma ressalva que a Fase 8 deixou clara: gametest cobre
comportamento, não custo. O travamento que quebrou o jogo do autor
passou por doze testes verdes.

---

Estado ao registrar:

```text
257 testes de unidade + 12 de jogo

./gradlew build → BUILD SUCCESSFUL
./gradlew runGametest → All 12 required tests passed

uma única coisa escrita e não vista em jogo: a derrubada
```

---

---

## 2026-08-08 — O lenhador derrubou; a colheita ganhou ordem

```text
[05:32:14] Worker e8f56d2b heading to the tree at 1120, 64, 669
           — 12 blocks away, work time: yes, path held: no, doing: idle
[05:32:44] Worker e8f56d2b felled 6 logs at 1120, 64, 669
```

Trinta segundos entre uma linha e outra: um ciclo. O bloqueio da Fase 8
caiu.

O caminho até aqui passou por três coisas, e só a primeira era a
prevista:

A task no Brain. `startMovingTo` era um pedido na língua errada; quem
manda no caminho do aldeão é a memória `WALK_TARGET`, e as tasks Vanilla
de movimento só começam com ela vazia.

O travamento ao carregar o mapa, que não tinha nada a ver com o Brain — o
jar que travou era anterior a ele. A detecção rodava inteira dentro do
evento de chunk carregado, uma vez por chunk, e uma vila de trinta camas
ocupa dezenas deles. Ao abrir o mundo, centenas chegam no mesmo tick.
Agora o gatilho enfileira e o tick drena uma varredura.

E duas rodadas de instrumentação, porque as duas primeiras sessões não
souberam dizer o que tinha acontecido. A primeira linha só falava quando
a árvore mudava, e deu uma linha e sete ciclos de silêncio. A segunda
fala a cada ciclo e diz distância, horário de trabalho, se a memória de
caminho sobreviveu e que Activity o Brain escolheu — e foi ela que
mostrou `work time: yes` e a chegada.

Detalhe que a linha revelou e que vale guardar: `path held: no`. No
instante da leitura o Vanilla já tinha descartado o `WALK_TARGET`, e o
aldeão chegou mesmo assim. A task repõe no tick seguinte, e é a
reposição — não a primeira escrita — que faz o caminho acontecer.

---

Com a derrubada em jogo, o autor fechou a regra da colheita. Ver §10.

O que mudou de comportamento: a árvore que não cabe no baú fica de pé,
em vez de virar madeira destruída; o tronco cortado no teto de 24 não
replanta, e a muda espera o último tronco cair; e a coluna acima da muda
é aberta de folha, senão ela nunca vira árvore.

A quarta regra abre exceção em "folha não é alvo". O teste antigo dessa
regra punha a folha exatamente na coluna da muda — ou seja, ele
guardava as duas coisas ao mesmo tempo e agora tinha de escolher. A
folha passou para o lado do tronco, que é o caso que a regra sempre quis
proteger: construção do jogador feita de folha.

---

Estado ao registrar:

```text
257 testes de unidade + 16 de jogo; build verde

Fase 8: fechada e verificada em jogo
travamento ao carregar: corrigido e confirmado em jogo
regra da colheita: escrita e coberta por teste, não vista em jogo
```

---

## 2026-08-08 — Todas as árvores, e a copa junto

Pedido do autor, com três decisões tomadas na hora:

```text
folhas       quebrar as folhas da árvore derrubada e
             recolher o que elas dropam

contagem     tipo próprio por madeira, somando numa
             categoria para efeito de meta

alcance      Overworld inteiro; Nether e bambu ficam na
             tabela, prontos para ligar
```

O centro da mudança é `TreeSpecies`: uma linha por árvore, e cada linha
diz tronco, folha, muda e recurso. Acrescentar uma árvore é acrescentar
uma linha — e o `MinecraftTypeAdapter` lê a mesma tabela, então a
contagem no estoque passa a existir sozinha.

Não é a tag `minecraft:logs` de propósito. A tag inclui tronco
descascado e bloco de madeira, que são material de construção do
jogador, não árvore, e não diz que muda replanta o quê.

Nether e bambu ficaram de fora com motivo, não por esquecimento: caule
carmesim e distorcido não têm muda, têm fungo, e o fungo só vira árvore
em nylium e com farinha de osso. Bambu não tem muda nenhuma — cresce da
própria base, e derrubá-lo inteiro impede que se reponha. Os dois pedem
um campo a mais na tabela, e ele entra quando houver colônia num bioma
que os tenha.

---

O grupo de recursos é a parte que muda o comportamento da colônia, e
não só do lenhador. `ResourceGroup.WOOD` faz o déficit somar as oito
madeiras: quem tem o baú cheio de bétula deixa de mandar buscar
carvalho. O estoque continua sabendo o tipo de cada tronco — é o
déficit que soma.

Isso resolve metade do que a sessão de 05:26 mostrou: a tarefa que
nascia a cada ciclo, para sempre. A outra metade é a meta ser constante,
que continua sendo a Fase 9.

---

A colheita agora devolve `Harvest` em vez de um número: quantos troncos,
quantas folhas, e os itens já somados por tipo. Os drops saem da mesma
tabela de loot que o jogo consultaria — é dela que vêm a muda de vez em
quando, a maçã do carvalho e o graveto. Repetir essas probabilidades no
mod seria inventar uma segunda verdade sobre o que uma árvore dá.

A copa é achada a partir dos troncos, nunca de um raio, e para a seis
blocos de qualquer tronco. Sem esse limite, copas encostadas ligariam
uma árvore à vizinha e derrubar uma levaria a copa de meia floresta.

O teste antigo "folha não é alvo" tinha de mudar de novo, e desta vez de
premissa: a copa agora vem junto. O que sobrou dele são três testes com
o que a regra ainda protege — folha longe do tronco, folha de outra
espécie, e o teto que não é folha.

---

Estado ao registrar:

```text
260 testes de unidade + 20 de jogo; build verde

Fase 8: fechada e verificada em jogo
todas as árvores, copa e drops: escritos e cobertos por teste,
não vistos em jogo
```


---

## 2026-08-11 — As duas regras da colheita, e o lenhador ganhou relógio

As regras 1 e 2 do §16 saíram do papel. Não foram duas mudanças: são a
mesma vista de dois lados, e foi assim que couberam numa sessão só.

---

### A meta virou uma pergunta sobre o mundo

```text
antes    meta = 64 de madeira, escrito em ColonyGoals
agora    meta = o que está guardado + o que ainda cabe
```

O déficit que `ResourceDemand` tira dessa meta é exatamente o espaço
livre, e nada em `ResourceDemand` precisou mudar para isso — a conta
dele sempre foi meta menos estoque. Baú cheio dá meta igual ao estoque,
déficit zero, nenhuma tarefa nova.

O espaço é medido por grupo, e não por item. Meia pilha de bétula num
slot é espaço para madeira mesmo que o próximo tronco seja de carvalho;
perguntar por um item só faria a colônia enxergar menos espaço do que
tem e parar de colher antes da hora. Slot ocupado por item do jogador
não conta como espaço — se contasse, a meta seria maior que o baú e o
lenhador colheria o que não caberia, que é a perda que a regra veio
evitar.

`ColonyGoals` recebe estoque e espaço por parâmetro. A camada core não
conhece baú, e não vai conhecer: quem mede é a camada fabric, e o que
atravessa a fronteira é um número.

---

### A árvore parou de cair dentro de um tick

O tempo de cada bloco sai da fórmula do Vanilla, e nenhum número dela
está escrito no mod:

```text
fração por tick = velocidade da ferramenta ÷ dureza ÷ divisor

divisor 30   quando a ferramenta colhe o bloco
divisor 100  quando não colhe
```

A dureza vem do bloco, a velocidade vem da ferramenta contra aquele
bloco, e o divisor vem de a ferramenta colher ou não. Machado de ferro
contra tronco dá dez ticks — meio segundo, que é o que um jogador leva —
e a folha sai da mesma conta, sem tabela paralela para envelhecer.

Isso obrigou o trabalho do lenhador a sair do ciclo de 600 ticks. São
dois relógios agora:

```text
ciclo de 600   despacha: abre trabalho para tarefa nova, fecha o de
               tarefa encerrada

tick           trabalha: avança um contador e, quando ele estoura,
               quebra um bloco
```

O custo por tick é um contador por lenhador. A parte cara — a varredura
por árvore do `TreeScanner`, que olha até mil colunas — tem orçamento de
uma por tick no servidor inteiro. Sem esse teto a Regra 1, que faz o
lenhador querer árvore nova assim que termina uma, transformaria
trabalho contínuo em varredura contínua.

A rachadura é desenhada e o braço balança. Sem isso a Regra 2 seria
invisível: o jogador veria um aldeão parado ao lado de uma árvore que
some sozinha meio minuto depois.

---

### A tarefa passou a durar

Decisão tomada nesta sessão, e é o que fecha o E1. A tarefa nasce com o
espaço livre como alvo e só termina quando não cabe mais madeira, com o
lenhador derrubando uma árvore atrás da outra dentro dela. Antes ela
morria em cada árvore e renascia no ciclo seguinte.

A outra metade do E1 era mais boba do que parecia: `purgeClosed` existia
desde a Fase 7 e nunca tinha sido chamado por ninguém. Agora é chamado
ao fim de cada ciclo.

---

### Duas coisas que apareceram no caminho

A pedra saiu da meta. Ninguém minera no MVP, e `ColonyCycle.typeFor`
manda todo recurso NATURAL para `COLLECT_WOOD`: a meta de 32 de pedra
virava uma tarefa de coleta que só o lenhador podia pegar, e ele
derrubava árvore para atendê-la. O `LumberjackWork` também passou a
filtrar por `ResourceGroup.WOOD`, para que o recurso — e não só o tipo
de tarefa — decida o que ele faz. Era um defeito que já existia; a Regra
1 o teria tornado permanente.

A colheita passou a conferir a espécie antes de quebrar cada bloco. O
risco nasceu com a Regra 2: entre planejar a árvore e chegar num bloco
dela passam-se dezenas de ticks, e nesse meio-tempo o jogador pode ter
derrubado o tronco e posto uma tábua ali. Quebrar o que estiver na
posição sem perguntar seria quebrar a construção dele.

---

### O que ficou provado, e por qual teste

```text
266 testes de unidade + 27 de jogo; build verde
```

O de jogo que importa é o que tica o mundo: colônia com tarefa, lenhador
com baú, aldeão ao pé da árvore, servidor ticando. Aos cinco ticks
nenhum tronco caiu — porque o tronco pede dez — e mais adiante a madeira
está no baú. Os outros verificam as peças, e todas podiam estar certas
com o lenhador parado: foi exatamente isso que aconteceu no bloqueio da
Fase 8, quando a versão que chamava `startMovingTo` passou por todos os
testes de derrubada sem o aldeão nunca chegar à árvore.

---

### O que esta sessão não fez

**Nada disto foi visto em jogo.** O build está verde e o mundo de teste
tica, mas ninguém abriu o save e olhou. É a mesma situação em que a
sessão de 2026-08-08 estava antes de a derrubada acontecer, e o §11
existe porque defeitos desta camada só aparecem lá — em particular o
custo, que gametest não cobre: o travamento que quebrou o jogo do autor
passou por doze testes verdes.

**Um lenhador de cada vez.** `ColonyCycle` abre uma tarefa por recurso, e
`WorkAssignment` a entrega a um trabalhador só. Isso já era verdade
antes, mas era invisível: a tarefa durava uma árvore e girava entre os
aldeões a cada ciclo. Com a tarefa durando até o baú encher, um único
lenhador trabalha por muito tempo e os outros ficam parados. Repartir uma
meta de colônia entre vários trabalhadores é uma decisão que não foi
tomada, e não foi tomada de propósito — não estava no pedido.

**O E2 continua aberto.** A sonda que vê 5 de 31 camas não foi
investigada nesta sessão.

**A perda de sobra (E3) ficou mais rara, não impossível.** O espaço é
conferido antes de cada árvore, e um trabalhador que só começa quando
cabe a árvore inteira quase nunca chega ao limite no meio. O aviso em
WARN continua lá para o caso de o jogador mexer no baú durante a
colheita — que agora leva muito mais tempo, e portanto é mais provável
do que era.

---

## 2026-08-11 — Um pedido por lenhador, e uma árvore para cada um

O item 6 tinha sido aberto poucas horas antes, na entrada acima, como
uma consequência da Regra 1 que não estava no pedido. O autor mandou
fechá-lo.

---

### O que estava errado

`ColonyCycle` abria um pedido por recurso, e Simulation-Loop.md é
explícito: uma tarefa tem um executor só. Uma vila com cinco lenhadores
punha quatro a olhar o quinto.

Isso já era verdade antes da Regra 1 e quase não aparecia, porque a
tarefa durava uma árvore e girava entre os aldeões a cada ciclo. Foi a
tarefa passar a durar até o baú encher que transformou um rodízio
disfarçado em quatro aldeões parados.

---

### O teto é quem sabe fazer, não quem está livre

```text
pedidos abertos de um recurso  ≤  trabalhadores capazes da colônia
```

A tentação era contar ociosos. Seria errado: quem está executando segura
um dos pedidos abertos e já está contado, e olhar só os ociosos abriria
uma tarefa nova a cada ciclo para quem já trabalha — o E1 de volta por
outra porta, com outro nome.

O teto acompanha a colônia nos dois sentidos. Um aldeão que vira
lenhador no meio da partida ganha trabalho no ciclo seguinte, sem
esperar a fila esvaziar.

A quantidade de cada pedido é a falta repartida entre eles. É divisão de
fachada e o código diz isso: quem de fato encerra o trabalho é o espaço
no baú **daquele** trabalhador, conferido a cada árvore. O número na
tarefa serve para o log e para o dia em que houver recurso cuja coleta
não passe por baú próprio.

---

### O defeito que a correção trouxe junto

```text
a busca por árvore parte do centro da colônia
a busca é determinística
dois lenhadores procurando  →  o mesmo tronco para os dois
```

Um derruba, o outro fica ao lado de um toco. Não é hipótese: é o que o
teste mostra quando se desliga a reserva.

O trabalho em curso passou a reservar os troncos do seu plano, e
`TreeScanner.findNearestLog` ganhou um filtro para pular o que já tem
dono — a espiral simplesmente continua até achar árvore livre.

Só o tronco entra na reserva. A busca nunca devolve folha, então
reservar a copa não impediria colisão nenhuma e faria o conjunto crescer
sete vezes à toa.

Tronco recusado não conta para o teto de mil colunas do scanner: a
coluna foi olhada e custou o mesmo. Um lenhador cercado de árvores
tomadas desiste no mesmo lugar em que desistiria se não houvesse árvore
nenhuma.

A reserva é devolvida em toda saída — árvore terminada, tarefa
encerrada, trabalhador morto, servidor parando. Uma reserva esquecida é
uma árvore que ninguém mais corta até reiniciar.

---

### Mudança de comportamento que vale registrar

Colônia sem ninguém capaz não abre mais o pedido. Antes ele nascia e
ficava na fila para sempre: nada o cancelava, porque a falta continuava,
e nada o concluía, porque não havia executor. Era assim que a meta de
pedra sobrevivia antes de sair.

Cinco testes de `ColonyCycleTest` mudaram por causa disto. Eles rodavam
sem registrar trabalhador nenhum e verificavam que a tarefa nascia — o
que agora é justamente o caso em que ela não deve nascer. Passaram a
registrar um lenhador, que é a situação que descrevem.

---

### O que ficou provado, e por qual teste

```text
271 testes de unidade + 28 de jogo; build verde
```

O teste dos dois lenhadores foi conferido pelo avesso: com a reserva
desligada, ele falha. É a única forma de saber que ele mede alguma
coisa, e vale a pena registrar que foi feito — um teste verde que
continuaria verde com o defeito de volta não prova nada.

---

### O que esta sessão não fez

**Continua sem verificação em jogo.** Vale para esta entrada e para a
anterior: nada das regras 1 e 2, nem o pedido por lenhador, foi visto
num save de verdade. O custo é o que mais preocupa — vários lenhadores
quebrando bloco a cada tick é trabalho por tick que antes não existia, e
gametest não mede custo.

**Nada distribui os lenhadores pelo mapa.** A reserva impede que dois
peguem o mesmo tronco, mas todos continuam procurando a partir do mesmo
centro: com muitos lenhadores, eles trabalham em árvores vizinhas, em
anéis cada vez mais largos. Repartir a floresta por setor é outra
decisão, e não foi tomada.

**O E2 continua aberto.**

---

## 2026-08-11 — O E2 investigado: eram três coisas, e a suspeita estava errada

O E2 estava aberto desde 2026-08-08 com uma frase que envelheceu mal:
"se a vila tiver de fato encolhido, a colônia está com 31 camas que não
existem". As 31 existem. O que não existia era o entendimento do que a
linha de 5 camas queria dizer.

A investigação começou onde deveria ter começado: o log real da sessão,
preservado em `AppData\Roaming\.minecraft\logs`. Trezentas e vinte e
nove linhas do E2.

---

### O que a estatística do log disse

```text
197×  saw 28 beds, keeping 31
114×  saw  5 beds, keeping 31
  5×  saw 27       1×  saw 24, 23, 20, 19, 4, 3
```

O 28 e o 5 são estáveis. As leituras esparsas — 19, 20, 23, 24, 27 — são
as varreduras que partem da posição do jogador, e variam porque ele
anda.

Uma visão parcial da mesma vila oscilaria como essas. Um 5 que se repete
cento e catorze vezes não é visão parcial: é outra coisa, sempre a
mesma.

---

### F1 — a linha de 5 camas é um segundo aglomerado

```text
cluster        separa o que está a mais de 32 blocos
adopt          considera a mesma vila o que está a até 64 do centro
```

Entre 32 e 64 existe uma faixa em que um punhado de camas é, ao mesmo
tempo, **outro aglomerado** e **a mesma colônia**. O vizinho de três a
cinco camas caiu exatamente nela.

Nunca foram 5 de 31. São 5 de outra coisa, contadas como se fossem a
mesma vila.

---

### F2 — o 28 contra 31 é geometria, não perda

A prova de completude exige toda cama a até
`SEARCH_RADIUS − CLUSTER_DISTANCE`, que são 32 blocos do gatilho. Uma
vila de 31 camas é muito maior que isso.

Consequência que vale escrever com todas as letras: **nenhuma sonda
ancorada no centro de uma vila grande vai provar completude, nunca.** O
`complete` é falso por construção para qualquer vila com mais de 32
blocos de raio, e a sonda alcança 28 das 31 porque as outras três estão
além do raio de 64 a partir do centro. Foram vistas numa sessão anterior
de um ponto mais favorável, e por isso ficaram registradas.

A prova geométrica não está errada — está inútil para o tamanho de vila
que o jogo produz. Já havia suspeita disso em §15, 2026-08-07.

---

### F3 — o defeito de verdade, achado por reprodução

```text
antes:  observedBeds=31  center=[27, 64, 0]
depois: observedBeds=5   center=[4, 64, 40]
```

Uma varredura, dois candidatos, a mesma colônia. Os dois chegam com a
mesma âncora — é a mesma varredura. O primeiro grava a leitura da sonda,
e o segundo é confirmado por ela **dentro do mesmo tick**.

A regra "duas leituras da mesma âncora confirmam uma diminuição" foi
escrita para ciclos sucessivos, e nada exigia que fossem sucessivos.

O estrago não é cosmético: o centro pula quarenta blocos para o vizinho,
e o centro é de onde o lenhador procura árvore e de onde a sonda do
ciclo seguinte parte. A contagem de vagas de profissão vai junto.

---

### A correção

Uma observação por colônia por varredura, e vence a que viu mais. É o
mesmo critério de `Colony#observe`: quem enxergou menos não tem
autoridade sobre quem enxergou mais.

O agrupamento é calculado antes de qualquer adoção, e por isso
`bestPerColony` devolve lista em vez de adotar — adotar move centros, e
um centro movido mudaria a resposta de `findNearest` para os candidatos
seguintes da mesma varredura.

Candidato que não cai em colônia conhecida continua passando. Agrupá-lo
faria a detecção perder vila.

---

### A instrumentação que faltava

A linha dizia `saw 5 beds, keeping 31` e não dizia **onde**. Foi essa
cegueira que fez o E2 durar três dias parecendo sonda com defeito.

Agora diz o centro do candidato e a âncora da varredura. Com o centro na
linha, dois aglomerados diferentes se distinguem de imediato de uma
leitura pobre do mesmo.

É o §11 de novo, e desta vez cobrado por um erro que já estava escrito:
a linha que expõe o caso precisa existir antes de alguém desconfiar
dele — e precisa dizer o suficiente.

---

### O que ficou sem resposta

**Por que o jar daquela noite nunca encolheu.** O log tem 329 linhas e
todas dizem "keeping 31": a colônia nunca mudou de tamanho. Pelo código
de hoje isso não deveria ser possível — mesmo sem o segundo candidato,
uma leitura ancorada de 28 se autoconfirmaria no ciclo seguinte e a
colônia cairia para 28.

O jar da sessão foi sobrescrito às 06:02 de 2026-08-08 e não dá para
diferenciá-lo do que estava no repositório. Fica registrado como
pergunta em aberto, e não como causa inventada: o F3 está provado por
reprodução no código atual, e é isso que a correção trata.

**Nada disto foi visto em jogo**, como as duas entradas anteriores.

---

Estado ao registrar:

```text
276 testes de unidade + 28 de jogo; build verde
```

---

## 2026-08-12 — Rodou em servidor de verdade, e o F1 estava errado

Primeira execução do mod fora de gametest desde que as três sessões de
trabalho começaram: um servidor dedicado sobre uma cópia do save do
autor, com os chunks da vila forçados para que as colônias ficassem
ACTIVE sem jogador.

```text
Loaded 3 colonies with 43 workers
Colony 0c2771b0 is now ACTIVE
Colony 9a5afa23 is now ACTIVE
```

---

### O que só apareceu ao rodar de verdade

O mod **não subia** num servidor dedicado. O `fabric.mod.json` declarava
três entrypoints `fabric-gametest` apontando para classes do sourceset
de teste, que nunca entraram no jar:

```text
Caused by: java.lang.ClassNotFoundException:
  com.villagecolony.gametest.ColonyDetectionGameTest
```

O cliente singleplayer não carrega esse entrypoint, e por isso o defeito
existia desde que os gametests foram escritos sem que ninguém notasse.
Corrigido no mesmo dia: os entrypoints foram para um `fabric.mod.json`
próprio do sourceset de gametest.

Vale registrar o que isso custou: três sessões de trabalho e vinte e
oito testes de jogo verdes não pegaram um defeito que a primeira
tentativa de rodar o mod pegou em trinta segundos.

---

### O F1 de ontem estava errado

A entrada de 2026-08-11 concluiu que a linha de 5 camas era um segundo
aglomerado de casas — outra vila pequena, contada como a mesma colônia.
A linha nova, que agora diz o centro e a âncora, desmente isso:

```text
saw 28 beds at [1109,64,730] from anchor [1109,64,730]
saw  5 beds at [1109,64,730] from anchor [1120,64,667]
```

**Mesmo centro, âncoras diferentes.** Não são dois aglomerados: é a
mesma vila, vista de dois pontos. O centro é o sino, e as duas leituras
acham o mesmo sino.

A âncora `[1120,64,667]` é o centro da colônia **9a5afa23**, a vizinha.
A sonda dela colhe as camas num raio de 64 do próprio centro, alcança só
a beirada da vila grande — cinco camas —, agrupa, acha o sino da vila
grande como centro, e `findNearest` entrega o candidato para a colônia
`0c2771b0`.

Ou seja: o E2 original estava certo no que suspeitava e eu estava errado
ao contestá-lo. "Uma sonda que vê 5 de 31 está partindo de um ponto que
não alcança a vila" — é exatamente isso, e o ponto é o centro da colônia
vizinha.

O argumento estatístico que me convenceu — "um 5 que se repete cento e
catorze vezes não é visão parcial" — não vale: o ponto de onde essa
leitura parte também é fixo, porque é o centro de uma colônia. Uma visão
parcial de âncora fixa é tão estável quanto a boa.

---

### E isso responde a pergunta que ficou em aberto

Ontem ficou registrado que o log de 2026-08-08 nunca encolheu a colônia
e que, pelo código, isso não deveria ser possível. A resposta é esta:

```text
12×  from anchor [1109,64,730]     a sonda da própria colônia
12×  from anchor [1120,64,667]     a sonda da vizinha
```

Uma colônia tem **um** lugar para guardar a âncora da sonda, e recebe
observações de **duas**. Elas se alternam, e cada uma sobrescreve a
âncora da outra. `from.equals(probeAnchor)` nunca é verdade, e
`confirmedByProbe` nunca dispara.

A colônia não encolhe nunca — não por prudência, mas porque a regra
está permanentemente quebrada quando duas colônias vizinhas têm raios de
sonda que se cruzam.

**Isto é o E2 de verdade, e a correção de ontem não o resolve.** O
`bestPerColony` desduplica candidatos de uma mesma varredura; estes vêm
de varreduras diferentes. A correção de ontem continua certa — o defeito
que ela trata está provado por reprodução —, mas trata outra coisa.

---

### O lenhador não cortou nada em onze ciclos

```text
11×  Colony 0c2771b0 assigned 6 tasks (0 open)
 0×  qualquer linha de corte
```

Seis tarefas atribuídas **a cada ciclo**, o que significa que as seis
saem do ar entre um ciclo e outro e voltam para a fila. O caminho que
faz isso sem dizer nada é o `release` por falta de baú: sem baú,
`LumberjackWork` solta a tarefa em silêncio, ela volta a AVAILABLE, e o
ciclo seguinte a entrega de novo.

Não dá para afirmar que é isso a partir deste log, e é essa a questão:
**não dá para afirmar nada**. Ao tirar o trabalho do lenhador do ciclo
de 600 ticks, a instrumentação foi junto — a linha "heading to the tree"
saiu e nada ocupou o lugar dela. O lenhador ficou mudo.

O que se sabe: seis lenhadores com tarefa, onze ciclos, nenhuma árvore.

---

### O que este teste não cobriu

Roda sem jogador, então nada do que é visto pelo cliente foi verificado:
o nome sobre a cabeça, a rachadura no bloco e o braço balançando — as
três coisas que a Regra 2 acrescentou para ser visível — continuam sem
prova. Isso precisa de alguém no teclado.

---

## 2026-08-12 — A marca do baú funciona; dois defeitos abertos

Verificado pelo autor dentro do jogo: o baú de `1105,64,681` tem **um**
quadro com o machado do lenhador, e o de `1127,65,665` tem o do
construtor. Bate com o log, que nomeou os dois.

Ou seja: a marca aparece, é única, e diz a profissão certa.

---

### Alarme falso que precisa ficar registrado

Antes de perguntar ao autor, eu li `Marked 2 chests` repetido a cada
ciclo e concluí que havia vinte quadros empilhados e risco para o save.
**Estava errado.** Havia um. A conclusão saiu de um número que se repete,
sem nenhuma observação do mundo — o mesmo erro do F1 do E2, cometido de
novo três dias depois.

A regra que isso cobra: contagem que se repete não prova acúmulo. Só
olhar prova.

---

### D1 — `markOne` diz que mudou quando não mudou

```text
[06:32:40] Marked 2 chests in colony 9a5afa23
[06:33:10] Marked 2 chests in colony 9a5afa23
   ... a cada 30 segundos, com um quadro só no mundo
```

O que se sabe, e o que isso exclui:

```text
quadro nascendo no lugar errado   excluído — seria reencontrado ou
                                  duplicaria, e nenhum dos dois acontece

quadro duplicado                  excluído — o autor viu um só

quadro encontrado e item          é o que sobra: o teste de isOf falha,
recolocado a cada ciclo           e o ícone é reposto sem necessidade
```

Não é destrutivo e não gasta entidade. É uma linha de log que mente e
uma escrita por ciclo que não precisava existir.

O gametest não pega porque afirma o item logo depois de marcar, sem
recarregar o chunk entre uma coisa e outra. A suspeita a investigar
primeiro é o `setHeldItemStack(badge, false)` de `place` contra o
`setHeldItemStack(badge)` da atualização, e o que sobrevive a um
save/load de chunk.

---

### D2 — a regra da vaga não vale entre vilas vizinhas

```text
0c2771b0 — MANUFACTURER 3f052d86 claimed the chest at 1069,65,727
0c2771b0 — FARMER       c7528432 claimed the chest at 1113,67,744
0c2771b0 — FARMER       b97c95f5 claimed the chest at 1120,70,727
0c2771b0 — MANUFACTURER fb3640ae claimed the chest at 1118,70,727
```

Dois fazendeiros e dois fabricantes na mesma linha de colônia, e nenhuma
linha `dismissed` na sessão.

A explicação provável, não verificada: `0c2771b0` e `9a5afa23` estão a
cerca de 65 blocos, e a caixa de `VillagerScanner` tem raio 64 em torno
de cada centro — elas se sobrepõem. `enforceVacancies` conta por colônia
**do trabalhador**; a linha nova reporta a colônia que **varreu**. Se for
isso, a linha está atribuindo dono errado e a regra tem um furo onde duas
vilas se encostam.

Ambos foram achados pela linha que nomeia quem pegou qual baú, no mesmo
dia em que ela foi escrita.

---

## 2026-08-12 (mais tarde) — D1 e D2 fechados, e as duas causas anotadas estavam erradas

Os dois foram investigados a partir do log de `06:47–06:51` da instalação
de teste. Nenhuma das duas hipóteses registradas de manhã sobreviveu.

---

### D1 — a busca aceitava quadro perto, e perto não é o mesmo que ser

A causa não é o `setHeldItemStack(badge, false)` nem o save/load de
chunk. A colônia `c18264c9` marcou **uma vez** e nunca mais — o caminho
que converge funciona, e nenhum recarregamento é necessário para
reproduzir o defeito. Ele é específico da `9a5afa23`.

```text
1069,65,727  MANUFACTURER
1105,64,681  LUMBERJACK     verificado pelo autor, machado
1118,70,727  MANUFACTURER   ┐ dois blocos
1120,70,727  FARMER         ┘
1127,65,665  BUILDER        verificado pelo autor, tijolo
```

`existingMarkerAt` procurava em `new Box(chest).expand(1.0)` e só
perguntava se o quadro era do mod. As caixas dos baús de `1118` e `1120`
se cruzam em `x ∈ [1119, 1120]`, e o quadro pendurado no vão entre eles
cai dentro das duas: o fabricante punha a mesa, o fazendeiro punha a
enxada trinta segundos depois, para sempre. Daí o **2** constante — nunca
3, nunca 5 — com um quadro só no mundo.

E não era só log mentiroso: o ícone alternava a cada ciclo, e um dos dois
baús estava sempre dizendo o dono errado.

A correção pergunta em que bloco o quadro está pregado.
`getAttachedBlockPos` devolve o bloco de ar que o quadro ocupa, não a
parede; a parede é o bloco seguinte na direção contrária à que ele olha,
que é como o próprio Vanilla a encontra em `canStayAttached`. `unmark`
herda a correção, e tinha o mesmo furo: podia arrancar o quadro do baú
vizinho.

Os cinco gametests usavam **um** baú, e por isso nenhum pegava isso. O
sexto põe dois a dois blocos, com só o vão livre. Ele foi rodado contra o
código sem a correção e falhou com "os dois baús ficaram trocando o mesmo
quadro" — é o defeito, e não uma variante dele.

Uma coisa que a investigação previu errado: eu esperava um quadro só, com
o segundo baú ficando sem marca. São dois. O mesmo bloco de ar comporta
um quadro em cada parede, e cada baú fica com o seu. O teste é que estava
errado, não o código.

---

### D2 — a regra da vaga não tem furo; a linha é que atribuía dono errado

O registro de manhã dizia que a linha reporta a colônia que varreu **e
que por isso a regra tem um furo**. A primeira metade está certa; a
conclusão não segue dela.

`enforceVacancies` itera `workers.ofColony(colonyId)` — a colônia gravada
no trabalhador —, e `dismissExtraWorkers` roda para cada colônia, todo
ciclo. Não há por onde escapar um segundo lenhador da mesma colônia.

A prova é uma ausência: **nenhuma linha `dismissed` na sessão inteira**, e
nenhuma `Assigned ... professions` — as profissões vieram do save. Dois
`MANUFACTURER` na mesma colônia do registro teriam gerado uma aposentadoria
no primeiro ciclo.

A geometria fecha o caso:

```text
9a5afa23   centro ≈ 1116,64,669
0c2771b0   centro   1109,64,730
                    └── 61 blocos, raio de varredura 64 cada
```

Os três baús anunciados sob `9a5afa23` em `z=727` estão a **3** blocos do
centro da `0c2771b0` e a **58** do da `9a5afa23`. E o `colonyId` do
trabalhador persiste no save, enquanto `workers.register` só grava colônia
em quem ainda não existe — por isso `3f052d86` e `fb3640ae` aparecem sob
`0c2771b0` numa sessão e sob `9a5afa23` na outra. O dono não mudou; o
varredor mudou.

`announce` passou a tirar a colônia do trabalhador.

**O que sobra, e não se conserta aqui:** a regra vale por colônia do
registro, não por vila física. Com dois centros a 61 blocos e raio 64, um
aldeão que mora numa vila pode estar registrado na outra, e duas vilas
encostadas podem ter dois lenhadores no mesmo lugar, cada um legítimo pela
sua colônia. Isso é o E2 — duas colônias que deveriam ser uma — e mexer em
`enforceVacancies` só o esconderia.

---

### O que continua aberto

- **E2** — `from.equals(probeAnchor)` nunca é verdade quando os raios de
  sonda se cruzam, e a colônia nunca encolhe. Intocado.
- **O lenhador mudo** — seis tarefas por ciclo, nenhuma árvore, sem
  instrumentação para dizer por quê. Intocado.
- **O lado do cliente** — nome, rachadura e braço continuam sem prova.
- **Novo, não investigado:** `Marked ... in colony 9a5afa23` aparece duas
  vezes no mesmo segundo em vários ciclos. A colônia está sendo
  processada duas vezes por ciclo. Pode ter relação com o E2.

---

### Verificado em jogo — sessão de 07:48

Antes disso houve duas tentativas que não valeram: a das 07:11 rodou o jar
de 06:25, ainda sem a correção, e a das 07:18 durou 40 segundos sem uma
única linha de colônia. O que o autor viu nas duas — foice no `1118`,
nada no `1120` — era o estado congelado que o defeito deixou: um quadro
só, pregado no baú do fabricante, com o ícone do fazendeiro, que foi o
último a escrever nele.

Na sessão das 07:48, com o jar correto e tempo para uns oito ciclos:

```text
D1   Marked aparece 2× na sessão inteira, ambas em 07:49:30, e para.
     Os ciclos seguem rodando até 07:53:41 (camas, lenhadores).
     Antes: de 30 em 30 segundos, para sempre.

D2   0c2771b0 — MANUFACTURER 3f052d86   (era 9a5afa23)
     Com a linha dizendo a verdade, as duas colônias aparecem com
     um de cada profissão. O furo nunca existiu.
```

E o autor confirmou dentro do jogo: os dois baús, cada um com a sua placa.

**Novo, do mesmo log:** os lenhadores estão cortando — `a60c4f43` com 22
toras, `e8f56d2b` com 7. Não é mérito desta correção, e não se sabe o que
mudou. A pendência do lenhador mudo precisa ser reexaminada contra este
log, e não contra o de ontem.

---

## 2026-08-12 (E2) — a sonda da vizinha apagava a leitura da própria

O log de 07:48 é a reprodução completa, com os números da vila do autor.
Três observações chegam à colônia `0c2771b0` por ciclo:

```text
07:52:11   saw 25 beds  from anchor none        keeping 36   jogador
07:52:11   saw 35 beds  from anchor 1109,730    keeping 36   sonda dela
07:52:11   saw 10 beds  from anchor 1116,669    keeping 36   sonda da vizinha
07:52:41   saw 35 beds  from anchor 1109,730    keeping 36
07:53:11   saw 35 beds  from anchor 1109,730    keeping 36
07:53:41   saw 35 beds  from anchor 1109,730    keeping 36
```

A sonda própria leu 35 quatro vezes seguidas, do mesmo ponto, contra 36
registradas — exatamente o que o §15 exige para encolher. A colônia
continuou em 36.

`Colony.observe` guardava uma âncora só e gravava qualquer uma. A leitura
de `1116,669` — centro da `9a5afa23`, a 61 blocos — apagava a de
`1109,730` entre um ciclo e o seguinte, e `from.equals(probeAnchor)`
nunca era verdade. `detectFromColonyCenters` sonda cada centro ativo, e o
candidato vai para a colônia mais próxima, que pode não ser a que sondou:
a sonda de uma vira observação da outra.

**A correção:** só a sonda ancorada no centro **desta** colônia confirma
e escreve na memória. Âncora alheia vale o mesmo que âncora nenhuma.
Custa um ciclo de memória quando o centro se move, e isso é correto —
leituras de pontos diferentes não são comparáveis.

O teste novo intercala a sonda da vizinha entre duas leituras da própria,
que é o que o log mostra e o que nenhum teste de sonda fazia. Rodado
contra o código sem a correção, falha.

---

### Decisão do autor: quando duas vilas viram uma

`0c2771b0` e `9a5afa23` estão a 61 blocos, e `DUPLICATE_DISTANCE` é 64 —
hoje `adopt` não as criaria separadas. **São vilas separadas assim
mesmo**, e é assim que devem continuar: distância não é o critério.

Duas vilas viram uma quando **um bloco de uma encostar no bloco da
outra** — quando a construção as junta, e não quando um número diz que
estão perto. A fusão **não reduz trabalhadores**: a vila resultante fica
com os de ambas.

A direção da construção pode acompanhar isso: o construtor tende para a
cidade mais próxima, e o encontro das duas frentes é o que dispara a
fusão.

Nada disso está implementado. É a decisão registrada para quando a
construção existir, e é ela que fecha o que sobrou do D2 — a vaga por
profissão vale por colônia do registro, e duas vilas encostadas só param
de disputar trabalhador quando forem uma.

---

### Fechado de quebra: a colônia processada duas vezes por ciclo

Não é defeito. `registerVillagers` roda uma vez por candidato adotado, e
com os raios se cruzando a mesma colônia recebe dois ou três candidatos
por ciclo — daí a linha `Marked` repetida no mesmo segundo. O custo é uma
consulta de entidades por adoção, o que merece um olhar quando a fusão
existir, mas não é acúmulo nem erro.

---

## 2026-08-12 (noite) — a copa separa árvore de casa, e a busca parou de morrer no mesmo lugar

Duas mudanças estavam escritas na árvore de trabalho e não tinham teste,
entrada aqui nem commit. Esta sessão fechou as duas, e a segunda cobrou
uma terceira.

---

### Regra nova — sem copa viva não é árvore

Casa de planície é feita de tronco de carvalho, e o carvalho da casa é o
mesmo da floresta: mesmo bloco, mesmo drop, mesma espécie na tabela. Até
aqui a única coisa que separava uma da outra era o teto de 24 troncos, e
ele não separa nada — uma cabana de dez troncos passa por baixo dele sem
esbarrar.

A diferença que o mundo registra é a copa. Folha de árvore crescida vem
com `persistent = false` e apodrece se o tronco cair; folha pendurada à
mão vem com `true` e nunca apodrece. É a única marca do Vanilla que
responde à pergunta, e o mod não tem nenhuma melhor.

O que passou a valer:

```text
grupo de tronco sem folha natural ligada    não é árvore, não se toca

folha pendurada à mão                       não é copa, e não vira copa
                                            de um pilar por engano

a copa é procurada antes do teto de 24      era o furo da primeira
                                            versão: a construção grande
                                            escapava sem nunca ser
                                            olhada
```

O terceiro ponto merece o destaque. Na versão que chegou aqui, a copa só
era procurada quando o tronco cabia no teto — e uma parede de vinte e
cinco troncos, que é justamente a construção que mais dói perder, nunca
chegava a ser examinada. O teste `lumber_big_wall` guarda esse lado.

---

### Consequência que a regra cobrou: a limpeza da coluna

`clearAbove` abre a coluna acima da muda e para no primeiro bloco que
não seja folha. Com a regra nova, "folha" deixou de ser suficiente:
folha pendurada à mão é construção como um telhado de tábua, e agora
encerra a limpeza do mesmo jeito.

Continua valendo folha de **qualquer** espécie — a copa que cobre esta
base pode ser da árvore vizinha —, desde que tenha nascido ali.

---

### A busca deixou de recomeçar do centro

`TreeScanner` olha no máximo 1024 colunas por busca, e 1024 colunas
acabam no anel 16. Como toda busca recomeçava do zero, o raio de 64 que
ela recebe era decorativo: uma colônia cuja floresta comece depois do
décimo sexto bloco morre no mesmo lugar, ciclo após ciclo, para sempre.

Agora cada centro guarda em que anel parou, em memória:

```text
teto estourado    guarda o anel em que estava, e recomeça nele —
                  ele ficou pela metade

achou             zera o cursor: a árvore sai do mundo, e a próxima
                  procura recomeça de perto

deu a volta       zera o cursor: a floresta cresce, e a muda
                  replantada perto volta a ser árvore
```

O custo por ciclo não muda — são as mesmas 1024 colunas —, e o alcance
cresce com o tempo. Perder o cursor ao reiniciar custa alguns ciclos de
busca perto do centro, e nada mais.

---

### O que os testes desta classe estavam provando errado

A `plantTree` dos testes do lenhador plantava quatro troncos e nenhuma
folha. Com a regra nova, essa árvore deixou de ser árvore — e oito
testes passariam **por não tocar em nada**, que é o oposto do que eles
afirmam medir.

Foi um caso raro em que a regra nova expôs testes fracos que já eram
fracos antes: `leavesOfAnotherSpeciesStay` e `fellingIgnoresOtherWoods`
verificam que um bloco alheio fica de pé, e um teste que nunca derruba
nada satisfaz isso sem esforço.

A `plantTree` passou a plantar tronco e copa, com a folha ao lado do
tronco de cima — e não acima dele, que é onde três testes medem a
coluna da muda.

---

### Verificado

```text
284 testes unitários            verdes
 40 testes de jogo              verdes

os 5 testes novos rodados       falham os 5, e só eles
contra o código sem a
correção
```

Os cinco novos são: tronco sem copa, folha pendurada não é copa, parede
de vinte e cinco troncos, limpeza que para na folha do jogador, e a
busca que avança e depois volta ao centro.

O teste da busca não afirma o cursor: afirma a consequência dele. Uma
árvore plantada perto **depois** da primeira busca não é vista na
segunda — sinal de que a varredura está longe, continuando — e volta a
ser vista quando a volta se completa. Sem a correção, a segunda busca a
encontra na hora, e o teste falha ali.

---

### Não verificado

**Nada disto foi visto em jogo.** É a dívida de sempre desta camada, e
o §11 diz o que ela costuma esconder: os defeitos sérios deste projeto
apareceram todos na fronteira com o Minecraft, não no teste.

O que uma sessão do autor precisa mostrar:

```text
a casa de tronco da vila continua de pé depois de vários ciclos
com lenhador trabalhando ao lado dela

a colônia cuja floresta começa longe passa a ter linha de corte,
em vez de "looking for a tree" para sempre

a muda continua nascendo, e a coluna acima dela continua sendo
aberta quando é folha de verdade que está no caminho
```

---

### O que continua aberto

- **O lenhador mudo** — o cursor da busca é uma causa provável para a
  colônia que nunca achava árvore, e não é prova. O log de 23:03 desta
  mesma noite mostra os dois lenhadores com 8 toras cada, o que já
  contraria a leitura mais pessimista da pendência. Ela precisa ser
  reexaminada contra um log novo.
- **O lado do cliente** — nome, rachadura e braço continuam sem prova.
- **E5** — continua como estava: só carvalho foi derrubado em jogo. O
  teste exercita carvalho e bétula; as outras seis espécies não têm caso
  próprio, e a regra da copa acabou de acrescentar mais uma coisa que
  varia entre elas.

---

## 2026-08-12 (noite, mais tarde) — o item A fechado, e um travamento de servidor que ele encontrou

O item A do §8 pedia três casos que o gametest não cobria: morte e
zumbificação do trabalhador, encolhimento da colônia, e o ciclo gerando
tarefa a partir de déficit. Os três entraram. O quarto teste desta
sessão não estava na lista — nasceu de um defeito que o próprio trabalho
de teste desenterrou.

---

### Os três casos do item A

```text
worker_death         o aldeão morre: vaga, baú e tarefa voltam
worker_conversion    o aldeão é zumbificado: o mesmo, por outro evento

colony_shrink        a vila perde casas e a colônia encolhe — mas só
                     quando a sonda dela repete a leitura menor

cycle_deficit        baú sem espaço não gera pedido; baú vazio gera
```

A morte e a zumbificação são eventos diferentes do Fabric, e é por isso
que são dois testes: `AFTER_DEATH` não dispara quando o aldeão é mordido
por um zumbi, que é o caso mais comum de perder um trabalhador em jogo.

O teste do encolhimento exige as duas metades: **não** encolher na
primeira leitura menor, e encolher na segunda. Com a regra apagada, só a
segunda metade falha — a primeira passaria de graça.

O teste do ciclo entope o baú com **terra**, e não com madeira. É o que
o torna capaz de falhar: com o baú cheio de madeira, a meta constante
antiga também daria déficit zero, e o teste passaria sem provar nada.
Com terra, a colônia tem zero madeira e nenhum espaço — pela regra velha
faltariam 64, pela Regra 1 não falta nada.

---

### O defeito que o teste desenterrou

Rodando a bateria contra o código com a Regra 1 desligada — o passo de
"provar que o teste falha" —, o servidor não falhou um teste: **caiu**.

```text
java.lang.IllegalStateException: Cannot complete a task that is RESERVED
  LumberjackWork.finishTask
  LumberjackWork.startNextTree
  ...
  VillageDetectionHandler.onServerTick
```

`Task.complete` exige EXECUTING, e o lenhador só marca a tarefa como
iniciada depois de escolher uma árvore. Quando a primeira árvore que ele
olha não cabe no baú, `finishTask` encerra a tarefa **antes** disso — e
a tarefa ainda está RESERVED.

Não é artefato de teste. É o fim normal da Regra 1 visto de perto:

```text
o baú termina quase cheio

o ciclo seguinte abre um pedido do tamanho do espaço que sobrou

o lenhador reserva, olha a primeira árvore, e ela não cabe

→ exceção dentro do tick do servidor
```

Nunca aconteceu em jogo porque o baú do autor tinha espaço de sobra
todas as vezes. Bastava um baú com espaço para dois troncos e uma árvore
de quatro.

A correção é a mesma transição que `startNextTree` já fazia: iniciar a
tarefa antes de encerrá-la. O teste `cycle_tree_too_big` monta
exatamente esse mundo, e rodado contra o código sem a correção derruba o
servidor com a mesma exceção.

---

### O que o teste do encolhimento cobrou

A sonda só roda para colônia ACTIVE, e ACTIVE quer dizer chunk ticando.
Em jogo quem mantém o chunk assim é o jogador por perto; no gametest não
há jogador, e o centro da colônia cai onde o aglomerado o puser —
inclusive fora da estrutura do teste, porque as camas das estruturas
vizinhas entram na conta.

O teste força o chunk do centro, e o pedido de carga só vale no tick
seguinte. Por isso ele atravessa ticks em vez de rodar os ciclos em
sequência, e por isso a mensagem de falha diz se a colônia estava ativa:
sem isso, uma sonda que nunca rodou se parece com uma regra que não
funciona.

Vale registrar como diagnóstico geral: no gametest, colônia longe da
estrutura é colônia dormente, e colônia dormente não pensa.

---

### Verificado

```text
284 testes unitários      verdes
 45 testes de jogo        verdes  (eram 40 no começo da noite)

negativo, por regra desligada:

  Regra 1 (meta pelo espaço)     cycle_deficit falha
  confirmação da sonda           colony_shrink falha
  handler de morte/conversão     worker_death e worker_conversion falham
  transição RESERVED→EXECUTING   cycle_tree_too_big derruba o servidor
```

---

### O que continua fora do alcance do gametest

```text
persistência entre sessões   exige fechar e reabrir o mundo, e a
                             bateria roda um servidor só (V3 do §7)

o lado do cliente            nome, rachadura e braço precisam de
                             alguém no teclado
```

---

## 2026-08-12 (madrugada) — o documento consolidado, e o log virou arquivo

Item B do §8. O `Project-State.md` tinha 5889 linhas, e 4700 delas eram
este arquivo. A regra do §16 — "quem abre este projeto hoje sabe onde
estamos e qual é o próximo passo?" — deixava de ser respondida pelo
tamanho: as respostas estavam lá, nas seções 1 a 14, soterradas por
trinta e tantas entradas de log.

---

### O que se fez

```text
o log saiu inteiro para docs/technical/Development-Log.md

o §15 do Project-State virou o ponteiro para cá — as referências a
"a entrada de §15 de 2026-08-07" espalhadas pelo código continuam
valendo

o §16 duplicado acabou: um era "Definition of Project Progress",
o outro eram as duas regras do autor. As regras viraram §18

§3, §5, §6, §7 e §8 foram reescritos contra o estado real
§9, §10, §11, §13, §14 e §17 foram atualizados
o README §14 também estava em "106 testes, fase 4 de 9"
```

Três entradas de log estavam **dentro** do §16 e do §17, sem que ninguém
tivesse notado: o documento vinha crescendo por acréscimo no fim, e o
fim nem sempre era o §15.

---

### O que estava desatualizado, e o que isso custava

```text
§3   "Fases 1 a 3 completas, Fases 4 e 5 escritas e não verificadas"
     — oito fases estavam completas e verificadas em jogo

§5   "Coletar recursos — não verificado em jogo"
     — verificado em 2026-08-08, com a primeira derrubada

§6   "Fase 9 — TASK-024 em diante", numeração errada; a lista de
     classes não tinha coordination, brain, mixin nem work

§8   a fila de prioridades era a foto de 2026-08-08, quatro dias
     e dois defeitos fechados atrás

§13  listava Simulation-Loop.md como "ainda não implementado";
     ColonyCycle existe desde 2026-08-08
```

O custo disso não é cosmético: o §14 manda começar pelo §8 ao retomar, e
o §8 apontava para um bloqueio que já tinha caído.

---

### O que se decidiu ao consolidar

```text
numeração preservada       o código cita §15, §17, §11, §8. Renumerar
                           essas seções invalidaria dezenas de
                           comentários em silêncio — o tipo de coisa
                           que este projeto trata como defeito

E1, E2, D1 e D2 encurtados o texto integral está aqui, por data. No
                           §17 ficou o resumo e o ponteiro

TASK-026 marcada cancelada  MVP-Tasks.md ainda a lista, e a madeira
                            vai direto para o baú desde 2026-08-08
```

Uma coisa que não se fez: reescrever entrada antiga. As de antes de hoje
citam "§16" ao falar das duas regras, que hoje é §18. O apontamento está
no cabeçalho deste arquivo, e o texto delas fica como estava.

---

### Estado ao registrar

```text
Project-State.md      1428 linhas, seções 1 a 18
Development-Log.md    4500 linhas, 56 entradas por data

284 testes unitários e 45 de jogo, verdes
```

---

## 2026-08-12 (madrugada) — o texto original do E1 e do E2

Os dois foram encurtados no §17 durante a consolidação: lá ficou o
resumo e o ponteiro, e o relato de cada investigação já está nas
entradas de 2026-08-11 e 2026-08-12 deste arquivo.

O que segue é o texto como estava registrado quando os defeitos ainda
eram defeitos abertos — inclusive as suspeitas que se revelaram erradas,
que são a parte que ensina. Nada aqui foi reescrito.

---

## E1 — A fila de tarefas não esvazia — **corrigido em 2026-08-11**

Fechado pelas duas metades previstas: a Regra 1 tirou a meta constante,
e `purgeClosed` — que existia desde a Fase 7 sem quem a chamasse —
passou a ser chamado ao fim de cada ciclo. O registro de tarefas deixou
de crescer para sempre.

O texto original fica abaixo.

---


```text
[05:19:31] Colony 0c2771b0 assigned 1 tasks (0 open)
[05:20:01] Colony 0c2771b0 assigned 1 tasks (0 open)
[05:20:31] Colony 0c2771b0 assigned 1 tasks (0 open)
   ... a cada 30 segundos, indefinidamente
```

Uma tarefa nova por ciclo, por colônia. Em sete minutos de sessão foram
catorze, e nada as remove.

Causa conhecida: a colônia compara estoque com uma meta constante e
gera tarefa enquanto faltar. Como o lenhador entrega devagar — uma
árvore por ciclo, no melhor caso — a meta demora, e a fila cresce mais
rápido do que esvazia.

Metade já foi corrigida em 2026-08-08: com `ResourceGroup.WOOD`, bétula
e abeto passaram a contar para a mesma meta, então a colônia se satisfaz
com o que já tem. A outra metade é a Regra 1 do §16.

Não é só cosmético: tarefa é objeto em memória, e nada as expira.

---

## E2 — A colônia 0c2771b0 nunca prova a visão completa — **investigado e corrigido em 2026-08-11**

A suspeita registrada abaixo estava errada, e o defeito real era outro.
Ver a entrada de §15 de 2026-08-11. Em resumo: as 31 camas existem; a
linha de 5 camas era um segundo aglomerado, não uma sonda com defeito; e
o defeito de verdade era dois candidatos da mesma varredura se
confirmando dentro do mesmo tick.

O texto original fica abaixo.

---


```text
Colony 0c2771b0 saw 28 beds, keeping 31 — view not provably complete
Colony 0c2771b0 saw 5 beds, keeping 31 — view not provably complete
```

Três linhas dessas por ciclo, sessão após sessão. A colônia registrou 31
camas uma vez e nunca mais viu as 31 ao mesmo tempo, então nunca encolhe
— o que é a regra funcionando, e não um defeito por si.

O que é suspeito é a linha com 5 camas: uma sonda que vê 5 de 31 está
partindo de um ponto que não alcança a vila, ou está rodando com metade
dos chunks fora. Isso não foi investigado.

Se a vila tiver de fato encolhido, a colônia está com 31 camas que não
existem — e a contagem de vagas de profissão sai errada por cima.

---

---

## 2026-08-13 — três regras do autor, e a quinta decidida por delegação

O autor trouxe duas regras novas e mandou resolver o P3. As duas
primeiras viraram código hoje; a terceira é decisão registrada, sem
código, porque o código dela não existe ainda.

---

### Regra 3 — nunca destruir bloco da vila original nem do jogador

O enunciado é curto e a implementação é o oposto disso, porque o
Minecraft não guarda quem pôs cada bloco. O que se conseguiu foi:

```text
vila original      o jogo guarda as peças de cada estrutura gerada.
                   A pergunta é por peça, não pela caixa da vila
                   inteira — a caixa cobre o campo aberto entre as
                   casas, e proibir o campo aberto proibiria a
                   colônia de trabalhar dentro da própria vila

jogador            só a folha responde: colocada à mão vem
                   persistent, nascida de árvore não. Para todo o
                   resto, o mundo não diz
```

Daí a forma da correção: `BlockProtection` é a porta, e a proteção real
continua sendo a inversa — o trabalhador só quebra o que prova ser
floresta. É a regra da copa, escrita ontem, que faz esse trabalho.

**A árvore é a exceção, e é a única.** Vila de planície nasce cercada de
carvalho, e boa parte dele cai dentro dos limites que o jogo registra
para a vila; sem a exceção não haveria colheita.

A consequência foi levantada como pergunta na entrega e **o autor
confirmou no mesmo dia**: árvore gerada junto com a vila é derrubável
como qualquer outra. A exceção é da árvore, não do lugar — estar dentro
da vila não muda o que um bloco é. Fica registrado como decisão dele, e
não como leitura minha do pedido.

**O que muda de comportamento hoje: quase nada, e vale dizer.** A única
coisa que o mod quebra é árvore, e árvore é a exceção. O único caminho
que passa pela porta é a limpeza da coluna da muda, que toca folha de
outra árvore — e essa passou a respeitar a peça de vila. A porta existe
para as Fases 9 e 10, quando fabricar e construir forem tocar no mundo.

A metade "vila original" não é testável por máquina: estrutura nasce da
geração de terreno, e o mundo do gametest não tem vila. O teste cobre o
caminho sem estrutura nenhuma — o que roda a cada colheita — e a outra
metade virou item de verificação em jogo no §8.

---

### Regra 4 — dois trabalhadores de cada profissão

Era um desde ontem, e antes disso ilimitado. O teto virou dois, e a
mudança maior não foi o número:

```text
vacancy()   passou a devolver a profissão mais escassa que ainda tem
            vaga, e não a primeira da lista

            com teto de dois, ir por ordem daria dois lenhadores antes
            do primeiro fabricante — uma vila com dois lenhadores e
            nenhum construtor é pior do que uma com um de cada

enforceVacancies()   passou a guardar um conjunto por profissão em vez
                     de um trabalhador, mantendo a preferência por quem
                     tem baú
```

Os testes que afirmavam "um de cada" viraram "dois de cada", e um teste
novo guarda a ordem: o quinto aldeão dobra o lenhador, e não o
construtor.

---

### Regra 5 — quanto fabricar, decidida por delegação

O autor disse "resolve o P3". Fica registrado que a decisão é minha e
não dele, porque é a que ele mais vai querer revisar.

A tentação era responder como a Regra 1 respondeu para a colheita — o
espaço dos baús. Ela se destrói sozinha: um tronco vira quatro tábuas,
então fabricar aumenta o volume guardado. "Fabricar até encher"
transformaria toda a madeira da colônia em tábua e pararia a coleta
junto, porque é o baú cheio que faz o lenhador parar.

```text
a meta de tábua é o que a obra pede

enquanto não houver obra, o fabricante enche metade do espaço de
armazenamento da colônia com tábua, e para
```

Metade e metade mantém as duas coisas vivas, e a metade é medida no
mundo — a capacidade dos baús que a colônia tem —, não é quantidade
inventada. Quando a Fase 10 trouxer a obra, a demanda dela substitui o
teto.

**Nada disso virou código, de propósito.** Abrir tarefa de fabricação
antes de existir quem a execute deixaria a tarefa reservada para sempre,
que é exatamente o defeito que o §11 já registra.

---

### Verificado

```text
285 testes unitários     verdes  (era 284)
 47 testes de jogo       verdes  (eram 45)
```

Nenhuma das duas regras novas foi vista em jogo.

---

## 2026-08-13 (madrugada) — a sessão de jogo, e a vaga que ia para a cama errada

Primeira sessão com o jar da noite: 00:31 a 00:36, dez ciclos, três
colônias, oitenta trabalhadores registrados.

---

### O que a sessão não pôde provar

```text
lumberjacks: 15be5ffc looking for a tree, off hours (0 logs so far)
```

**`off hours` em todas as linhas.** A sessão caiu fora do horário de
trabalho da agenda Vanilla, e nenhum lenhador podia trabalhar. Nada
sobre a regra da copa, o cursor da busca ou a Regra 3 apareceu, e não
por defeito.

Fica a nota de método: teste de lenhador exige horário de trabalho. É
`/time set day` e ficar perto da vila, e vale mais que qualquer linha de
log nova.

---

### O que ela provou

```text
Assigned 4 professions in colony c18264c9
Assigned 1 professions in colony 9a5afa23
lumberjacks: 2898aeb3 ... ; a60c4f43 ...
```

A Regra 4 funcionou em jogo: três colônias, dois lenhadores em cada,
nenhuma linha `dismissed` — o teto subiu e ninguém sobrou. As vagas
novas puxaram baús novos junto, e a correção do E2 continua de pé: a
sonda da vizinha aparece nomeada e não apaga mais a leitura da própria.

Nenhuma exceção do mod na sessão inteira.

---

### O defeito que ela mostrou

```text
Worker 15be5ffc has no chest — wood task returned to the queue
Worker cc8800ac has no chest — wood task returned to the queue
   ... a cada 30 segundos, com baú livre na vila
```

Dobrar as vagas dobrou a demanda por baú, e apareceu o que uma vaga só
escondia: **`assignMissing` dava a função sem olhar se aquele aldeão
conseguiria um baú.** `enforceVacancies` já tinha essa preferência desde
2026-08-12; a atribuição inicial, não. A vaga ia para a cama errada, e o
trabalhador passava a sessão pegando a tarefa e devolvendo à fila.

Do lado de fora isso se parece com trabalho acontecendo — é a mesma
armadilha do lenhador mudo.

---

### A correção

```text
ChestScanner.hasFreeChest     a mesma pergunta de scan, sem reivindicar

VillagerScanner               monta o conjunto dos que conseguiriam
                              baú, e só quando há vaga aberta

ProfessionAssigner            duas passadas: primeiro quem consegue,
                              depois os demais
```

É preferência, não exigência: esgotados os candidatos com baú possível,
a vaga vai para quem sobrar. Vaga vazia não é melhor que um trabalhador
sem baú — o jogador pode construir o baú depois, e aí ele o reivindica
no ciclo seguinte.

O custo é uma varredura de baús por candidato, e por isso a pergunta só
é feita quando existe vaga aberta. Depois dos primeiros ciclos não
existe, e o custo é zero.

Dois candidatos podem enxergar o mesmo baú livre e só um ficar com ele.
Isso continua possível, e é aceitável: a preferência não promete baú,
promete não escolher quem certamente não terá nenhum.

---

### Verificado

```text
288 testes unitários     verdes  (eram 285)
 47 testes de jogo       verdes

negativo: com a preferência desligada, o teste novo falha
```

Uma correção do relatório da sessão: eu ia registrar as árvores achadas
a 76 e 63 blocos como sinal do cursor da busca funcionando. **Não são.**
A busca mede do centro da colônia, não do aldeão, e as três árvores
estão nos anéis 15, 14 e 4 — todas dentro do alcance que o código antigo
já tinha. O cursor continua sem evidência em jogo.

---

## 2026-08-13 (madrugada, 00:47) — a primeira derrubada com a regra da copa, e a hora certa do relógio

Sessão de três minutos, e a mais produtiva do projeto até agora.

```text
[00:49:25] Worker e8f56d2b finished the tree at 1120, 64, 669
           — 5 logs and 56 leaves, 5 logs this task
```

**Cinco troncos e cinquenta e seis folhas.** É a primeira colheita com a
regra da copa em jogo, e ela mostra o lado que interessava: árvore de
verdade continua caindo inteira, com a copa junto. A regra não quebrou a
colheita.

O que ela ainda não mostra é o outro lado — a casa de tronco da vila
continuar de pé. Ausência de estrago não aparece no log; isso é o autor
olhando.

---

### A correção das 00:45 funcionou em jogo, quinze minutos depois

```text
has no chest — wood task returned to the queue     0 ocorrências
```

Na sessão anterior era a cada trinta segundos, com dois lenhadores. Nesta
não aparece nenhuma vez, e as linhas de reivindicação mostram por quê:
`LUMBERJACK a60c4f43 claimed the chest at 1090,64,738`, e mais quatro.

É o ciclo mais curto que este projeto já teve entre um defeito observado
em jogo e a correção confirmada em jogo.

---

### A hora certa do relógio, que custou duas sessões

```text
[00:49:03]  lumberjacks: ... looking for a tree, work time
[00:49:18]  [Majest: Tempo definido para 1000 tiques]
[00:49:33]  lumberjacks: ... walking — ..., off hours
```

O autor pôs `/time set day` para ajudar, e **encerrou a janela de
trabalho**. A agenda Vanilla põe WORK no tique 2000 e o tira em 9000;
`day` é 1000, que é antes. A instrução que eu tinha dado estava errada.

O que serve é `/time set noon` (6000) ou `/time set 2000`. Está
registrado no §11 como armadilha de método, junto com a do jar trocado
com o jogo aberto.

---

### O que o log diz de resto

```text
Loaded 3 colonies with 80 workers

dois lenhadores por colônia, os dois com baú e com árvore

e8f56d2b seguiu para a segunda árvore com a mesma tarefa — "5 logs
so far" — que é a Regra 1 trabalhando: ele colhe até o baú encher

a sonda da vizinha continua aparecendo nomeada e sem apagar nada
```

Nenhuma exceção do mod.

**Continua sem prova:** o cursor da busca — as árvores desta sessão estão
a 4, 14 e 15 anéis do centro, todas ao alcance do código antigo — e a
metade "vila original" da Regra 3.


---

## 2026-08-13 (01:21) — sete árvores numa vila, zero na outra: a regra da copa travava a colônia

Sessão de vinte e quatro minutos, com `/time set noon`. Duas coisas
opostas no mesmo log.

---

### A colheita contínua funcionando, e bonita de ler

```text
01:13:05  e8f56d2b finished the tree at 1117,63,657 — 9 logs and 57 leaves,  9 this task
01:13:17  ae64bb8c finished the tree at 1096,65,649 — 8 logs and 75 leaves,  8 this task
01:13:40  e8f56d2b finished the tree at 1107,63,692 — 5 logs and 52 leaves, 14 this task
01:13:52  ae64bb8c finished the tree at 1139,69,647 — 8 logs and 54 leaves, 16 this task
01:14:13  e8f56d2b finished the tree at 1091,64,645 — 10 logs and 32 leaves, 24 this task
01:14:43  ae64bb8c finished the tree at 1091,64,642 — 10 logs and 100 leaves, 26 this task
01:14:49  e8f56d2b finished the tree at 1086,63,672 — 9 logs and 85 leaves, 33 this task
```

Sete árvores em menos de dois minutos, dois lenhadores, e a contagem por
tarefa subindo — 9, 14, 24, 33. É a Regra 1 inteira: a tarefa não acaba
numa árvore, acaba quando o baú enche. E a regra da copa não atrapalhou
nada: todas as sete tinham copa, e a copa desceu junto.

---

### E a outra vila, dezesseis minutos sem cortar nada

```text
00:58:20  0c2771b0 lumberjacks: 2898aeb3 looking for a tree, work time (0 logs so far)
01:13:07  0c2771b0 lumberjacks: 2898aeb3 looking for a tree, work time (0 logs so far)
01:14:37  0c2771b0 lumberjacks: 2898aeb3 looking for a tree, work time (0 logs so far)
```

Horário de trabalho confirmado, dois lenhadores, floresta ao alcance —
na sessão de 00:49 essa mesma colônia tinha um lenhador andando até uma
árvore a quinze anéis do centro. Nada.

**A causa é minha, de ontem.** A busca é determinística a partir do
centro, e devolve sempre o tronco mais próximo. Se esse tronco é
construção — e o centro desta colônia é o sino, no meio da vila —, a
regra da copa devolve plano vazio, o lenhador desiste, e no ciclo
seguinte a busca recomeça do centro e acha **o mesmo tronco**. Para
sempre, e sem uma linha de log dizendo o quê.

```text
findNearestLog acha um tronco  →  NEXT_RING.remove(center)   (zera o cursor)
plan(...) devolve vazio        →  Outcome.SEARCHED           (desiste)
ciclo seguinte                 →  busca do anel 0            (acha o mesmo)
```

Antes da regra da copa não travava porque a casa era derrubada — o
defeito veio junto com a proteção que o autor pediu, e ficou escondido
por um dia porque a vila que trava é a que tem construção de tronco
perto do centro.

---

### A correção

`LumberjackWork` passou a anotar o grupo de tronco que a regra da copa
recusou, e a busca deixa de devolvê-lo:

```text
REJECTED       guarda o grupo inteiro, não o tronco que a busca
               devolveu — recusar de um em um faria uma parede de
               vinte e cinco troncos custar vinte e cinco buscas

teto de 4096   uma vila cercada de construção de madeira encheria o
               conjunto sem limite; estourar o teto esquece tudo, o
               que custa uma busca perdida por grupo

log            "Not a tree at 1124,68,738 — 12 logs without a living
               canopy, skipping it from now on"
```

A linha de log é o que faltava: o lenhador mudo passou dois dias sem
dizer por que não trabalhava.

O teste novo põe a construção **mais perto** que a árvore, que é
exatamente a ordem que trava, e afirma as duas coisas: o lenhador chega
à árvore, e o pilar continua de pé. Rodado contra o código sem a
correção, falha.

---

### Um teste que estava frágil, e foi corrigido junto

O `colony_shrink` falhou nesta rodada — não por defeito do mod, mas
porque a estrutura de teste nova deslocou as vizinhas, e a colônia dele
passou a encolher pelo caminho da observação **provadamente completa**,
que é legítimo e imediato.

O teste exigia "não encolheu ainda na primeira leitura menor", e essa
metade não pode ser afirmada num mundo partilhado: se a observação é
provavelmente completa ou não depende das camas do vizinho. Ela ficou
onde já estava coberta —
`PartialObservationTest#aSingleProbeReadingProvesNothing` — e o teste de
jogo passou a afirmar só o caminho inteiro: cama destruída vira contagem
menor.

---

### O que continua sem prova

```text
o cursor da busca      as sete árvores desta sessão estão dentro do
                       alcance do código antigo

a vila original        a metade estrutural da Regra 3

a casa de pé           agora tem duas provas indiretas — o pilar do
                       teste novo e as sete árvores com copa — mas
                       nenhuma sessão em que o autor tenha olhado
                       uma casa de tronco com lenhador ao lado
```

E os dois lenhadores sem baú da `c18264c9` continuam devolvendo tarefa a
cada ciclo. A preferência de 00:45 não os alcança: ela escolhe **quem
recebe** a função, e esses dois já a tinham do save. Fica anotado; a
correção é dispensar quem não consegue baú quando há candidato que
consegue, e é decisão de regra, não de código.

---

### Verificado

```text
288 testes unitários     verdes
 48 testes de jogo       verdes  (eram 47)

negativo: sem a recusa, o teste novo falha
```

---

## 2026-08-13 (01:45) — quem não consegue baú perde a vaga para quem consegue

Decisão do autor, fechando o que a sessão de 01:21 deixou aberto: os dois
lenhadores sem baú da `c18264c9` devolviam a tarefa à fila a cada trinta
segundos, e a preferência escrita às 00:45 não os alcançava — ela escolhe
quem **recebe** a função, e eles já a tinham do save.

---

### A regra

```text
trabalhador com função e sem baú perde a função

só quando existe aldeão sem função que conseguiria um baú

sem candidato, ninguém é dispensado
```

A última linha é a mesma regra da atribuição, vista do outro lado: vaga
vazia não é melhor que trabalhador sem baú. O jogador pode construir o
baú depois, e aí ele o reivindica no ciclo seguinte.

O número de trocas é o número de candidatos, e é o que faz isto
convergir: um baú livre e dois lenhadores sem baú dão uma troca, o
candidato reivindica o baú, e no ciclo seguinte não há mais candidato
nenhum. Sem esse teto, os dois seriam dispensados para um baú só.

---

### O que mudou

```text
ProfessionAssigner    enforceVacancies ganhou o teto de trocas, e
                      dispensa quem falha no teste do baú

VillagerScanner       passou a montar o conjunto de quem consegue baú
                      também quando alguém está ocupando vaga sem baú,
                      e não só quando há vaga aberta

log                   diz quantos dos dispensados foram por falta de
                      baú, e não só quantos foram
```

O custo da sondagem continua barato: o baú é procurado a seis blocos da
cama, o que são um ou dois chunks por candidato.

---

### Verificado

```text
291 testes unitários     verdes  (eram 288)
 48 testes de jogo       verdes

negativo: com o teto de trocas zerado, o teste novo falha
```

Em jogo, o que confirma é a linha `dismissed ... of them had no chest` na
`c18264c9`, seguida do silêncio: sem mais `has no chest` a cada ciclo.

---

## 2026-08-13 (01:53) — a sessão que fechou três pendências de uma vez

Três minutos de jogo, e o log responde tudo o que estava aberto sobre o
lenhador.

---

### A colônia que não cortava há dois dias, cortando

```text
01:52:01  a60c4f43 finished the tree at 1089,63,717 —  8 logs,  49 leaves
01:52:26  2898aeb3 finished the tree at 1095,66,751 — 17 logs, 160 leaves
01:53:07  a60c4f43 finished the tree at 1131,69,731 — 12 logs, 155 leaves
01:53:17  2898aeb3 finished the tree at 1086,66,754 — 20 logs, 163 leaves
```

`2898aeb3` e `a60c4f43` são os dois lenhadores da `0c2771b0`, a vila de
`1109,730` — a que passou a sessão de 01:21 inteira em "looking for a
tree" com horário de trabalho confirmado.

---

### E o cursor da busca, provado pela distância

```text
árvore em 1089,717   anel 20
árvore em 1095,751   anel 21
árvore em 1131,731   anel 22
árvore em 1086,754   anel 24
árvore em 1084,724   anel 25
```

O teto de mil e vinte e quatro colunas acaba no anel 16. **Cinco das seis
árvores desta colônia estão além dele**, e o código anterior não tinha
como alcançá-las: toda busca recomeçava do centro e morria no mesmo
lugar.

É a prova que faltava desde 2026-08-12, e ela veio junto com a explicação
de por que a vila era muda: os anéis de perto dela só tinham construção,
e a floresta começa no vigésimo bloco.

---

### A regra da copa, dos dois lados na mesma sessão

```text
Not a tree at 1092,64,736 — 24 logs without a living canopy
Not a tree at 1122,71,714 — 13 logs
Not a tree at 1098,63,714 —  7 logs
Not a tree at 1128,68,720 —  5 logs
Not a tree at 1103,63,694 —  3 logs
Not a tree at 1103,63,697 —  3 logs
Not a tree at 1112,70,744 —  3 logs
Not a tree at 1113,68,743 —  1 log     ┐
Not a tree at 1115,68,743 —  1 log     │ os postes de lampião
Not a tree at 1113,68,747 —  1 log     │ da vila
Not a tree at 1115,68,747 —  1 log     ┘
```

Onze construções olhadas e deixadas em pé — sessenta e um troncos —, e
oito árvores de verdade derrubadas no mesmo intervalo. É a melhor prova
que o log pode dar da Regra 3: o mod diz, bloco a bloco, o que reconheceu
como construção.

O grupo de 24 é o caso que a primeira versão da regra deixava passar: a
copa só era procurada quando o tronco cabia no teto, e a construção
grande escapava sem ser olhada.

---

### E a troca por baú, silenciosa depois de acontecer

```text
01:51:19  Colony 9a5afa23 dismissed 2 workers (2 of them had no chest
          and lost the job to someone who can get one)
```

Uma linha, e depois nada: **nenhum `has no chest` na sessão inteira**,
contra um a cada trinta segundos nas duas anteriores. É o que a regra
prometia — a troca acontece uma vez e converge.

---

### O que o estoque mostra

```text
01:51:19  0c2771b0 stores {OAK_LOG=128} in 2 of 8 chests read
01:51:49  0c2771b0 stores {OAK_LOG=128, JUNGLE_LOG=8, CHERRY_LOG=16}
                                        in 4 of 8 chests read
```

A madeira nova entrando, e mais baús sendo lidos conforme os
trabalhadores novos reivindicam os seus.

---

### O que continua aberto

```text
a metade estrutural da Regra 3    quem tem protegido a construção em
                                  jogo é a regra da copa, não a
                                  pergunta sobre peça de vila

o lado do cliente                 nome, rachadura e braço

E3, E4, E5                        como estavam
```

Nenhuma exceção do mod na sessão.

---

## 2026-08-13 — A Fase 9: a colônia aprendeu a transformar

Quatro peças, na ordem em que cada uma pôde ser testada sozinha, e a
torneira por último — que é a ordem que a Fase 8 ensinou.

---

### 1. Tirar do baú

`ChestWithdrawer`, o par simétrico do depositor. É a primeira coisa que
este mod faz que **diminui** o que o jogador tem: até aqui a colônia
contava o baú, punha madeira dentro, e nada saía.

Duas regras próprias, por causa disso: só tira o que a colônia conta —
item do jogador guardado no mesmo baú fica onde está — e devolve o que
de fato saiu, e não o que foi pedido, porque a receita depende da
espécie do tronco e um número não diria qual é.

---

### 2. As oito tábuas

A colônia contava oito troncos e uma tábua só, e isso não fechava: o
fabricante transformaria bétula em tábua que o estoque não enxerga, e a
contagem passaria a mentir sem avisar.

As oito entraram na tabela de espécies, que é onde as madeiras já moram.
E o grupo `PLANKS` entrou junto, corrigindo o que o `ResourceGroup`
dizia. A frase antiga — "tábua não se substitui" — valia para **receita**
e continua valendo; a pergunta do grupo é outra: "já tenho material
fabricado suficiente?". Sem ele, uma colônia de floresta de bétula
fabricaria para sempre, que é o E1 por outra porta.

---

### 3. A receita, perguntada ao jogo

`CraftingLookup` pergunta ao `RecipeManager` do servidor o que sai de um
tronco sozinho. A alternativa — escrever "um tronco dá quatro tábuas" no
mod — seria uma segunda verdade sobre o jogo, divergente no dia em que um
datapack mudasse a receita. É a mesma escolha da tabela de loot da
colheita.

Só receita de uma casa, que é a que cabe na mão sem bancada. **A pergunta
"onde a colônia fabrica" não foi respondida, e não precisou ser:** ela só
tem sentido quando existir receita que exija mesa, e aí é decisão do
autor.

---

### 4. O fabricante

`ManufacturerWork` tem a forma do lenhador — despacho no ciclo longo,
trabalho um passo por tick, anda até o próprio baú e trabalha ali — com
uma regra que o lenhador não tem:

```text
nada sai do baú antes da peça ficar pronta
```

O tronco é retirado, transformado e devolvido no mesmo tick. Durante a
espera existe um contador, e não um tronco na mão de um aldeão que pode
morrer, ser zumbificado, ou estar num servidor que vai ser desligado. O
E3 registra o que acontece do outro lado, quando algo sai do mundo antes
de ter para onde ir.

Um segundo por peça. É número inventado, e está dito na classe: a Regra 2
tem a fórmula do jogo para tempo de quebra, e fabricar não tem
equivalente — o jogador faz tábua num clique.

---

### 5. A torneira, por último

A meta de tábua — Regra 5 do §18 — entrou depois de existir quem
executasse. É a lição da Fase 8: tarefa aberta sem executor possível fica
reservada para sempre.

```text
meta = (tábua guardada + tábua que ainda cabe) / 2

e zero enquanto não houver tronco guardado
```

A segunda linha nasceu de uma pergunta feita antes de rodar: sem ela, uma
colônia sem madeira abriria tarefa de fabricação a cada ciclo para o
fabricante encerrá-la no tick seguinte por falta de material. Trabalho
nenhum e uma linha de log por ciclo — o E1 de novo.

O teste que fecha o laço não entrega tarefa pronta a ninguém: põe tronco
no baú, roda o ciclo da colônia e afirma que a tarefa nasce e vira tábua.
Rodado com a torneira fechada, falha.

---

### O suite parou de se sabotar, e isso custou meia sessão

Enquanto a Fase 9 era escrita, dois testes antigos passaram a falhar de
forma intermitente. A causa não era o mod: **os testes de jogo rodam
concorrentes**. Um teste que atravessa noventa ticks continua vivo
enquanto os batches seguintes começam, e cada teste chamava
`COLONIES.clear()` — apagando a colônia de quem estava no meio do
caminho.

O sintoma era o `cycle_deficit` acusando "a colônia não pediu madeira"
com a colônia dele já apagada por um vizinho.

```text
ColonyFixture      cada teste guarda o que registrou e desfaz só aquilo

detecção           procura a colônia da própria estrutura, em vez de
                   afirmar sobre a contagem global

encolhimento       o teste saiu. Ele precisa de duas leituras sobre o
                   mesmo aglomerado de camas, e o aglomerado inclui as
                   camas das estruturas vizinhas — que outros testes
                   plantam enquanto ele mede
```

O motivo do teste removido ficou escrito no lugar onde ele estava, ao
lado do caso idêntico descartado em 2026-08-08 pela mesma razão. A regra
continua coberta em `PartialObservationTest`, e o caminho inteiro foi
verificado em jogo em 2026-08-07.

Depois disso, três rodadas completas seguidas, todas verdes.

---

### Verificado

```text
299 testes unitários     verdes
 60 testes de jogo       verdes, em rodadas repetidas

negativo, por regra desligada:

  a recusa do grupo sem copa    o lenhador trava na construção
  a torneira da Regra 5         o ciclo não abre tarefa nenhuma
```

**Nada da Fase 9 foi visto em jogo.** É a dívida de sempre desta camada,
e o §8 tem o item: a linha `manufacturers:` aparecendo, tábua entrando no
baú, tronco sumindo na mesma conta, e a colônia parando sozinha na
metade.

---

## 2026-08-13 — Fecho de sessão: o que foi feito, o que falhou, e o que fica

Quinze commits, de `c7670bf` a `6bd2a57`. Esta entrada existe para que
quem retomar não precise ler as outras.

---

### O que foi criado

```text
BlockProtection      a porta única para "posso quebrar isto?" (Regra 3)
ChestWithdrawer      tirar item do baú — o mod nunca tinha feito isso
CraftingLookup       a receita perguntada ao RecipeManager do jogo
ManufacturerWork     o fabricante: tronco vira tábua, no baú dele
ColonyFixture        o teste limpa só o que criou

Development-Log.md   este arquivo, separado do Project-State
```

---

### O que mudou de comportamento

```text
tronco sem copa viva não é árvore        Regra 3, e a exceção da árvore

a busca guarda em que anel parou         o raio de 64 deixou de ser
                                         decorativo

grupo recusado não é reencontrado        sem isso a colônia trava

dois trabalhadores por profissão         Regra 4, era um

a vaga prefere quem consegue baú         na atribuição e na dispensa

a meta de tábua                          Regra 5, metade do que cabe

o teto de tarefa RESERVED                Task.complete deixou de
                                         derrubar o servidor

as oito tábuas e o grupo PLANKS          a colônia enxerga o que fabrica
```

---

### O que falhou, e o que cada falha ensinou

```text
a regra da copa travou uma colônia inteira

  Escrita num dia, e no seguinte a vila de 1109,730 passou dezesseis
  minutos em horário de trabalho sem cortar nada: a busca achava a casa
  mais próxima, a copa recusava, e o ciclo seguinte achava a mesma casa.
  Regra nova precisa ser olhada pelo lado de quem ela recusa.

Task.complete numa tarefa RESERVED derrubava o servidor

  Achado pelo teste rodado contra a própria regra desligada, e não em
  jogo. O passo de "provar que o teste falha" pagou o preço dele inteiro
  numa vez só.

/time set day não é horário de trabalho

  Instrução minha, errada, que custou duas sessões do autor. A agenda do
  aldeão põe WORK em 2000; day é 1000.

os testes de jogo rodam concorrentes

  Cada um limpava os registros globais, e um apagava a colônia do outro
  no meio do caminho. Meia sessão para descobrir, e um teste que não
  pode existir nesta bateria — o do encolhimento — foi removido com o
  motivo escrito no lugar dele.

duas conclusões minhas foram desmentidas pelo próprio log

  As árvores "longe" da sessão de 00:49 estavam dentro do alcance
  antigo, e não provavam o cursor. A prova veio depois, com árvores nos
  anéis 20 a 25.
```

---

### O que ficou por fazer

```text
1  ver a Fase 9 em jogo

   Nada dela rodou fora de teste. A linha "manufacturers:", a tábua
   entrando no baú, o tronco sumindo na conta de quatro para um, e a
   colônia parando na metade.

2  a metade estrutural da Regra 3

   Hoje quem protege a construção em jogo é a regra da copa. A pergunta
   sobre peça de vila gerada nunca foi exercitada: o mundo do gametest
   não tem vila.

3  o lado do cliente

   Nome sobre a cabeça, rachadura no bloco e braço balançando.

4  E3, E4 e E5 do §17

   Perda de item na sobra da colheita, o "path held: no", e as seis
   espécies que nunca caíram em jogo.

5  a Fase 10

   Três decisões de regra antes da primeira linha de código: onde a
   colônia constrói, o que constrói, e quando para. Ver §7.
```

---

### O que não está escrito em lugar nenhum, e devia

Nada. As regras do autor estão no §18 com a data de cada uma, os erros
conhecidos no §17, a fila no §8, e o caminho de volta no §14.

---

## 2026-08-13 — Os três itens que não dependiam de ninguém

Os itens A, B e C do §8: as três regras aceitas em documento que nunca
tinham sido implementadas, e que não esperavam nem decisão do autor nem
sessão de jogo. Saíram as três na mesma sessão.

---

### A — a colônia abandonada

`ColonyState.ABANDONED` existia no enum desde a Fase 1, com javadoc
explicando por que o nome não era DORMANT, e **nada em produção jamais
chamou `setState`**. O eixo inteiro de estado da colônia era um campo
gravado no save e nunca escrito.

A causa estava na detecção, e a ADR-003 §6 já a nomeava: `VillageScanner`
devolvia apenas aglomerados aprovados. "A vila deixou de ser viável" e "a
vila não foi observada" chegavam ao mesmo lugar com a mesma cara — uma
lista vazia.

O que foi feito:

```text
ClusterRejection     o aglomerado recusado, com o motivo: camas de
                     menos ou gente de menos

rejectionOf          a validação da ADR-003 §3 num lugar só, e é
                     dela que evaluate passou a depender. Duas
                     cópias da regra divergiriam no dia em que
                     alguém mexesse numa

VillageScanner       ganhou survey, que devolve o aprovado e o
                     recusado. scan continua devolvendo só o
                     aprovado, que é o que a detecção precisa

ColonyAbandonment    a decisão, no Core: quem sonda de dentro de si
                     mesma e não acha vila, é abandonada
```

Três guardas, e cada um responde a uma forma de errar:

```text
só a sonda da própria colônia    a varredura do jogador parte de um
                                 ponto que muda; a sonda parte sempre
                                 do centro

só colônia ACTIVE                colônia dormente tem os chunks
                                 descarregados, sua varredura não
                                 acharia cama alguma, e toda vila
                                 longe do jogador seria declarada
                                 morta

bioma recusado não condena       aglomerado fora de PLAINS é limite
                                 do MVP (ADR-003 §5), não vila morta.
                                 Um centro que caminhasse para a
                                 borda do bioma condenaria uma vila
                                 cheia de gente
```

Não exige confirmação em dois ciclos, ao contrário do encolhimento, e a
diferença tem motivo: o encolhimento **perde** informação — a contagem
menor sobrescreve a maior e não há volta. O abandono não perde nada e se
desfaz sozinho na primeira varredura que enxergar vila.

O que a colônia abandonada faz de diferente: **nada**. Ela é marcada,
gravada e continua sendo simulada. Parar de simular vila morta é decisão
que ninguém tomou, e não é o lifecycle que a resolve — ABANDONED com
jogador ao lado continua ACTIVE.

---

### B — o aviso de sobreposição

A ADR-003 §5 pede a linha desde 2026-08-06. `ColonyService.overlapping`
responde quem está a menos de 32 blocos, e a detecção avisa uma vez por
par por sessão — a sobreposição não se resolve sozinha, e sem a memória
do par seriam cem linhas iguais por hora.

O par é ordenado pelo id antes de virar chave. A sonda de cada uma das
duas encontra a outra, e sem a ordem fixa o mesmo par sairia duas vezes.

O aviso não funde nada, e é isso que a ADR manda. O que ele dá é o nome
do problema quando ele aparecer: duas colônias sobrepostas disputam
trabalhador, e sem esta linha o sintoma seria um aldeão trocando de vila
sem motivo aparente.

---

### C — a ferramenta do trabalhador

`ToolType` existia desde a Fase 4, cada profissão declarava a sua, e não
havia conversão para item. `MinecraftTypeAdapter.toItem` fechou o vão, e
`WorkerEquipment` entrega — e retira de quem perde a função, pelo mesmo
motivo que a marca do baú sai.

Duas decisões que valem ficar escritas:

```text
não muda a velocidade de       a Regra 2 diz "no tempo de um jogador
trabalho                       com ferramenta de ferro". Passar a
                               medir pelo machado de madeira que o
                               aldeão agora carrega tornaria a
                               colheita mais lenta do que a regra
                               manda — seria trocar uma regra do
                               autor por uma consequência de
                               implementação. LumberjackWork continua
                               de ferro, e o comentário que prometia
                               o contrário foi corrigido

não vira drop                  a ferramenta é criada do nada. Se
                               caísse com a morte do aldeão, matar
                               trabalhadores seria uma forma de
                               colher machados
```

E o limite honesto: **ninguém vê**. Conferido no jarro mapeado da
1.21.1 — `VillagerResemblingModel` implementa `ModelWithHead` e
`ModelWithHat`, nunca `ModelWithArms`, e `VillagerEntityRenderer` não
monta `HeldItemFeatureRenderer`. O modelo de aldeão do Vanilla não tem
onde pendurar um item. Foi feito porque Profession-System.md pede, porque
persiste no NBT e porque é de onde a Fase 10 vai puxar a ferramenta do
construtor — não porque acrescente algo à tela.

---

### O que os testes acharam

```text
o aglomerado vazio            evaluate deixou de barrá-lo quando
                              passou a depender de rejectionOf, e
                              averageOf dividiu por zero. Achado por
                              emptyClusterIsNotAVillage, que já
                              existia

                              A correção não foi só um if: "não vi
                              nada" e "vi e recusei" precisam ser
                              coisas diferentes, porque a segunda
                              marca colônia abandonada e a primeira
                              não pode marcar nada
```

E o passo do §11 — rodar o teste novo contra a regra desligada — fez o
que devia: com `WorkerEquipment.equip` devolvendo zero, os dois testes de
jogo novos falharam com a mensagem certa.

```text
LUMBERJACK deveria segurar minecraft:wooden_axe e segura nada
nenhum trabalhador armado para matar — o teste não afirmaria nada
```

Nessa mesma rodada sabotada apareceu um terceiro, que não é meu:
`villagersBecomeWorkersWithAProfession` falhou com "trabalhador sem
profissão: 4 de 12". É a contaminação entre testes concorrentes já
descrita em ColonyDetectionGameTest — a colônia absorveu aldeões das
estruturas vizinhas — encontrando o teto da Regra 4: com doze
trabalhadores e oito vagas, quatro ficam sem função, e a afirmação "todos
têm profissão" deixa de ser verdadeira sem que nada esteja errado. Passou
nas três rodadas não sabotadas. Fica registrado como flaky latente.

---

### O que ficou por fazer

```text
1  ver os três em jogo

   Nenhum rodou fora de teste. Cabem na mesma sessão da Fase 9: a
   linha "is now ABANDONED" ao demolir camas, o "Equipped N workers",
   e o aviso de sobreposição se o mundo der o acaso.

2  a colônia abandonada não muda nada

   Ela é marcada e continua sendo simulada. Se isso deve mudar é
   decisão do autor, e não estava no item A.

3  o que já estava por fazer

   A Fase 9 em jogo, a metade estrutural da Regra 3, o lado do
   cliente, E3/E4/E5, e as três decisões da Fase 10.
```

---

## 2026-08-14 — A Fase 10 abre: o projeto, a casa do jogo, e onde ela cabe

O autor decidiu as três perguntas que bloqueavam a fase desde que ela foi
escrita, e delegou as decisões de implementação. O enunciado está no §18
como Regra 6; aqui fica o que foi construído sobre ele.

---

### O que foi entregue

```text
TASK-030  Blueprint, BlueprintBlock, ResourceId
TASK-031  StructureBlueprintReader — a casa vem do arquivo do jogo
TASK-032  ConstructionProject, ConstructionState, e a Regra 5 ligada
extra     BuildSiteScanner — onde a próxima casa cabe
```

Nada disto mexe no mundo. O que falta da fase é justamente o que mexe:
TASK-033 a 035.

---

### A medição que decidiu o desenho

A casa de planície do Vanilla, lida do próprio arquivo:

```text
oak_stairs        49
cobblestone       43
oak_planks        33
stripped_oak_log  16
wall_torch         3
glass_pane         3
oak_door           2
white_bed          2
```

A colônia produz **tábua**, e só. Escada e porta ela saberia fazer — têm
receita a partir de tábua, e o `CraftingLookup` já pergunta ao jogo.
Pedra, vidro, cama e tocha estão fora do MVP; mineração é pós-MVP
declarado.

**Decisão tomada por delegação:** a obra pede tudo o que a casa tem, e o
que a colônia não produz precisa já estar nos baús. Construção nunca cria
recurso — é regra do próprio Construction-System.md. Na prática o jogador
estoca pedra e vidro e a vila levanta a casa; sem isso a obra fica em
WAITING_RESOURCES, que é estado previsto, não defeito.

É a decisão mais fácil de derrubar desta fase, junto com a Regra 5.

---

### As quatro decisões de implementação

Três delegadas pelo autor, e uma que o código obrigou:

```text
distância da rua    encostada, um bloco. Mais que isso abre quintal
                    entre a casa e a rua

rua por vez         nenhuma ainda. A leitura barata da Regra 6 é
                    procurar lote ao lado de rua que já existe — e a
                    vila de planície nasce cheia de rua. Estender a rua
                    fica para quando a beira livre acabar

desnível            dois blocos dentro do lote

janela do lote      dois blocos acima do nível do centro da colônia, e
                    oito abaixo. Vila não constrói no morro que a olha
                    de cima; e o centro sai das camas, que ficam no
                    piso das casas, acima da rua
```

---

### Três defeitos, e o que cada um ensina

```text
getInfosForBlock(pos, data, null) devolve zero

  A forma óbvia de enumerar um template. Na 1.21.1 ela não enumera
  nada, e não avisa: o template carrega, informa o tamanho certo e
  entrega lista vazia.

  Só foi possível ver porque a mensagem tinha sido separada antes —
  "o jogo não tem essa estrutura" e "tem, e não enumerou" são
  correções diferentes. A leitura passou a ser pelo NBT que o próprio
  jogo grava, que aliás traz o nome do bloco, que é o que o Blueprint
  guarda.
```

```text
o mapa de alturas não serve dentro do gametest

  A arena é fechada por barreira. MOTION_BLOCKING devolveu h=-51 com a
  grama em -59: o teto, não o chão. Num mundo de verdade daria a
  superfície e estaria certo — é um defeito que só existe no ambiente
  de teste, e mesmo assim precisava de correção, porque o teste é onde
  a fronteira se prova.

  Trocado por uma janela de colunas em torno do nível da vila, que
  vale nos dois mundos.
```

```text
a janela recortava o morro                    ← este era de verdade

  Com janela de dois blocos para cima, uma torre de quatro era lida
  como dois — a altura do teto da janela — e um lote com desnível de
  quatro passava pelo limite de dois. A casa nasceria enfiada na
  encosta.

  Agora qualquer coisa acima da janela reprova a coluna inteira.
  Efeito colateral assumido: lote com árvore em cima é recusado, e a
  colônia procura outro lugar em vez de derrubar o que não planejou.
```

O terceiro foi achado pelo gametest do desnível, que existia havia dez
minutos. Os dois primeiros, pela linha de diagnóstico escrita antes da
suspeita — o §11 de novo, pela terceira sessão seguida.

---

### Um teste meu que estava errado

Os dois gametests de equipamento, escritos na véspera, afirmavam sobre os
trabalhadores da colônia mais próxima e **matavam um deles**. Numa
bateria concorrente essa colônia é compartilhada com as estruturas
vizinhas: o teste matava o aldeão de outro teste, que é exatamente o que
o `ColonyFixture` proíbe — e eu tinha citado essa regra ao escrevê-los.

Refeitos para construir os próprios `Worker` na hora, apontando para
aldeões que eles mesmos criaram. Ganharam dois casos que faltavam: o que
o jogador já segura fica, e a ferramenta volta quando a função vai
embora.

---

### O que ficou por fazer

```text
1  TASK-033 a 035 — o que mexe no mundo

   A tarefa de obra, o construtor e a colocação de blocos. É onde a
   Fase 10 encosta no mundo pela primeira vez, e o §11 diz o que
   esperar disso.

2  estender a rua

   Quando não houver mais lote encostado em rua. Sem isso a vila para
   de crescer antes do que a Regra 6 permite.

3  a Fase 11 junto da TASK-035

   Colocar o bloco e registrar de quem ele é são o mesmo momento, e a
   fusão de vilas decidida em 08-12 depende desse registro.

4  o que já estava por fazer

   A Fase 9 em jogo, os itens A/B/C em jogo, a metade estrutural da
   Regra 3, o lado do cliente, e E3/E4/E5.
```

---

## 2026-08-14 (fim da noite) — A colônia constrói

O sexto passo do MVP. Depois desta entrada não há mais fase por escrever:
o que falta é ver.

---

### O que foi entregue

```text
TASK-033  ConstructionService, ConstructionPlanner
TASK-034  BuilderWork
TASK-035  colocar blocos, um por segundo
TASK-036  Building, BuildingRegistry
TASK-037  todo bloco da caixa é infraestrutura da colônia
```

A Fase 11 veio junto porque é o mesmo instante: colocar o bloco e dizer
de quem ele é não são dois passos, e a fusão de vilas decidida em 08-12
depende do segundo.

---

### Os quatro guardas do construtor

É o primeiro trabalho do mod que **acrescenta** bloco. O lenhador tira, o
fabricante transforma. Bloco posto no lugar errado é dano que ninguém
desfaz, e por isso cada guarda responde a uma forma de estragar o mundo
do jogador:

```text
o lote já vem livre    BuildSiteScanner recusa terreno com qualquer
                       coisa em cima

nada substitui nada    só entra bloco onde havia ar, grama alta ou flor

o material sai antes   a peça é tirada do baú e só então o bloco entra
                       no mundo. Sem material não há bloco

o que não se apoia     tocha sem parede e porta sem chão são riscadas
é riscado              com uma linha, e não tentadas para sempre. Obra
                       que não termina é pior que casa com buraco
```

---

### Duas decisões de ordem

```text
de baixo para cima     o arquivo do jogo não promete ordem nenhuma, e
                       seguir a dele poria o telhado antes da parede. A
                       ordenação é da leitura do projeto, não do
                       construtor: duas leituras do mesmo arquivo dão
                       exatamente a mesma casa

a torneira por último  não se abre obra sem construtor na vila. Uma obra
                       sem executor ficaria aberta para sempre, e a meta
                       de tábua da Regra 5 apontaria para uma casa que
                       ninguém levanta — o fabricante encheria os baús
                       por causa de um canteiro fantasma. É a mesma
                       ordem que a Fase 9 seguiu
```

---

### O registro é por caixa, não por bloco

`Building` guarda os dois cantos da construção. Responde às três
perguntas que dependem dele — a proteção, a fusão e o próximo lote — por
um preço muito menor que cento e cinquenta posições por casa.

O que se perde: o vazio dentro da casa passa a ser "da colônia". Uma
árvore que nascesse no meio do quarto estaria protegida. É o lado seguro
do erro — protege demais, nunca de menos.

---

### O que foi assumido, e é bom não confundir com esquecimento

```text
o material não viaja   o construtor tira do baú da colônia sem ir até
                       ele. Carregar material é logística, declarada
                       fora do MVP em MVP-Tasks.md

PREPARING passa direto o lote só é aceito quando não há nada em cima
                       dele, então a limpeza de terreno não tem o que
                       limpar. Implementá-la agora seria escrever código
                       para um caso que a escolha do lote já excluiu

o estado do bloco      escada e porta saem no padrão, sem a orientação
                       que tinham no arquivo. A casa sai de pé e um
                       pouco tosca
```

---

### A dívida nova, e é a mais cara do projeto

Nem a obra nem o registro de construções vão para o save.

A obra interrompida é replanejada na sessão seguinte, e isso passa. O que
dói é o registro: **ao reabrir o mundo, a colônia esquece que a casa é
dela.** A casa continua de pé — quem guarda blocos é o mundo —, mas a
proteção do §10 da Constituição some, e a fusão de vilas perde a memória
de que depende.

Está no §9. É o primeiro item da fila depois da sessão de jogo.

---

### O que ficou por fazer

```text
1  ver o MVP inteiro em jogo

   A Fase 9, a 10 e a 11 numa sessão só. Precisa de um construtor na
   vila e de pedra e vidro nos baús — a colônia produz tábua e nada
   mais, e a casa pede 43 de pedra.

2  persistir obra e construção

   A dívida acima.

3  estender a estrada

   Hoje a vila só constrói em beira de rua que já existe.

4  o que já estava por fazer

   Os itens A/B/C em jogo, a metade estrutural da Regra 3, o lado do
   cliente, E3/E4/E5, e a TASK-042.
```

---

## 2026-08-14 (madrugada) — A casa continua sendo da colônia

A dívida que a entrada anterior deixou aberta, e que ela mesma chamou de
mais cara do projeto: nem a obra nem o registro de construções iam para o
save. A casa ficava de pé — quem guarda blocos é o mundo —, mas a colônia
reabria sem saber que ela era dela.

---

### O que vai para o disco

No mesmo arquivo das colônias, pelo mesmo motivo dos trabalhadores em
2026-08-07: arquivos separados permitiriam construção órfã, apontando
para colônia que não foi gravada, sem transação que mantivesse os dois em
sincronia.

```text
construções   id, colônia, estrutura, e os dois cantos da caixa

obras         id, colônia, estrutura, lugar e estado
```

---

### O que não vai, e por quê

**O progresso da obra.** Quem sabe o que já está de pé é o mundo, e é a
ele que a sessão seguinte pergunta: ao retomar, cada bloco do projeto
cujo lugar já contém o bloco certo sai da lista.

Sai mais barato no disco e sai mais **certo**. Uma lista de posições
gravada juraria que a parede está lá; se o jogador a derrubou entre uma
sessão e outra, a colônia a levanta de novo — e essa é a resposta que se
quer, não a que o arquivo teria dado.

Custa uma leitura de bloco por peça, uma vez por colônia por sessão.

---

### A obra volta em duas etapas

Porque precisa: o save é lido antes de haver mundo, e um projeto precisa
da estrutura do jogo para existir. Então a identidade e o lugar saem do
arquivo na abertura, e o projeto inteiro nasce no primeiro ciclo da
colônia, quando há a quem perguntar.

```text
estrutura que sumiu    obra abandonada com uma linha, em vez de tentar
                       renascer a cada ciclo para sempre

estado desconhecido    vira BUILDING, que é de onde a obra continua
                       sozinha. COMPLETED apagaria do registro uma casa
                       pela metade; PLANNED a faria esperar por uma
                       preparação que já aconteceu

save antigo            carrega sem as chaves. Perde-se a memória de
                       casas anteriores a esta versão — que é o que a
                       versão anterior perdia toda vez
```

---

### Um defeito meu, do dia anterior

O `ColonyFixture` não limpava obra nem construção. Os testes de construção
escritos horas antes deixavam canteiro e casa nos registros globais, e a
bateria roda concorrente: o lote que a colônia de outro teste escolhesse
poderia cair sobre a casa deste.

É a segunda vez em dois dias que eu escrevo um teste que suja o mundo dos
vizinhos, depois de citar a regra que proíbe isso. A regra está no topo do
`ColonyFixture`; o que faltou foi lembrar dela ao criar um registro
global novo.

---

### O que ficou por fazer

```text
1  ver o MVP inteiro em jogo

   Continua sendo o primeiro item. Precisa de um construtor na vila e
   de pedra e vidro nos baús.

2  estender a estrada

   A vila só constrói em beira de rua que já existe.

3  o que já estava por fazer

   Os itens A/B/C em jogo, a metade estrutural da Regra 3, o lado do
   cliente, E3/E4/E5, e a TASK-042 — que agora tem o que verificar:
   fechar o mundo com uma obra em curso e reabrir.
```

---

## 2026-08-14, à noite — a primeira sessão da Fase 9, e o que ela mostrou

O autor entrou no mundo, passeou dezesseis minutos e saiu. A sessão foi
curta e rendeu mais que várias longas: era a primeira vez que a Fase 9
rodava fora de teste.

Ela não fabricou nada.

### O que o log disse

```text
Colony 0c2771b0 stores {OAK_LOG=134, JUNGLE_LOG=14, CHERRY_LOG=49}
Worker 3f052d86 finished crafting — 0 pieces made, stopped because
                no logs left in the chest
```

Dezessete vezes essa segunda linha, uma por fabricante por ciclo. Os
lenhadores derrubaram o tempo todo — 28 toras cada, árvores de 179
blocos —, a colônia guardava 134 troncos, e os quatro fabricantes
encerravam a tarefa dizendo que não havia tronco.

As duas linhas estão a poucos segundos uma da outra e se contradizem.
É o tipo de contradição que só o log em jogo produz, e que nenhuma das
duas linhas, sozinha, denunciaria.

### A causa

Não era o executor nem a torneira. Era o desacordo entre os dois sobre
onde fica o estoque.

```text
ColonyGoals            a meta da Regra 5 se mede no ResourceTally da
                       colônia inteira — a soma de todos os baús

ManufacturerWork       convertOne tirava o tronco de
.convertOne            storage.chestPosition(): o baú do próprio
                       fabricante

LumberjackWork         deposita em storage.chestPosition(): o baú do
                       próprio lenhador
```

Nada nunca põe tronco no baú de um fabricante. A meta olhava a colônia
e via madeira; o executor olhava um baú e via vazio. A cada ciclo a
meta abria a tarefa e o executor a encerrava no tick seguinte.

O comentário do próprio `ColonyGoals`, escrito em 08-13, descreve esse
desfecho com precisão — "abriria tarefa de fabricação a cada ciclo para
o fabricante encerrá-la no tick seguinte, que é o E1 voltando por outra
porta" — e põe um guarda contra a causa errada. O guarda é "não pedir
tábua sem tronco guardado", e ele funcionou: havia tronco guardado. O
E1 voltou pela porta ao lado.

### A correção

A retirada passou a ser da colônia, percorrendo os baús dos
trabalhadores dela — que é exatamente o que `BuilderWork.takeMaterial`
já fazia desde a Fase 10, e cujo comentário dizia, em voz alta, que o
fabricante era o contrário disso "porque o que ele faz é transformar o
que já é seu". A intenção era legítima e o mundo não a sustenta.

A tábua volta ao baú **de onde o tronco saiu**, e não ao do fabricante:
preserva a regra do mesmo baú no mesmo tick, e o lugar aberto pela
retirada é onde a peça cabe.

O que continua sendo do fabricante é o lugar. Ele anda até o próprio
baú e trabalha ali, e sem baú próprio não trabalha — o que mantém de pé
a Regra 4 e a dispensa de quem não consegue baú.

### Por que 76 testes de jogo verdes não pegaram

Todos os quatro testes da Fase 9 punham o tronco no baú do fabricante.

O §11 deste projeto ensina que o teste unitário não alcança a fronteira,
e a Fase 9 tinha teste de fronteira — quatro. O que faltava era outra
coisa, e virou linha nova no §11: **o teste precisa modelar o mundo que
acontece.** A pergunta não é "este código funciona?", é "quem põe esta
coisa aqui, em jogo?". Ninguém põe tronco no baú de um fabricante.

O teste novo põe o tronco no baú do lenhador, com o baú do fabricante
vazio, e falhava antes da correção.

### TASK-045, de carona

Com o log já lido e a árvore limpa, fechou-se a dívida que o §8 chamava
de mais barata da lista: `BlockProtection` passou a consultar
`BuildingRegistry.isColonyInfrastructure`. São três agora os que não se
quebram — bloco de vila gerada, bloco do jogador, e bloco de casa que a
colônia levantou.

O teste novo foi rodado contra a regra desligada, como o §11 manda, e
falhou sozinho.

### O que a sessão mostrou e não foi corrigido

```text
E11  rodízio de profissão

     Nove dispensas em dezesseis minutos na colônia 9a5afa23, uma por
     ciclo, cada uma seguida de "Assigned 1 professions". A colônia
     dispensa quem não conseguiu baú em favor de quem consegue, e o
     substituto também não consegue.

     Não travou nada. Custa trabalho por ciclo e trabalhador trocando
     de função sem que nada tenha mudado no mundo.

     Não foi corrigido porque a correção mexe na Regra 4 — dispensar
     só faz sentido se o substituto puder de fato conseguir baú. É
     decisão do autor: TASK-049.

E12  "Equipped N workers" nunca apareceu

     Nem "Named N workers", com 80 trabalhadores em três colônias. As
     duas linhas rodam na bateria de gametest.

     A explicação provável é que só registram quando o número é maior
     que zero, e as colônias vieram do save já nomeadas e equipadas.
     Provável, não verificado. O item C do §8 continua sem ter sido
     visto em jogo.
```

### O que ficou por fazer

```text
1  rodar a sessão de novo

   A correção do E10 muda exatamente o que a sessão não conseguiu
   ver. Precisa de um construtor na vila, /time set noon, e pedra e
   vidro nos baús — a colônia produz tábua e nada mais, e a casa pede
   43 de pedra.

   O jar trocado com o jogo aberto não testa nada.

2  decidir o E11                   TASK-049

3  o que já estava por fazer

   Estender a estrada, os itens A/B/C em jogo, a metade estrutural da
   Regra 3, o lado do cliente, E3/E4/E5/E8/E9, e a TASK-042.
```

366 testes unitários e 78 de jogo, verdes.

---

### Adendo, mesma noite — o jar era velho

Escrito depois do texto acima, e ele fica como está para que o erro
apareça: parte do que se concluiu da sessão não se sustenta.

Ao instalar o jar novo, o antigo estava datado de **08-13, 08:55** —
anterior às Fases 10 e 11 e aos itens A, B e C.

A prova não depende da data do arquivo. A sessão encerrou com

```text
Saved 3 colonies with 80 workers
```

e o código de 08-14 escreve `... workers, {} buildings and {} open
projects`. O formato mudou quando a persistência da obra entrou. A
linha antiga diz qual código estava rodando.

**O que cai e o que fica.** O E10 fica inteiro: `ManufacturerWork` é
código de 08-13, idêntico no jar velho e no novo, e o defeito é real —
dezessete tarefas encerradas com tronco na colônia. A correção continua
de pé e continua necessária.

O que cai é a leitura do silêncio das Fases 10 e 11. Não houve linha
"planned" nem construtor trabalhando porque **esse código não estava
no jar**, e não por falta de pedra nos baús. Sobre elas a sessão não
disse nada, e o texto acima chegou a insinuar que sim.

Fica como E13 do §17, e a lição é mais simples que a do E10: o log diz
qual versão está rodando, na linha de carregamento, e esse é o primeiro
lugar a olhar — antes de concluir qualquer coisa a partir do que uma
fase deixou de dizer.
