# STATE — 2026-10-14

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
