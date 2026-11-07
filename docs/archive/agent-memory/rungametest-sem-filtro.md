---
name: rungametest-sem-filtro
description: runGametest não aceita --tests; não dá para rodar um GameTest isolado pela linha de comando do Gradle
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-22T22:17:32.854Z
---

A task `runGametest` deste projeto **não aceita `--tests`**. Tentar
`./gradlew.bat runGametest --tests "*algumBatch*"` falha com
`Unknown command-line option '--tests'` antes de compilar qualquer coisa.

Não existe, hoje, um caminho de linha de comando para rodar um único GameTest
ou um único `batchId`. A bateria roda inteira ou não roda.

**Why:** perdi uma tentativa de reprodução isolada do
`FarmPlanGameTest.thenextturnafterahouseisnonresidential` com essa flag. O
`--tests` existe na task `test` (unitários Java), não na `runGametest`.

**How to apply:** quando o objetivo for reproduzir um GameTest específico,
rode `./gradlew.bat runGametest --rerun-tasks` completo e filtre a saída, ou
mude o `batchId`/registro temporariamente. Some isso a
[[bateria-gametest-instavel]]: como a bateria inteira leva minutos e ~25% de
um teste falha por instabilidade, uma única rodada não decide — e como não dá
para isolar, a repetição é o único instrumento confiável.

Ver também [[contagem-honesta-de-testes]] para ler o número real de aprovados
no XML de `build/test-results` em vez da mensagem de resumo.
