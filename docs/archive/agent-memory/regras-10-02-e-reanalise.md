---
name: regras-10-02-e-reanalise
description: Decisões do autor de 02-10 (Regras 2-e1, 18-e1, 46–50) aplicadas, e a reanálise das profissões com os maiores desperdícios medidos
metadata:
  type: project
---

Decididas e feitas em 2026-10-02 (commits 5ce71b0f..7b0d4d79, JAR 924f7c1e):
Regra 50 (WorkTime → VC_TIME, scripts/time_ledger.py, CLAUDE.md §0.4: >40% sem
trabalhar = fluxo, >10% bloqueado+encalhado = trava); 2-e1/37-e1 (Eficiência e
encantamento no ToolUpgrade/ActionTool, troca pela pilha exata); 49 (cama →
casa do ofício → demais; ConstructionTurn, ConstructionPriority.WORKSHOP; o
mineiro ganhou o tool_smith); F-3 (rastro no WorkMarksSavedData). 18-e1, 46,
47 = o que já acontecia. **48 (recolher do chão só o que a obra espera) NÃO
implementada.**

Reanálise (`docs/research/2026-10-02-reanalise-profissoes.md`,
`scripts/activity_report.py`): carpinteiro 14/22 tarefas com 0 peça (meta pede
tábua, executor guarda metade em tora) = A-1, a recomendação nº 1; lenhador
66% procurando; mineiro 0 pedra; construtor 50% esperando; pastor e fundidor
ociosos por desenho.

**Why:** o autor quer o tempo dos aldeões como critério de toda verificação.
**How to apply:** depois de cada playtest, rodar time_ledger e activity_report
e comparar com estes números. HousePlans está congelado em 563 linhas
(oversized): lógica nova vai em classe à parte. Ver [[f1-f2-fluidez]],
[[mine-return-rastro]].
