# ADR-049 — O socorro do MAE: a vila sem lote pede a própria obra

**Estado:** aceita pelo autor em 2026-11-06. Emenda o plano mestre do MAE (§0.2 item 10 e Fase 6) e a ADR-047. Regra 222.

## Contexto

Depois da ADR-047, a obra MAE só nascia por `/vc mae obra`. A Fase 6 do plano mestre pedia que a vila coordenasse área, terreno, estrutura
e caminho no próprio ciclo. A vila já tem busca de lote, e depois das ADR-047 e ADR-048 o construtor levanta toda obra e o grafo traça
todo caminho: o que o MAE ainda acrescenta é achar lote sem exigir beira de rua, e limpar as árvores dele.

## Decisão do autor

1. **Só no socorro.** A busca normal continua. O MAE entra quando a volta inteira termina sem lote e a rua não abriu um.
2. **Só avisa.** A obra começa sem aprovação, com aviso no chat, e pode ser cancelada por `/vc mae parar` ou pela tocha das almas. Revoga
   para este caso o modo `supervised` do §0.2 item 10.
3. **Até 24 blocos da borda da caixa da vila.**
4. **Lote que toca água é recusado.** A obra automática nunca aterra água. Para a obra pedida pelo jogador, a regra da água segue aberta.

## Como

- `LotSeeking` conta a volta sem lote em `MaeRescue`, e a volta que achou lote zera a conta. A primeira volta é da rua (Regra 15); o socorro
  começa na segunda.
- Cada passada olha um lado da caixa, em rodízio, e confere no máximo 2 plantas e 4 lotes, porque roda no tique do servidor.
- O pedido é o mesmo de `/vc mae obra`, sem dono, com a porta virada para a vila, e passa pela mesma conferência (`MaeSites.start`).
- Roça fica de fora: precisa ficar ao alcance do fazendeiro.
- Um pedido de cada vez por colônia, e vila sem caixa medida não pede.
- O que dura é a fila, já gravada no save. A conta de voltas e o lado da vez ficam na memória: reiniciar só recomeça a conta.
- `VC_MAE_SITE result=rescue` diz o lado, se achou e quanto custou em milissegundos.

## Consequências

- Continuam existindo duas formas de achar lote, por escolha do autor (opção de menor risco).
- A construção pode nascer sem rua perto; quem liga a porta à rede é o grafo (Regra 214).
- A limpeza de árvores do lote é a mesma da Regra 200, e o lote sem árvore vem primeiro.
- Fica fora desta decisão o "bairro novo" do plano mestre: área reservada com várias construções e caminho comum.
- Não medido em jogo: quantas vezes o socorro dispara, o custo em terreno real e se a vila fica bem desenhada com ele.
