# Checklist — performance com aldeões

> Aldeões são a entidade mais cara do jogo por unidade — cada um roda um Brain
> completo. **Uma vila grande já pesa antes do seu mod.**

```text
custo = custo unitário × aldeões × frequência
```

## Chunk — o mais grave

```bash
grep -rn "world.getBlockState\|getBlockEntity\|getWorld().getBlockState" src/main/java
```

```text
[ ] nenhuma leitura de bloco em posição arbitrária força carga de chunk
[ ] uso getChunkManager().getWorldChunk(x >> 4, z >> 4)
[ ] chunk null é tratado como "não sei agora, pulo" — caso NORMAL
```

> De dentro de um evento de carga de chunk, forçar carga **trava a thread do
> servidor**. O sintoma não é lag: é o servidor parado.

## Frequência

```text
[ ] toda lógica no tick tem frequência justificada
[ ] preferi reativo > sob demanda > periódico > por tick
[ ] trabalho periódico é ESCALONADO entre aldeões
```

```java
if ((villager.getId() + world.getTime()) % INTERVALO != 0) return;
```

> Escalonar não muda o custo total e **divide o pico por N**. Picos são o que o
> jogador sente: TPS médio ótimo com o jogo travando é trabalho concentrado.

## Sensores

```text
[ ] a frequência é a menor que resolve
[ ] a área é a menor que resolve
[ ] considerei reagir a EVENTO em vez de perceber periodicamente
[ ] considerei consultar o PointOfInterestStorage em vez de varrer
```

> Sensor de raio 32, a cada 20 ticks, com 50 aldeões, é a causa número um de lag
> em mod de aldeão.

## Busca

```text
[ ] raio é o menor que resolve
[ ] busca de entidade por CAIXA, nunca iteração global
[ ] varredura de bloco substituída por índice, cache ou varredura incremental
[ ] varredura incremental tem cursor PERSISTIDO
```

**Raio é cúbico:**

```text
raio 16 → ~32 mil blocos   ·   32 → ~262 mil   ·   64 → ~2 milhões
```

Metade do raio é **1/8 do custo** — a otimização de maior retorno.

> Cursor não persistido: cada sessão recomeça e as curtas nunca completam uma
> volta. O sistema parece não funcionar.

## Pathfinding

```text
[ ] recalcula só quando o destino MUDA ou o caminho FALHA
[ ] nunca por tick
[ ] há limite de tentativas
[ ] alvo inalcançável é ABANDONADO
[ ] uso WALK_TARGET e deixo as tasks Vanilla moverem
```

> `[FATO]` `CANT_REACH_WALK_TARGET_SINCE` existe no Vanilla justamente para
> desistir. Retry infinito gasta o cálculo mais caro do jogo, para sempre.

## População

```text
[ ] há limite superior de aldeões
[ ] a reprodução acelerada pelo mod tem teto
[ ] sei o que acontece com 200 camas
```

> Reprodução é a única mecânica que cria entidades sozinha — um gerador de lag
> com atraso.

## Alocação

```text
[ ] nenhum new BlockPos em laço quente (BlockPos.Mutable)
[ ] nenhuma lista nova por tick
[ ] nenhum log por tick por aldeão
```

> Só em caminho quente. Sintoma de GC: travamento **periódico** com TPS normal.

## Escala testada

```text
[ ] 1 aldeão
[ ] 2       ← pega conflito e disputa
[ ] 10
[ ] 50      ← pega custo
[ ] 100
```

> O autor quase sempre testou com dois.

## Medição

```text
[ ] confirmei que o lag é do MOD (reproduzi sem ele)
[ ] distingui TPS baixo (servidor) de FPS baixo (cliente)
[ ] medi com /debug start … /debug stop
[ ] procurei o caminho QUENTE, não o código feio
[ ] escrevi os NÚMEROS: aldeões, frequência, raio
[ ] uma mudança por vez, medindo
```

## Depois de otimizar

```text
[ ] a feature ainda funciona
[ ] o caso null (chunk não carregado) é tratado
[ ] o cache invalida quando deve
[ ] gametest passa
[ ] números antes/depois registrados
```

> Registrar os números evita que a próxima sessão "otimize" de volta o que foi
> feito de propósito.

## Relato

**Com números medidos.** "Ficou mais rápido" não é resultado; "o scanner caiu de
12 ms para 0,4 ms com 20 aldeões" é.
