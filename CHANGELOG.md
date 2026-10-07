# Changelog

Mudanças que chegam ao JAR ou à forma de verificar o mod. O formato segue o
[Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/).

A versão continua **0.3.0 alpha**: o mesmo número é republicado a cada
entrega, e o que separa uma publicação da outra é o commit e o SHA-256 do JAR
(ver `STATE.md` e `scripts/release_manifest.py`). O histórico anterior a
2026-09-24 está em [`docs/archive/technical/Development-Log.md`](docs/archive/technical/Development-Log.md)
e [`docs/archive/technical/Historico-2026-09.md`](docs/archive/technical/Historico-2026-09.md).

## [0.3.0] — publicação de 2026-10-03, fim da tarde (playtest da manhã)

**Ainda não visto em jogo; a última parte não foi testada** (pedido do autor). JAR SHA-256 `D4FFE0FC…7C99`.

### Corrigido

- Ícones de profissão e de obra trocados: os arquivos de textura estavam com
  os nomes errados.
- Obra parada por peça sem rota uma de cada vez: as tentativas contam para
  todas as peças que faltam no mesmo ciclo.
- Obra com só peças sem apoio sobrando esperava 10 min; agora 1 min.

### Adicionado

- Placa com fundo em pixel art, ícone à esquerda e texto à direita.
- Área da vila marcada com partículas verdes perto do jogador.
- Água e lava lidas uma vez ficam de fora das buscas seguintes (sem teste).

### Alterado

- Base das obras: só a grama fica como chão; a plantação constrói a base
  inteira; bloco que já está no lugar conta como assentado.
- Mina: arco de 5 × 4 com um lampião de cada lado e vão de 3 × 3; salas de
  busca de 3 × 3.

## [0.3.0] — publicação de 2026-10-03, tarde (medida da vila)

**Ainda não visto em jogo.** JAR SHA-256 `00658B2C…92E4`.

### Alterado

- **Medida da vila (ADR-003 Emenda 8):** parte das peças da vila gerada, sem
  a folga de 12 do jogo; 15 blocos em volta de cada obra; só o lado que a obra
  passa avança; lados ímpares, para o centro ser um bloco só.
- **Rua faz a vila crescer só com 10 caminhos conectados fora dela**,
  encostados num caminho de dentro — de quem quer que seja, colônia ou
  jogador. Antes, cada bloco que a colônia assentava empurrava a borda.
- Saves antigos têm a vila medida de novo na primeira visita.

## [0.3.0] — publicação de 2026-10-03, manhã (revisão das profissões)

**Ainda não visto em jogo.** JAR SHA-256 `368A4C4F…FD65`. Relatório em
`docs/research/2026-10-03-paradas-por-profissao.md`.

### Adicionado

- **Arte em pixel nos overlays:** ícone de profissão sobre o aldeão e de
  estado sobre a obra, desenhados no `WorldRenderEvents.LAST` com buffer
  próprio (o cliente com shaders do Iris não mostrava nada).
- **Material que não se acha aparece:** três buscas vazias do alcance da vila
  põem o material no baú de quem o usa (fundidor para areia, argila e cacto;
  construtor para o resto). Vale para material da natureza — revê a decisão
  de 26-09. O castigo entre buscas caiu de 5/10/20 min para 1/2/4 min.

### Corrigido

- Pastor sem ovelha com lã no raio segurava a tarefa calado; agora solta e,
  na 3ª busca, a lã aparece.
- Carpinteiro fechava tarefa de tábua com 0 peça: lia só os baús de
  trabalhador, e a meta todos os da vila.
- Fundidor repetia `stopped — none of … had sand` a cada segundo.

### Retirado

- O atalho que riscava a vidraça na primeira busca vazia de areia (da
  publicação das 02h): com a areia aparecendo, a casa não perde a janela.

## [0.3.0] — publicação de 2026-10-03, 02h (playtest da madrugada)

Correções do playtest de 01:02–01:32 (Spark `r6nErbWNSL`: TPS 20, mod em
1,9% do tick). **Ainda não vistas em jogo.** JAR SHA-256 `D2374106…0615`.

### Corrigido

- **Construtor parado fora da casa fechada:** o lugar de pé que a navegação
  Vanilla marca como inalcançável (`CANT_REACH_WALK_TARGET_SINCE`) é riscado
  em 1 s e ele vai a outro lado; sem nenhum lado alcançável, a peça vai para
  o fim da fila na hora. Antes: 300 tiques parado ou 200 por peça.
- **Vidraça esperando areia que o mundo não tem:** com a varredura de areia
  vazia e nenhum vidro no baú, a barreira da Regra 28 risca a vidraça sem a
  carência de 2,5 min.

## [0.3.0] — publicação de 2026-10-03 (corrigíveis sem jogo)

JAR do commit dos corrigíveis, SHA-256 `923D9769…40C0`, em `downloads/` e em
`.minecraft/mods` (três cópias conferidas). **Nada desta lista foi visto em
jogo.**

### Adicionado

- **Busca de lote mais rápida na vila com jogador dentro** (estudo de 01-10,
  §7-A): uma passagem extra por segundo continua a varredura em curso, com o
  mesmo prazo de 15 ms do ciclo.
- **Encalhado fechado por blocos protegidos** atravessa só a peça da planta
  da própria colônia (o reparo a reconstrói) e, sem saída nem assim, avisa os
  jogadores no chat a cada 5 minutos. Teletransporte continua vetado.
- **Fundidor sem cru** desce um degrau da fornalha: a obra que pede pedra lisa
  põe a pedra na lista, e a pedra sai do pedregulho.
- **Linhas novas no log:** para onde o aldeão ia quando entrou no curral
  (`entered a pen`) e quem mudou as regras de profissão
  (`Profession policy changed by`).
- `scripts/verdict.py` com os itens de 30-09 a 02-10, lendo também os
  `.log.gz` do dia.

### Mudado

- Nenhum arquivo de produção passa de 500 linhas (oito divididos, texto
  movido sem mudança).
- Cobertura JaCoCo da bateria de jogo em `build/reports/jacoco/gametest`.

## [0.3.0] — publicação de 2026-10-02 (consolidação)

JAR do commit da consolidação de 02-10, SHA-256 `AB762691…9FFA`, em
`downloads/` e em `.minecraft/mods` (três hashes conferidos). Junta tudo o que
saiu de 26-09 a 02-10; o detalhe de cada item está no
[`Development-Log.md`](docs/archive/technical/Development-Log.md) e no `TODO.md`.
**Nada desta lista foi visto em jogo depois da consolidação.**

### Adicionado

- **Regras de profissão por mundo** pelo Mod Menu (opcional): ativar,
  limitar, raio de busca e ordem de contratação (ADR-030). A fundação mantém o
  carpinteiro logo depois do lenhador.
- **Sobreposições no cliente:** profissão sobre o aldeão e progresso da obra
  com o primeiro material que falta.
- **Ajudante no tempo ocioso (Regra 48, metade de B-1):** recolhe do chão só
  o item que falta à obra aberta.
- **`/vc log` com as esperas longas (B-5)** e o tempo dos aldeões por
  profissão (Regra 50, linha `VC_TIME`).
- Vila como caixa que cresce e só trabalha com jogador dentro (ADR-003
  Emendas 6 e 7); colônias que se tocam viram uma (ADR-007); rua que cresce
  para fora; pasta de modelos da colônia (Regra 27-e3); ordem das obras
  (Regra 49); índice de árvores (A-2).

### Corrigido

- Aldeão preso no curral (E52), encalhado que não desistia e o retorno pela
  mina (E47), ponta de vila virando colônia nova (E51), tarefa solta voltando
  ao mesmo aldeão (E49), canteiro na grama (E50).
- Obra: carpinteiro fechando tarefa com 0 peça (A-1), piso e base da obra,
  material pedido inteiro (A-3), construtor parando fora do alcance, placa
  órfã, obra que continua longe do jogador.
- Fundidor aceita carvão vegetal onde a obra pede carvão (metade de B-4).

### Mudado

- Ferramenta: velocidade da que ele tem e a melhor do baú (Regras 2-e1,
  37-e1); mina nova na borda, longe da água (A-4); baús da colônia são todos
  os da vila (Regra 45).

## [0.3.0] — entregas de 2026-09-26 a 09-30 (publicadas nos JARs seguintes)

### Corrigido

- **Todo aldeão de profissão tem baú** (decisão do autor). Na sessão longa, o
  lenhador, o mineiro e o pedreiro passaram 4h45 sem baú e, sem baú, sem
  tarefa. Quem não acha baú ganha um ao lado da cama (regra b) ou, sem cama,
  perto do centro; o vínculo é salvo com o mundo.
- **A peça pronta só nasce para bloco de manufatura** (decisão do autor): tora,
  terra, grama, pedra, areia, lã tosquiada, flor e muda vêm da profissão.
- O guarda de alcance não larga mais a obra encostada numa rua só porque o
  índice de ruas ainda não existe.
- A obra que só tem o chão enterrado volta a poder ceder lugar (defeito
  introduzido pela camada da rua).

### Mudado

- **Pastor e fazendeiro trabalham sem depender da obra**, enquanto couber no
  baú deles; a obra continua tendo prioridade.
- O fundidor deixa de perseguir produto de fornalha sem matéria-prima no bioma
  (582 buscas inúteis por arenito numa vila de planície).

## [0.3.0] — publicação de 2026-09-26 (sessões de jogo de 26-09)

JAR do commit `70af4c8`, SHA-256 `9EB559D1…9423`, em `downloads/` e em
`.minecraft/mods` (três hashes conferidos).

### Corrigido

- **A casa nasce com o chão e a porta na altura da rua, sem camada de terra
  na base** (pedido do autor). As casas de vila do jogo gravam uma fundação
  abaixo do piso — na `plains_small_house_5`, uma camada inteira de terra — e
  o mod punha a planta um bloco acima do chão: a casa saía sobre um monte de
  terra, com a porta três blocos acima da rua. Agora a camada em que se pisa
  ao entrar (uma abaixo da porta) vai no chão; o que fica abaixo dela e a
  terra/grama na altura da rua não são construídos, e o piso dessa camada toma
  o lugar da grama. Casas e obras que o mundo já tem não mudam.
- **O viveiro cheio não reconta o terreno a cada chamada.** Com os 10 viveiros
  de pé, cada pedido relia ~166 mil blocos (3º maior custo do mod no spark de
  26-09); agora espera os 5 minutos como quem plantou.

- **O lenhador não é mais expulso enquanto procura árvore.** Na vila sem
  árvore natural, o guarda de imobilidade devolvia a tarefa a cada 300 tiques
  ("while looking for a tree") até ele largar o ofício. Agora os guardas só
  contam com uma árvore escolhida.
- **O tapete verde (e toda peça colorida) não é mais pedido pela receita de
  tingir peça pronta** ("corante + tapete de outra cor", que puxava uma
  corrente de tingimentos que nunca fechava). Tingir lã e misturar corante
  continuam valendo: são produção.
- **A espera por peça sem rota não recomeça ao reabrir o mundo.** O relógio de
  10 ciclos da ADR-022 é salvo com o mundo; na sessão de 26-09 o tapete ia
  chegar 30 s depois de o jogo fechar.
- **O desvio do mineiro:** o próprio corpo parado na beira do bloco não impede
  mais o degrau ("something stood in"); ele volta ao meio do bloco. Quem sai
  do caminho (caiu, foi empurrado) tem o desvio replanejado de onde está, até
  3 vezes, em vez de esperar 100 tiques pelo passo que ficou para trás.

### Mudado

- **Baú das camas da vila vanilla (decisão do autor, regra b):** ao lado da
  cama, encostado numa parede, nunca na frente da porta. A porta deixou de
  decidir se há baú — a regra de 23-09 recusava 3 de 3 camas quando a caixa da
  casa incluía o degrau de fora.
- **O lenhador sem árvore pede um lote de 4 mudas** ao viveiro (no mesmo ritmo
  de 5 min e no mesmo teto de 10), em vez de 1 muda por vez.

## [0.3.0] — publicação de 2026-09-25, tarde (ADR-025, fases 1 e 2)

JAR do commit `a222342`, SHA-256 `8862EC4F…07C0`, em `downloads/` e em
`.minecraft/mods` (os três hashes conferidos pelo `release_manifest.py`).

### Adicionado

- **O mineiro que não chega cava o próprio caminho** (fase 2). Quando fica
  parado, não se aproxima ou anda demais sem chegar, ele planeja um desvio de
  até 16 blocos: abre rocha, põe pedregulho onde falta chão (tirado do baú
  dele) e anda um bloco de cada vez até ter a pedra ao alcance. Só sem desvio
  ele larga a pedra como antes. Log: `takes a detour`, `is through its detour`,
  `its detour failed — …`.
- **O encalhado sem escada natural também desvia.** Onde o E47 desistia com
  "no natural, dry way up", ele procura o nível do terreno cavando e pondo
  bloco; se o raio não basta, anda o trecho que mais se aproxima e tenta de
  novo.
- O desvio nunca cava rocha encostada em líquido nem com areia em cima, nunca
  pisa em líquido, e pergunta de novo antes de cada passo — água que chega
  depois do plano o faz parar.

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

- **O mineiro não quebra mais bloco com líquido atrás** (pedido do autor).
  Nenhuma pedra, minério, areia ou degrau encostado em água ou lava vira alvo;
  a galeria contorna. Antes ele quebrava e vedava depois, e a vedação não
  alcançava a água da vila. Areia de praia colada na água deixa de ser colhida.
- **A pedra recusada volta a ser recusada depois de carregar o mundo.** A
  marca que afasta a pedra inalcançável (E44) é salva
  (`villagecolony_marks.dat`); antes sumia a cada sessão, e o mineiro voltava
  à mesma pedra.

### Ainda não resolvido

- O motivo do travamento em 553, 39, 158: a geometria reconstruída do save é
  andável e a navegação Vanilla chega lá (GameTest forense). Espera a linha
  `brain:` de uma sessão de jogo.

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
