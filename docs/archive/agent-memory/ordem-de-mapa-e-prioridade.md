---
name: ordem-de-mapa-e-prioridade
description: Map.copyOf devolve mapa SEM ordem e embaralhado por execução — apagou a ordem da planta e virou a prioridade do fabricante em sorteio
metadata:
  type: reference
---

`Map.copyOf` e `Map.of` devolvem mapas imutáveis **sem ordem de iteração**, e a
JDK a embaralha a cada execução da máquina virtual (SALT em `ImmutableCollections`).
Copiar um `LinkedHashMap` com `Map.copyOf` **joga a ordem fora em silêncio**.

Onde mordeu (2026-09-09): `ConstructionProject.remainingMaterials()` montava um
`LinkedHashMap` na ordem da planta e devolvia `Map.copyOf(tally)`.
`ManufacturerWork.produceForWork` percorre esse mapa e **para no primeiro material
que consegue produzir** — então a ordem *é* a prioridade dele, e era sorteada.
Resultado medido: 13 lotes de escada, 7 de tábua, e **zero troncos descascados**
com 59 toras no baú, o construtor parado em `waiting for stripped_oak_log`, e 16
peças riscadas pela barreira.

Correção: `Collections.unmodifiableMap(tally)` — imutável **e** ordenado.

**A lição geral:** quando a ordem de um mapa é contrato, `Map.copyOf` é o inimigo,
e o defeito é invisível na leitura — o `LinkedHashMap` logo acima dá a impressão
de que a ordem está preservada.

**Ao testar ordem embaralhada, use muitos elementos:** com 2 chaves o teste passa
por acaso metade das vezes. Com 6, é 1 em 720. Foi conferido nos dois sentidos.

O contrato silencioso ficou travado em `ConstructionProjectTest`
(`theFirstMaterialIsTheOneTheBuilderNeedsNext`): o primeiro material da lista é o
do `nextBlock()`, que é o que trava a obra. Ver [[roca-sem-lote-trava-a-vila]].
