---
name: peca-de-parede-sem-direcao
description: A planta perde o facing; ladder/wall_torch do miolo ficavam facing=north e eram adiadas para sempre — a obra nunca fechava
metadata:
  node_type: memory
  type: project
  modified: 2026-09-25T12:10:35.664Z
---

Visto em jogo em 2026-09-25: o templo de planície ficou aberto por três
sessões com 9 peças "every remaining piece is waiting for physical support"
(4 `ladder`, 5 `wall_torch`). É a mesma lacuna que [[riscado-nao-e-assentado]]
deixou "em aberto".

Causa: `StructureBlueprintReader` lê `Properties` da paleta mas descarta o
`facing`; `BlueprintBlock` só tem posição e id. `BlockShaping.facing` só vira
a BORDA da caixa; no miolo a peça fica no estado padrão (`facing=north`,
apoio procurado ao SUL). Havia pedregulho a oeste/norte/leste e ar ao sul.

Correção (25-09): `BlockShaping.leanOnAWall` + `ConstructionProject.retry`
chamado em `reconsiderDeferredPieces` quando a peça já cabe. Pendente no TODO:
guardar o `facing` na planta (conserto da aparência).

**Why:** a vaga de obra é única; obra que não fecha para a vila inteira.
**How to apply:** "waiting for physical support" com parede ao lado = direção
errada, não falta de apoio. As peças adiadas estão no save: leia
`saves/<mundo>/data/villagecolony_colonies.dat` (NBT gzip) → `projects[].
deferredPieces` traz posição, bloco e o fingerprint dos 6 vizinhos. Com
"resumed ... N to go" crescendo entre sessões, lembre que a retomada relê o
mundo e devolve tudo que foi riscado.
