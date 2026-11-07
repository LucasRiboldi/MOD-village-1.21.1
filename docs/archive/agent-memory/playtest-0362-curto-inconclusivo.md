---
name: playtest-0362-curto-inconclusivo
description: "Playtest 0.3.62 (Spark NMmCkiNYUo, versão confirmada) foi curto (15,7 min, 3 janelas VC_TIME) — amostra pequena demais para fechar o aceite do P1/mina. Bancadas carregaram sem erro. Aceite da mina segue PENDENTE de playtest ≥30 min com caverna."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T05:37:27.583Z
---

Playtest **0.3.62** (15-10, Spark `NMmCkiNYUo`, versão confirmada `villagecolony 0.3.62`). **Curto:
15,7 min, 3 janelas VC_TIME** — amostra pequena.

**Bancadas (0.3.62): OK.** Nenhum erro de textura/registro do villagecolony no log (os ERRORs finais são
de outros mods: overflowingbars, ftblibrary, jei). Carregaram sem problema.

**Mina: INCONCLUSIVO.** REROUTED=0 (o mineiro não topou o caso de parede nesta vila), NO_STANDING=17
(baixo), went one level deeper=0, MINER Regra 51 ~55%, 62 MINED/mineiro. Com 15 min não dá para dizer
que o P1/A3 passou nem que falhou — o aceite (NO_STANDING caindo, Regra 51 <15%, numa vila COM caverna)
**segue pendente de um playtest ≥30 min**.

Outros sinais (amostra pequena, não acionáveis ainda): SHEPHERD 49% blocked (COLLECT_WOOL ×8 solto),
SMELTER 64% espera, MASON 81% idle (vila madura). São o C4/P3 e o ócio de vila madura — fases depois.

Relacionado: [[playtest-0360-p1-parede-caverna]], [[regra-51-tempo-ocioso]], [[lento-nao-e-travado]].
