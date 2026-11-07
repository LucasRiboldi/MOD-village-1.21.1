---
name: playtest-0364-ociosidade-altura
description: "Playtest 0.3.64 (Spark jDqDQaNI9F, 60min, versão confirmada) — ociosidade geral alta (MASON 93%, LUMBERJACK 71%) tem raiz comum no ELEMENTO DE ALTURA: a vila montanhosa (y59-106) não acha lote plano (Regra 19 exige cada coluna a ±1 do roadY), então nenhuma obra abre e as construtoras param."
metadata:
  node_type: memory
  type: project
  modified: 2026-10-15T14:51:46.289Z
---

Playtest **0.3.64** (15-10, Spark `jDqDQaNI9F`, **60 min, 12 janelas**, versão confirmada). Vila
**montanhosa**: aldeões de **y59 a y106** (47 blocos de amplitude).

**Ociosidade geral alta (Regra 51):** MASON 93%, LUMBERJACK 71%, SHEPHERD 63%, MINER 52%, BUILDER 36%,
FARMER 22%. Só CARPENTER ok.

**RAIZ COMUM = o elemento de altura.** A ociosidade não é de cada profissão — é a vila montanhosa que
**não gera obra**:
- `LotLevel.flatGroundAt` (linha 179-180): a **Regra 19** exige cada coluna do lote a **±1 do nível da
  rua** (`roadY`) — "no nível da rua, não só plano entre si" (evita varanda sem escada). Numa montanha
  de 47 blocos de amplitude, quase nenhuma pegada de 13×13 tem todas as colunas a ±1 → **"no empty plot
  within 32 blocks" ×20**, "found no lot for a mason in a whole sweep".
- Sem obra → MASON 93% / BUILDER 36% / carpenter idle (dependem de obra). 245× "no task open"; 93 "no
  smelter work", 56 "no building work".
- As de navegação travam no desnível: 43× `outcome=ABANDONED reason=WORK_STALLED` (MINER/FARMER/SHEPHERD
  — tarefa abandonada por travamento, NÃO colônia abandonando aldeão); 41× "no path home"; 17× "could
  not reach"; 35 stranded. MINER TARGET_REJECTED 2.7/min (parede de montanha, A3 ainda parcial).

**São dois eixos do mesmo elemento de altura:** (a) **gerar obra em montanha** (afrouxar a Regra 19 /
aterrar / aceitar lote em patamar com escada) — destrava mason/builder/carpinteiro; (b) **navegação em
desnível** (miner/shepherd/farmer alcançarem alvos) — 0.3.64 já proibiu não-mineiro de remodelar, falta
jogar. O FARMER soma "no empty plot" (roça) ao resto.

Direção a decidir com o autor. O eixo (a) é o de maior alcance na ociosidade (as construtoras são as
mais ociosas e todas dependem de obra).

**SPIKE (os números exatos do LotRefusals, que o próprio código diz que a decisão exige):** na última
medição, **1.272.326 candidatos de lote recusados**, por motivo:
- **443.181 (35%) reserved road area** — o maior; o índice de ruas reserva faixa demais.
- **393.140 (31%) no ground in the vertical window** — a montanha sai do range (`WINDOW_UP=MAX_SLOPE=2`,
  `WINDOW_DOWN=8`): o chão está fora da janela vertical da busca.
- **382.212 (30%) not at street level** — a Regra 19 (`ROAD_LEVEL_TOLERANCE=0`, cada coluna a ±1 do
  roadY; `LEVEL_BASE_PERCENT=50`).
- 22.855 not natural soil; 15.735 Rule 3; 33 bed.

**Conclusão do spike:** os três gargalos são **reserved road (35%) + vertical window (31%) + street
level (30%)** = 96%. Dois deles (vertical window + street level, **61%**) são diretamente o relevo. As
constantes candidatas a afrouxar, em ordem de impacto: `WINDOW_UP`/`MAX_SLOPE=2` (a janela vertical é
estreita para montanha) e `ROAD_LEVEL_TOLERANCE=0`/`LEVEL_BASE_PERCENT=50` (Regra 19). O `MAX_SLOPE=2`
também limita quanto a obra aplaina — subi-lo deixa a vila aceitar e nivelar terreno mais inclinado. O
`reserved road` (35%) é outro sistema (índice de ruas reserva faixa), fora do relevo.
**Decisão do autor pendente:** qual(is) constante(s) afrouxar e quanto, sem reabrir a "varanda sem
escada" que a Regra 19 evita. O javadoc do WINDOW_UP já previa: "se ficar apertado, é uma constante".

Relacionado: [[playtest-0363-vila-montanhosa]], [[obra-sem-construtor-trava-de-fundacao]],
[[indice-de-ruas-vira-rua-candidata]], [[ociosidade-vila-madura]], [[log-decide-a-constante]].
