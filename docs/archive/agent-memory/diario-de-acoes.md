---
name: diario-de-acoes
description: ActionJournal (0.3.11) grava JSONL por ação/aldeão; action_report.py lê; decisões do autor de 08-10
metadata:
  type: project
---

Desde 0.3.11 (f6ac1a91, branch claude/obra-lote-e-trava, sem PR): `logs/villagecolony-actions-<sessão>.jsonl`
(cliente: %APPDATA%/.minecraft/logs; bateria: build/gametest/logs). `python scripts/action_report.py`
lê o mais novo. Use ANTES de regex no latest.log para "o que cada aldeão fez".

Decisões do autor em 08-10: peça sem material fica de lado e a obra segue (WAITING_MATERIAL);
nome da profissão dentro do painel (plaquinha Vanilla escondida por mixin de cliente).
Pendentes do autor: 5 perguntas do plano do nivelador (`docs/research/2026-10-08-nivelamento-do-solo-da-vila.md`);
erros E1–E7 em `docs/research/2026-10-08-erros-atuais-e-correcoes.md` (E1 mineiro tooHigh é o próximo).

**How to apply:** depois de playtest, rode action_report + time_ledger; conclusão de tarefa do
mineiro ~24% na bateria já era sinal do E1/E2.
