# ADR-029 - Ofício da colônia exclui ofício Vanilla

**Status:** Accepted
**Date:** 2026-09-30
**Decision Type:** Gameplay / Mixin
**Amends:** ADR-004 §3 (superfície de Mixin permitida)

## Contexto

O mod atribui a própria profissão por necessidade (`ProfessionAssigner`), e o
Vanilla continuava dando a dele: um lenhador da colônia podia ser ferreiro do
jogo, andar até a bigorna no expediente e segurar a estação de trabalho de
outro aldeão. `Profession-Responsibility.md` registrava isso como pendência
"nunca decidida por escrito".

## Decisão

O autor decidiu:

> Aldeão que recebe profissão vinda do mod não recebe profissão vinda do vanilla.

`VanillaProfessionGuard` faz isso por duas portas:

1. **Mixin 3, `VillagerDataMixin`**: `@ModifyVariable` no argumento de
   `VillagerEntity.setVillagerData`. Se o aldeão é trabalhador da colônia com
   profissão e o dado traz um ofício Vanilla que não é `NONE` nem `NITWIT`, o
   dado sai com `NONE`. O Vanilla dá ofício por vários caminhos (tarefa de ir
   ao trabalho, cura de zumbi, NBT), e todos passam por esse método.
2. **Varredura** (`VillagerScanner`, a cada ciclo): quem já tinha ofício Vanilla
   antes de ser contratado o perde, e as memórias `JOB_SITE` e
   `POTENTIAL_JOB_SITE` são esquecidas, com o ticket de POI liberado. A
   estação volta a ficar livre para os outros aldeões.

O aldeão sem profissão do mod, e o nitwit, não são tocados.

## Conformidade com a ADR-004

- Regra 1: `@ModifyVariable` é permitido com justificativa. A justificativa é
  que só o argumento revela o ofício novo, venha ele de onde vier.
- Regra 2: nada é cancelado. A chamada segue, e só o dado muda.
- Regra 3: o mixin só delega a `VanillaProfessionGuard.filter`.

A ADR-004 §3 passa a listar três mixins em `VillagerEntity`: `initBrain`
(TAIL), `onDeath` (HEAD, se ainda existir) e `setVillagerData` (HEAD,
`@ModifyVariable`).

## Consequências

- O aldeão contratado perde as trocas Vanilla, porque sem ofício não há troca.
  É a consequência pedida.
- O Vanilla ainda tenta reivindicar a estação ao ver uma livre. A varredura
  solta a reivindicação a cada ciclo, então a estação fica presa no máximo por
  um ciclo. Se isso aparecer como custo no Spark, a saída é filtrar também o
  sensor de POI, com uma decisão nova.
- Guardado por `ProfessionReviewGameTest.aColonyWorkerCannotTakeAVanillaTrade`.
  O controle é um aldeão de fora da colônia, que continua fazendeiro.
