# Análise de performance

O Minecraft roda a **20 ticks por segundo**. Cada tick tem 50 ms para mover todas
as entidades, rodar toda a IA, atualizar todas as block entities, gerar chunk e
falar com a rede. Passar disso é queda de TPS, e o jogador sente como travamento.

Todo custo que você acrescenta é multiplicado por **frequência × população**. É
essa multiplicação, e não o custo unitário, que decide.

## A conta que importa

```text
custo por tick = custo unitário × entidades × frequência
```

Um scan de 32×32×32 blocos custa ~32 mil leituras. Por aldeão. Vinte aldeões, todo
tick: 655 milhões de leituras por segundo. O autor testou com dois aldeões e achou
rápido.

**Sempre teste mentalmente com 1, 10, 50, 100 e 500.** Lógica que funciona para um
pode destruir uma vila grande.

## Escala de custo

Do mais barato ao mais caro:

```text
ler campo em memória
ler bloco de chunk JÁ carregado
buscar entidade por caixa pequena
buscar POI
buscar bloco num volume
PATHFINDING
carregar/gerar chunk        ← trava o tick do servidor
```

Os dois últimos merecem cuidado desproporcional.

## Chunk: o erro que trava o servidor

`world.getBlockState(pos)` e `world.getBlockEntity(pos)` **carregam o chunk que
faltar**. Dentro do tick, isso é gerar terreno no meio do laço. Chamado de dentro
de um evento de carga de chunk, a thread passa a esperar por um chunk que só ela
poderia produzir — e trava.

A leitura segura:

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`[FATO]` verificado em MC 1.21.1: `getWorldChunk` devolve `null` para chunk não
carregado, sem forçar geração.

Tratar `null` como "não sei agora, pulo" é quase sempre a semântica correta: o mod
não deve segurar chunk que o jogo já soltou. E isso costuma ser também a regra de
design certa — colônia hiberna com o chunk, como a vila Vanilla.

## Frequência: as quatro categorias

```text
EVENT DRIVEN   reage a algo que aconteceu      ← prefira
ON DEMAND      calcula quando perguntam
PERIODIC       a cada N ticks
EVERY TICK     20×/s                            ← justifique
```

Períodos usuais: **20** (1 s), **100** (5 s), **600** (30 s). A pergunta antes de
qualquer tick:

```text
Por que precisa rodar todo tick?
Pode reagir a um evento?
Pode ter cooldown?
Pode rodar a cada N ticks?
Pode ser calculado sob demanda?
```

**Escalone.** Se N entidades precisam de trabalho periódico, não faça todas no
mesmo tick — distribua por `entityId % intervalo`. O custo total é o mesmo, o pico
por tick cai por um fator de N. Picos são o que o jogador sente.

## Pathfinding

Entre os cálculos mais caros do jogo, e o mais fácil de desperdiçar.

```text
Com que frequência o caminho é recalculado?
Quantas entidades ao mesmo tempo?
O que acontece quando falha? Há retry infinito?
O destino está em chunk carregado?
O destino é alcançável? (custo alto é gasto para descobrir que não)
```

Recalcule quando o destino **mudar** ou o caminho **falhar** — não por tick. Em
mobs de Brain, escreva `WALK_TARGET` e deixe as tasks Vanilla moverem; elas já
têm a lógica de custo. Ver `ai-brain-analysis.md`.

Retry infinito num alvo inalcançável é o pior caso: gasta o cálculo mais caro do
jogo, para sempre, sem progresso.

## Busca

**Entidades** — nunca itere a população do mundo para achar algumas. Busca por
caixa, com o menor raio que resolve. Raio é cúbico: dobrar o raio multiplica o
volume por oito.

**Blocos** — varredura por volume é cara. Alternativas, em ordem:

1. índice mantido por evento (bloco colocado/quebrado)
2. cache com validade
3. varredura incremental — um pedaço por ciclo, guardando o cursor
4. varredura completa, só se as anteriores não servirem

A terceira é subestimada: uma varredura de dezessete passagens, feita uma por
ciclo, não aparece no tick. Feita de uma vez, aparece.

**POI** — o `PointOfInterestStorage` já é um índice espacial. Perguntar a ele é
muito mais barato que varrer blocos procurando camas ou locais de trabalho.

## Alocação

Milhares de objetos por segundo geram pressão de GC, e GC é pausa — que aparece
como travamento periódico, não como TPS baixo.

Cuidado com: `new BlockPos` em laço (use `BlockPos.Mutable`), lista nova a cada
tick, boxing em laço quente, string concatenada em log de tick.

Isso só importa em caminho quente. Otimizar alocação em código que roda uma vez
por sessão é ruído.

## Rede

```text
Packet por tick por entidade × N entidades × M jogadores
```

Satura antes da CPU e o sintoma **parece** lag de servidor. Sincronize por
mudança, não por tick; agregue quando possível; não mande o que o cliente já tem
(ver `client-server-analysis.md`).

## Investigar performance existente

Modo FORENSIC. Ordem que costuma achar rápido:

1. **Confirme que é o mod.** Reproduza sem ele. Sem essa comparação você pode
   otimizar o inocente.
2. **Onde está o tempo** — profiler embutido (`/debug start` … `/debug stop`), ou
   um profiler de JVM.
3. **Ache o caminho quente**, não o código feio. Impressão de lentidão erra mais
   que acerta.
4. **Conte**: quantas entidades? qual frequência? qual raio? Escreva os números.
5. **Uma mudança por vez**, medindo. Duas mudanças juntas e você não sabe qual
   funcionou.

## O que registrar

```text
FREQUÊNCIA DE TICK · Nº DE ENTIDADES · TAMANHO DO MUNDO
ACESSO A CHUNK · CUSTO DE PATHFINDING · RAIO DE BUSCA
CUSTO DE ITERAÇÃO · ALOCAÇÃO · TRÁFEGO DE REDE
```

Com números, não adjetivos. "Busca de raio 64 por aldeão a cada 5 s, com até 20
aldeões" é analisável. "Pode ficar pesado" não é.
