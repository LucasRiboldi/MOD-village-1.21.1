# Plano de alturas — a vila inteira em degraus de 1

**Pesquisa de 2026-11-25** para a escolha G5 🅲 do autor, no jogo da 0.4.136 no deserto. **Decisões do autor tomadas em 25-11; ADR-063.** Pesquisa anterior do mesmo tema, sobre por que a vila não crescia: [`terraplanagem-da-vila.md`](terraplanagem-da-vila.md)
(09-11).

## O pedido

> o curral grande deve sempre ter perímetros de blocos de solo do bioma em volta dele, de modo a criar a estrutura de
> escadaria de um bloco de diferença de todos os lados (ideia para que todas as estruturas sejam acessíveis por todos os
> aldeões) e esse plano de nivelamento também se aplica a qualquer estrutura da vila, a vila inteira deve ser nivelada de
> modo que todos os aldeões possam caminhar por toda ela e ter acesso a todas as estruturas, tentando deixar planos os
> caminhos o máximo que der; não podendo, forma-se um degrau, ou mais se precisar.

As imagens do autor estão em `reports/imagens-autor/2026-11-25/jogo-0136/`: img1 é o curral grande de hoje, sobre um
paredão de pedra e areia; img7 e img8 são o antes e o depois do nivelamento desejado; img9 é a altura de acesso do
curral.

## O que existe hoje

| Regra | Quem | O que faz | Limite |
|---|---|---|---|
| 152 | geração | alisa o chão da vila antes das peças, degrau de até 1 | só na geração |
| 297 | Zelador | janela de 12 em volta da obra nova, na altura da rua | só em volta de cada obra |
| 323 | Zelador | fila de quadrados 16×16 da caixa, primeiro buraco e bloco isolado, depois degrau > 1 | 256 colunas por dia |
| 304, 356 | Zelador | vão sob peça da vila e construção da colônia | só embaixo |

Cada sistema olha uma janela e decide sozinho a altura de cada coluna. Nenhum sabe a altura que a vila inteira deveria
ter, e um desfaz o outro: em volta da casa do mineiro, a Regra 297 cortou 573 blocos para a altura da rua, e a fila da
Regra 323 tapou depois a coluna "isolada".

**O número do jogo.** Os degraus maiores que 1 da Regra 323 subiram de **518 no dia 1 para 1.909 no dia 29**. O
Zelador nivelou 620, 376, 225 e 1 coluna nos dias 26 a 29, e passou boa parte do dia calçando (1.823 ações de calçar
contra 544 de nivelar). A vila cresce mais depressa do que ele nivela.

## A ideia central: uma altura-alvo para cada coluna, calculada de uma vez

Em vez de cada janela decidir, a colônia calcula **um plano de alturas** para toda a caixa da vila, e o Zelador só
executa o plano.

1. **Âncoras fixas.** São colunas cuja altura não se discute: o piso na frente de cada porta (a entrada do grafo), a
   faixa de solo em volta de cada construção na altura do piso dela (o perímetro que o autor pediu, 1 bloco de largura,
   2 no curral grande), a borda da caixa no chão natural e a água.
2. **O envelope de degrau 1.** Para cada coluna, o alvo mais baixo permitido é `L = max(âncora − distância)` e o mais
   alto é `U = min(âncora + distância)`, com distância em passos de 4 vizinhos. Toda superfície entre `L` e `U` tem
   degraus de no máximo 1 e toca cada âncora na altura dela.
3. **O alvo.** `alvo = limitar(natural, L, U)`: a coluna fica onde está, se cabe; senão sobe até `L` ou desce até `U`.
   Isso move o mínimo de terra para o degrau 1 caber. Com a Regra 297 hoje, a janela inteira vai para a altura da rua.
4. **Caminho plano.** A coluna do grafo ganha um alvo próprio: a altura da vizinha de caminho mais próxima, quando cabe
   no envelope. O caminho fica plano onde der e sobe 1 por vez onde não der.
5. **Âncoras incompatíveis.** Duas âncoras mais distantes em altura do que em passos (porta em y 70 a 3 blocos de outra
   em y 64) não cabem em degrau 1. A linha entre elas recebe escada de blocos de escada do bioma (só caminho), ou fica
   marcada `VC_GRADE_CONFLICT` para o autor ver.

O cálculo é aritmética de grade (duas varreduras de distância), sem Minecraft: cabe no `core/`, com teste de unidade e
de propriedade (jqwik) que prova "nenhum vizinho com diferença > 1 e toda âncora na altura dela".

## Execução pelo Zelador

- A fila deixa de ser por quadrado e passa a ser por **diferença entre o alvo e o chão**, em ordem: frente de porta,
  caminho, perímetro das construções, resto.
- Corte e aterro usam a mesma coluna segura de hoje (`GroundSmoothing.column` com as recusas da Regra 356) e o solo do
  bioma (escolha G1 🅲): areia sobre arenito no deserto, para não cair.
- O plano é recalculado quando o grafo ou a lista de construções muda, e nunca a cada passada.

## Como medir (o 🅲 do autor)

- **GameTest de acesso:** numa arena com relevo, depois do plano executado, uma busca a pé (degrau ≤ 1) a partir do
  centro alcança todas as portas.
- **Linha `VC_GRADE` por dia:** colunas fora do alvo, degraus > 1, portas alcançáveis e não alcançáveis, blocos
  cortados e aterrados. A meta é a contagem de degraus cair dia a dia; o `game_report.py` passa a mostrar a curva.
- **Simulação longa:** o perfil `longo` do Sentinela compara degraus e portas alcançáveis nos três biomas.

## Decisões do autor (2026-11-25)

1. **Orçamento cresce com o atraso (🅲).** O Zelador nivela 256 colunas por dia mais um tanto proporcional aos degraus
   pendentes, até 1.024. Se o atraso não cai em 3 dias, a vila contrata um segundo Zelador. A linha `VC_GRADE` mostra a
   curva por dia.
2. **Rampa, depois escada (🅲).** Entre âncoras que não cabem em degrau 1, primeiro uma rampa mais longa, desviando dentro
   da folga (zigue-zague). Só sem rampa possível, escada de blocos de escada do bioma no caminho. Cada caso sai em
   `VC_GRADE_CONFLICT`.
3. **Tudo, e arruma o paredão (🅲).** A faixa vale em volta de toda estrutura, também das peças da vila gerada, e o
   paredão que a geração deixou dentro da faixa vira degraus. Os blocos das peças nunca são mexidos.
4. **Praia com o contorno natural (🅲).** A margem vira praia de solo do bioma em degraus até a água. Isso se junta ao
   plano do contorno natural da vila junto da água (`TODO.md`, PR #113), sem parede reta.

## Riscos

- **Mexe em muita terra no começo.** A primeira execução de uma vila antiga pode cortar e aterrar centenas de blocos. O
  orçamento por dia limita isso.
- **Disputa com a obra aberta.** O lote da obra aberta fica fora do plano até ela terminar, como hoje (Regras 297 e 356).
- **Desempenho.** O cálculo é O(colunas) por âncora com fila de distância. Uma caixa de 250×250 tem cerca de 62.500
  colunas, o que cabe em poucos milissegundos fora do tique. O `VC_COST` mede antes de qualquer otimização.

## Próximo passo

Com as 4 decisões acima: uma ADR (plano de alturas substitui a fila de quadrados da Regra 323),
a regra nova, o cálculo no `core/` com testes, e depois a execução pelo Zelador com o GameTest de acesso.
