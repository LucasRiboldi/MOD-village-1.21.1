---
name: jars-nao-instalados-deslocamento
description: "ERRO corrigido 15-10 — SendUserFile entrega o JAR ao chat mas NÃO copia para a pasta de mods. 0.3.59 e 0.3.60 nunca foram instaladas; todos os playtests rodaram 0.3.58. SEMPRE confirmar 'villagecolony 0.3.X' no log antes de atribuir ganho a um conserto."
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-15T03:36:28.325Z
---

**Erro de método (15-10):** atribuí a playtests o número de versão errado por 3 rodadas. A pasta de
mods do TLauncher (`AppData/Roaming/.minecraft/mods`) só tinha até **0.3.58**; a 0.3.59 e a 0.3.60
**nunca foram copiadas** — `SendUserFile` entrega o JAR ao chat, não instala.

Deslocamento real (confirmado por `grep "villagecolony 0.3." no log de cada jogo):
- Spark `YOT1oqyhIv` = **0.3.57** (eu disse 0.3.58).
- Spark `1nwGlUtitv` = **0.3.58** (eu disse 0.3.59 e celebrei como "prova do conserto do DirectPath").
- Spark `aCYKSpgkXT` = **0.3.58** de novo (seria o teste do P1/0.3.60).

**Consequência:** o salto MINED 0,8→19/min que atribuí ao meu conserto do DirectPath (0.3.59) foi na
verdade **0.3.57→0.3.58** (a expansão ágil / outra coisa), NÃO o meu código. **Os consertos do mineiro
(0.3.59 DirectPath e 0.3.60 reroute) seguem provados só em teste/mutação, NÃO em jogo.**

**Por quê e como aplicar:**
- **Antes de atribuir qualquer ganho em jogo a um conserto, confirmar a versão:**
  `grep -iE "villagecolony 0\.3\.[0-9]+" <log>` — a linha de carga do mod. Nunca assumir que o JAR
  enviado é o jogado.
- **Instalar de fato:** `mv mods/village-colony-<antiga>.jar{,.disabled}` e
  `cp build/libs/village-colony-<nova>.jar mods/` e conferir o sha256 contra o build. Só então pedir
  playtest. (0.3.60 instalada e conferida em 15-10: F164319032...)
- Vale um script `scripts/install-mod.sh` que faça isso e valide o hash (pendência).

Relacionado: [[minecraft-test-install]], [[contagem-honesta-de-testes]],
[[playtest-0359-spark-1nwGlUtitv]], [[playtest-0358-spark-YOT1oqyhIv]].
