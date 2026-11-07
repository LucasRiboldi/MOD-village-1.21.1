---
name: merge-na-main-autorizado
description: Autorização permanente do autor para fazer merge na main quando for a próxima ação recomendada
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-04T22:03:18.315Z
---

Fazer o merge do PR na `main` sempre que essa for a próxima ação recomendada, sem perguntar
(autor, 2026-11-04: "Faça o Merge na main sempre que isso for a próxima ação recomendada").

**Why:** o autor confirmou merges seguidos (#52, #53) e não quer parar o fluxo para cada um.

**How to apply:** só com CI verde e sem conflito, merge commit (`gh pr merge --merge`, como os PRs
anteriores), sem `--force` e sem auto-merge. Depois, atualizar a `main` local e apagar a branch local
integrada. Push de outras coisas, release e tag continuam pedindo ordem. Ver [[commit-push-atalho]].
