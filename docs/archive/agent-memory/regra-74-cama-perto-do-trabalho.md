---
name: regra-74-cama-perto-do-trabalho
description: "Regra 74 (autor, 22-10): cama mais perto do trabalho; BedPlan/BedChoice na 0.3.92, as quatro escolhas do autor e as armadilhas do GameTest"
metadata:
  type: project
---

Pedido do autor (22-10): cada aldeão troca para a cama mais perto de onde trabalha, sem conflitos nem erros.

**Escolhas do autor** (todas as recomendadas):
- **Trabalho:** onde trabalhou de fato, a mediana das últimas 32 ações com lugar do diário; com menos de 8, fica onde está.
- **Cama ocupada:** troca se a soma das caminhadas cai e o dono não anda mais; o sem ofício cede.
- **Frequência:** 1×/dia ao amanhecer, com ganho mínimo de 16 blocos.
- **Baú:** acompanha a cama.

**Feito na 0.3.92** (ac984d41, PR #33, não mesclada):
- `core.coordination.BedPlan` decide;
- `fabric.work.BedChoice` mede (via `ActionJournal.action`), roda no `StrandedEscape.tick` e aplica: solta os dois bilhetes de POI, faz `giveHome`, faz `STORAGES.remove`;
- a cama livre só vale se alcançada a pé (`ReachMap`);
- quem dorme não muda.
JAR 1F03E481… instalado, NÃO jogado.

**Why:** no deserto, os mineiros dormiam a 70 e 97 blocos da mina. O construtor e o mineiro dos dados reais NÃO trocam: a obra fica ao sul e a cama do mineiro ao norte, então o construtor andaria mais. Conferir no jogo antes de prometer a troca.

**How to apply:**
- O carpinteiro, o pedreiro e o fundidor registram CRAFTED sem posição, então nunca mudam; isso está pendente.
- **GameTest de cama e alcance:** cenário a y40. Perto do chão o heightmap pega restos e o `ReachMap.startOf` parte de cima deles.
- Um lote por cenário largo. Limpar só os próprios aldeões (`BedChoice.forget`), nunca `clearAll`, porque o vizinho do lote roda junto.

Relacionado: [[movimentacao-natural]], [[decisoes-autor-2026-10-22-deserto]], [[gametest-runcyclenow-adota-vizinho]].
