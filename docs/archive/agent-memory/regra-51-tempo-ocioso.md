---
name: regra-51-tempo-ocioso
description: Regra 51 (autor, 08-10): ocioso+bloqueado ≤15% por profissão; feito 0.3.19 (WalkLegs, bosque 6, BuilderPaving); alternativas pendentes de escolha
metadata:
  type: project
---

Regra 51: ocioso + bloqueado + encalhado ≤ 15% por profissão no expediente (NONE fora); espera > 40% é alerta. Está no `time_ledger.py` e no CLAUDE.md §0.4. Análise e lista de alternativas: `docs/research/2026-10-08-tempo-ocioso.md`.

0.3.19 (commits aabe59ec..0e1eae3c, enviados): WalkLegs (pernas de 24 na superfície), bosque de 6 com farinha de osso, BuilderPaving (construtor sem tarefa calça grama no meio do caminho; WorkTime conta como trabalho).

**Pendente de escolha do autor:** A1 (defeito: runOngoingWork não chama CraftingWork.run → fabricante espera até 30 s por tarefa, 51% de espera — o autor NÃO marcou na 1ª pergunta, foi reoferecido), A2, A3, A6, A9, A10, B1–B6.

**How to apply:** todo playtest roda time_ledger e aponta REGRA 51 por profissão; nova correção de fluxo só está provada quando a profissão sai do veredito. Ver [[spark-com-action-report]], [[feno-bosque-pastor-ajudante]].
