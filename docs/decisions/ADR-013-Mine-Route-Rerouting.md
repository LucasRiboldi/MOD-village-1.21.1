# ADR-013 — Alternância de rota da mina no limite

**Status:** Accepted  
**Date:** 2026-09-13  
**Decision Type:** Architecture / Gameplay / Save Data

## Contexto

No limite de profundidade, a mina encerrava os quatro braços e chamava
`MineShaft.turned()`. Esse método muda apenas `gallery`; `descent` permanece
igual e a hélice retorna às mesmas posições. O log de 2026-09-13 registrou
repetidamente pedra sem espaço para o mineiro ficar e nenhum trabalho nos
braços abertos.

## Decisão

1. A entrada (`entry`) permanece fixa. Quando todos os braços acabam e não
   há nível mais fundo, a mina gira `descent` um lado no sentido horário e
   recalcula a galeria inicial a partir desse rumo.
2. `MineShaft.turned()` continua reservado à curva de galeria e à criação dos
   quatro braços. A mudança de hélice é uma operação distinta (`rerouted`).
3. Os quatro braços reiniciam seus cursores no novo desenho. Blocos já
   abertos são reconhecidos pelo mundo e pulados; a mina não grava snapshots
   nem coloca blocos para criar rota.
4. `MineSave.SHAPE_VERSION` passa de 4 para 5 porque a ordem de posições muda
   após a troca de rota. Saves v4 preservam boca, rumo salvo e estado do arco,
   mas os cursores são zerados para evitar interpretar índices na nova ordem.
5. A sequência é determinística e percorre os quatro rumos cardinais. Trocas
   de rota só ocorrem após todos os braços terem terminado no nível mais
   fundo; níveis que ainda podem descer mantêm o comportamento existente.

## Consequências

- A entrada e o caminho inicial ficam reconhecíveis, enquanto uma barreira
  na hélice não força a repetição do mesmo percurso.
- O mundo permanece a fonte da verdade para túneis já cavados.
- Na migração, a mina pode reexaminar índices já abertos, com custo limitado
  pela leitura incremental; o estado do arco não volta a ser construído.
- A geometria e a persistência têm cobertura unitária. A confirmação de que
  o mineiro efetivamente atravessa a rota alternativa ainda exige playtest.

## Verificação

`MineTest` verifica nova orientação, boca estável e braços reabertos.
`MineSaveTest` verifica migração v4, cursor zerado, arco preservado e
persistência da rota alternativa. `build` passou com 808 testes unitários e
`runGametest` com 314/314; a verificação visual permanece pendente.
