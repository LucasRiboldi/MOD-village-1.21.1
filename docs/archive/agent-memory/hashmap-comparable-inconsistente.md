---
name: hashmap-comparable-inconsistente
description: "Chave de HashMap que é Comparable por um campo que o equals ignora some do mapa quando o balde vira árvore — crash do Corridor na 0.3.89"
metadata:
  type: feedback
---

O `State` do `Corridor` implementava `Comparable` só pela prioridade, e o `equals` ignorava a prioridade. Em
22-10 isso derrubou o servidor da 0.3.89 (NPE em `cost.get(at)`) assim que o corte de degraus multiplicou os
estados. Com ≥8 chaves de hash igual, o HashMap vira árvore e procura pelo `compareTo`, então a chave com
outra prioridade não é achada. O hash do BlockPos se repete com y+31 e z−1.

**Why:** os GameTests pequenos nunca têm estados suficientes para treeificar; o bug só aparece no mundo real.

**How to apply:** registro ou classe usada como chave de mapa nunca implementa `Comparable` inconsistente com o
`equals`. Para fila de prioridade, use `PriorityQueue(Comparator.comparingInt(...))`. Para testar, force o mesmo
hash (`CorridorStateTest`).

Relacionado: [[corredor-e-mineiro-profundidade]].
