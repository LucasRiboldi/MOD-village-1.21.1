---
name: bau-cheio-destroi-o-que-a-obra-espera
description: "Um bau de mineiro cheio jogou 365 sandstone no chao enquanto a obra esperava sandstone; quem escreve usa 1 bau, quem le usa 21"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-19T15:37:19.267Z
---

Sessão de 2026-09-19 12:09→12:28: a obra parou em
`WAITING_RESOURCES ... waiting for minecraft:sandstone` com **318 de 323
blocos** por pôr, enquanto **365 sandstone** eram jogados no chão.

**A assimetria é a raiz:** o construtor **LÊ** todos os 21 baús da colônia
(`ColonySupply.take`), mas o mineiro só **ESCREVE** no próprio
(`MinerHaul.deposit` → `ChestDepositor.deposit` num baú só). **Um** baú
cheio (668×) bloqueou tudo, com 20 baús livres ao lado.

O javadoc do `MinerHaul` diz que isso é deliberado — *"sem contaminar o
baú de outro trabalhador"* —, então não é descuido: é decisão com motivo
escrito, e `ColonyChests` documenta o risco de revogá-la
("trocaria um defeito de contabilidade por um de desenho").

**E o fundidor é sintoma, não causa:** `SANDSTONE` é declarado
`Production.MINED` em `ResourceType`, e `ColonyCycle.typeFor` roteia pela
produção declarada. Sandstone existe no deserto (por isso o mineiro trouxe
365) **e** é craft de 4 areias — a colônia só conhece o primeiro caminho.

**How to apply:** a decisão do autor foi **puxar, não empurrar** — quando
o baú de uma profissão passa da metade, as outras profissões devem
converter aquela matéria nos blocos que as estruturas do bioma vão pedir.
Drena o baú e adianta a obra ao mesmo tempo. Ver
[[cadeia-de-producao-recursiva]] e [[roca-sem-lote-trava-a-vila]].
