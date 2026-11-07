---
name: crash-retomada-e-e1-e7
description: "0.3.12 (08-10) — crash \"open project\" pela SweepCadence, E1–E7 corrigidos com mutação; o que falta ver em jogo"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-08T12:45:47.005Z
---

0.3.12 na branch `claude/obra-lote-e-trava` (commits 2132c67d..02f84a62, enviados; sem PR para a main).

- Crash de 07-10 e 08-10 = o mesmo: `SweepCadence` (passagem extra 1/s com jogador) abria lote com obra **pendente** do save; a retomada registrava por cima. Assinatura no log: "Loaded … 1 paused sweeps and 1 projects to resume" + "planned …" antes de "resumed".
- E1 `MiningTarget` (geometria + busca local, nunca findPathTo — ver TreeMarks), E2 `MineBranch`, E3 cursor na roça conhecida, E5 `ProductionChain` ([CADEIA] no /vc log), E6 `CraftingLookup.isStorageForm`, E7 `ShepherdFlock.breedForWool`, M1 motivos no action_report.
- Lição repetida: a primeira versão do teste do crash **sobreviveu à mutação** (obra salva sem base era largada antes) — [[teste-vazio-por-condicao-impossivel]].
- Mutação em lote num worktree com `Boolean.getBoolean("vc.never") &&` (ErrorProne recusa `false &&` e parâmetro sem uso) roda em paralelo à bateria limpa.

**Why:** decisões do autor de 08-10 por item, no doc `docs/research/2026-10-08-erros-atuais-e-correcoes.md`.
**How to apply:** próximo passo é playtest da 0.3.12 (checklist no TODO.md); depois decidir E1 alternativo (pôr chão onde falta) pelo número de TARGET_REJECTED, M2 antes do nivelador, e ADR do ciclo comum de tarefa (decisão do autor). Ver [[diario-de-acoes]], [[spark-com-action-report]].
