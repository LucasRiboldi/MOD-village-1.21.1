---
name: validar-antes-de-transformar
description: A planta era girada DEPOIS de o lote ser aprovado; o giro troca os eixos e a casa saía da caixa validada
metadata:
  type: project
---

Em 2026-09-16 a casa nasceu em cima da roça da vila. A causa não era o
filtro de solo — era **ordem de operações**.

`ConstructionPlanner.open` comparava `plan.size()` com `site.size()`, e só
**depois** a Regra 17 girava a planta para a porta olhar a rua. Num
retângulo o giro de 90° **troca os eixos**: a casa 13×11 aprovada num lote
13×11 vira 11×13 e ocupa treze blocos onde onze foram verificados.

A assinatura no log: blocos riscados **muito além** da origem, com um vão
vazio no meio (`origem z=-3045`, riscos em `z=-3031`). Caixa contínua não
faz isso — quando os extremos não fecham, a transformação veio depois da
validação.

**Why:** validar a forma de origem e aplicar a transformação depois deixa
a verificação falando de uma caixa que nunca existiu.

**How to apply:** aplique a transformação **antes** do filtro, e corrija
também a **fonte dos candidatos** — aqui, `sizesOf` precisou passar as duas
orientações ao scanner. Sem isso o filtro corrigido não acha planta nenhuma
e a vila para de construir **em silêncio**, que é pior que o defeito
visível. Relacionado a [[obra-orfa-por-deriva-do-centro]] e a
[[lento-nao-e-travado]].

**Hipótese descartada aqui (não repetir):** `farmland` **não** é
`isSolidBlock`; o scanner já a recusa por `NOT_NATURAL_GROUND`.
