---
name: grupo-de-pedra-e-decisao
description: Separar SANDSTONE de COBBLESTONE parece conserto e desfaz decisão do autor; o defeito era a discordância
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-10T02:36:13.307Z
---

Toda proposta de **separar `SANDSTONE` de `COBBLESTONE` em grupos de recurso
diferentes** deve ser recusada, e o argumento está em
`ResourceSubstitution:143-148`.

**Ela parece certa porque o defeito é real e aconteceu**: em 2026-08-22 a
colônia dava a meta de pedra por cumprida, o mineiro não ia cavar, e o
construtor esperava arenito. Mas o diagnóstico do projeto é mais fino que o
sintoma — **o defeito era a discordância entre a conta e o construtor, não a
substituição**. A correção foi fazer os dois lados concordarem (Emendas 1 e 2
da Regra 27, decisão do autor em 08-26: *"alternativas de recursos para todas
as construções dos biomas"*).

**E o grupo já não é usado para a pedra.** `ResourceTally.amountOfGroup` é
chamado em três lugares, todos `WOOD` ou `PLANKS`. A meta de pedra é posta por
**nome** em `ColonyGoals:414` (`goals.put(stone, ...)`), com a `VillagePalette`
do bioma dizendo qual. Separar hoje só devolveria a dependência da espécie
exata.

**A corrente de responsabilidade** — quem responde por qual material — está
documentada em `docs/technical/Profession-Responsibility.md` e guardada por
`ProfessionResponsibilityTest`. A mutação que a prova: trocar
`case FARMED -> COLLECT_FOOD` por `COLLECT_WOOD` em `ColonyCycle.typeFor` faz
cair **só 2 casos de 686**.

Vale também para propostas de **cadastrar paleta de blocos por bioma à mão**:
o `StructureBlueprintReader` lê o `.nbt` do jogo, e lista escrita à mão é menos
exata e envelhece. A `VillagePalette` tem três eixos só — estilo, pedra, vidro
— porque é só isso que não dá para ler da estrutura.

Ver [[e42-lacuna-do-planejador]] para a outra armadilha de método do projeto:
teste que confere o predicado longe de quem o consome.
