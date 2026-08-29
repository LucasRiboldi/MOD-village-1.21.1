# Performance com aldeões

Aldeões são a entidade mais cara do jogo por unidade: cada um roda um Brain
completo, com sensores, memórias e pathfinding. Uma vila grande já pesa **antes**
do seu mod.

```text
custo por tick = custo unitário × aldeões × frequência
```

## Escala de teste

```text
1 · 10 · 50 · 100 · 500
```

**O autor quase sempre testou com dois.** Uma lógica que roda em 0,5 ms por
aldeão parece instantânea com dois e consome metade do tick com cinquenta.

## Os custos, do maior para o menor

```text
PATHFINDING             o mais caro
VARREDURA DE BLOCO      raio é cúbico
BUSCA DE ENTIDADE       por caixa; nunca global
CONSULTA A POI          barata — é índice
LEITURA DE MEMÓRIA      quase grátis
```

Se a sua lógica está no topo dessa lista e roda por tick, o problema é esse.

## Chunk — o erro que trava

```java
// ✗ carrega o chunk que faltar
BlockState state = world.getBlockState(pos);

// ✓ só lê o carregado; null = "não sei agora"
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`[FATO]` MC 1.21.1: `getWorldChunk` devolve `null` para chunk não carregado, sem
forçar geração.

Do tick, forçar carga é gerar terreno dentro do laço. De dentro de um evento de
carga de chunk, **trava a thread do servidor**: ela passa a esperar por um chunk
que só ela poderia produzir.

E é a regra de design certa também: uma colônia **hiberna com o chunk**, como a
vila Vanilla.

## Frequência

```text
EVENT DRIVEN   ← prefira
ON DEMAND
PERIODIC       20 / 100 / 600 ticks
EVERY TICK     ← justifique
```

```java
if (world.getTime() % 20 != 0) return;    // 1×/s em vez de 20×/s
```

### Escalonar — o ganho maior

```java
if ((villager.getId() + world.getTime()) % INTERVALO != 0) return;
```

Custo total igual, pico por tick dividido por N. **Picos são o que o jogador
sente**: TPS médio ótimo e o jogo travando a cada segundo é sinal de trabalho
concentrado.

## Busca

**Raio é cúbico:**

```text
raio 16 →  ~32 mil blocos
raio 32 →  ~262 mil
raio 64 →  ~2 milhões
```

Metade do raio é **1/8 do custo**. É a otimização de maior retorno e a mais
esquecida.

Alternativas à varredura, em ordem:

1. **`PointOfInterestStorage`** — se o alvo pode ser POI, use o índice do jogo
2. **índice próprio por evento** — bloco colocado/quebrado atualiza
3. **cache com validade**
4. **varredura incremental** — um pedaço por ciclo, com cursor **persistido**
5. varredura completa

O cursor persistido importa: sem ele, cada sessão recomeça e as sessões curtas
nunca completam uma volta — o sistema parece não funcionar.

## Sensores

Sensor é o custo estrutural do Brain. Sensor próprio com frequência alta e área
grande, multiplicado pela vila, é a causa número um de lag em mod de aldeão.

```text
[ ] a frequência é a menor que resolve?
[ ] a área é a menor que resolve?
[ ] dá para reagir a evento em vez de perceber periodicamente?
```

## Pathfinding

```text
[ ] recalcula só quando o destino MUDA ou o caminho FALHA
[ ] nunca por tick
[ ] há limite de tentativas
[ ] alvo inalcançável é abandonado
```

`[FATO]` `CANT_REACH_WALK_TARGET_SINCE` existe no Vanilla justamente para
desistir. **Retry infinito num alvo inalcançável é o pior caso possível:** gasta
o cálculo mais caro do jogo, para sempre, sem progresso.

## Reprodução

A única mecânica que **cria entidades sozinha**.

```text
[ ] há limite superior de população?
[ ] o que acontece se o jogador construir 200 camas?
```

Um mod que acelera reprodução sem limite é um gerador de lag com atraso.

## Medir, não adivinhar

```text
/debug start
… reproduzir 20-30 s …
/debug stop
```

```text
[ ] confirmei que o lag é do MOD (reproduzi sem ele)
[ ] distingui TPS baixo (servidor) de FPS baixo (cliente)
[ ] procurei o caminho QUENTE, não o código feio
[ ] escrevi os NÚMEROS: aldeões, frequência, raio
[ ] uma mudança por vez, medindo
```

**TPS baixo é servidor; FPS baixo é cliente.** Lógica de aldeão dá TPS.
Travamento periódico com TPS normal costuma ser GC (alocação).

## Depois de otimizar

```text
[ ] a feature ainda funciona
[ ] o caso null (chunk não carregado) é tratado
[ ] o cursor de varredura incremental persiste
[ ] gametest passa
[ ] os números antes/depois registrados
```

Registrar os números evita que a próxima sessão "otimize" de volta o que foi
feito de propósito.

## Checklist

```text
[ ] nenhuma leitura de bloco força carga de chunk
[ ] frequência justificada, e escalonada entre aldeões
[ ] raios são os menores que resolvem
[ ] POI consultado em vez de varredura, quando aplicável
[ ] varredura incremental com cursor persistido, se necessária
[ ] sensores com frequência e área mínimas
[ ] pathfinding não recalcula por tick
[ ] há limite de população
[ ] testado com 1, 10, 50, 100
[ ] números medidos, não impressões
```
