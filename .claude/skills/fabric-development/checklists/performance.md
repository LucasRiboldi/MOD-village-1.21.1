# Checklist — performance

> A conta que decide: **custo unitário × entidades × frequência**. Não é o custo
> unitário que mata; é a multiplicação.

## Acesso a chunk — o mais grave

```bash
grep -rn "world.getBlockState\|world.getBlockEntity\|getWorld().getBlockState" src/main/java
```

```text
[ ] nenhuma leitura de bloco em posição arbitrária força carga de chunk
[ ] leituras usam getChunkManager().getWorldChunk(x >> 4, z >> 4)
[ ] chunk null é tratado como "não sei agora, pulo" — não como erro
```

> `world.getBlockState` **carrega o chunk que faltar**. Do tick, é gerar terreno
> no laço; de dentro de um evento de carga de chunk, **trava a thread**.

## Frequência

```text
[ ] toda lógica no tick tem frequência justificada
[ ] preferi reativo > sob demanda > periódico > por tick
[ ] períodos usam múltiplos sensatos (20 = 1s · 100 = 5s · 600 = 30s)
[ ] trabalho periódico é ESCALONADO entre entidades
```

```java
if ((entity.getId() + world.getTime()) % INTERVALO != 0) return;
```

> Escalonar não muda o custo total e divide o **pico** por N. Picos são o que o
> jogador sente — o TPS médio pode estar ótimo e o jogo travando.

## Busca

```text
[ ] nenhuma iteração global de entidades
[ ] busca de entidade por caixa, com o menor raio que resolve
[ ] varredura de bloco evitada, ou incremental com cursor
[ ] POI consultado em vez de varredura, quando aplicável
[ ] cache tem validade explícita
```

> **Raio é cúbico.** Dobrar o raio multiplica o volume por 8; metade do raio é
> 1/8 do custo. É a otimização de maior retorno e a mais esquecida.

## Pathfinding

```text
[ ] recalcula só quando o destino MUDA ou o caminho FALHA
[ ] nunca por tick
[ ] há limite de tentativas
[ ] alvo inalcançável é abandonado, não retentado para sempre
[ ] em mobs de Brain, uso WALK_TARGET em vez de navegação direta
```

> Retry infinito num alvo inalcançável gasta o cálculo mais caro do jogo, para
> sempre, sem progresso.

## Alocação

```text
[ ] nenhum new BlockPos em laço quente (BlockPos.Mutable)
[ ] nenhuma lista nova por tick
[ ] nenhuma string concatenada em log de tick
```

> Só em **caminho quente**. Otimizar o que roda uma vez por sessão é ruído que
> piora a legibilidade sem ganho.
>
> Sintoma de pressão de GC: travamento **periódico** com TPS normal.

## Rede

```text
[ ] sincronização por mudança, não por tick
[ ] nenhum packet para dado que o cliente já tem
[ ] packets agregados onde faz sentido
```

> Rede satura antes da CPU, e o sintoma **parece** lag de servidor.

## Escala

```text
[ ] testei mentalmente com 1, 10, 50, 100, 500
[ ] testei DE VERDADE com carga realista
```

> O autor quase sempre testou com duas entidades.

## Medição

```text
[ ] confirmei que o lag é do MOD (reproduzi sem ele)
[ ] distingui TPS baixo (servidor) de FPS baixo (cliente)
[ ] medi com /debug start … /debug stop
[ ] procurei o caminho QUENTE, não o código feio
[ ] escrevi os NÚMEROS: frequência, raio, população
[ ] uma mudança por vez, medindo
```

## Depois de otimizar

```text
[ ] a feature ainda funciona
[ ] o comportamento é o mesmo, só mais barato
[ ] gametest passa
[ ] o caso null (chunk não carregado) é tratado
[ ] cache invalida quando deve
[ ] os números antes/depois estão registrados
```

> Registrar os números evita que a próxima sessão "otimize" de volta o que foi
> feito de propósito.

## Relato

**Com números medidos.** "Ficou mais rápido" não é resultado; "o tick do scanner
caiu de 12 ms para 0,4 ms com 20 aldeões" é.
