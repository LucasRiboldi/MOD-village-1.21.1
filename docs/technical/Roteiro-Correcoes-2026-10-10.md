# Roteiro de correções e melhorias — auditoria de 2026-10-10

**Proposto por Claude a partir da auditoria técnica de 2026-10-10** (commit `b290ed10`,
JAR 0.3.23). **As decisões da Fase 0 foram tomadas pelo autor em 10-10 e estão na
[ADR-041](../decisions/ADR-041-decisoes-da-auditoria-2026-10-10.md)**: liberar a auditoria,
integrar a linha Codex, regra do ócio abrangente, `/vc log` aberto com dados amplos.

Este arquivo é o **roteiro**: o que fazer e em que ordem. O estado de cada
item continua no [`TODO.md`](../../TODO.md) (seção "Auditoria técnica 10-10"), e
a evidência de cada achado está em [`avaliacao/ACTION-QUEUE.md`](avaliacao/ACTION-QUEUE.md)
e nos arquivos `avaliacao/D01`–`D12`. O [`Plano-de-Correcao.md`](Plano-de-Correcao.md)
de 09-11 continua valendo para os itens dele; este roteiro não o substitui.

> **Validado contra o código antes de virar trabalho** (a lição do P0.0 de 09-11:
> 12 itens vencidos contra 7 entregues). Em 10-10 a validação mudou duas
> prioridades. (1) A correção do fundidor em roda **já existe**, mas só na
> `codex/fluxo-grafo-waypoints` (`cd7ca2c8`); por isso a Fase 0 vem antes da Fase 2.
> (2) O pico de 67 ms do ciclo **não se repetiu** na sessão 0.3.23, e o AUD-04 virou
> medida antes de otimização.


## A régua

A do `Plano-de-Correcao.md` vale inteira: teste verde não é pronto, a cadeia
vem antes do sintoma, nenhum limite é aumentado para teste passar. Mais quatro,
desta auditoria:

1. **Toda correção de fluxo mede o tempo dos aldeões** (Regra 50). Depois de cada
   playtest: `bash scripts/audit/check-tick-budget.sh --log <latest.log> --journal <actions.jsonl>`.
   A correção está provada quando a proporção cai no playtest seguinte.
2. **Antes de cada item, as quatro perguntas** do `CLAUDE.md` §0.3: problema,
   sistema, arquivos, decisão arquitetural. Elas estão respondidas abaixo; se a
   resposta mudar ao abrir o código, o item volta para cá antes de virar commit.
3. **Um item por commit**, no formato `AUD-NN: <o que mudou> (<teste que prova>)`.
   Não agrupar AUD-02 com AUD-03, mesmo sendo a mesma classe.
4. **Uma linha de desenvolvimento por vez** (ADR-035 §2). Nada da Fase 2 começa
   enquanto a Fase 0.2 não estiver decidida.

**Antes de começar cada fase:** `bash scripts/audit/check-tests-green.sh`. Se
estiver verde, seguir; se estiver vermelho, parar e reportar.


## Fase 0 — Consolidar antes de qualquer código

Sem jogo, sem código de produção. Tudo aqui é decisão ou integração.

### 0.1 — Salvar a auditoria
**O quê:** commitar a branch `claude/auditoria-tecnica-2026-10-10` (documentos,
`scripts/audit/`, skills). Está pronta e verificada, mas não foi commitada, como o
pedido da auditoria mandava. **Decisão do autor:** liberar o commit e o PR.

### 0.2 — AUD-05: a linha Codex
| | |
|---|---|
| Problema | 14 commits e a ADR-040 vivem só na `codex/fluxo-grafo-waypoints` (worktree ativo, último commit 20:34 de 10-10). A PR #19 dela foi fechada às 20:53 como "substituída pela integração reconciliada na main (0.3.23)", mas a reconciliação **não trouxe** `ConstructionSupplyDispatches`, `AdvanceConstructionStock`, a ADR-040 nem a retenção do fundidor (conferido por conteúdo). Desde o ponto de divergência (`399113f0`, 07-10) ela mudou 81 arquivos, 47 dos quais a `main` também mudou |
| Por que primeiro | ela contém duas peças de que a Fase 2 precisa: `cd7ca2c8` (o fundidor mantém a tarefa enquanto o cru falta) e o despacho físico de material da ADR-040 (`94c51267`, `9fa820f0`, `4b0a59d9`) |
| Decisão do autor | **Integrar (ADR-041 #2).** Merge de três vias numa branch a partir da `main`, sem reescrever a do Codex; resolver os conflitos com o `133c69db`, que trouxe parte dela à mão; rodar a bateria 3×; abrir PR |
| Pronto quando | `git cherry -v main codex/fluxo-grafo-waypoints` vazio, ou a branch apagada com uma nota no `STATE.md` dizendo o que foi descartado e por quê |

### 0.3 — Regra 51: qual é a régua
| | |
|---|---|
| Problema | as duas metades da Regra 51 discordam. `IdleYield` cede trabalhador só por **ócio puro** (`WorkTime.java:195`); o `time_ledger` reprova **ocioso + bloqueado + encalhado**. Com a `cd7ca2c8`, o fundidor que espera minério passa a contar como "espera", e a espera não entra em nenhuma das duas contas |
| Decisão do autor | **(a), a abrangente (ADR-041 #3):** ocioso + bloqueado + encalhado ≤ 15%; o `IdleYield` passa a usar a mesma soma do ledger |
| Risco | ceder trabalhador de um ofício que **espera a cadeia** é castigo no ofício errado (memória "castigo por ofício vira rodízio"). As duas sessões de 10-10 registram cerca de 20 mineiros saindo do ofício (`MINER just left a trade`) enquanto o mineiro não achava boca de mina |

### 0.4 — AUD-24 e AUD-17
`/vc log` **continua aberto e ganha dados amplos** (ADR-041 #4; vira o AUD-19). Apagar as 7
branches já contidas na `main` ficou sem resposta e não é feito.

### 0.5 — AUD-26: duas "Regra 51"
A regra do ócio (`CLAUDE.md` §0.4, `time_ledger.py`, `STATE.md`) usa o número da Regra 51 do
`docs/RULES.md` (peça na 4ª tentativa, 06-10), e não está no `RULES.md`. **Decisão do autor:**
qual número ela recebe. Até lá, o código e os documentos novos dizem "regra do ócio".


## Fase 1 — Proteger o save

Independe de jogo e da Fase 0.2. Esforço: meio dia. Classe `data/save`.

### AUD-03 — gravar `saveVersion`
| | |
|---|---|
| Problema | `SaveMigration.migrate` põe a versão só na cópia em memória (`SaveMigration.java:67-75`); `ColonySavedData.writeNbt` (`:363`) não a grava. A v0→v1 roda a cada carga |
| Sistema / arquivos | persistência: `data/save/ColonySavedData.java`, `data/save/SaveMigration.java` |
| Decisão arquitetural | não; cumpre a "decisão 8A" de 09-24, que criou a versão |
| Passos | 1) teste que falha: `writeNbt(...).getInt("saveVersion") == SaveMigration.CURRENT`; 2) `nbt.putInt("saveVersion", SaveMigration.CURRENT)` no `writeNbt` (as duas classes estão em `data.save`, então `CURRENT` já é visível; a chave pode virar constante compartilhada); 3) teste de ida e volta: um save gravado e relido não roda migração (`migrate` devolve a versão sem passar pelo laço) |
| Teste que prova | `ColonySavedDataTest`; mutação: remover o `putInt` derruba o teste |
| Commit | `AUD-03: gravar saveVersion no save (ColonySavedDataTest)` |

### AUD-02 — leitura que não apaga o mundo
| | |
|---|---|
| Problema | qualquer exceção em `ColonySavedData.readNbt` faz o `PersistentStateManager` criar estado vazio e gravá-lo por cima (bytecode conferido). Lançam hoje: `ResourceId.parse` sem proteção (`ColonySaveReader.java:93,117,161`), com dois `:` ou nome em branco; `saveVersion < 0` (`SaveMigration.java:82-83`); `MinecraftTypeAdapter.toBlock` (`:245`), com caractere inválido |
| Sistema / arquivos | `data/save/ColonySaveReader.java`, `data/save/SaveMigration.java`, `fabric/adapter/MinecraftTypeAdapter.java` |
| Decisão arquitetural | **sim, pequena**: "entrada ruim é descartada com `WARN`, nunca lança". Registrar no javadoc de `ColonySaveReader` com a regra; ADR só se o autor quiser |
| Passos | 1) mover `scripts/audit/probes/NbtFuzzProbeTest.java` para `src/test/java/com/villagecolony/data/save/` (12 casos; hoje 6 falham); 2) em cada laço de `ColonySaveReader`, envolver a leitura de **uma** entrada em `try/catch (RuntimeException e)`: `WARN` com a chave e o índice, e `continue`; 3) `SaveMigration`: `from < 0` é tratado como 0; 4) `toBlock`: `Identifier.tryParse(namespace, path)` (existe no 1.21.1) e `Optional.empty()` quando devolver `null`, como o javadoc promete; 5) estender a sonda aos leitores NBT de `fabric/work` (`MineVein`, `MineClaims`, `ShepherdHerding`, `FarmOwners`, `SurfaceHits`, `VillageTrees`, `WorkMemory`) |
| Teste que prova | `NbtFuzzProbeTest` 12/12 dentro da bateria; `check-nbt-migration.sh` sem falhas |
| Commit | `AUD-02: entrada ruim do save é descartada, não apaga a colônia (NbtFuzzProbeTest)` |


## Fase 2 — A cadeia de produção (AUD-01, o P0)

Depende da Fase 0.2 (a correção do fundidor e a ADR-040) e da 0.3 (a régua).
Esforço: vários dias, com playtests. **Cada passo termina num playtest medido.**

O defeito é da cadeia, não do fundidor. Nas duas sessões de 10-10:

```text
mineiro sem boca de mina  →  sem pedra, sem ferro  →  fundidor sem cru  →  construtor sem material
(no mine mouth 115× / 50×)    (MINED 0,8 → 0,1/min)   (48× / 12× "none of N    (SET_ASIDE no material
                                                       colony chests had …")    22× / 3×)
```

### 2.1 — AUD-25: o diário diz por que soltou
| | |
|---|---|
| Problema | `TASK_RELEASED` grava o estado (`RESERVED`), não o motivo; as 882 solturas da manhã não dizem a causa |
| Sistema / arquivos | telemetria: `ActionJournal`, os pontos de `release` de cada `*Work`, `scripts/action_report.py` |
| Decisão arquitetural | não |
| Passos | motivo no campo `detail` (o mesmo texto do `StopLog`/`giveUp`); o `action_report` agrupa as solturas por motivo |
| Teste que prova | `tests/test_action_report.py` com soltura e motivo; GameTest do fundidor confere o `detail` |
| Por que antes | sem isto, o playtest de 2.2 repete a investigação por cruzamento de log |

### 2.2 — O mineiro não acha a boca da mina
| | |
|---|---|
| Problema | a colônia tem `Mine` registrada (`Mine … the surface entrance is remembered at …`), mas o mineiro cai na busca de pedra de superfície (`MineVein.java:184-191`), cuja varredura em anéis esgota o orçamento a cada passagem: `no mine mouth, and no exposed stone … the budget ran out before an answer` |
| Sistema / arquivos | mina: `fabric/work/MineVein.java`, `MineDigging.java`, `core/construction/model/Mine.java` (`entry()`), `MineRecovery` |
| Decisão arquitetural | não, até o diagnóstico dizer o contrário |
| Passos | **diagnóstico primeiro** (skill `superpowers:systematic-debugging`): 1) no save de 10-10, comparar a `Mine` registrada (`world_survey.py` ou `/vc log`) com o que `MineVein` consulta como boca; 2) a hipótese a refutar é que `Mine.entry()` aponta para o nível atual (memória "mineiro pela boca") e a pergunta "há boca?" usa outro critério; 3) GameTest que reproduz: mina registrada com a boca recordada e o nível abaixo, e o mineiro recebe alvo de mina, não de superfície; 4) só então corrigir |
| Teste que prova | o GameTest novo; mutação (desligar a correção derruba só ele) |
| Prova em jogo | `no mine mouth` = 0 com `Mine` registrada; `MINED` por minuto acima do 0,8 da 0.3.22; MINER fora da Regra 51 |

### 2.3 — A Regra 51 não pune o ofício que espera a cadeia
| | |
|---|---|
| Problema | cerca de 20 mineiros por sessão "saíram do ofício" enquanto esperavam a varredura. Hipótese: `IdleYield` (`IdleYield.java:22,51`) cede vaga de um ofício bloqueado por falta de alvo, o que reduz a produção da raiz da cadeia |
| Depende de | a decisão 0.3 |
| Passos | contar, no log, quem saiu do ofício e o motivo do ócio naquela janela; se a hipótese se confirmar, o `IdleYield` não cede em janela cujo ócio é `SWEEP_INCOMPLETE` ou falta de cru |
| Teste que prova | `IdleYieldTest`: ócio por espera de cadeia não cede; ócio puro cede (o caso atual continua) |

### 2.4 — Fundidor e despacho físico
Com a Fase 0.2 integrada, a `cd7ca2c8` mantém a tarefa durante a falta, e a ADR-040
leva o material até a obra. **Não reescrever** o que já está lá: medir.
**Prova em jogo:** fundidor com conclusão ≥ 50% no `action_report`, solturas
abaixo de 10% das pegas, SMELTER fora da Regra 51.

### 2.5 — Playtest de aceite do AUD-01 (e do AUD-13)
Sessão de pelo menos 60 min na versão com as Fases 1, 2.1–2.4.
`check-tick-budget.sh` sem `REGRA 51`; o mesmo playtest registra 4 obras seguidas
na mesma colônia (`grep opened`) para fechar a alternância do AUD-13.


## Fase 3 — Rede de testes

Sem jogo; pode correr em paralelo com a Fase 2 (classes diferentes).

| Item | O que fazer | Arquivos | Teste que prova | Esf. |
|---|---|---|---|---|
| **AUD-06** | Uma asserção por material em `constructionMaterials` (pedra, tábua, lã, vidro, carvão, `smelted`, `surfaceGathered`) e o caso do ferro (`max` do lingote, nunca a soma) | `src/test/.../core/coordination/WorkDemandTest.java` | `check-mutation-threshold.sh --run`: 0 sobreviventes em `WorkDemand.constructionMaterials`; folga do PIT ≥ 20 mutantes | P |
| **AUD-11** | Unitário do `ProfessionPolicyPayloadCodec` (limites, duplicata, ordem); GameTest de `applyUpdate` com jogador nível 0 e nível 2; teste do `VillageLogPresenter` | `fabric/network`, `fabric/command` | `check-coverage.sh`: `fabric.network` ≥ 60% | M |
| **AUD-07** | Régua v2: C14 conta itens sob seção 🔴; C01 conta linhas como o `wc -l`; `print` seguro em cp1252. Recalcular 09-24 e 10-10 (METODOLOGIA §8) | `scripts/assess/assess_project.py:187,280`, `java_metrics.py:87`, `tests/test_assess_rubric.py` | os casos novos no `test_assess_rubric.py` | P |
| **AUD-21** | 8 rodadas da bateria antes de declarar a intermitência histórica curada | — | `check-tests-green.sh --runs 8` sem falha | P |


## Fase 4 — Desempenho (condicionada)

| Item | Gatilho | O que fazer |
|---|---|---|
| **AUD-04** | o próximo playtest com ≥ 20 amostras de `VC_COST` mostra p95 do ciclo > 50 ms | instrumentar sub-fases do planejador (ADR-035 §5: sem número, sem otimização); fatiar o planejador entre tiques para que o prazo de 15 ms valha para a passagem inteira |
| **AUD-16** | um Spark mostra `CraftingLookup` no topo | cache item → entradas de fundição, limpo em `ServerMemory` |

Sem gatilho, nada desta fase vira trabalho.


## Fase 5 — Estrutura e compatibilidade (dívida)

Sem pressa, um item por vez, nunca no meio de uma correção da Fase 2.

| Item | O que fazer | Decisão arquitetural |
|---|---|---|
| **AUD-12** | `"suggests": {"modmenu": "*"}` no `fabric.mod.json`; `WARN` uma vez por aldeão quando um trabalhador registrado não tem a tarefa da colônia no `Brain`; GameTest que limpa o `Brain`. Lista de `breaks` só com mod testado (nível 4 do modsmith) | não |
| **AUD-23** | javadoc de `VillagerEntityMixin.java:14` ("o único mixin") | não |
| **AUD-22** | `BigHouseFoundation.java:79`: `world.getRandom()` ou semente da colônia | não |
| **AUD-15** | `RemoveUnusedImports` do Error Prone como erro; um commit só de limpeza | não |
| **AUD-20** | dividir por eixo de domínio as 15 classes em 488–500 linhas, **antes** da próxima mudança em cada uma, não durante | não |
| **AUD-10** | esvaziar o registro congelado da regra `aStaticMutableCollectionIsServerMemory`, que já exige o `ServerMemory` (a auditoria a dava como inexistente) | não |
| **AUD-09** | tirar os registros globais de `VillageColonyMod` (`WORKERS`, `COLONIES`, `CONSTRUCTIONS`…) para um contexto de serviço, quebrando o ciclo raiz ↔ `fabric` | **sim, ADR**: é a mudança mais larga do roteiro; depende do AUD-10 |


## Fase 6 — Documentação e release

| Item | O que fazer | Quando |
|---|---|---|
| **AUD-08** | arquivar em `docs/archive/technical/Historico-2026-10.md`, sem editar, as seções fechadas de 02-10 a 08-10 do `STATE.md` (663 → ≤ 150 linhas) | logo depois da Fase 0.2, que mexe no `STATE` |
| **AUD-14** | entradas 0.3.1–0.3.23 no CHANGELOG, a partir dos commits e do `STATE` | a qualquer momento |
| **AUD-13** | atualizar `docs/behavioral-tests/README.md` (a lacuna 1 está coberta pelo GameTest) | junto com o playtest 2.5 |
| **AUD-18** | tag leve por JAR copiado para `.minecraft` (`scripts/release_manifest.py` já dá o SHA) | a cada publicação |
| **AUD-19** | `/vc log` com dados amplos (ADR-041 #4): uma linha por ofício com a última janela do `WorkTime` (trabalho, caminhada, espera, bloqueio, ócio, encalhe) e a marca da regra do ócio acima de 15% | junto com a 2.3, que muda a mesma conta |
| **AUD-17** | ✅ decidido: o `/vc log` continua aberto (ADR-041 #4); nada a mudar no código | — |

**Melhoria proposta (não é achado):** levar ao CI os `check-*.sh` que não precisam de
jogo, como `check-mixin-safety.sh` e `check-docs-drift.sh`. Este segundo depende da
correção do AUD-14 e do AUD-08 para ficar verde.


## Ordem de execução

```text
bateria verde (check-tests-green.sh)
  Fase 0   0.1 commit da auditoria          ← decisão do autor
           0.2 AUD-05 linha Codex            ← decisão do autor; bloqueia a Fase 2
           0.3 Regra 51: qual régua           ← decisão do autor; bloqueia 2.3 e AUD-19
           0.4 AUD-24, AUD-17                 ← opcionais
  Fase 1   AUD-03 → AUD-02                    (sem jogo; pode começar já)
  Fase 3   AUD-06 → AUD-07 → AUD-11 → AUD-21  (sem jogo; em paralelo com a Fase 2)
  Fase 2   2.1 AUD-25 → 2.2 mineiro → 2.3 IdleYield → 2.4 medir fundidor → 2.5 playtest de aceite
  Fase 4   AUD-04 / AUD-16, só com gatilho
  Fase 6   AUD-08 (depois da 0.2) · AUD-13 (com a 2.5) · AUD-14 · AUD-18 · AUD-19 · AUD-17
  Fase 5   AUD-12 → AUD-23 → AUD-22 → AUD-15 → AUD-20 → AUD-10 → AUD-09 (ADR)
  → gauntlet-verifier sobre o conjunto
```

Primeiras sessões, de forma realista:

| Sessão | Conteúdo | Saída esperada |
|---|---|---|
| 1 | decisões 0.1–0.3; AUD-03, AUD-02, AUD-06 | save protegido, PIT com folga, linha Codex resolvida |
| 2 | AUD-25; diagnóstico 2.2 com GameTest que reproduz | causa do `no mine mouth` provada, não suposta |
| 3 | correção 2.2 + 2.3; playtest de 60 min | primeiro `time_ledger` pós-correção |
| 4+ | o que o playtest da sessão 3 mandar; Fase 3 e Fase 6 entre playtests | — |


## Decisões do autor que este roteiro espera

1. ✅ Liberar o commit e o PR da auditoria (0.1) — ADR-041 #1.
2. ✅ Integrar a `codex/fluxo-grafo-waypoints` (0.2, AUD-05) — ADR-041 #2.
3. ✅ Regra do ócio abrangente (0.3) — ADR-041 #3.
4. ✅ `/vc log` aberto, com dados amplos (AUD-17, AUD-19) — ADR-041 #4.
5. ⬜ Apagar as 7 branches já contidas na `main` (AUD-24).
6. ⬜ O número da regra do ócio (AUD-26).
7. ⬜ A ADR do AUD-09, quando chegar a vez dele.


## Riscos do roteiro

- **A Fase 2.2 pode revelar outra causa.** O roteiro trata "mineiro sem boca de mina"
  como hipótese, não como fato. Se o diagnóstico derrubar a hipótese (memória
  "frase ausente refuta mecanismo"), a 2.2 volta para cá antes de virar código.
- **Integrar a linha Codex pode conflitar com o `133c69db`**, que trouxe parte dela à
  mão. É por isso que a 0.2 pede a bateria 3× antes do PR.
- **Duas sessões não decidem o AUD-04.** Otimizar sem o terceiro número é otimizar
  por palpite.


## Estado da execução

Atualizado a cada item entregue. ✅ só com bateria verde e mutação conferida; a
coluna "em jogo" é a que a régua cobra.

| Item | Estado | Em jogo |
|---|---|---|
| 0.1 commit da auditoria | ⬜ liberado (ADR-041 #1) | — |
| 0.2 AUD-05 | ✅ integrada na branch `claude/integra-fluxo-grafo-waypoints` (0.3.24): 1402 unitários, 696/696 GameTests × 3, PIT 85,56% | ⬜ espera playtest |
| 0.3 regra do ócio | ✅ decidida: abrangente (ADR-041 #3) | — |
| AUD-03 | ✅ `11bd4799` — `SaveMigration.stamp` no `writeNbt`; mutação conferida | — |
| AUD-02 | ✅ leitura por entrada e por seção, versão negativa = 0, `toBlock` sem lançar; `ColonySaveFuzzTest` 13/13, `WorkMemoryLoadTest`; mutação conferida | — |
| AUD-06 | ✅ `WorkDemand` 20/20 mutantes mortos; PIT 1820/2112 (86,17%), folga 24 | — |
| AUD-07 | ✅ régua v2; `tests/test_assess_counts.py`; 17613fa e b290ed10 recalculadas: 3,14 e 3,14 | — |
| AUD-11 | ⚠️ parcial: codec, save e permissão testados (mutação conferida); `fabric.network` 42% / 46%, abaixo dos 60%; ordem ruim no save não apaga mais as regras do Mod Menu | — |
| AUD-21 | ⬜ | — |
| AUD-25 | ⬜ | ⬜ |
| 2.2 mineiro | ⬜ | ⬜ |
| 2.3 IdleYield | ⬜ | ⬜ |
| 2.4 fundidor | ⬜ (código na linha Codex) | ⬜ |
| AUD-01 aceite | ⬜ | ⬜ 0.3.22 e 0.3.23 reprovadas |
| AUD-04 | ⬜ medir | ⬜ 67 ms (0.3.22) · 27,7 ms (0.3.23) |
| AUD-13 | ⬜ | ⬜ |
| Fase 5 e Fase 6 | ⬜ | — |
