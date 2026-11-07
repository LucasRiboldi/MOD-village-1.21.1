---
name: sessao-2026-09-04-seis-defeitos
description: "Diagnóstico dos seis defeitos que o autor viu em jogo em 2026-09-04 — lenhador na mina, mineiro que não cava, tábuas, teto da small house"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-05T02:14:25.022Z
---

Sessão de jogo de 2026-09-04 22:37–22:57 (`%APPDATA%\.minecraft\logs\latest.log`,
colônia `c4706b63`). O autor pediu seis correções; a causa de cada uma, medida no log:

1. **Lenhador minerando / mineiro cortando lenha** — é a 2ª passagem de
   `WorkAssignment.takeOneTask` (a "mão emprestada" da ADR-010, 2026-09-02).
   O log mostra `af897f92 (LUMBERJACK lending a hand)` dentro da mina e
   `d8560cec (MINER lending a hand)` nas árvores — trocaram de função.

2. **Mineiro não cava** — três defeitos compostos em `MinerReach.legTowards`:
   - `outside` comparava o Y do destino com o Y **do aldeão**, que muda de 44
     para 45 enquanto ele anda, e o mesmo alvo era classificado ora
     "superfície" ora "fundo da mina". O destino de caminhada alternava entre
     a boca e o alvo cru: ping-pong eterno, distância travada em 50,7.
   - o ramal reservado pode deixar de ser aquele em cujo corredor o aldeão
     está (o `release` de passagem estéril devolve o ramal e o `claimArm`
     seguinte dá outro), e aí `stepAlongTheShaft` devolve nulo para sempre.
   - tarefa de **areia** não reserva ramal, `armOf` vem vazio, e `legTowards`
     devolve o destino cru — o mineiro a 19 blocos de profundidade fica
     `unable to climb` por 2400 tiques.

3. **Picareta de diamante** — já estava corrigido no commit 49cfb49 (21:34 do
   mesmo dia). O jar de 22:29 tem a correção e o log de 22:37 não traz nenhuma
   linha de troca de ferramenta. O que o autor viu foi de sessão anterior.

5. **Todo tronco vira tábua** — `ColonyGoals`: a meta de tábua é
   `(storedPlanks + plankRoom) / 2` e o único portão é "tem pelo menos um
   tronco". Nada reserva tronco. Medido: `CHERRY_LOG=135, CHERRY_PLANKS=1257`.

6. **Buraco no centro do teto da small house** — não é bloco faltando na
   planta. A camada y=5 do `small_house.nbt` tem um vão de 1×1 **de propósito**
   em (3,5,3), tapado pela calota 3×3 de y=6 (8 `oak_stairs` + 1 `oak_planks`).
   A colônia para em `13 blocks left ... waiting for minecraft:oak_stairs`: fez
   40 das 49 escadas e ficou sem. Sem a calota, o vão de y=5 aparece como um
   buraco no meio do teto. A escada de carvalho precisa de `oak_planks`, e a
   colônia tem 1257 tábuas de **cerejeira** e 2 de carvalho — os lenhadores
   cortam cerejeira.
</content>
</invoke>
