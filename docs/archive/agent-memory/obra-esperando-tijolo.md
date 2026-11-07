---
name: obra-esperando-tijolo
description: 0.3.33 — peça sem ResourceType (bricks) não descia a receita, nenhuma tarefa nascia e o surgimento nunca contava; mineiro 0% aberto
metadata:
  type: project
---

Playtest 0.3.32 (12-10, 09:08–09:34): obra presa em 3 `bricks`. Cadeia: bricks (pedreiro) ← brick
(fundidor assa) ← clay_ball (fundidor coleta). Nenhuma tarefa de brick/clay_ball na sessão.
Causa: `WorkMaterialRecipeNeeds.addSmeltedIngredients` só descia receita de bloco que já é
ResourceType; corrigido em 0.3.33 (178a2637, JAR 021C1176…), recursivo 3 degraus, ignora IRON_INGOT/GLASS
(contados em WorkMaterials.iron/.glass). Sem tarefa aberta, `ConstructionSupplyDispatches.renewForCycle`
não conta ciclo → o surgimento de último caso nunca dispara: qualquer elo sem demanda trava a obra para sempre.
Armadilha de teste: billFor de brick_stairs usa o cortador (1:1), não a bancada (6→4) — calcule pelo bill.
Aberto (TODO 🔴): mineiro 0% com 3 de 4 ramais esgotados e pedra inalcançável no ramal aberto.
Ver [[playtest-2026-10-11-031]], [[cadeia-de-producao-recursiva]].
