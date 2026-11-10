# Pesquisa: melhorias do grafo de caminhos

Pedido do autor em 2026-11-10. **Só pesquisa: nenhum código foi alterado.** Lê o código da `main` (`ce1106a5`, 0.4.43), os Sparks `7d0PErq0RJ` e `u6XC34ZC0k` e os logs de 7 a 9 de novembro. Antes de implementar, exige ADR-050 e escolha do autor (`CLAUDE.md` §0.2.2).

## O pedido, em cinco pontos

1. Todo nó busca a linha mais próxima.
2. Todo bloco de caminho entra no cálculo do grafo.
3. As ligações são retas de um ponto a outro.
4. Trecho que fica 10 dias de jogo sem mudar trava e não é recalculado.
5. O chão do trecho fixo vira caminho; todo outro bloco de caminho, fora das linhas e dos nós, volta ao solo do bioma.

## Como o grafo é feito hoje

`RoadGraphRefresh` (fabric) lê o mundo e entrega as colunas a `RoadLines.build` (core, puro):

| Etapa | Classe | O que faz |
|---|---|---|
| Leitura | `RoadGraphRefresh.read` | A cada 6.000 tiques, ou 600 se há ligação ou calçada pendente, varre a caixa da vila a 2.048 colunas por tique. Só entra bloco de caminho dentro da caixa. |
| Faixa | `RoadMask` | Fecha vão de até 2 blocos e buraco de até 4. |
| Linha do meio | `RoadThinning` | Afina a faixa a 1 bloco (Zhang-Suen) e tira pontas de até 4 blocos. |
| Nós e trechos | `RoadChains` | Nó onde linhas se encontram; encontros a até 2 blocos viram um nó. |
| Retas | `RoadRouting` | Cada trecho vira reta ou uma dobra só, com desvio máximo da faixa e degrau de 1 bloco. |
| Quadrados | `RoadSquares` | Tira a quina que forma quadrado de nós (Regra 223). |
| Ligações | `RoadLines.stub` e `link` | Entrada solta: reta mais curta em 4 direções, até 16 blocos. Parte solta: busca A* até 64 blocos, **uma por releitura**. |
| Sobra | `RoadLines.surplus` e `PathTrim` | O caminho a até 3 blocos da linha volta ao solo, 16 colunas por passada do pedreiro. |

## O que já existe e o que falta, ponto a ponto

### 1. Nó liga à linha mais próxima: em parte

Hoje a entrada solta só liga por **reta nos 4 eixos**. Se a reta não acerta a linha em 16 blocos, cai na busca `link`, que roda **uma por releitura**. Efeito medido: `isolated` ficou entre 7 e 11 por todo o jogo da 0.4.42, e entre 1 e 9 na 0.4.43.

Falta: ligar ao **ponto mais próximo da linha**, e não só às 4 retas, e ligar todas as soltas na mesma releitura.

### 2. Todo bloco de caminho entra no cálculo: não

- A leitura só vê a caixa da vila. Caminho a até 32 blocos fora dela (a rede segue até `RoadNetwork.MARGIN`) fica de fora; só o portão de mina lê além da caixa.
- **Defeito latente:** `RoadGraphRefresh.publish` retorna sem publicar se há mais de **6.000 colunas** de caminho (`RoadNetwork.MAX_COLUMNS`). A vila de 63 minutos tinha 795, mas uma vila grande trava o grafo em silêncio, sem log.
- No save da 0.4.43 havia 344 de 795 colunas de caminho fora da linha, e 118 colunas da linha sem caminho ainda. O grafo não mostra o que ficou de fora.

### 3. Retas de ponto a ponto: o desenho é por faixa

O método atual é **da faixa para a linha**: afina o que existe e depois alinha. Isso gera desvio, o nó que muda de lugar e as oscilações vistas em jogo (202 mudanças de grafo gravadas em 2 h 27 na 0.4.43, uma a cada 45 s em média).

A alternativa é **dos nós para a linha**: pegar os pontos que importam (centro, entradas, encontros, pontas) e ligá-los por uma árvore de retas, com custo menor sobre bloco de caminho que já existe. Esse método usa todo o caminho e dá reta por construção.

### 4. Trava de 10 dias: não existe

Não há estado de "fixo". Toda releitura recalcula tudo.

**Aviso sobre o número:** 10 dias de jogo são 240.000 tiques, ou 3 h 20 min de jogo corrido. Os jogos de playtest duram de 1 a 2 h e chegam a 1 ou 2 dias. **A trava de 10 dias nunca dispararia num playtest.**

**Custo que se economiza:** pequeno. Os dois Sparks não mostram nenhum método de grafo entre os mais pesados do mod, e o ciclo da colônia custa de 8 a 22 ms. O ganho de verdade é **estabilidade**: o trecho fixo para de oscilar e de gerar pedido de calçar.

### 5. Solo do bioma para tudo fora do grafo: só perto da linha

`PathTrim` já devolve ao solo o que está **a até 3 blocos da linha** (Regra 186) e o anel antigo de plantação (Regra 208). O resto não sai. Estender para "todo bloco fora do grafo" muda o que se apaga, e traz riscos:

| Caminho fora do grafo | Risco de apagar |
|---|---|
| Rua da vila gerada (peças `straight`, `crossroad`) | Já entra no grafo, então fica; mas se o grafo não a incluir (item 2), sumiria |
| Caminho do jogador (jardim, trilha à mão) | Apaga sem aviso; a Regra 177 diz que o jogador pode corrigir o caminho |
| Caminho da casa do mineiro e da boca da mina | A boca é protegida por `WorldPathTerrain.MINE_CLEARANCE` |
| Componente solto que o grafo ainda não ligou | Apagar antes de ligar perde o que faltava só um `link` |

Hoje `PathTrim.pass` só confere que não está dentro de uma construção. Não confere se o bloco é da vila original (Regra 3) nem se é do jogador.

## Proposta de desenho

Quatro peças, que podem entrar em ordem e cada uma sozinha tem valor.

### A. Ligar sempre ao ponto mais próximo (pontos 1 e 3)
- Para cada nó solto, achar a coluna de linha mais próxima por distância de Manhattan e ligar por reta, com uma dobra só se preciso.
- Ligar todas as soltas na mesma releitura, com teto de tempo, em vez de uma por releitura.
- Fica em `core`, puro e testável. Mexe em `RoadLines.stub` e `link`.

### B. Ler todo caminho (ponto 2)
- Ler a faixa de `MARGIN` (32 blocos) fora da caixa, que `RoadNetwork` já aceita.
- Trocar o teto de 6.000 colunas, que hoje cala o grafo, por **corte por componente** com `VC_ROAD_GRAPH ... capped=`.
- Registrar no log quantas colunas de caminho ficaram fora da linha (`offLine=`), para medir o ponto 5.

### C. Trava por trecho (ponto 4)
- Cada aresta ganha o **dia em que mudou pela última vez** (`world.getTimeOfDay() / 24000`).
- Passou N dias sem mudar: a aresta fica **fixa**. A releitura pula as colunas dela e só confere que o chunk está carregado.
- Descongela quando um bloco da aresta muda no mundo (`PlayerWorldChangeHandler` e `RoadIndex.reconcileWorldChange` já avisam) ou quando uma entrada nova liga nela.
- **Onde guardar:** o codec `roadGraphs` tem versão (`RoadGraphSave.CURRENT`); o dia vai em campo novo, com leitura compatível. Sem persistir, a trava se perde ao reiniciar e recomeça a contagem.
- **Valor de N:** 10 dias é o pedido. Sugiro **3** como padrão configurável, porque 10 nunca dispara num playtest (ver ponto 4).

### D. Devolver ao solo o que está fora do grafo (ponto 5)
- Ampliar `RoadLines.surplus` de "a até 3 blocos" para "qualquer bloco de caminho dentro da caixa que não é linha, entrada nem trecho fixo", com **exceções**: dentro de construção, na folga da mina, sob estrutura da vila original e todo componente que o grafo ainda não ligou.
- Marcar o que o jogador pôs à mão: só apagar o que a colônia calçou (`RoadIndex.isLaidByAColony`) e o que a vila gerada deixou fora das linhas.
- O pedreiro continua apagando 16 colunas por passada, então o efeito aparece em dias.

## Riscos e o que a pesquisa não provou

- **Não medi** o custo da leitura do mundo por releitura: o Spark só mostra que ele não está entre os maiores. Antes de "economizar leituras", vale um `VC_COST` próprio da fase de grafo.
- **Não provei** que a oscilação de 202 mudanças venha da faixa: pode ser trabalho em curso (calçar e apagar). A peça C e a A resolvem as duas hipóteses sem decidir qual vale.
- O ponto 5 muda a Regra 186, e a peça C muda a Regra 214. Pedem ADR-050 e, para o ponto 5, decisão do autor sobre o caminho feito à mão.
- Mexer em `RoadLines` quebra testes de forma (`RoadLinesTest`, `RoadRulesGameTest`, `EntrancesAtTheDoorGameTest`): o desenho do autor para a 0.4.43 precisa continuar valendo.

## Ordem sugerida de entrega

| Passo | Peça | Risco | Prova |
|---|---|---|---|
| 1 | B, só o log e o teto honesto | baixo | `VC_ROAD_GRAPH` com `capped=` e `offLine=` |
| 2 | A: ligar ao ponto mais próximo | médio | teste puro com nó solto e GameTest com entrada fora da reta |
| 3 | C: trava por trecho | médio | GameTest com dias simulados e descongelar por bloco mudado |
| 4 | D: solo para tudo fora do grafo | alto | só depois de o passo 1 mostrar quantas colunas seriam apagadas |
