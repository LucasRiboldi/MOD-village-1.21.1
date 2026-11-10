# ADR-050 — Grafo estável: todo caminho conta, toda parte liga em reta e o trecho que ficou trava

**Estado:** aceita pelo autor em 2026-11-10, sobre a pesquisa `docs/research/2026-11-10-melhorias-do-grafo.md`. Regras 224 e 225. Emenda a ADR-043 (persistência do grafo) e a ADR-048.

## Contexto

O grafo era refeito do zero a cada releitura, a partir da faixa de caminho afinada. Três problemas:

- A releitura só via a caixa da vila, e com mais de 6.000 colunas de caminho o grafo deixava de ser publicado, sem log.
- Parte solta só ligava pela reta dos 4 eixos a partir de uma porta, ou pela busca cara, uma por releitura. `isolated` ficou entre 1 e 11 nos jogos de 7 de novembro.
- O grafo mudou 202 vezes em 2 h 27 de jogo: nada ficava pronto.

## Decisão do autor

1. **Seguir a ordem da pesquisa:** primeiro medir e não calar o grafo; depois ligar ao ponto mais próximo; depois a trava.
2. **A trava é longa, para ser a versão final:** 10 dias de jogo sem mudar. O autor recusou a sugestão de 3 dias.
3. **O solo do bioma para todo caminho fora do grafo fica para depois** do jogo que medir `offLine`.

## Como

- **Todo caminho (Regra 224):** a releitura guarda também o caminho até 32 blocos fora da caixa; entra no grafo só o que encosta, passo a passo, no de dentro (`RoadCells.touching`). Acima de 6.000 colunas, ficam as mais perto do centro (`RoadCells.nearest`), e o corte vai para o log.
- **Reta ao ponto mais próximo (Regra 224):** toda parte solta tenta antes uma reta em L até o ponto mais perto da linha, nos 8 pares mais próximos e até 64 blocos (`RoadLinks.straight`). Não gasta a busca cara, então todas ligam na mesma releitura. Sem reta possível, seguem o ramal de porta e a busca, como antes.
- **Trava (Regra 225):** cada coluna da linha guarda o dia de jogo em que entrou (`RoadSettling`). Com 10 dias, fica fixa: o afinamento, o corte de quadrado e o de ponta não a tiram, e ela volta à linha depois do traçado. O resto se ajeita em volta, e a linha paralela sai pelo corte de quadrado.
- **Destrava:** a coluna fixa sem bloco de caminho no mundo perde a trava e recomeça a conta. Sair da linha também zera.
- **Dia:** `world.getTimeOfDay() / 24000`, o relógio de jogo, em que dormir conta.
- **Persistência:** o dia de cada coluna vai no payload `roadGraphs` (chave `settled`, triplas x, z, dia), na mesma versão 1. Payload sem a chave lê como "nenhuma trava". Trocar o grafo preserva a trava.
- **Log:** `VC_ROAD_GRAPH` ganhou `offLine` (caminho lido fora da linha), `capped` (cortado pelo teto) e `fixed` (colunas travadas).

## Consequências

- **Leitura:** a releitura continua lendo o mundo inteiro da caixa, porque é ela que vê o caminho que sumiu. A economia está no recálculo: o trecho fixo não se move nem gera pedido de calçar. Os Sparks de 7 de novembro não mostram o grafo entre os custos mais altos do mod.
- **Desenho:** mais partes ligadas mudam o traçado de vários cenários do `RoadGeometryPinTest`; o registro foi refeito depois de conferir que as invariantes seguem valendo.
- **Limite:** obra demolida não solta o ramal fixo enquanto o bloco de caminho dele existir no mundo.
- **Não medido em jogo:** quanto `offLine` vale numa vila de verdade, quantas partes passam a ligar e se o grafo para de oscilar.
