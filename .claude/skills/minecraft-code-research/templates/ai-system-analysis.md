# Análise de IA — <entidade / comportamento>

> Para mobs de `Brain` e de `Goal`. Guarde em `docs/research/systems/`.

**Entidade:** `net.minecraft.entity.…`
**Sistema:** Brain / Goal / ambos — verificado com
`javap -cp "$MC_JAR" <fqn> | grep -i "brain\|goal"`
**Minecraft:** <versão> · **Mappings:** <…> · **Data:** AAAA-MM-DD

## Pergunta

<O que esta análise responde. Ex.: "por que o aldeão ignora o destino que eu
escrevo?">

---

## Se for Goal

| Goal | Prioridade | Controles | Quando começa | Quando para |
|---|---|---|---|---|
| | | MOVEMENT/LOOK/JUMP/TARGET | | |

> Prioridade sozinha não explica: o que decide convivência é **prioridade +
> controles**. Dois goals pedindo o mesmo controle não rodam juntos.

---

## Se for Brain

### Cadeia

```text
MUNDO → SENSOR → MEMORY → ACTIVITY → TASK → AÇÃO
```

### Memórias

| Memória | Tipo | Quem escreve | Quem lê | Validade | Persiste |
|---|---|---|---|---|---|
| `MemoryModuleType.…` | | sensor / task / evento | | ticks ou permanente | |

> Memória precisa estar **registrada no perfil do Brain** para ser usada.

### Sensores

| Sensor | O que observa | Frequência | Memória que escreve | Custo |
|---|---|---|---|---|

> Sensor **percebe**; Behavior **age**. Sensor com lógica de ação é anti-padrão.

### Activities

| Activity | Quando ativa | Quem escolhe | Tasks |
|---|---|---|---|
| `Activity.CORE` | sempre | — | |
| `Activity.WORK` | | Schedule | |

> Quem escolhe a Activity ativa é a `Schedule`, por meio de uma task Vanilla de
> CORE. **Uma Activity que a Schedule não conhece nunca é escolhida.**

### Tasks

| Task | Activity | Prioridade | Memórias exigidas | Início | Fim |
|---|---|---|---|---|---|

Assinaturas de `MultiTickTask` verificadas em 1.21.1:

```java
protected boolean shouldRun(ServerWorld, E);
protected void    run(ServerWorld, E, long);
protected boolean shouldKeepRunning(ServerWorld, E, long);
protected void    keepRunning(ServerWorld, E, long);
protected void    finishRunning(ServerWorld, E, long);
```

### Schedule

| Janela de tempo | Activity |
|---|---|

> Não recrie horário com `world.getTimeOfDay() % X` antes de olhar a `Schedule`.
> Um horário paralelo briga com o Vanilla.

---

## Movimento

<Como o destino é definido. Em mobs de Brain, `WALK_TARGET` — navegação direta é
sobrescrita pelo cérebro no mesmo tick.>

## Prioridades e conflito

| Comportamento | Deve vencer | Deve perder | Por quê |
|---|---|---|---|

## Onde encaixar sem quebrar Vanilla

| Opção | Degrau | Remove algo Vanilla? | Risco |
|---|---|---|---|
| Registrar memória/sensor novo | 2 | não | baixo |
| `setTaskList` acrescentando | 2–4 | não (acrescenta) | baixo |
| Activity nova | | depende da Schedule | médio |
| Mixin em `initBrain` | 10 | não, se `TAIL` sem cancelar | médio |
| Remover task Vanilla | 11 | **sim** | alto |

## Depuração

> Comece pelo **estado**, não pelo código. Quase sempre a task está correta e a
> memória que ela exige nunca foi escrita.

| Pergunta | Observado |
|---|---|
| Qual Activity está ativa? | |
| Deveria ser essa? | |
| Quais memórias estão preenchidas? | |
| Alguma está vencida ou errada? | |
| Qual sensor deveria atualizá-la? Rodou? | |
| Qual task está rodando? | |
| Qual task está bloqueando a desejada? | |
| O gate de memórias da task passa? | |

## Performance

| | |
|---|---|
| Nº de entidades | |
| Frequência de sensor | |
| Frequência de pathfinding | |
| Raio de busca | |

## Evidência

| Afirmação | Etiqueta | Fonte |
|---|---|---|

## Conclusão
