# Caminho e movimento

O erro mais comum do domínio, e o mais fácil de corrigir depois de entendido.

## Quem manda no caminho

```java
// ✗ o aldeão anda dois blocos e volta
villager.getNavigation().startMovingTo(x, y, z, 0.5f);

// ✓
brain.remember(MemoryModuleType.WALK_TARGET,
        new WalkTarget(new BlockPosLookTarget(destino), velocidade, alcance));
```

`[FATO]` MC 1.21.1: em mobs de Brain, o destino é a memória `WALK_TARGET`. As
tasks Vanilla de movimento só agem quando ela está posta, e o cérebro **reescreve
o destino no mesmo tick** seguindo a agenda dele. Navegação direta é sobrescrita.

## Manter, não só escrever

Escrever a memória uma vez não basta: outra task pode limpá-la, ou ela expira.

```java
@Override
protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
    return temDestino(villager);
}

@Override
protected void keepRunning(ServerWorld world, VillagerEntity villager, long time) {
    repor(villager);        // mantém WALK_TARGET enquanto o destino valer
}
```

**É a task que repõe a memória que segura o aldeão no caminho** — sem cancelar
nada e sem remover task alguma.

## `WalkTarget`

```java
new WalkTarget(lookTarget, velocidade, alcanceDeConclusao)
```

| Parâmetro | Cuidado |
|---|---|
| destino | `BlockPosLookTarget`, `EntityLookTarget`, … |
| velocidade | `0.5f` é passo de trabalho; valores altos parecem fuga |
| alcance | pequeno demais = ele nunca "chega"; grande = para longe |

O alcance é o que mais gera bug silencioso: com alcance 0, o aldeão pode nunca
satisfazer a condição de chegada e ficar oscilando ao lado do destino.

## Desistir

`[FATO]` o Vanilla tem `MemoryModuleType<Long> CANT_REACH_WALK_TARGET_SINCE`.

Ele existe porque **o próprio jogo assume que retry infinito é bug**. Use o mesmo
princípio:

```text
[ ] limite de tentativas
[ ] timeout (MAX_RUN_TIME na task)
[ ] o que fazer ao desistir: esquecer o alvo, liberar reserva, escolher outro
```

Sem isso, o aldeão fica preso tentando alcançar o inalcançável — gastando o
cálculo mais caro do jogo, para sempre, e parecendo quebrado.

## Alcançabilidade

Nem todo destino é alcançável:

```text
bloco cercado         · buraco sem saída   · outro lado da água
chunk descarregado    · altura demais      · porta fechada por outro mod
```

E descobrir que **não** é alcançável custa quase o mesmo que achar o caminho.

```text
[ ] o destino é alcançável a pé?
[ ] o destino está em chunk carregado?
[ ] existe verificação barata antes do cálculo caro?
```

## Custo

Pathfinding está entre os cálculos mais caros do jogo.

```text
✗  recalcular por tick
✓  recalcular quando o destino MUDA ou o caminho FALHA
```

Com vinte aldeões recalculando por tick, o TPS cai — e o autor testou com dois.

Ver `references/villager-performance.md`.

## Chunk

```text
[ ] o destino está em chunk carregado?
[ ] se descarregar no meio do caminho, o que acontece?
```

Leitura de bloco pelo caminho deve usar chunk carregado:

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

`world.getBlockState` **força carga do chunk** — do tick, isso é gerar terreno
dentro do laço.

## Olhar

```text
LOOK_TARGET   para onde ele olha
WALK_TARGET   para onde ele anda
```

São memórias separadas. Um aldeão que anda sem olhar parece errado; olhar sem
andar é o normal quando ele está parado trabalhando.

## Velocidade e aparência

Velocidade alta faz o aldeão parecer em pânico. Se o comportamento é trabalho,
use um passo de trabalho — a coerência visual faz parte da feature.

## Checklist

```text
[ ] uso WALK_TARGET, não getNavigation().startMovingTo
[ ] a memória é MANTIDA enquanto o destino valer
[ ] o alcance de conclusão é sensato
[ ] há limite de tentativas e timeout
[ ] há comportamento definido ao desistir
[ ] alvo inalcançável é abandonado
[ ] não recalculo por tick
[ ] chunk descarregado é caso normal
[ ] leitura de bloco não força carga
[ ] a velocidade combina com o comportamento
[ ] LOOK_TARGET considerado
```
