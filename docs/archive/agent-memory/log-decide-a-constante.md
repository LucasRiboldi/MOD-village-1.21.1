---
name: log-decide-a-constante
description: "Quando precisar de um limite numérico novo, tire-o da distribuição do log do playtest em vez de arbitrar"
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-15T23:08:47.262Z
---

Limite novo sai do log, não de chute. Em 2026-09-15 era preciso separar
"toco de floresta" de "construção de tronco" sem que o Vanilla marcasse
tronco colocado. A distribuição das 114 recusas `Not a tree` decidiu
sozinha: 93 grupos com 1–7 troncos, **vale limpo entre 7 e 10**, e 21
grupos de 10 a 57. `BARE_TRUNK_LIMIT = 8` caiu no vale.

O comando que gera a distribuição:

```bash
grep -o "Not a tree at [0-9, -]*— [0-9]* logs" latest.log | sed 's/.*— //;s/ logs//' | sort -n | uniq -c
```

**Why:** o autor cobra número justificado, e o javadoc do projeto registra
a origem de cada constante. Em 2026-08-19 o padrão inverso já apareceu:
quando o número da recusa é exatamente o limite (`24 logs without a living
canopy`, quatro vezes), **o limite é a causa** — ver
[[sessao-2026-09-06-07-lenhador-e-barrier]].

**How to apply:** antes de escrever a constante, extraia a distribuição do
`latest.log` e procure o vale. Cite o número e a contagem no javadoc e no
commit. Compare também com a distribuição do caso **bom** (ali, as árvores
realmente colhidas tinham 4, 5, 6 e 27 troncos) para não cortar o legítimo.
