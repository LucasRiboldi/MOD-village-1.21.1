# Estudo do grafo de caminhos — 2026-11-18

Pedido do autor em 2026-11-18: conferir se a ideia do grafo está certa, se as ações dele estão certas e quais alternativas
corrigem ou melhoram o funcionamento, à luz do conceito do mod. **Só estudo: nenhum código foi alterado.** Base: `fix/jogo-0490`
em `3f164a3f` (0.4.92), ADR-043, ADR-048, ADR-050 e ADR-052, as cerca de 50 regras de caminho de `docs/RULES.md`, a pesquisa
`2026-11-10-melhorias-do-grafo.md`, os logs da 0.4.90 e da 0.4.91 e a pesquisa externa citada no fim.

Marcas: **CONFIRMADO** (visto no código ou no log), **PROVÁVEL** (mecanismo visto, efeito não medido), **SUSPEITO**.

## 1. Para que o grafo existe

Lido nas ADRs e nas regras, o grafo responde a seis perguntas da vila:

| # | Pergunta | Quem usa | Regras |
|---|---|---|---|
| F1 | Por onde a vila anda (linhas de 1 bloco, "mapa de trilhos") | aldeão prefere a rua; `RoadNetwork` | 151, 177, 216 |
| F2 | O que falta calçar e o que sobra | `PathPaving` (calça), `PathTrim` (apaga) | 186, 214, 215 |
| F3 | Toda entrada está ligada ao centro | releitura liga partes soltas | 207, 210, 224, 300 |
| F4 | Que chão é rua e não pode virar lote | busca de lote | 282 |
| F5 | Para onde a vila cresce | pontas azuis, 1 bloco por dia, caixa alcança a ponta | 185, 206, 229, 230 |
| F6 | Onde vão os enfeites da rua | poste de luz na aresta fixa | 225, 234, 261, 306 |

A ideia de fundo é boa e combina com o conceito do mod: **a vila decide sozinha onde vai a rua, e são os aldeões que calçam.**
O problema está em **como** o grafo é obtido.

## 2. Como o grafo é feito hoje

`RoadGraphRefresh` lê o mundo e entrega a `RoadLines.build` (núcleo puro, cerca de 2.000 linhas entre `RoadLines`, `RoadMask`,
`RoadThinning`, `RoadChains`, `RoadRouting`, `RoadSquares`, `RoadLinks`, `RoadTips`, `RoadSettling`):

```
mundo (dirt_path lido)  ──►  faixa fechada  ──►  afinamento Zhang-Suen ×3  ──►  nós e trechos  ──►  retas
      ▲                                                                                              │
      │                                                                                              ▼
 PathPaving calça / PathTrim apaga  ◄──  toPave e surplus  ◄──  partes ligadas, pontas podadas, trava de 10 dias
```

**É um laço fechado.** O grafo sai dos blocos de caminho do mundo, e os blocos de caminho do mundo saem do grafo: ele manda
calçar o que falta e apagar o que sobra. Na releitura seguinte, lê o que ele mesmo mandou fazer (CONFIRMADO,
`RoadLines.java:93-230`, `PathTrim.java:89-117`).

## 3. O que está errado na ideia

### 3.1 A fonte da verdade do grafo é o produto do próprio grafo (CONFIRMADO)

O laço da §2 não tem ponto fixo garantido. Tudo o que mexe em `dirt_path` vira entrada do grafo:
- a calçada em curso, cortada no meio do expediente;
- a sobra ainda não apagada;
- a entrada moldada na geração (até a Regra 313 punha `dirt_path` na frente de cada porta);
- o `StreetLayer` da vila gerada;
- o `Corridor` e o `StripLot` (ainda traçam caminho próprio, `TODO.md`);
- a `DoorPaths` da Regra 314;
- o jogador.

Cada mudança move o esqueleto, e o esqueleto move o que se calça e o que se apaga.

**Medido:** 202 mudanças de grafo em 2 h 27 na 0.4.43, uma a cada 45 s. A trava de 10 dias (Regra 225, refeita pela 306) é um
amortecedor, não uma correção. Com jogos de 6 min a 4 h ela quase nunca dispara: `fixed=0` no jogo da 0.4.91, e só depois de
248 min na 0.4.90.

### 3.2 O afinamento é instável por natureza (CONFIRMADO na literatura)

Esqueleto por afinamento (Zhang-Suen) é sensível a ruído na borda. Um bloco a mais ou a menos na faixa cria ramos e desloca
nós, e o resultado sempre pede uma poda depois. Na comparação de métodos, o Zhang-Suen é o mais rápido e o de pior qualidade
de esqueleto.

O código já carrega as consequências, uma regra para cada:
- poda de ponta de até 4 blocos;
- fusão de nós a até 3 blocos;
- três passadas;
- corte de quadrado (Regra 223);
- quinas (Regra 184).

Cada remendo diminui uma oscilação e cria uma geometria nova, que pede outro remendo.

### 3.3 O grafo diz o que existe, e não o que precisa ligar (CONFIRMADO)

Não há um modelo de demanda, isto é, quais pontos precisam estar ligados e com que prioridade. A ligação sai de quatro
remendos, em ordem:
1. reta em L (Regra 224);
2. ramal de porta (Regra 178);
3. busca A* de uma parte por releitura, em rodízio (Regras 295 e 300);
4. junção da porta sozinha (Regra 300).

Resultado: `isolated=15` e `loose=` com dezenas de partes no jogo da 0.4.91; 17 partes e 6 portas soltas o jogo todo na 0.4.90.

### 3.4 Apagar a sobra destrói o que o conceito manda preservar (CONFIRMADO)

`PathTrim.pass` devolve ao solo todo caminho a até 3 blocos da linha que não está dentro de construção. Não pergunta se o
bloco é da vila original (Regra 3) nem do jogador. A rua de 3 da vila gerada vira rua de 1, contra a ADR-043 item 6 ("três
blocos para rua principal").

`offLine=2033` no jogo da 0.4.91: são 2.033 colunas de caminho fora da linha, candidatas a apagar ao longo dos dias.

### 3.5 Crescer pela ponta mistura defeito de geometria com plano da vila (PROVÁVEL)

A ponta azul de cada lado é o ponto do esqueleto mais longe do centro (Regra 229) e cresce 1 bloco por dia, haja ou não
necessidade. Como o esqueleto oscila, a ponta muda de lugar. A caixa da vila salta quando a ponta sai 7 blocos (Regra 230),
e com ela a busca de lote, os baús e a releitura. Crescer deveria ser uma resposta a "não há lote", e não ao calendário.

### 3.6 A trava por dias depende de um relógio que o playtest não alcança (CONFIRMADO)

10 dias de jogo são 3 h 20 sem dormir. Os enfeites que dependem da trava (poste de luz, Regras 261 e 306) quase não aparecem
num playtest. A ADR-050 registrou a recusa do autor a 3 dias. O problema não é o número: a estabilidade está amarrada ao
tempo, e não a eventos.

## 4. O que está certo e deve ficar

- **O modelo `RoadGraph`** (nós, arestas, tipos centro, junção, ponta e entrada) e a persistência versionada (`roadGraphs` v1).
  A §6 reaproveita os dois.
- **Os vetos de traçado:** nunca sobre a água (Regra 264), nunca atravessar estrutura (265), degrau de no máximo 1 bloco,
  telhado não é chão (307).
- **O calçamento por aldeão** (ADR-048), no ritmo de quem calça: é o coração do conceito.
- **A entrada pela porta**, lida do molde (Regras 303, 305 e 313): agora estável, com o encaixe (`jigsaw`) contado.
- **Medir antes de mudar:** `VC_ROAD_GRAPH` com `offLine`, `capped`, `fixed`, `refused` e `loose`.

## 5. O que os outros fazem

| Projeto | Como decide a rua | Lição para o mod |
|---|---|---|
| **GDMC** (competição acadêmica de vilas no Minecraft) | As entradas são os nós; uma árvore geradora mínima decide quais ligar; A* sobre o mapa de alturas, com custo de rampa, traça cada ligação; depois aplaina a faixa da rua | A rua é **plano**: sai das entradas, e não dos blocos que já existem |
| **GDMC 2022, Henry Li** | Cada casa nova liga, pelo caminho mais curto, a outra casa ou à rede que já existe | Crescer por evento: rua nova só quando entra uma construção |
| **AgentCraft** (GDMC) | Agentes andam; a rua nasce do tráfego, com teste de passagem; o que não liga agora tenta depois, com a rede maior | Tráfego como prioridade; ligar mais tarde não é falha |
| **Lechner et al.** (cidades por agentes) | Agentes constroem rua num laço por tique; geometria irregular | Rua só por agente sai imprevisível; misturar com regra |
| **Helbing, active walker** e o jogo **Foundation** | Trilha nasce onde se pisa muito e some onde ninguém passa; rua movimentada alarga | "Movimentação natural" literal; precisa de contagem de passos por coluna |
| **Millénaire** (6.2) | Caminhos partem de prédios-nó (poço, praça), com menos desvio, um pouco abaixo do chão | Nó semântico (sino, poço, praça) como raiz da rede |
| **Countered's Settlement Roads**, **RoadArchitect** | A* entre vilas, contornando água e montanha, com cache de terreno | A* com cache de terreno é o padrão do Fabric 1.21 |
| **MineColonies** | O jogador desenha a rua; caminho só deixa o aldeão mais rápido; o aldeão preso é teletransportado | O mod não decide a rua; o caminho é custo menor no pathfinding |
| **Baritone** | A* com custo de quebrar e pôr bloco (pilar, ponte), cache de chunk em 2 bits | Modelo de custo explícito para cavar e pôr (vale para o mineiro, ver a avaliação conceitual) |

**Padrão comum:** quem gera rede estável (GDMC, Millénaire, os mods de estrada) parte dos **pontos que precisam de ligação**,
liga por **árvore e A* com custo de terreno** e trata o mundo como **resultado**. Nenhum deriva a rede afinando os blocos
de caminho que ela mesma pôs.

## 6. Alternativas

### A. Plano primeiro: a rede nasce das entradas (recomendada como destino)

- **Nós semânticos:** centro (sino), cada entrada do registro de construções, boca da mina, entrada de plantação e curral, e
  uma ponta por lado só quando a busca de lote pede (§3.5).
- **Quem liga a quem:** cada nó novo liga ao ponto mais próximo da rede existente, como no GDMC 2022. Uma árvore geradora
  mínima só na fundação, para as peças da vila gerada.
- **Traçado:** A* sobre `PathTerrain`, com custo de rampa, caminho existente mais barato (reaproveita a rua da vila gerada e a
  do jogador) e os vetos da §4.
- **Fonte da verdade:** as **arestas planejadas, persistidas** no `roadGraphs`. O mundo só responde "esta coluna está calçada?"
  e "algo bloqueou?". Nada de afinamento.
- **Muda quando:** construção nova, demolição, bloqueio confirmado no mundo. Fora disso o grafo é estável por construção.
- **Prós:**
  - acaba com a oscilação;
  - liga toda entrada por definição;
  - deixa de apagar a rua da vila;
  - a trava de 10 dias deixa de ser necessária;
  - o poste pode ir em qualquer aresta calçada.
- **Contras:**
  - reescreve `RoadLines` e companhia, cerca de 2.000 linhas do núcleo e dezenas de testes de forma (`RoadGeometryPinTest`);
  - pede ADR nova;
  - revê o "o mundo é a fonte da verdade", que passa a valer para o **estado físico**, não para a **intenção**. A ADR-043
    já persiste o grafo, então o passo é pequeno no conceito.

### B. Cortar o laço sem reescrever (recomendada para já)

- A vila gerada entra **uma vez**: as peças de rua do `StructureStart` (`straight`, `crossroad`) e as entradas viram arestas na
  adoção, sem ler `dirt_path`. É a semântica de importação da ADR-043.
- Daí em diante, só as arestas **planejadas e persistidas** mudam o grafo. A releitura do mundo só marca "calçada / falta /
  bloqueada".
- **Identidade de nó com histerese:** o nó que se move menos de 3 blocos fica onde estava.
- **Estabilidade por evento:** a aresta fica fixa quando passa N releituras sem mudar (por exemplo 3), e não depois de 10 dias.
- **`PathTrim` só apaga o que a colônia calçou** (`RoadIndex.isLaidByAColony`), nunca a rua da vila gerada nem a do jogador.
- **Prós:** menor custo; a maior parte do núcleo fica. **Contras:** o afinamento continua na importação, com a mesma
  sensibilidade, mas só uma vez.

### C. Trilha de desejo: a rua nasce do passo (camada opcional)

- `WorkTime.sample` já olha cada aldeão a cada segundo. Contar os passos por coluna e calçar a coluna que passa de um limiar;
  a que fica dias sem passo volta ao solo.
- **Serve para:** alargar e priorizar a rua (qual aresta calçar primeiro), e não para ligar. O andar Vanilla corta jardins e
  faz desvio.
- **Prós:** o mais "natural" de todos, alinhado ao conceito. **Contras:** lento para convergir; precisa de A ou B por baixo
  para garantir ligação.

### D. Só remendos no laço atual

Histerese de nó, trava por releitura e `PathTrim` restrito, sem mexer na origem do grafo. **Prós:** o menor risco imediato.
**Contras:** o laço continua, e a próxima geometria nova pede a próxima regra.

**Recomendação:** B agora (ADR curta, 3 a 4 commits), A como destino (ADR nova, uma fase do cronograma), C depois de A, só
para largura e prioridade. D só se o autor não quiser mexer na origem do grafo.

## 7. As ações do grafo, uma a uma

| Ação | Está certa? | Correção proposta |
|---|---|---|
| Calçar (`PathPaving`) | Sim; lenta (cerca de 40 passos por dia com 1 calçador) | Ordenar o que se calça pela demanda: primeiro a aresta que liga uma entrada sem ligação, depois a de mais tráfego (C) |
| Apagar a sobra (`PathTrim`) | **Não** (§3.4) | Só o que a colônia pôs; nunca a rua da vila gerada nem a do jogador |
| Ligar partes soltas | Parcial (§3.3) | Em A, deixa de existir: nó novo já nasce ligado. Em B, tentar todas na mesma releitura com teto de tempo |
| Pontas crescem 1 por dia | **Não** como regra fixa (§3.5) | Crescer quando a busca de lote falha naquele lado |
| A caixa alcança a ponta | Sim, mas herda o problema da ponta | Igual: caixa cresce com a obra que pediu o lote |
| Trava de 10 dias | Amortecedor (§3.6) | Estabilidade por evento (A) ou por releituras (B) |
| Poste na aresta fixa | Certo na ideia, raro na prática | Poste em aresta calçada há N releituras |
| Ler o piso da coluna no mundo | Frágil em montanha (Regras 291, 295, 307) | Em A, a altura é a do traçado A*, conferida no mundo só para "bloqueou?" |

## 8. Contrato proposto (invariantes que viram teste)

1. Toda entrada registrada está ligada ao centro, ou tem um motivo no log (`loose=` com causa: água, estrutura, sem chão).
2. Nenhuma aresta passa sobre água, atravessa estrutura ou sobe mais de 1 bloco por passo.
3. A aresta só muda por evento: construção nova, demolição ou bloqueio confirmado. **Métrica:** mudanças de grafo por dia de
   jogo, com teto (por exemplo 3 numa vila parada).
4. Nenhum bloco de caminho da vila gerada ou do jogador é apagado.
5. A ponta só cresce quando um lote foi recusado por falta de espaço naquele lado.

Cada invariante vira teste de propriedade sobre o núcleo puro (jqwik com o runner certo, ver a memória "jqwik silencioso")
e uma linha de medida no `VC_ROAD_GRAPH`.

## 9. Ordem sugerida

| Passo | O quê | Prova |
|---|---|---|
| 1 | `PathTrim` só no que a colônia calçou | GameTest com rua de 3 da vila gerada que continua de 3 |
| 2 | Métrica de mudanças do grafo por dia (`changes=`) | Linha no `VC_ROAD_GRAPH`; medir num jogo |
| 3 | B: importação única da vila gerada + histerese + trava por releitura | GameTest que reabre o grafo 10 vezes sem mudança |
| 4 | Ponta cresce por recusa de lote | GameTest com lote recusado de um lado |
| 5 | ADR do plano primeiro (A) | Decisão do autor |

## Fontes

- GDMC: [paper original](https://arxiv.org/pdf/1803.09853.pdf), [AgentCraft](https://ceur-ws.org/Vol-3217/paper3.pdf),
  [GDMC 2022, Henry Li](https://github.com/HSTEHSTEHSTE/GDMC_2022/blob/main/README.md),
  [árvore + A* com rampa](https://alexlaverty.github.io/blog/roads-that-follow-the-land/),
  [tese MUN, árvore sobre entradas](https://www.cs.mun.ca/~dchurchill/publications/pdf/theses/ChristopherYates_Thesis_Honours.pdf),
  [Physarum em mapa de alturas](https://studenttheses.uu.nl/handle/20.500.12932/44257)
- Cidades por agentes: [Lechner et al.](https://ccl.northwestern.edu/testsite/rp/cities/papers/midgraph.cities.pdf)
- Trilha de desejo: [Helbing et al., active walker](https://arxiv.org/abs/cond-mat/9806097),
  [Foundation](https://pcgamer.com/foundation-is-a-freeform-medieval-city-builder-with-no-grids-to-follow)
- Mods: [Millénaire 6.2](https://www.curseforge.com/minecraft/mc-mods/millenaire/files/2554233),
  [Countered's Settlement Roads](https://modrinth.com/mod/rrAGfOzL), [RoadArchitect](https://modrinth.com/project/dLRvLyY3),
  [MineColonies, caminhos](https://minecolonies.com/wiki/systems/pathing),
  [Baritone](https://raw.githubusercontent.com/cabaletta/baritone/1.19.4/FEATURES.md)
- Afinamento: [comparação de métodos](https://digifors.cs.up.ac.za/issa/2011/Proceedings/Research/Khanyile_Tapamo_Dube.pdf),
  [sensibilidade à borda](https://hal.archives-ouvertes.fr/hal-01217974/file/3DSkASymHAL_v2.pdf)
