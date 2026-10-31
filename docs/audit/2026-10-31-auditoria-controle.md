# Auditoria de controle — MOD-village-1.21.1 (2026-10-31)

Modo: **somente leitura**. Nenhum arquivo do repositório foi alterado, nenhum commit, nenhum checkout.
Snapshot: branch `ccr-4cf7a1a1-w9h8bq` (= `origin/main` + 8 commits), `origin/main` em `36e3cee` (PR #39).
**Build e testes NÃO foram executados nesta auditoria** — contagens de teste abaixo vêm de `grep` ou do `STATE.md`.

Marcas: **[C]** confirmado por comando nesta sessão · **[D]** dito pela documentação, não reverificado · **[H]** hipótese.

---

## PARTE A — Situação atual

### A.1 Tamanho [C]
| Conjunto | Arquivos | Linhas |
|---|---|---|
| `src/main/java` | 603 | 99.529 |
| `src/test/java` (JUnit) | 234 | 32.122 |
| `src/gametest/java` | 248 | 52.728 (940 ocorrências de `@GameTest`) |
| Markdown fora de `src/` | 467 | ~69.500 só em `docs/` |
| Scripts Python em `scripts/` | ~38 | — |

### A.2 Arquitetura [C]
- Entrypoints: `VillageColonyMod` (main), `VillageColonyClient`, `VillageColonyModMenu`.
- Duas camadas: `core/*` (domínio puro: coordination, construction, colony, storage, worker, task, telemetry, movement)
  e `fabric/*` (integração com o jogo). Fronteira vigiada por ArchUnit (`ArchitectureRulesTest` + `archunit_store`
  congelado — há violações congeladas, número não contado).
- `fabric/work` concentra **216 classes** e `fabric/integration` **131**: é aí que mora quase todo o comportamento.
- 7 mixins (`ChunkGenerator`, `HostileSpawn`, `ItemStackCraft`, `LandPathNodeMaker`, `MoveToTargetTask`,
  `VillagerData`, `VillagerEntity`), `required: true`, `defaultRequire: 1`.
- Ciclo principal: `fabric/event/ColonyCycleRunner` + `VillageDetectionHandler`; persistência em `data/save`.
- **Já existe um mapa de arquitetura**: `docs/audit/SYSTEM-MAP.md` (2026-10-06/08). Não foi reescrito aqui; está
  ~100 versões atrás (0.3.15 → 0.3.110) e precisa de atualização, não de substituição.

### A.3 Complexidade [C]
- **Teto artificial de 500 linhas**: `scripts/gauntlet_checks.py:55` (`MAX_FILE_LINES = 500`) e o hook
  `file_size_guard.py`. Resultado: **38 classes entre 450 e 500 linhas**, as 21 maiores todas entre 485 e 500
  (`SurfaceGatheringWork` 500, `ColonyGoals` 500, `StrandedEscape` 499, `ColonyCycleRunner` 499…).
  Isso indica classes **cortadas para caber na régua**, não por responsabilidade [H forte — padrão numérico confirmado,
  motivo de cada corte não verificado]. Efeito: a lógica de um ofício fica espalhada em vários arquivos
  (ex.: `MinerWork` + `MineDigging` + `MineRestart` + `Mine`), o que aumenta leituras por tarefa.
- Escape/encalhe tem várias classes próximas (`StrandedEscape`, `PenEscape`, …) — candidato a sobreposição [H].

### A.4 Organização [C]
- **Três auditorias anteriores** sobre o mesmo tema: `docs/audit/PROJECT-RECOVERY-AUDIT.md` + 5 irmãos (10-06),
  varreduras 10-14 e 10-20, e `PROJECT_AUDIT.md` na raiz. Esta é a quarta. O problema não é falta de diagnóstico,
  é diagnóstico que não vira estado vivo.
- Documentos de entrada concorrentes na raiz: `CLAUDE.md`, `AGENTS.md`, `PROJECT_CONSTITUTION.md` (339 l),
  `PROJECT_AUDIT.md`, `STATE.md` (545 l), `TODO.md`, `CHANGELOG.md` (1.474 l), `README.md`.
- `STATE.md` deixou de ser "estado": é um changelog por versão (0.3.103 → 0.3.110 no topo), duplicando `CHANGELOG.md`.
- **Regras sem documento único**: README cita até Regra 158; Regras 159–162 só existem em `STATE.md`/`CHANGELOG.md`.
  `docs/RULES.md` (298 l) cita só 14 números "Regra N".
- Duas ADR-025 com assuntos diferentes (`ADR-025-client-side-pixel-art-overlays.md`, `ADR-025-mineiro-autonomo.md`).
- Lixo de documentação: `docs/TODO-archive.md.txt`, `docs/superpowers/`, `docs/workers-analysis/` [conteúdo não lido].

---

## PARTE B — Problemas técnicos

| # | Problema | Evidência | Marca |
|---|---|---|---|
| B1 | **8 versões seguidas publicadas sem playtest** (0.3.103–0.3.110). `STATE.md` tem 23 ocorrências de "Não jogado"/"não foi jogada". Cada versão empilha regras novas sobre comportamento não validado. | `STATE.md` linhas 1–60 | C |
| B2 | **Testes instáveis**: na 0.3.110 `ShepherdHerding`, `ShepherdFeeding` e `Builder ground` falharam uma vez e passaram na repetição. | `STATE.md` §0.3.110 | D |
| B3 | **Regras 161 e 162 sem teste** — viola a Regra 3 do `CLAUDE.md`. | `STATE.md` §0.3.110 | D |
| B4 | **0.3.110 não está na `main`**: os 4 commits estão em `fix/playtest-0102` e nesta branch `ccr-…` (+ merge, docs e 2 commits de CI/release). Mas o pré-lançamento `v0.3.110-alpha` já foi publicado a partir dela. | `git rev-list` | C |
| B5 | Workflow `release.yml` publica só `remapJar` (sem testes) por `workflow_dispatch`. Um JAR pode sair sem a bateria. | commit `05d3405`, `29b9ca4` | C (mensagem) / conteúdo do yml não lido |
| B6 | 6 branches remotas com commits fora da `main` por patch: `fix/obra-pecas-de-lado` (+27), `fix/playtest-0397` (+15), `claude/opcao-b-e-plano-de-testes` (+9), `ccr-e0fb7319-jp46gw` (+2), `fix/playtest-0102` (+4), esta (+8). As duas primeiras parecem ter entrado na `main` por squash (PR #37 cita 0.3.98→0.3.102 e `VC_STATE` existe na `main`) — **não verificado arquivo a arquivo**. ~25 outras branches remotas já estão contidas na `main`. | `git cherry` | C / H |
| B7 | ArchUnit com violações **congeladas** (`archunit_store`): dívida de dependência aceita, número não medido. | `src/test/resources/archunit_store` | C |
| B8 | Mixin `MoveToTargetTaskMixin` e `VillagerDataMixin` sem `@Inject/@Redirect/@Overwrite` — usam outro mecanismo (`@ModifyArg`, `@Accessor`…?) ou estão vazios. | `grep -c` = 0 | C / precisa investigação |

### Classificação dos sistemas
Aviso: sem jogo desde a 0.3.102 e sem rodar a bateria aqui, **nenhum sistema pode ser marcado ESTÁVEL com evidência desta
sessão**. A coluna reflete o que a documentação + os testes existentes sustentam.

| Sistema | Pacote principal | Estado | Base |
|---|---|---|---|
| Detecção de vila / colônia | `fabric/event/VillageDetectionHandler`, `core/colony` | FUNCIONAL COM RESSALVAS | jogado em versões anteriores [D] |
| Ciclo da colônia | `ColonyCycleRunner`, `core/coordination/ColonyGoals` | FUNCIONAL COM RESSALVAS | classe no teto de 499/500 |
| Profissões (8) e atribuição | `core/worker/service/ProfessionAssigner`, `fabric/work/*Work` | FUNCIONAL COM RESSALVAS | Regra 51 (ócio ≤15%) sem medição recente |
| Mineração | `MinerWork`, `MineDigging`, `MineRestart`, `core/construction/model/Mine` | FUNCIONAL COM RESSALVAS | mudou nas 0.3.106/108/109/110, não jogado |
| Construção / obras | `BuilderWork`, `HousePlans`, `ConstructionProject` | PRECISA DE INVESTIGAÇÃO | B2 + "construtor sem material" pendente [D] |
| Pastor / rebanho | `ShepherdWork`, `PenEscape` | PROBLEMÁTICO (flaky) | B2 |
| Solo / alisamento / mistura | `VillageBlend`, `GroundSmoothing`, `BlockShaping` | EXPERIMENTAL | Regras 152/156/162 recentes, não jogadas |
| Ruas e trajeto | `RoadIndex`, `RoadExtension`, `StreetRoute`, `MoveToTargetTaskMixin` | EXPERIMENTAL | Regra 151, não jogada |
| Movimento (pulo, telhado) | `LandPathNodeMakerMixin`, `StrandedEscape` | EXPERIMENTAL | Regras 159/160 só na 0.3.110 |
| Armazém / estoque | `core/storage`, ADR-024/032/033 | PRECISA DE INVESTIGAÇÃO | não auditado nesta sessão |
| Persistência | `data/save`, ADR-039 (branch não integrada) | PRECISA DE INVESTIGAÇÃO | B6 (`opcao-b` traz teste de save não integrado) |
| Telemetria/diários | `VC_TIME/COST/EVENT/STATE` + scripts | FUNCIONAL | CI roda os testes Python |
| SEM UTILIZAÇÃO | — | **nenhum confirmado**: não verifiquei registradores/reflexão, então não declaro nada morto |

---

## PARTE C — Ambiente Claude Code

### Skills do projeto (`.claude/skills/`, 24) [C]
Custo: a **descrição** de cada skill entra em todo contexto; o corpo só quando invocada. As 24 descrições são longas.

| Manter (núcleo) | Motivo |
|---|---|
| `fabric-development` | implementação; citada no workflow do CLAUDE.md |
| `minecraft-code-research` | Regra 1 do CLAUDE.md |
| `minecraft-villager-systems` | domínio central |
| `minecraft-testing` (Jahrome907) | Regra 2/3 |
| `minecraft-performance-profiling` | Regra 50, VC_COST |

| Manter sob demanda | Motivo |
|---|---|
| `fabric-persistence-migration`, `java-architecture-review`, `git-workflow-audit` | específicas do projeto, curtas (~3 KB) |
| `minecraft-resource-pack` | só para texturas/overlays (ADR-025) |
| `minecraft-world-generation` | o mod mexe em geração (`ChunkGeneratorMixin`, `beard_box`) |

| Remover do projeto (proposta, aguardando autorização) | Motivo |
|---|---|
| `minecraft-plugin-dev`, `minecraft-essentials-ops`, `minecraft-server-admin`, `minecraft-worldedit-ops` | Paper/Bukkit/servidor — fora do escopo de um mod Fabric |
| `minecraft-multiloader` | projeto é só Fabric |
| `modpack-modsmith` | em espanhol, sobrepõe `fabric-development` |
| `minecraft-commands-scripting`, `minecraft-datapack`, `minecraft-imagegen` | uso raro; reativar se surgir tarefa |
| `minecraft-modding`, `minecraft-ci-release` (Jahrome907) | sobrepõem `fabric-development` e o CI existente |
| `java-static-analysis` | avaliar: útil só em auditoria de qualidade |
| `graphify` (43 KB) | maior skill; CLAUDE.md Regra 4 exige — **decisão do autor** |

Isso reduz de 24 para ~10 descrições carregadas. `skills-lock.json` só registra 3 (as de Jahrome907); as outras 21
não têm origem registrada [C].

`chouzz/minecraft-mod-dev` e o repositório Jahrome907: **não avaliados por conteúdo nesta sessão** (sem WebFetch);
três skills de Jahrome907 já estão instaladas.

### Skills de plugin / globais [C]
Listadas nesta sessão também: `pdf-viewer:*`, `cowork-plugin-management:*`, `anthropic-skills:*` (docx, pptx, xlsx,
web-database, web-deploy…), conectores Adobe, Figma, Notion, Google Drive. **Nada disso serve ao mod**; são do nível
de conta/ambiente e cada uma soma descrição ao contexto. Desligar fora deste projeto é decisão sua (não removi nada).

### MCPs
- Projeto: **nenhum `.mcp.json`** [C].
- npm [C, `npm view` em 2026-10-31]: `@adhisang/minecraft-modding-mcp` 8.0.0 (atualizado 2026-10-28);
  `@mcdxai/minecraft-dev-mcp` 1.3.0 (2026-08-06); `craftmcp` 1.0.2 (2026-05-22); `headlessmc-mcp` **não existe no npm (404)**.
- Funcionalidades declaradas de cada um (Yarn, descompilação, validação de mixin/access widener): **não verificadas**.
- Recomendação: no máximo **um** MCP de leitura de código vanilla, escolhido após teste numa classe conhecida
  (ex.: `VillagerEntity.tick`). Hoje o próprio `./gradlew genSources` já dá as fontes Yarn sem MCP — verificar se o MCP
  traz algo além disso antes de instalar. `craftmcp`/headless: **não recomendado agora** — o `runGametest` já sobe um
  servidor e o gargalo é playtest humano, não automação.

### Agentes, comandos e hooks [C]
- `.claude/agents/gauntlet-verifier.md` (9,9 KB) + `/gauntlet` + `scripts/gauntlet.py`: verificador independente —
  útil, mas caro (roda a bateria). Usar só antes de entrega.
- Hooks `commit_gate.py` (roda `gradlew build` em cada commit que toca `src/`) e `file_size_guard.py`: **não estão
  ligados neste ambiente** (sem `.claude/settings.json`). O README dos hooks diz que o settings é ignorado pelo git.
- `AGENTS.md` (instruções para Codex) existe ao lado do `CLAUDE.md`.

---

## PARTE D — Economia de tokens

| Fonte de desperdício | Evidência | Ação |
|---|---|---|
| `STATE.md` de 545 linhas lido "1º" em toda tarefa | CLAUDE.md §0.1 | cortar para ≤60 linhas: versão, o que espera jogo, P0 aberto. Histórico vai para o CHANGELOG |
| 24 descrições de skill + dezenas de skills de conta | listagem desta sessão | Parte C |
| Auditorias repetidas (4) | Parte A.4 | uma única `docs/audit/` viva; as outras para `docs/archive/` |
| Regra de 500 linhas → um ofício em 4–6 arquivos | A.3 | subir o teto (ex.: 800) e parar de fatiar; não reagrupar o existente em massa |
| Bateria inteira (940 GameTests) a cada versão + 1663 unitários | STATE | ver estratégia abaixo |
| Comentários/relatórios longos por versão | STATE/CHANGELOG duplicados | uma entrada por versão, só no CHANGELOG |

### Estratégia de testes por nível
1. Mudança em regra pura (`core/`): só os unitários da classe/pacote (`./gradlew test --tests '…'`).
2. Mudança num ofício: unitários + GameTests da profissão (filtro por classe/batch do `runGametest`, se o projeto já
   suportar — **verificar** em `build.gradle`).
3. Antes de PR: `./gradlew build` completo (já é o que o CI faz).
4. Antes de release: bateria completa + auditoria de suprimento + **playtest** + `time_ledger.py`.

### Fluxo por tarefa (ex.: bug de mineração)
`STATE.md` curto → `docs/PATTERNS.md` (grep pelo sintoma) → classes `Mine*`/`MinerWork` → ADR-025/027/033 →
skill `minecraft-code-research` só se tocar vanilla → teste red → fix → testes do pacote mina → PR.
Textura: só `minecraft-resource-pack` + `src/main/resources/assets`. Nada de domínio.

---

## PARTE E — Plano de recuperação

### P0
| Ação | Problema/evidência | Risco | Esforço | Testes |
|---|---|---|---|---|
| **P0-1 Congelar features e jogar a 0.3.110** | B1: 8 versões sem jogo | regressões acumuladas e não atribuíveis | 1 sessão de jogo + `time_ledger`/`action_report` | playtest + Regras 50/51 |
| **P0-2 Decidir o destino da 0.3.110** (PR para `main` ou não) | B4: release publicado fora da `main` | `main` não reproduz o JAR público | pequeno | `./gradlew build` no PR |
| **P0-3 Tratar os 3 flaky** | B2 | bateria deixa de ser sinal | médio | repetir N vezes cada um |

### P1
- P1-1 Testes das Regras 161 e 162 (B3).
- P1-2 Encurtar `STATE.md`; uma única lista de regras numeradas (159–162 hoje fora do README).
- P1-3 Limpar branches: confirmar por `git diff` que `fix/obra-pecas-de-lado` e `fix/playtest-0397` estão na `main`;
  decidir `claude/opcao-b-e-plano-de-testes` (traz teste de save, ADR-039). **Exclusão só com sua autorização.**
- P1-4 Enxugar skills (Parte C) — com backup em `docs/archive/skills/` ou só desativar.
- P1-5 Atualizar `SYSTEM-MAP.md` em vez de criar novo mapa; mover auditorias antigas e `PROJECT_AUDIT.md` para o arquivo.
- P1-6 Renumerar a ADR-025 duplicada.

### P2
- P2-1 Rever o teto de 500 linhas (decisão sua); unir só quando já for tocar a classe.
- P2-2 Contar e reduzir as violações congeladas do ArchUnit.
- P2-3 Release só depois do CI verde (gate no `release.yml`).
- P2-4 Escolher e testar **um** MCP vanilla.

### P3
- Avaliar `craftmcp`/automação de cliente para playtest roteirizado.
- Consolidar classes de escape/encalhe se a investigação confirmar sobreposição.

---

## PARTE F — Organização recomendada (adaptada ao que já existe)
```
CLAUDE.md          ≤150 l, ponto de entrada (já está)
STATE.md           ≤60 l, só estado vivo
TODO.md            fila aberta (já está)
CHANGELOG.md       histórico por versão (único)
docs/RULES.md      todas as regras numeradas, uma linha cada + teste que a cobre
docs/PATTERNS.md   assinaturas de defeito (já está)
docs/decisions/    ADRs (já está)
docs/audit/        SYSTEM-MAP.md atualizado + TEST-STRATEGY.md; o resto → docs/archive/
docs/research/     relatórios de jogo (já está)
docs/archive/      histórico (já está)
.claude/skills/    ~10 skills
```
Não criar `architecture/`, `testing/`, `development/`: os equivalentes já existem.

## PARTE G — Próximas 5 ações
1. Você joga a 0.3.110 em mundo novo; eu rodo `time_ledger.py`, `action_report.py`, `cost_ledger.py` nos logs.
2. Você decide: PR desta branch para `main` (com os 2 commits de CI/release) ou não.
3. Eu rodo `./gradlew build` e a bateria aqui para ter números desta sessão (não feito ainda).
4. Lista de skills a remover — você aprova; eu movo com backup.
5. Encurto `STATE.md` e reúno as Regras 159–162 em `docs/RULES.md` (PR só de docs).

---

## Fechamento
- **Fatos confirmados**: tamanhos, branches e divergências, teto de 500 linhas e seu efeito numérico, 7 mixins, 24 skills,
  ausência de MCP do projeto, hooks desligados, versões npm dos MCPs, 4 auditorias anteriores.
- **Hipóteses**: fatiamento por régua em vez de responsabilidade; branches `fix/obra-pecas-de-lado`/`playtest-0397`
  incorporadas por squash; sobreposição nas classes de escape.
- **Lacunas**: build/testes não executados; conteúdo dos 2 mixins sem `@Inject`; violações ArchUnit; capacidades reais
  dos MCPs; repositórios de skills externos; código morto (não procurado com registradores/reflexão).
- **Confiança**: alta no inventário e no git; média na classificação de sistemas (depende de jogo que não houve).
