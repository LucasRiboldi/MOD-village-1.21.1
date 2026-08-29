# Contrato de sistema de aldeão — <nome>

> Para sistemas que vão durar: colônia, coleta de recursos, automação por
> aldeões. Escrito **antes** do código. Guarde em `docs/architecture/`.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Purpose

<Por que este sistema existe. Em linguagem de domínio, sem citar `BlockPos`.>

## Responsibilities

## Non responsibilities

> **O campo que impede a God Class.** Escrito no começo, resiste; escrito depois,
> já não descreve o código.

<O que ele explicitamente NÃO faz, e de quem é.>

## Escopo

```text
[ ] individual (por aldeão)
[ ] por POI
[ ] por VILA / colônia
[ ] por mundo
```

<Escolher errado produz: dois aldeões fazendo a mesma coisa, ou um bloqueando
todos.>

## Atores

| Ator | Papel |
|---|---|
| aldeão | |
| POI | |
| vila / colônia | |
| jogador | |

## Estado

| Estado | Dono | Mecanismo | Persiste | Escopo |
|---|---|---|---|---|
| | aldeão / vila / mundo | memória · NBT · PersistentState | | |

**O que deliberadamente NÃO persiste, e por quê:**

<Existe no mundo → pergunte ao mundo. Registre, senão alguém "conserta" depois.>

## Percepção

| O que precisa ser notado | Sensor | Frequência |
|---|---|---|

## Decisão

<Quem decide o quê. Activity, gate de memórias, prioridade.>

## Ação

| Task | Faz | Activity | Prioridade |
|---|---|---|---|

## Lifecycle

```text
quando o sistema começa → como opera → quando para
```

<E o que acontece no `SERVER_STARTED` e no `SERVER_STOPPING`.>

## Dependencies

| Depende de | Para quê |
|---|---|

## Events

| Evento | Consome ou emite | Quando |
|---|---|---|
| `AFTER_DEATH` | | |
| `MOB_CONVERSION` | | ← o caso mais comum |

## Persistence

<O que sobrevive, em qual arquivo. Dados relacionados por id **no mesmo
arquivo** — não há transação entre arquivos.>

## Networking

<— não se aplica: toda a IA é server-side.>

## Extension points

<Como outra parte do mod se conecta sem tocar neste sistema por dentro.>

## Invariants

> Documentadas, **validadas** e **protegidas no código** — as três.

| Invariante | Onde é protegida | Se violada |
|---|---|---|
| "um aldeão não pode ter dois trabalhos simultâneos" | | |
| "apenas o servidor modifica o estado" | | |
| "toda reserva tem exatamente um dono vivo" | | |
| "uma tarefa não executa sem recursos" | | |

## Coordenação entre aldeões

```text
AUTORIDADE     quem decide
PROPRIEDADE    de quem é o recurso agora
SINCRONIZAÇÃO  quando o estado compartilhado é lido/escrito
CONFLITO       o que acontece quando dois querem o mesmo
```

**Reserva:**

```text
[ ] tem dono (UUID)
[ ] tem validade
[ ] liberada ao concluir
[ ] liberada na morte E na conversão
[ ] liberada se o alvo sumir
```

> Sem expiração, uma reserva órfã bloqueia o recurso para sempre — e o sintoma é
> "os aldeões pararam", horas depois, sem causa aparente.

## Failure cases

| Situação | Comportamento |
|---|---|
| alvo sumiu | |
| caminho não encontrado | |
| POI indisponível | |
| recurso indisponível | |
| chunk descarregado | **caso normal** |
| aldeão morreu / convertido | |
| jogador interrompeu | |
| anoiteceu / raid | ceder ao Vanilla |
| servidor reiniciou | |

```text
[ ] toda ação tem TIMEOUT
[ ] toda ação tem condição de DESISTÊNCIA
[ ] falha em um aldeão NÃO interrompe os outros
```

## Performance

| Operação | Frequência | Custo | × aldeões |
|---|---|---|---|

```text
[ ] escalonado entre aldeões
[ ] raios justificados
[ ] testado com 1, 10, 50, 100
```

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH

```text
[ ] quantos Mixins em VillagerEntity
[ ] alguma task Vanilla removida
[ ] a Schedule foi alterada
```

## Decisões relacionadas

| ADR | Assunto |
|---|---|
