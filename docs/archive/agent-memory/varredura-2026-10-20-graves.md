---
name: varredura-2026-10-20-graves
description: "Varredura 20-10 achou 3 defeitos graves no código não jogado das 0.3.64/0.3.66; CORRIGIDOS na 0.3.67 (branch fix/varredura-g1-g3, commit 0d7839f5, push feito, sem merge). Lição: testar o chamador em tiques seguidos, não só a regra pura."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-20T15:04:21.933Z
---

Varredura de 20-10: `docs/audit/2026-10-20-varredura.md` (commitado na 0.3.67).

**Três graves, corrigidos na 0.3.67** (branch `fix/varredura-g1-g3`, commit `0d7839f5`, push feito, **sem
merge**; JAR `A0594C3F…` instalado e conferido por hash; **não jogado**), com as opções do autor:
- **G1** `CraftingWork.atBench` zerava o prazo ao desistir → artesão voltava a caminhar a cada 200 tiques.
  Agora estourado o `APPROACH_BUDGET` fabrica no lugar até o fim da tarefa.
- **G2** bancada procurada todo tique (POI raio 64 + ~9.800 blocos para o carpinteiro). Agora uma vez por
  tarefa: `CraftStation.Approach` no `Job` (o `Job` nasce por tarefa).
- **G3** não-mineiro desistia mas seguia em `StrandedWorkers`. Agora `ClimbPolicy.giveUp` →
  `BedTeleport.toBed` (já libera dos encalhados); sem cama, `StrandedWorkers.release` + volta à fila.
  **Sem teste próprio** (depende de mundo).

**Lição:** os testes passavam porque cobriam só a função pura; o defeito estava no chamador. Para regra de
estado ao longo do tempo, testar o objeto em tiques seguidos (o `BenchApproachTest` novo faz isso, e a
mutação do G1 o derruba). Ver [[teste-vazio-por-condicao-impossivel]].

**Restante da varredura (ordem):** jogar 0.3.67 na vila montanhosa → 6 faces do pedreiro e do fazendeiro +
modelo/colisão do pedreiro + texturas órfãs → fundidor (C4) → terraplanagem → mina na parede → índice de
ruas reservando 35% das recusas de lote.

Relacionado: [[roteiro-ociosidade-pendente]], [[plano-terraplanagem-vila]], [[jars-nao-instalados-deslocamento]].
