---
name: sessao-de-jogo-destrava-o-plano
description: O gargalo do mod não é código — sete entregas de 09-11 esperam sessão de jogo, e o número do LotRefusals decide o P0.7
metadata:
  type: project
---

Em 2026-09-11 o plano de correção fechou todo o P0 e quase todo o P1 —
e **nada disso foi visto em jogo**. Sete itens estão `⬜ espera sessão`:
P0.1-c, P0.3, P0.5, P0.6, P1.3, P1.5, P1.13, P2.1.

A sessão entrega um número específico: o **`LotRefusals`**, que conta
quantos lotes o planejador recusa por `OFF_ROAD_LEVEL` contra os outros
quatro motivos do `flatGroundAt`. **É esse número que abre ou fecha a
terraplanagem (P0.7)** — represada de propósito, para não abrir por
inferência.

**Why:** o projeto tem 778 unitários e ~299 gametests verdes, e a régua
do autor separa *tem teste* de *foi visto em jogo*. Escrever mais código
antes da sessão acumula entregas não validadas em cima de entregas não
validadas.

**How to apply:** antes de propor trabalho novo de código, diga que a
sessão de jogo é o próximo passo do plano. Conferir o md5 do jar antes
de todo relato de jogo — ver [[jar-difere-so-por-crlf]] e o ritual em
[[relatorio-commit-ritual]].
