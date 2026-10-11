# D7 — Testes e qualidade

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `minecraft-testing` (bateria, rodadas, registro) e `java-static-analysis`
(JaCoCo por pacote, PIT, SpotBugs, Checkstyle).
**Nota da dimensão:** 8/10.

## Números desta sessão (rodados, não copiados)

| Medida | Resultado | Fonte |
|---|---|---|
| Unitários | **1398 testes, 0 falhas** (191 classes) | `build/audit/raw/test-results-unit/*.xml` |
| GameTests | **690/690** × 3 rodadas locais; 690/690 com Fabric API 0.116.17 e Loader 0.19.5; 690/690 + 1 auditoria no CI de `b290ed10` | `build/audit/raw/gametest-*.log`, `ci-main-b290ed10.log` |
| Python | **104 testes**, todos passam (CI: `Ran 104 tests`) | `tests/` |
| PIT (`core`) | **1804/2106 = 85,66%**, força 91,5%, 134 sem cobertura. Idêntico no CI | `build/audit/pit.json` |
| JaCoCo unitário | `core` 85–100% por pacote; `data.save` 86,9%; `fabric` 14% | `build/audit/coverage-unit.json` |
| JaCoCo da bateria | `fabric.work` 84,3%, `fabric.integration` 84,9%, `fabric.event` 72,9% | `build/audit/coverage-gametest.json` |
| Propriedades novas | 6/6 (2100 verificações; P3 com 54% dos casos chegando ao alvo) | `check-nbt-migration.sh` |
| SpotBugs (esforço máx., confiança média) | 73 achados, **0 defeito de correção confirmado**. Os 5 de categoria correção ou nulo foram lidos: `BC_IMPOSSIBLE_CAST` é o idioma `(Object) this` do mixin; `IM_BAD_CHECK_FOR_ODD` em `MineShaft:292` tem índice ≥ 0 garantido em `:283`; os nulos de `getServer()` são de jogador conectado | `build/reports/spotbugs/main.xml` |
| Checkstyle (`sun_checks`) | 16 081 avisos, quase todos de estilo que o projeto não adota (parâmetro final, javadoc, 80 colunas). Real: **848 imports não usados** (AUD-15) | `build/reports/checkstyle/main.xml` |
| Error Prone + NullAway | 0 avisos (C10 = 4) | `build/audit/raw/test.log` |
| PMD (complexidade cognitiva) | 70 violações, só relatório | `build/reports/pmd/main.html` |

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-06 | **P1** | A demanda de material de obra não tem asserção: remover qualquer material sobrevive ao PIT | `core/coordination/WorkDemand.java:98-103,112` (`removed call to WorkDemand::add`, 10 sobreviventes), embora `WorkDemandTest` exista. Placar total com **folga de 13 mutantes** sobre o limiar de 85% (era 87,7% em 09-25) | É a peça que decide "o que destrava a obra antes da reserva", a área da memória "casa não sobe é fome". Um defeito ali passa pelo CI. Mais 14 mutantes sobreviventes reprovam o CI | Um caso em `WorkDemandTest` por campo (pedra, tábua, lã, vidro, carvão, `smelted`, `surfaceGathered`) e o caso do ferro (`max`, não soma) | `check-mutation-threshold.sh --run`: nenhum sobrevivente em `WorkDemand.constructionMaterials` |
| AUD-11 | P2 | Rede e comando quase sem teste | `fabric.network` 20,3% (bateria) / 0% (unitário); `fabric.command` 2,4% / 37,8%; `client.*` 0%. Sem teste de `ProfessionPolicyNetworking`, `ProfessionPolicyPayloadCodec` nem `ProfessionPolicySavedData` | A única entrada que o cliente controla (política de profissão) não tem teste de permissão nem de limites | Unitário do codec (limites 0..512, raio −1 ou 16..128, duplicata, ordem) e GameTest de `applyUpdate` com jogador sem e com nível 2 | Cobertura de `fabric.network` ≥ 60% |
| AUD-07 | P2 | A régua de avaliação mede errado em dois critérios, e o erro aumenta a nota | **C14** conta só linhas `- [ ] 🔴` (`assess_project.py:187`), mas o `TODO.md` marca a prioridade no título da seção: há **41 itens abertos sob 🔴**, então a nota real é 0, não 4. **C01:** `java_metrics.py:87` usa `len(text.split('\n'))` e conta uma linha a mais (`Mine.java` e `SmelterWork.java` têm 500, não 501), então a nota real é 4, não 3. O coletor também quebra no console cp1252 (`assess_project.py:280`, emoji no `print`) | A média publicada (3,36) sobe 0,21 por conta do C14 | Corrigir os dois e subir a régua para v2 (METODOLOGIA §8), recalculando 09-24 | `tests/test_assess_rubric.py` com casos de seção 🔴 e de arquivo com `\n` final |
| AUD-21 | P3 | Estabilidade da bateria: ver D3 | — | — | 8 rodadas | — |

## Python: o que cobre

104 testes em `tests/`, todos no CI (`python -m unittest discover -s tests`, `ci.yml`).
Cobrem as réguas de jogo (`time_ledger`, `cost_ledger`, `action_report`,
`analyze_village_log`), o veredito do gauntlet, a régua de avaliação e o
`graphify_relabel`. Não cobrem os scripts novos de `scripts/audit/`.

## Mutantes sobreviventes: classes com mais casos

`VillageBounds` 14 · `WorkDemand` 10 · `MineShaft` 8 · `ShapeStates` 8 · `ColonyCycle` 7 ·
`ColonyRoads` 7 · `Side` 7 · `AdvanceStock` 6 · `ProfessionAssigner` 5 · `StockRules` 5.
Lista dos 20 primeiros em `build/audit/pit.json` e `build/audit/mutation.txt`.

## Propriedades (pedido 2.3)

`scripts/audit/probes/PathPlannerProperties.java` (jqwik, fora da bateria):

| # | Propriedade | Tentativas | Resultado |
|---|---|---|---|
| P1 | todo passo do desvio é um movimento legal | 300 | ok |
| P2 | o desvio fica no raio de 16 e no teto de 2500 nós | 300 | ok |
| P3 | `reachesGoal` ⇒ o último passo é o alvo (161 de 300 chegaram) | 300 | ok |
| P4 | mesma entrada, mesmo plano (determinismo) | 150 | ok |
| P5 | um movimento nunca cava nem coloca bloco em célula protegida, nunca cava fluido ou barreira, e só coloca piso onde o terreno aceita (158 de 1000 cavam ou colocam) | 1000 | ok |

As propriedades de lote ("nenhuma obra reserva sobre estrutura existente",
"viveiro ≤ 10 árvores") dependem da camada `fabric` (mundo real) e já são
exercidas por GameTest (`TreeNurseryGameTest.theNurseryStopsAtTenTrees:240`,
`TreeNurseryGameTest.aSpotInsideAnOpenBuildIsNeverANursery:112`, e unitário
`LumberjackNurseryTargetTest:11`). Não viraram propriedade jqwik.

## Não verificado

- Nível 4 do modsmith (dentro do pack): manual.
- 8 rodadas da bateria (AUD-21).
