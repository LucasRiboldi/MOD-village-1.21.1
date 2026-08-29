# Trabalho, descanso e horário

O aldeão não trabalha o tempo todo. Quem decide **quando** é a `Schedule`, e
brigar com ela é a fonte de metade dos "meu aldeão não faz nada".

## O ciclo do dia

```text
TEMPO DO MUNDO → SCHEDULE → ACTIVITY → conjunto de TASKS
```

`[FATO]` MC 1.21.1 — `net.minecraft.entity.ai.brain.Schedule` traz `EMPTY`,
`SIMPLE`, `VILLAGER_BABY`, `VILLAGER_DEFAULT`, a constante `WORK_TIME` e um
`ScheduleBuilder`.

O dia de um aldeão adulto, em linhas gerais:

```text
acordar → MEET (sino) → WORK → IDLE → WORK → IDLE → REST (cama)
```

E, sobrepondo-se a tudo, `PANIC`, `PRE_RAID`, `RAID` e `HIDE` — que **devem**
ganhar do seu trabalho.

## A regra que evita a briga

```java
// ✗ um horário paralelo, que o Vanilla não conhece
if (world.getTimeOfDay() % 24000 > 2000 && world.getTimeOfDay() % 24000 < 9000) {
    trabalhar();
}

// ✓ consulte a Activity vigente, ou a Schedule
```

Recriar o horário faz o aldeão trocar de modo no meio da sua lógica: a Schedule
manda ele dormir, e a sua task continua achando que é hora de trabalhar. O
resultado é um aldeão oscilando entre a cama e o trabalho.

## O ciclo do trabalho

```text
Villager
  ↓ Schedule diz: é hora de trabalhar
Work Activity
  ↓ a task procura o que fazer
JOB SITE (memória JOB_SITE, um GlobalPos)
  ↓ WALK_TARGET leva até lá
Comportamento
  ↓ ação
Resultado
  ↓ cooldown
```

## As perguntas antes de criar um sistema de trabalho

```text
Quando COMEÇA?         qual condição, além do horário
Quando TERMINA?        conclusão, ou desistência
Qual POI?              onde ele trabalha
Qual DISTÂNCIA?        quão longe pode ir
Qual COMPORTAMENTO?    o que ele faz lá
Qual RECURSO consome?
Qual RESULTADO produz?
Quanto TEMPO leva?
Pode FALHAR?           quase sempre sim
E DEPOIS da falha?     ← a pergunta mais esquecida
```

A última é o que separa um sistema robusto de um aldeão travado olhando para o
nada.

## Trabalhar só no horário certo

Duas formas, e a segunda é quase sempre melhor:

**A — Activity própria.** Registrar `meumod:trabalho` e esperar que ative.
`[FATO]` **não funciona:** quem escolhe a Activity é a Schedule, e ela não conhece
a sua. Ver `references/brain-system.md`.

**B — task em CORE com as condições dentro.**

```java
@Override
protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
    return ehHorarioDeTrabalho(world, villager)   // a condição que a Activity daria
        && temTrabalho(villager);
}
```

Mesmo resultado em jogo, pelo caminho mais barato e de menos conflito.

## Prioridade em relação ao Vanilla

Sua task de trabalho deve **perder** para:

```text
PANIC     ele está sendo atacado
RAID      há uma incursão
REST      é hora de dormir
```

Em `Activity.CORE`, isso se consegue ficando **depois** das tasks Vanilla na
ordem — água, portas, pânico, acordar, sino, incursão. Ficar depois é
deliberado, não descuido.

E as suas condições de `shouldRun` devem incluir "não estou em pânico".

## Descanso e encontro

```text
REST   → memória HOME (GlobalPos)          → a cama
MEET   → memória MEETING_POINT (GlobalPos) → o sino
```

Se o seu sistema move o aldeão para longe, ele pode não conseguir voltar para
dormir. Isso não é bug do Vanilla: é consequência do alcance que você escolheu.

```text
[ ] o raio de trabalho permite voltar para casa antes da noite?
[ ] o que acontece se ele estiver longe quando a Schedule mudar para REST?
```

## Cooldown

Trabalho sem cooldown é trabalho por tick. Todo ciclo precisa de:

```text
[ ] tempo mínimo entre execuções
[ ] tempo máximo da task (MAX_RUN_TIME)
[ ] limite de tentativas antes de desistir
```

`[FATO]` o Vanilla tem `CANT_REACH_WALK_TARGET_SINCE` justamente para desistir de
destinos inalcançáveis. É o modelo a copiar: **o próprio jogo assume que retry
infinito é bug.**

## Falha

```text
alvo sumiu                  → esquecer, escolher outro
caminho não encontrado      → desistir após N tentativas
recurso indisponível        → esperar, com cooldown
POI ocupado por outro       → liberar a intenção
chunk descarregado          → pular este ciclo (caso NORMAL, não erro)
inventário cheio            → depositar antes de continuar
anoiteceu                   → deixar a Schedule assumir
inimigo por perto           → deixar PANIC assumir
```

As duas últimas são as que mais faltam: um sistema de trabalho que não cede lugar
ao ciclo Vanilla faz o aldeão trabalhar de madrugada durante um ataque de zumbis.

## Máquina de estados

Para trabalho com etapas, modele explicitamente:

```text
IDLE → SEARCHING → TRAVELING → WORKING → RETURNING → DEPOSITING → IDLE
```

Cada transição precisa de **condição, ação, falha e timeout**. Estados
impossíveis (viajando sem destino, depositando sem carga) devem ser
inalcançáveis por construção, não evitados por convenção.

Ver `references/resource-gathering.md`.

## Checklist

```text
[ ] a task consulta a Schedule/Activity, não recria o horário
[ ] perde para PANIC, RAID e REST
[ ] fica depois das tasks Vanilla de CORE
[ ] tem cooldown e MAX_RUN_TIME
[ ] tem limite de tentativas
[ ] todos os casos de falha têm comportamento definido
[ ] chunk descarregado é caso normal
[ ] o raio permite voltar para casa
[ ] testado: aldeão dorme, come e socializa normalmente
```

O último é o teste que separa "funciona" de "funciona sem quebrar o Vanilla".
