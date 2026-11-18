# ADR-057 — Três modos de montar o grafo, um ligado por vez

**Estado:** aceita pelo autor em 2026-11-18, sobre o estudo `docs/research/2026-11-18-estudo-do-grafo.md` e a pesquisa de
três formas de grafo feita na mesma conversa. Emenda a ADR-043 (como o grafo é montado) e a ADR-050 (a trava de dias vale só
no modo 1).

## Contexto

O grafo de hoje afina os blocos de caminho do mundo, e esses blocos são os que ele mesmo manda pôr e apagar: é um laço que
oscila. O perfil longo da 0.4.92 mediu 10 a 15 mudanças do grafo por dia e 11 ou 12 partes soltas nos três biomas.

Cada alternativa traz um risco próprio:
- Fixar o grafo ignora o que muda de verdade: casa nova, chão nivelado pelos aldeões e edição do jogador.
- O autor não confia num resultado que não pode comparar.

## Decisão do autor

1. **O grafo atual fica guardado como modo 1**, e entram dois modos novos atrás de uma chave. **Só um modo fica ligado
   por vez.** Ligar um desliga o outro. O modo é por mundo e fica gravado.
2. **Comando:** `/vc grafo modo` mostra o modo ligado; `/vc grafo modo <atual|regioes|vizinhanca>` liga outro.
   Exige operador.
3. **Cor das arestas por modo,** no overlay das ruas: amarelo para `atual`, laranja para `regioes` e roxo para
   `vizinhanca`. A cor dos nós continua a do papel deles (rosa, azul, branco e vermelho do centro).
4. **Modo 2, `regioes`.** É o grafo atual, refeito só onde algo mudou:
   - A caixa da vila é dividida em quadrados de 16 × 16. Cada quadrado tem uma impressão digital: os blocos de caminho,
     o planejado, as entradas e a altura do chão nas colunas lidas.
   - O quadrado cuja impressão não mudou é limpo. As linhas do grafo anterior que passam por ele entram fixas na
     montagem (o `fixed` do `RoadLines.build`), e só os quadrados sujos se ajeitam em volta.
   - Sem nenhum quadrado sujo, o grafo anterior fica como está, sem montagem.
   - **Aviso por evento:** um mixin no `WorldChunk.setBlockState` do servidor marca sujo o quadrado em que um bloco mudou
     dentro da caixa de uma colônia. Assim, chão nivelado ou estrutura nova refazem a região na releitura seguinte, sem
     esperar a impressão digital. O mixin só anota: não cancela nada e não muda o bloco.
   - Sem trava de dias: o quadrado que não muda já é estável.
5. **Modo 3, `vizinhanca`.** É plano primeiro, com voltas:
   - **Nós:** o centro e cada entrada lida, isto é, as portas das construções da colônia e da vila gerada, a boca da
     mina, o portão do curral e a quina da plantação.
   - **Quem liga a quem:** o grafo de vizinhança relativa entre os nós. Dois nós ligam se nenhum terceiro nó está mais
     perto dos dois do que eles estão um do outro. Dá voltas como numa vila de verdade, sem ligar todos com todos.
   - **Traçado:** cada ligação é traçada por A\* sobre o chão, com os custos abaixo. Proibido: passo sobre água, através de
     estrutura ou com mais de 1 bloco de degrau.

     | Passo | Custo |
     |---|---|
     | Plano | 1 |
     | Subir ou descer 1 bloco | +2 |
     | Sobre bloco de caminho que já existe | ×0,3 |
     | Curva | +0,5 |

   - **Memória:** a ligação já traçada entre o mesmo par de nós é guardada e só é refeita quando um quadrado sujo
     (o mesmo do modo 2) cruza o traçado, ou quando o par muda.
   - **Ligação que falha:** a entrada sem nenhuma ligação possível entra em `loose=` com o motivo.
6. **Trava de dias:** as Regras 225 e 306 (aresta inteira de rosa a rosa, 10 dias) valem só no modo 1.

## Como

- **Núcleo puro (`core/construction/model`):**
  - `RoadGraphMode` (os três modos e a cor da aresta);
  - `RoadRegions` (quadrados, impressão digital, sujos);
  - `NeighborhoodRoads` (vizinhança relativa e A\* com custo).
  - Os três entregam o mesmo `RoadLines.Result`. Calçamento, sobra, lote, pontas e poste não mudam.
- **Fabric:**
  - `RoadGraphRefresh.publish` escolhe o modo;
  - `GraphModeSavedData` grava o modo;
  - `GraphModeCommand` (`/vc grafo`) troca o modo;
  - `RoadMapSync` pinta a aresta com a cor do modo.
- **Mixin:** `WorldChunkBlockChangeMixin` só chama `RoadRegionEvents.changed(world, pos)`. A conta fica fora do mixin.
- **Log:** `VC_ROAD_GRAPH` ganha `mode=` e, nos modos 2 e 3, `dirty=` (quadrados refeitos).
- **Troca de modo:** o grafo da colônia é refeito do zero na releitura seguinte, e o plano de calçada em memória é zerado.
  O que já está calçado fica no mundo.

## Consequências

- Os testes de forma do grafo seguem no modo 1, que continua o padrão. Os modos 2 e 3 ganham testes próprios.
- O perfil longo compara os modos rodando a mesma semente com `-Dvillagecolony.graphMode=<modo>`.
- Enquanto o autor compara, existem três montagens. O modo que perder sai, por decisão do autor.
- **Fica para depois:**
  - a porta e o caminho do jogador no grafo (a porta térrea fora de construção registrada como entrada, e a memória do
    que a colônia calçou), que vêm numa regra própria depois dos modos;
  - a trilha de desejo (modo 4 da pesquisa).
