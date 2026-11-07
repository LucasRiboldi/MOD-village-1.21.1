---
name: casa-nao-sobe-e-fome-nao-travamento
description: "não vi construção de casa" era fome de oak_log e wheat, não obra travada — a raiz é a roça parada e o lenhador recusando
metadata:
  type: project
---

Sessão de jogo de 2026-09-11 (23:03–23:32, jar `249283ca`, 20 colônias / 380
trabalhadores). Relato do autor: *"não vi nada acontecendo, todos se moviam,
não vi construção de casa"*.

**A construção não estava travada.** O log mostra a obra andando:

```
23:07:51  resumed plains_small_farm_1 — 33 blocks already standing, 58 to go
23:08:21  BUILDING, 58 blocks left
23:08:51  BUILDING, 42 blocks left      <- 16 blocos em 30 s
23:09:04  stopped — no minecraft:oak_log in the colony chests
23:09:21  WAITING_RESOURCES, 31 blocks left
23:27:55  WAITING_RESOURCES, 28 blocks left — waiting for minecraft:wheat
23:28:25  gives up — 28 blocks never came in 20 cycles
```

Ela foi de 58 para 28 blocos restantes em 20 minutos e morreu esperando
material. O autor olhou uma obra de 91 blocos já pela metade que passa mais
tempo faminta do que assentando.

**A cadeia de fome tem raiz única, e são os outros dois achados da sessão:**

- falta `oak_log` -> quem traz é o lenhador -> e o lenhador gasta o turno
  recusando as mesmas árvores ("out of reach", "11 logs without a living
  canopy", os mesmos pares de coordenadas reaparecendo a cada expiração de
  6000 ticks). Ver [[sessao-2026-09-06-07-lenhador-e-barrier]].
- falta `wheat` -> quem traz é a roça -> e a roça **nunca abriu**, 13 vezes:
  `no farmer work: still sweeping — the budget ran out before an answer —
  nothing ripe and no empty plot within 32 blocks of the village`. Note que a
  mensagem não diz "não há lote": diz que **o orçamento acaba antes da
  decisão**. Ver [[roca-sem-lote-trava-a-vila]] e [[e42-lacuna-do-planejador]].

**Why:** três queixas que pareciam independentes são uma cadeia de suprimento
com duas pontas quebradas. Consertar o construtor não faria casa nascer — ele
já funciona.

**How to apply:** o P0.7 é o orçamento da varredura da roça (`RingSweep`), não
o construtor. Dois efeitos colaterais a lembrar: a `small_farm` que morreu
deixou **lote permanentemente ocupado** (*"The half-built house and its lot
stay taken"*), e o `plains_butcher_shop_2` que abriu em seguida tem 383 blocos
— com a mesma cadeia, não sobe também. O ciclo também passou de um tick:
`116 ms — planner 83 ms` (72% no planejador).
