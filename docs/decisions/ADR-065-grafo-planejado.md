# ADR-065 — O grafo planejado: um modo só, árvore que cresce, aresta com estado

**Estado:** aceita pelo autor em 2026-11-26, sobre a pesquisa `docs/research/2026-11-26-grafo-de-um-modo.md` (L4 do jogo
da 0.4.150). Implementação em etapas (§ Ordem), com um jogo do autor entre elas. Substitui a ADR-057. Emenda a ADR-050 (a
trava de 10 dias sai), a ADR-052 (a ponta deixa de andar 1 bloco por dia) e a ADR-043 (o grafo deixa de ser relido do
mundo depois da importação).

## Contexto

O grafo de hoje lê os blocos de caminho do mundo, afina em linhas e remonta a rede a cada leitura. Esses blocos são os
que ele mesmo manda pôr e apagar: é um laço que oscila (estudo de 18-11, §3.1). A ADR-057 abriu três modos para comparar.
As medidas:

- **Bancada de 18-11, 3 dias por bioma:**
  - `atual` muda 16 a 20 vezes por dia, com 7 a 12 partes soltas;
  - `regioes` muda 20 a 22 vezes por dia, com as mesmas partes soltas;
  - `vizinhanca` muda 4 a 11 vezes por dia, sem parte solta, mas com 3 a 8 vezes mais colunas a calçar.
- **Jogo da 0.4.150, no `atual`:**
  - 13 mudanças por dia;
  - de 509 colunas calçadas pelo mod, 266 viraram grama, e de 174 do jogador, 86, sem nenhuma troca no diário dos blocos;
  - o Zelador calçava o que a linha mandava e, quando a linha andava, a limpeza apagava (L1, L2).

O mod RoadArchitect não oscila porque não relê o mundo:
- o nó nasce quando a estrutura é achada;
- cada aresta guarda o traçado e um estado (nova, pronta, falhou);
- tudo fica no save.

## Decisão do autor

1. **Um modo só.** Saem os modos `atual` e `regioes`, a chave `/vc grafo modo` e as cores de aresta por modo. A base é o
   modo `vizinhanca` (`NeighborhoodRoads`): nós semânticos, traçado por A* com custo e o aviso de bloco mudado por mixin.
2. **Nós:**
   - o centro;
   - cada entrada: porta de construção da colônia, peça da vila gerada, boca da mina, portão do curral, quina da plantação
     e porta do jogador (Regra 321);
   - as pontas do §5.
3. **Quem liga a quem: árvore que cresce.** Cada nó novo liga ao ponto mais perto da rede que já existe, e não a outro nó
   solto. Na fundação, as entradas da vila gerada entram pela ordem da distância ao centro. Não há laço planejado; o
   caminho que já existe no mundo, da vila gerada ou do jogador, entra como chão barato no A* e pode virar trecho da rede.
4. **Traçado:**
   - o A* de hoje: plano 1, degrau de 1 bloco +2, caminho existente ×0,3, curva +0,5;
   - proibido: água, construção, degrau maior que 1;
   - com cache de altura do chão por montagem.
5. **Pontas: só por recusa de lote.** Uma ponta nasce ou avança de um lado da caixa só quando a busca de lote fecha a volta
   sem lote daquele lado. Sem recusa, a rede não cresce sozinha.
6. **Estado de cada aresta:**

   | Estado | Quer dizer | Quem muda |
   |---|---|---|
   | `PLANEJADA` | traçado feito, ainda pode mudar | a montagem |
   | `TRAVADA` | traçado igual em 3 leituras seguidas e chão conferido; não muda mais sem evento | a montagem |
   | `CALÇADA` | toda coluna tem caminho no mundo | a leitura do mundo |
   | `FALHOU` | sem traçado, com o motivo: água, construção, sem chão, longe demais | a montagem |
   | `NA_FILA` | ainda não traçada nesta montagem (teto por leitura) | a montagem |

   `NA_FILA` não é falha: o log separa os dois (`loose=` só para `FALHOU`, com o motivo).
7. **Quando o grafo muda: só por evento.**
   - Eventos: construção nova ou demolida, entrada nova ou retirada, bloco mudado no traçado (o aviso do mixin
     `WorldChunkBlockChangeMixin`, que já existe), ou recusa de lote do §5.
   - Fora disso, a leitura do mundo só atualiza o estado: está calçada, ou algo bloqueou.
   - A aresta `TRAVADA` bloqueada volta a `PLANEJADA` e é traçada de novo.
8. **Guardado no save** (`RoadGraphStore`): as arestas, o traçado, o estado e a contagem de leituras iguais. Reabrir o mundo
   não muda o grafo.
9. **Zelador e limpeza** (pedidos L1 e L2 do autor):
   - o Zelador só calça aresta `TRAVADA`;
   - a limpeza só devolve ao solo o caminho que a colônia pôs, que está fora de toda aresta há pelo menos 2 dias de jogo;
     nunca o da vila gerada nem o do jogador;
   - toda troca dela vai para o diário dos blocos (Regra 384).
10. **Migração:** na primeira carga de um mundo com o grafo antigo, ele é importado uma vez. A aresta toda calçada entra
    `CALÇADA` e trava; a outra entra `PLANEJADA`. Nada calçado é apagado na troca.

## A fonte da verdade

O invariante do `AGENTS.md` diz: "o mundo é a fonte da verdade: não persista estado que o Minecraft já guarda". O
traçado planejado e o estado da aresta são intenção da colônia, que o Minecraft não guarda, então guardá-los cumpre o
invariante. O estado físico (calçado ou não, bloqueado ou não) continua lido do mundo.

## Consequências

- **Sai:** o afinamento e o que depende dele:
  - `RoadThinning`, `RoadChains`, `RoadRouting`, `RoadMask`, `RoadSquares`, `RoadSettling` e boa parte de `RoadLines`;
  - `RoadRegions` como modo; a impressão digital fica só se servir de evento;
  - a trava de 10 dias (Regras 225 e 306);
  - cerca de 2.000 linhas do núcleo, e os testes de forma (`RoadGeometryPinTest` e vizinhos) saem ou mudam.
- **Regras afetadas:**
  - a emendar: 177 (deixa de ser afinamento), 214, 216, 223 a 225, 229, 230, 306 e 319 a 322;
  - a criar: uma regra por etapa.
- **Contrato, que vira teste e medida:**
  1. Toda entrada está ligada ao centro, ou `FALHOU` com motivo no log.
  2. Nenhuma aresta passa sobre água, através de construção ou com degrau maior que 1.
  3. Numa vila parada, no máximo 3 mudanças de grafo por dia (`changes=`); hoje são 13 a 22.
  4. Nenhum caminho da vila gerada ou do jogador é apagado; nenhuma coluna calçada é apagada antes de 2 dias fora de
     toda aresta.
  5. Reabrir o mundo não muda o grafo.
- **Risco:** é a maior troca do núcleo desde a ADR-043. Por isso as etapas 1 a 3 correm dentro do modo `vizinhanca`, com
  os outros dois modos ainda de pé, e só a etapa 4 os tira.

## Ordem

| Etapa | O quê | Prova |
|---|---|---|
| 1 | Estado por aresta, traçado guardado no save, `NA_FILA` separado de `FALHOU` (dentro do `vizinhanca`) | GameTest que reabre o mundo e o grafo fica igual |
| 2 | O Zelador só calça `TRAVADA`; a limpeza com os 2 dias, sem o caminho do jogador, com diário | GameTest com caminho do jogador que fica |
| 3 | Árvore que cresce no lugar da vizinhança relativa; ponta só por recusa de lote | Teste de propriedade: toda entrada ligada ou com motivo |
| 4 | Migração do mundo antigo; o `vizinhanca` vira o único modo; saem `atual`, `regioes` e a chave | GameTest de mundo antigo carregado |
| 5 | Perfil longo nos 3 biomas contra a bancada de 18-11 | `changes=` ≤ 3 por dia numa vila parada |

Cada etapa sai numa versão própria e espera o jogo do autor antes da seguinte.
