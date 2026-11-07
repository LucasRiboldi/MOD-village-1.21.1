---
name: entradas-e-velas-0437
description: "0.4.37 (Regras 210 a 212): as velas do autor eram a tocha da colônia do carpinteiro; portão da mina lido do mundo; a ordem dos nós dava grafo oscilante"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-06T13:49:01.781Z
---

Candidata 0.4.37, de 2026-11-06, não jogada. O autor pediu entregue pronto, então os quatro pedidos foram feitos sem as opções do §0.2.2.

- "Velas no chão": o save tinha 0 velas na superfície e 13 `colony_torch` no chão, postas pelo carpinteiro (`IdleHelps`). Tirei essa ajuda. Se o autor ainda vir velas, a leitura estava errada.
- Portão da mina: nada no save guarda a boca da superfície, e `shaft().entry()` desce com a mina. `MineGates` lê o portão no topo da coluna durante a varredura do grafo, 32 blocos além da caixa da vila; a casa do mineiro usa `MineEntrance.surfaceOf`.
- A casa do mineiro já existia (Regra 175), mas buscava lote em volta do `entry` fundo e não acompanhava a boca nova.
- `RoadChains.Node` não tem `hashCode`: `HashSet`/`HashMap` de nós dava ordem por identidade, e o traçado mudava entre leituras. Só o `RoadGeometryPinTest` flakeando revelou. Provável causa de `nodes=84/85/87` alternando no `VC_ROAD_GRAPH`; falta confirmar em jogo.
- `Set.of().removeIf` lança sempre, mesmo vazio: quebrou a releitura do grafo e só o Sentinela completo pegou, porque o teste focado sempre tinha portão.

**Why:** o gate do PIT parou a PR 54 em 78%; o autor escolheu testar em vez de excluir. O registro `road_geometry_pin.txt` mata mutante, mas só diz que nada mudou, não que está certo.
**How to apply:** traçado mudou de propósito: `ROAD_PIN_UPDATE=1 ./gradlew.bat test --tests '*RoadGeometryPinTest*'`, olhando o efeito antes. Próximo jogo: conferir `TODO.md`, Abertos do jogo da 0.4.36.
