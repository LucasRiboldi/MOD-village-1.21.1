# Playtest 2026-10-08, 12:11–12:36 (JAR 0.3.14) — Spark `bal0IJeOGc`

Sem crash. Spark: TPS 20 (mín. 16,3), 12,9 ms/tique em média, pico de 121 ms; **mod 2,1%** da
thread do servidor (`VillageDetectionHandler.onEndTick` 1,59%; `SmeltInput` 0,09%; `ReachMap` abaixo
da medida).

## `time_ledger.py`

| profissão | work | walk | wait | blocked | idle |
|---|---|---|---|---|---|
| BUILDER | 45% | 8% | 3% | 20% | 24% |
| CARPENTER | 14% | 30% | 50% | 1% | 5% |
| FARMER | 1% | 49% | 38% | 0% | 12% |
| LUMBERJACK | 6% | 58% | 1% | 26% | 9% |
| MASON | 18% | 21% | 56% | 1% | 3% |
| MINER | 0% | 52% | 34% | 1% | 13% |
| SHEPHERD | 1% | 26% | 16% | 1% | 57% |
| SMELTER | 0% | 0% | 0% | 0% | 100% |

## `action_report.py` (22,3 min)

| profissão | pegas | feitas | soltas | conclusão | ações/min |
|---|---|---|---|---|---|
| BUILDER | 12 | 12 | 0 | 100% | PLACED 20,1, SET_ASIDE 5,6 |
| CARPENTER | 110 | 107 | 0 | 97% | CRAFTED 12,0 |
| MASON | 72 | 70 | 0 | 97% | CRAFTED 12,8 |
| LUMBERJACK | 4 | 0 | 3 | 0% | FELLED_TREE 8,4 (32 árvores) |
| MINER | 6 | 0 | 4 | 0% | — (0 pedras) |
| SHEPHERD | 18 | 0 | 14 | 0% | SHEARED 1,7 |
| FARMER | 4 | 0 | 4 | 0% | HARVESTED 1,7 |

Motivos: BUILDER 125× SET_ASIDE "no material" (hay_block 93, deepslate_tiles 17, tuff_bricks 9,
chain 6); MINER 4× "walked 2400 ticks without arriving"; LUMBERJACK 1× STRANDED.

## O que funcionou
- **Fundidor sem roda:** 0 pegas sem minério (antes 191 pegas e 191 soltas).
- **Boca de superfície deduzida:** "the surface entrance is remembered at -775, 68, -922".
- **M2:** `VC_REACH … places=36 reached=36`; `[ALCANCE] 36 de 36` no `/vc log`.
- **`/vc log`:** sem "Obra: já há uma aberta".

## Corrigido (0.3.15)
1. **Mineiro indo e voltando entre a boca e o primeiro lance** (0 pedras em 22 min): a entrada de
   cada nível fica dois acima do chão onde o aldeão para; a régua "a até 3 blocos em linha reta" não
   dava a chegada (3,6 blocos), e quem estava logo abaixo da boca era mandado de volta a ela.
   Agora chegar é estar a até 3 em cada eixo, vale a entrada mais funda já alcançada, e o destino é o
   lugar de pé da entrada.
2. **`/vc log` com o fundidor duas vezes** ("smelter" e "surface gathering", mesmo motivo).

## Visto e não corrigido
- **Obra esperando feno (hay_block ×93):** cada fardo pede 9 trigos e a roça é pequena — o gargalo é
  o fazendeiro (0% de conclusão, colheita 1,7/min).
- **Lenhador 26% blocked:** as faixas funcionam, mas não há árvore no miolo da vila; todas as 32
  cortadas ficam a 48–107 blocos do centro. Viveiro perto da vila é o caminho.
- **Pastor 57% ocioso:** rebanho cheio, lã crescendo — por desenho (E7).
