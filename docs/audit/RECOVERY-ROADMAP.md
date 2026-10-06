# Roteiro de recuperação — Village Colony

**Data:** 2026-10-06 · **Snapshot:** `main` `6022021e` · parte da
[auditoria de recuperação](PROJECT-RECOVERY-AUDIT.md).
Nada aqui foi executado. Cada item espera aprovação do autor.

## 1. Registro de problemas

Status: todos **ABERTO**. Prioridade: P0 quebra a lógica da vila · P1
funcionalidade/estabilidade/desempenho · P2 dívida técnica · P3 manutenção ·
P4 ideia futura. Bug e melhoria não se misturam (melhorias no §2).

| ID | Pri | Problema | Sintoma | Causa | Onde | Evidência | Teste existe | Teste necessário | Playtest? | Solução recomendada | Risco da correção | Depende de |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| P0-01 | P0 | Duas linhas paralelas com colisão lógica | não há um "estado do projeto"; toda correção nova aumenta a divergência | Claude e Codex atenderam os mesmos pedidos de 03-10 em branches separadas | `claude/corrigiveis-sem-jogo` (20 commits) × `codex/village-visuals-logistics-mine-sweep` (12) | CONFIRMADO: `merge-tree` 22 conflitos; `VillageFocusPlayerGameTest` não compila após a fusão (classe movida por B); `closePlan` duplicado (`TreeChoice:441` × `TreeFelling:175`); import `Worker` removido por A e usado por B em `ColonyCycleRunner` | sim, dos dois lados | rodar os dos dois lados após a fusão | sim (mina com save antigo) | D-01..D-03, depois integração A→B (§4) | alto (SHAPE_VERSION 7×8) | decisão do autor |
| P0-02 | P0 | Registro da colônia só gravado no `SERVER_STOPPING` | após crash/kill: colônias, obras, minas, ruas e cursores voltam ao estado do último fechamento normal, enquanto os blocos no mundo são do último autosave | `sync` tem um único chamador | `ServerLifecycleHandler.java:44,205-231`; `ColonySavedData.java:329` | mecanismo CONFIRMADO; perda em jogo PROVÁVEL, não reproduzida | não | T-01, T-02 | sim (matar o processo e reabrir) | chamar o mesmo `sync` periodicamente (ex.: no ciclo longo, a cada N ciclos) — sem mudar formato | baixo-médio (custo do sync por ciclo: medir) | — |
| P1-01 | P1 | Ciclo sem orçamento fora do planejador | — (não medido) | só `ConstructionPlanner` tem prazo | `ColonyCycleRunner:~191-416` | código CONFIRMADO; custo SUSPEITO | CycleCostTest (conta) | — | sim, Spark + breakdown | medir primeiro; só então fatiar a fase que dominar | médio | P2-07 |
| P1-02 | P1 | Lenhador caro sem árvore | 73% do custo do mod (30-09) | `TreeScanner` raio 64 sem prazo de relógio | `integration/TreeScanner.java:49,82,151-165` | CONFIRMADO no Spark de 30-09; correções posteriores não remedidas | LumberjackGameTest | — | sim | remedir; se persistir, prazo de relógio como no `SweepDeadline` | baixo | — |
| P1-03 | P1 | Sem reload real, sem fim-a-fim de casa, sem 2 colônias | regressões de save/integração passam pelo CI | testes focam unidades e cenários pequenos | `src/gametest` | CONFIRMADO (lacuna) | — | T-01..T-04 | — | escrever só esses 4 | baixo | P0-01, P0-02 |
| P1-04 | P1 | 15 conflitos regra × código | agente segue a regra errada | emenda nova sem emendar a fonte antiga | RULE-RECONCILIATION §10 | CONFIRMADO | — | — | — | autor decide C-01..C-15; RULES.md vira fonte única | baixo | — |
| P1-05 | P1 | Documentação engana o agente | agente lê classes que não existem; contagens erradas | docs de agosto nunca arquivados | raiz; `claude/`; `agent/`; `START_PROJECT.md` | CONFIRMADO (§3) | — | — | — | §3 | baixo | — |
| P2-01 | P2 | 10 ciclos de pacote congelados em `fabric/` | acoplamento difícil de rastrear | crescimento sem fronteira interna | `archunit_store/bd1afc07…` (400 linhas) | CONFIRMADO; a linha B já zera o store | ArchUnit | — | — | vem com P0-01 | baixo | P0-01 |
| P2-02 | P2 | Imports mortos em 4 classes de `fabric/event` | leitura cara, acoplamento aparente | divisão de classe antiga | `ColonyCycleRunner`, `VillageAdoption`, `VillagerRegistration`, `VillageDetectionHandler` | CONFIRMADO (contagem mínima) | compilação | — | — | remover imports, nada mais | muito baixo | P0-01 |
| P2-03 | P2 | Código morto e utilitário de teste em produção | — | — | `ColonyFocus`, `PlanAffordability`; `MinerProbe`, `EnduranceReport` | CONFIRMADO por grep | só testes deles | — | — | remover os 2 mortos com seus testes; mover os 2 utilitários para `src/gametest` | baixo | aprovação |
| P2-04 | P2 | `TestBarrier` é regra de produção com nome de teste | confunde leitura e auditoria | provisório que ficou | `work/TestBarrier.java:48`, `BuilderPlacement:170-193` | CONFIRMADO | BuilderGameTest | — | — | renomear depois que a Regra 28 for decidida (fica ou sai) | baixo | C-02 |
| P2-05 | P2 | Lógica duplicada | correção em um lugar só | cópia | `searchRadius` (Farmer, Shepherd, TreeChoice); `speedOf` (`ActionTool:71`, `ToolUpgrade:194`) | CONFIRMADO pela frente de regras | parcial | — | — | extrair uma função cada | baixo | — |
| P2-06 | P2 | Duas ADR-025 | referência ambígua | numeração manual | `docs/decisions/ADR-025-*` | CONFIRMADO | — | — | — | renumerar uma (ou anotar 025a/025b) | muito baixo | — |
| P2-07 | P2 | Custo do ciclo só logado ≥ 50 ms; nada por colônia | otimização às cegas | telemetria por limiar | `VillageDetectionHandler:112,443` | CONFIRMADO | — | — | sim | amostrar o breakdown 1 vez a cada N ciclos | muito baixo | — |
| P2-08 | P2 | Pathfinding Vanilla invisível | encalhe repetido custa sem aparecer | mod usa `WalkTarget` | `brain/GoToWorkTargetTask:114-124` | CONFIRMADO | — | — | sim (Spark) | só medir | — | — |
| P3-01 | P3 | `MinerGameTest` 5.890 linhas / 85 testes; 0 parametrizados | manutenção cara | cópia de cenário | gametest | CONFIRMADO | — | — | — | TEST-STRATEGY §6 | médio | P0-01 |
| P3-02 | P3 | PIT só no `core`; sem piso JaCoCo; PMD `ignoreFailures` | eficácia dos testes da camada fabric desconhecida | escolha de custo | `build.gradle:161` | CONFIRMADO | — | — | — | manter; registrar como decisão consciente | — | — |
| P3-03 | P3 | JAR binário versionado; `Website/` no repo | repo pesado; JAR conflita em toda fusão | — | `downloads/`, `Website/` | CONFIRMADO | — | — | — | JAR em GitHub Releases | baixo | aprovação |

### 1b. Reprodução dos P0/P1

| ID | Como reproduzir |
|---|---|
| P0-01 | `git merge-tree --write-tree --name-only claude/corrigiveis-sem-jogo codex/village-visuals-logistics-mine-sweep` → 22 conflitos; após a fusão, `gradlew compileGametestJava` falha em `VillageFocusPlayerGameTest` |
| P0-02 | abrir um mundo, deixar uma colônia abrir obra/mina, encerrar o processo Java à força (sem "Salvar e sair"), reabrir: `Saved N colonies` não aparece no log e o registro volta ao fechamento anterior. **Não executado.** |
| P1-01 | playtest com 4 colônias carregadas; ler o breakdown de `Colony cycle took` (só aparece ≥ 50 ms) |
| P1-02 | vila sem floresta perto + Spark: nós `TreeScanner`/`TreeChoice`/`FarmerNursery` |
| P1-03 | `grep -rn "save\|reload" src/gametest` — nenhum teste reinicia o servidor |
| P1-04 | RULE-RECONCILIATION §10 (cada linha cita os dois textos e o código) |
| P1-05 | `grep -rw ColonyManager src/main/java` → vazio; `Class-Architecture.md` o descreve |

### 1c. Atualização de 2026-10-06 (integração)

- **P0-01: integrado** na branch `integra/linhas-2026-10-06` (`852f3a54`), com
  D-01..D-03. Verificado: build, 1.325 unitários, 92 Python, 610/610 GameTests
  em duas rodadas. Falta: PR para a `main` e o playtest (mina com save antigo,
  painéis com Iris, argila no lago).
- **Novo, CONFIRMADO:** `MineOverflowStorageGameTest` (linha Codex) nunca esteve
  no `fabric.mod.json` — a bateria dizia 585 sem ele. Registrado. É a prova de
  que T-05 (paridade `@GameTest` × registro) vale o custo.
- **Novo, pendente (P2):** `CropPatch` e `SandGathering` deixaram de pular
  fluidos (o `FluidColumns` da linha Claude saiu pela D-02). Para ligar o
  `VillageFluidIndex` a eles, a classe precisa sair de `fabric/work` para
  `fabric/integration`, senão fecha ciclo de pacote.

## 2. Melhorias

| ID | Melhoria | Problema | Benefício | Complexidade | Risco | Arquivos | Testes | Vale agora? |
|---|---|---|---|---|---|---|---|---|
| M-01 | Gravação periódica do registro | P0-02 | crash não apaga a colônia | B | B | `ServerLifecycleHandler`, `VillageDetectionHandler` | T-01, T-02 | **FAZER AGORA** |
| M-02 | Integração das duas linhas | P0-01 | um só estado | A | A | 22 arquivos | bateria completa ×2 | **FAZER AGORA** (após D-01..D-03) |
| M-03 | Teste de paridade GameTest × mod.json | silêncio | trava | B | B | `ModMetadataTest` | ele mesmo | **FAZER AGORA** |
| M-04 | Arquivar docs | P1-05 | contexto do agente cai ~90% | B | B | raiz, `claude/`, `agent/` | links | **FAZER AGORA** (só `git mv`) |
| M-05 | Breakdown amostrado do ciclo | P2-07 | medir antes de otimizar | B | B | `VillageDetectionHandler` | CycleCostTest | FAZER DEPOIS (com o próximo playtest) |
| M-06 | Remover imports mortos e código morto | P2-02, P2-03 | leitura | B | B | 6 arquivos | compilação | FAZER DEPOIS de P0-01 |
| M-07 | GameTests T-03, T-04 | P1-03 | prova a promessa central | M | B | gametest | eles | FAZER DEPOIS de P0-01 |
| M-08 | Pedido de material explícito com motivo visível (MineColonies) | travas silenciosas de obra | diagnóstico | M | M | `core/coordination`, `WaitingWork` | novos | FAZER DEPOIS (P4 até haver playtest mostrando a trava) |
| M-09 | Paleta escolhida pelo entorno (Verdant Villagers) | "rota na receita não existe no bioma" | menos espera | B | B | `HousePlans.paletteOf` | HousePlansTest | FAZER DEPOIS |
| M-10 | Chave de ordenação explícita de tarefas (RimWorld) | desempate implícito | previsibilidade | B | B | `WorkAssignment` | WorkAssignmentTest | FAZER DEPOIS |
| M-11 | Parametrizar famílias de GameTest | P3-01 | −30% de linhas de teste | M | M | 3 classes | contagem antes/depois | FAZER DEPOIS |
| M-12 | Dividir `ColonyCycleRunner` por fase | acoplamento | leitura | M | M | fabric/event | ColonyCycleGameTest | **SUBSTITUIR POR SOLUÇÃO MAIS SIMPLES**: só remover imports e extrair `buildWorkDemand` |
| M-13 | Injeção de dependência no lugar dos 8 singletons | estado global | testabilidade | A | A | ~300 arquivos | todos | **NÃO FAZER** — `ServerMemory` já resolve o reset; ganho não paga o risco |
| M-14 | Behavior Trees / GOAP | — | — | A | A | — | — | **NÃO FAZER** |
| M-15 | Courier (aldeão carregador) | — | — | A | A | — | — | **NÃO FAZER** — dobra pathfinding; o baú da vila já cobre |
| M-16 | Mixin mais fundo no `VillagerEntity`/Brain | — | — | M | A | — | — | **NÃO FAZER** |
| M-17 | Reescrever qualquer sistema | — | — | A | A | — | — | **NÃO FAZER** (§4 opção E) |

## 3. Documentação: o que fica, o que arquiva

**Ficam ativos (~10):** `CLAUDE.md`, `AGENTS.md` (reduzido a ponteiro),
`STATE.md`, `TODO.md` (≤ 200 linhas), `docs/RULES.md`, `docs/PATTERNS.md`,
`PROJECT_CONSTITUTION.md` (emendado por C-01/C-06/C-10/C-11),
`docs/decisions/ADR-*`, `docs/technical/Plano-de-Correcao.md`,
`docs/technical/Testing-Strategy.md` + `Performance-Rules.md`, e esta pasta
`docs/audit/`. Para humanos: `README.md`, `CHANGELOG.md`.

**Arquivar com `git mv` (histórico preservado) + banner "HISTÓRICO":**

| Destino | O quê |
|---|---|
| `docs/archive/design-2026-08/` | Architecture-Foundation, Class-Architecture, Construction-System, Data-Model, Development-Roadmap, Fabric-Implementation-Plan, MVP, MVP-Tasks, Profession-System, Resource-System, Save-Data-System, Simulation-Loop, Storage-System, START_PROJECT, AUDIT_REPORT, `claude/*` |
| `docs/archive/technical/` | Development-Log, Project-State, Historico-2026-09, Backlog, Auditoria-*, Revisao-*, Project-Audit, Operational-Status, E45/E46, Initial-Setup-Checklist, Codex-Setup |
| remover do git | `agent/` (duplica `.claude/skills` com conteúdo divergente), `mods/` (vazia) — **só com aprovação** |

**Classificação completa** (frente de documentação, 06-10):

| Documento / grupo | Classe | Motivo |
|---|---|---|
| CLAUDE.md, STATE.md, docs/RULES.md, docs/PATTERNS.md | ACTIVE | governam o trabalho |
| AGENTS.md | ACTIVE (Codex) | duplica o resumo do CLAUDE.md |
| TODO.md (1.582 linhas) | ACTIVE, inchado | topo com estado de 28-09 |
| PROJECT_CONSTITUTION.md | ACTIVE | emendado em 06-10 (ADR-034) |
| README.md, CHANGELOG.md | REFERENCE | README:210 com contagem antiga |
| docs/decisions/ADR-* | ADR | duas ADR-025 |
| docs/technical/Plano-de-Correcao, Testing-Strategy, Performance-Rules, Debugging-Strategy, Development-Workflow, Fabric-Version, Profiling-spark, Vanilla-Integration, Village-Economy, village-growth-planner, Profession-Responsibility, Profession-Policies-Assessment | ACTIVE / REFERENCE | — |
| docs/behavioral-tests/* | REFERENCE | known-failures em uso |
| docs/proxima-sessao.md | STALE | JAR e contagens de 28-09 |
| Development-Log, Project-State, Historico-2026-09, Backlog | HISTORICAL | só grep |
| Auditoria-*, Revisao-*, Project-Audit, Operational-Status, E45/E46, Identidade-da-Vila, Fluidez-* | HISTORICAL | datados |
| docs/research/*, docs/superpowers/* | HISTORICAL | planos executados |
| docs/workers-analysis/* | ORPHAN | ninguém aponta |
| Class-Architecture, Data-Model, Fabric-Implementation-Plan, MVP-Tasks | STALE (enganam) | 29 / 9 / 11 / 4 classes inexistentes |
| Architecture-Foundation, Construction-System, Development-Roadmap, MVP, Profession-System, Resource-System, Save-Data-System, Simulation-Loop, Storage-System | HISTORICAL | design de agosto |
| START_PROJECT.md | STALE / DELETE-CANDIDATE | manda ler 20 docs de agosto |
| AUDIT_REPORT.md | HISTORICAL | auditoria de 14-09 |
| claude/* | DUPLICATE / STALE | CLAUDE.md antigo em inglês concorre com o da raiz |
| agent/skills/* | DUPLICATE | cópia divergente de .claude/skills, não é carregada |
| Initial-Setup-Checklist, Codex-Setup | DELETE-CANDIDATE | setup de agosto; duplica AGENTS.md |

**Correções pontuais:** CLAUDE.md §0.1 remete a "§1 e §2" que não existem;
contagens em CLAUDE.md:47, TODO.md:12, README.md:210, proxima-sessao.md:8
(fonte certa: STATE.md); `docs/proxima-sessao.md` reescrito em ≤ 80 linhas.
Depois de mover, rodar uma checagem de links relativos.

Evidência: 29 classes citadas em `Class-Architecture.md` não existem
(`ColonyManager`, `TaskManager`, `*Service`…); 9 em `Data-Model.md`; 11 em
`Fabric-Implementation-Plan.md`.

## 4. Decisões arquiteturais

### Opções gerais

| Opção | Custo | Risco | Benefício | Regressão | Veredito |
|---|---|---|---|---|---|
| A — manter + pequenas correções | B | B | resolve P0/P1 | baixa | **base** |
| B — refatoração gradual | M | M | reduz acoplamento em `fabric/event` | média | só onde um P1 pedir |
| C — reorganização parcial | M-A | M | — | média | não agora |
| D — reescrever sistemas | A | A | nenhum problema medido exige | alta | **não** |
| E — reescrita completa | muito A | muito A | — | total | **não** |

**Escolha: A, com B pontual.** O núcleo é puro e testado; nenhuma classe é
gigante; nenhum problema encontrado exige trocar arquitetura.

### Decisões do autor (tomadas em 2026-10-06)

| ID | Decisão do autor | Efeito |
|---|---|---|
| D-01 | **salões** (linha B, Codex) | `MineShaft`/`MineMouth`/testes da mina vêm de B; `SHAPE_VERSION 8` |
| D-02 | **ADR** (motor de B, ADR-031) | `VillageSpiralSweep` + `VillageFluidIndex`; de A ficam `LocateFallback` e a exceção da argila |
| D-03 | **painel do Claude** (linha A) | `OverlayDrawing`/`OverlaySprites` base; de B entra `OverlayPreferences` (esconder texto) |
| D-04 | **emendar**; os quatro itens podem surgir do nada | ADR-034; Constituição 1.2.0 (§4, §5, §7, §9) |
| D-05 | **vale o código atual** | ADR-034 §5; nota de superação na ADR-030 |

### Decisões que estavam pendentes (histórico da pergunta)

| ID | Pergunta | Linha A (Claude) | Linha B (Codex) | Sugestão |
|---|---|---|---|---|
| D-01 | Geometria da mina | salas 3×3, `SHAPE_VERSION 7`, arco que emoldura as pistas (`MineMouth.archStones`) | salões 10×10×3, escada de 2 lances, `SHAPE_VERSION 8`, arco centrado | autor escolhe em jogo; o teste segue o lado escolhido; conferir save antigo |
| D-02 | Motor de varredura de superfície e fluidos | `RingSweep` + `FluidColumns` (commit diz "não testado"; usado também em `CropPatch`, `SandGathering`) | `VillageSpiralSweep` + `VillageFluidIndex` (ADR-031, testes) | **B**, ligando o índice também a `CropPatch`/`SandGathering`; manter de A o `LocateFallback` |
| D-03 | Sistema de painel | `OverlayDrawing`/`OverlaySprites`, `WorldRenderEvents.LAST` (corrige Iris) | `PixelPanelLayout`/`WorldPixelPanelRenderer`, opção de esconder texto | **A** como base + `OverlayPreferences` e tabela estado→textura de B |
| D-04 | Constituição §4/§7/§9/§5 × código | — | — | emendar a Constituição com as exceções aceitas (C-01, C-06, C-10, C-11) |
| D-05 | Fundação: ADR-030 × ADR-011/Regra 35 | — | — | uma frase em ADR-030 dizendo qual vale |

### Formato das demais recomendações

```text
P0-02  PROBLEMA  registro só vai ao disco ao fechar
       CAUSA     sync tem um chamador (SERVER_STOPPING)
       IMPACTO   crash → colônia volta no tempo, mundo não
       ALTERNAT. (a) sync a cada ciclo longo  (b) mixin em MinecraftServer.save
                 (c) markDirty por mutação em cada serviço
       RECOMEND. (a), a cada N ciclos; sem mixin, sem mudar formato
       RISCO     baixo; custo do sync deve ser medido (log de ms)
       CUSTO     pequeno (1 chamada + 2 testes)
       PRIOR.    P0
```

## 5. Pesquisa externa

| Projeto | Ideia | Adaptar | Não copiar | Custo | Risco |
|---|---|---|---|---|---|
| [MineColonies — Requests](https://minecolonies.com/wiki/systems/request/) | pedido com estado; resolvedores em cadeia; "sem solução" visível | objeto `Request` no core; ordem mãos→baú→fabricar→natureza→jogador | Courier, Postbox, escala; issues abertas de builder (#10802, #11388) mostram que o modelo não elimina trava, só a torna visível | M | M |
| [Verdant Villagers](https://github.com/CosmicTerrorTurtle/Verdant-Villagers) | "coração" da vila planeja; paleta pelo entorno | paleta pelo entorno | coração como entidade; colocação instantânea (delegação aos aldeões não está feita lá) | B | B |
| [RimWorld JobGiver_Work](https://github.com/josh-m/RW-Decompile/blob/master/RimWorld/JobGiver_Work.cs) | ordenação por chaves fixas | desempate explícito em `WorkAssignment` | o resto | B | B |
| [Factorio — logistic network](https://wiki.factorio.com/Logistic_network) | separar pedido (puxar) de descarte (empurrar), prioridade entre fontes | ordem explícita de origem | — | B | B |
| [Fabric — automated testing](https://docs.fabricmc.net/develop/automatic-testing) | `batchId`, `maxAttempts`, relatório em arquivo | só para instabilidade de ambiente provada; subir `build/test-results` no CI | mascarar defeito de mecanismo | B | B |
| [rubenwardy — IA de colonos](https://blog.rubenwardy.com/2022/07/17/game-ai-for-colonists/) | BT para execução, GOAP para decisão | confirma o desenho atual (planejador central + execução) | migrar | — | — |
| Práticas AGENTS.md / CLAUDE.md | arquivo raiz curto, leitura sob demanda | já praticado; falta arquivar o ruído | — | B | B |

NÃO ENCONTRADO: "Live Villages"; mod que classifique necessidade da vila sobre
estruturas Vanilla; referência específica de time-slicing por tick.

## 6. Método de manutenção — **Protocolo PASSO**

Nome: **PASSO** — *Pergunta · Achar · Seguro · Subir pouco · Observar*.
Feito para este projeto: grande, crescido com IA, caro em contexto, com
comportamento emergente que teste não prova sozinho.

### Antes de alterar

1. **Pergunta** — qual problema, com ID (P0-02, D-01…). Sem ID, não começa.
2. **Achar** — ler STATE.md → item do ROADMAP → `grep` no símbolo. Nunca
   documento inteiro de histórico. Registrar arquivos tocados.
3. **Seguro** — escrever o comportamento esperado em 1 frase e o teste mínimo
   que falha hoje (nível 0–2 da escada; ver TEST-STRATEGY §5).

### Durante

4. **Subir pouco** — uma responsabilidade por commit. Nunca misturar no mesmo
   commit: refatoração + comportamento; código + regra; duas linhas de
   trabalho. Uma branch viva por vez a partir da `main` (lição de P0-01).

### Depois

5. **Observar** — teste específico → relacionados → GameTest relacionado →
   bateria só se o risco pedir. Informar sempre:

```text
TESTE EXECUTADO · POR QUE · RESULTADO · O QUE FOI COMPROVADO · O QUE AINDA NÃO
AUTOMATICAMENTE COMPROVADO | AINDA PRECISA DE PLAYTEST
```

6. **Registrar** — STATE.md (sobrescreve), item do ROADMAP com status; se uma
   regra mudou, emendar **também a fonte antiga** (Constituição/ADR/comentário)
   no mesmo commit — é isso que gerou os 15 conflitos.

### Protocolo curto para agentes (copiar no início da sessão)

```text
1. Leia STATE.md e o item do ROADMAP pelo ID. Nada mais até precisar.
2. Diga: problema, causa, menor correção, como será provada.
3. Um commit = uma coisa. Uma branch viva por vez.
4. Teste mais específico primeiro; bateria completa só para fechar P0/release.
5. Nunca "passou" sem ter rodado. Nunca "funciona em jogo" sem playtest.
6. Mudou regra? Emende a fonte antiga junto.
7. Atualize STATE.md e o status do item. Commit.
```

## 7. Plano de recuperação (ordem real)

```text
ETAPA 0  Autor decide D-01..D-05                 ✅ feito em 06-10 (ADR-034)
   ↓
ETAPA 1  ✅ 06-10 (852f3a54, 610/610 ×2; falta PR na main e playtest)
         P0-01 Integração: branch integra/A→main, B por cima,
         decisões aplicadas, binários regenerados; bateria ×2   (1 PR)
   ↓
ETAPA 2  P0-02 Gravação periódica + T-01/T-02                   (1 PR)
   ↓
ETAPA 3  Playtest único com Spark + time_ledger + kill do processo
         → fecha P0-02 em jogo, remede P1-01/P1-02, valida as 20+
           correções pendentes do STATE
   ↓
ETAPA 4  Documentação: git mv para docs/archive, corrigir contagens,
         RULES.md resolve C-01..C-15                            (só docs)
   ↓
ETAPA 5  Testes que faltam: T-03, T-04, T-05
   ↓
ETAPA 6  Performance — só a fase que o Spark da Etapa 3 apontar
   ↓
ETAPA 7  Limpeza: imports, código morto, duplicações (P2-02..P2-05)
   ↓
ETAPA 8  Consolidação de GameTests (P3-01), um arquivo por commit
   ↓
ETAPA 9  Melhorias M-08..M-10, uma por vez, cada uma com playtest
```

Por que esta ordem: integrar primeiro impede que todo o resto seja feito duas
vezes; a persistência vem antes do playtest para que o playtest a prove; a
documentação vem depois das decisões para não arquivar algo que a decisão
reabre; otimização só depois de medir; limpeza e consolidação por último
porque não mudam comportamento e não devem competir com P0.
