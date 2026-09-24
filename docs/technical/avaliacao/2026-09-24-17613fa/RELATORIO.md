# Avaliação técnica — Village Colony — 2026-09-24

**Commit avaliado:** `17613fa` (branch `codex/bighousemod`) · **Régua:** v1
(`../METODOLOGIA.md`) · **Dados:** `metricas.json` e `scorecard.md` nesta pasta
· **Primeira avaliação:** é a linha de base; a próxima se compara com esta.

---

## 1. Resumo executivo

**Conceito B, média 3,21 de 4** (14 de 14 critérios medidos).

O projeto tem engenharia de verificação acima do comum para um mod: camadas
respeitadas, teste de mutação, bateria de 433 testes de jogo estável em duas
rodadas e documentação de decisão. Os pontos fracos não estão nas peças, e
sim no **sistema**: estado global espalhado em quase 50 classes, cada ofício
reimplementando o mesmo ciclo de tarefa, e um ciclo de colônia que passa do
tique do servidor cerca de 29 vezes por hora de jogo.

| Três forças | Três riscos |
|---|---|
| Núcleo de domínio isolado do Minecraft (C04 = 0 violações) e 91,7% coberto | Estado estático mutável: 89 campos, 49 classes com `clearAll`, 78 limpezas manuais no ciclo de vida (C05 = 2) |
| Testes que provam comportamento: PIT 77,5% (força 86%), 1,19 linha de teste por linha de produção, 433 GameTests verdes em 2 rodadas | Desempenho: 196 ciclos acima de um tique em 6,7 h de jogo (C13 = 1) |
| Processo automatizado completo: CI, análise estática, mutação, cobertura e hook de commit (C12 = 7/7) | Integração: 85 commits no branch de trabalho sem ir para a `main`, que ficou parada |

**Recomendação principal:** medir o ciclo com o spark no próximo playtest e
atacar o custo por tique antes de qualquer funcionalidade nova. É o único
critério com nota 1, e é o que o jogador sente.

---

## 2. Scorecard

| # | Critério | Medida | Nota |
|---|---|---|---|
| C01 | Arquivos > 500 linhas (produção) | 0% (0 de 283) | **4** |
| C02 | Tamanho de método (p90, linhas de código) | p90 24, máx 98 | **3** |
| C03 | Complexidade ciclomática (% métodos > 10) | 2,7%, máx 26 | **4** |
| C04 | Regra de camadas | 0 violações | **4** |
| C05 | Estado estático mutável | 89 campos (3,96/kLOC) | **2** |
| C06 | Volume de teste | 1,19x | **4** |
| C07 | Cobertura do core+data | 91,7% (fabric por unitário: 11,6%) | **4** |
| C08 | Mutação (PIT, core) | 77,5% mortas, força 86,4% | **3** |
| C09 | Estabilidade da bateria de jogo | 2 rodadas, 0 falha | **4** |
| C10 | Análise estática | 57 avisos (2,5/kLOC) | **3** |
| C11 | Densidade de comentário | 0,98 | **3** |
| C12 | Práticas automatizadas | 7 de 7 | **4** |
| C13 | Desempenho em jogo | 29,2 ciclos > 1 tique por hora | **1** |
| C14 | Dívida crítica | 4 itens 🔴 de 52 abertos | **2** |
| | **Média** | | **3,21 — B** |

**Por dimensão:**

| Dimensão | Nota |
|---|---|
| Organização | 3,5 |
| Complexidade | 4 |
| Arquitetura | 3 |
| Testes | 3,75 |
| Qualidade estática | 3 |
| Legibilidade | 3 |
| Processo | 4 |
| Desempenho | **1** |
| Registros | 2 |

---

## 3. Comparação

Não há avaliação anterior. Duas referências informais da mesma sessão ajudam
a ler o número:

| Medida | Antes | Agora |
|---|---|---|
| Classes de produção acima de 500 linhas | 17 (maior: `BuildSiteScanner`, 2.136) | 0 |
| Avisos `InvalidLink` | 73 (a refatoração quebrou 62 links de javadoc) | 9 (links corrigidos nesta avaliação) |

---

## 4. Métricas detalhadas

### 4.1 Tamanho e forma

| Medida | Valor |
|---|---|
| Arquivos de produção | 283 |
| Linhas físicas | 51.875 |
| Linhas de código | 22.463 |
| Linhas de comentário | 21.958 |
| Camada `fabric` | 163 arquivos, 16.979 linhas de código (75%) |
| Camada `core` | 110 arquivos, 4.608 linhas de código (21%) |
| Camada `data` | 9 arquivos, 840 linhas de código (4%) |
| Métodos medidos | 1.507 |
| Linhas por método: mediana, p90, máximo | 6, 24, 98 |
| Métodos com mais de 50 linhas de código | 26 |
| Maior arquivo de produção | 494 linhas |
| Maior arquivo de teste | `MinerGameTest.java`, 5.807 linhas |

### 4.2 Complexidade

CC média de 2,91 e p90 de 6,4. São 41 métodos com CC > 10 (2,7%) e 2 com
CC > 20. Os pontos quentes:

| CC | Linhas de código | Método |
|---|---|---|
| 26 | 71 | `MinecraftTypeAdapter.toResourceType` (tabela de tradução com `if` encadeado) |
| 23 | 98 | `BuildSiteScanner.find` |
| 20 | 69 | `MineCuts.nextCut` |
| 20 | 41 | `EquivalentPieces.familyOf` |
| 19 | 82 | `LotLevel.flatGroundAt` |
| 18 | 48 | `ProfessionAssigner.vacancyFor` |
| 17 | 95 | `MinerSteps.step` |
| 17 | 57 | `ConstructionCancellation.cancelAtSoulTorch` |
| 17 | 20 | `ShepherdWork.woolOf` (troca por cor) |
| 16 | 90 | `VillageFoundation.ensure` |

Os dois primeiros são tabelas de mapeamento e cabem num `Map` ou `switch`.
`find`, `nextCut`, `flatGroundAt` e `step` são algoritmos de verdade e
concentram o risco.

### 4.3 Testes

| Medida | Valor |
|---|---|
| Testes unitários | 1.025, em 9,8 s |
| Testes de jogo | 433, em cerca de 54 s, 0 falha em 2 rodadas |
| Código de teste | 175 arquivos, 26.708 linhas de código |
| Cobertura de linha, `core` | 90,9% |
| Cobertura de linha, `data` | 96,3% |
| Cobertura de linha, `fabric` | 11,6% (não medida pela bateria de jogo) |
| PIT | 1.291 mutações, 1.000 mortas, 158 sobreviventes, 133 sem cobertura |
| Mais sobreviventes no PIT | `MineShaft` 37, `ProfessionAssigner` 13, `ColonyCycle` 12, `Building` 12 |

### 4.4 Análise estática (Error Prone 2.50, só avisos)

57 avisos. 39 deles são de javadoc (`MissingSummary` 14, `InvalidLink` 9,
`NotJavadoc` 7 e outros). Os de código são:
- `EnumOrdinal` 7: ordem de enum usada como prioridade, frágil se alguém
  reordenar;
- `ImmutableEnumChecker` 5;
- `LongDoubleConversion` 4;
- `UnusedVariable` 2;
- `IntLongMath` 2.

Nenhum aponta defeito de lógica.

### 4.5 Desempenho em jogo (sessão de 24-09, 6,7 h, 221.814 linhas de log)

- **Ciclo acima de um tique:** 196 vezes. O servidor não registrou nenhum
  `Can't keep up`, então o custo aparece como pico, e não como lag contínuo.
- **Repetições que o analisador marca como candidato a laço:**

  | Assinatura | Ocorrências |
  |---|---|
  | `builder_pathing_stalled` | 106 |
  | `construction_waiting_resources` | 79 |
  | `missing_profession` | 77 |
  | `site_sweep_budget_exhausted` | 58 |
  | `construction_let_go` | 13 |

- **`house_finished`:** 0, o E48 visto em números.
- **Ressalva:** a sessão é anterior às mudanças N1 a N11 e ao E47/E48. A nota
  C13 é da versão jogada, não do código atual.

### 4.6 Registros e processo

| Medida | Valor |
|---|---|
| ADRs | 24 |
| Documentos `.md` em `docs` | 95 |
| `TODO.md` | 968 linhas, 52 itens abertos (4 🔴) e 94 feitos |
| `STATE.md` | 659 linhas; o cabeçalho dele manda não passar de 150 |
| `Development-Log.md` | 9.325 linhas |
| Commits | 608 no total, 391 nos últimos 30 dias |
| Branch de trabalho | 85 commits à frente da `main`, que tem 0 à frente |
| Prefixo dos últimos 300 commits | 69 sem prefixo de tipo; os outros usam `docs`, `fix`, `P1`, `P0`, `feat`, `refactor`… |

---

## 5. Análise qualitativa

| # | Item | Nota | Evidência e leitura |
|---|---|---|---|
| Q1 | Modelo de domínio | **Forte** | O `core` tem modelo com nome do domínio (`Colony`, `Worker`, `ConstructionProject`, `Mine`, `ColonyGoals`, `ProfessionAssigner`) e nada de Minecraft (C04 = 0, `DependencyRuleTest`). Cada regra do autor tem ADR e nome ("Regra 27", "E48"). O ponto fraco é que parte da regra mora na camada `fabric`: rodízio de obra em `HousePlans`/`ConstructionOrder`, família de material em `MaterialChoice`. É decisão pura que caberia no `core`. |
| Q2 | Fluxo e estado | **Frágil** | São oito serviços globais em `VillageColonyMod` (`COLONIES`, `WORKERS`, `TASKS`…), 89 campos estáticos mutáveis e 49 classes com `clearAll()`. O `ServerLifecycleHandler` chama 78 limpezas à mão. A memória do projeto registra **seis canais de interferência** entre testes por esse estado (`gametest-interferencia-global`). Esquecer uma limpeza vaza estado entre mundos, e não há teste que pegue isso. |
| Q3 | Repetição entre ofícios | **Frágil** | São 8 classes com `Map<UUID, Job> JOBS`, 6 `giveUp`, 6 `new WorkStall()`. Cada profissão reimplementa achar alvo, andar, trabalhar, guarda de imobilidade, desistir e esquecer. Defeitos do mesmo desenho reaparecem por ofício (memória `castigo-por-oficio-vira-rodizio`; "o lenhador era o único dos seis que não zerava o `WorkStall`"). |
| Q4 | Robustez e recuperação | **Forte** | Há várias camadas de recuperação: guarda de imobilidade (300), guarda de travamento (2400), `WorkerStrikes`, encalhado que cava a saída, reparo cíclico, `SaveMigration` versionada, `BlockProtection` antes de todo bloco quebrado. O custo é a complexidade: parte dos E1–E48 nasceu da interação entre guardas. |
| Q5 | Desempenho | **Frágil** | Existe orçamento por passagem (`RingSweep`, `SEARCHES_PER_TICK = 1`), só colônias perto de jogador trabalham, e nunca se força chunk. Mesmo assim o ciclo passa do tique 29 vezes por hora (C13). Não há perfil gravado: não se sabe se o custo é varredura, leitura de baú ou planejamento. |
| Q6 | Nomenclatura | **Adequado** | Identificadores em inglês, descritivos e consistentes (`StrandedEscape`, `LotClearance`, `GatheringReach`); comentários em português. A mistura é deliberada e estável. O atrito é o comentário virar diário: são **982 datas** dentro do código de produção (`— 2026-09-15`), e o javadoc conta a história da decisão em vez de só dizer o contrato. |
| Q7 | Registros | **Adequado** | A cobertura é excelente: 24 ADRs, `PATTERNS.md` com assinatura de defeito, `STATE`/`TODO`/log de desenvolvimento, e uma memória de agente com quase 50 lições. A governança está no limite: `STATE.md` tem 4 vezes o próprio teto, o `TODO.md` quase mil linhas e o log de desenvolvimento 9 mil. A regra "uma fonte por assunto" convive com `Project-State.md` e `Backlog.md` marcados como históricos. |
| Q8 | Ferramentas | **Forte** | Tudo é reprodutível por comando: Gradle/Loom com versões fixas, JUnit 5 com `fabric-loader-junit`, GameTest, PIT, JaCoCo, Error Prone, depuração de mixin, CI, `release_manifest.py`, o analisador de log com histórico, spark e agora este coletor. |
| Q9 | Testes | **Adequado** | O teste prova comportamento (mutação à mão e PIT) e a bateria está estável depois do isolamento de 24-09. Três limites: (1) `runGametest` não filtra teste, então a volta é sempre de ~55 s; (2) arquivos de teste gigantes (`MinerGameTest` com 5,8 mil linhas); (3) a camada `fabric` não tem cobertura medida. |
| Q10 | Processo | **Adequado** | Commit por marco, verificação antes de commitar, hooks prontos. Mas o trabalho vive num branch que não volta para a `main` (85 commits), o CI só passou a rodar nele hoje, e a mensagem de commit mistura convenções (`P1:`, `E47:`, `feat:`, sem prefixo). |

---

## 6. Riscos priorizados

| Prioridade | Risco | Impacto | Probabilidade | Base |
|---|---|---|---|---|
| 🔴 | Custo do ciclo no tique do servidor | Picos no tique, mais graves com várias vilas carregadas | Alta (medido) | C13, Q5 |
| 🟠 | Estado global com limpeza manual | Vazamento entre mundos, interferência entre testes, defeito silencioso ao criar classe nova | Média | C05, Q2 |
| 🟠 | Ciclo de tarefa copiado em cada ofício | Correção feita num ofício e esquecida nos outros | Alta (já aconteceu) | Q3 |
| 🟠 | Branch de trabalho longe da `main` | Integração cara; a `main` não representa o que está publicado | Média | Q10 |
| 🟡 | Documentos de estado acima do próprio teto | O agente e o autor gastam contexto lendo histórico | Alta | Q7 |
| 🟡 | Camada `fabric` sem cobertura medida | Não se sabe o que a bateria de jogo realmente exercita | Média | C07 |
| 🟢 | 57 avisos de análise estática | Baixo: quase todos de javadoc | Baixa | C10 |

---

## 7. Recomendações, com critério de aceite para a próxima avaliação

| # | Ação | Aceite medível |
|---|---|---|
| R1 | Gravar perfil do spark no próximo playtest (`Profiling-spark.md`) e atacar os três nós mais pesados do ciclo | **C13 de 1 para ≥ 3** (≤ 5 ciclos acima de um tique por hora) |
| R2 | Juntar o estado global num contexto por servidor (`ColonyRuntime`) e registrar cada memória nele, em vez de 78 limpezas à mão | **C05 de 2 para ≥ 3** (≤ 3/kLOC); nenhum `clearAll` chamado à mão no ciclo de vida |
| R3 | Extrair o ciclo de tarefa comum (achar, andar, trabalhar, guardas, desistir) para uma base usada pelos 7 ofícios | Nº de `static final Map<UUID, Job> JOBS` de 8 para ≤ 2; nº de `giveUp` de 6 para 1 |
| R4 | Levar o branch para a `main` por PR com o CI verde, e fazer PR por marco daqui em diante | Distância `main...branch` ≤ 10 commits na próxima avaliação |
| R5 | Enxugar `STATE.md` até o teto dele (150) e mover a história para `Historico-2026-09.md` | `STATE.md` ≤ 150 linhas |
| R6 | Matar os sobreviventes do PIT em `MineShaft`, `ProfessionAssigner` e `ColonyCycle` | **C08 de 3 para 4** (≥ 85%) |
| R7 | Trocar as tabelas de `if` (`toResourceType`, `woolOf`) por `Map` ou `switch` | CC máx ≤ 20 |
| R8 | Ligar o JaCoCo no `runGametest` para medir a camada `fabric` | Cobertura do `fabric` medida (hoje, desconhecida) |
| R9 | Padronizar a mensagem de commit (`tipo(escopo): …`, com `E47`/`N1` no escopo) | "sem prefixo" < 10% nos últimos 300 |
| R10 | Mover a história datada dos comentários para ADR ou log de desenvolvimento quando o arquivo for tocado | C11 ≤ 0,6 (conceito 4) em até três avaliações |

**Ordem sugerida:** R1, R4 e R5 são baratos e destravam as outras. R2 e R3
são as duas refatorações de arquitetura de maior retorno; cada uma merece
uma ADR antes do código.

---

## 8. Limitações desta avaliação

- **C13:** mede a versão 0.3.0 jogada em 24-09 de madrugada, **antes** das
  mudanças N1 a N11 e do E47/E48. O código atual não foi jogado.
- **Complexidade:** métodos de classe aninhada e lambdas ficam fora da
  contagem, e a CC é aproximada (ver `METODOLOGIA.md`, §7).
- **C09:** duas rodadas verdes; com a taxa histórica de ~1 falha em 8
  rodadas, isso não prova que a intermitência acabou.
- **Q1 a Q10:** são julgamento. Cada um cita a evidência, mas outro avaliador
  pode dar nota vizinha; o que se compara entre avaliações é principalmente
  o scorecard.
- **Correção durante a avaliação:** 64 links de javadoc quebrados pela
  refatoração foram corrigidos (62 reapontados, 2 trocados por `{@code}`),
  com build e as duas rodadas da bateria refeitos em seguida. Por isso os
  avisos contam 57, e não 121.
