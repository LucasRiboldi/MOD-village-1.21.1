# Performance

O Minecraft roda a **20 ticks por segundo** — 50 ms para mover todas as entidades,
rodar toda a IA, atualizar todas as block entities, gerar chunk e falar com a
rede. Passar disso é queda de TPS, que o jogador sente como travamento.

A conta que decide:

```text
custo por tick = custo unitário × entidades × frequência
```

**Não é o custo unitário que mata. É a multiplicação.**

## O erro que trava o servidor

```java
// ✗ carrega o chunk que faltar
BlockState state = world.getBlockState(pos);
BlockEntity be   = world.getBlockEntity(pos);
```

Dentro do tick, isso é gerar terreno no meio do laço. Chamado de dentro de um
evento de carga de chunk, a thread passa a esperar por um chunk que só ela
poderia produzir — e **trava**.

```java
// ✓ só lê o que já está carregado
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`[FATO]` verificado em MC 1.21.1: `getWorldChunk` devolve `null` para chunk não
carregado, sem forçar geração.

Tratar `null` como **"não sei agora, pulo"** é quase sempre a semântica correta —
e costuma ser a regra de design certa também: o mod não deve segurar chunk que o
jogo já soltou.

```bash
grep -rn "world.getBlockState\|world.getBlockEntity\|getWorld().getBlockState" src/main/java
```

Todo resultado é suspeito. Confira se a posição é arbitrária e se está no tick.

## Frequência — as quatro categorias

```text
EVENT DRIVEN   reage a algo que aconteceu       ← prefira
ON DEMAND      calcula quando perguntam
PERIODIC       a cada N ticks
EVERY TICK     20×/s                             ← justifique
```

Antes de qualquer tick:

```text
Por que precisa rodar todo tick?
Pode reagir a um evento?
Pode ter cooldown?
Pode rodar a cada N ticks?  (20 = 1s · 100 = 5s · 600 = 30s)
Pode ser calculado sob demanda?
```

```java
if (world.getTime() % 20 != 0) return;    // 1×/s em vez de 20×/s
```

### Escalone

Se N entidades precisam de trabalho periódico, **não faça todas no mesmo tick**:

```java
if ((entity.getId() + world.getTime()) % INTERVALO != 0) return;
```

Custo total igual; pico por tick dividido por N. **Picos são o que o jogador
sente** — o TPS médio pode estar ótimo e o jogo travando a cada segundo.

## Busca

**Entidades** — nunca itere a população do mundo. Busca por caixa, com o menor
raio que resolve.

**Blocos** — varredura por volume é cara. Alternativas, em ordem:

1. **índice mantido por evento** (bloco colocado/quebrado)
2. **cache com validade explícita**
3. **varredura incremental** — um pedaço por ciclo, guardando o cursor
4. varredura completa, se nada acima servir

A terceira é subestimada: dezessete passagens espalhadas não aparecem no tick;
feitas de uma vez, aparecem.

**POI** — `PointOfInterestStorage` já é um índice espacial. Perguntar a ele é
muito mais barato que varrer blocos procurando camas ou locais de trabalho.

### Raio é cúbico

```text
raio 16 →  ~32 mil blocos
raio 32 →  ~262 mil
raio 64 →  ~2 milhões
```

**Metade do raio é 1/8 do custo.** É a otimização de maior retorno e a mais fácil
de esquecer.

## Pathfinding

Entre os cálculos mais caros do jogo.

```text
[ ] recalcula só quando o destino MUDA ou o caminho FALHA — nunca por tick
[ ] há limite de tentativas
[ ] alvo inalcançável é abandonado, não retentado para sempre
[ ] o destino está em chunk carregado
```

Em mobs de Brain, escreva `WALK_TARGET` e deixe as tasks Vanilla moverem — elas
já têm a lógica de custo. Ver `ai-development.md`.

**Retry infinito num alvo inalcançável é o pior caso possível:** gasta o cálculo
mais caro do jogo, para sempre, sem progresso.

## Alocação

Milhares de objetos por segundo geram pressão de GC — que aparece como
**travamento periódico**, não como TPS baixo. Se o sintoma é "trava a cada tanto"
com TPS normal, suspeite daqui.

```text
new BlockPos em laço          → BlockPos.Mutable
lista nova a cada tick        → reutilize
boxing em laço quente
string concatenada em log de tick
```

**Só em caminho quente.** Otimizar alocação no que roda uma vez por sessão é
ruído que piora a legibilidade sem ganho.

## Rede

```text
packet por tick × entidades × jogadores
```

Satura antes da CPU, e o sintoma **parece** lag de servidor — o que manda a
investigação para o lado errado. Sincronize por mudança, agregue, e não mande o
que o cliente já tem. Ver `networking.md`.

## Teste mentalmente com escala

```text
1 · 10 · 50 · 100 · 500
```

Lógica que funciona com uma entidade pode destruir uma vila grande. **O autor
quase sempre testou com duas.**

## Medir, não adivinhar

```text
/debug start
… reproduzir 20-30 segundos …
/debug stop
```

**Procure o caminho quente, não o código feio.** O laço que parece caro roda uma
vez por sessão; o método de três linhas roda vinte vezes por segundo por entidade.

Uma mudança por vez, medindo. Duas juntas e você não sabe qual funcionou — nem se
uma piorou.

Ver `workflows/performance-workflow.md`.

## Checklist

```text
[ ] nenhuma leitura de bloco força carga de chunk
[ ] toda lógica no tick tem frequência justificada
[ ] trabalho periódico é escalonado entre entidades
[ ] raios de busca são os menores que resolvem
[ ] pathfinding não recalcula por tick
[ ] há limite de tentativas e timeout
[ ] busca de entidade usa caixa, não iteração global
[ ] POI é consultado em vez de varredura, quando aplicável
[ ] testado com carga realista, não com duas entidades
[ ] números medidos, não impressões
```
