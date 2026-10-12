# ADR-042 — Mina em rede: poço seguro, níveis em grafo e caminho só pelo que está pronto

**Estado:** proposta, aguardando o autor (2026-10-12).
**Pesquisa:** [`docs/research/2026-10-12-mineiro-mina-em-rede.md`](../research/2026-10-12-mineiro-mina-em-rede.md).

## Contexto

A mina de hoje (`MineShaft`, `Mine`, `MineArm`) é uma escada em caracol por nível, um salão e
quatro ramais fixos que descem em escada. O nível só desce com os quatro ramais terminados; um ramal
travado trava a mina. Não há piso garantido: a mina rompe cavernas e deixa quedas de 3 blocos, e o
mineiro desce por elas e não volta. O caminho é uma perna pela ordem de cavar, e não por onde já se
sabe andar. No playtest 0.3.32 a mina estava assim e o mineiro cavou zero blocos.

O autor pediu (12-10) uma mina como percurso infinito e interligado, construída pelo mineiro:
várias galerias seguindo veios, zonas grandes, nível após nível até o fundo, caminhos laterais e
volta subindo, sempre seguro, pondo bloco para subir.

## Decisão proposta

| # | Regra | Origem |
|---|---|---|
| D1 | **Poço seguro:** escadaria reta (degrau de 1) a partir da boca da borda, patamar a cada nível; piso do patamar e 2 blocos em volta garantidos com pedregulho; fluido vedado | MineColonies `secureBlock`; pedido do autor |
| D2 | **Nível em grafo:** nós numa grade de 7 blocos no plano do nível, corredor 3 largo × 3 alto entre nós; nó aberto/em obra/pronto/vedado | MineColonies `MineNode`, `MinerLevel` |
| D3 | **Vários mineiros**, um nó cada (no lugar de um por ramal fixo) | pedido do autor |
| D4 | **Veio primeiro:** minério visto na parede do corredor é seguido até acabar (teto de blocos) e o mineiro volta ao nó | `MineVein` atual; MineColonies |
| D5 | **Andar só pelo pronto:** rota = BFS no grafo de nós prontos; pernas de nó a nó (≤ 7 blocos). Piso que falta recebe bloco; degrau > 1 recebe degrau | MineColonies (nó-pai); Baritone (`allowPlace`, queda máx. 3) |
| D6 | **Caverna e fluido vedados:** abertura do corredor para ar de fora da mina ou para fluido recebe pedregulho; nó com fluido não abre | MineColonies |
| D7 | **Descer:** sem nó aberto alcançável no raio do nível, o poço desce um nível; os níveis antigos ficam no grafo | pedido do autor |
| D8 | **No fundo:** caminhos laterais e segunda escadaria de volta, ligando os níveis por outro lado (circuito, duas saídas) | pedido do autor |
| D9 | **Save:** o grafo (nós e estados) gravado por mina; mina antiga do save é adotada como nível 0 do grafo a partir do que está cavado | MineColonies grava `MinerLevel` |

## Alternativas

1. **Remendar a mina atual** (ramal travado termina, caminho direto pela navegação, escada para
   sair do buraco) — feito na 0.3.34. Destrava o save de hoje, mas não muda a forma: quatro ramais
   que descem em escada, sem piso garantido, quedas de mão única.
2. **Rede de nós (esta ADR).** Reescreve `MineShaft`/`Mine`/`MineArm`/`MineCuts`/`MinerLeg` como
   grafo; maior custo, maior ganho.
3. **Poço de escada de mão (MineColonies puro).** Aldeão Vanilla sobe escada de mão; mais simples
   que a escadaria, mas a escada precisa de madeira e trava se faltar.

## Consequências

- Reescrita do núcleo da mina em fases (D1 → D2/D5 → D3/D4 → D6 → D7 → D8 → D9), cada fase com
  GameTest e playtest; a mina atual segue funcionando até a troca.
- Gasto de pedregulho no piso e na vedação, coberto pelo que a própria mina produz.
- Decisões em aberto para o autor: altura entre níveis (proposta: 8); largura do corredor (3);
  teto do veio (proposta: 64 blocos); escadaria ou escada de mão (D1).
