# ADR-063 — O plano de alturas da vila

**Estado:** aceita pelo autor em 2026-11-25 (G5 do jogo da 0.4.136, as quatro decisões 🅲), sobre a pesquisa
`docs/research/2026-11-25-plano-de-alturas.md`. Implementação em fases (abaixo); as fases 1 e 2 só leem (0.4.138, linha `VC_GRADE`), e a regra nasce com a fase 3,
que mexe no chão. Substitui a fila de quadrados da Regra 323 e o alisamento em volta da casa pronta da Regra 297. Emenda a ADR-059
(item 2: uma vaga de Zelador por vila).

## Contexto

Três sistemas nivelam o chão, e cada um olha uma janela e decide sozinho a altura de cada coluna: a geração (Regra 152),
a volta da obra (Regra 297) e a fila de quadrados do Zelador (Regra 323). Nenhum sabe a altura que a vila inteira
deveria ter, e um desfaz o outro: em volta da casa do mineiro, a Regra 297 cortou 573 blocos para a altura da rua e a
fila tapou depois a coluna "isolada". No jogo da 0.4.136, os degraus maiores que 1 subiram de 518 no dia 1 para 1.909
no dia 29, com o Zelador nivelando de 1 a 620 colunas por dia. O curral grande ficou sobre um paredão, sem acesso.

O autor pediu (25-11): a vila inteira nivelada para todo aldeão andar por toda ela e chegar a toda estrutura, os
caminhos o mais planos possível, um degrau onde não der e mais de um só se precisar, e uma faixa de solo do bioma em
volta de cada estrutura, a começar pelo curral grande.

## Decisão

### 1. Uma altura-alvo para cada coluna, calculada de uma vez

A colônia calcula um **plano de alturas** para a caixa da vila mais uma margem, e o Zelador só executa o plano.

- **Altura de uma coluna:** o `y` do bloco de chão de cima, onde se pisa.
- **Âncora:** coluna cuja altura não se discute:
  - a frente de cada porta e portão, na altura do piso da entrada;
  - a faixa em volta de cada estrutura, de 1 bloco (2 no curral grande), na altura do chão de entrada dela. Vale para a
    construção da colônia e para a peça da vila gerada (decisão 3);
  - a borda da margem, no chão natural;
  - a margem da água, na altura da água (decisão 4).
- **Fora do plano:** a pegada das estruturas, a obra aberta e o lote dela, a folga da boca da mina (Regra 356), o chão
  do jogador (Regra 334) e a água.
- **Envelope de degrau 1:** em cada coluna livre, `L = max(âncora − passos)` e `U = min(âncora + passos)`. Os passos
  contam 4 vizinhos e contornam o que está fora do plano. Toda superfície entre `L` e `U` tem degrau de no máximo 1 e toca
  cada âncora na altura dela.
- **Partida:** `limitar(natural, L, U)`. A coluna fica onde está, se cabe; senão sobe até `L` ou desce até `U`.
- **Alvo:** a média entre o maior relevo de degrau 1 abaixo da partida (o corte) e o menor acima dela (o aterro). Os dois
  têm degrau 1, então a média também; o paredão longe das âncoras vira encosta, metade cortada e metade aterrada, e o
  chão que já anda de 1 em 1 fica onde está (`GradePlan`).
- **Caminho plano:** a coluna do grafo toma a altura da vizinha de caminho já decidida, quando cabe no envelope. Assim o
  caminho só sobe 1 onde precisa.
- **Paredão** (decisão 3): o paredão de geração dentro da faixa de uma estrutura vira degraus pelo próprio envelope. Os
  blocos da peça nunca entram no plano.

### 2. Âncoras que não cabem em degrau 1 (decisão 2)

Quando `L > U` numa coluna, duas âncoras estão mais longe em altura do que em passos.

1. Primeiro, **rampa mais longa**: o caminho entre elas desvia em zigue-zague dentro da faixa livre até a distância
   caber na diferença de altura.
2. Sem rampa possível, **escada de blocos de escada do bioma** no caminho, só na coluna do grafo.
3. Cada caso sai em `VC_GRADE_CONFLICT` (âncoras, diferença, passos, solução).

### 3. Execução pelo Zelador (decisão 1)

- A fila é a diferença entre o alvo e o chão, nesta ordem: frente de porta, caminho, faixa das estruturas, resto.
- Corte e aterro usam a coluna segura de hoje (`GroundSmoothing.column`, com as recusas das Regras 156 e 356) e o solo
  do bioma, com areia sobre arenito no deserto (G1).
- **Orçamento pelo atraso:** `256 + pendentes / 4` colunas por dia, até 1.024.
- **Segundo Zelador:** se as colunas pendentes não caem em 3 dias seguidos, a vila abre uma segunda vaga de Zelador, até
  duas. Isto emenda o item 2 da ADR-059.
- O plano é recalculado quando o grafo, as construções ou a água mudam, no máximo uma vez por dia de jogo, e nunca a cada
  passada.

### 4. Água (decisão 4)

A margem vira praia de solo do bioma, em degraus até a água. A forma da margem segue o plano do contorno natural da vila
junto da água (`TODO.md`, PR #113), sem parede reta. A fase 5 espera a escolha do autor naquele plano.

## Fases

| Fase | O que entra | Prova |
|---|---|---|
| 1 | cálculo do plano no `core/` (sem Minecraft): âncoras, envelope, alvo, caminho plano, conflitos | teste de unidade e de propriedade (jqwik): nenhum vizinho com diferença > 1 fora dos conflitos, toda âncora na altura dela, coluna que cabe não se mexe |
| 2 | leitura do mundo para o plano (`fabric/`): alturas, âncoras, o que fica fora; `VC_GRADE` diário | GameTest: plano lido de uma arena com casa, curral, caminho e mina |
| 3 | o Zelador executa o plano, com o orçamento pelo atraso e o segundo Zelador; sai a fila de quadrados (Regra 323) e a Regra 297 na casa pronta | GameTest de acesso: depois do plano, uma busca a pé (degrau ≤ 1) do centro alcança todas as portas |
| 4 | rampa e escada nos conflitos | GameTest: duas portas a 4 de altura e 3 passos ficam ligadas por rampa ou escada |
| 5 | praia na margem, com o contorno natural | depende da escolha do plano da água |

Cada fase é um ciclo próprio: teste que falha antes, Sentinela, JAR, e o jogo do autor confirma pela linha `VC_GRADE`
(degraus pendentes caindo dia a dia, portas alcançáveis).

## Consequências

- Um só dono da altura do chão. A geração (Regra 152) continua igual; os vãos sob peça e construção (Regras 304 e 356)
  também.
- Vila antiga recebe muita terra mexida no começo. O orçamento por dia limita isso, e `VC_GRADE` mostra a curva.
- O cálculo fica no `core/`, testável sem servidor. A leitura de alturas de uma caixa de 250×250 (cerca de 62.500
  colunas) é feita uma vez por recálculo; o `VC_COST` mede antes de qualquer otimização.
- Risco: a faixa em volta das peças da vila gerada muda o chão em volta de casas que o jogo pôs. Os blocos das casas
  ficam; o chão em volta é o pedido do autor.
