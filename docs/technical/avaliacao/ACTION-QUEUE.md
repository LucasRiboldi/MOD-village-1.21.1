# ACTION-QUEUE — auditoria técnica de 2026-10-10

> Commit auditado: `b290ed10` (`main`). Branch da auditoria: `claude/auditoria-tecnica-2026-10-10`.
> Evidência de cada item: o arquivo da dimensão (D01–D12) e os brutos em
> `build/audit/` (fora do Git; regeráveis pelos `scripts/audit/check-*.sh`).
> A fila canônica continua sendo o `TODO.md`; esta tabela entrou lá na seção
> "Auditoria técnica 10-10", com os mesmos IDs.

**Severidade.** **P0**: o mod falha na promessa central, visto em jogo. **P1**: defeito
com caminho concreto ou regra do projeto quebrada. **P2**: risco que vira defeito
ao crescer. **P3**: higiene.
**Esforço.** P = até 2 h · M = meio dia · G = um dia ou mais.

## P0

| ID | Dim. | Skill usada | Achado | Arquivo:linha | Impacto | Esf. | Depende de | Teste que valida |
|---|---|---|---|---|---|---|---|---|
| AUD-01 | D2, D4 | minecraft-performance-profiling, minecraft-testing | Regra 51 reprovada em jogo nas duas sessões de 10-10: 0.3.22 (SMELTER 86%, BUILDER 68%, MINER 21%, FARMER 16%) e 0.3.23 (SMELTER 91%, BUILDER 92%, MINER 17%). Fundidor: 882/883 e 311/311 tarefas soltas. Raiz: o mineiro não acha boca de mina (115× e 50× `no mine mouth … budget ran out`) e não produz pedra nem ferro | `MineVein.java:184-191`; `SmelterWork.java:249-283`; `IdleYield.java:22,51`; `WorkTime.java:195` | Aldeões não trabalham sozinhos, a promessa do mod | G | AUD-05 (a correção do fundidor `cd7ca2c8` e a ADR-040 estão no Codex) | `check-tick-budget.sh --log <log> --journal <jsonl>` sem `REGRA 51`; fundidor com conclusão ≥ 50% |

**Pronto quando:** um playtest de ≥ 60 min com a versão corrigida passa no
`check-tick-budget.sh` sem `REGRA 51` em nenhum ofício, e o `action_report` mostra o
fundidor com conclusão ≥ 50%. Se falhar, a correção decide antes se a Regra 51 é o
ócio puro do `IdleYield` ou o ocioso + bloqueado + encalhado do ledger (D2).
**Cruza com o TODO:** "Playtest da 0.3.23 / 0.3.20" (linhas 31–32) e "Playtest da 0.3.19" (44, 46).

## P1

| ID | Dim. | Skill usada | Achado | Arquivo:linha | Impacto | Esf. | Depende de | Teste que valida |
|---|---|---|---|---|---|---|---|---|
| AUD-02 | D5, D9 | fabric-persistence-migration | Exceção na leitura do save faz o Vanilla criar estado vazio e gravá-lo por cima. 6/10 casos de NBT malformado lançam | `ColonySaveReader.java:93,117,161`; `SaveMigration.java:82-83`; `ResourceId.java:69-70`; `MinecraftTypeAdapter.java:245` | Todas as colônias somem em silêncio | M | — | `scripts/audit/probes/NbtFuzzProbeTest.java` promovido a `src/test`, 12/12 |
| AUD-03 | D5 | fabric-persistence-migration | `saveVersion` nunca é gravado; a v0→v1 roda a cada carga | `SaveMigration.java:67-75`; `ColonySavedData.java:363` | A primeira migração não idempotente vai corromper saves já migrados | P | — | `ColonySavedDataTest`: `writeNbt(...).getInt("saveVersion") == CURRENT` |
| AUD-05 | D8, D10 | git-workflow-audit | `codex/fluxo-grafo-waypoints`: 14 commits e a ADR-040 fora da `main`, worktree ativo, ausente do `STATE.md` | `git cherry -v main codex/fluxo-grafo-waypoints` | Retrabalho e conflito (ADR-035 §2) | P (decidir) / M (integrar) | **decisão do autor** | `git cherry` vazio, ou branch encerrada com nota no `STATE.md` |
| AUD-06 | D7 | java-static-analysis | `WorkDemand.constructionMaterials` sem asserção (10 mutantes sobreviventes); PIT com folga de 13 mutantes sobre 85% | `WorkDemand.java:98-103,112`; `WorkDemandTest` | Defeito na demanda que destrava a obra passa pelo CI | P | — | `check-mutation-threshold.sh --run`: 0 sobreviventes nesse método |

**Pronto quando:** cada um tem o teste da última coluna no repositório e verde no CI.
O AUD-05 é pronto com a decisão escrita no `STATE.md`.
**Cruza com o TODO:** nenhum destes existia.

## P2

| ID | Dim. | Skill usada | Achado | Arquivo:linha | Impacto | Esf. | Depende de | Teste que valida |
|---|---|---|---|---|---|---|---|---|
| AUD-07 | D7, D10 | java-static-analysis | Régua de avaliação: C14 cego para as seções 🔴 do TODO (41 itens: nota real 0, não 4); C01 conta uma linha a mais; coletor quebra em console cp1252 | `assess_project.py:187,280`; `java_metrics.py:87` | Nota publicada otimista | P | — | `tests/test_assess_rubric.py` com seção 🔴 e arquivo com `\n` final; régua v2 |
| AUD-08 | D10 | git-workflow-audit | `STATE.md` com 663 linhas contra o teto de 150 | `STATE.md:9` | Entrada obrigatória cara e confusa | M | AUD-05 (o STATE vai mudar) | `check-docs-drift.sh` sem a linha do STATE |
| AUD-09 | D1 | java-architecture-review | Ciclo raiz ↔ 8 pacotes `fabric.*`: `VillageColonyMod` é o localizador de serviço global | `VillageColonyMod` (`LOGGER` 266×, `WORKERS` 81×, `COLONIES` 66×…) | `fabric` não testável em isolamento; estado global | G | — | `check-architecture.sh` sem ciclo pela raiz |
| AUD-10 | D3 | minecraft-testing | 181 campos estáticos mutáveis; configuração estática não-final | `MineLease.java:80`, `TreeChoice.java:80`, `MinerDetours.java:39`, `FarmerWork.java:78` | Vazamento entre mundos e testes | M | AUD-09 | store congelado da `aStaticMutableCollectionIsServerMemory` sem violações (a regra já existe) |
| AUD-11 | D7, D9, D11 | java-static-analysis | Rede de política, comando e cliente quase sem teste (`fabric.network` 20%, `fabric.command` 2–38%, `client` 0%) | `ProfessionPolicyNetworking.java:32-46`; `ProfessionPolicyPayloadCodec.java:35-56` | Permissão e limites da única escrita do cliente sem rede de proteção | M | — | cobertura de `fabric.network` ≥ 60% |
| AUD-12 | D6, D12 | modpack-modsmith, minecraft-mod-dev | Outro mod que troque o `Brain` desliga a colônia sem aviso; `fabric.mod.json` sem `suggests`/`breaks` | `VillagerEntityMixin.java:29`; `fabric.mod.json` | Colônia parada num modpack | M | — | GameTest que limpa o `Brain` e espera o `WARN`; nível 4 do modsmith |
| AUD-13 | D2, D10 | minecraft-testing | "Alternância de obras" documentada como falha, mas o teste passa 5×; falta prova em jogo | `README.md:225-227`; `docs/behavioral-tests/README.md:16-17`; `FarmPlanGameTest.java:297` | Caça a defeito que a bateria não reproduz | P | AUD-01 (mesmo playtest) | playtest com 4 obras seguidas; documento atualizado |
| AUD-04 | D4 | minecraft-performance-profiling | Ciclo com p95 67 ms na 0.3.22 (13 amostras), mas 27,7 ms na 0.3.23 (8 amostras): o pico não se repetiu | `PlanningBudget.java:27-33`; `ColonyCycleRunner.java:276-285` | Engasgo a cada 30 s, se confirmado | P (medir) / G (fatiar) | sessão com ≥ 20 amostras | `check-tick-budget.sh`: p95 total ≤ 50 ms em ≥ 20 amostras |
| AUD-25 | D11 | minecraft-performance-profiling | O diário grava `TASK_RELEASED` com o estado (`RESERVED`), não o motivo da soltura | `ActionJournal` (campo `detail`) | 882 solturas sem causa no diário; o motivo só aparece cruzando com o log | P | — | `action_report.py` mostra o motivo das solturas por profissão |

## P3

| ID | Dim. | Skill usada | Achado | Arquivo:linha | Impacto | Esf. | Depende de | Teste que valida |
|---|---|---|---|---|---|---|---|---|
| AUD-14 | D10 | git-workflow-audit | CHANGELOG parado em 0.3.0 (03-10); README com contagens de 03-10 (**corrigido nesta auditoria**) | `CHANGELOG.md:6,12` | Leitor desinformado | M | — | `check-docs-drift.sh` 0 divergências |
| AUD-15 | D1, D7 | java-static-analysis | 848 imports não usados | ex.: `ConstructionProject.java:13` | Ruído | P | — | Error Prone `RemoveUnusedImports` como erro |
| AUD-16 | D4 | minecraft-performance-profiling | `listAllOfType` a cada chamada, sem cache | `CraftingLookup.java:102,190,240,330` | Custo não medido | P | Spark | Spark sem `CraftingLookup` no topo |
| AUD-17 | D9 | java-static-analysis | `/vc log` sem permissão — **decidido: continua aberto** (ADR-041 #4) | `VillageLogCommand.java:36-37` | Informação da colônia aberta em servidor público, por escolha | — | — | — |
| AUD-18 | D8 | minecraft-ci-release | 0.3.4–0.3.23 sem tag; última release `v0.3.3-alpha` | `git describe` | Sem volta a um JAR jogado | P | — | tag por JAR do STATE |
| AUD-19 | D11 | minecraft-modding | `/vc log` não mostra a Regra 51 | `VillageLogPresenter.java:70` | Descoberta só depois da sessão | M | — | teste do presenter com janela de 16% |
| AUD-20 | D1 | java-architecture-review | 15 classes em 488–500 linhas; 39 métodos > 50 linhas; 9 com CC > 20 | `build/audit/main-file-sizes.txt` | Divisão forçada no meio de correção | M | — | `gauntlet_checks.py` verde |
| AUD-21 | D3, D7 | minecraft-testing | 5 rodadas verdes não excluem a intermitência histórica de 1/8 | `METODOLOGIA.md` §7 | Falsa sensação de cura | P | — | `check-tests-green.sh --runs 8` |
| AUD-22 | D3 | minecraft-testing | `Random.create()` sem semente na fundação | `BigHouseFoundation.java:79` | Fundação não reproduzível | P | — | GameTest com semente fixa |
| AUD-23 | D6, D10 | modpack-modsmith | Javadoc "o único mixin do mod" | `VillagerEntityMixin.java:14` | Comentário falso | P | — | — |
| AUD-24 | D8 | git-workflow-audit | 7 branches já contidas na `main` | `git rev-list --count main..<b>` = 0 | Ruído | P | decisão do autor | — |
| AUD-26 | D10 | git-workflow-audit | Duas "Regra 51": a do ócio (`CLAUDE.md` §0.4, `time_ledger.py:35`) e a da peça na 4ª tentativa (`docs/RULES.md:78`); a do ócio não está no `RULES.md` | `CLAUDE.md` §0.4; `docs/RULES.md:78` | Comentário e documento citam a regra errada (ADR-035 §6) | P | decisão do autor (o número) | `grep -rn "Regra 51"` só acha uma regra |

## Ordem sugerida

1. Roteiro detalhado: `docs/technical/Roteiro-Correcoes-2026-10-10.md`.
2. **AUD-05**: decidir a linha Codex antes de qualquer código novo — ela já traz a correção do fundidor.
3. **AUD-03 + AUD-02** juntos (mesma classe, mesmo teste), e AUD-06: rápidos e protegem dados.
4. **AUD-04** só se o próximo playtest repetir o pico (é o "F1" de `fluidez-passo-do-ciclo`).
5. P2 e P3 conforme a folga.
