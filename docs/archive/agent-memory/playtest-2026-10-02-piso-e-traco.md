---
name: playtest-2026-10-02-piso-e-traco
description: "Playtest 02-10 (Spark LhqqBh973A) — piso da obra nunca assentado porque mayBreak protege a própria obra aberta; /vc log \"travado\" era linha antiga do traço; mineiro preso sob pedregulho"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-02T04:28:27.201Z
---

Log `.minecraft/logs/latest.log` 00:43–01:04, JAR 17ceb450. TPS 20, mod 1,7%.

- Casa 58405bf6 ficou com grama por piso: `skips … grass_block is in the way`
  90x. `BuriedPieces.mayReplaceGround` perguntava `BlockProtection.mayBreak`,
  que protege a obra ABERTA — a posição é da própria obra, sempre "não". O
  GameTest passava porque não registrava a obra. Corrigido com
  `mayBuildOver` + teste com a obra registrada e controle `isColonyBuilt`.
- `/vc log` [TRAVADO] Fazendeiro com o fazendeiro colhendo: o ActivityTrace
  (salvo no save, sem horário) só recebia desistência; a última era de 01-10
  09:22. `WorkerStrikes.worked` grava RECOVERED na tarefa concluída.
- Mineiro 199ad062 boxed in 15 min sob cobblestone (aterro da colônia);
  escape agora cava pedregulho > 4 abaixo da superfície.
- Autor pediu: golem no curral (porteira dupla; simples ele pula — 1,4 de
  largura), base construída ("só construir a base"), escada de madeira na porta
  2 acima, roça sem camada da rua (um acima do chão).

**Why:** proteção de "obra aberta" e "não quebrar" são a mesma porta; quem
constrói dentro da obra não pode perguntar por ela.
JAR 7511f2b4 em mods (commits b19a350b, 9d14c152, enviados). 554/554 GameTests em 1 rodada só.

**How to apply:** teste de regra de proteção sempre com a obra registrada e
um assert de controle de que a proteção cobre a posição. Ver
[[teste-vazio-por-condicao-impossivel]], [[playtest-2026-10-01-noite]].
