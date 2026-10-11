# D1 — Arquitetura e camadas

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `java-architecture-review`, aplicada pelo checklist de camadas, ciclos por
bytecode e tamanho; e `java-static-analysis`, que leu o PMD e o coletor da régua.
**Nota da dimensão:** 8/10.

## Como foi medido

| Medida | Comando | Resultado |
|---|---|---|
| Regras ArchUnit | `scripts/audit/check-architecture.sh` | passou (`ArchitectureRulesTest`) |
| Dependências entre pacotes | `jdeps -verbose:package` sobre `build/classes/java/main` → `scripts/audit/arch_graph.py` | 39 pacotes, 150 arestas internas |
| `core` → fora do `core` | idem | **0** |
| `core` → Minecraft/Fabric | idem | **0** |
| Ciclos | idem (Tarjan) | **1 componente** com 9 pacotes |
| Tamanho | `scripts/assess/assess_project.py` (C01–C03) | máx. 500 linhas físicas; p90 de método 24 linhas; 3,2% dos métodos com CC > 10 |

O isolamento `core`/`fabric` vale para **todos** os 23 pacotes `core`: a medida é por
bytecode e não depende da lista de pacotes que o ArchUnit enumera.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-09 | P2 | A raiz `com.villagecolony` e 8 pacotes `fabric.*` (`brain`, `command`, `event`, `integration`, `network`, `overlay`, `work`, `world`) formam um único ciclo | `build/audit/arch-graph.json`. Causa: `VillageColonyMod` guarda os registros globais e o `fabric` os acessa por estática: `LOGGER` 266×, `WORKERS` 81×, `COLONIES` 66×, `CONSTRUCTIONS` 61×, `TASKS` 45×, `BUILDINGS` 45×, `STORAGES` 42×, `MINES` 32× | Nenhum pacote `fabric` é testável ou substituível sem carregar o mod inteiro; o estado global atravessa testes (ver D3) | Extrair os registros para um contexto de colônia (`ColonyServices`) passado por parâmetro, ou ao menos para `fabric.registry`, quebrando a volta para a raiz. Não é P0: o `core` está limpo | `check-architecture.sh` relata 0 ciclos envolvendo a raiz |
| AUD-20 | P3 | 15 classes de produção entre 488 e 500 linhas, 39 métodos acima de 50 linhas, 9 métodos com CC > 20 (máx. 27) | `build/audit/main-file-sizes.txt`; `metricas.json` (`methods_over_50_lines`, `methods_cc_over_20`) | A próxima mudança em `SmelterWork`, `Mine`, `HousePlans`, `BuilderWork`, `ClimbOut`, `ConstructionProject`, `ColonyCycleRunner`, `Worker` ou `ColonyGoals` força uma divisão no meio de uma correção | Dividir por eixo de domínio antes da próxima entrega que tocar a classe, não durante | `gauntlet_checks.py` (500 linhas) continua verde depois da divisão |
| AUD-15 | P3 | 848 imports não usados | Checkstyle `UnusedImportsCheck`, ex.: `ConstructionProject.java:13` (`OptionalInt`), `ConstructionReach.java:4-5` | Ruído; esconde dependência real no `jdeps` por fonte | Ligar `RemoveUnusedImports` do Error Prone como erro em `compileJava` | Build com Error Prone passa sem avisos |

### `HousePlans.java`: dividir por bioma?

**Não agora.** São 499 linhas e 19 métodos, sem linha acima de 116 colunas. Não
é god-class. O catálogo por bioma é dado, não lógica; se crescer, a divisão
natural é tirar as listas de IDs para um recurso de dados
(`data/villagecolony/...`), e não fazer cinco classes Java. Fica em AUD-20.

## Positivo, com evidência

- `core` sem nenhuma dependência de Minecraft ou Fabric (C04 = 4/4, 0 violações por bytecode).
- O teto de 500 linhas é imposto por `scripts/gauntlet_checks.py:55`. As classes no
  limite não estão comprimidas: a maior linha em `HousePlans` tem 116 colunas.

## Não verificado

- CBO e LCOM por classe não foram calculados. O pedido os previa; o skill aponta
  `javap` + script, que não foi escrito por falta de prioridade diante de D2, D4 e D5.
