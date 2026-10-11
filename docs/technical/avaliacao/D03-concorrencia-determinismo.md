# D3 — Concorrência e determinismo

> Auditoria técnica de 2026-10-10 · commit `b290ed10` · fila em [ACTION-QUEUE.md](ACTION-QUEUE.md)

**Skills:** `minecraft-testing`, que rodou a bateria 3 vezes e comparou as saídas;
`minecraft-performance-profiling`, no checklist de estado entre ticks.
**Nota da dimensão:** 7/10.

## Medidas

| Pergunta | Resultado | Evidência |
|---|---|---|
| Estado tocado fora da thread do servidor? | **Não.** Nenhum `CompletableFuture`, `Executor`, `new Thread` ou `parallelStream` em `src/main`. Os receptores de rede despacham com `server.execute` | `ProfessionPolicyNetworking.java:27,29` |
| `PersistentState` com escrita concorrente? | Não. `ColonySavedData.sync` → `markDirty` só em `BEFORE_SAVE` e `SERVER_STOPPING` | `ColonySavedData.java:329`; `ServerLifecycleHandler.java:50-51,220-237,280` |
| Bateria repetível? | **3 rodadas, 690/690 em todas**, os mesmos 23 avisos. A diferença entre os logs (3656, 3415 e 3462 linhas) é só a barra de progresso do runner e o sinal das coordenadas da arena | `build/audit/raw/gt{1,2,3}-allsig.txt` |
| Aleatoriedade com semente? | Sim no planejamento (`PlanOrdering.java:57`, `new Random(seed)`); `world.random` no resto | ver AUD-22 |

## Achados

| ID | Sev. | Achado | Evidência | Impacto | Recomendação | Teste que valida |
|---|---|---|---|---|---|---|
| AUD-10 | P2 | 181 campos estáticos mutáveis (4,78/kLOC, C05 = 2/4), além de configuração estática não-final | coletor `metricas.json`; `MineLease.java:80`, `TreeChoice.java:80`, `MinerDetours.java:39`, `FarmerWork.java:78`; limpeza central em `ServerMemory.resetAll()` (`ServerLifecycleHandler.java:79,267`) | Estado que sobrevive entre mundos no mesmo processo (singleplayer: sair e entrar noutro mundo) e entre GameTests; memória `gametest-interferencia-global` | Esvaziar o registro congelado da regra: a regra ArchUnit `aStaticMutableCollectionIsServerMemory` já exige o registro no `ServerMemory` e está congelada com as violações antigas; o trabalho é esvaziar o congelado (corrigido em 10-10 ao abrir o código) | Store do ArchUnit sem violações dessa regra |
| AUD-21 | P3 | 5 rodadas verdes não provam estabilidade: o histórico mediu ~1 falha em 8 rodadas | `METODOLOGIA.md` §7; memória `bateria-gametest-instavel` | Afirmar "estável" com 5 rodadas é otimista (com taxa de 1/8, P(5 verdes) ≈ 51%) | `check-tests-green.sh --runs 8` antes de declarar a intermitência curada | 8 rodadas sem falha |
| AUD-22 | P3 | `Random.create()` sem semente ao assentar a fundação | `fabric/integration/BigHouseFoundation.java:79` | Processadores da estrutura (integridade, troca de bloco) variam entre execuções | Usar `world.getRandom()` ou semente derivada da colônia | GameTest da fundação com dois mundos de mesma semente compara blocos |

## Não verificado

- Limpeza individual dos 181 campos; amostrados `SweepState`, `lastStock` e `CraftingLookup`.
- Comportamento com dois mundos abertos em sequência no mesmo cliente.
