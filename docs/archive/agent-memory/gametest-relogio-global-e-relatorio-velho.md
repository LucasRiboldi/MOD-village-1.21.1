---
name: gametest-relogio-global-e-relatorio-velho
description: WorkHoursGameTest acertava o relógio mundial no fim do expediente; e o resumo da bateria lê relatório velho se a compilação falha
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-05T20:59:28.203Z
---

Duas armadilhas vistas em 2026-11-05 ao rodar a bateria depois do modo sem testes:

1. A hora do mundo é global. `WORK_END` foi para 10000 e o `WorkHoursGameTest` ainda punha o relógio às 10000: testes vizinhos
   caíam fora do expediente (lã, mineiro). Ao mudar o expediente, mudar a hora dos testes que acertam o relógio.
2. `scripts/gametest_summary.py` lê `build/gametest-report.xml` mesmo quando o Gradle nem rodou. Uma mutação que não compilou
   mostrou "1123 cases, 0 failed", o que pareceria teste vazio.

3. `TestContext.getRelativePos` não desfaz `getAbsolutePos` em arena girada, e o giro é sorteado: quem monta em coordenada do mundo usa `ColonyFixture.colonyAtWorld` e `spawnAtWorld`. Bicho fora da caixa do teste pode não tiquear: marcar `setOnGround(true)` antes de pedir caminho. Foi essa a causa da falha intermitente do `aTiedAnimalGoesToThePen`, não divisa de chunk.

**Why:** o resultado falso quase virou conclusão errada sobre os testes novos.

**How to apply:** apagar o relatório antes da rodada, e conferir a linha `required tests` no log do Gradle. Mutação não pode
deixar parâmetro sem uso: o ErrorProne com -Werror derruba a compilação. Ver [[contagem-honesta-de-testes]].
