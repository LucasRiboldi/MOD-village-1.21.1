# ADR-027 - Acesso selado de mina sob agua e eixos 3x3

**Status:** Accepted
**Date:** 2026-09-27
**Decision Type:** Architecture / Gameplay / Save Data

## Contexto

A boca normal da mina exige solo firme e rejeita agua. Em uma vila fundada
sobre agua, essa regra correta deixava a colonia sem uma rota fisica para
chegar a pedra, mesmo quando havia uma camada mineravel abaixo do fundo.

A geometria anterior tambem usava dois lances de largura. O autor definiu que
qualquer descida de mina deve ter tres blocos de largura e tres blocos uteis de
altura, inclusive no caso submerso.

## Decisao

1. A mina seca continua sendo a regra normal. Somente quando ela esgota todos
   os niveis, a reabertura tenta primeiro o acesso submerso; se ele nao for
   valido, `MineSite` continua escolhendo a boca seca no lado oposto.
2. A excecao exige fundacao predominantemente em agua, todos os chunks da rota
   ja carregados e uma saida de exatamente 8x8 blocos de pedra natural
   mineravel. Nao carrega chunks nem cria pedra para satisfazer a condicao.
3. A rota submersa tem tres lances, degraus de `stone_brick_stairs`, duas
   camadas livres acima de cada degrau e casco de vidro em paredes e teto. O
   degrau mais as duas camadas livres formam os tres blocos uteis de altura.
4. A rota nunca pode atravessar estruturas Vanilla, blocos da colonia ou os
   blocos persistentes protegidos pelo mod. A verificacao precede qualquer
   escrita, portanto uma tentativa recusada nao deixa uma escada parcial.
5. A fronteira, os cortes e a busca de veios ignoram os degraus e o casco de
   vidro dessa rota. O mineiro so passa por ela; nao a minera, nao a desvia e
   nao a interpreta como um veio.
6. Toda escada normal passa a usar tres lances. `MineSave.SHAPE_VERSION` sobe
   de 6 para 7: saves na forma 6 conservam boca, profundidade, galeria e arco,
   mas descartam os cursores de corte calculados para a largura antiga.
7. Ao abrir a rota submersa, a mina recebe iluminacao interna sem erguer o
   arco decorativo da boca seca, que bloquearia a passagem selada.

## Consequencias

- Vilas em terra mantem a escolha e a aparencia da mina seca.
- Uma vila aquatica sem 64 blocos naturais contiguos abaixo dela continua sem
  mina; a regra nao inventa terreno nem expande a area para alem do mundo
  carregado.
- A migracao reavalia no maximo os cortes da geometria, sem mudar a entrada
  que o jogador ja conhece.
- A verificacao visual de uma vila aquatica em save real continua necessaria,
  sobretudo para a navegacao do aldeao ao longo da escada.

## Verificacao

`MineShaftTest` fixa os tres lances e tres alturas; `MineSaveTest` fixa a
migracao 6 para 7. `WaterMineAccessTest` cobre a geometria, o casco e a saida
64/64. `WaterMineAccessGameTest` enche a rota com agua, esgota uma mina,
confirma a transferencia para a entrada inferior e preserva degraus, casco e
interior sem fluido. Em 2026-09-27, `runGametest --rerun-tasks` terminou com
488/488 GameTests obrigatorios.
