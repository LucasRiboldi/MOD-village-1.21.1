# ADR-051 — Fechamento do MAE: socorro da vila inteira, bairro, água interna e ruínas antigas

**Estado:** aceita pelo autor em 2026-11-10. Regras 226, 227 e 228. Emenda a ADR-049 (socorro) e a ADR-047 (regiões antigas).

## Contexto

O jogo da 0.4.43 mostrou o socorro disparando 29 s antes de a busca normal abrir uma obra: a conta somava as voltas sem lote de
tipos diferentes de obra. Faltavam também quatro decisões do autor: a água sob a obra pedida, as ruínas de saves antigos, o
bairro novo do plano mestre e a recusa de `/vc mae obra`, que mostrava só o motivo do último lote.

## Decisão do autor

1. **Água (item 12):** a obra pedida pelo jogador aterra a água da zona interna da vila; mar e rio aberto, não.
2. **Ruínas (item 13):** limpar as obras de saves antigos.
3. **Bairro (item 14):** "aplique a melhoria que considerar mais natural".
4. **Socorro e recusas (item 2):** corrigir.

## Como

- **Socorro da vila inteira (Regra 226):** só conta a volta sem lote da casa (`LotSeeking.callsTheRescue`). A oficina sem lote já
  sai da vez por meia hora (`LotTypeRefusals`), e com todos os tipos fora a vez é da casa: se nem ela cabe, a vila inteira ficou
  sem lugar.
- **Bairro (Regra 226), a forma mais natural escolhida:** a obra seguinte do socorro fica do mesmo lado da anterior e o mais perto
  dela, e não do centro. As casas se juntam num bairro, e o grafo liga as portas (Regra 214). Lado sem lote solta o bairro, e a
  vez passa ao lado seguinte. O bairro fica na memória do servidor: reiniciar recomeça pelo centro.
- **Recusa completa (Regra 226):** `/vc mae obra` sem lote diz cada motivo e quantos lotes recusou, como em
  `no_lot:occupied×5,no_base×2`. O tempo de cada pedido vai para o log (`ms=` em `queued` e `refused`), para medir a travada antiga
  de 2,5 s antes de mexer nela.
- **Água interna (Regra 227):** água sob o lote pedido pelo jogador, ligada pela superfície a no máximo 512 colunas e toda dentro da
  caixa da vila, é aterrada com o chão do bioma (`MaeWater`). Se a água sai da caixa ou passa disso, o lote é recusado com
  `open_water`. A obra automática, sem dono, segue recusando toda água.
- **Ruínas (Regra 228):** a região antiga que não volta para a fila (colônia sumiu, ou a fila recusou) tem a pegada limpa
  (`MaeRuins`): sai só o bloco igual ao da planta naquela posição. Com chunk descarregado, a região espera no save até ele carregar.
- **Prova por seção:** item fechado sem código. A Regra 202, revogada pela ADR-047, tirou a prova por seção da obra pedida.

## Consequências

- O socorro dispara menos: só quando nem casa cabe.
- **Limite das ruínas:** saves que já abriram a 0.4.38 ou depois perderam as regiões antigas; a ruína deles não tem como ser achada
  pelo mod.
- **Limite da água:** só a água entre o piso e 8 blocos abaixo dele, na pegada do lote, entra na conta. Água vizinha fora da pegada
  fica.
- **Não medido em jogo:** frequência do socorro, desenho do bairro, aterro de lago e custo de `/vc mae obra`.
