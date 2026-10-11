# D10 — Documentação e drift

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skill:** `git-workflow-audit`, que comparou números com comandos
(`scripts/audit/check-docs-drift.sh`) e branches com a `main`.
**Nota da dimensão:** 5/10.

## Drift numérico (antes das correções desta auditoria)

`scripts/audit/check-docs-drift.sh`, saída 1:

| Documento | O quê | Escrito | Medido |
|---|---|---|---|
| `README.md:205` | unitários | 1295 | **1398** |
| `README.md:206` | Python | 91 | **104** |
| `README.md:207` | GameTests | 575 | **690** |
| `README.md:229` | GameTests (nota de 09-21) | 397 | 690 (é registro histórico; mantido) |
| `README.md:212` | PIT | 87,7% | **85,66%** |
| `README.md:43-44` | "os GameTests foram pulados" na 0.3.23 | — | 690/690 × 5 rodadas nesta auditoria |
| `CHANGELOG.md:6,12` | "a versão continua 0.3.0 alpha"; último registro em 03-10 | 0.3.0 | **0.3.23**: as versões 0.3.1 a 0.3.23 não têm entrada |
| `STATE.md` | teto que o próprio arquivo declara (150 linhas) | 150 | **663** |
| `docs/behavioral-tests/README.md:16-17` | alternância como falha obrigatória | falha | passa (AUD-13) |
| `VillagerEntityMixin.java:14` | "o único mixin do mod" | 1 | 3 (AUD-23) |

O badge do README e a linha "JAR atual" do `STATE.md` batem com o `gradle.properties` (0.3.23).

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-08 | P2 | `STATE.md` com 663 linhas contra o teto declarado de 150 | `STATE.md:9` ("Se passar de 150 linhas, algo está errado") | O arquivo de entrada obrigatório (`CLAUDE.md` §0.1) custa 4× o que deveria a cada sessão, e esconde o P0 atual entre 20 seções antigas | Arquivar em `docs/archive/technical/Historico-2026-10.md` as seções de 02-10 a 08-10 já fechadas, sem editar o texto (mesmo procedimento de 09-30) | `check-docs-drift.sh` sem a linha do `STATE.md` |
| AUD-14 | P3 | README e CHANGELOG atrasados | tabela acima | Quem chega lê números de 03-10 | README corrigido **nesta auditoria**; o CHANGELOG ganhou só a entrada da auditoria. As entradas 0.3.1–0.3.23 ficam com o autor (o texto delas está nos commits e no `STATE.md`) | `check-docs-drift.sh` com 0 divergências |
| AUD-05 | P1 | Linha Codex fora da `main` e fora do `STATE.md` | D8 | D8 | D8 | D8 |
| AUD-07 | P2 | A régua de avaliação superestima o C14 | D7 | D7 | D7 | D7 |

## ADRs e constituição

- **ADR-035 §2 (uma linha por vez):** violada no estado atual (AUD-05).
- **ADR-035 §6 (comentário diz a regra):** um caso, AUD-23.
- **ADR-036 (JAR fora do repo):** cumprida.
- **ADR-004 §3–4 (mixins):** cumprida (D6).
- **ADR-040:** existe só na `codex/fluxo-grafo-waypoints`; a `main` vai até a ADR-039.
- `PROJECT_CONSTITUTION.md`: não contém as Regras 48, 51 e 52, que moram em
  `docs/RULES.md` e no `CLAUDE.md`. Não achei emenda violada por commit recente,
  mas a leitura foi por amostragem, não completa.

## Não verificado

- Leitura completa de cada ADR contra o código; só as citadas no `STATE.md`.
