---
name: playtest-2026-10-08-funil
description: Playtest 08-10 (0.3.8) — funil travou o celeiro; meta de ferro só via lampião; abertos do mineiro/fazendeiro/pastor
metadata:
  type: project
---

Playtest 2026-10-08 00:33–00:44, 0.3.8. Relatório: `docs/reports/Playtest-2026-10-08.md`.
Funil: lingote ia ao fundidor, mas `WorkMaterials.iron` só contava LANTERN e `RAW_IRON` não estava em
`WorkDemand.constructionMaterials` → mineiro só "wants cobblestone". Corrigido em 2d9b3451 (0.3.9,
branch claude/obra-lote-e-trava, push feito, sem PR). Overlay: ícones lisos + painel sobre a placa (d09f3ac3).

**Abertos:** mineiro preso a pedra de pé inalcançável; 2º mineiro sem alvo com relatório "waiting for
a branch"; varredura do fazendeiro `budget ran out`; pastor sem ovelha com lã; construtor parado por
peça RESOLVING (ADR-036 item 6 — decisão do autor). Não confirmado: o que o autor quis com "nome da
profissão no overlay" — o código já desenha o nome quando o texto está ligado (padrão).

**How to apply:** obra parada por peça com cadeia (ferro, argila) → siga a meta até a folha
(`cadeia-de-producao-recursiva`) e confira se o cru entra em constructionMaterials (prioridade).
