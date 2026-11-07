---
name: cenario-longe-da-arena-e-sorteado
description: "As provas de coleta montam o cenário a 65-97 blocos da arena numa direção sorteada; a arena muda de lugar a cada rodada, e o teste passa a depender do terreno gerado"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-22T23:09:00.679Z
---

`SurfaceGatheringGameTest` (relva, areia, argila, terra) monta o cenário a 65
ou 97 blocos da arena, na direção que `FarthestVillageSector` devolve. Num
mundo de teste não há estrutura de vila em chunk carregado, então
`farthestLoadedSector` cai no ramo de sorteio:

```java
if (structures.isEmpty()) {
    return CARDINALS.get(Math.floorMod(colonyId.hashCode(), CARDINALS.size()));
}
```

e o teste passa `UUID.randomUUID()`. **A direção muda a cada rodada** — e a
própria arena cai em coordenadas diferentes a cada execução (medido:
`-11941544` numa rodada, `-4621311` na seguinte). O cenário acaba montado em
terreno gerado que ninguém controla.

**Why:** isso é a origem da intermitência de ~25% que a bateria carrega há
semanas, e que já foi listada como gate P0 sem diagnóstico. Instrumentar a
mensagem de recusa mostrou **duas causas distintas**, não uma:

- **terra/fazendeiro** — `chunk carregado: true, removido: false`, o aldeão na
  posição exata em que nasceu, e mesmo assim `world.getEntity(uuid)` não o
  devolve. Desde a 1.17 a entidade mora em seção própria e só é registrada com
  a seção em **ENTITY_TICKING**; `setBlockState` carrega o chunk até FULL, que
  não tica entidade. Forçar o chunk (`setChunkForced`) é o que dá o ticket.
- **relva/fundidor** — bloco certo, teto livre, coluna no setor, e
  `BlockProtection.mayBreak` = **false**. `isPlayerPlaced` está descartado (só
  vale para folha com `PERSISTENT`), restando caixa registrada por outra prova
  ou vila que o jogo gerou naquele pedaço.

**Três hipóteses medidas e REFUTADAS em 22-09** — não repetir:

1. *chunk descarregado* — `chunk carregado: true`. E `setBlockState` carrega o
   chunk sozinho, então a suspeita nasce refutada em qualquer posição onde o
   teste escreveu bloco.
2. *falta ticket de tique de entidade* — `setChunkForced` medido com
   `forçado: true`, sem mudar nada.
3. *o ticket só vale no tique seguinte* — nascer o trabalhador no tique 2 e
   medir no 5 também não mudou nada.

O que sobrou medido: `spawnEntity` devolve **true**, a entidade **não** está
removida e está na posição exata, e o mundo não a devolve nem pelo índice de
uuid nem por varredura (`no mundo: false`). **A premissa a questionar é que o
trabalhador consiga existir a 65-97 blocos da arena no GameTest**, não o
carregamento de chunk.

Também medido e descartado: aplainar o terreno em volta (`flatten`) — 4 falhas
em 6 rodadas contra 3 em 5 do baseline, ou seja, sem efeito.

**How to apply:** não trate as falhas de `SurfaceGatheringGameTest` como um
defeito só, e não conserte pelo nome do teste que caiu — elas compartilham a
origem (cenário sorteado longe da arena) mas têm sintomas e consertos
diferentes. Antes de mexer em coleta, raio protegido ou timeout, leia a
mensagem instrumentada: ela diz setor, chunk, bloco, teto e as três portas da
proteção separadas. `[colonia: true]` já apareceu em jogo: é **obra aberta de
outra prova da bateria** — `BlockProtection.isOpenSite` varre
`CONSTRUCTIONS.all()` global e testa contenção de caixa, e as arenas ficam
perto o bastante para uma alcançar o cenário da outra.

Ver [[bateria-gametest-instavel]] (a repetição é o único instrumento, já que
[[rungametest-sem-filtro]] impede rodar um teste isolado) e
[[gametest-interferencia-global]].
