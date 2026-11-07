---
name: deadlock-no-de-seguranca-sem-enchimento
description: "Playtest 0.3.50 — mina para de vez num nó OPEN que pede enchimento sem ter, e trava a vila inteira em cascata"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-13T22:08:48.395Z
---

Playtest 0.3.50 (13-10, Spark `NDXMZczlmB`, 42 min, mundo "Majest"). TPS 20.0/20.0/20.0,
MSPT mediana ~10 ms (picos 64-80), mod 1,70% — **não é desempenho, é fluxo**.

`time_ledger`: MINER work **0%** (wait 62%), SMELTER work 0% (wait 62%), BUILDER 88%
ocioso+bloqueado, MASON 84% idle, LUMBERJACK 77%. Regra 51 reprova em quase todos.
`action_report`: MINER MINED 0.2/min, 0 tarefas; BUILDER 97× / MASON 41× / CARPENTER 20×
"SET_ASIDE: no material"; SHEPHERD 17× solta COLLECT_WOOL "no sheep with wool in the radius".

**Raiz (deadlock), provada por log:** a mina minerou bem DONE 6→35 em ~70 s (17:44:41→17:45:52),
depois **nenhum nó novo em 41 min**: census congelou em `OPEN=19, DIGGING=2`. O nó (-1, 2) fica
OPEN esperando `secureCorridor` pôr piso/beirada, que precisa de enchimento (cobblestone) dos
6 baús mais próximos (`NetworkSafety.CHESTS`, `ColonyChests.nearestFirst`). Log 17:51:10:
`made the way safe — 1 blocks placed, 26 left (no fill block in the chests)`. Sem enchimento,
`nextTarget` cai em `quarryFor` (cavar a parede), que **retornou vazio** (sem "digging the wall
for them" no log). Então `MinerSteps.startNextStone` recebe `Optional.empty()` → `return true`
→ mineiro ocioso "looking for stone, wants coal, 0 of 20" para sempre (400× no log vs 48 digging).

O caminho da célula recusada sela o nó após 3 falhas (`corridorHasClosedButAllRefused` →
`failed`); o caminho **"sem enchimento E sem pedreira" não tem saída**: não sela, não desce
(`exhausted()` é falso porque há OPEN), não troca de nó. É o buraco do `NetworkDigging.nextTarget`
(linhas ~165-174) + `startNextStone` (`found.isEmpty() → return true`, MinerSteps.java:437).

Detalhe: "cobblestone" aparece **471×** no log — não falta cobblestone na vila, falta **perto da
mina**. A 0.3.50 ("espalha excedente para baús sem profissão") pode ter afastado o enchimento da
boca (superfície -392,68; nível -377,38). Suspeita a testar: transferência automática espalhou o
cobble para longe de onde a mina precisa.

**Corrigido na 0.3.51** (JAR `EE1D0A4F…`, não jogado): decisão do autor — toda a construção
interna da mina (piso, beirada) **surge** quando o mineiro precisa, sem gastar recurso da vila,
como o `MineFlooding` já fazia para o fluido. `NetworkSafety.takeFill`/`ColonyChests`/`NO_FILL_*`/
`quarryFor` removidos; `secure()` sempre fecha a passagem. Teste novo
`withoutFillInChestsTheMineFillsItselfAndFinishesTheNode` (gametest), mutação (pôr ar) o derruba;
test 1503/0, runGametest 761/761. **Falta playtest** confirmar que a mina volta a produzir e a
cascata se desfaz. Relacionado: [[mineiro-preso-na-descida]], [[took-zero-e-bau-cheio]],
[[regra-45-baus-da-vila]], [[mineiro-mina-em-rede]], [[spark-com-action-report]].
