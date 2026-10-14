# Percurso por etapas até a boca da mina — design (2026-10-14)

## Objetivo

O mineiro não alcança a frente de trabalho quando a boca da mina está longe (playtest 14-10,
Spark `MiIM0AisSh`: alvo a ~95 blocos, `walking to nowhere`, MINER 0%). A causa não é a falta de
uma rampa — o caracol de descida (`MineShaft`) e o roteamento boca→nível (`MineEntrance.legDown`)
já existem. A causa é a **primeira perna**: o mineiro recebe a boca como alvo único de caminhada,
e a navegação Vanilla não traça um caminho tão longo de uma vez.

Esta mudança dá ao mineiro um **percurso por etapas até a boca**, usando **caminhos reais da
navegação Vanilla** (nunca divisão geométrica de uma reta). Foco estrito no percurso/descida;
**não** reescreve a mina nem a rede de túneis (ADR-042 permanece: túnel 1×2, espaçamento 4, boca
na borda).

Decisão do autor, 2026-10-14 (abordagem A, híbrida), com os ajustes das duas rodadas de revisão
dele incorporados abaixo.

## Comportamento confirmado no código (fonte de verdade, não presumido)

Inspeção feita antes de escrever (critério de aprovação do autor):

- **`EntityNavigation.findPathTo(BlockPos target, int distance)`** (Yarn 1.21.1,
  `net/minecraft/entity/ai/pathing/EntityNavigation.java`) → devolve `Path` ou `null` quando não
  há caminho. `distance` é a folga ao redor do alvo.
- **`Path`** (`.../pathing/Path.java`): campo `reachesTarget` fixado na construção —
  `reachesTarget()` é `true` só quando o caminho chega ao alvo; **um caminho parcial** (o pathfinder
  achou rota que não alcança o alvo) tem `path != null`, nós presentes e `reachesTarget()==false`.
  `getEnd()` → `PathNode` final (ponto mais longe alcançável); `getNode(i)`/`getLength()` dão os nós.
- **`MinerDirectPath.reaches`** (`fabric/work/MinerDirectPath.java`) já usa
  `villager.getNavigation().findPathTo(approach, 0)` e trata `reachesTarget() || getEnd()` a ≤ `NEAR`
  (2) do alvo como "chega"; cacheia o veredito por `RECHECK_TICKS` (200). É o padrão a reusar para
  não recalcular A* a cada tick e não conflitar com a navegação ativa (um `findPathTo` por janela).
- **`MinerSteps.step`** (`fabric/work/MinerSteps.java`): quando o mineiro está fora do nível,
  `MineEntrance.legDown(world, colonyId, miner, target)` devolve o próximo alvo de descida (a boca
  se ele está longe/na superfície; senão a entrada do nível abaixo). Hoje o código faz
  `WorkTargets.set(workerId, down.get(), MinerReach.ARRIVAL)` direto. Há um guarda de descida
  dedicado: `job.descent.outOfTimeOnDescent(world, villager, target)` → `stuck(...)` com a frase
  "the way down got no closer than …". É nele que a falha de aproximação deve se integrar.
- **Liberação de tarefa (reusar, não inventar):** `MinerSteps.stuck` → `MinerHands.giveUp(world,
  workerId, job, why)` → `job.task.release(why)`; `MineClaims.stepAside` cede a vez na mina;
  `MineDescent.stoppedAt(world.getTime(), pos, job.target)` já registra a parada na descida.
- **`WorkTargets.set(UUID, BlockPos)`** e **`set(UUID, BlockPos, int arrival)`** (DEFAULT_ARRIVAL=2);
  `MinerReach.ARRIVAL=0`, `REACH=4`. O alvo de caminhada é o que muda; o destino da tarefa não.

## Arquitetura

### Onde entra (mudança mínima, um ponto)

No ramo de `MinerSteps.step` em que `down = MineEntrance.legDown(...)` está presente e o código
chama `WorkTargets.set(workerId, down.get(), ARRIVAL)`: interpor
`MineApproachPath.toward(world, villager, down.get())`, que devolve **o alvo de caminhada a usar**:

1. Pede à navegação `findPathTo(boca, ARRIVAL_FOLGA)`.
2. `path != null && (reachesTarget() || getEnd() a ≤ NEAR da boca)` → devolve a **boca** (caso
   "boca a 20 blocos": comportamento atual inalterado).
3. `path != null && parcial` (nós presentes, não alcança) → devolve `getEnd().getBlockPos()` — um
   **waypoint caminhável real**, o ponto mais longe que a navegação alcança no rumo da boca.
4. `path == null` (sem caminho nenhum) → **não substitui por posição arbitrária**: devolve a boca
   como estava e deixa o guarda de descida (abaixo) medir a falha. Nunca inventa waypoint.

O waypoint é só o alvo de **caminhada**; `job.target` (pedra/boca final) e a tarefa não mudam.

### Validação do waypoint (ajuste 1 do autor)

`getEnd()` só é aceito como waypoint quando: o path é válido (não nulo, com nós) **e** o `getEnd()
está mais perto da boca** que a posição atual do mineiro (progresso real em direção à boca). Se o
`getEnd()` não representa avanço, não é usado como alvo novo (segue o alvo anterior e conta para o
guarda de bloqueio). Nunca se aceita posição que não venha de um path Vanilla.

### Recálculo e detecção de progresso (ajustes 2 e 3)

`MineApproachPath` guarda por mineiro: a boca-alvo, o waypoint atual, a menor distância já atingida
à boca (`closest`) e `sinceProgress` (tentativas sem melhorar `closest`). Recalcula o `findPathTo`
quando: (a) o mineiro **alcança** o waypoint atual (dentro de `ARRIVAL`/`REACH`); (b) o
`RECHECK_TICKS` (≈200) venceu — rede de segurança, não o único gatilho; (c) o guarda de imobilidade
do mineiro (`job.stall`/`WorkStall`, que já existe) indica parada. Caminho parcial **não** é falha:
compara `closest` anterior e atual com tolerância pequena; só conta "sem progresso" quando não
melhora.

### Falha explícita (ajuste 4) — sem perder a tarefa

Após `APPROACH_FAILURES` recálculos **sem progresso** (não após um único path parcial), integra-se
ao guarda de descida existente: `stuck(world, villager, job, "no walkable approach to the mine
mouth at X,Y,Z — closest got to D in N tries")`, que já chama `MinerHands.giveUp`→`job.task.release`
e `MineDescent.stoppedAt`. A falha é registrada **uma vez por episódio** (ao disparar `stuck`), não
a cada tick. O estado do `MineApproachPath` para esse mineiro é limpo quando: a boca-alvo muda, a
tarefa é cancelada/liberada, ou o mineiro conclui a aproximação (entra no nível) — ganchos em
`MinerHands.release`/`forget` (padrão do `MinerDirectPath.forget`).

### Limiar de 24 blocos (ajuste do autor: experimental, não regra)

A lógica principal **não depende do número**: sempre tenta o alvo direto e, se o path é parcial,
aproveita o `getEnd()`. O 24 entra apenas como atalho opcional (abaixo dele, pular o `findPathTo` e
ir direto, economizando A*), documentado como **hipótese de teste** numa única constante nomeada
`DIRECT_WHEN_WITHIN` — sem duplicar constantes. A decisão de verdade é sempre `reachesTarget()`/
path parcial, não a distância.

## Fluxo de dados

1. `MinerSteps.step`, fora do nível, `legDown` dá a boca/entrada.
2. `MineApproachPath.toward` pede o path Vanilla; devolve a boca (chega) ou o `getEnd()` (parcial).
3. `WorkTargets.set(workerId, alvo, ARRIVAL)` — o mineiro anda até o waypoint real.
4. Ao chegar, recalcula → próximo trecho; e assim até a boca, de onde o caracol/`legDown` assumem.
5. Sem progresso por N tentativas → `stuck`→`giveUp`→`release` (falha diagnosticável, tarefa cedida).

## Telemetria (ajuste 4 e critério do autor)

Por episódio de aproximação: distância inicial à boca, nº de recálculos, distância ao chegar (ou
`closest` na falha), e o veredito do pathfinding (chegou / parcial / sem caminho). Uma linha ao
concluir ou ao falhar — permite distinguir **ausência de caminho** × **falta de progresso** ×
**chegada à boca**. Sem log por tick.

## Testes

**Regressão/aceitação (GameTest, cenários da tabela do autor):**
- Boca a ~20 blocos com caminho livre → alvo direto, inalterado.
- Boca a ~80 blocos com caminho → avança por waypoints (`getEnd()` sucessivos) e **chega** à boca.
- Obstáculo entre waypoints → recalcula; se não há progresso por N tentativas, **falha explícita**,
  nunca "walking to nowhere" eterno.
- Caminho impossível (boca murada) → `path == null`/sem progresso → falha diagnosticável + tarefa
  liberada.
- A descida pelo caracol segue funcionando após a boca (controle devolvido ao `legDown`).

**Determinismo (ajuste 5):** o GameTest monta o terreno (chão/obstáculos) explicitamente para que
o pathfinding seja reprodutível, e afirma sobre o veredito do path (chegou/parcial) além da posição
final. A **mutação** desliga o uso do waypoint (sempre alvo direto): o cenário de ~80 blocos volta a
não chegar — prova que o mecanismo é essencial.

## Arquivos

**Novo:** `fabric/work/MineApproachPath.java` (o percurso por etapas: `toward`, estado por mineiro,
`forget`), com GameTest `MineApproachGameTest`.
**Editados (mínimo):** `fabric/work/MinerSteps.java` (interpor `MineApproachPath.toward` no ramo da
descida; integrar a falha ao guarda de descida), `fabric/work/MinerHands.java` (`forget` ao liberar).
Nada da rede/túneis/ADR-042 muda.

## Fora de escopo (YAGNI)

Nó central de distribuição + galerias radiais (abordagem B da planta); espiral externa 40×40 em
(+90,0); galeria 3×3; eixos a cada 12; camadas a cada 15; boca fora da vila. A planta serve de visão
de longo prazo; esta mudança entrega só o percurso de descida que resolve o 0% medido, sobre a
estrutura que já existe.

## Versão

Sai como 0.3.55. Commit por marco; JAR e playtest **de dia** (o último foi ~84% à noite) para medir.
