# scripts/audit — fluxos de inspeção

Criados na auditoria técnica de 2026-10-10 (`docs/technical/avaliacao/`). Cada
`check-*.sh` aceita `--help`, grava o relatório em `build/audit/` e sai com
código diferente de zero quando a verificação reprova. Rodam no Git Bash do
Windows e em Linux. Exigem JDK 21: `JAVA_HOME`, que por padrão aponta para
`C:\Program Files\Java\jdk-21.0.12`.

| Script | O que verifica | Reprova quando |
|---|---|---|
| `check-architecture.sh` | ArchUnit + grafo de pacotes por bytecode (`jdeps` → `arch_graph.py`) | `core` depende de fora dele ou de Minecraft/Fabric |
| `check-tests-green.sh [--runs N]` | Python, unitários (contados pelos XML) e N rodadas de GameTest | qualquer falha, ou rodada sem a linha "All N required tests passed" |
| `check-mutation-threshold.sh [--run]` | placar do PIT, folga e top 20 sobreviventes (`pit_report.py`) | placar < 85% (2 se não houver mutação) |
| `check-coverage.sh [--run]` | JaCoCo por pacote, unitário e bateria (`coverage_report.py`) | pacote crítico < 60% |
| `check-docs-drift.sh` | contagens e versão do README, STATE e CHANGELOG contra o medido (`docs_drift.py`) | qualquer divergência |
| `check-mixin-safety.sh [--pack <.minecraft>]` | mixins listados, sem `@Overwrite`/`@Redirect`/`@ModifyConstant`, prefixo `villagecolony$`, `defaultRequire`, remapeamento; níveis 1–2 do `probar.py` do skill `modpack-modsmith` | qualquer regra |
| `check-nbt-migration.sh` | migração `BREEDER`, fuzzing de NBT e propriedades do planejador (sondas em `probes/`) | qualquer teste ou sonda |
| `check-tick-budget.sh [--log] [--journal]` | `cost_ledger`, `time_ledger` e `action_report` sobre um log de jogo | p95 do ciclo > 50 ms ou Regra 51 violada |

`static-extra.init.gradle` aplica SpotBugs e Checkstyle por fora do
`build.gradle`, só para relatório:

```bash
./gradlew.bat -I scripts/audit/static-extra.init.gradle spotbugsMain checkstyleMain --no-daemon
```

## Sondas (`probes/`)

Testes da auditoria que **não** fazem parte da bateria: o
`check-nbt-migration.sh` os copia para `src/test/java/com/villagecolony/audit/`,
roda e remove a pasta na saída (`trap`), mesmo em falha. Hoje há só as
propriedades do planejador de desvio (`PathPlannerProperties`). A sonda de NBT
malformado, que falhava em 6 de 12 casos em 10-10, virou o `ColonySaveFuzzTest`
da bateria quando o AUD-02 foi corrigido.
