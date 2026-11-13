# Avaliação — Bloco B: testes, ferramentas, build e CI

Snapshot: `main` em `ac2f1977` (0.4.74), worktree isolada `agent-ab2891e5f6c9f4ec6`. Data: 2026-11-13.
Só diagnóstico: nada do repositório foi editado nem commitado. `docs/testing/regras-e-testes.md` **não** foi regravado
(`rule_coverage.py` rodou com `--check` e por importação). Saídas brutas e scripts de leitura ficaram no scratchpad da
sessão (`cov.py`, `pit.py`, `hist.py`, `q/` = artefatos do CI).

## O que rodou de verdade nesta sessão

| Verificação | Comando | Resultado |
|---|---|---|
| Sentinela `fast` | `python scripts/test_lab.py fast` (12:34–12:42, 7 min 24 s) | **PASS 3/3** — run `20261113T153454Z`, commit `ac2f1977` |
| Unitários | suite `unit` (230,9 s com compilação) | 1.804 casos, 0 falha, 0 ignorado |
| Propriedade | suite `property` (9,3 s) | **1** caso jqwik, verde |
| GameTest | suite `gametest` (184,3 s, `--rerun-tasks`) | 1.112 casos, 0 falha (auditoria de suprimento fica de fora por padrão) |
| Repetição GameTest | `python scripts/test_lab.py fast --only gametest --repeat 2` (12:43–12:49) | **FAIL 2/2** — run `20261113T154321Z`: tentativa 1 caiu `ShepherdPensGameTest.eachSpeciesGetsItsOwnPen` ("Didn't succeed or fail within 40 ticks"); tentativa 2 caiu `BiomeBlueprintsGameTest.theTaigaLargePenHasSpruceFencesAndTheOakOneStillStands` ("o curral grande da taiga não tem cerca de pinheiro") |
| Análise | `python scripts/sentinel_analysis.py` | 1ª: 0 achados; 2ª (2 execuções): 2 achados TEST, os dois acima |
| Histórico | `sentinel_analysis.py --runs-root <main>/reports/test-runs` + `hist.py` | 85 execuções, 83 baterias inteiras |
| Python | `python -m unittest discover -s tests` | 167 testes, OK (2,8 s) |
| Regras | `python scripts/rule_coverage.py --check` | 268 regras; "sem teste": 7 `[33, 47, 201, 202, 203, 204, 205]` |
| Cobertura e PIT | artefato `quality-reports-43438557019` do CI (push na `main`, `ac2f1977`) + JaCoCo da bateria desta sessão | ver D5 |

Não rodei `pitest` localmente (17 min no CI; usei o relatório do CI do mesmo commit). Não rodei o perfil `full`.

---

### D5 — Testes (ISO 25010: confiabilidade › maturidade; manutenibilidade › testabilidade)
Nota proposta: **6/10** — bateria grande, registrada por inteiro, com mutação no core e cobertura real da camada fabric
pela bateria; mas no mesmo commit só 1 de 3 baterias saiu verde (instáveis que mudam de nome a cada rodada), há 1
propriedade jqwik no projeto, nenhum teste de recarga real de save nem de escala/multijogador, e um terço das regras
provadas só "pela classe".

Inventário (lido no código; contagens por `grep`, execução confirmada pelo Sentinela):

| Tipo | Arquivos | Casos | Onde |
|---|---|---|---|
| Unitário JUnit 5 (+ fabric-loader-junit) | 257 | 1.790 `@Test` → **1.804 executados** | core/construction 397, fabric/work 322, core/coordination 302, core/colony 169, core/worker 138, data/save 108, fabric/integration 66, core/resource 63, … |
| ArchUnit / regras de arquitetura | 5 classes em `architecture/` | 18 | ciclos, fronteira core/fabric, conversão, tamanho de arquivo, registro de GameTest |
| Propriedade jqwik | **1** (`HiringProperties`) | **1** `@Property(tries = 300)` | só contratação |
| GameTest Fabric | 277 (272 com `@GameTest`) | **1.113** (1.112 na bateria comum + auditoria) | Miner 106, Village 68, Build 63, Builder 60, Lumberjack 48, Mine 39, Chest 38, Road 37, Mae 28, … |
| Python (scripts) | 22 | 167 | 23 de 41 scripts têm teste |

Evidências:
- **CONFIRMADO (rodado)** — Sentinela `fast` verde de primeira em `ac2f1977`: 1.804 unitários, 1 propriedade, 1.112
  GameTests, 0 falha (`reports/test-runs/20261113T153454Z/summary.md`; contagem lida dos XML dos artefatos).
- **CONFIRMADO (rodado)** — Repetindo a bateria duas vezes no mesmo commit, **as duas caíram**, cada uma num teste
  diferente e nenhum dos dois instáveis "conhecidos" do brief: `eachSpeciesGetsItsOwnPen` (prazo de 40 tiques; já no
  `TODO.md:148` e `:195`) e `theTaigaLargePenHasSpruceFencesAndTheOakOneStillStands`
  (`BiomeBlueprintsGameTest.java:81`, criado em `306f031c`, 12-11; **não registrado** em `TODO.md` nem em
  `known-failures.md`). Placar no commit atual: **1 verde em 3 baterias** (33 %). Mecanismo do teste da taiga: SUSPEITO —
  o cenário põe o bioma por `village(context, center, "minecraft:taiga")`, e a memória do projeto já registrou que o
  `fillbiome` em GameTest volta sem chunk forçado; não reproduzi isolado.
- **CONFIRMADO (lido)** — Registro dos GameTests completo: 272 classes com `@GameTest`, 272 entradas em
  `src/gametest/resources/fabric.mod.json`, 0 fora e 0 sobrando (script de comparação nesta sessão). O
  `GameTestRegistryTest` reprova classe fora do registro; nenhum `required = false`, `@Disabled`, `maxAttempts` ou
  `requiredSuccesses` no projeto (`grep` = 0) — instável não é escondido por repetição automática.
- **CONFIRMADO (rodado, CI do mesmo commit)** — PIT no core: 4.059 mutantes, 3.458 mortos, **85,2 %** (limiar 85, folga
  de ~7 mutantes); 463 sobreviventes + 138 sem cobertura; força 88 % (log do run 43438557019: "Generated 4059 mutations
  Killed 3458 (85%)"). Sobreviventes concentrados em `core.construction.model` (306 + 62 sem cobertura): `MineShaft` 35,
  `RoadRouting` 28, `RoadLines` 26, `VillageSurface` 24, `RoadThinning` 24, `VillageBounds` 24, `RoadLinks` 22,
  `VillageBlend` 21, `ConstructionService` 14 — exatamente mina, grafo de ruas e caixa da vila, os sistemas dos últimos
  playtests. PIT **não muta a camada fabric** (≈ 80 % do código: `fabric/work` 49,6 mil linhas, `fabric/integration` 28 mil).
- **CONFIRMADO (rodado)** — Cobertura de linhas por pacote, `ac2f1977` (JaCoCo; unitário = CI, bateria = esta sessão):
  total unitário 28,5 % linhas / 21,5 % ramos; **bateria 79,0 % linhas / 66,6 % ramos**. `fabric/work` 12,8 % / 81,5 %;
  `fabric/integration` 8,8 % / 83,8 %; `core/*` 94–100 % no unitário; `data/save` 91,0 % / 54,2 %;
  `fabric/command` 31,4 % / 31,9 %; `client` 22,9 % / 0 %; `client/modmenu` 0 / 0; `fabric/mixin` 0 / 0 (JaCoCo não vê
  classe de mixin — esperado, mas nada mede os 8 mixins). Pacotes < 60 % na melhor das duas: command, client, modmenu,
  network (67/46), storage (core 95 % unit).
- **CONFIRMADO (histórico lido)** — Instabilidade: das 83 baterias inteiras salvas em `<main>/reports/test-runs`
  (05-11 a 13-11), só **42 (51 %)** saíram sem nenhuma falha. `workerCollectsWhileWalkingPast` caiu em **19/83 (23 %)**,
  `animalsWalkInThroughEachOfTheFourGates` em **10/83 (12 %)**; outras 24 falhas isoladas misturam instáveis e vermelho
  legítimo de TDD (não dá para separar só pelo XML). No CI, 3 de ~10 pushes na `main` de 11 a 13-11 caíram por GameTest
  instável: `animalsWalkInThroughEachOfTheFourGates` (run 43058330427), `workerCollectsWhileWalkingPast` (43140845667) e
  **`LumberjackGameTest.theStallGuardReturnsTheTaskAndForgetsTheTree` (43301842040)** — este é o KF-004
  (`docs/behavioral-tests/known-failures.md:105`), não está na lista de instáveis conhecidos do brief.
- **CONFIRMADO (lido)** — Teste de propriedade praticamente ausente: 1 propriedade no projeto inteiro
  (`src/test/java/com/villagecolony/core/worker/service/HiringProperties.java:83`), embora o build tenha tarefa própria
  (`propertyTest`) e o comentário do build diga que é "teste de propriedade com estado". Planejador de lote, grafo de
  ruas, reserva de mina e save — onde os sobreviventes do PIT estão — não têm propriedade.
- **CONFIRMADO (rodado)** — Prova das regras (`rule_coverage.py`, importado sem escrever): 268 linhas — **161 citam a
  regra**, **94 só usam a classe** indicada (grau fraco: "o teste toca a classe"), 7 "sem teste", 6 fora de vigor. Dos 7,
  cinco (201–205) são **regras revogadas** que o script não reconhece: a linha tem só 2 células e o código lê o estado em
  `cells[-2]` só quando há ≥ 3 (`scripts/rule_coverage.py:74`) — defeito do script, que não tem teste. As duas restantes
  são de processo (33, JAR local) e de Vanilla (47, aldeão não envelhece). Regra 52 não existe na tabela.
- **CONFIRMADO (lido)** — Save: 14 classes unitárias em `data/save` (108 casos), com migração idempotente,
  `BREEDER→SHEPHERD`, versão futura intocada e fuzz de NBT (`ColonySaveFuzzTest`); 2 GameTests de salvamento
  (`SaveDuringPlayGameTest`: autosave copia a colônia; o save pós-`SERVER_STOPPING` não grava vazio). **Não há** fixture de
  save de versão publicada (`src/test/resources` só tem arquitetura e um pino de geometria) nem teste que pare e reabra o
  servidor e confira o mundo (o próprio teste diz "o mundo não é gravado em disco").

Lacunas para a 1.0 (todas lidas, nenhuma reproduzida):
- **Recarga de save:** sem teste de ida e volta servidor→disco→servidor, e sem save dourado de cada versão pública
  (0.4.x → 1.0). PROVÁVEL risco de perda silenciosa (o padrão "PersistentState engole exceção" já apareceu antes).
- **Servidor dedicado:** a bateria roda em `server()` sem cliente (`build.gradle`, `loom.runs.gametest`), então carga de
  classe do lado servidor está coberta de fato. Cliente (`client/*`, overlay, rede cliente) tem 0 % na bateria e 23 % no
  unitário; nenhum teste com dois jogadores reais. Pacotes de rede: 9 unitários.
- **Duas colônias:** há casos pontuais (baú de outra colônia, semente em baú de outra colônia; 15 arquivos citam outra
  colônia), mas a interferência entre arenas é a causa histórica dos KF-006 a KF-010 — sem cenário dedicado de duas vilas
  vizinhas disputando mina, lote e baús.
- **Vila grande / escala:** só testes de orçamento local (`aPassLooksAtMostAFewStonesSoTheTickStaysCheap`,
  `aWholeLevelDigsAtMostEighteenHundredBlocks`, terreno "em poucas centenas de ms"). Nenhum teste de carga com dezenas de
  aldeões ou várias colônias medindo ms/tique; o `cost_ledger.py` só vale com playtest.
- **Teste que copia o predicado:** SUSPEITO, baixo. 16 arquivos de GameTest têm predicado local `private static boolean
  is…`; os 12 amostrados são observação do mundo (`isOpen`, `isInside`, `isHome`), não cópia da regra. Não auditei os 1.113.

Riscos para a 1.0:
- Instáveis derrubam metade ou mais das baterias locais (2 de 3 hoje; 41 de 83 no histórico) e ~1 em 3 pushes na `main`:
  o CI vermelho deixa de ser sinal e o autor se acostuma a "caiu só o instável" — e o instável muda de nome a cada rodada.
- Mudança de formato de save entre versões públicas sem teste de recarga nem fixture.
- PIT no limiar (85,2 % contra 85): a próxima feature do core pode reprovar o CI por 8 mutantes, ou o limiar é baixado.
- 94 regras provadas só por "usa a classe"; a régua `rule_coverage` mente por 5 revogadas.

O que falta para 7/10 e para 9/10:
- Para 7: bateria verde em ≥ 9 de 10 rodadas no mesmo commit — corrigir ou quarentenar com registro os cinco instáveis
  vistos (`workerCollectsWhileWalkingPast`, `animalsWalkInThroughEachOfTheFourGates`, `theStallGuardReturnsTheTask…`,
  `eachSpeciesGetsItsOwnPen`, `theTaigaLargePen…`) (M); arrumar `rule_coverage.py` para linhas de 2 células e dar-lhe
  teste (P); 5–10 propriedades nos pontos de maior sobrevivente do PIT (M).
- Para 9: save dourado por versão pública + GameTest de parar/reabrir servidor (M–G); 10–20 propriedades jqwik nos
  sistemas com mais sobreviventes — grafo de ruas, `MineShaft`, `VillageBounds`, planejador de lote (M); PIT com folga
  ≥ 88 % e extensão a 1–2 pacotes puros de `fabric/work` (M–G); cenário de duas colônias vizinhas e cenário de vila grande
  com teto de custo por tique (M); subir as 94 regras "classe" para "cita" nas regras de jogo ativas (M); teste do overlay
  e da rede cliente (P–M); bateria 3× verde seguida como critério de release (P).

Bloqueia a 1.0? **Não por si só**, mas a ausência de teste de recarga de save **bloqueia junto com D3**: a meta "sem
perda de save" não tem prova automatizada. Os instáveis não bloqueiam se forem quarentenados e registrados.

---

### D10 — Ferramentas, build e CI (ISO 25010: manutenibilidade › testabilidade e analisabilidade; confiabilidade › maturidade)
Nota proposta: **7/10** — cadeia reprodutível e travada (versões fixas, verificação de dependências, ações por SHA,
Error Prone `-Werror`, PIT com limiar, sem `continue-on-error`), Sentinela local com evidência e análise; perde por CI
lento (≈ 21 min, 80 % no PIT), relatório de cobertura da bateria corrompido no CI, um passo do CI que não verifica nada,
NullAway e PIT só no core e release que compila JAR sem testar.

Evidências:
- **CONFIRMADO (lido)** — Versões fixas: `gradle.properties` (Minecraft 1.21.1, Yarn `1.21.1+build.3`, Loader 0.19.3,
  Fabric API `0.116.15+1.21.1`, JUnit 5.12.2, ArchUnit 1.5.1, jqwik 1.9.3, NullAway 0.14.2, PMD 7.28.0); plugins com
  versão exata (Loom 1.17.18, PIT 1.19.0, errorprone 5.1.1, `error_prone_core:2.50.0`); wrapper Gradle 9.6.1 com
  `distributionSha256Sum`; `gradle/verification-metadata.xml` com 305 componentes e `verify-metadata=true` (assinaturas
  desligadas; 3 exceções com motivo escrito: Fabric API jar, Minecraft e mod remapeado). Todas as Actions fixadas por SHA
  de commit. CI do Python com `nbtlib==2.0.4 pillow==12.2.0`. Nenhum `latest`, `+` ou SNAPSHOT achado.
- **CONFIRMADO (lido)** — `.github/workflows/ci.yml`: `git diff --check` → testes Python → `./gradlew build
  --rerun-tasks` (unitários, propriedade, ArchUnit, Error Prone + NullAway `-Werror`, PMD só relatório) →
  `runGametest --rerun-tasks` → auditoria de dados → `pitest` (limiar 85) → artefatos. **Nenhum `continue-on-error`**
  (`grep` = 0); o comentário registra que ele já escondeu o PIT parado de `78e7efc` a `7619b1d`. Push só na `main`; PR roda
  tudo; `concurrency` cancela o run antigo.
- **CONFIRMADO (rodado, `gh run view`)** — Tempo do CI no run da `main` 43438557019 (`ac2f1977`): build+unitários 1 min
  50 s, GameTest 1 min 37 s, auditoria 25 s, **PIT 16 min 51 s**; total ≈ 21 min. PRs recentes: 18 a 68 min de relógio
  (com fila). Localmente: Sentinela `fast` 7 min 24 s (unit 231 s, property 9 s, gametest 184 s).
- **CONFIRMADO (rodado/comparado)** — **Relatório de cobertura da bateria no CI está errado:** o artefato
  `quality-reports` do run 43438557019 dá 7,3 % de linhas para a bateria (fabric/work 4,8 %), contra 79,0 % na bateria
  inteira desta sessão no mesmo commit. O padrão (core/type 73 %, fabric/content 42 %) casa com só a auditoria de
  suprimento: o passo `runGametest -PgametestAudit=only` roda depois da bateria e regrava
  `reports/jacoco/gametest`. PROVÁVEL quanto ao mecanismo exato (sobrescrita do `.exec`/relatório), CONFIRMADO quanto ao
  número publicado estar errado.
- **CONFIRMADO (lido)** — O passo "Check whitespace" (`git diff --check`, `ci.yml`) compara a árvore com o índice; num
  checkout limpo do CI não há diferença, então o passo **nunca reprova nada** (não reproduzi com espaço sobrando).
- **CONFIRMADO (lido)** — Escopo da análise estática: Error Prone em `compileJava` com `-Werror`; NullAway só nos 23
  pacotes `@NullMarked` — **todos do core**; nenhum pacote `fabric/*` (≈ 80 % do código). PMD só complexidade cognitiva
  (nível 15), `ignoreFailures = true`. SpotBugs/Checkstyle não aplicados pelo build (há `build/reports/spotbugs` na pasta
  principal, de rodada avulsa). ArchUnit com violações congeladas em `src/test/resources/archunit_store`.
- **CONFIRMADO (lido)** — `release.yml` compila só `remapJar`, sem testes, e diz isso nas notas ("os testes não rodaram
  nesta compilação"); o JAR publicado tem outro SHA-256 que o testado. A prova do release depende de o CI do mesmo commit
  ter passado — não há `needs`/checagem de status no workflow.
- **CONFIRMADO (rodado)** — Sentinela: `test_lab.py` guarda log, XML, JaCoCo, métricas de CPU/RAM e commit por execução;
  marca falha que passa ao repetir como UNSTABLE; `sentinel_analysis.py` classifica achados (85 execuções lidas: 25
  achados, inclusive 23 "EVIDENCE_GAP" de suites sem XML copiado). Ferramentas de diagnóstico de jogo (`time_ledger`,
  `action_report`, `cost_ledger`, `save_read`…) existem; **18 de 41 scripts sem teste**, entre eles `rule_coverage`,
  `release_workflow` e `release_manifest`, que alimentam documentação e release.

Riscos para a 1.0:
- CI de ~21 min por PR, dominado pelo PIT: ciclo lento e tentação de pular.
- Cobertura da bateria publicada pelo CI é falsa (7 %); quem ler o artefato decide errado.
- Release sem amarração ao CI verde do commit; JAR publicado ≠ JAR testado.
- NullAway fora da camada fabric, onde estão os NPE de jogo.
- Build e testes só exercitados em Windows (local) e Ubuntu (CI) com Temurin 21; sem matriz, aceitável para mod.

O que falta para 7/10 e para 9/10:
- Para manter 7: separar o relatório JaCoCo da auditoria do da bateria (P); trocar o "Check whitespace" por
  `git diff --check <base>...HEAD` ou `git ls-files | xargs` equivalente (P).
- Para 9: release exige CI verde do mesmo SHA e anexa os relatórios (P–M); PIT incremental no PR e completo só na `main`
  ou por agendamento, para o PR cair abaixo de 10 min (M); NullAway pacote a pacote na camada fabric (G); bateria GameTest
  repetida 2–3× no CI noturno com quarentena declarada dos instáveis (M); testes para `rule_coverage`, `release_*` e
  `gametest_*` (P–M); publicação reprodutível (Modrinth/CurseForge via workflow, com changelog e SHA) (M).

Bloqueia a 1.0? **Não.** A cadeia é reprodutível e reprova de verdade; os defeitos são de sinal (cobertura falsa,
whitespace inócuo) e de tempo, e o elo release↔CI verde é pequeno de fechar — mas deve ser fechado antes de publicar.

---

## Top 5 problemas do bloco (por gravidade)

1. **Sem teste de recarga de save nem fixture por versão pública** (D5, ligado a D3). A meta "sem perda de save" da 1.0
   não tem prova automatizada; a unidade cobre NBT, não o ciclo servidor→disco→servidor.
2. **Instáveis crônicos derrubam a bateria** (D5/D10): nesta sessão, 2 de 3 baterias no mesmo commit caíram, por dois
   testes que não estão na lista de "conhecidos" (um deles, o da taiga, nem registrado). No histórico, 49 % das 83
   baterias locais com ≥ 1 falha; `workerCollectsWhileWalkingPast` 23 %, `animalsWalkInThroughEachOfTheFourGates` 12 %, e
   o KF-004 (`theStallGuardReturnsTheTaskAndForgetsTheTree`) derrubou a `main` no CI. 3 de ~10 pushes da `main` em 11–13/11.
3. **Cobertura da bateria publicada pelo CI é falsa** (D10): 7,3 % no artefato contra 79,0 % real — a auditoria roda
   depois e sobrescreve o relatório.
4. **Mutação e nulidade só no core** (D5/D10): PIT a 85,2 % (folga de ~7 mutantes) e sobreviventes concentrados em mina,
   grafo e caixa; NullAway e PIT não tocam `fabric/*`, ≈ 80 % do código. Propriedade jqwik: 1 no projeto.
5. **Régua de prova das regras fraca e com defeito** (D5): 94 de 262 regras em vigor provadas só por "usa a classe"; o
   `rule_coverage.py` (sem teste) conta 5 revogadas como "sem teste".

Menores: release sem amarração ao CI verde; passo "Check whitespace" inócuo; CI ~21 min (80 % PIT); 18 scripts sem teste.

## O que eu não consegui verificar

- **PIT local:** não rodei; usei o `mutations.xml` do CI do mesmo commit (`ac2f1977`, run 43438557019, 13-11 14:21 UTC).
- **Perfil `full`** (auditoria + PIT) não rodou localmente; a auditoria de dados só vi verde no CI.
- **Mecanismo exato** da cobertura falsa no CI (sobrescrita do `.exec` ou só do relatório): inferido pelo padrão dos
  números, não reproduzido.
- **Os dois que caíram na repetição** (`eachSpeciesGetsItsOwnPen`, `theTaigaLargePen…`) não foram rodados isolados; a
  causa do da taiga (bioma que volta) é suspeita, não medida.
- **Separar instável de vermelho de TDD** nas 24 falhas isoladas do histórico: o XML não diz se a falha era esperada.
- **"Check whitespace" inócuo:** conclusão por leitura da semântica do `git diff --check`; não forcei um espaço no CI.
- **Teste que copia o predicado:** amostrei 12 de 16 predicados locais; não li os 1.113 GameTests.
- **Servidor dedicado de verdade** (JAR remapeado num servidor sem dev env) e **multijogador**: nada roda isso; fica com o
  Bloco C.
- **Reprodutibilidade bit a bit do JAR:** não comparei dois builds (memória do projeto diz que só CRLF muda o MD5).
- Minecraft: o launcher do TLauncher estava aberto (processo `java.exe` do `.tlauncher\starter`), **o jogo não**; a
  bateria rodou mesmo assim e passou — não afetou o servidor de teste.
