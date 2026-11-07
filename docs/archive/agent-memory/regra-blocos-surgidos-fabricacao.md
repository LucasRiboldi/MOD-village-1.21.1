---
name: regra-blocos-surgidos-fabricacao
description: Regra do autor para análises — listar todo bloco que surgiu sozinho (sem origem física) e registrar para buscar fluxo de fabricação
metadata:
  node_type: memory
  type: feedback
  modified: 2026-11-05T02:04:00.895Z
---

Em toda análise futura, identificar todos os blocos/itens que nasceram automaticamente
(surge de obra, "drop ingredients appear on their own", supplied/WAITED_TOO_LONG, SHAPED_GROUND,
Regra 113/3 buscas) e registrar cada um para buscar alternativa de fluxo de fabricação
(autor, 2026-11-04).

**Why:** o mundo é a fonte da verdade (ADR-028/034); cada bloco surgido é cadeia de produção faltando.

**How to apply:** na triagem, contar por item as linhas de surgimento no log (ex.: `surged`,
`appear on their own`, `supplied=`), e registrar no TODO.md (seção de fabricação) com contagem e
a cadeia possível. Ver [[cadeia-de-producao-recursiva]], [[triagem-ler-notas-e-dados]].
