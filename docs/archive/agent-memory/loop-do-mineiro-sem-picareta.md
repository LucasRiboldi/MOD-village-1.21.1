---
name: loop-do-mineiro-sem-picareta
description: E45 — o poço exige 120 posições e o cursor nunca passa de 8; 166 mil linhas a 10/s com zero pedra quebrada
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-17T01:34:13.039Z
---

Playtest de 2026-09-16 (03:44–08:23, `latest.log`, 341 mil linhas): o mineiro
girou em falso 4h40. 166.559 linhas de "hit stone with nowhere to stand",
**10 por segundo por 16.657 s sem interrupção**, e **zero** "Miner … took" —
nenhuma pedra saiu do mundo. Zero construções concluídas. É o **E45**,
analisado em `docs/technical/E45-mina-presa-na-boca.md`.

**A causa — corrigida em 2026-09-16 22:30, contra o playtest das 21:41.**
Com o poço não cavado, `branchesOpenNow()` vale 1, então só existe o braço 0.
Ele fecha em 8 recusas (`BLOCKED_BEFORE_TURNING`) dentro de um orçamento de
64 (`CUTS_PER_SEARCH`), `claimArm` o solta por não haver segundo braço, e a
passagem seguinte o reocupa **no mesmo `cut`**. O laço é a **mesma passagem
repetida**, e o poço exige 120 posições (`CARVED = 5×3×2×4`) que nunca são
vencidas.

⚠️ **A versão anterior desta memória dizia que
`deepenIfEveryOpenArmIsDone` + `restartAt` reiniciavam o braço. É falso.**
Esse caminho **nunca é alcançado**: `went one level deeper` = 0 contra
`no miner branch work` = 17.517, e a segunda frase só é escrita quando o
reinício não ocorreu. Ver [[frase-ausente-refuta-mecanismo]].

**Cinco hipóteses que testei e caíram — não repetir:**
- Não é o `ColonyEdits` de [[mineiro-em-cima-da-mina]]: ele só age depois que a
  picareta pega, e ela nunca pegou.
- Não é o lenhador via `reopenFrom`: ~1 árvore a cada 55 s não sustenta 10/s.
  **Compare a frequência da causa com a do efeito antes de aceitar uma raiz.**
- Não é o fundo da mina (`rerouted`): todos os Y do log entre 59 e 65,
  superfície; `DEEPEST` é -59. Esse ramo e seu teste estão corretos.
- **Não é o reinício** por `deepenIfEveryOpenArmIsDone`/`restartAt`: era a
  tese antiga, e `went one level deeper` = 0 a derruba.
- **Não é `MineFrontier.findTheFrontier`** repondo o cursor: ele escreve
  `The gallery really ends at` a cada reposição — **1** linha contra 17.518
  recusas.

**A assinatura para reconhecer em qualquer log:** `0 digger(s) in 1 open
branch(es)` imutável, `deeper` com contagem zero, e `took` com contagem zero.
O "1" diz que o poço não abriu; o "took" zero diz que não é problema de
reabertura, é de progresso.

**O defeito de fundo, que vale além deste bug:** `stall 0/2400`, `still 0/300`,
`adrift 0/400` ficaram zerados as 4h40 — o caminho de falha zera os contadores
no mesmo tique em que falha. **Guarda que o caminho de falha zera não é
guarda.** Terceira volta do mesmo laço (19.193 linhas em 02:58, nove
desistências em 09-11, 166.559 agora); cada conserto fechou uma porta
específica e o guarda genérico nunca disparou. Ver [[lento-nao-e-travado]]:
aqui não é lento, é travado, e a linha do tempo prova.

**E o laço é mudo:** as duas frases que ele emite descrevem estados normais
("o braço acabou", "não há trabalho"). Nenhuma diz que **nada progrediu**,
por isso ele só era visível pelo efeito colateral (cobblestone 411 → 10,
builder parado 46 vezes, obra morta com 12 blocos restantes).

**A correção mudou junto com a causa:** C3 (mexer em `everyOpenArmIsDone`)
foi **retirado** — premissa refutada, e mexer ali arriscaria o limbo de
09-04. C2 precisa de contador que sobreviva ao **`finish()`**, não ao
`restartAt`, guardado na `Mine`. **C4 é a correção de verdade** e espera
decisão do autor: boca intransponível → mudar a boca, girar a hélice, ou
desistir por um prazo.

**Não confundir com [[e46-obra-fecha-sozinha]]:** as construções pararem é
defeito **independente** deste. Corrigir o mineiro sozinho não faz a vila
construir.
