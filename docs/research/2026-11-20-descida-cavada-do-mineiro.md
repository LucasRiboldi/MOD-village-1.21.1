# A descida cavada do mineiro — formato e regras

Pedido do autor, 2026-11-20: antes de escolher a correção do mineiro (jogo da 0.4.93 em planície, 33% do expediente
sem trabalhar), pesquisar o modelo da descida cavada: o mineiro desce abrindo espaço de 3 blocos de largura por 3 de
altura, com o formato da descida definido.

## 1. O que o jogo mostrou

`time_ledger.py`: mineiro com 28% de trabalho, 34% de caminhada, 27% ocioso, 5% bloqueado e 1% encalhado.

- **O ócio é castigo, não falta de trabalho.** Cada desistência põe o ofício em descanso por `Worker.REST_CYCLES` = 4
  ciclos (2.400 tiques). Foram 13 desistências; no diário, a tarefa seguinte vem uns 2.000 tiques depois de cada uma.
  Isso dá ~26 mil tiques, os 27%.
- **11 das 14 desistências têm o alvo da caminhada onde a navegação não leva:**
  - 5 vezes na boca da mina (341,67,-237 e 369,67,-251), com o alvo na própria boca e a navegação parada;
  - 6 vezes em 340,63,-222, com o alvo em 340,61,-222, 2 blocos abaixo dos pés (o caminho tem 1 nó: o lugar dele).

  O guarda de imobilidade (`MinerSteps.guardOrWalk`, 300 tiques) desiste. Ninguém cava o degrau que falta.
- **O laço:** 3 pedras que ninguém alcança fecham o ramal (`MinerSteps.stuck`, `UNREACHABLE_BEFORE_DONE`). A escada
  fechada antes do salão gira o caracol (`MineTrouble.rerouteBlockedStair`, ADR-037 M1), e o caracol novo parte da
  mesma entrada, com todos os degraus por cavar. `MineEntrance.route` entrega ao mineiro os pontos do caracol
  planejado (`MineShaft.waypoints`), cavados ou não, e o próximo ponto fica dentro da rocha embaixo dele. Em
  14:55:57 o caracol girou 4 vezes no mesmo instante.

## 2. O formato que já existe (Regra 119, Regra 254)

`MineShaft` já desenha a descida em 3 × 3:

| Peça | Medida | Constante |
|---|---|---|
| Degrau | 3 pistas de largura × 3 de altura, 1 abaixo e 1 à frente do anterior (9 blocos) | `STAIR_LANES`, `STAIR_HEADROOM`, `STAIR_STEP_BLOCKS` |
| Lance | 3 degraus, depois vira à direita | `HELIX_SIDE` |
| Caracol do nível | 4 lances, uma volta, 12 degraus (12 blocos de queda) | `HELIX_FLIGHTS`, `DESCENT` |
| Patamar | 3 × 3 plano na dobra, com tocha na parede de fora (só nível novo) | `LANDING_ROWS`, Regra 254 |
| Salão | 9 × 9 × 3 ao pé do caracol | `HALL_SIDE`, `HEADROOM` |
| Ramais | 4 caracóis iguais, um em cada canto do salão, cada um com seu salão | `ARM_STAIRS`, `ARM_AREA_BLOCKS` |

As pistas ficam à esquerda de quem desce, e o miolo do caracol é de rocha. O formato está certo; o defeito é a ordem.
O mineiro **anda** para degraus que ainda não **cavou**.

## 3. O que o Vanilla permite (1.21.1)

- Aldeão: 0,6 × 1,95 (`EntityType.VILLAGER`); passo de 0,6 (`EntityAttributes.GENERIC_STEP_HEIGHT`). Para subir um
  degrau de 1 bloco ele pula.
- Navegação (`LandPathNodeMaker`): sobe 1 bloco por nó e desce até a distância de queda segura (3 blocos). Não traça
  caminho por dentro de bloco sólido: alvo dentro da rocha dá caminho que "não chega" ou nó único no próprio pé.
- Altura livre: com 3 de altura por degrau, quem sobe tem 3 blocos de ar na coluna de baixo. O pulo de 1 bloco de
  um corpo de 1,95 cabe (o teto mais baixo do pulo é 3,0 acima do pé). Com 2 de altura, o pulo bate no teto.
- Degrau de 1 abaixo por 1 à frente (45°) é o mais íngreme em que a navegação sobe e desce sem cair.

## 4. A vida real

A mina de gente tem dois caminhos: a rampa larga e suave, para máquinas (1:7 a 1:9), e o passadiço de pessoas (escada
de uns 45°, com patamar a cada lance). O caracol do mod é o passadiço de pessoas. O mineiro de verdade abre a frente e
só pisa onde já abriu; o degrau de baixo é cavado de pé no degrau de cima.

## 5. Formatos possíveis para a descida cavada

| # | Formato | O que muda | Risco |
|---|---|---|---|
| A1 | **Caracol cavado à frente.** Mesmo caracol da Regra 119. O mineiro só anda até o degrau aberto mais fundo; o próximo, ele cava de pé no último aberto (as 9 células, de cima para baixo). O caracol não gira enquanto há degrau cavado: o degrau bloqueado (bloco protegido, água) vira a dobra, com patamar, e o lance segue para o lado livre. | `MineEntrance.route` só entrega ponto cavado; `rerouteBlockedStair` só gira sem degrau cavado; o cavar à frente usa o desvio só-cavar que já existe (`MineAccess`, `MinerTerrain.digOnly`). | Escada torta onde a rocha obriga; a geometria salva muda de dono (o cursor deixa de ser só a ordem fixa). |
| A2 | **Rampa reta com patamar.** Da boca, uma escada reta de 3 × 3, 1 abaixo por 1 à frente, rumo para fora da vila, com patamar de 3 × 3 a cada 4 degraus; no fundo, o salão e os ramais de sempre. | Geometria nova para o trecho da boca ao primeiro salão (ADR nova, emenda à Regra 119). | Pegada comprida: 12 de queda ocupam 15 a 18 blocos de lado, pode sair da caixa da vila ou cortar caverna. |
| A3 | **A1 com piso garantido.** Igual à A1; o degrau aberto sem chão embaixo (caverna) ganha piso com a pedra que ele cavou, só dentro da montanha (Regra 71). | A1 mais o piso. | Põe bloco: só onde a Regra 71 já deixa. |

Em todos: o alvo da descida nunca é o próprio pé nem a rocha embaixo dele. Sem degrau aberto abaixo, ele cava.

## 6. Recomendação e escolha

A3: mantém o traçado que o autor já aprovou (Regras 119 e 254) e corrige a ordem (cava antes de andar). O laço do
caracol girado acaba, e o degrau sem chão não vira queda. O descanso de 4 ciclos fica como está e é medido no
próximo jogo.

Escolha do autor (2026-11-20): A3. Implementada na 0.4.100 (Regra 336, ADR-061).
