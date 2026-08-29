# Contrato de sistema — <nome>

> Para sistemas que vão durar. Escrito **antes** do código. Guarde em
> `docs/architecture/`.

**Data:** AAAA-MM-DD · **Minecraft:** <versão>

## Purpose

<Por que este sistema existe. Uma ou duas frases, em linguagem de domínio.>

## Responsibilities

<O que ele faz.>

## Non responsibilities

> **O campo mais importante deste documento.** É ele que impede a God Class.
> Escrito no começo, resiste; escrito depois, já não descreve o código.

<O que ele explicitamente NÃO faz, e de quem é essa responsabilidade.>

## Inputs

| Entrada | De onde vem | Validação |
|---|---|---|

## Outputs

| Saída | Para onde vai |
|---|---|

## State

| Estado | Dono | Mecanismo | Sobrevive a |
|---|---|---|---|
| | | campo / NBT / PersistentState / block state | tick / chunk / save / restart |

## Lifecycle

```text
quando nasce → quando é atualizado → quando morre
```

<E em que ponto do ciclo de vida do mod cada um acontece.>

## Dependencies

| Depende de | Para quê | Acoplamento |
|---|---|---|
| | | contrato explícito / detalhe interno ← evite o segundo |

## Events

| Evento | Emite ou consome | Quando |
|---|---|---|

## Persistence

<O que sobrevive, em qual arquivo, escrito por quem, lido por quem.>

<E o que **deliberadamente não** é persistido, com o motivo — senão alguém
"conserta" depois.>

## Networking

<O que atravessa a fronteira. — não se aplica: <por quê>>

## Extension points

<Como outra parte do mod (ou outro mod) se conecta a este sistema sem tocar nele
por dentro.>

## Invariants

> Documentadas, **validadas** e **protegidas no código** — as três. Documentada e
> não protegida é comentário; protegida e não documentada é comportamento
> misterioso.

| Invariante | Onde é protegida | O que acontece se violada |
|---|---|---|
| <ex.: "uma entidade não pode ter dois trabalhos simultâneos"> | | |
| <ex.: "apenas o servidor modifica o estado"> | | |
| <ex.: "toda reserva tem exatamente um dono vivo"> | | |

## Failure cases

| Situação | Comportamento esperado |
|---|---|
| alvo sumiu | |
| chunk descarregado | |
| recurso indisponível | |
| entidade morreu ou foi convertida | |
| servidor reiniciou no meio | |
| dois atores disputam o mesmo recurso | |

> Toda ação precisa de **timeout, condição de desistência e o que fazer depois**.

## Concorrência

<Vários atores mexem no mesmo estado? Quem tem autoridade? Como o conflito é
resolvido? — não se aplica.>

## Performance

| Operação | Frequência | Custo | Multiplicado por |
|---|---|---|---|

## Compatibilidade

**Classificação:** LOW / MEDIUM / HIGH

## Decisões relacionadas

| ADR | Assunto |
|---|---|
