---
name: minecraft-performance-profiling
description: Medir e auditar o custo por tick de um mod Fabric — Spark, linhas VC_COST, orçamento de tick, pathfinding de aldeões, varreduras de bloco e alocação por tick. Use antes de qualquer otimização, ao ler um perfil do Spark, ou ao auditar a dimensão de performance. Não use para defeitos de lógica sem sintoma de lag.
---

# minecraft-performance-profiling

## Propósito

Responder *quanto custa* antes de *como otimizar*. O tick do servidor tem
50 ms; o mod disputa esse orçamento com o Vanilla e com os outros mods.

## Quando usar

- Dimensão **D4 (performance)** da auditoria.
- Depois de um playtest com Spark (ADR-035 §5: nenhuma otimização começa sem
  o número do `cost_ledger.py`).
- Ao revisar código novo que varre blocos, procura caminho ou aloca por tick.

## Mapeamento para a auditoria

| Dimensão | O que este skill entrega |
|---|---|
| D4 Performance | custo por fase (VC_COST), hotspots do Spark, varreduras sem limite |
| D3 Determinismo | estado compartilhado entre ticks, cadência das varreduras |

## Fontes de medida, em ordem de confiança

1. **Linhas `VC_COST`** no log do jogo → `python scripts/cost_ledger.py`
   (média, p95, máximo por fase e por faixa de colônias).
2. **Spark** (`/spark profiler --timeout 120`) — ver a memória
   `spark-log-decodificar`: `times` é inclusivo; sempre mostrar junto o
   `action_report.py` (pedido do autor, 2026-10-08).
3. **Leitura de código** — só aponta *risco*; nunca vira número.

## Checklist de leitura de código

- `BlockPos.iterate`/`stream`/laços 3D: há teto de volume? há guarda
  `isChunkLoaded`/`isPosLoaded` antes de `getBlockState` (que carrega chunk)?
- Busca de caminho: `findPathTo`/`startMovingTo` por tick ou com cadência?
  Há cache de alvo inalcançável (memória `marca-escrita-e-nao-lida`)?
- `RecipeManager`/`getFirstMatch` por tick: cachear por item.
- Alocação: `new BlockPos`, `Vec3d`, `ArrayList` dentro de laço de tick;
  `stream()` em caminho quente.
- Varredura com cadência fixa sobre todas as colônias: o custo cresce com o
  número de vilas — ver `SweepCadence`.

## Comandos

```bash
python scripts/cost_ledger.py <latest.log>
python scripts/time_ledger.py <latest.log>
python scripts/action_report.py <pasta-do-diario>
grep -rn "BlockPos.iterate\|getBlockState" src/main/java | wc -l
```

## Anti-padrões

- Comparar com número de outro mod (ex.: "Sapience faz X ms") sem medir o
  Village Colony na mesma máquina e com o mesmo número de aldeões.
- Otimizar o que o Spark não mostrou.
- Ler `times` do Spark como exclusivo.
- Declarar "sem lag" com TPS 20 numa vila pequena: meça a faixa de colônias.
