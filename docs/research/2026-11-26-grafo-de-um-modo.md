# O grafo em um modo só — pesquisa de 2026-11-26

Pedido do autor (L4 do jogo da 0.4.150): "tem que ser avaliado o funcionamento do grafo, compilar os 3 tipos em um só, tem
que acontecer uma pesquisa profunda de como melhorar o funcionamento do grafo". Base: o estudo de 18-11
(`2026-11-18-estudo-do-grafo.md`), a ADR-057, a bancada de 18-11 (`run/biome-sim/reports/*@*`), o jogo da 0.4.150 e o
código do mod RoadArchitect. Nada aqui foi implementado; as decisões do §7 são do autor.

## 1. O que já estava decidido e o que foi feito

- **Estudo de 18-11:** apontou a causa raiz. O grafo lê os blocos de caminho do mundo, e esses blocos são os que ele
  mesmo manda pôr e apagar: é um laço que oscila. Recomendou o "plano primeiro" como destino.
- **ADR-057:** em vez de escolher, criou três modos para comparar: `atual` (o laço), `regioes` (o laço refeito só nos
  quadrados que mudaram) e `vizinhanca` (plano primeiro, com A*). "O modo que perder sai, por decisão do autor."
- **Ordem do estudo (§9), o que andou:**

  | Passo | Estado |
  |---|---|
  | 1. A limpeza só apaga o que a colônia calçou | **Parcial.** A Regra 316 poupa a rua da vila gerada; o caminho do jogador ainda sai (`PathTrim.keep` só olha `isVillageOriginal`) |
  | 2. Medir mudanças do grafo por dia | Feito (`changes=`, Regra 317) |
  | 3. Importação única, histerese, trava por releitura | Só a trava por releitura (Regra 322, em memória) |
  | 4. A ponta cresce por recusa de lote | Não feito |
  | 5. ADR do plano primeiro | Não feito |

## 2. As medidas

`changes=` conta as mudanças do grafo no dia e zera no dia seguinte; a linha `VC_ROAD_GRAPH` só sai quando o grafo muda.

**Bancada de 18-11** (0.4.92, mesma semente, 3 dias por alvo):

| Alvo | Mudanças por dia | Partes soltas (média) | Arestas no fim | A calçar no fim | Portas sem ligação no fim |
|---|---|---|---|---|---|
| planície, `atual` | 20 | 11,8 | 53 | 26 | 14 |
| planície, `regioes` | 20 | 11,4 | 55 | 37 | 13 |
| planície, `vizinhanca` | 8 | 0 | 86 | 199 | 0 |
| taiga, `atual` | 16 | 7,5 | 46 | 25 | 9 |
| taiga, `regioes` | 20 | 7,0 | 34 | 111 | 12 |
| taiga, `vizinhanca` | 11 | 0 | 70 | 200 | 4 |
| deserto, `regioes` | 22 | 6,6 | 30 | 24 | 14 |
| deserto, `vizinhanca` | 4 | 0 | 14 | 21 | 7 |

**Jogo da 0.4.150** (planície 18cd007a):

- `atual`, 18:15 a 20:37: 107 mudanças em cerca de 8 dias de jogo (13 por dia), 11 partes soltas em média.
- `regioes`, 20:37 a 20:39: 4 mudanças. `vizinhanca`, 20:39 a 20:44: 8 mudanças, as arestas subiram de 18 para 64.
- As **30 portas `no-route`** do fim do jogo (nota 6) falharam de verdade. A ligação das partes soltas (`connect`) não
  tem teto de vez, então nenhuma estava na fila: o A* não achou passagem com degrau de no máximo 1 nessa vila de morro,
  dentro do teto de 8.000 colunas. E cada falha era tentada de novo em toda leitura, 3 vezes por porta. Correção de
  2026-11-26: a primeira versão desta pesquisa dizia que era fila.
- **Efeito no chão** (diário dos blocos, Regra 384): de 509 colunas que o mod calçou, 266 estão grama no save; de 174 do
  jogador, 86. Nenhuma dessas trocas está no diário: a limpeza (`PathTrim`) não registra. O nivelamento cortou 29 colunas
  recém-calçadas.

**Leitura:** o `regioes` não resolve o laço; só o espalha em quadrados, e muda tanto quanto o `atual` ou mais. O
`vizinhanca` muda menos e não deixa parte solta, mas pede 3 a 8 vezes mais calçada e ainda não está pronto para jogo
(§3.3).

## 3. Os três modos, por dentro

### 3.1 `atual` (`RoadLines` e companhia, cerca de 2.000 linhas do núcleo)
Lê o caminho do mundo, fecha a faixa, afina em linhas, acha nós e liga as partes soltas em reta (até 64 blocos, 128 para a
mina). **Forte:** respeita a rua que existe. **Fraco:** a fonte da verdade é o produto dele mesmo; o afinamento muda com
um bloco; a trava de 10 dias não alcança o playtest; a porta longe da linha fica solta.

### 3.2 `regioes` (`RoadRegions`, 181 linhas)
O `atual` refeito só no quadrado de 16 × 16 cuja impressão mudou, com aviso de bloco mudado por mixin. **Forte:** o aviso
por evento (`WorldChunkBlockChangeMixin` → `RoadRegionEvents`) é a peça certa para saber quando algo mudou de verdade.
**Fraco:** herda o laço inteiro.

### 3.3 `vizinhanca` (`NeighborhoodRoads`, 441 linhas)
Nós semânticos (centro, entradas, pontas); liga dois nós se nenhum terceiro está mais perto dos dois (vizinhança
relativa, até 96 blocos); traça cada ligação por A* com custo (plano 1, degrau +2, caminho existente ×0,3, curva +0,5;
água, construção e degrau maior que 1 proibidos); guarda a ligação por par e só a refaz quando um quadrado sujo a cruza.
**Forte:** é o "plano primeiro" do estudo, e liga toda porta. **Fraco, hoje:**

1. As ligações ficam **só na memória** (`RoadGraphBuilds.ROUTES`): ao reabrir o mundo, tudo é traçado de novo.
2. A vizinhança relativa dá voltas: na planície, 86 arestas contra 53, e 199 colunas a calçar contra 26.
3. A falha não fica guardada: a porta sem passagem é tentada de novo em toda leitura, sem motivo no log.
4. Não há estado por aresta: o Zelador calça o que ficou igual por 3 leituras (Regra 322), contagem também em memória.

## 4. O mod RoadArchitect

[Shadscure/RoadArchitect](https://github.com/Shadscure/RoadArchitect), branch `1.21.1-multiloader`, Apache-2.0. Liga
estruturas do mundo (vilas, postos) com estradas.

- **Nó** (`storage/components/Node.java`): `id`, posição e tipo da estrutura. Nasce por evento, quando a estrutura é achada;
  o grafo nunca é relido do mundo.
- **Aresta** (`EdgeStorage`): o par de nós e um estado `NEW → SUCCESS | FAILURE`. Liga quando o par está dentro do raio e
  o segmento **não cruza aresta existente** (`RoadGraphState.connect`).
- **Caminho** (`PathStorage`): a lista de blocos de cada aresta e um estado `PENDING → PROCESSING → READY | FAILED`. O
  caminho é construído quando o chunk dele carrega (`getPendingForChunk`).
- **Persistência:** os três vão para o `SavedData` do mundo.
- **Traçado:** A* com custo de bioma e de estabilidade do terreno, e cache de altura, estabilidade e bioma
  (`PathFinder`, `CacheManager`); aceita caminho parcial por configuração.
- **API de extensões:** vistas só de leitura do grafo (`RoadGraphView`, `NodeView`, `EdgeView`, `PathView`).

**Lições para o nosso:**

1. **A intenção fica guardada, e o mundo só executa.** O grafo dele não oscila porque não se relê.
2. **Estado por aresta.** A pergunta "posso calçar?" vira "a aresta está pronta para obra?", que é exatamente o pedido do
   L2 ("só calça caminho travado em definitivo").
3. **A falha é um estado**, com motivo, e não uma nova tentativa a cada leitura.
4. **Cache de terreno** na busca, que o nosso A* não tem.

**O que não serve:**

- integrar pela API: cria dependência de outro mod, e a escala dele é entre vilas, não dentro de uma;
- copiar código: ele usa os nomes da Mojang, e o nosso usa os do Yarn;
- "liga todos dentro do raio": dentro de uma vila isso vira malha demais.

**Literatura (GDMC):**

- As boas entradas usam árvore geradora mínima para decidir quem liga a quem, e A* para o traçado.
- Na vila que cresce, cada construção nova liga à rede que já existe.
- O nó inalcançável espera e é tentado de novo depois que a rede cresce.

## 5. A proposta: um modo só, o grafo planejado

Base: o `vizinhanca`, que já é plano primeiro e já tem A*, custo e quadrados sujos, completado com o que falta.

| Peça | Como fica | De onde vem |
|---|---|---|
| Nós | centro, cada entrada (construção, vila gerada, mina, curral, plantação, porta do jogador) | `vizinhanca` |
| Quem liga | **decisão do autor (§7a)**: árvore que cresce (cada nó novo liga ao ponto mais perto da rede) ou vizinhança relativa | GDMC / `vizinhanca` |
| Traçado | A* de hoje, com caminho existente barato (aproveita a rua da vila gerada e a do jogador) e cache de altura | `vizinhanca` + RoadArchitect |
| Estado da aresta | `PLANEJADA → TRAVADA → CALÇADA`; `FALHOU` com motivo (água, construção, sem chão, fila) | RoadArchitect |
| Quando muda | só por evento: construção nova ou demolida, porta nova, bloco mudado no traçado (o aviso do mixin) | `regioes` |
| Guardado | as arestas, o traçado e o estado no save (`RoadGraphStore`) | RoadArchitect |
| Zelador | só calça aresta `TRAVADA` (L2) | pedido do autor |
| Limpeza | só apaga caminho que a colônia pôs, fora de toda aresta, depois de N dias, e registra no diário (L1) | estudo §3.4 + L1 |
| Pontas | **decisão do autor (§7b)** | estudo §3.5 |

**Quando a aresta trava:** o traçado igual por 3 leituras seguidas e o chão do traçado conferido. Depois disso só um
evento a destrava.

**Migração:** na primeira carga de um mundo antigo, o grafo atual é importado uma vez; as arestas já calçadas entram
`CALÇADA`, as outras `PLANEJADA`. Nada calçado é apagado na troca.

**O que sai:** os modos `atual` e `regioes`, a chave `/vc grafo modo`, as cores por modo, a trava de 10 dias (Regras 225 e
306) e o afinamento (`RoadThinning`, `RoadChains`, `RoadRouting`, `RoadMask`, `RoadSquares`, `RoadSettling`, boa parte de
`RoadLines`). Cerca de 2.000 linhas e os testes de forma (`RoadGeometryPinTest` e vizinhos) saem ou mudam.

**Contrato (vira teste e medida):**

1. Toda entrada ligada ao centro, ou `FALHOU` com motivo no log; fila não é falha.
2. Nenhuma aresta sobre água, através de construção ou com degrau maior que 1.
3. Numa vila parada, no máximo 3 mudanças de grafo por dia (hoje 13 a 22).
4. Nenhum caminho da vila gerada ou do jogador apagado; nenhuma coluna calçada apagada em menos de N dias.
5. Reabrir o mundo não muda o grafo.

## 6. Riscos

- **É a maior troca do núcleo desde a ADR-043.** Cerca de 2.000 linhas mudam e dezenas de testes de forma mudam junto.
- **Mais calçada:** com a vizinhança relativa, 3 a 8 vezes mais colunas a calçar, num Zelador que no jogo da 0.4.150 calçou 42
  e 122 colunas por dia. A árvore reduz isso.
- **Fonte da verdade:** passa a ser "o mundo manda no estado físico (está calçado? bloqueou?), o grafo manda na intenção".
  Isso emenda o invariante do `AGENTS.md` e pede ADR.

## 7. Decisões do autor

a. **Quem liga a quem:** árvore que cresce (menos calçada, sem voltas) ou vizinhança relativa (voltas como vila de verdade,
   muito mais calçada).
b. **Pontas:** seguem crescendo 1 bloco por dia (Regra 229) ou só crescem quando a busca de lote recusa aquele lado.
c. **ADR nova** para o grafo planejado, emendando a ADR-057 (os três modos saem) e o "mundo é a fonte da verdade".
d. **Ordem:** a sugerida no §8.

**Escolhas do autor (26-11):** a. árvore que cresce; b. a ponta só cresce quando a busca de lote recusa aquele lado. c e d sem resposta ainda.

## 8. Ordem sugerida

| Etapa | O quê | Prova |
|---|---|---|
| 1 | Estado por aresta e traçado guardados no save, dentro do `vizinhanca`; `FALHOU` separado de fila | GameTest que reabre o mundo e o grafo fica igual |
| 2 | Zelador só calça `TRAVADA`; limpeza só no que a colônia pôs, depois de N dias, com diário (L1, L2) | GameTest com caminho do jogador que fica |
| 3 | Quem liga a quem e pontas, conforme o §7 | Teste de propriedade: toda entrada ligada ou com motivo |
| 4 | Migração do mundo antigo; saem `atual`, `regioes` e a chave | GameTest de mundo antigo carregado |
| 5 | Perfil longo nos 3 biomas contra a bancada de 18-11 | `mudanças por dia ≤ 3` numa vila parada |

## Fontes

- Estudo de 18-11, ADR-043, ADR-050, ADR-057 e Regras 177, 214, 223 a 225, 229, 306, 316, 319 a 322.
- Bancada de 18-11: `run/biome-sim/reports/{plains,taiga,desert}@{atual,regioes,vizinhanca}`.
- Jogo da 0.4.150: `docs/testing/relatorios/jogo-20261126-2045.md` e `logs/villagecolony-blocks-20261126-181446.jsonl`.
- RoadArchitect: [GitHub](https://github.com/Shadscure/RoadArchitect), [Modrinth](https://modrinth.com/mod/roadarchitect),
  [CurseForge](https://www.curseforge.com/minecraft/mc-mods/roadarchitect).
- GDMC: [Yates, tese de 2018 (árvore geradora e A*)](https://www.cs.mun.ca/~dchurchill/publications/pdf/theses/ChristopherYates_Thesis_Honours.pdf),
  [CEUR-WS vol. 3217 (rede que cresce; nó inalcançável espera)](https://ceur-ws.org/Vol-3217/paper3.pdf),
  [Li, GDMC 2022 (cada construção nova liga à rede)](https://github.com/HSTEHSTEHSTE/GDMC_2022/blob/main/README.md),
  [Salge et al., 2018](https://arxiv.org/pdf/1803.09853.pdf).
