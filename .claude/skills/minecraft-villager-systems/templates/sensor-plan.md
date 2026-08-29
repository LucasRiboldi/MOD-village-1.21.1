# Plano de sensor — <nome>

> **Sensor percebe. Task age.** Sensor com lógica de ação é anti-padrão.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Propósito

<O que ele observa, em uma frase.>

## Existe um Vanilla que serve?

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.sensor.SensorType | head -20
```

**Resposta:** <…>

## Existe alternativa sem sensor?

> As duas que eliminam o sensor por completo:

```text
[ ] dá para reagir a um EVENTO em vez de perceber periodicamente?
[ ] dá para consultar o PointOfInterestStorage em vez de varrer?
```

<Se sim, prefira — sensor é custo estrutural, evento é custo zero quando nada
acontece.>

## Entrada

<O que ele examina: blocos, entidades, itens, POIs, estado do mundo.>

## Área de busca

| | |
|---|---|
| Forma | caixa / raio |
| Tamanho | |
| Justificativa | por que este e não menor |

> **Raio é cúbico.** Metade do raio é 1/8 do custo — a otimização de maior
> retorno.

## Frequência

| | |
|---|---|
| A cada | <n> ticks |
| Justificativa | por que não menos frequente |

## Saída

| Memória escrita | Tipo | Valor |
|---|---|---|

## Expiração

<Quanto dura o que ele escreveu? Se não expira, quem limpa?>

## Custo

```text
custo = área × frequência × nº de aldeões
```

| | |
|---|---|
| Custo de uma passagem | |
| Aldeões esperados | |
| **Custo por segundo** | |

> Sensor de raio 32, a cada 20 ticks, com 50 aldeões, é a causa número um de lag
> em mod de aldeão.

## Leitura de bloco

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

```text
[ ] não uso world.getBlockState em posição arbitrária
[ ] chunk null é tratado como "não sei agora"
```

> `world.getBlockState` **força carga de chunk** — de dentro de um sensor, isso é
> gerar terreno periodicamente, por aldeão.

## A regra rígida

```text
[ ] o sensor NÃO executa ação de gameplay
[ ] NÃO quebra bloco, não move o aldeão, não muda inventário
[ ] só OBSERVA e ESCREVE memória
```

> Sensor roda com frequência própria, **fora do controle da Activity** e sem o
> gate de memórias. Ação ali acontece quando não deveria.

## Registro

```text
[ ] registrado em Registries.SENSOR_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] acrescentado ao perfil do Brain do aldeão
[ ] a memória que ele escreve está registrada no perfil
```

## Teste

```text
[ ] escreve a memória quando o alvo existe
[ ] NÃO escreve quando não existe
[ ] a memória expira como planejado
[ ] com 50 aldeões, não pesa (medido)
[ ] chunk descarregado não causa erro nem carga
```
