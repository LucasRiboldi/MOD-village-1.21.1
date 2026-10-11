# D4 — Performance e orçamento de tique

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skill:** `minecraft-performance-profiling`, que mediu antes de opinar: `cost_ledger.py`,
`time_ledger.py` e `action_report.py` sobre o log de jogo, e só depois leu o código.
**Nota da dimensão:** 6/10.

## Medida em jogo: playtest de 10-10, 10:45–11:51, villagecolony 0.3.22

`scripts/audit/check-tick-budget.sh --log build/audit/playtest-2026-10-10-manha-0.3.22.log`

```text
2-3 colônias — 13 amostras (ms: média / p95 / máximo)
  planner        20.91    53.00    55.62
  detect          6.72     9.15    13.15
  assign          5.06     4.84    32.58
  total          36.54    67.42    70.80
```

- O ciclo roda a cada 600 tiques (`VillageDetector.java:57`). O custo **médio por
  tique** é ~0,06 ms, desprezível; o problema é o **pico**: um tique de 67–71 ms a
  cada 30 s, acima dos 50 ms do tique.
- Régua C13: **39,4 ciclos acima de um tique por hora** (43 em 1,09 h), nota 1/4,
  igual à de 09-24.
- O prazo de 15 ms (`PlanningBudget.java:30`, `SweepDeadline`) não segura o
  planejador: p95 de 53 ms. A memória `fluidez-passo-do-ciclo` (F1) já dizia
  isso; o número continua de pé.

**Na 0.3.23 (10-10, 21:24–22:06, 8 amostras) o pico não se repetiu:** total p95 27,7 ms,
planejador p95 14,1 ms. Com 13 e 8 amostras, as duas sessões não decidem entre "a 0.3.23
melhorou" e "a colônia da manhã estava num momento mais caro". O AUD-04 passa a exigir
nova medida (≥ 20 amostras) antes de qualquer otimização.

**Comparação com outros mods:** o pedido cita números do Sapience. Eles não foram
reproduzidos nesta máquina e não servem de régua (anti-padrão do skill). A régua
é o tique de 50 ms.

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-04 | P2 | O ciclo da colônia passou do tique na 0.3.22 com só 2–3 colônias; na 0.3.23 não | 0.3.22: p95 67,4 ms, máx. 70,8 ms, planejador p95 53 ms (13 amostras). 0.3.23: p95 27,7 ms (8 amostras) | Engasgo perceptível a cada 30 s; cresce com o número de colônias (custo quadrático aceito na Emenda 7) | Medir de novo com ≥ 20 amostras. Se repetir: fatiar o planejador entre tiques (o prazo vale para a passagem inteira, não só para a varredura) | `check-tick-budget.sh`: p95 total ≤ 50 ms em ≥ 20 amostras |
| AUD-16 | P3 | Receita de fundição consultada varrendo todas as receitas a cada chamada | `fabric/integration/CraftingLookup.java:102` (`listAllOfType(SMELTING)`), também `:190`, `:240`, `:330`; cache só para `STORAGE_FORMS` (`:45`) | Custo não medido; aparece no Spark só se o fundidor chamar muito, e o AUD-01 mostra 883 pegas/h | Cache item → entradas, limpo em `ServerMemory` | Spark sem `CraftingLookup` no topo |

## Leitura de código (risco, não medida)

| Ponto | Situação |
|---|---|
| Caminho Vanilla `findPathTo` | só `NightHome.java:86`, a cada 600 tiques por aldeão (`:30`) |
| Desvio próprio | `DetourPlanner`: teto de 2500 nós (`:38`) e raio 16; propriedades P1–P4 provadas (ver D7) |
| Cache de inalcançável | `UnreachableSpots` (64), `TreeMarks`, `MineMarks`, `RoadRefusals`, todos com teto |
| Varreduras sem guarda de chunk | `FencedIn.java:431`: 13×3×13 em volta do próprio aldeão, cujo chunk está carregado. Risco baixo, sem item |
| Fila de chunks | `VillageDetectionHandler.java:112` (teto 256, um por tique) |
| Gravação em disco no tique | `ActionJournal` a cada 200 tiques (`:50,79-81`), na thread do servidor |

## Não verificado

- **Perfil do Spark nesta sessão:** exige jogador conectado. O último Spark citado
  pelo `STATE.md` (`9CJSHC5a77`, 0.3.21) deu TPS 20.
- Custo com mais de 3 colônias: o log não tinha a faixa.
