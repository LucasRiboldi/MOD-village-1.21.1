---
name: mineiro-directpath-falso-caminho
description: "Spike 0.3.58 — o MineApproachPath não falha; ele é curto-circuitado pelo MinerDirectPath, que valida um falso caminho à superfície acima da pedra (findPathTo sobe o alvo sólido até a superfície). Mineiro fica em y89-93 sobre alvo y80."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T01:52:46.913Z
---

Spike (14-10, playtest 0.3.58 Spark `YOT1oqyhIv`): **por que o mineiro não chega à frente/boca.**

**Causa raiz (provada por código + log):** não é o `MineApproachPath` que falha — ele mal roda.
Em `MinerSteps.step` (~linha 260), a ordem de escolha do alvo de caminhada é:
1. `MinerDirectPath.reaches(world, villager, job.approach)` → anda direto à pedra. **← pega aqui**
2. `NetworkRoute.waypoint` → rota em rede.
3. `down.isPresent()` → **só aqui** entra `MineApproachPath.toward` (a boca).
4. `MinerLeg.legTowards` (perna antiga).

`MinerDirectPath.reaches` faz `findPathTo(approach, 0)` e aceita `reachesTarget()`. Mas o
`MobNavigation` do Minecraft **sobe o alvo sólido/distante até a superfície** (é o E32, comentário na
linha 252 de MinerSteps: "SOBE o alvo até sair da rocha, e dentro de uma mina isso é a superfície").
Então para uma pedra em y80 a 30+ bl, `findPathTo` devolve um caminho que chega à **superfície acima
da pedra** com `reachesTarget()==true` → `reaches=true` → o mineiro anda direto e **nunca desce**.

**Evidência no log:**
- 461 linhas travadas "walking to <pedra>" vs só 3 "walking to the mine mouth" — o ApproachPath (passo
  3) quase nunca roda; o DirectPath (passo 1) domina.
- Alvo em y80-81, mineiro parado em **y89-93** (≈10 bl acima, na superfície). Confirma o falso caminho.
- `went one level deeper` 1× em 100 min; GAVE_UP 22× "not moved a block"; closest não encolhe.

**Implicação para o conserto:** o problema é o **falso positivo do `MinerDirectPath`** — ele aceita um
caminho cujo fim está longe (no eixo Y) do lugar de pé real da pedra. O `MineApproachPath` já tem a
lógica certa (waypoints por caminho parcial real), mas é curto-circuitado. Direções possíveis:
(a) `MinerDirectPath.reaches` exigir que o fim do caminho esteja perto do approach **nas 3 dimensões**
(hoje `getManhattanDistance <= NEAR` com NEAR=2, mas `reachesTarget()` passa direto sem checar Y);
(b) reordenar para o ApproachPath cobrir também o caso "alvo fundo, não só boca";
(c) unificar DirectPath + ApproachPath num só julgador de caminhada.

**Refinamento (investigação 2):** o defeito é **intermitente, não terminal**. Timeline de minerações
reais ("settle at"): bursts em 18:00-07, **deserto 18:08-19:50** (1h30 quase sem minerar, onde se
concentram os 461 "walking to" travados), e bursts grandes de novo 19:51-20:57 descendo até y70.
- **Quando chega à frente, minera ~1 bloco/tick** ("settle after 1 ticks", frente inteira em sequência).
  A rota boa existe e leva — o que prende é a janela longa entre bursts (o falso caminho à superfície).
- A mina progrediu: completou 1 nível (DONE=150) e desceu 1×; no fim já cavava y70. Não travou de vez.
- Segundo defeito em série, **depois** de chegar: `closest` encalha em **4,1-5,2** (fora do REACH 4) —
  é o NO_STANDING_POSITION/approach (A1/A3), que só aparece quando ele já está perto.

**Conclusão do spike:** dois defeitos em série. (1) **DirectPath falso** prende a chegada em janelas
longas → o conserto principal. (2) **approach encalha a 4-5 bl** → secundário, para depois. Endurecer o
DirectPath é seguro: a cascata cai para ApproachPath/perna, que comprovadamente levam (bursts provam).

**CORRIGIDO (14-10, branch `fix/miner-directpath-superficie`, commit 6197a276, SEM merge/PR ainda):**
`MinerDirectPath.endReachesApproach(end, approach)` exige o fim do caminho a ≤NEAR(2) Manhattan 3D do
approach (Y incluído); removido o `reachesTarget() ||` que curto-circuitava. Verificado: test 1518/0
(3 novos em MinerDirectPathTest); runGametest 769/769 exit 0 (1ª rodada foi flake ~25%); mutação
(ignorar distância) derruba `endOnTheSurfaceAboveTheStoneDoesNotReach`. **Falta: JAR + playtest de dia**
(provar que o mineiro chega e desce — `went one level deeper` > 1, MINED/min sobe). O defeito 2
(approach 4-5bl) continua aberto para o próximo ciclo.

Relacionado: [[mina-longe-por-vila-grande]], [[percurso-descida-mina-waypoints]],
[[frase-ausente-refuta-mecanismo]], [[playtest-0358-spark-YOT1oqyhIv]], [[lento-nao-e-travado]].
