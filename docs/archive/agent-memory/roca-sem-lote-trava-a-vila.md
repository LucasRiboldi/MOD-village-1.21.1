---
name: roca-sem-lote-trava-a-vila
description: Roça devida sem lote ao alcance do fazendeiro encerrava o ciclo do planejador e bloqueava TODAS as obras — a vila inteira parava
metadata:
  type: project
---

Visto em jogo 2026-09-09, colônia `634bf5cc`, uma hora de sessão.

**O sintoma que o autor relata é "não vi os trabalhadores trabalhando".** A causa
não é do trabalhador: é a colônia sem tarefa nenhuma — **78 de 108 ciclos com
`assigned 0 tasks (0 open)`**, com 86 pedregulhos e 83 tábuas paradas no baú.

**A cadeia:** `ConstructionPlanner.plan` escolhe as plantas da roça quando
`FarmPlans.owedToThePopulation` é true (1 roça por 15 aldeões). Se o lote que a
varredura devolve está fora de `withinTheFarmersReach`, ele retorna `silent(...)`
— **encerra a passagem e nunca tenta uma casa**. O lote de amanhã é o mesmo de
hoje, então a recusa se repete para sempre: `sweep: 108 planner runs, 108
answered by the index, 0 complete rounds`.

Sem obra aberta não há pedido de tábua nem de pedra além do piso, então o
construtor e o mineiro ficam sem tarefa. O lenhador continua (tem meta de
madeira), e é por isso que a vila parece meio viva.

**Correção:** `FarmPlans.postpone/postponed` — a roça recusada cede a vez por 20
ciclos e as casas passam. A cota continua de pé; só muda quando é cobrada.
Coberto por `FarmPostponementTest` (6 casos, stdlib).

**A lição de diagnóstico, que vale além deste caso:** "trabalhador parado" quase
nunca se investiga no trabalhador. A linha que resolve é `assigned N tasks (N
open)` — com 0 aberto, o defeito está no que **abre tarefa** (metas atingidas ou
planejador travado), não em quem a executa. Ver [[sessao-2026-09-06-07-lenhador-e-barrier]].

**Efeito colateral do E3 (mineiro para na meta):** ele tornou a ociosidade
*visível*. Antes o mineiro cavava para sempre e sempre parecia ocupado, mesmo com
a meta cumprida. Parar na meta está certo — mas some o "movimento de fachada"
que escondia a vila sem obra.
