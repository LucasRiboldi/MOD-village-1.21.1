# STATE — 2026-11-13

> Arquivo de estado vivo. **Sobrescreve, não acumula; teto de 150 linhas.** O que saiu daqui mora em
> [`docs/archive/technical/Historico-2026-11.md`](docs/archive/technical/Historico-2026-11.md) (o STATE de 13-11 inteiro,
> com as candidatas da 0.3.58 à 0.4.74), [`Historico-2026-10.md`](docs/archive/technical/Historico-2026-10.md) e
> [`Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md); consulta por `grep`.
>
> **Retomada por outro agente:** comece por `HANDOFF.md`.

## Onde o projeto está

- **Versão:** 0.4.77 nesta linha de trabalho; a publicada é `v0.4.74-alpha`. **Não jogadas.** A última jogada é a 0.4.68
  (12-11, planície cercada de água, Spark `El42EuZ5w9`; análise em `docs/audit/2026-11-13-jogo-0468.md`).
- **Qualidade:** nota 5,0/10 pela ISO/IEC 25010 (`docs/audit/2026-11-13-avaliacao-qualidade.md`), com a meta de uma
  1.0 pública. Seis dimensões bloqueiam a 1.0: regras de negócio, save, desempenho, compatibilidade, experiência do
  jogador e publicação.
- **Fase do cronograma:** F0, sinal limpo (documentos nos tetos, instáveis declarados, ferramentas de régua corrigidas).
  Depois vêm F1 (a vila trabalha) e o portão Beta 0.5.0.
- **Modo:** correção por playtest (`CLAUDE.md` §0.2.2): log primeiro, opções ao autor, teste que falha, correção,
  Sentinela, commit, JAR.

## Candidatas que esperam jogo

Cada uma tem teste que falha sem ela; nenhuma foi vista em jogo.

| Versão | Regra | O que muda |
|---|---|---|
| 0.4.69 | 263 | o fundidor funde onde está, também com baú em casa da vila original |
| 0.4.70 | 264, 265 | o grafo nunca passa sobre a água (sem ponte); a linha planejada sai da construção erguida depois |
| 0.4.71 | 266 | uma casa do mineiro pronta basta, mesmo com a boca da mina mudada |
| 0.4.72 | 267 | a plantação grande sai em todo bioma com o fazendeiro parado |
| 0.4.73 | 268 | o bosque do lenhador acha o chão em qualquer altura |
| 0.4.74 | 269 | o prazo do pastor cresce com a distância; o animal largado fica amarrado na cerca |
| 0.4.75 | 270 | sem ofício do mod fica fora de toda análise de profissão (tempo, Regra 51, selos, relatórios) |
| 0.4.76 | 271 | a muda do bosque surge (também no deserto), em terra enraizada, declarada no `VC_SUPPLY` |
| 0.4.77 | 272 | o pastor colhe flor no ócio e faz corante; cor sem flor, falha ou demora fazem o corante surgir (Sentinela pendente) |

Da 0.4.54 à 0.4.67 (Regras 243 a 261) também não houve confirmação escrita em jogo; a 0.4.68 trouxe as que o jogo de
12-11 viu funcionar: oito ofícios na fundação, casa do mineiro e mina com arco, patamar com tocha e curral achado.

## 🔴 P0 — a vila não trabalha sozinha (F1)

Nos jogos, a maior parte do expediente é espera, bloqueio ou ócio: 11% de trabalho na 0.4.43; na 0.4.68, fundidor
80% parado e fazendeiro 0%. **Aceite do Beta:** dois playtests seguidos, em dois biomas, com todo ofício abaixo de 40%
parado (espera + bloqueio + ócio + encalhe) e nenhum travado; a meta da 1.0 é a Regra 51 (≤ 15%) em três biomas.

## O que o próximo jogo precisa mostrar

Vila nova com a 0.4.74. Depois de jogar: `time_ledger.py`, `action_report.py`, `cost_ledger.py` e o Spark (§0.4).

| # | Item | Sinal |
|---|---|---|
| 1 | Fundidor (Regra 263) | `SMELTED` no diário dos três; `SMELT_MATERIAL` sai de `RESERVED` |
| 2 | Grafo (Regras 264 e 265) | `python scripts/save_read.py map`: nenhuma linha sobre `~`; nenhum `PAVED bridge` |
| 3 | Fazendeiro (Regra 267) | a plantação grande abre como obra; fazendeiro acima de 0% de trabalho |
| 4 | Bosque (Regra 268) | linhas `near the village at ... (grove N of M)`; lenhador andando menos de 67% |
| 5 | Pastor (Regra 269) | "into the pen"; nenhum animal largado solto |
| 6 | Poste de luz (Regra 261) | `VC_LAMP_POST` quando o construtor fica ocioso (na 0.4.68 nunca ficou) |

## Instáveis declarados

Medidos e não corrigidos; registro com evidência em `docs/behavioral-tests/known-failures.md`. Um vermelho só deles
na bateria não reprova a entrega, mas tem de ser dito; qualquer outro vermelho reprova.

| Teste | Taxa medida | Registro |
|---|---|---|
| `GroundPickupGameTest.workerCollectsWhileWalkingPast` | 23% das baterias inteiras (histórico local) | KF-011 |
| `PenAccessGameTest.animalsWalkInThroughEachOfTheFourGates` | 12% das baterias inteiras | KF-012 |
| `ShepherdPensGameTest.eachSpeciesGetsItsOwnPen` | 1 em 3 baterias de 13-11 (estourou 40 tiques) | KF-013 |
| `BiomeBlueprintsGameTest.theTaigaLargePenHasSpruceFencesAndTheOakOneStillStands` | 1 em 3 baterias de 13-11 | KF-014 |
| `LumberjackGameTest.theStallGuardReturnsTheTaskAndForgetsTheTree` | caiu no CI da `main` (11 a 13-11) | KF-004 |

## Aberto que não é código

- **Branches locais de outras linhas** (as sessões paralelas): cada uma fecha a própria; não apagar a de outra sessão.
- **Publicação:** declaração de uso de IA, procedência do ícone, das texturas e da `big_house_mod.nbt` (F5).

## Dívida conhecida

- Camada fabric num ciclo só, presa aos serviços estáticos de `VillageColonyMod` (259 arquivos); `fabric/work` com
  250 classes, 146 num ciclo. Não bloqueia a 1.0; está em "Depois da 1.0" na avaliação.
- NullAway só no `core`; `data/save` sem checagem de nulos é o primeiro a cobrir (F2).
- Teto de 500 linhas virou meta: 44 arquivos entre 450 e 500, 5 em exatamente 500.
- `MinerProbe`, `EnduranceReport` e `VillageInventoryObserver` estão em `src/main` só como sonda dos GameTests; mover
  para o conjunto de teste exige separar os unitários que os usam.
- `runGametest` não filtra por teste; use `scripts/gametest_focus.py` e restaure antes de commitar.

## Como avaliar e investigar

- **Avaliação de qualidade:** `docs/audit/2026-11-13-avaliacao-qualidade.md` e os anexos em
  `docs/audit/avaliacao-2026-11-13/`; refaça as notas a cada portão.
- **Sentinela:** `python scripts/test_lab.py fast`, depois `python scripts/sentinel_analysis.py`, com o Minecraft
  fechado; `python scripts/rule_coverage.py` quando mudar regra ou teste. Não rode `./gradlew.bat --stop` com outra
  sessão usando o Gradle.
- **Instrumentar antes de consertar**; montar a linha do tempo antes de declarar impasse; ler o save quando o log não
  basta (`scripts/save_read.py graph` e `map`).

| Ferramenta | Para quê |
|---|---|
| `scripts/time_ledger.py`, `scripts/action_report.py`, `scripts/cost_ledger.py` | Regras 50 e 51, diário de ações, custo do ciclo |
| `scripts/spark_decode.py` | TPS, picos e métodos do mod num perfil do Spark |
| `scripts/save_read.py` | grafo de ruas e blocos do save, sem abrir o jogo |
| `scripts/analyze_village_log.py`, `scripts/verdict.py` | assinaturas no log; veredito por item pendente |
