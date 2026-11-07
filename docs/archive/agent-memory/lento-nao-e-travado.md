---
name: lento-nao-e-travado
description: Antes de declarar "impasse fechado", monte a linha do tempo do log — o sistema pode estar funcionando devagar, e o patch errado quebra a regra que já funcionava
metadata:
  type: feedback
---

Em 2026-09-15 li `still sweeping`, `found no road end` e `0 complete rounds`
e concluí "impasse fechado: a rua não cresce". Escrevi o patch que largava
o índice de ruas ao fim da volta vazia. **Onze GameTests quebraram** e
mostraram que a Regra 15 — crescer a rua — roda exatamente nesse ponto; o
patch consumia o momento dela.

A linha do tempo real, que eu não tinha montado:

```
21:45:11  extended the road 2 blocks west
21:46:11  extended the road 3 blocks north
21:47:41  extended the road 5 blocks south
```

A rua **crescia**. O sistema estava lento, não travado — e a lentidão era
a fila do `PlannerTurns` que eu mesmo criara na véspera (16 vezes em 20 min).

**Why:** contadores agregados (`0 complete rounds`, `N recusas`) descrevem
o estado, não o movimento. Só a linha do tempo distingue "parado" de
"andando devagar", e a diferença muda completamente a correção.

**How to apply:** antes de chamar algo de travado, rode
`grep -E "<eventos de progresso>" latest.log` ordenado por hora e olhe se
**alguma coisa avança**. Se avança, o problema é frequência ou taxa, não
lógica. E quando muitos testes quebram de uma vez, **eles estão certos** —
reverta e releia, não conserte os testes. Relacionado a
[[obra-orfa-por-deriva-do-centro]] e [[roca-sem-lote-trava-a-vila]].
