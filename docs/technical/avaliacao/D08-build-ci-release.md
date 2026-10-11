# D8 — Build, CI e release

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `minecraft-ci-release` (workflow, artefato, SHA-256) e `modpack-modsmith`
(níveis 1–3); `git-workflow-audit` para tags e branches.
**Nota da dimensão:** 8/10.

## O que foi conferido

| Item | Resultado | Evidência |
|---|---|---|
| CI na `main` | **verde** em `b290ed10` (run de 2026-10-10 23:53 UTC) | `gh run list`; `build/audit/ci-main-b290ed10.log` |
| GameTest obrigatório no CI | sim: `runGametest --rerun-tasks` em todo push; auditoria de dados e PIT em PR, `main` e disparo manual | `.github/workflows/ci.yml` |
| Ações fixadas por SHA | sim (checkout, setup-java, setup-python, setup-gradle, upload-artifact) | `ci.yml` |
| PIT reprova abaixo do limiar | sim (`mutationThreshold = 85`, sem `continue-on-error`) | `build.gradle:168`; CI: `Generated 2106 mutations Killed 1804 (86%)` |
| JAR fora do repositório (ADR-036) | sim: `.gitignore:109` cobre `/downloads/`, `:3` cobre `build/`; `git ls-files` sem `.jar` de mod | — |
| SHA-256 do JAR publicado | `6067E01A…866661`: **igual** em `downloads/`, em `%APPDATA%/.minecraft/mods/` e no `STATE.md` | `sha256sum` |
| `fabric.mod.json` | `${version}` expandido (`0.3.23`); entrypoints `main`, `client` e `modmenu` presentes no JAR; `depends` com fabricloader ≥0.16.0, fabric-api ≥0.116.15, minecraft ~1.21.1 e java ≥21 | `probar.py` nível 1 |
| Nível 2 do modsmith | loader 0.16.0 ≤ 0.19.5 do pack; MC 1.21.1; 4 dependências presentes; sem JAR duplicado | `build/audit/probar-0.3.23.txt` |
| Nível 3 (servidor sobe) | a bateria roda em `server()` (`build.gradle:207-208`), executa o entrypoint `main`, não carrega o mixin de cliente e não tem `Mixin apply failed` | `gametest-1.log` |
| JARs embutidos | 0 (`META-INF/jars` vazio): o mod não distribui biblioteca | `unzip -l` |
| CVEs | 137 coordenadas consultadas na OSV. 8 com aviso, **todas do runtime do Minecraft 1.21.1** (netty 4.1.97, log4j 2.22.1, lz4 1.8.0, commons-lang3 3.14.0), nenhuma declarada pelo mod | `build/audit/osv.json` |

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-05 | **P1** | Linha de desenvolvimento paralela fora da `main` | `codex/fluxo-grafo-waypoints`: **14 commits** que `git cherry` marca como ausentes da `main` (P0.7, P0.8, P0.56 e a **ADR-040**, que não existe na `main`); último commit 2026-10-10 20:34; worktree ativo em `.codex/worktrees/4384`; 106 arquivos de `src/main` diferem. A PR #19 dela foi fechada às 20:53 como "substituída pela integração reconciliada na main", mas o `133c69db` ("unificar fluxo de obra na main") trouxe só parte: `ConstructionSupplyDispatches`, `AdvanceConstructionStock`, a ADR-040 e a retenção do fundidor não estão na `main` (conferido por conteúdo, não por patch). Nem `STATE.md` nem `TODO.md` citam a branch | É o cenário que a ADR-035 §2 proíbe (03-10 e 04-10 custaram um dia de integração). O próximo trabalho na `main` pode refazer o que já está lá, ou entrar em conflito com ele | O autor decide: integrar por PR, ou encerrar a branch e registrar no `STATE.md` o que foi descartado | `git cherry -v main codex/fluxo-grafo-waypoints` vazio, ou branch apagada com nota no `STATE.md` |
| AUD-18 | P3 | Versões sem tag | última release `v0.3.3-alpha` (07-10); 0.3.4 a 0.3.23 só no `STATE.md`. `git describe` = `v0.3.3-alpha-93-gb290ed10` | Não há como voltar a um JAR jogado pelo número da versão | Tag leve por JAR copiado para `.minecraft` (`scripts/release_manifest.py` já gera o SHA) | `git tag -l 'v0.3.*'` cobre o JAR do `STATE.md` |
| AUD-24 | P3 | 7 branches locais e 6 remotas já contidas na `main` (`ahead = 0`) | `claude/correcoes-2026-10-07d`, `claude/correcoes-erros-2026-10-07`, `claude/decisoes-2026-10-07c`, `claude/memorias-no-save`, `claude/opcao-b-e-plano-de-testes`, `codex/bighousemod`, `codex/village-visuals-logistics-mine-sweep` | Ruído na escolha de base | Apagar é decisão do autor; não foi feito | — |

## Histórico (30 dias, `main`, sem merges)

539 commits; 98 com mais de 500 linhas; **33 com mais de 500 linhas de produção**.
Os maiores de produção são refatorações de divisão (`4596c8d6` BuildSiteScanner em 7,
`d38c7d7e`, `10247d7e`), o que é aceitável. O maior em documentação (`487a2651`, 33 mil
linhas) é arquivamento.

## Não verificado

- Release pública do 0.3.23: não houve publicação, e esta auditoria não publica.
