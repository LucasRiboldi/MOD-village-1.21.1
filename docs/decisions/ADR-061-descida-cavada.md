# ADR-061 — A descida cavada do mineiro

**Estado:** aceita pelo autor em 2026-11-20 (opção A3), sobre a pesquisa
`docs/research/2026-11-20-descida-cavada-do-mineiro.md`. Regra 336. Emenda as Regras 82 (a entrada memorizada) e 119
(a escada da mina).

## Contexto

No jogo da 0.4.93 em planície, o mineiro trabalhou 28% do expediente e ficou 27% ocioso. O ócio era o descanso de 4
ciclos (2.400 tiques) depois de cada uma das 13 desistências. Em 11 delas o alvo da descida era um lugar que a
navegação não alcança: a própria boca, com ele de pé nela, ou um ponto 2 blocos embaixo dos pés. A rota da descida
(`MineEntrance.route`) entrega os pontos da escada planejada (`MineShaft.waypoints`), cavada ou não, e o caracol girado
(`MineTrouble.rerouteBlockedStair`, ADR-037 M1) recomeça com todos os degraus por cavar. O guarda de imobilidade
desistia, e ninguém cavava o degrau que faltava.

## Decisão do autor

O mineiro só pisa no degrau já aberto, e o degrau que falta ele cava (`MineStepDown`):

1. **Quando cava.** O alvo da descida é o pé dele, fica embaixo dele (até 1 bloco de lado) ou não tem lugar de pé
   (`MineStepDown.cannotWalkTo`). Aí, em vez de andar, ele começa a descida cavada rumo à pedra da tarefa.
2. **O formato.** O da escada da mina (Regra 119): cada degrau tem 3 pistas de largura, à esquerda de quem desce, por 3
   de altura, um bloco à frente e um abaixo do anterior, cortado de cima para baixo. Ele vai rumo à pedra pelo eixo
   mais longo; a até 2 blocos dela na horizontal, desce em caracol, virando à direita a cada 3 degraus.
3. **O que não se cava.** Bloco protegido (Regra 3), líquido e bloco que segura líquido. O lado assim é trocado pelo da
   direita. Com degrau cavado, ele nunca vira à esquerda nem volta: ali ficam as pistas já abertas.
4. **O piso.** O degrau sem chão embaixo (vão de caverna) ganha piso de pedra antes de ele pisar, tirada do baú dele ou
   criada (decisão A-5 de 2026-10-02), só dentro da montanha (Regra 71). A pista do meio sem chão ao céu aberto
   descarta o lado.
5. **Os guardas.** Enquanto cava, os guardas de travamento não correm; cavar é sair do lugar. O degrau tem 100 tiques
   para ser pisado depois de aberto. A descida acaba na altura da pedra ou com ela ao alcance, e a falha devolve a pedra
   como antes.

## Consequências

- A desistência por alvo inalcançável na descida deixa de existir nos casos do jogo; o descanso de 4 ciclos continua e
  é medido no próximo jogo.
- A escada cavada assim pode não coincidir com o caracol planejado do nível; as duas ficam abertas.
- **Não decidido aqui:** o caracol girado com degrau já cavado (ADR-037 M1) segue girando; com a descida cavada, o
  mineiro desce ao caracol novo cavando.
