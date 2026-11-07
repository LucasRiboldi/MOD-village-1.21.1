---
name: dois-mineiros-mesmo-tunel
description: Mineiros enfileirados no mesmo túnel é o E44 — o cursor segura pedra inalcançável sem prazo
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-10T12:26:54.550Z
---

**Dois mineiros parados no mesmo túnel, um atrás do outro, é o E44** — não é
colisão de reserva de ramal, que funciona.

**A assinatura no log**, vista em jogo pela primeira vez em 2026-09-10 às
08:33:

```text
miners: <A> digging <pedra> at X,Y,Z ... (out of reach) ... stall N/2400
        <B> waiting for a branch — 2 of 4 taken, <A> in one
```

seguida, ~2.400 tiques depois, de `<A> gave up the stone at X,Y,Z — it walked
for 2400 ticks of work time without arriving`, e então **`<B>` recebendo a
mesma coordenada**.

**A leitura:** `waiting for a branch` está **certo** — um mineiro por ramal é
de projeto. O defeito é o cursor: `couldNotReach` → `holdPositionAt` segura a
posição **sem prazo**, e ela é servida a quem assumir o ramal. Os dois se
revezam no mesmo alvo impossível, e por isso ficam enfileirados.

**`N blocks below it and unable to climb` acompanha, e não é o E40.** O E40
(escolha do lugar de ficar de pé) fechou em 09-09; quando esta frase volta, é
o caso em que **nenhum** lugar alcançável existe — o E44.

**O conserto que o projeto já tem do outro lado:** o `TreeMarks` do lenhador
sobe uma escada de prazos (6.000 → 48.000 tiques) e nunca apaga a marca. A
mina não tem equivalente. **Pular a posição por uma desistência não serve** —
custou três sessões com a galeria intacta em 08-27.

Ver [[mineiro-em-cima-da-mina]] para a outra assinatura de mineiro parado, que
é diferente: `still 0/300` é quem gira no próprio eixo.

**25-09: a marca não sobrevive ao carregar.** `MineMarks.refuse` vive só em
memória: a cada sessão o mineiro volta à mesma pedra inalcançável (553,39,158
no save do autor, de 01:xx a 09:17), desiste, fica preso e o `StrandedEscape`
desiste ("no natural, dry way up") — fora da escala o resto da sessão.
