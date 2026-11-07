---
name: rota-na-receita-nao-e-rota-no-mundo
description: "hasRouteInBiome julga pela receita, não pela entrega; obra parou 10 min esperando white_terracotta porque a rota da argila existia no papel e não no mundo"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-22T23:40:08.146Z
---

`BiomeConstructionSupply.hasRouteInBiome` desce a árvore de receitas e diz
"existe rota" quando alguma folha é recurso do bioma. A regra de suprimento
então se cala: se **qualquer** alternativa da família tem rota, a peça é
responsabilidade do ofício e não é depositada.

Medido em jogo (22-09): a obra ficou **dez minutos** em `WAITING_RESOURCES`
por `white_terracotta`. A família tem rota — argila vai à fornalha e vira
terracota — mas o fundidor repetiu, a cada ciclo do começo ao fim,
`none of 14 colony chests had minecraft:clay to smelt`. Naquele mundo não
havia argila ao alcance. **A rota existia na receita e não no mundo.**

**Why:** era um impasse sem prazo — a obra esperava um ofício que esperava um
recurso que ninguém traria. E o `STATE.md` já declarava esse caso resolvido
desde a sessão anterior, o que mostra que o conserto anterior tratou a
seleção da peça e não a entrega.

**How to apply:** a correção foi uma carência — passados
`BiomeConstructionSupply.OVERDUE_TICKS` sem o ofício entregar, a peça
preferida passa a ser depositada no baú da obra, e a entrega reinicia o
relógio daquela peça. **A constante saiu do log, não de chute**: no mesmo
playtest a espera mais longa que *foi* atendida durou cinco ciclos, e a que
nunca foi acumulou vinte; dez ficam ao dobro de uma e à metade da outra. É o
mesmo método de [[log-decide-a-constante]].

Ao ver obra parada, compare a frase de espera do construtor com a do ofício
correspondente: se as duas se repetem no mesmo ritmo por muitos ciclos, é este
impasse, e não lentidão.
