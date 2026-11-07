---
name: analise-biomas-logs-0393
description: "Análise de 5 logs da 0.3.93 (23–26/10): 7 colônias em savana, planície, neve, taiga e deserto; achados por bioma e correções pontuais propostas"
metadata:
  node_type: memory
  type: project
  modified: 2026-10-27T15:33:42.903Z
---

Logs de 23-10 a 26-10, todos da versão 0.3.93. A análise foi feita em 27-10 e nenhuma correção foi implementada ainda.

**Fato confirmado: 14 aldeões morreram sufocados ao serem teleportados para a cama.** Foram 12 na neve (cebb933e) e 2 no deserto (2ba27bf8). Cada morte veio 2 a 10 s depois de "teleported … to its bed". A causa está em `BedTeleport.toBed`: o `requestTeleport` põe o aldeão em pé na cama (`BED_TOP=0.5625`) sem conferir se há espaço livre para a cabeça. Nas casas de neve, de teto baixo, ele fica com a cabeça dentro do bloco.

**Presos por horas sem saída:**
- Taiga: o mineiro 0b0fa2d6 ficou 3,5 h "fenced in", com 0 portões, a 2–4 blocos da própria cama. O teleporte noturno o devolveu para dentro do mesmo cercado.
- Savana: o construtor 67cdc3b7 ficou 4 h preso em y=55, foram 2318 trocas de STAIRS. Nunca foi teleportado. A hipótese é que `bedOf` não achou a cama, mas isso não foi confirmado.

**Achados por bioma:**
- Savana: o lenhador não concluiu nenhuma tarefa e há muitos "Not a tree", porque a acácia tem tronco diagonal. Faltaram acacia_log e acacia_wood.
- Planície: sem argila (6657 linhas de log), o fundidor espera 56% do tempo. Na colônia 1bd4fbb9 o fazendeiro pegou só 1 tarefa em 9 h, e a vila ficou em 16 aldeões mesmo com 41 camas.
- Deserto: falta smooth_sandstone (146). O pastor fica 69% ocioso, porque não há ovelha. O lenhador fica 61% ocioso.
- Neve: faltam snow, snow_block e diorite. Foram 4 obras em 2,5 h.

**Comum a todos os biomas:**
- o storage_majest pede deepslate, calcite e acacia_leaves em qualquer bioma;
- "o pastor não tem ração" aparece milhares de vezes;
- o mineiro é recusado com NO_STANDING e TOO_HIGH.

**Why:** o autor pediu melhorias pontuais por bioma a partir desses logs.
**How to apply:** a prioridade é corrigir o BedTeleport e o escalonamento de quem fica preso sem saída. Ligado a [[regra-113-pecas-de-lado]] e [[movimentacao-natural]].
