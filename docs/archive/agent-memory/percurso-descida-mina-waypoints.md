---
name: percurso-descida-mina-waypoints
description: 0.3.55 MineApproachPath — waypoints caminháveis (path parcial da navegação) até a boca da mina
metadata:
  node_type: memory
  type: project
  modified: 2026-10-14T06:27:33.239Z
---

0.3.55 (14-10). Pedido: "métodos melhores de percurso para descer na mina" + planta técnica
grande. Decisão do autor (duas rodadas de revisão): abordagem A híbrida — só o percurso/descida,
mantendo a ADR-042 (túnel 1×2, espaçamento 4, boca na borda). A planta completa (espiral externa
40×40, galerias radiais 3×3, camadas a 15) fica como visão de longo prazo, fora de escopo.

Raiz (playtest MiIM0AisSh): o caracol de descida (`MineShaft`) e o `MineEntrance.legDown` já
existem; a falha era a primeira perna — mineiro recebia a boca a ~95 blocos como alvo único e a
navegação Vanilla não traça caminho tão longo → "walking to nowhere".

**`MineApproachPath.toward(world, villager, mouth)`** (fabric/work): interposto em `MinerSteps` no
ramo `if (down.isPresent())` antes do `WorkTargets.set`. Pede `villager.getNavigation().findPathTo
(mouth, NEAR=2)`. Se alcança (reachesTarget ou getEnd a ≤NEAR) → a boca. Se parcial → `getEnd()`
(caminhável real) **só com progresso útil** (`usefulProgress`: encurta `closest` por ≥PROGRESS_MIN=2).
Se `path==null` → devolve a boca, NÃO inventa waypoint; o guarda de descida `job.descent`
(`MineLease.outOfTimeOnDescent`, mede distância à pedra + descer de nível) é quem conta a falta de
progresso e cede a tarefa (`MinerSteps.stuck`→`MinerHands.giveUp`). **Sem segundo sistema de
travamento** (ressalva do autor). Cache por RECHECK_TICKS=200, recalcula ao chegar no waypoint.
`MinerHands.release` chama `MineApproachPath.forget`.

APIs confirmadas no código (não presumidas): `Path.reachesTarget()` true só se chega; parcial =
path!=null + getEnd + reachesTarget false. `findPathTo` logo após spawn dá null — GameTest precisa
`waitAndRun(5)` ([[gametest-navegacao-espera-pousar]]).

**Testes:** `MineApproachProgressTest` (unidade, regra de progresso — mutação sempre-true derruba).
Os GameTests de integração (near/blocked) foram **removidos**: o pathfinder varia entre execuções
num espaço pequeno (2→3 falhas alternando), não dá mutação conclusiva — o autor advertiu disso.
test 1512/0, runGametest 766/766.

**Pendente / honestidade:** a TELEMETRIA prevista no spec (nº de recálculos, distância ao chegar,
veredito do path) NÃO foi implementada no toward — só o comportamento. Falta implementar o log do
percurso e o **playtest de dia** (o critério do autor é o mineiro chegar fisicamente à boca; o
último playtest foi ~84% à noite). JAR 0.3.55 = 4605A53E…, instalado, não jogado. Spec:
`docs/superpowers/specs/2026-10-14-percurso-descida-mina-design.md`. Relacionado:
[[mina-longe-por-vila-grande]], [[pendencias-0354-mina-folhas-esperando]].
