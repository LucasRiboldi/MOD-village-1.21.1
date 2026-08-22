# ADR-009-Autonomous-Village-Evolution.md

# Architecture Decision Record 009

# Village Colony — Evolução autônoma da vila por bioma e recursos

**Status:** Accepted
**Date:** 2026-08-22
**Accepted:** 2026-08-22
**Decision Type:** Architecture — direção do projeto
**Supersedes:** a fila de prioridades anterior
**Amends:** ADR-001 (princípios), e a leitura de `ResourceGroup`

---

# 1. O princípio

```text
O bioma define o contexto.
Os recursos disponíveis definem as possibilidades.
As necessidades da vila definem as prioridades.
Os trabalhadores executam o plano.
```

Uma vila de deserto não é uma vila de planície com outra textura. Ela
tem economia própria.

O mod deve deixar de ser

```text
BIOMA → ESCOLHER ESTRUTURA
```

e passar a ser

```text
BIOMA + TERRITÓRIO + RECURSOS + PRODUÇÃO + POPULAÇÃO
     + NECESSIDADES + CAPACIDADE + RISCOS
                    ↓
            DECISÃO AUTÔNOMA
                    ↓
           EVOLUÇÃO DA VILA
```

**Um motor universal alimentado por perfis** — não uma árvore de
evolução escrita à mão por bioma. Bioma novo entra como perfil, e não
como dezenas de exceções.

---

# 2. A regra de ouro

Toda funcionalidade nova responde a uma pergunta:

> Isto funciona para **uma** vila, ou para **qualquer** vila com um
> contexto diferente?

Uma solução da forma

```text
if (desert) { ... }
```

deve ser questionada. A forma preferida é

```text
VillageProfile → BiomeRules → ResourceRules
               → ProductionRules → ConstructionRules
```

---

# 3. A arquitetura de destino

```text
              VILLAGE PROFILE
                     │
            BIOME + TERRITORY
                     │
            RESOURCE INVENTORY
                     │
            PRODUCTION SYSTEM
                     │
             VILLAGE PLANNER
                     │
      ┌──────────────┼──────────────┐
   WORKERS       BUILDINGS      EXPANSION
      └──────────────┼──────────────┘
                     │
             VILLAGE EVOLUTION
```

## 3.1 O perfil da vila

O bioma deixa de ser um `if` espalhado e vira característica
consultável: preferências de material, recursos disponíveis, escassez,
opções de produção, estilo, comida, prioridade de trabalhador,
estratégia de expansão e limites de sobrevivência.

## 3.2 O inventário do território

Saber "estou no deserto" não basta. É preciso saber **"estou no deserto
e tenho acesso a"** — com quantidade, distância e risco. Duas vilas do
mesmo bioma podem evoluir diferente: a que tem água perto vira agrícola,
a que não tem prioriza infraestrutura hídrica.

## 3.3 Três categorias de recurso

```text
DISPONÍVEL     o que a vila obtém direto
PRODUZÍVEL     o que ela faz a partir de outro
IMPOSSÍVEL     o que ela não consegue razoavelmente
```

O impossível **não vira espera infinita**: vira `BLOCKED_REQUIREMENT`, e
o planejador procura outro objetivo.

## 3.4 Grupo não é equivalência

**Implementado em 2026-08-22 — ver §5.** Grupo classifica. Substituição
se declara, uma exigência por vez.

## 3.5 Orçamento e cadeia produtiva

```text
NECESSÁRIO − DISPONÍVEL − PRODUÇÃO POSSÍVEL = DÉFICIT REAL
```

O planejador monta a cadeia antes da obra, e avalia a cadeia **inteira**
— não só o material da parede.

## 3.6 A vila nunca fica presa

Obra bloqueada devolve ao planejamento. Se a casa não pode subir, a
fazenda, o armazém ou a estrada podem. **A vila continua evoluindo.**

## 3.7 Objetivos graduais

```text
SOBREVIVÊNCIA → ESTABILIZAÇÃO → INFRAESTRUTURA
              → EXPANSÃO → ESPECIALIZAÇÃO → CRESCIMENTO
```

Nada de grande estrutura antes da infraestrutura básica. E a população
cresce pela **capacidade** — casas, comida, segurança —, não pelo
relógio.

## 3.8 Reserva de sobrevivência

Nenhuma obra pode consumir o que a vila não repõe. Toda construção tem
classe de custo, e existe um mínimo intocável de comida, combustível e
material.

## 3.9 Dependência circular

```text
CASA → MADEIRA → LENHADOR → CASA
```

O planejador precisa **detectar o ciclo** e ter um estado inicial
sustentável. Um planejador ingênuo entra em laço aqui.

## 3.10 Preferência não é obrigação

```text
PREFERRED · ACCEPTABLE · ALTERNATIVE · FORBIDDEN
```

Deserto **prefere** arenito; isso não quer dizer que só possa arenito.
Variedade sem perder identidade.

---

# 4. Testes: autonomia, não função isolada

`DesertVillageAutonomyTest`, `PlainsVillageAutonomyTest`,
`TaigaVillageAutonomyTest`, `SavannaVillageAutonomyTest`,
`SnowyVillageAutonomyTest`.

A pergunta que eles fazem não é "a casa subiu?", e sim **"a vila
continuou evoluindo sem intervenção?"**.

**A planície deixa de ser o ambiente padrão universal.** A arena de
planície escondeu por uma semana que o mod não reconhecia a rua do
deserto, e escondeu de novo que ele pedia o arenito errado. O deserto é
o primeiro caso completo.

---

# 5. O que já foi feito, nesta ADR

**§3.4 está implementado.** `ResourceSubstitution` nasceu em
2026-08-22:

```text
o padrão é NÃO substituir
uma exigência se satisfaz só com ela mesma
substituição é declarada, e a lista é a declaração

declarado hoje:  tronco por tronco, tábua por tábua
não declarado:   pedregulho por arenito
```

`ResourceDemand.available` somava o grupo inteiro e passou a somar só o
declarado. Era isso que fazia uma vila de deserto com 320 de pedregulho
concluir que a meta de arenito estava cumprida.

**O resto desta ADR é direção, e não código.** O que existe hoje é a
metade de baixo do diagrama — trabalhadores, construções, produção —
funcionando sem a metade de cima. `VillageProfile`, o inventário de
território, o orçamento e o planejador que desiste de uma obra e escolhe
outra **ainda não existem**.

---

# 6. As decisões dos itens abertos, revistas

| | Item | Decisão |
|---|---|---|
| 1 | `SANDSTONE` + `COBBLESTONE` | ✅ **feito em 08-22.** Grupo não é equivalência |
| 2 | `SMOOTH_SANDSTONE` | Pelo sistema **genérico** de produção. Nenhuma exceção de deserto |
| 3 | StallGuard | Diagnosticar e tornar determinístico. **Não** esconder com retry |
| 4 | Arquivos grandes | Refatorar por **responsabilidade**. 500 linhas é indicador, não regra |
| 5 | Pedra não chega ao baú | **Instrumentar primeiro.** Não corrigir sem diagnóstico |
| 6 | Ícone | Otimizar. Dívida de distribuição, não prioridade funcional |
| 7 | `furniture()` | Eliminar dono ambíguo. Uma decisão, uma autoridade |
| 8 | Regra 25 / 28 | Remover a 25 se estiver definitivamente substituída. Sem lógica morta |
| 9 | Arena | Fim da dependência exclusiva de planície. Cenário por bioma |
| 10 | Development Log | Virar **registro de conhecimento arquitetural** |

---

# 7. Consequences

**Ganha:** bioma novo passa a custar um perfil, e não uma varredura de
`if`. E a vila deixa de poder ficar parada esperando um material que
ninguém ali sabe fazer.

**Custa:** é a maior mudança de forma desde a ADR-001. O planejador
atual escolhe **uma** obra e a persegue; o novo precisa escolher entre
objetivos, o que toca `ConstructionPlanner`, `ColonyGoals` e o modelo de
tarefa.

**O risco declarado:** construir o motor inteiro antes de ter um bioma
funcionando de ponta a ponta seria repetir o erro que esta ADR corrige —
projetar longe do que o jogo mostra. O deserto vai primeiro, e o motor
se generaliza a partir dele.
