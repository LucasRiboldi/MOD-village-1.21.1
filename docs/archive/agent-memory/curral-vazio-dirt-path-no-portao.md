---
name: curral-vazio-dirt-path-no-portao
description: "0.4.28 teve 0 entregas no curral porque a saída dos dois portões é dirt_path, e isSideSolidFullSquare é falso nele"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-05T17:27:48.651Z
---

Jogo da 0.4.28 (2026-11-05): 14 `VC_PENS` com os dois currais quase sempre em 0 animais, 0 "into the pen" no log, todo
`dropped the herd` com destino "a post", e os animais amarrados nas cercas (137,64,-222) e (137,67,-148).

Causa provável, lida no código e no save, ainda sem prova em jogo: `ShepherdPens.standing` exige
`isSideSolidFullSquare(UP)` no chão. O save mostra `dirt_path` na saída dos portões (79,63,-220) e (105,63,-231). O
`dirt_path` tem altura 15/16 (`FarmlandBlock.SHAPE`), então a face de cima não é cheia e o acesso do cercado é descartado.

**Why:** o pedido do autor sobre o curral grande parecia ser de detecção. O `VC_PENS` já marca `large=true`; o que falha é a
checagem do acesso. A rua nova da 0.4.25 pode ter passado a pavimentar a saída do portão, mas isso não foi verificado.

**How to apply:** ao checar se dá para ficar de pé num bloco, não usar `isSideSolidFullSquare` onde há rua. Outros usos do
mesmo teste: `IdleHelps`, `VillageHelpers`, `PublicChestPlacement`. Ver [[playtest-0417-encalhado-falso-solto]] para a
regra de marcar correção como candidata até o próximo jogo.
