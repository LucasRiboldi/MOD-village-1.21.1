# STATE — 2026-09-24

> Arquivo de estado vivo. **Sobrescreve, não acumula.**
> Se passar de 150 linhas, algo está errado — P0 não está fechando.
>
> O que já foi resolvido mora em
> [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md)
> e se consulta por `grep`. Ele já passou do teto duas vezes (2.277 linhas
> em 09-19; 659 em 09-24): o texto antigo foi arquivado lá, sem edição.

---

## Em uma linha

O código está à frente do jogo. Desde o último playtest (24-09, madrugada)
entraram E47/E48, a revisão de naturalidade, a rodada de qualidade, a
refatoração e a **vila foco**. Nada disso foi visto em jogo ainda. A
**próxima sessão de jogo** é o que destrava o resto.

## Versão publicada

- O JAR em `mods` e em `downloads/` é o do commit `b957f5e` (cache da
  varredura de baús livres, sobre a vila foco e o `ServerMemory`), com
  SHA-256 `2CFD08DB…5DBA`.
- Build limpo e 1036 unitários verdes; GameTest 435/435 numa rodada, com a
  mutação do cache confirmada. PIT e CI não foram rodados de novo para
  este commit — os últimos verdes são do `78e7efc`.
- O branch `codex/bighousemod` foi levado para a `main` pelo PR #2.

## O que o próximo jogo precisa mostrar

O roteiro completo está em [`docs/proxima-sessao.md`](docs/proxima-sessao.md).
Em ordem:

| # | Item | Sinal no log |
|---|---|---|
| 1 | Perfil de desempenho (spark) | link do `/spark profiler stop`; ver `docs/technical/Profiling-spark.md` |
| 2 | Vila foco e ciclo mais leve | `Focus village is now`, `Planner turns`, **menos** `Colony cycle took` |
| 3 | E47: o encalhado sai cavando | `is stranded at`, `dug a step`, `is out at`; nunca `cannot dig out` em massa |
| 4 | E48: casa quando falta cama, rodízio sem repetir | um segundo `the house is up` |
| 5 | N1: filhote nasce e ganha ofício | `shared supper with`; nenhum adulto aparecendo do nada depois da fundação |
| 6 | N7, N9, N10 | placa 5 blocos acima do telhado; roça ou oficina depois da 1ª casa; `finished backfilling` |
| 7 | Os 5 playtests da Task 14 | arco da mina, mina finita, baú cheio, BigHouse migrada, traço de atividade |

Depois de jogar, rodar `python scripts/analyze_village_log.py`, que conta
todas essas assinaturas.

## 🔴 Aberto

A lista completa e priorizada está no `TODO.md`, nas seções "Pendências de
correção levantadas pela avaliação" e "Avaliação técnica". Em aberto:

- **R1, desempenho.** Correção feita (vila foco, prazo de 15 ms, cota
  ajustável); falta medir em jogo. É o único critério da avaliação com nota 1.
- **Estado global (R2).** A limpeza já é garantida pelo `ServerMemory`
  (item 3, feito em 24-09), mas os 89 campos estáticos mutáveis continuam
  — consolidá-los num contexto por servidor é o que falta para o C05.
- **Item 4, mutações sobreviventes no PIT.** `MineShaft` 37,
  `ProfessionAssigner` 13, `ColonyCycle` 12.
- **Itens 9 e 10 (ciclo de tarefa comum aos ofícios; regras de decisão
  para o `core`).** Pedem ADR antes do código.
- **Mineiro que não entrega (E44/E45)** e **segunda obra que não abre.**
  Estado de 09-20, sem playtest novo desde as correções; o detalhe está no
  `Historico`, seção "Arquivado do STATE.md".

## Dívida conhecida

- **Hooks do Claude Code:** os scripts estão em `scripts/hooks/`; quem liga
  no `.claude/settings.json` é o autor (a escrita pelo agente foi recusada).
- **Presença da vila foco** não vai para o save. Ao reabrir o mundo, o foco
  se refaz em poucos ciclos.
- **Bateria de jogo:** 3 testes intermitentes foram isolados em 24-09. A
  taxa histórica era de ~1 falha a cada 8 rodadas, e só a repetição prova
  que acabou. `runGametest` não filtra teste.
- **`ColonyDetectionGameTest`:** a falha de 09-19 (24 trabalhadores em vez
  de 30) nunca foi reproduzida nem diagnosticada.
- **Cobertura da camada `fabric`:** não é medida, porque o JaCoCo não
  instrumenta a bateria de jogo (item 5 das pendências).

## Como avaliar e investigar

- **Avaliação técnica:** a metodologia está em
  `docs/technical/avaliacao/METODOLOGIA.md` e roda com
  `python scripts/assess/assess_project.py --run --gametest-runs 2`. A
  última deu B, 3,21 de 4.
- **Instrumentar antes de consertar.** Três defeitos de 09-19 se decidiram
  numa única leitura depois de instrumentados.
- **Quando a mesma causa reaparece em vários itens, desconfie da
  ferramenta,** e não conclua que houve várias regressões.
- **Ferramentas:**

  | Ferramenta | Para quê |
  |---|---|
  | `scripts/analyze_village_log.py` | assinaturas e peças esperadas no log |
  | `scripts/verdict.py` | veredito por item pendente |
  | `ChainRootsGameTest` | onde cada cadeia de produção começa |
  | `StructureCoverageGameTest` | quem fabrica cada peça |
  | `CraftReasons`, `VolumeSample`, `ProtectionSample` | por que algo não saiu ou foi recusado, no log |
