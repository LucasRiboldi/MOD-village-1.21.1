---
name: mineiro-em-cima-da-mina
description: "Estar perto da passagem não é estar nela" — orderIndexNear aceitava 8 blocos e o mineiro na superfície recebia passo dentro da rocha
metadata:
  type: project
---

Visto em jogo 2026-09-09: *"o mineiro está perdido, rodando no próprio eixo"*.

**O mecanismo.** `MinerReach.stepAlongTheShaft` ancora em
`orderIndexNear(villager, ...)`, que aceitava qualquer posição da ordem de cavar a
até `LEG` (8 blocos). O aldeão **na superfície em cima da mina** passa nesse teste,
e daí a cadeia inteira mente: ela é contígua *dentro da ordem*, e os dois guardas
(E32 bloco não cavado, E34 parede) só olham daí para a frente — nenhum verifica o
**primeiro elo**, isto é, se o aldeão está no corredor.

```
he is at 2430,64,-1435 · walking to 2427,61,-1429 · boca em 2426,63,-1432
stall 1198 → 1798 → 2398/2400   ·   still 0/300
```

**`still 0/300` é a assinatura**: o guarda de imobilidade não pega quem **gira**. A
navegação não traça caminho para dentro da rocha, vira o aldeão para o alvo e o
deixa lá. Só o guarda de travamento o solta, 2 min por tentativa.

**A calibração foi medida, não escolhida** — o caracol entrega cada degrau como
três posições (piso, peito, cabeça):

| de onde | posição mais próxima | distância |
|---|---|---|
| na boca | piso | 1,00 |
| em cima da mina | **cabeça** | 3,16 |

Por isso `REACH` (4) **não serve**: a cabeça da escada cabe nele. A constante nova
é `IN_THE_PASSAGE = 2`. São três perguntas diferentes — braço (cavar), perna
(andar), e *onde ele está*.

**A lição de teste:** `fromTheSurfaceHeAimsForTheMouth` já existia e passava — põe o
aldeão a 20 blocos, onde a ordem já está fora de alcance. O buraco era o aldeão
**perto**. Teste de superfície tem de ficar *em cima* da mina para valer.
Ver [[gametest-copia-do-predicado]].
