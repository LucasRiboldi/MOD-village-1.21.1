# Changelog

Mudanças que chegam ao JAR ou à forma de verificar o mod. O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

A versão continua **0.3.0 alpha**: o mesmo número é republicado a cada
entrega, e o que separa uma publicação da outra é o commit e o SHA-256 do JAR
(ver `STATE.md` e `scripts/release_manifest.py`). O histórico anterior a
2026-09-24 está em [`docs/technical/Development-Log.md`](docs/technical/Development-Log.md)
e [`docs/technical/Historico-2026-09.md`](docs/technical/Historico-2026-09.md).

## [Não publicado] — ADR-025, fase 1 (mineiro autônomo)

### Adicionado

- **A passagem da mina sai com chão.** Quando a picareta abre uma célula da
  escada ou da sala sobre um vão de caverna que a mina não planejou, o vão
  recebe pedregulho na hora (`The mine floored …` no log). O degrau seguinte e
  a camada de baixo da sala não são tapados; fora da passagem — o veio que o
  mineiro segue — também não.
- **O mineiro não entra na água.** Ele passa a dar a volta seca em vez de
  atravessar nadando (penalidade de água -1 só para ele; lava já era proibida
  no Vanilla).
- A linha de travamento do mineiro traz o estado do cérebro e da navegação
  (`brain: activity …, our walk task …, walk target …, navigation …`), para a
  próxima sessão dizer por que ele para na pedra 553, 39, 158.

### Corrigido

- **A pedra recusada volta a ser recusada depois de carregar o mundo.** A
  marca que afasta a pedra inalcançável (E44) é salva
  (`villagecolony_marks.dat`); antes sumia a cada sessão, e o mineiro voltava
  à mesma pedra.

### Ainda não resolvido

- O motivo do travamento em 553, 39, 158: a geometria reconstruída do save é
  andável e a navegação Vanilla chega lá (GameTest forense). Espera a linha
  `brain:` de uma sessão de jogo.
- Bloco que segura fluido protegido (água da vila) ainda é quebrado.

## [0.3.0] — publicação de 2026-09-25, manhã (sessão das 09:13)

### Corrigido

- **A vila presa a templos.** O reparo rodava antes do rodízio casa ↔ outra
  obra e reabria a obra abandonada no mesmo segundo em que a colônia
  desistia dela; ao terminar o templo, a vila reabriu na hora um templo
  abandonado vizinho de 301 blocos. Agora a obra abandonada só volta **na
  vez do tipo dela** (decisão do autor) — nunca logo depois de largada —, e
  a construção terminada que perdeu blocos continua sendo reparada sempre.
  A obra abandonada que o save trouxe aberta também espera a vez.
- A obra retomada e terminada passa a contar como a última tentada no
  rodízio.

### Adicionado

- **O mineiro cava sem parar enquanto os baús dele tiverem espaço**
  (decisão do autor: "galerias novas sem parar"). A meta de pedra passa a
  ser o guardado mais o espaço livre nos baús dos mineiros, como a da
  madeira; a mina continua crescendo sozinha e cada corte segue o minério
  que encontra.

### Ainda não resolvido

- O mineiro empaca sempre na mesma pedra inalcançável (553, 39, 158 no save
  do autor), fica preso e sai da escala até alguém o soltar. A marca que
  afasta a pedra recusada vive só na memória e some ao carregar o mundo
  (E44/E45).

## [0.3.0] — publicação de 2026-09-25, manhã (sessão das 08:31)

### Corrigido

- **A obra que nunca fechava.** O templo da vila ficou aberto por três
  sessões com nove peças "sem apoio" — quatro escadas de mão e cinco tochas
  de parede, todas com pedregulho encostado. A planta não guarda a direção
  do bloco, e as peças do miolo da casa ficavam viradas para o norte,
  procurando apoio num lado onde só havia ar. Agora a peça de parede que não
  se sustenta na direção deduzida se apoia na parede que existe.
- **Peça adiada por uma versão anterior volta à fila** quando já cabe, sem
  esperar a vizinhança mudar. É o que destrava o save em que a obra já está
  presa.
- **A placa conta as peças esperando apoio** ("Obra: 9 blocos · 9 sem
  apoio"), em vez de parecer que a obra voltou atrás.

### Ainda assim

- A direção de verdade (a tocha do lado que o arquivo da estrutura manda)
  continua perdida na leitura da planta: a peça agora se apoia numa parede
  que existe, que pode não ser a do desenho original.
- Peças riscadas (tocha sem carvão pela barreira de teste, bloco no caminho)
  voltam como pendentes a cada carregamento do mundo, porque a retomada relê
  o mundo. Com as peças de parede destravadas a obra fecha; o riscado vira
  lacuna, como antes.

## [0.3.0] — publicação de 2026-09-25, tarde

Correções do que a sessão de jogo de 25-09 (01:19–01:45) mostrou.

### Corrigido

- **Fechar o mundo derrubava a limpeza da memória do servidor**
  (`ConcurrentModificationException` em `ServerMemory.resetAll`): uma limpeza
  carregava outra classe, que se inscrevia no meio da volta, e o resto ficava
  sem limpar. A mesma chamada roda ao abrir o mundo. Agora a volta é sobre uma
  cópia, repetida até ninguém novo aparecer.
- **A placa da obra diz o bloco que realmente falta**, com o nome em
  português que o jogo dá: o do próximo bloco em que o construtor parou, se
  os baús não o têm; senão, o primeiro material em falta; e, sem nada em
  falta, só a contagem de blocos. Antes ela mostrava o primeiro material da
  planta, e no templo de 25-09 podia anunciar o pedregulho (34 no baú)
  enquanto a obra esperava tocha.

### Medido em jogo

- Perfil do spark da sessão: TPS 20 em todos os 22 minutos, MSPT mediano de
  14 a 16 ms. O mod caiu de 4,1% para 0,3% da thread do servidor, e o
  `nearestFirst` de 332 ms para 16 ms — com a ressalva de que o construtor
  passou boa parte da sessão parado esperando tocha.

## [0.3.0] — publicação de 2026-09-25, madrugada

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
