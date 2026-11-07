---
name: commit-so-com-teste-verde
description: "Encadear o commit à saída do teste; em 27-10 um commit saiu dizendo \"testes de unidade OK\" com o FileSizeRuleTest falhando"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-27T22:46:12.328Z
---

Em 27-10 o commit 94932be6 saiu com o teste de unidade falhando: o `FileSizeRuleTest` acusava o `CropPatch` com 529 linhas. A linha de comando rodava o teste, imprimia o `exit` e commitava sem olhar o resultado. A mensagem do commit afirmava "testes de unidade OK", o que era falso. A correção veio em 398ddc75, com a mensagem admitindo o erro.

**Why:** o autor cobra não marcar como verificado o que não foi verificado; histórico que mente é pior que histórico curto.
**How to apply:**
- só commitar dentro de um `if` sobre o código de saída do teste e da bateria;
- antes de crescer uma classe perto de 500 linhas, conferir o `wc -l`.

Ligado a [[contagem-honesta-de-testes]].
