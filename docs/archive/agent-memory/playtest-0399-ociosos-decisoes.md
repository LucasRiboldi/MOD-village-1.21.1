---
name: playtest-0399-ociosos-decisoes
description: "Playtest 0.3.99 (Spark eVkPhNEtZT, 76 min, savana) — ócio por ofício, causas e as escolhas do autor de 28-10 para cada uma"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-28T05:03:34.687Z
---

**Playtest 0.3.99 (28-10, Spark eVkPhNEtZT):** 20 TPS, sem travão, mod a 1,2% do tique.

**Parado (ocioso + bloqueado) e causa:**
- **Lenhador 74%, pedreiro 48%, fundidor 44%:** estoque cheio (413 toras, 1.202 pedregulhos, 695 pedras).
- **Carpinteiro 42% esperando:** 69 tarefas param com "half of the colony's wood stays in logs".
- **Fazendeiro, 0 tarefas concluídas:** "could not reach the crop" 9×; a roça fica 2 acima, a 3 de distância.
- **Pastor:** 28 tarefas de lã sem ovelha lanuda, com 102 lãs no baú.
- **Mineiro:**
  - 30 min preso na boca antiga (1943, 68, -10328), com alvo 30 abaixo e sem escada;
  - depois foi minerar raso em x≈2155.
- **6 de 18 aldeões sem profissão**, todas as profissões na meta.

**Escolhas do autor:**
1. **Estoque cheio:**
   - **baús cheios → a obra prioritária é um ARMAZÉM** (texto do autor);
   - mais obras (abrir antes, ou 2 ao mesmo tempo com 2 construtores);
   - produtos de valor (carvão vegetal, tijolo, pedra lavrada, vidro colorido, terracota).
2. **Carpinteiro:** não abrir a tarefa de estoque quando a reserva de metade em tora não deixa fazer nem 1 peça.
3. **Fazendeiro:** colhe e planta a até 4,5 blocos, com linha livre.
4. **Pastor:** a tarefa de lã só abre com ovelha lanuda no alcance E menos de 64 lãs no baú.
5. **Mineiro:** recomeçar a escada; o cursor volta à 1ª célula da escada ainda fechada no mundo.
6. **Sem profissão:** subir a meta das profissões que a vila espera.

**Feito em d4969c68 (0.3.100, PR #37; 898/898, build com propriedade OK; JAR 19795344… instalado; não jogado):**
- Regras 133 a 137: `measureFullness`/`setSpare`, `ValueGoods`, `spareHand` (teto de 2 acima da cota), `woodHeldInLogs`, `WoolGate`, `seesWithinEyeReach`, `MineRestart`.
- Limite: o lote de valor é instantâneo e não conta como trabalho no `time_ledger`.

**0.3.102 (97eb1fb1):**
- tocha nova pela referência do autor;
- `VC_STATE` (`ColonyState` + `AnalyticsLog`, em `<mundo>/villagecolony/analytics/*.jsonl`);
- `/vc dados` e `scripts/state_report.py`;
- 900/900;
- PR #37 com mescla automática ligada, esperando o CI.

**How to apply:** implementar um por vez com teste e commit, na branch `fix/playtest-0397` (PR #37). Ligado a [[playtest-0397-e-decisoes-2026-10-27]], [[ociosidade-vila-madura]] e [[caminho-rente-ao-chao]].
