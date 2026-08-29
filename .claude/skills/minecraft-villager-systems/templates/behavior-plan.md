# Plano de comportamento — <nome da task>

> Uma por task. Preencha antes de escrever.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Propósito

<O que o aldeão faz, em uma frase, em linguagem de jogo.>

## Precondições

<O que precisa ser verdade para ele começar.>

## Memórias exigidas

> Use o **gate do construtor**, não checagem em `shouldRun` — é mais barato e
> fica visível.

```java
super(Map.of(MemoryModuleType.X, MemoryModuleState.VALUE_PRESENT), MIN, MAX);
```

| Memória | Estado exigido | Vanilla ou minha |
|---|---|---|
| | `VALUE_PRESENT` / `VALUE_ABSENT` / `REGISTERED` | |

## Memórias lidas e escritas

| Memória | Lê | Escreve | Quando limpa |
|---|---|---|---|

## Sensores dos quais depende

| Sensor | Escreve qual memória | Frequência |
|---|---|---|

## Activity

**Onde vive:** `CORE` / outra

<Se `CORE`, quais condições a task carrega que a Activity daria?>

> `[FATO]` Quem escolhe a Activity é a `Schedule`. Activity que ela não conhece
> nunca é escolhida — ver `workflows/add-activity.md`.

## Prioridade

**Valor:** <n>

**Deve perder para:** `PANIC` · `PRE_RAID` · `RAID` · <tasks Vanilla de CORE>

**Deve ganhar de:** <…>

> Prioridade é ordem **dentro** da Activity, não importância global. Ficar depois
> das tasks Vanilla de CORE costuma ser o correto.

## Ação

<O que ela faz em `run` e `keepRunning`.>

**Movimento:** `WALK_TARGET` mantido? alcance? velocidade?

```text
[ ] uso WALK_TARGET, não getNavigation().startMovingTo
[ ] a memória é MANTIDA em keepRunning enquanto o destino valer
```

## Conclusão

<Quando ela termina com sucesso, e o que limpa.>

## Interrupção

<O que a faz parar antes: `shouldKeepRunning` false, horário, ameaça.>

## Timeout

**MAX_RUN_TIME:** <n> ticks

## Falha

| Situação | Comportamento |
|---|---|
| alvo sumiu | |
| caminho não encontrado | |
| chunk descarregado | pular — caso **normal** |
| recurso indisponível | |
| anoiteceu | ceder à Schedule |
| inimigo perto | ceder a PANIC |

```text
[ ] limite de tentativas
[ ] o que fazer ao desistir (esquecer alvo, liberar reserva)
```

> **Retry infinito é bug.** `[FATO]` o Vanilla tem
> `CANT_REACH_WALK_TARGET_SINCE` justamente por isso.

## Side effects

<O que muda no mundo: bloco, inventário, memória de outro aldeão, reserva.>

## Performance

| | |
|---|---|
| Com que frequência `shouldRun` é avaliado | |
| Custo de uma execução | |
| Multiplicado por | nº de aldeões |
| Há busca? Qual raio? | |
| Há pathfinding? | |
| Escalonado? | |

## Persistência

<O estado desta task sobrevive ao save? Se sim, onde? Se não, é intencional?>

## Teste

```text
[ ] roda quando deveria
[ ] NÃO roda quando não deveria
[ ] termina corretamente
[ ] o timeout funciona
[ ] cada caso de falha se comporta como planejado
[ ] os comportamentos Vanilla continuam
[ ] cede em PANIC e RAID
[ ] com 10 aldeões, o TPS aguenta
```
