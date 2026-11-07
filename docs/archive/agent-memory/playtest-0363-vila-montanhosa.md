---
name: playtest-0363-vila-montanhosa
description: "Playtest 0.3.63 (Spark D391hWELDL, versão confirmada) numa vila montanhosa (y60-106, 46 blocos de amplitude). Padrão transversal: várias profissões não alcançam alvos em níveis de altura diferentes (plantação/ovelha/baú/cama). 22× \"no path home at night\"."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T13:16:55.588Z
---

Playtest **0.3.63** (15-10, Spark `D391hWELDL`, 14 min, versão confirmada `villagecolony 0.3.63`).
Vila **dispersa em montanha**: aldeões espalhados de **y60 a y106** (46 blocos de amplitude).

**Padrão dominante: o terreno vertical quebra o ALCANCE de várias profissões.** Não é defeito de uma
profissão — é transversal, o desnível corta o caminho (aldeão Vanilla sobe 1 bloco, cai 3):
- FARMER: "could not reach the crop" ×2.
- SHEPHERD: "could not reach the sheep" ×4, 56% blocked, STRANDED "frozen twice on the same spot" ×2.
- SMELTER: "unable to reach N,N,N" ×2.
- MINER: TOO_HIGH ×23 + NO_STANDING ×14 (caverna/parede de montanha; REROUTED rodou ×0.1).
- **22× "no path home at night"** — aldeões teletransportados para a cama porque o desnível corta o
  caminho de volta (ex.: de y63 para cama em y70/y78).

**O que existe e o que falta:** `ClimbOut`/`ClimbTerrain` destrava quem já está ENCALHADO (roda ×7,
"after 0 steps"), mas **não** resolve ALCANÇAR um alvo em outro nível. Só a mina tem "climbable walk
target" (`MinerApproach.climbableWalkTarget`) — as outras profissões não têm equivalente. `BedTeleport`
tapa o buraco do retorno (teletransporte), mas é remédio, não alcance.

**SPIKE (leitura das posições) — corrige o diagnóstico: são TRÊS causas distintas, não "alcance
vertical" genérico:**
1. **FARMER = falta de lote plano** (não alcance). Ele CHEGA à roça em y81 e colhe (`harvested 4 at
   -138,81,712`); o "could not reach the crop" é pontual. O travamento real é **"no empty plot within
   32 blocks" ×9** — a montanha não tem área plana para expandir a roça (wait 59%).
2. **SHEPHERD = laço de encalhe num ponto** (`-164,86,753`): "frozen twice on the same spot" a cada 2
   min, o ClimbOut solta e ele **reentra no mesmo bolsão**. Não é alcançar a ovelha — é ficar preso.
3. **MINER = parede de montanha** (TOO_HIGH ×23, NO_STANDING ×14) — já tratado parcialmente pelo A3.

**DEFEITO CONCRETO achado:** as **7 chamadas do ClimbOut foram "after 0 steps"** — ele declara que o
aldeão saiu sem movê-lo de verdade neste terreno. É a causa do laço do shepherd (solto mas não movido).
`BedTeleport` tapa o retorno (22× "no path home at night"), aceitável como salvaguarda.

**Direção de melhoria (a decidir com o autor):** o alvo mais claro e contido é o **ClimbOut "0 steps"**
— por que ele não move o aldeão num bolsão de montanha. Depois, a falta de lote plano do farmer (expandir
roça em terreno acidentado) e o alcance da mina (já em andamento).

**CORRIGIDO (0.3.64, 15-10, branch fix/climb-nao-mineiro-so-natural, commit fc11a631):** regra do autor
— **só o mineiro remodela o terreno para escalar**. O ClimbOut caía para PILLAR/TUNNEL para qualquer
profissão; agora `ClimbPolicy.mayReshapeTerrain` = só MINER. Não-mineiro: `next()` fica em STAIRS; o
`stairs()` só aceita degrau natural (toBreak vazio), senão `StrandedEscape.naturalStepAround` (raio 6)
procura um, e sem nada `ClimbPolicy.leaveToNavigation` larga o encalhe sem tocar o mundo. Extração de
classe (F2): ClimbPolicy + ClimbReshape (túnel do mineiro); ClimbOut 499→452. test 1526/0; runGametest
769/769; JAR 0.3.64 = 2F946991... **Falta playtest** na vila montanhosa (pastor sai do laço).
Pendente ainda: lote plano do farmer em montanha; mina (A3 em andamento).

Relacionado: [[climb-out-nunca-desiste]], [[folga-de-chegada-contra-alcance]],
[[mineiro-directpath-falso-caminho]], [[regra-tres-buscas-material]].
