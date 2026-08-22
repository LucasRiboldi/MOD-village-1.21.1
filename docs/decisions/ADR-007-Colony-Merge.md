# ADR-007-Colony-Merge.md

# Architecture Decision Record 007

# Village Colony — Fusão de colônias

**Status:** Accepted
**Date:** 2026-08-21
**Accepted:** 2026-08-21
**Decision Type:** Architecture / Data
**Implements:** TASK-044, B3 do Backlog
**Amends:** ADR-003 §5

---

# 1. Context

A ADR-003 §5 previu a fusão e não disse como fazê-la. O autor enunciou a
regra em 2026-08-12 e a confirmou em 2026-08-15:

```text
Duas vilas viram uma quando um bloco de uma encostar no bloco
da outra — quando a construção as junta. Não é distância.

A fusão não reduz trabalhadores: a vila resultante fica com os
de ambas.
```

Isso decide **o quê**. Faltavam três perguntas que só aparecem na hora de
escrever, e o autor as respondeu em 2026-08-21.

---

# 2. Decision

## 2.1 Qual UUID sobrevive: o da maior

```text
vence a colônia de maior observedBeds

empate → a de id lexicograficamente menor,
         para que a escolha não dependa da ordem do mapa
```

**Por que a maior.** O UUID nomeia o dono de tudo o que está em disco —
trabalhadores, baús, construções, a mina. Fundir é reescrever esses
vínculos, e reescrever menos é errar menos. A colônia maior é a que tem
mais a perder e menos a reescrever.

**O que isso custa:** a vila pequena perde o id. Nada no jogo mostra o
id ao jogador, então o custo é de save e não de experiência.

## 2.2 O teto de profissão é violado, e permanece violado

```text
MAX_PER_PROFESSION vale na CONTRATAÇÃO, não na fusão
```

Duas vilas com dois lenhadores cada dão **quatro** lenhadores, e os
quatro ficam. É a regra do autor dita por inteiro: "a fusão não reduz
trabalhadores".

**O teto continua valendo para o que vier depois.** Com quatro lenhadores
e teto dois, nenhuma vaga de lenhador abre — `ProfessionAssigner` só
preenche vaga, e não há vaga a preencher. A colônia volta ao teto
naturalmente, à medida que os aldeões morrem, e sem ninguém ser demitido.

**A alternativa recusada:** demitir o excedente. Ela contradiz a regra do
autor e produz o pior momento possível de jogo — a vila cresce e
trabalhadores param de trabalhar.

## 2.3 O centro da resultante

```text
o centro da colônia que sobreviveu, sem recalcular
```

E ele volta a se mover pela sonda, como manda a **Emenda 4 da ADR-003**.
Recalcular aqui seria inventar um centro que nenhuma observação viu — e
foi exatamente isso que a Emenda 2 proibiu.

A sonda da colônia resultante parte do centro herdado, enxerga a vila
maior no ciclo seguinte, e o centro converge sozinho.

---

# 3. O que a fusão move

```text
trabalhadores    colonyId reescrito para o sobrevivente
baús             o mesmo
construções      o mesmo
mina             a colônia absorvida tem a sua, e ela É MANTIDA
tarefas          reapontadas; nenhuma é cancelada
estado           ACTIVE se qualquer uma das duas era ACTIVE
observedBeds     o do sobrevivente
```

**Duas minas numa colônia só** é o único caso em que o modelo atual não
tem resposta pronta: `MineRegistry` guarda uma mina por colônia. A
decisão é **manter a do sobrevivente e esquecer a outra** — a escada da
vila absorvida continua no mundo, aberta, e ninguém a continua. É o mesmo
lado seguro do erro que o resto do mod escolhe: nada é destruído, só
deixa de ser trabalhado.

---

# 4. O que dispara

Não é distância — é encosto de bloco, e quem sabe disso é a construção.
`BuildingRegistry` já responde `isColonyInfrastructure(pos)`: quando uma
obra terminada de A encostar numa construção de B, as duas viram uma.

**Enquanto a construção não rodar em jogo, nada disto dispara.** É por
isso que esta ADR não bloqueia o MVP: ela existe para que a
implementação, quando vier, não pare de novo nas três perguntas.

---

# 5. Consequences

**Ganha:** a promessa da ADR-003 §5 deixa de ser um aviso no log.

**Perde:** o teto de profissão passa a ser um teto de contratação, e não
um invariante. Todo código que assumir "no máximo dois lenhadores" está
errado a partir daqui — e o `ProfessionAssignerTest` precisa dizer isso.

**Não decide:** a divisão. Uma vila que se parta em duas continua sendo
uma colônia só, e isso continua sendo o §5 da ADR-003 por escrever.
