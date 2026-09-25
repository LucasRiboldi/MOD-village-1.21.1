# Changelog

Mudanças que chegam ao JAR ou à forma de verificar o mod. O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

A versão continua **0.3.0 alpha**: o mesmo número é republicado a cada
entrega, e o que separa uma publicação da outra é o commit e o SHA-256 do JAR
(ver `STATE.md` e `scripts/release_manifest.py`). O histórico anterior a
2026-09-24 está em [`docs/technical/Development-Log.md`](docs/technical/Development-Log.md)
e [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md).

## [0.3.0] — publicação de 2026-09-25

### Adicionado

- Cache de um segundo na varredura dos baús livres da vila
  (`VillageChests`). O perfil do spark de 2026-09-24 apontou
  `ColonyChests.nearestFirst` como o maior custo do mod nos ticks lentos. Baú
  posto ou quebrado invalida o cache na hora; nome, BigHouse e chunk
  carregado são conferidos a cada leitura.
- `ServerMemory`: cada classe com estado de servidor se inscreve sozinha, e o
  ciclo de vida esquece tudo por `resetAll()`. Achou uma limpeza que nunca era
  chamada (`BiomeConstructionSupply`) e duas listas de limpeza divergentes.
- `CHANGELOG.md`.

### Corrigido

- O PIT não rodava desde o `78e7efc`: um teste do `core` que sobe o jogo
  derrubava a rodada inteira, e o CI escondia a falha. O teste saiu da
  rodada do PIT, e o CI agora reprova quando o PIT nem começa.
- `./gradlew javadoc` voltou a passar (tinha 6 links quebrados). Os 32 avisos
  de javadoc do Error Prone foram zerados; vários eram comentários separados
  do método pela divisão de classes.

### Testes

- Mutação (PIT, `core`): de 78% para **87,7%** das mutações mortas (1151 de
  1312), força de teste 95%. `MineShaft`, `Building`, `ColonyCycle`,
  `Worker`, `ProfessionAssigner`, `Mine`, `ColonyGoals`, `BuildingRegistry`,
  `ConstructionProject`, `ColonyRoads`, `VacancyEnforcer` e `HiringLog` ficaram
  sem sobrevivente, ou só com equivalentes.
- Três testes que passavam sem medir nada foram refeitos: o cancelamento de
  pedido no `ColonyCycle`, o adiamento de peça no `ConstructionProject` e o
  recentrar das ruas no `ColonyRoads`.
- GameTest novo para o cache de baús, confirmado por mutação.

### Não verificado

- Nada desta publicação foi visto em jogo. O próximo perfil do spark diz se o
  cache baixou o custo da varredura.
