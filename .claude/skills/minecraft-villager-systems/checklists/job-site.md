# Checklist — local de trabalho (POI)

## Precisa mesmo?

```text
[ ] existe um POI Vanilla que serve?
[ ] o bloco precisa ser local de TRABALHO, ou basta interação?
[ ] exige profissão nova, ou serve a uma existente?
```

## O record

```bash
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
```

`[FATO]` MC 1.21.1: `PointOfInterestType(Set<BlockState>, int ticketCount, int searchDistance)`

```text
[ ] confirmei a forma na minha versão
```

## Block states — a armadilha

```text
[ ] TODOS os block states do bloco estão incluídos
[ ] se algum foi excluído de propósito, o motivo está escrito
```

```java
// ✗ ele perde o local quando o bloco muda de estado
Set.of(ModBlocks.FORJA.getDefaultState())

// ✓
ImmutableSet.copyOf(ModBlocks.FORJA.getStateManager().getStates())
```

## ticketCount

```text
[ ] 1 se o local é exclusivo
[ ] se >1, sei quantos aldeões devem compartilhar
```

## searchDistance

```text
[ ] o menor que resolve
[ ] o aldeão consegue ir até lá no horário de trabalho
[ ] e voltar para dormir
```

> Grande demais: ele reivindica de longe, nunca chega, e a busca fica cara.

## Registro

```text
[ ] bloco registrado ANTES
[ ] Registries.POINT_OF_INTEREST_TYPE
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
```

## A profissão

```text
[ ] heldWorkstation casa com este POI
[ ] acquirableWorkstation casa com este POI
```

## Ciclo de vida

```text
[ ] bloco destruído       → POI some, o aldeão perde JOB_SITE
[ ] aldeão morre          → ticket liberado (AFTER_DEATH)
[ ] aldeão convertido     → ticket liberado (MOB_CONVERSION)
[ ] muda de profissão     → comportamento definido
[ ] chunk descarrega      → POI permanece; o aldeão não age
[ ] outro já reivindicou  → ticketCount esgotado
[ ] bloco muda de estado  → continua sendo POI
```

## Memórias

```text
[ ] JOB_SITE é GlobalPos, não BlockPos
[ ] POTENTIAL_JOB_SITE considerado (o Vanilla separa "achei" de "é meu")
```

## Busca

```text
[ ] uso o PointOfInterestStorage em vez de varrer blocos
```

> Varrer 64³ são milhões de leituras; o índice do jogo já existe.

## Resources do bloco

```text
[ ] lang            [ ] blockstate      [ ] modelo de bloco
[ ] modelo de item  [ ] textura         [ ] loot table
[ ] tags de mineração                   [ ] item group
```

## Teste da reivindicação — faça primeiro

> **Antes** de escrever comportamento. Sem lógica no caminho, o diagnóstico é
> direto.

```text
[ ] colocar o bloco perto de um aldeão SEM profissão
[ ] ele adquire a profissão
[ ] só UM adquire
[ ] quebrar o bloco → ele perde (se nível 1)
[ ] recolocar → outro pode adquirir
```

## Teste completo

```text
[ ] o ciclo de vida inteiro
[ ] ele vai até o local no horário de trabalho
[ ] os resources aparecem
[ ] fechar e reabrir mantém a reivindicação
[ ] 20 aldeões + 20 blocos: a distribuição faz sentido
[ ] servidor dedicado
```

## Sintomas

```text
ignora o bloco          → POI ou block state faltando
adota e larga           → predicado da profissão não casa
dois no mesmo bloco     → ticketCount > 1
nunca chega             → searchDistance grande demais
perde ao virar o bloco  → só um block state registrado
```
