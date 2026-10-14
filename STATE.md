# STATE — 2026-10-14

> **0.3.58** (14-10): playtest da 0.3.57 (Spark `iALIIoOwEK`, 42 min) — a horta destravou (obra
> avança, 201 peças, TPS 20), mas **ociosidade geral alta**: lenhador 98% ocioso, pedreiro 71%,
> pastor 80%, fundidor 60× "no task open". Raiz medida: a **vila está madura e sem obra** — 21×
> "no empty plot", estoque cheio (837 cobble, 322 toras); os produtores param porque não há obra
> pedindo, não por falta de material. E a **expansão era lenta**: esperava 3 voltas de varredura
> (`FULL_AGAIN_AFTER=3*FULL_AFTER`, dezenas de min) entre anexos. **Correção (decisão do autor):**
> `FULL_AGAIN_AFTER = FULL_AFTER` — a vila anexa a faixa já na volta que confirma a falta de lote,
> crescendo no ritmo em que fica ociosa; a salvaguarda contra inchaço (só lado plano/seco/sem outra
> colônia) permanece. **Não jogado.** Verificado: test 1515/0; runGametest 769/769. JAR
> `village-colony-0.3.58.jar` = `429511EA11985FA09724E6A69F5E4C77A222DBB4DD6824709ABF0FDBD39BA659`.
> **Aberto:** o mineiro ainda tem 172× NO_STANDING_POSITION (44% bloqueado) — o único ócio por
> defeito real, não por vila madura.

> **0.3.57** (14-10): playtest da 0.3.56 (Spark `hquYC03AAc`, 96 min) — **a vila parou**: a obra
> `plains_small_house_8` travou nos últimos ~12 blocos (a **horta** da casa: 35 farmland + 16 wheat
> + 2 water) e, aberta para sempre, bloqueou o planejador → lenhador 100% ocioso, pedreiro 99%,
> construtor 75%, pastor 83%, fundidor 0%. Raiz: `farmland` não é fabricável ("could not make
> minecraft:farmland") nem coletável, e o assentamento falhava em loop. **Correção (decisão do
> autor):** a horta **surge sem gastar recurso** — `BuilderPlacement` molda no local os blocos de
> chão (farmland, água, caminho, cultivo) com o terreno garantido embaixo (`BlockShaping.groundFor`),
> antes da cadeia de material; só sobre lugar livre (Regra 3 preservada). **Não jogado.** Verificado:
> test 1515/0; runGametest 769/769; mutação (desligar o surgimento) derruba 6 testes de construção.
> JAR `village-colony-0.3.57.jar` = `4DB9A471779F49D62D11A91C381C34193774343A65CC100E90BEE7A0850D6F69`.

> **0.3.56** (14-10): **o pastor aproveita o rebanho** (manutenção do ofício, não toca a obra —
> Regra 59). `FlockHarvest`: com mais de `HARVEST_KEEP=8` adultos de uma espécie (a criação enche
> até `FLOCK_TARGET=12`), abate **1 por ciclo** o excedente deixando sempre ≥8 vivos, e deposita o
> drop Vanilla direto no baú (vaca→couro+carne, porco/ovelha→carne, galinha→frango+pena). A **carne
> crua acima de `ITEM_KEEP=16` é cozida** (`CraftingLookup.smelted`, sem combustível — como o
> fundidor; nenhuma profissão precisa de carvão). O ovo a galinha já larga no chão (Vanilla) e a
> coleta do chão recolhe. Ligado em `ShepherdFlock.tend`. **Não jogado.** Verificado: test 1515/0;
> runGametest 768/768; mutação (HARVEST_KEEP=0) derruba `FlockHarvestTest`. Spec em
> `docs/superpowers/specs/2026-10-14-pastor-aproveita-rebanho-design.md`. JAR
> `village-colony-0.3.56.jar` = `9BE8707A710E4DA6D12D0D92DCCD82C675254680D48FC709E1C8CED18A2F2F55`.

> **0.3.55** (14-10): **percurso por etapas até a boca da mina** (planta do autor, abordagem A
> híbrida; spec em `docs/superpowers/specs/2026-10-14-percurso-descida-mina-design.md`). O mineiro
> não alcançava a frente quando a boca ficava longe numa vila grande (playtest `MiIM0AisSh`: alvo a
> ~95 blocos, "walking to nowhere", MINER 0%). `MineApproachPath.toward`: quando a navegação não
> alcança a boca inteira, o mineiro anda até o **fim do caminho parcial** que o pathfinder achou
> (`Path.getEnd()`, posição caminhável real — nunca divisão geométrica), com progresso útil mínimo;
> recalcula ao chegar. `path==null` devolve a boca e deixa o guarda de descida (`job.descent`) contar
> a falha — sem segundo sistema de travamento. Interposto em `MinerSteps` no ponto da descida;
> `forget` ao liberar a tarefa (`MinerHands`). Mantém a ADR-042 (túnel 1×2, espaçamento 4, boca na
> borda); não reescreve a mina. A planta completa (espiral externa, galerias radiais 3×3) fica como
> visão de longo prazo, fora de escopo. **Não jogado.** Verificado: test 1512/0; runGametest 766/766;
> mutação (usefulProgress sempre true) derruba `MineApproachProgressTest`. Os GameTests de aproximação
> foram removidos por não-determinismo do pathfinder (o autor advertiu); a regra pura fica em unidade
> e o efeito, no playtest. JAR `village-colony-0.3.55.jar` =
> `4605A53E82EEF6834944A32463322BA3EA7AD35CA43D799161AA721A4C59A40B`.
> **O próximo jogo precisa mostrar:** o mineiro chegar fisicamente à boca e descer (playtest **de
> dia**, com a telemetria do percurso).


> Arquivo de estado vivo. **Sobrescreve, não acumula.** Se passar de 150 linhas, algo
> está errado: P0 não está fechando. O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md)
> (o STATE de 10-10 inteiro, 709 linhas, sem edição) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.

---

## 🔴 P0 — As profissões não trabalham de forma contínua; a mina é a raiz

Varredura de engenharia 14-10 ([`docs/audit/2026-10-14-varredura-engenharia.md`](docs/audit/2026-10-14-varredura-engenharia.md)).
O fio comum dos últimos playtests: **ação interrompida** (parada, bloqueada, esperando), e a
**mina** concentra o risco de código (A1: `MinerSteps.step` com complexidade cognitiva 62),
o P0 de produto (C1: boca a ~77–95 blocos numa vila grande, o mineiro não alcança) e o grosso das
falhas. Em volta: produção antecipada sem teto e cadeia de material que trava a obra (C2),
profissões periféricas em laço improdutivo (C4: pastor pede lã que não há, fundidor espera cru,
fazendeiro sem lote).

**Direção aceita pelo autor (14-10):** simplificar sem quebrar, deixando as ações das profissões
contínuas e ininterruptas. Caminho: (1) a frente de mineração a um raio caminhável do baú do
mineiro (C1); (2) quando um recurso não existe no raio por N tentativas, a profissão troca de
tarefa em vez de reinstalar o pedido (C4); (3) refatorar o núcleo da mina em vez de remendar (A1).

**Aceite:** playtest de dia, ≥ 30 min; Regra 51 (ocioso+bloqueado+encalhado) ≤ 15% por ofício no
expediente; `MINED`/min > 0,8; nenhuma profissão em laço de recusa visível no `action_report`.

## Branches e processo

**`main` é a linha oficial** (ADR-035 §2). A branch `claude/autossuficiencia` foi integrada na
`main` em 14-10 (0.3.25→0.3.55); daqui em diante, branch curta por ciclo e PR para a `main` sem
acumular. Erro de processo corrigido nesta rodada: a branch tinha 74 commits sem merge (B1 da
varredura).

## O que o próximo jogo precisa mostrar

JAR **0.3.55** instalado. Playtest **de dia** (o de 14-10 foi ~84% à noite e mascarou a medição):

| # | Item | Sinal |
|---|---|---|
| 1 | Mineiro chega à boca e desce | `MINED`/min > 0,8; sem "walking to nowhere" prolongado; telemetria do percurso (a implementar) |
| 2 | Profissões contínuas | `time_ledger`: Regra 51 ≤ 15% por ofício no expediente |
| 3 | Sem laço de recusa | `action_report`: soltas/pegas < 10%, sem o mesmo motivo repetido por profissão |
| 4 | Obra não trava | `grep VC_SUPPLY_ERROR` perto de zero; "opened a build task" avança entre obras |

Depois de jogar: `scripts/analyze_village_log.py`, `time_ledger.py`, `action_report.py`,
`cost_ledger.py` (CLAUDE.md §0.4).

## 🔴 Aberto

A fila completa e priorizada está no `TODO.md` (seção AUD) e no roteiro
(`docs/technical/Roteiro-Correcoes-2026-10-10.md`, tabela "Estado da execução").

- **2.4, fundidor e despacho:** o código da linha Codex está integrado; falta medir em jogo.
- **AUD-11 (parcial):** `fabric.network` já passa dos 60% (67%); faltam `fabric.command` (27%) e o cliente (0%).
- **AUD-04 e AUD-16:** só com gatilho (Fase 4). Sem número, sem otimização (ADR-035 §5).
- **Fase 5 (dívida):** AUD-15, 20, 10 e 09; o AUD-09 pede ADR.
- **Decisões do autor:** apagar as 7 branches já contidas na `main` (AUD-24); o número da
  regra do ócio, que divide o "Regra 51" com a da peça na 4ª tentativa (AUD-26); a ADR do AUD-09.
- **AUD-18:** tag por JAR publicado. Não foi feito: criar tags é publicação.

## Dívida conhecida

- Nenhum arquivo de produção acima de 500 linhas; 12 entre 488 e 500 em 11-10 (AUD-20; eram 15 na auditoria).
- `runGametest` não filtra por teste dentro da bateria completa; `-PgametestOnly=Classe` roda
  uma classe isolada. Testes rodam juntos num mundo só: quem conta entidades num raio grande
  afirma sobre as suas (o `FreeChestCountGameTest` falhava 2 em 8 por isso).
- Cobertura da bateria: `fabric.command` e `fabric.network` baixas; cliente 0% (sem cliente no teste).
- Hooks do Claude Code em `scripts/hooks/`; ligá-los no `.claude/settings.json` é do autor.

## Como avaliar e investigar

- **Avaliação técnica:** `docs/technical/avaliacao/` (auditoria de 10-10, régua v2 na
  `METODOLOGIA.md`); `python scripts/assess/assess_project.py --run --gametest-runs 2`.
- **Verificações reprodutíveis:** `scripts/audit/check-*.sh` (README na pasta).
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse.
- **Ler o save quando o log não basta:** a RC2 só se decidiu lendo as tochas do mundo.

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest`, `StructureCoverageGameTest` | onde cada cadeia começa; quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
