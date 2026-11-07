---
name: selagem-de-teste-com-bloco-solido
description: para declarar terreno inaproveitável em GameTest de lote use vidro; pedra é sólida e vira base de aterro
metadata:
  type: feedback
---

Em 2026-09-30 o aterro passou de 1 para 3 camadas e nove cenários de
`RoadExtensionGameTest` passaram a achar lote no chão do mundo plano (2 abaixo
da rua da arena). Selei o entorno com pedra e piorou: `LotGround.isLotGround`
aceita **qualquer bloco sólido**, então a pedra virou a base do aterro de 1
camada. Vidro resolveu: não é vazio (`isNothing`) nem sólido para a régua.

**Why:** "não é chão natural" (isNaturalGround) e "apoia aterro"
(isLotGround) são perguntas diferentes no código.

**How to apply:** ao tornar terreno inaproveitável num cenário, use bloco
não sólido e não substituível (vidro), e imprima a origem do lote aceito na
asserção antes de chutar a geometria — a arena é rotacionada e
`getRelativePos` engana. Ver [[teste-vazio-por-condicao-impossivel]].
