# D2 — Correção e regressões funcionais

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `minecraft-modding` e `minecraft-testing`, que leram o GameTest e o
registro da bateria; `minecraft-performance-profiling`, que leu os ledgers de jogo
(Regra 50).
**Nota da dimensão:** 5/10. A bateria automática está verde, mas o jogo real
reprova a Regra 51 em metade dos ofícios.

## Fontes

| Fonte | O que é |
|---|---|
| `build/audit/raw/gametest-{1,2,3}.log`, CI `b290ed10` | bateria de jogo: **690/690** em 3 rodadas locais + 1 no CI + 1 com Fabric API 0.116.17 |
| `%APPDATA%/.minecraft/logs/2026-10-10-6.log.gz` (SHA-256 `37eff502…`) | playtest de 10-10, 10:45–11:51, **villagecolony 0.3.22** |
| `villagecolony-actions-20261010-104617.jsonl` | diário de ações da mesma sessão |
| `scripts/time_ledger.py`, `scripts/action_report.py` | réguas da Regra 50 e da Regra 51 |

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-01 | **P0** | **Regra 51 reprovada em 4 de 8 ofícios** no último playtest completo (0.3.22) | `time_ledger`: SMELTER 86% ocioso + bloqueado, BUILDER 68%, MINER 21% (16% bloqueado + encalhado: travamento), FARMER 16%. `action_report`: o fundidor **pegou 883 tarefas e soltou 882** em 60 min (`SMELT_MATERIAL glass` e `iron_ingot` em roda); um FARMER soltou 198 tarefas de fundição; MINER 42× `TARGET_REJECTED: TOO_HIGH` | O ciclo central do mod, aldeões trabalhando sozinhos, não se sustenta em jogo. O jogador vê fundidores parados e obra esperando material | Medir a 0.3.23 (P0.55 suprimento do fundidor; P0.56 argila e barn), que **ainda não foi jogada**. A bump da 0.3.22 (`3c4a30fb`) não contém P0.55; o JAR jogado foi regerado às 10:26 a partir de árvore não registrada, então não dá para provar o que ele trazia | `check-tick-budget.sh --log <log 0.3.23>` sem `REGRA 51`; `action_report` com SMELTER soltas/pegas < 10% (TODO, seção "Playtest da 0.3.23") |
| AUD-13 | P2 | "Alternância de obras": os documentos dizem que a regressão é reproduzível, mas o teste dela passa | `README.md:225-227`; `docs/behavioral-tests/README.md:16-17` (lista `FarmPlanGameTest.theNextTurnAfterAHouseIsNonResidential` como falha obrigatória); o teste (`FarmPlanGameTest.java:297`) passou nas 5 rodadas (`build/gametest-report.xml`, sem `failure`). O P0.54 (`a80c8cf4`) trata de resgate de obra em `WAITING_RESOURCES`, **não** da alternância | Documento desatualizado manda caçar defeito que a bateria não reproduz, e esconde que falta a prova em jogo | Marcar a lacuna 1 do `behavioral-tests` como coberta pelo GameTest, com a data; abrir item de playtest "casa → não residencial → casa → outro tipo" | Playtest com 4 obras seguidas na mesma colônia; `grep "opened"` no log mostra a sequência |

### Veredito da 0.3.23 (sessão de 10-10, 21:24–22:06)

`check-tick-budget.sh --log build/audit/playtest-2026-10-10-noite-0.3.23.log --journal
villagecolony-actions-20261010-212530.jsonl` (`build/audit/tick-budget-0.3.23.txt`):
8 janelas de 5 min, 8 amostras de custo, 39,2 min de diário.

- Regra 51 **ainda reprovada em 3 ofícios**: SMELTER 91%, BUILDER 92% (13% bloqueado),
  MINER 17% (14% bloqueado). O FARMER passou (10%).
- Fundidor: **311 pegas, 0 feitas, 311 soltas** (5 fundidores). A P0.55 não curou a roda.
- Ciclo: p95 27,7 ms, abaixo do tique. O AUD-04 não se repetiu nesta sessão.

### A cadeia por trás do AUD-01 (as duas sessões)

O fundidor e o construtor param **por falta de material**, e o material falta
porque o mineiro não cava. O defeito é da cadeia, e a raiz é a mina (memória
"cadeia de produção é recursiva"):

| Elo | 0.3.22 (manhã) | 0.3.23 (noite) | Fonte |
|---|---|---|---|
| Mineiro: `no mine mouth, and no exposed stone … the budget ran out before an answer` | 115× | 50× | `MineVein.java:184-191`; log |
| Mineiro: `MINED` por minuto | 0,8 | 0,1 | `action_report` |
| Mineiros que "saíram do ofício" (`HiringLog BETWEEN_TRADES`) | 20 | 19 | log `Colony … hiring` |
| Fundidor parado por cru ausente (pedregulho, areia, pedra, ferro) | 48× | 12× (ferro) | `SmelterWork`, log `Smelter … stopped — none of N colony chests had …` |
| Construtor `SET_ASIDE: no material` | 22× | 3× | `action_report` |

Hipótese a provar primeiro (Fase 2 do roteiro): a colônia tem `Mine` registrada
(`Mine … surface entrance is remembered`), mas o mineiro não a encontra como boca e
cai na busca de pedra de superfície, que esgota o orçamento a cada passagem. Enquanto
isso, a Regra 51 (`IdleYield`) tira mineiros de um ofício que está esperando, e não
ocioso por escolha.

**Correção existente fora da `main`:** `cd7ca2c8` (`codex/fluxo-grafo-waypoints`) faz
o fundidor manter a tarefa enquanto o cru falta. O próprio commit avisa que isso só
reduz o ócio "quando houver cadeia física de minério até os baús" (ADR-040, também só
no Codex). Ver `docs/technical/Roteiro-Correcoes-2026-10-10.md`.

**Lacuna de telemetria (AUD-25):** o diário grava `TASK_RELEASED` com
`detail: RESERVED` (o estado), não o motivo. As 882 solturas da manhã não dizem por
que aconteceram; o motivo só aparece cruzando com o log do servidor.

## Regra 48 e Regra 51: o que existe

| Regra | Onde está | Teste espelho | Prova em jogo |
|---|---|---|---|
| 48: ocioso recolhe do chão o que a obra precisa | `fabric/work/GroundPickup.java:32-35`, chamada em `StrandedEscape.java:95-96` | `GroundPickupTest:36`, `GroundPickupGameTest:37` (asserção cita a Regra 48 em `:123`) | não medida separadamente |
| 51: ocioso + bloqueado + encalhado ≤ 15% | ledger: `scripts/time_ledger.py:35,43,45`; cessão de vaga: `core/coordination/IdleYield.java:22,51` | `tests/test_time_ledger.py:18-24` (limiar 15% exato); `IdleYieldTest:21,40,49`; `IdleYieldGameTest:23` | **reprovada** (AUD-01) |

**Discordância entre as duas metades da Regra 51:** `IdleYield` cede trabalhador só
por **ócio puro** (`WorkTime.java:195` usa só `State.IDLE`), enquanto o ledger
reprova **ocioso + bloqueado + encalhado**. Um ofício com 40% bloqueado e 0%
ocioso passa no jogo e reprova na régua. Isso faz parte de AUD-01: a correção
precisa decidir qual das duas é a regra.

## Fluxos pendentes de playtest

`STATE.md` e `TODO.md` listam **12 entregas** entre a 0.3.5 e a 0.3.23 ainda não
vistas em jogo (TODO:9, 19, 28, 42, 48, 55, 62, 66, 74, 85, 117, 129). Uma sessão
com a 0.3.23 começou às 21:24 de 10-10, durante esta auditoria; até 21:32 o
log tinha uma única amostra de custo, insuficiente para veredito.

## Não verificado

- Reprodução da alternância **em jogo**: exige playtest.
- Se a sessão 0.3.23 em curso já mostra o fundidor curado: só ao fim dela.
