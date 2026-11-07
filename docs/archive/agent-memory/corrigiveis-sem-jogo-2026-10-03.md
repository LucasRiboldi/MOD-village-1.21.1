---
name: corrigiveis-sem-jogo-2026-10-03
description: "12 frentes corrigíveis sem jogo feitas no branch claude/corrigiveis-sem-jogo (c41ea7b8), sem push; JAR 923D9769 instalado"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-03T03:51:55.289Z
---

Em 2026-10-02/03, a pedido do autor ("resolva os corrigíveis sem acesso ao jogo"), 13 commits no branch
`claude/corrigiveis-sem-jogo` (a partir da main 6022021e), **sem push**: docs históricos, verdict.py
com itens novos, B-4 (degrau de fornalha), PenEntryLog (§8-A), ColonyPieces + StrandedNotice (§9b),
LotSeeking + SweepCadence (§7-A), testes do pastor, oito arquivos >500 divididos, JaCoCo na bateria
de jogo, GameTest com FakePlayer. Verificado: 1295 unitários, 575/575 GameTests ×2, 91 Python.
JAR 923D9769…40C0 em downloads/ e .minecraft/mods. **Nada visto em jogo.**

Ficaram de fora com motivo: B-1 metade do baú (construtor tira de qualquer baú sem andar — decisão do
autor) e F13 restante (medir antes de cachear espaço de baú do ofício).

**Why:** o próximo passo útil é playtest + `python scripts/verdict.py`, que agora cobre esses itens.
**How to apply:** antes de novo trabalho, perguntar se faz push/PR deste branch. FakePlayer funciona em
GameTest (onPlayerConnected/removePlayer). Ver [[climb-out-nunca-desiste]] (refreeze pega todas as regras)
e [[bateria-gametest-instavel]] (SmelterGameTest caiu só em rodadas sem --rerun-tasks).
