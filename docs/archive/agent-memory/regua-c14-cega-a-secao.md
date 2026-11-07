---
name: regua-c14-cega-a-secao
description: "A régua scripts/assess superestima a nota — C14 só conta \"- [ ] 🔴\" (o TODO marca prioridade no título da seção) e C01 conta uma linha a mais"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-11T00:43:36.991Z
---

`scripts/assess/assess_project.py:187` conta dívida crítica só em linhas
`- [ ] 🔴`; o `TODO.md` põe o 🔴 no título `## 🔴 …`. Em 10-10 eram 41 itens abertos
sob seções 🔴 e o C14 deu 4/4 (real: 0). `java_metrics.py:87` usa
`len(text.split('\n'))`, contando o `\n` final como linha: arquivo de 500 linhas vira 501
(C01 3 em vez de 4). O coletor também quebra ao imprimir emoji no console cp1252 —
rodar com `python -I -X utf8` (o `-I` ignora `PYTHONIOENCODING`).

**Why:** a média publicada em 10-10 foi 3,36; corrigida, 3,14. Mesmo padrão de
[[assinatura-numerica-casa-no-meio]]: a ferramenta mede outra coisa.
**How to apply:** até a régua v2 (AUD-07), reportar a nota do coletor e a corrigida lado a lado.
