---
name: obra-sem-construtor-trava-de-fundacao
description: "Playtest 07-10 noite (0.3.7) — obra \"AVAILABLE with nobody\" 37 min; trava BUILD_SITE recusa muda porque o lote ficou um acima da rua em chão plano"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-08T02:20:23.470Z
---

Playtest 2026-10-07 22:33–23:13, JAR 0.3.7 (branch claude/opcao-b-e-plano-de-testes, não a main),
save "Novo mundo", colônia 33a6b9c4. Spark bpHFsttvlp: TPS 20, MSPT mediano ~12 ms, picos 100–190 ms.

Sintoma: `builders: 0 working, BUILDING ... — AVAILABLE with nobody` a sessão inteira; time_ledger
BUILDER 100% idle. Roça plains_small_farm_1 (-743,70,-855) e depois plains_small_house_2 (-723,70,-862).

Causa provada: os dois lotes têm origem y70 sobre grama plana em y69; as duas plantas têm camada da
rua 0 (porta da casa em y1, encaixe da roça em y0, lido do .nbt do jar). A cota da rua = y70 = só ar →
`FoundationPreparation.prepareIfQualified` dá apoio 0% < 50% → `ColonyCycleRunner.canReserveTask`
(BUILD_SITE) devolve false → ninguém reserva, sem uma linha de log.
Descartados: encalhe, descanso, coletor tirando terra do lote, lag.

Não provado: por que o planejador pôs a roça em y70 às 22:32 e ela foi reservada na hora (64 blocos)
e, retomada do save, virou 91 blocos e nunca mais. A casa veio de um Building abandonado de 06-10.
Pista: a2b1d1c6 aceita lote "um acima da rua" se metade apoia — em chão plano isso nunca apoia.

**How to apply:** "AVAILABLE with nobody" + BUILDER idle 100% = olhe a trava BUILD_SITE e o terreno
na cota origem+streetLayer. Leitor de blocos do save: scratchpad blocks.py (regiões .mca + nbtlib).
Relacionado: [[roca-sem-lote-trava-a-vila]], [[grupo-de-pedra-e-decisao]], [[reparo-adota-casa-fantasma]].

**Atualização 2026-10-08:** causa da roça achada — `FarmPlans.withoutTheCrops` (sem trigo, sem camada
da rua) só no planejamento; a releitura (`BlueprintPlacement.blueprintOf`) vinha crua. Corrigido em
`claude/obra-lote-e-trava` (9f726adf), junto com a opção B, versão 0.3.8 instalada em mods; push feito,
**sem PR para a main ainda**. 658/658 GameTests, 4 mutações provadas. Falta: playtest no save "Novo mundo".
