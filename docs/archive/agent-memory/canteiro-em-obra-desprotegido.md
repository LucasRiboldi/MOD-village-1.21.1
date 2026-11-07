---
name: canteiro-em-obra-desprotegido
description: BlockProtection consultava só Building (obra terminada); a obra em andamento ficava desprotegida e o lenhador colhia a parede
metadata:
  type: project
---

`BlockProtection.isColonyBuilt` perguntava só ao registro de `Building`, e
`Building` **só nasce quando a obra termina** (ou é abandonada). A obra em
andamento não estava em lugar nenhum que a proteção consultasse.

Resultado visto em jogo em 2026-09-16: o construtor assentava tronco na
parede e o lenhador ia colhê-lo como árvore, **na mesma coordenada**:

```
lumberjacks: ... walking — tree at 2503, 63, -3035, block 1 of 1
builders:    1 working, BUILDING at ColonyPos[x=2503, y=63, z=-3035]
```

`block 1 of 1` é a assinatura — um tronco solto, que é parede e não árvore.
Junto vinha `No OAK sapling at ... — Block{minecraft:oak_log} is in the way`.

**Why:** o registro guarda *o que a colônia levantou*, não *o que ela está
levantando*. A janela desprotegida é exatamente quando há material solto no
canteiro — e numa vila de planície esse material é tronco de carvalho.

**How to apply:** ao proteger bloco, cubra **os dois estados**: obra aberta
(`CONSTRUCTIONS.all()` + `Building.of(project).contains`) e casa pronta
(`BUILDINGS.isColonyInfrastructure`). O mesmo furo vale para qualquer
profissão que quebre bloco perto de obra — mineiro e pedreiro inclusive.
Relacionado a [[marca-escrita-e-nao-lida]]: o dado existia, ninguém o lia.
