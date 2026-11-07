---
name: fluidez-passo-do-ciclo
description: "o que mais trava a vila em 30-09 — tarefa só no ciclo de 30 s, obra esperando peça sem cadeia, e prazo de 15 ms que não cobre o planejador inteiro"
metadata:
  node_type: memory
  type: project
  modified: 2026-09-30T06:28:21.268Z
---

Levantamento de 2026-09-30 em `docs/technical/Fluidez-Profissoes-2026-09-30.md`
(itens F1–F14 no topo do `TODO.md`).

- A tarefa só é distribuída dentro do ciclo de colônia (`ColonyCycle.java:153`,
  600 tiques): quem termina no meio espera até 30 s, e castigos contam ciclos.
- A casa do pastor levou 19 min, ~12 esperando tear (sem fonte de linha, 3
  tentativas antes de estocar) e vidraça (sem coleta de areia aberta).
- **F1 é regressão minha**: `291187e` tirou o teto de 1.024 colunas com prazo
  armado; o planejador subiu de 1–2 ms para 28 ms de mediana e 178 de máximo,
  porque o prazo só envolve a varredura, não rua/índice/abertura.

**Why:** a vila parece "pausada" por espera de ciclo e de peça, não por
trabalhador travado; e mudar teto por prazo sem cobrir o resto do planejador
move custo em vez de limitá-lo.

**How to apply:** ao mexer em orçamento do planejador, meça `planner N ms` no
`Colony cycle took` antes e depois. Ver [[folga-de-chegada-contra-alcance]] e
[[cadeia-de-producao-recursiva]].

**Estado (2026-09-30, fim da rodada):** F1, F2, F3, F4, F5, F10, F12 feitos;
F6 e F13 parciais; F7, F8, F9, F11, F14 abertos (tabela em §8 do doc). JAR
com `22b40bf`..`cd22243` NÃO instalado; nada visto em jogo.

**Regras do autor revistas em 30-09 (tarde):** lote só na altura da rua ou um
acima (nunca abaixo; lacuna de 1 aterrada; degrau se a porta ficar 2 acima);
peça sem rota → após 3 tentativas aparece o INGREDIENTE sem rota no baú do
artesão, nunca a peça pronta nem item natural; areia vale em qualquer bioma
perto de água. Commits `a2b1d1c`, `1920948`. JAR não instalado.
