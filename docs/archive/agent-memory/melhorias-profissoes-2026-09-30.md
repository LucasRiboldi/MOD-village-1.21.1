---
name: melhorias-profissoes-2026-09-30
description: "Revisão das profissões de 09-30 concluída e enviada (ef84143, 88510e6, 14bde52); o que falta é playtest e Spark"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-30T18:59:44.714Z
---

Ciclo pedido pelo autor em 2026-09-30, **concluído e enviado** no branch
claude/sync-local-github-80cc8a: código `ef84143`, docs `88510e6`, JAR `14bde52`
(SHA-256 ADD930E8…AA9C9 em build/libs, downloads e mods, conferido por
release_manifest.py). 1.196 unitários e 528 GameTests verdes. **Nada visto em jogo.**

Relatório: docs/technical/Revisao-Profissoes-2026-09-30.md; ADR-028 (regra "não cria
recurso" retirada, lava nunca), ADR-029 (ofício Vanilla bloqueado, mixin em
setVillagerData), RULES 34–44.

**Armadilha que me pegou:** eu afirmei na varredura que VillageMeals só alimentava
quem tem ofício. Falso: o VillagerScanner registra TODO aldeão da vila em WORKERS,
com ou sem função. "Worker" nesse registro não quer dizer "empregado".

**Why:** a próxima sessão precisa saber que o ciclo fechou e o que ainda não foi
visto, para não reabrir nem declarar verificado.
**How to apply:** o próximo passo é o playtest (frases na §2 do relatório; a
BigHouseMOD já erguida no save tem 6 camas, só uma nova terá 7) e o Spark
(countIn por drop do mineiro, varredura de ofício Vanilla). Ver [[sessao-de-jogo-destrava-o-plano]].
