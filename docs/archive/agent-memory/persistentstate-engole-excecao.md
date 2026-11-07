---
name: persistentstate-engole-excecao
description: "No MC 1.21.1, exceção no readNbt de um PersistentState vira estado vazio gravado por cima do .dat — perda silenciosa de todas as colônias"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-11T00:43:32.095Z
---

`PersistentStateManager.readFromFile` (1.21.1, conferido com `javap -c` no JAR yarn
de `.gradle/loom-cache`) envolve a leitura em `catch (Exception)`, loga
"Error loading saved data" e devolve `null`; `getOrCreate` então constrói o
estado **vazio** e faz `set(...)`. No próximo save o vazio sobrescreve
`villagecolony_colonies.dat`. Sem crash, só uma linha ERROR.

Em 10-10 a sonda `scripts/audit/probes/NbtFuzzProbeTest.java` derrubou o leitor em
6/10 NBTs ruins: `ResourceId.parse` sem proteção em `ColonySaveReader.java:93,117,161`
(dois `:` ou blueprint vazio) e `saveVersion` negativo (`SaveMigration.java:82`).
Também: `saveVersion` nunca é gravado no disco (AUD-03).

**Why:** "o save carrega" em teste não prova robustez; o Vanilla esconde a falha.
**How to apply:** todo leitor de save do mod deve tratar entrada ruim como
descarte com log, nunca lançar. Ver [[auditoria-tecnica-2026-10-10]].
