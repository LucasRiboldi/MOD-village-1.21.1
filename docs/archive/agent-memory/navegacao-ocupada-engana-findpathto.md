---
name: navegacao-ocupada-engana-findpathto
description: findPathTo devolve o currentPath em curso quando o alvo bate com o currentTarget da busca anterior; aldeão de GameTest passeia no tique 3 e vira flake
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-22T00:25:33.125Z
---

`EntityNavigation.findPathToAny` tem atalho: se `currentPath` não terminou e o alvo pedido é o
`currentTarget`, devolve o `currentPath` sem buscar. Toda chamada de `findPathTo` grava
`currentTarget` mesmo sem andar. E pôr bloco perto do caminho em curso dispara `recalculatePath`
para esse `currentTarget` (com cooldown de 20 tiques).

**Why:** em 2026-10-21 o `MinerDirectPathGameTest` falhava ~1/3 (as duas mensagens: "navegação não
chegou" e "parede ainda aceita"). O A* era determinístico: o aldeão desempregado pousa no tique 2 e
começa a passear no tique 3 (6 de 17 cópias). O probe gravava `currentTarget = alvo`, e o `reaches`
recebia o caminho do passeio; a parede recalculava o caminho para o alvo no 1º bloco posto. O
conserto foi `villager.getNavigation().stop()` no tique da prova; `setAiDisabled(true)` NÃO serve
(sem IA não há `travel`, ele nunca fica `onGround` e o `findPathTo` dá nulo).

**How to apply:** GameTest que pergunta caminho a aldeão com cérebro ligado: pare a navegação no
mesmo callback antes de perguntar. Para estressar um GameTest numa rodada só, duplique o método com
`batchId` distintos (~25 cópias) — `maxAttempts`/`requiredSuccesses` não repetiu. O mesmo atalho
afeta `MinerDirectPath.reaches` em produção (mineiro andando), ver [[mineiro-directpath-falso-caminho]].
Relacionado: [[gametest-navegacao-espera-pousar]], [[bateria-gametest-instavel]].
