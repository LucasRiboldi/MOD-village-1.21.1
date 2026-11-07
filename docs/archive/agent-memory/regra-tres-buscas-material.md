---
name: regra-tres-buscas-material
description: Regra do autor 2026-10-03 — material não achado em 3 buscas aparece no baú de quem usa (natureza inclusive); revê a decisão de 26-09
metadata:
  node_type: memory
  type: project
  modified: 2026-10-03T06:09:40.094Z
---

Pedido do autor em 2026-10-03: "se não há material necessário depois de 3 tentativas de
localizá-lo, deve aparecer no baú da profissão que precisou; se achado no bioma alcançável,
o aldeão vai buscá-lo; pode estar em qualquer baú da vila, inclusive do jogador".

Implementado: `LocateFallback` (3ª varredura vazia do `EmptySweeps` → `BiomeConstructionSupply
.stockAfterEmptySearches`; fundidor se a fornalha transforma, senão construtor), `EmptyFlock`
(pastor sem ovelha), recusa de natureza removida de `stock`/`stockForCraftsman`. Castigo entre
buscas 1/2/4 min (era 5/10/20).

**Why:** a decisão de 26-09 ("natureza nunca aparece") existia porque a regra fabricava tora
que a profissão achava; agora só entra o que a busca real não achou três vezes.
**How to apply:** não reintroduzir recusa de natural; se aparecer material demais no baú,
olhar se a "busca vazia" está contando busca incompleta (paused). Duas decisões abertas do
autor: ociosos da cota ADR-011 (8 vagas/15 adultos) e pedreiro ocioso. Ver
[[playtest-2026-10-03-madrugada]], [[rota-na-receita-nao-e-rota-no-mundo]].
