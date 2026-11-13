# Avaliação — Bloco C: save, desempenho, compatibilidade e segurança (2026-11-13)

Snapshot: worktree isolada em `ac2f1977` (0.4.74). Só diagnóstico: nada no repositório foi alterado nem commitado.

## O que foi rodado de verdade e o que só foi lido

**Rodado nesta sessão:**

- `./gradlew.bat assemble -q`: EXIT 0, gerou `build/libs/village-colony-0.4.74.jar` (4,8 MB). Não rodei `build` inteiro
  porque o `check` arrasta `propertyTest`/`test`, e outra sessão roda os testes agora.
- `./gradlew.bat runServer` (servidor dedicado de teste, porta 25599, `online-mode=false`, mundo novo, sem jogador), com
  `save-all` aos 150 s e `stop` 15 s depois. O `eula.txt` foi copiado do `run/` que o autor já usa no repositório
  principal. O diretório `run/` da worktree foi apagado depois do teste. O log ficou num arquivo temporário, não versionado.
- `python scripts/cost_ledger.py` sobre o `latest.log` (jogo da 0.4.68, 1 colônia) e sobre as linhas `VC_COST` dos 40
  `.log.gz` mais recentes (de 07-11 a 11-11, até 6 colônias).
- `python scripts/time_ledger.py` sobre o `latest.log`.
- `grep`/`ls`/`du` nos arquivos que o mod grava em `%APPDATA%\.minecraft`.
- Leitura do código-fonte Yarn do Minecraft 1.21.1 (jar de fontes do Loom) para `PersistentState.save`,
  `PersistentStateManager.readFromFile`, `NbtIo.writeCompressed`, `NbtCompound.getUuid` e
  `ServerChunkLoadingManager.generate`.

**Só lido, sem rodar:** todo o código do mod citado abaixo, `fabric.mod.json`, os mixins, `README.md`, `SECURITY.md` e
`docs/audit/2026-11-13-jogo-0468.md`, de onde vêm os números do Spark `El42EuZ5w9`. Não baixei o Spark. Não rodei
`runGametest`, Sentinela nem testes unitários, conforme o pedido.

---

### D3 — Confiabilidade e save (ISO 25010: confiabilidade — tolerância a falhas, recuperabilidade)
Nota proposta: 6/10. O save tem versão, migração, leitura defensiva e cópia a cada autosave. Faltam três coisas: prova
com save antigo de verdade, proteção contra arquivo truncado e uma política para a mina que não descarte progresso na
atualização.

Evidências:
- CONFIRMADO: o estado vive em 5 `PersistentState` no Overworld (`ColonySavedData`, `WorkMarksSavedData`,
  `WorkMemorySavedData`, `DemandRetrySave`, `ProfessionPolicySavedData`, em `src/main/java/com/villagecolony/data/save/`).
  No save do autor eles são pequenos: `villagecolony_colonies.dat` tem 22.640 bytes e os outros têm menos de 1 KB
  (`ls` em `saves/Novo mundo/data`).
- CONFIRMADO: existe versão de esquema com migração encadeada. `SaveMigration.CURRENT = 1`
  (`data/save/SaveMigration.java:47`), com `migrate` v0→v1 (BREEDER→SHEPHERD) e `stamp`. A versão roda antes de
  qualquer leitor (`ColonySavedData.java:487`). Seções com versão própria: `RoadGraphSave` (`VERSION`), `MaeWorkStore`
  (1 a 3, `MaeWorkStore.java:20-21`) e `MineSave.SHAPE_VERSION = 9` (`MineSave.java:124`). Os outros 4 arquivos não
  gravam versão.
- CONFIRMADO: o relato "um `readNbt` que lança vira estado vazio gravado" já foi tratado no `ColonySavedData`. Cada
  entrada e cada seção passam por `readEntry`/`readSection`, que pegam `RuntimeException` e registram aviso
  (`ColonySaveReader.java:151-166`, AUD-02). Há `ColonySaveFuzzTest` (`@TestFactory`), e são 109 `@Test` em
  `src/test/java/com/villagecolony/data/save/`. Os outros leitores conferem `containsUuid` antes de `getUuid`
  (`WorkMarksSavedData.java:230,240`; `DemandRetrySave.java:64`). Isso importa porque, no 1.21.1, `getUuid` de uma
  chave ausente lança `NullPointerException` (`NbtHelper.toUuid`, fonte Yarn).
- PROVÁVEL: o arquivo inteiro corrompido continua sem proteção. O vanilla grava o `.dat` direto por cima, sem arquivo
  temporário e sem `_old` (`PersistentState.save` → `NbtIo.writeCompressed(Path)` com `Files.newOutputStream`, fonte
  Yarn). Num gzip truncado, `PersistentStateManager` registra "Error loading saved data" e devolve `null`
  (`PersistentStateManager.java:74-78`). O mod então cria um estado vazio, e o próximo `BEFORE_SAVE` chama `sync` →
  `markDirty()` e grava o vazio por cima. Somem as colônias, as casas protegidas, as minas e as profissões. Uma parte se
  refaz sozinha, porque a vila Vanilla é redetectada. O mod não guarda cópia de segurança.
- CONFIRMADO: a gravação é periódica, e não só no fechamento. `ServerLifecycleEvents.BEFORE_SAVE` copia o registro a
  cada autosave e a cada `/save-all` (`fabric/event/ServerLifecycleHandler.java:51,218-230`). A trava `stopping` impede
  gravar o vazio depois do fechamento, e há GameTest para isso (`SaveDuringPlayGameTest`: `anAutosaveCopiesTheColonyIntoTheSave`,
  `theSaveThatFollowsTheShutdownCopiesNothing`). Num crash, perde-se no máximo o que mudou desde o último autosave. O
  servidor dedicado de teste rodou `save-all` → "Saved the game" e `stop` → "Saved 0 colonies…" sem erro.
- CONFIRMADO: a recarga no meio de uma obra segue a regra "o mundo é a verdade". A obra grava identidade, planta,
  origem, estado e peças adiadas, sem o progresso (`ColonySavedData.sync`, javadoc). Ela renasce no primeiro ciclo
  (`ConstructionResume`; `SessionResumeTest` com 5 testes). A mina grava a boca e a fronteira.
- CONFIRMADO: a política da mina descarta progresso quando a forma muda. Se a `SHAPE_VERSION` gravada for diferente da
  atual, a fronteira é apagada e a mina recomeça (javadoc de `SaveMigration`, `MineSave.java:124`, já na versão 9).
  Numa 1.0 que promete "sem perda de save", cada mudança dessa constante faz a mina recomeçar a varredura de todos os
  saves atualizados.
- CONFIRMADO: nenhum teste lê um `.dat` gravado por uma versão publicada. Não há fixture `.dat` em `src/test` nem em
  `src/gametest` (`find src -name "*.dat"` só achou estruturas `.nbt`). O NBT antigo é montado à mão no teste
  (`SaveMigrationTest`). Na volta para uma versão mais velha (downgrade), `stamp` grava `CURRENT` por cima de um save
  futuro, e os campos novos se perdem sem aviso (`SaveMigration.java:83-85`).

Riscos para a 1.0: as colônias se perdem por completo depois de queda de energia ou crash durante a gravação. A mina
recomeça a cada mudança de forma. Uma atualização 0.4.x→1.0 nunca foi provada com o save real de uma versão publicada.

O que falta para 7/10 e para 9/10:
- 7/10: cópia de segurança do `villagecolony_colonies.dat` antes de gravar, ou leitura de recuperação que não grave o
  vazio quando o arquivo existia e falhou (M). Fixtures `.dat` de 2 ou 3 versões publicadas, lidas num teste de unidade
  (M). Versão de esquema nos outros 4 arquivos (P).
- 9/10: migração da fronteira da mina entre formas, em vez de descartá-la (G). Teste de "save truncado" e de downgrade
  (M). Gravação atômica própria (temporário + rename) para os 5 arquivos (M). Comando `/vc backup` ou backup rotativo (M).

Bloqueia a 1.0? **Sim, em parte.** A meta diz "sem perda de save". Só os dois itens de 7/10 de prioridade (backup ou
recuperação, e fixture de versão publicada) bloqueiam. O resto é melhoria.

---

### D4 — Desempenho (ISO 25010: eficiência de desempenho — comportamento no tempo, uso de recursos, capacidade)
Nota proposta: 5/10. A instrumentação é boa e a média é aceitável com 1 vila. O ciclo inteiro, porém, roda num tique só
a cada 30 s, passa de 50 ms com frequência e chegou a 458 ms com 4 ou mais vilas. Ninguém mediu multijogador nem
servidor com muitas vilas.

Evidências:
- CONFIRMADO (Spark, lido): no Spark `El42EuZ5w9` o TPS médio foi 19,68, com mínimo de 0,71 em três picos de 240–260 ms
  atribuídos a fora do mod. O mod pesa 2,4% da thread (`docs/audit/2026-11-13-jogo-0468.md:25-26`).
- CONFIRMADO (rodado): `cost_ledger.py` no `latest.log` (0.4.68, 1 colônia, 15 amostras) dá total médio de 18,7 ms, p95
  de 33,0 ms e máximo de 56,4 ms. No mesmo log há 18 avisos `Colony cycle took` acima de 50 ms, com máximo de 93 ms
  (`grep` no `latest.log`). O ciclo é de 600 tiques (`core/colony/service/VillageDetector.java:60`) e roda inteiro num
  tique só. Cada aviso desses é um tique acima do orçamento de 50 ms.
- CONFIRMADO (rodado): `cost_ledger.py` nas linhas `VC_COST` dos `.log.gz` de 07-11 a 11-11:

  | Colônias | Amostras | Média | p95 | Máximo |
  |---|---|---|---|---|
  | 1 | 233 | 31,1 ms | 75,2 ms | 126,8 ms |
  | 2-3 | 37 | 29,2 ms | 59,1 ms | 111,5 ms |
  | 4+ | 191 | 26,9 ms | 54,0 ms | **458,7 ms** |

  O máximo de 4+ colônias vem do `planner` (294,9 ms) e de `workers` (113,9 ms).
- SUSPEITO: as linhas `VC_DAY` do jogo da 0.4.68 registram `"mspt":35.2` e `"mspt":37.1`, para 1 vila com 17 aldeões
  (`ColonyState.java:79`, `getAverageNanosPerTick`). Isso é muito para um tique, mas o servidor era integrado (cliente
  junto) e ninguém atribuiu a causa.
- CONFIRMADO: há 25 classes com listener de tique (`END_SERVER_TICK`/`END_WORLD_TICK` etc.) e 272 coleções estáticas
  mutáveis em `fabric/`. 179 classes se inscrevem no `ServerMemory`, que limpa tudo ao abrir um mundo. Por amostra, os
  caches por posição têm limpeza ou validade (`ColonyTorches`, `BenchPresence` por TTL; `VillageGround.PLANS` como LRU
  com `synchronized`). Não auditei os 272 um a um.
- CONFIRMADO: há gravação em disco na thread do servidor. `SupplyGapLedger` e `UnreachablePieces` abrem e fecham o
  arquivo a cada linha (`Files.writeString … APPEND`, `SupplyGapLedger.java:85`, `UnreachablePieces.java:111`). O
  `ActionJournal` descarrega a cada 200 linhas (`ActionJournal.java:50,230`).
- CONFIRMADO: `OverlaySync` monta, a cada 20 tiques, uma lista com **todos** os trabalhadores e todas as obras abertas
  de **todas** as vilas, e a manda para cada jogador (`fabric/overlay/OverlaySync.java:26-44`). O custo e a banda
  crescem com jogadores × trabalhadores do servidor inteiro, sem recorte por distância.
- CONFIRMADO: o gancho de pathfinding sai cedo para quem não é aldeão da colônia (`ColonyPathing.forWalker`, linhas
  66-74; `PenFloors.forWalker`, linha 67). Ele não pesa nos outros mobs.
- NÃO CONFIRMADO: nunca foram medidos servidor dedicado com mais de 1 jogador, mais de 6 vilas, sessão longa (heap e
  crescimento dos estáticos ao longo de dias) nem TPS com vários jogadores em vilas diferentes.

Riscos para a 1.0: num servidor público com várias vilas carregadas, tiques de 100–450 ms a cada 30 s são "lag spikes"
visíveis. Somam-se o overlay global e a gravação síncrona em disco.

O que falta para 7/10 e para 9/10:
- 7/10: repartir o ciclo entre tiques (uma colônia ou fase por tique, ou orçamento de tempo com retomada) (G). Recortar
  o `OverlaySync` pela vila do jogador ou por distância (P). Tirar a gravação dos ledgers da thread do servidor, com
  fila e thread de escrita (P). Medir 1 servidor dedicado com 2 a 3 jogadores e 6 vilas por 1 h, com Spark e
  `cost_ledger` (M).
- 9/10: p95 do ciclo abaixo de 10 ms por tique em qualquer número de vilas (G). Teste de carga reproduzível no Sentinela
  (perfil "carga" com N vilas) (M). Perfil de heap de sessão longa (M).

Bloqueia a 1.0? **Sim.** A meta inclui servidor dedicado e multijogador, e picos de 458 ms já aparecem com 4 ou mais
vilas num jogo solo.

---

### D7 — Compatibilidade (ISO 25010: compatibilidade — coexistência, interoperabilidade; e portabilidade)
Nota proposta: 5/10. O servidor dedicado sobe e o lado cliente está bem separado. A coexistência com outros mods nunca
foi testada. Um gancho de geração de mundo usa estado sem trava em threads de trabalho. E o README promete algo que
provavelmente é falso.

Evidências:
- CONFIRMADO (rodado): o servidor dedicado sobe com o mod. O log mostra `Env=SERVER`, `villagecolony 0.4.74`,
  `[Village Colony] Mod initialized`, `Done (9.641s)!`, `Saved the game` depois de `save-all`, e no `stop` a linha
  `Saved 0 colonies with 0 workers…`, sem `ERROR` do mod e sem falha de mixin. Limite: não havia jogador, então a
  detecção de vila, o ciclo e os pacotes não foram exercitados.
- CONFIRMADO: os GameTests rodam com `server()` (`build.gradle`, `loom.runs.gametest`), num ambiente de servidor
  dedicado. A bateria de cerca de 1.100 GameTests é prova indireta de que o código comum não depende do cliente. O
  `biomeSim` também é um servidor sem jogador, e subiu em 29-10 (`run/biome-sim/logs/latest.log`: `Done (5.452s)`).
- CONFIRMADO: a separação cliente/servidor está limpa. Nenhum arquivo de `core/`, `data/`, `fabric/` ou
  `VillageColonyMod.java` importa `com.villagecolony.client`, `net.minecraft.client` ou `MinecraftClient` (`grep`). O
  mixin do cliente está num config com `"environment": "client"`. O `fabric.mod.json` usa `"environment": "*"`, e os
  entrypoints `client` e `modmenu` ficam no pacote `client`.
- PROVÁVEL: a frase do README "clientes que entram em um servidor dedicado não precisam instalá-lo" (`README.md:656-657`)
  deve ser falsa. O mod registra blocos, itens e POI próprios (`fabric/content/ModContent.java:175-182`). Com isso, a
  sincronização de registros da Fabric API recusa um cliente sem o mod. Não testei com cliente vanilla.
- PROVÁVEL: há risco de thread na geração de mundo. `HostileSpawnMixin` (`fabric/mixin/HostileSpawnMixin.java:28`)
  chama `ColonyTorches.suppresses`, que trata também `SpawnReason.CHUNK_GENERATION`
  (`fabric/integration/ColonyTorches.java:84-104`). Nessa chamada há um `HashMap` estático sem trava (`NEAR`), leitura
  do registro `MINES` e consultas a `PointOfInterestStorage` (`BenchPresence.of`, `anyWithin`). O log do servidor de
  teste mostra a geração rodando em `Worker-Main-N`, e `ServerChunkLoadingManager.generate` chama `step.run` nessas
  threads (fonte Yarn, linhas 628-629). O `VillageGround` se protege com `synchronized` (`VillageGround.java:172,465`).
  O `ColonyTorches` não. O efeito (corrida, `ConcurrentModificationException`, carga de POI fora da thread principal)
  não foi reproduzido.
- CONFIRMADO: o mod tem 7 mixins comuns e 1 de cliente (`villagecolony.mixins.json`), todos com `defaultRequire: 1`. Não
  há `@Overwrite`, e a ADR-004 rege a política. Os alvos são sensíveis a conflito: `LandPathNodeMaker.getNodeType`,
  `getJumpOnTopNode` e `getStepHeight` (área que o Lithium otimiza); `ChunkGenerator.generateFeatures` e
  `addStructureReferences` (mods de geração e C2ME); e `VillagerEntity.initBrain`, `setVillagerData` e
  `MoveToTargetTask.hasFinishedPath` (mods de aldeão: MCA, Guard Villagers, Easy Villagers). Com `require 1`, um alvo
  alterado por outro mod derruba a carga. Ninguém testou nenhuma combinação.
- CONFIRMADO: as dependências estão fixadas (`fabricloader >=0.16.0`, `fabric-api >=0.116.15`, `minecraft ~1.21.1`,
  `java >=21`; `gradle.properties`). Não há `breaks`/`conflicts`, e `modmenu` aparece só em `suggests`.
- CONFIRMADO (multijogador): a política de profissão é global e só muda com op nível 2
  (`fabric/network/ProfessionPolicyNetworking.java:13,33`). A detecção corre em volta de todos os jogadores
  (`VillageDetectionHandler.java:402`). O `RoadMapSync` guarda quem observa, por jogador, e limpa quem saiu
  (`RoadMapSync.java:62`, `removeIf`). O overlay é global (ver D4). Nunca houve teste com 2 ou mais jogadores.

Riscos para a 1.0: crash ou corrupção rara na geração de mundo em servidor, pior com C2ME. Pacotes de mods comuns
(Lithium e mods de aldeão) podem impedir a carga. Quem ler o README e entrar sem o mod será recusado.

O que falta para 7/10 e para 9/10:
- 7/10: tirar `CHUNK_GENERATION` do `ColonyTorches` ou torná-lo seguro para threads (P). Corrigir o README sobre o
  cliente (P). Fazer uma sessão real de servidor dedicado com 2 clientes por 30 min (M). Fazer uma matriz mínima de
  compatibilidade com Lithium, FerriteCore, ModernFix e um mod de aldeão, ao menos subindo o jogo e entrando num mundo
  (M).
- 9/10: CI que sobe o servidor dedicado com 1 jogador falso por N minutos (M). Teste de carga com Lithium e C2ME (M).
  Declarar `breaks` para as incompatibilidades conhecidas (P). Revisar `require` por injeção, para que o opcional não
  derrube a carga (P).

Bloqueia a 1.0? **Sim.** A meta é servidor dedicado e multijogador. O risco de thread na geração e a frase falsa do
README têm conserto pequeno, mas são obrigatórios. A matriz mínima de mods é necessária para a Modrinth e a CurseForge.

---

### D9 — Segurança (ISO 25010: segurança — confidencialidade, integridade, responsabilização; e resistência a abuso)
Nota proposta: 6/10. Não há rede própria nem execução externa, e a escrita no mundo por comando e por pacote exige op.
Dois pontos pesam num servidor público: arquivos de diagnóstico crescem sem teto e comandos sem op escrevem em disco. Há
um terceiro: a proteção de construções do jogador é uma heurística.

Evidências:
- CONFIRMADO: o `SECURITY.md` documenta a superfície. Não há HTTP nem socket, não há `Runtime.exec`/`ProcessBuilder`, e
  a tabela dos arquivos gravados existe. O `grep` por `System.getProperty`/`getenv` acha só 3 chaves de desenvolvimento.
- CONFIRMADO: o pacote C2S é validado. `ProfessionPolicyUpdatePayload` só se aplica com `hasPermissionLevel(2)`
  (`ProfessionPolicyNetworking.java:33-38`). O codec confere a contagem esperada e limita a string a 32 caracteres
  (`ProfessionPolicyPayloadCodec.java:35,62`). `/vc mae …`, que pede obra no terreno, exige op 2
  (`fabric/command/MaeCommand.java:56`).
- CONFIRMADO: `/vc bioma`, `/vc bioma nota <texto>`, `/vc dados` e `/vc log` não exigem permissão
  (`BiomeCheck.java:63-69`, `VillageLogCommand.java:37-38`). Com `nota`, qualquer jogador grava texto livre
  (`greedyString`) no `latest.log` e no `analytics/<data>.jsonl` do mundo. Com `dados`, qualquer jogador grava um retrato
  em disco. O freio é só o anti-spam de comandos do vanilla.
- CONFIRMADO: os arquivos crescem sem teto entre sessões. O diário `logs/villagecolony-actions-<sessão>.jsonl` abre um
  arquivo por sessão e nunca apaga. Na máquina do autor são 39 arquivos, 31,2 MB, até 3,9 MB cada (`ls`). Só se desliga
  com a propriedade de JVM `-Dvillagecolony.actions=false` (`ActionJournal.java:70`). `analytics/<data>.jsonl` para em
  32 MB por arquivo, mas há um arquivo por dia e nenhum é apagado (`AnalyticsLog.java:32,79,84`). `supply-gaps.jsonl` e
  `unreachable-pieces.jsonl` crescem para sempre na pasta do jogo (hoje 304 KB e 25 KB). Fora isso, há cerca de 330
  chamadas `LOGGER.info`, com linhas `VC_*` em INFO sempre ligadas (1,4 MB de `latest.log` em ~2 h).
- PROVÁVEL: a escrita no mundo fora das regras do jogador depende de heurística. O `BlockProtection` admite que só
  reconhece bloco do jogador pela folha `persistent` (`fabric/integration/BlockProtection.java`, javadoc e `mayBreak`).
  A proteção real é "só quebrar o que se prova floresta". Mas mineiro, nivelamento (corte e aterro até 4, Regra 242) e
  obra mexem no terreno, e o mod não consulta `doMobGriefing` (`grep GameRules` → só `RANDOM_TICK_SPEED`). Também não
  há configuração para desligar a modelagem do solo na geração (`ChunkGeneratorMixin`). O README avisa para usar um
  mundo de teste.
- CONFIRMADO: os registros não guardam dado pessoal, só coordenadas e UUID de aldeão (`SECURITY.md`; formato das linhas
  em `SupplyGapLedger.java:73-81`). A mudança de política registra o nome do op (`ProfessionPolicyNetworking.java:42`),
  o que é bom para responsabilização.

Riscos para a 1.0: num servidor público, um jogador sem op pode encher o log e a pasta do mundo. Dedicado 24/7 acumula
diário sem fim. Construções de jogador perto da vila podem ser cortadas pelo nivelamento ou pela mina.

O que falta para 7/10 e para 9/10:
- 7/10: exigir permissão (op 2, ou configurável) em `/vc bioma nota` e `/vc dados`, e limitar o tamanho do texto (P).
  Rotação ou teto total do diário e dos ledgers (manter N arquivos ou X MB) (P). Opção de configuração para desligar o
  diário e as linhas `VC_*` em produção (P).
- 9/10: respeitar `doMobGriefing`, ou criar uma gamerule própria para o trabalho que altera terreno (M). Proteção opcional
  de região do jogador (raio em volta da cama ou do spawn, ou marcação) (M). Arquivo de configuração de servidor
  documentado (M).

Bloqueia a 1.0? **Não por si**, mas os três itens de 7/10 (P) devem entrar antes de anunciar suporte a servidor
público.

---

## Top 5 problemas do bloco (por gravidade)

1. **O ciclo da colônia roda inteiro num tique** (D4). Picos de 57–93 ms com 1 vila (18 avisos no último jogo), p95 de
   75 ms com 1 vila e máximo de 458 ms com 4 ou mais vilas (`cost_ledger`, logs de 07 a 11-11). Num servidor
   multijogador é lag visível a cada 30 s. Conserto G.
2. **Estado sem trava acessado da geração de mundo** (D7). `HostileSpawnMixin` → `ColonyTorches.suppresses` em
   `CHUNK_GENERATION`, com `HashMap` estático, registro `MINES` e consulta de POI em `Worker-Main` (PROVÁVEL). O risco é
   crash raro ou corrupção, pior com C2ME. Conserto P.
3. **Perda total de save com arquivo truncado** (D3). Escrita vanilla não atômica, sem backup. Na falha de leitura, o
   mod grava o estado vazio por cima no autosave seguinte. A mina descarta a fronteira a cada `SHAPE_VERSION` (já na
   9). Nenhum teste com `.dat` de versão publicada. Conserto M.
4. **Compatibilidade não verificada e uma promessa errada** (D7). O README diz que o cliente não precisa do mod, mas há
   blocos, itens e POI registrados (PROVÁVEL que o cliente seja recusado). São 7 mixins `require 1` em pathfinding,
   geração e cérebro do aldeão, sem nenhum teste com Lithium, C2ME ou mods de aldeão. Conserto P a M.
5. **Servidor público: escrita em disco sem op e sem teto** (D9/D4). `/vc bioma nota` e `/vc dados` sem permissão.
   Diário, analytics e ledgers sem rotação (31 MB só de diário na máquina do autor). E/S síncrona na thread do servidor.
   Overlay global mandado a todos os jogadores a cada segundo. Conserto P.

## O que eu não consegui verificar

- **Multijogador real:** 2 ou mais clientes no servidor dedicado, overlay e política com vários jogadores. O servidor
  de teste subiu sem nenhum jogador, então detecção, ciclo, pacotes e o salvamento de uma colônia de verdade não rodaram
  no dedicado.
- **Cliente vanilla entrando no servidor com o mod:** a recusa pela sincronização de registros é dedução, não teste.
- **Coexistência com outros mods** (Lithium, C2ME, FerriteCore, MCA, Guard Villagers): nada foi testado.
- **Efeito real da corrida em `ColonyTorches`:** o mecanismo foi visto (`generate` em `Worker-Main`, `HashMap` sem
  trava). Não confirmei que `canSpawnInDark` com `CHUNK_GENERATION` cai nessas threads, nem reproduzi falha.
- **Crash no meio da gravação e save truncado:** só raciocínio sobre o código vanilla e o do mod. Não simulei.
- **Spark `El42EuZ5w9`:** não baixei nem decodifiquei. Usei os números de `docs/audit/2026-11-13-jogo-0468.md`.
- **Heap e crescimento dos 272 estáticos numa sessão longa:** não medi. Só amostrei 6 caches.
- **0.4.69 a 0.4.74 em jogo:** nenhuma foi jogada. Os números de desempenho são da 0.4.68 e anteriores.
- **Testes:** não rodei `test`, `runGametest` nem o Sentinela (pedido do brief). As contagens de testes citadas vêm de
  `grep` e do `STATE.md`.
