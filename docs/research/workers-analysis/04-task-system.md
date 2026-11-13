# 04 — Sistema de tarefas

---

## 1. A conclusão, primeiro

**O Workers não tem sistema de tarefas.**

Não há classe `Task`, nem fila, nem prioridade de tarefa, nem estado de
tarefa, nem cancelamento, nem timeout. Verificado por inventário completo
dos 144 arquivos: nenhuma classe com esse papel existe.

O que existe no lugar é: **cada trabalhador procura o próprio trabalho, e
o progresso dele é o estado do Goal dele.**

Isto é o oposto exato do Village Colony, cuja `Simulation-Loop.md` diz —
e cujo `core/task/model/Task.java:13-15` cita:

> a colônia nunca executa, só cria tarefas; o aldeão nunca cria, só
> executa.

No Workers, o aldeão cria e executa; ninguém coordena.

---

## 2. As perguntas do briefing, respondidas pelo código

| Pergunta | Resposta no Workers |
|---|---|
| Como uma tarefa nasce? | Não nasce. O `canUse()` do Goal fica verdadeiro. |
| Quem cria? | Ninguém. |
| Quem seleciona? | O próprio trabalhador, em `getAvailableWorkAreasByPriority`. |
| Quem atribui? | Ninguém. É auto-serviço. |
| Como sabe o que fazer? | Pela **classe** dele. `LumberjackEntity` tem `LumberjackWorkGoal`. |
| Como sabe onde? | Varre entidades `LumberArea` num raio de 64 do próprio corpo. |
| Como sabe que terminou? | A `Stack` esvaziou → estado `DONE`. |
| Como sabe que falhou? | Estados `ERROR_*` nomeados (11 deles). |
| Existe prioridade? | Da **área**, não da tarefa. Soma de três parcelas. |
| Existe timeout? | **Não.** |
| Existe cooldown? | Ad-hoc, `int` por Goal. |
| Existe estado? | Sim: `enum State` dentro do Goal, **não persistido**. |
| Existe concorrência? | Sim, e não é controlada. Ver `03 §5`. |
| Existe reserva? | `boolean isBeingWorkedOn` na área — frouxa. |
| Como evita duplo trabalho? | `stackOfTrees.pop()` remove o alvo da pilha compartilhada. |
| Como se cancela? | Não se cancela. O `canUse()` deixa de valer. |

---

## 3. O que faz as vezes de tarefa: as três pilhas

O mais próximo de uma "tarefa" no Workers é a `Stack<BlockPos>` guardada
na área de trabalho:

```java
// world/Tree.java:8-10
private final Stack<BlockPos> stackToShear;
private final Stack<BlockPos> stackToStrip;
private final Stack<BlockPos> stackToBreak;

// entities/workarea/LumberArea.java
public Stack<Tree>     stackOfTrees;
public Stack<BlockPos> stackToPlant;
public Stack<BlockPos> stackToBoneMeal;

// entities/workarea/BuildArea.java
public Stack<BuildBlock> stackToPlace;
public Stack<BuildBlock> stackToPlaceMultiBlock;
public Stack<BlockPos>   stackToBreak;
public Stack<BlockPos>   stackToFree;
```

O padrão de consumo é sempre o mesmo, e é honesto:

```java
// LumberjackWorkGoal.java:307-333
public boolean breakBlocks(Stack<BlockPos> positions){
    if(blockPos == null){
        if(!positions.isEmpty()) blockPos = positions.pop();
        return blockPos != null;
    }
    if (level.getBlockState(blockPos).isAir()) {
        blockPos = positions.isEmpty() ? null : positions.pop();
    } else {
        this.worker.mineBlock(blockPos);
    }
    return true;
}
```

Retorna `true` enquanto há trabalho, `false` quando acabou. O chamador
usa isso como condição de troca de estado. É um iterador com efeito
colateral, e serve.

**O que ele não faz:** a pilha é `transient`. Não vai para NBT. Ver `08`.

---

## 4. O que isso custa

### 4.1 Nada sobrevive ao reload

`stackOfTrees` é reconstruída por `scanForTrees()` toda vez. O
trabalhador que estava no meio de derrubar uma árvore, ao recarregar o
chunk, recomeça a varredura da área inteira. Não perde item — perde
tempo, e refaz um flood fill caro.

### 4.2 A colônia não pode priorizar nada

Não há onde dizer "esta obra é mais urgente que aquela coleta". A
prioridade existe entre áreas do mesmo tipo, para um trabalhador. Um mod
que precisasse de "pare de cortar madeira, a muralha está caindo" não tem
onde escrever isso.

Para o Workers isso está **certo**: quem prioriza é o jogador, plantando
e removendo áreas. Para o Village Colony estaria errado, porque a colônia
é que decide.

### 4.3 Não dá para testar

Não existe nada exercitável sem `ServerLevel`, sem entidade e sem mundo.
O mod tem zero testes, e a arquitetura explica por quê: não há uma
unidade sem Minecraft para testar.

O seu `DependencyRuleTest` + os 366 testes de unidade sobre `core/` são a
resposta oposta a esse mesmo problema, e a análise do Workers **confirma
que a resposta foi certa**.

---

## 5. Comparação lado a lado com o seu

| | Village Colony | Workers |
|---|---|---|
| A tarefa é | objeto `Task` (`core/task/model/Task.java`) | um `enum State` num Goal |
| Estados | `AVAILABLE → RESERVED → EXECUTING → COMPLETED` (+ `CANCELLED`) | 4 a 17, por profissão |
| Transição inválida | `IllegalStateException` explícita (`Task.require`) | não existe |
| Executor | `UUID executorId`, com `reserveFor` que recusa duplo | `worker.currentXArea`, campo público |
| Devolução | `release()` — volta a `AVAILABLE`, mantém o pedido | não existe |
| Cancelamento | `cancel()`, e `COMPLETED` não pode ser cancelada | não existe |
| Quem cria | `ColonyCycle.requestMissing` | ninguém |
| Quem casa com trabalhador | `WorkAssignment.assign` | ninguém |
| Prioridade | `TaskPriority`, no objeto | `int` calculado sobre a área |
| Persistência | `ColonySavedData` (colônias, obras, construções) | nenhuma |
| Testável | sim, 366 testes | não |

**A sua arquitetura é estritamente melhor neste eixo.** Não é opinião: o
`Task.reserveFor` recusa reserva dupla lançando exceção
(`Task.java:150-157`), e o Workers não tem onde escrever essa regra.

E vale lembrar o que o seu §11 registra: `Task.complete` numa tarefa
`RESERVED` **derrubou o servidor** — porque a máquina de estados é
explícita e reclama. No Workers esse mesmo erro seria um trabalhador
parado em silêncio, para sempre. Exceção que derruba o mundo é ruim;
estado inconsistente e silencioso é pior, e mais caro de achar.

---

## 6. O que o Workers ensina sobre tarefas, apesar de não ter tarefas

Três coisas, e todas são sobre a **fronteira** entre a tarefa e o mundo:

### (a) A tarefa precisa ter um relógio próprio, separado do ciclo

O `LumberjackWorkGoal.tick()` roda a máquina a cada 10 ticks, mas trata
`WOOD_CUTTING` **antes** do gate, todo tick (linhas 81-88). Você chegou
na mesma separação por outro caminho — `LumberjackWork.run` no ciclo
longo, `LumberjackWork.tick` no tick. **Convergência independente é o
sinal mais forte que uma análise dessas pode dar.**

### (b) A tarefa precisa saber declarar o que lhe falta, sem morrer

O `addNeededItem` é o mecanismo que impede o deadlock: em vez de o Goal
falhar por não ter machado, ele **registra a falta** e outro Goal atende.
O seu `Task` não tem esse eixo — hoje um `LumberjackWork` sem machado
não tem como pedir. Como você entrega a ferramenta na atribuição
(`WorkerEquipment`, item C), o problema não apareceu; ele aparece no dia
em que houver consumo de ferramenta ou tarefa que exija material.

**Recomendação concreta:** ver `12-recommendations.md §3.2`.

### (c) O motivo de não trabalhar precisa ser um valor, não um silêncio

Onze `ERROR_*` nomeados. Você pagou três sessões de jogo pelo E14 por não
ter isso na Fase 10. Já corrigiu ali; falta generalizar.
