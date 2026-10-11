# STATE — 2026-10-11

> **JAR instalado: `village-colony-0.3.24.jar`** (commit `0af32d84`, 11-10) =
> `C553E735AC09A7CEDE0FE6547DDD2513EEA56422298DF3D6634EF4FC35A25E8F`, igual em `build/libs`,
> `downloads/` e `.minecraft/mods` (`release_manifest.py`); a 0.3.23 ficou como `.jar.disabled`.
> Traz a linha Codex integrada e as correções do roteiro de 10-10. **Ainda não jogada.**
>
> **Última verificação (`claude/correcoes-fase5-6-higiene`, 11-10):** 1445 unitários; 705/705
> GameTests em **8 rodadas seguidas** (AUD-21); 112 testes Python. PIT 1820/2112 (86,17%) em `e71607dd`.

> Arquivo de estado vivo. **Sobrescreve, não acumula.** Se passar de 150 linhas, algo
> está errado: P0 não está fechando. O que já foi resolvido mora em
> [`docs/archive/technical/Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md)
> (o STATE de 10-10 inteiro, 709 linhas, sem edição) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.

---

## 🔴 P0 — AUD-01: a cadeia de produção para na mina

A Regra 51 reprovou nas duas sessões de 10-10 (0.3.22: SMELTER 86%, BUILDER 68%, MINER 21%;
0.3.23: SMELTER 91%, BUILDER 92%, MINER 17%). O fundidor pegou 311 tarefas e soltou 311.
A raiz é a mina: **zero mineração**. O fundidor e o construtor param por falta de material.

Diagnóstico de 11-10, a partir de três sessões, do save NBT e das regiões Anvil (roteiro 2.2):

1. O mineiro dono do único ramal ia e voltava numa descida até um patamar de pedra maciça.
   Na descida, o `MinerSteps` zerava os dois prazos a cada tique, então nenhum guarda
   disparava. Os outros três mineiros ficavam barrados ("no mine mouth" é a frase do barrado).
2. O patamar era de pedra porque a rota estava errada, não porque a mina não existisse. Os
   cinco níveis foram cavados descendo NORTH a partir da boca (−768, 66, −923). O giro da
   escada (ADR-037 M1) trocou o rumo para EAST, e o `MineEntrance` deduzia todos os níveis
   de cima pelo rumo de agora: a boca deduzida ficava 50 blocos ao sul.

| Correção | Commit | Prova | Em jogo |
|---|---|---|---|
| RC1: a descida tem prazo próprio (60 s sem chegar mais perto nem descer mais fundo) | `08b0c34f` | `MineDescentLeaseGameTest`, `MineLeaseTest`, mutações | ⬜ |
| RC2: a rota segue a escada cavada; a cadeia de entradas é guardada e sobrevive ao giro | `36d3946b` | `MineEntranceGameTest` (+2, reprovam no código antigo), mutações; regra simulada no save real | ⬜ |
| 2.3: a regra do ócio cede vaga por ocioso + bloqueado + encalhado (ADR-041 #3) | `da43ef34` | `IdleYieldGameTest` | ⬜ |

**Aceite (roteiro 2.5):** playtest de 60 min com o JAR novo; `time_ledger` com ocioso +
bloqueado + encalhado ≤ 15% por ofício; fundidor com soltas/pegas < 10% no `action_report`.

## PRs abertas (empilhadas; nenhuma na `main`)

| PR | Branch | Conteúdo |
|---|---|---|
| #20 | `claude/auditoria-tecnica-2026-10-10` | auditoria D1–D12, `scripts/audit/`, roteiro, ADR-041 |
| #21 | `claude/integra-fluxo-grafo-waypoints` | AUD-05: linha Codex integrada, 0.3.24 |
| #22 | `claude/correcoes-fase1-save` | AUD-02/03: o save não perde colônias por entrada ruim |
| #23 | `claude/correcoes-fase3-testes` | AUD-06/07/11: rede de testes |
| #24 | `claude/correcoes-fase2-cadeia` | AUD-25, regra do ócio, AUD-19 (`/vc log`), RC1 e RC2 do mineiro |

A ordem de merge é a da pilha: #20 primeiro. Cada uma aponta para a anterior.

**Sessão em curso:** `claude/correcoes-fase5-6-higiene` (sobre a #24): AUD-08, 11 (rede), 12, 13,
14, 21, 22 e 23 feitos. Restam sem jogo: AUD-15, 20, 10 e 09 (Fase 5).

## O que o próximo jogo precisa mostrar

JAR 0.3.24 já instalado. Em ordem:

| # | Item | Sinal |
|---|---|---|
| 1 | Mineiro chega ao nível pela escada real | `the surface entrance is remembered at -768, 66, -923 … 5 levels down` no save do autor; `MINED` por minuto acima de 0,8 |
| 2 | Ninguém segura o ramal parado | nenhum mineiro com o ramal por mais de 60 s de descida; se houver, `the way down got no closer than` |
| 3 | Regra 51 por ofício | `python scripts/time_ledger.py`; `/vc log` mostra a última janela de cada ofício |
| 4 | Fundidor sai da roda | `python scripts/action_report.py`: soltas/pegas < 10%, com o motivo de cada soltura |
| 5 | Custo do ciclo (AUD-04) | `python scripts/cost_ledger.py`: ≥ 20 amostras de `VC_COST`; p95 > 50 ms abre a Fase 4 |
| 6 | Alternância de obras (AUD-13) | quatro obras seguidas, `grep "opened"` |

Os playtests pendentes de antes (0.3.5 a 0.3.23) estão por versão no `TODO.md`; os de setembro,
no `Historico-2026-09.md`. Depois de jogar: `scripts/analyze_village_log.py`,
`scripts/time_ledger.py`, `scripts/action_report.py` e `scripts/cost_ledger.py` (CLAUDE.md §0.4).

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
