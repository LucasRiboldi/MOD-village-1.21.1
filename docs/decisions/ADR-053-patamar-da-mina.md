# ADR-053 — O patamar nas dobras da escada da mina

**Estado:** aceita pelo autor em 2026-11-12. Regra 254. Emenda a Regra 119 e a luz da mina (`MineLighting`).

## Contexto

Na Regra 119, cada nível desce por um caracol de 4 lances de 3 degraus, com três pistas de largura e três de altura. Na dobra entre
dois lances não há patamar: o último degrau de um lance tem 1 bloco de fundo e o lance seguinte já desce. No jogo da 0.4.57 o autor
pediu a dobra com 3 blocos de profundidade e uma tocha na parede.

## Decisão do autor

1. Cada dobra ganha um patamar plano de 3 x 3, na altura do último degrau do lance, com uma tocha na parede de fora.
2. Só os níveis novos: o nível que já está sendo cavado termina no formato antigo; o próximo nível que a mina abrir já sai com patamar.

## Como

- **Geometria (`MineShaft`):** o patamar ocupa as duas fileiras de rocha além do último degrau (`LANDING_ROWS = 2`), nas mesmas três
  pistas e na altura dele. Com o degrau, são 3 de fundo. Essas colunas já eram rocha sem uso, e o lance seguinte encosta nelas: nenhuma
  célula muda de lugar.
- **Ordem de escavação:** o patamar vem no fim do ramal, depois do salão dele (`ARM_END`). O ramal zero cava os patamares do caracol do
  nível e os do seu caracol; os outros ramais, só os do seu. São 270 blocos a mais por nível (de 1.755 para 2.025).
- **Formato por nível:** `MineShaft.landings`. `from` e `deepened` abrem com patamar; `turned` e `rerouted` mantêm o formato; a rampa do
  fundo não tem. O save grava `landings`; ausente é falso. Fica fora do `SHAPE_VERSION`, porque nenhuma célula antiga mudou e a versão
  de forma zeraria o cursor.
- **Tocha (`MineLighting`):** uma por patamar, na pista do meio, à altura da cabeça, presa na rocha de fora da dobra. É a única tocha de
  parede da mina: o `MineShaftLayoutTest` prova que nenhum ramal deste nível nem do seguinte cava essa rocha, e o trabalho lateral
  (`MineSideWork`) a protege como protege os degraus.
- **Escada protegida:** os conjuntos de degraus de `MineBranches` e `MineSideWork` incluem o patamar.

## Consequências

- O nível fica 15% maior. O teto de 1.800 blocos da Regra 119 vale para o traçado sem patamar; o patamar é acréscimo pedido pelo autor.
- Minas abertas antes desta versão seguem sem patamar até descerem um nível.
- Falta prova em jogo: o mineiro cavar o patamar no fim do ramal e a tocha aparecer na parede.
