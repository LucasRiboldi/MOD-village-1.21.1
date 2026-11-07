---
name: gametest-nao-equipa-trabalhador
description: A bateria de gametest cria trabalhador de mãos vazias — só WorkerEquipmentGameTest chama equip
metadata:
  type: project
---

Dos 52 `assign(ProfessionType.*)` espalhados por 12 arquivos de gametest,
só os de `WorkerEquipmentGameTest` chamam `WorkerEquipment.equip`. Todos
os outros criam um trabalhador de **mãos vazias**.

Isso ficou invisível enquanto `MinerWork.TOOL` e `TreeFelling.TOOL` eram
constantes (diamante e ferro): o tempo de quebra não olhava a mão. Em
2026-09-04 o `BlockBreakTime` passou a perguntar à mão, e três testes de
mineiro estouraram na hora — pedra saltou de 6 tiques para 150.

**Why:** teste que mede tempo de trabalho sem equipar mede a mão nua, não
a profissão. Passa por folga no `tickLimit`, não por estar certo.

**How to apply:** ao escrever ou depurar gametest que meça tempo de
coleta, chamar `WorkerEquipment.equip(world, List.of(worker))` logo depois
do `assign`. Três testes de `MinerGameTest` já o fazem; os outros 49
`assign` continuam de mãos vazias e são dívida registrada. Ver
[[contagem-honesta-de-testes]] e [[gametest-registro-silencioso]].
