# Perfil de desempenho com spark

**Instalado em 2026-09-24:** `spark-1.10.109-fabric.jar` (Modrinth, Fabric
1.21.1), em `%APPDATA%\.minecraft\mods`. SHA-512 conferido com o publicado
no Modrinth: `367f574f…c94250`. Não é dependência do mod; é só ferramenta
de playtest, e não entra no jar da Village Colony.

## Por que agora

O analisador de log (`scripts/analyze_village_log.py`) contou, no playtest
de 24-09, **196 linhas `Colony cycle took N ms — longer than a server tick`**
(`cycle_over_tick`). O log diz *que* o ciclo passou do tique; não diz
*onde* o tempo foi. É essa a pergunta que o spark responde: amostragem de
pilha em gráfico de chama, num servidor ao vivo e sem acrescentar lag.

## Procedimento de playtest

1. Entrar no mundo com a vila carregada e esperar a colônia trabalhar
   (alguns minutos, até o log começar a mostrar ciclo e obra).
2. Começar a amostragem só nos tiques lentos, que é onde está o custo:

   ```text
   /spark profiler start --only-ticks-over 50
   ```

3. Jogar normalmente por **5 a 10 minutos**, perto da vila. Menos que 3
   minutos dá amostra pequena demais.
4. Parar e guardar o link:

   ```text
   /spark profiler stop
   ```

   O spark devolve um link `spark.lucko.me/…`. Colar esse link, e o
   `latest.log` da mesma sessão, na conversa seguinte.
5. Opcional, para a foto do momento:

   ```text
   /spark health
   /spark tickmonitor --threshold-tick 50
   ```

   `health` dá TPS, MSPT e memória; `tickmonitor` escreve no chat cada
   tique que passar de 50 ms.

## O que procurar no gráfico

Os nós `com.villagecolony.*` e o peso de cada um dentro do tique:

| Suspeito | Por quê |
|---|---|
| `VillageDetectionHandler.onServerTick` / `ColonyCycle` | é o ciclo de 600 tiques que o log acusa |
| `BuildSiteScanner` / `RingSweep` | a varredura de lote, raio 64, com orçamento por passagem |
| `ChestInventoryReader` / `ColonyChests` | leitura de baú a cada ciclo, com N baús |
| `TreeScanner` / `LumberjackWork` | busca de árvore, até 64 blocos |
| `MineDigging` / `MinerReach` | planejamento da mina |

Um nó do mod que ocupe mais de alguns por cento do tique durante toda a
amostra é candidato a orçamento, cache ou espaçamento; um pico isolado não é.

## Depois

Cada perfil vira uma entrada no `Development-Log.md`, com o link, a
sessão e os três nós mais pesados. Com dois perfis dá para comparar antes e
depois de uma mudança.
