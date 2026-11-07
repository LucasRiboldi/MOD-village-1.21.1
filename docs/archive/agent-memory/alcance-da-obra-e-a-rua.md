---
name: alcance-da-obra-e-a-rua
description: "O raio de 64 é da detecção de vila, não limite de crescimento — quem responde \"obra alcançável\" é a rede de ruas"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-17T02:27:42.980Z
---

Decisão do autor em 2026-09-16, no C4 do E46: **o scanner estava certo e o
guarda errado.**

`RoadExtension.consider` ordena as pontas candidatas da **mais distante para
a mais perto** — *"a rua cresce pela ponta, e não pelo meio"*. A estrada é
**projetada para sair do raio de 64**, então pôr teto no índice de ruas
seria remendar o sintoma de uma intenção deliberada.

**O raio de 64 é da detecção de vila, não um limite de crescimento.**

O guarda (`ConstructionProject.isOutOfReach`) passou a medir da **rede de
ruas**, via `ColonyRoads.blocksToTheNearestRoad`: larga a obra a mais de
`BESIDE_THE_ROAD` (16) blocos de qualquer rua. **Sem índice cai na régua
antiga** — não saber onde estão as ruas não é o mesmo que não haver nenhuma.

**Why:** o guarda existe para um defeito real (obra inalcançável segurando a
vaga única em silêncio, 2026-09-15), então afrouxá-lo sem cuidado o
ressuscita. O que impede isso é o **par** de testes: um afirma que a obra
encostada na rua e longe do centro fica; o outro afirma que a obra longe do
centro **e** de qualquer rua continua sendo largada.

**How to apply:** ao mexer em alcance, distância ou raio neste mod, pergunte
primeiro *alcançável por quem, e por onde ele anda*. E ao afrouxar qualquer
guarda, escreva também o teste que prova que o defeito original continua
pego — senão o conserto vira regressão silenciosa. Relacionado:
[[e46-obra-fecha-sozinha]], [[obra-orfa-por-deriva-do-centro]],
[[indice-de-ruas-vira-rua-candidata]].
