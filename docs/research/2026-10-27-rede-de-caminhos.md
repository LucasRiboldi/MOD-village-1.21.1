# Rede de caminhos da vila — estudo (2026-10-27)

> Pedido do autor, 27-10. Este documento é só estudo: não tem código e
> ainda não tem regra numerada. As decisões que faltam estão no §7.

## 1. O pedido

1. **Identificar e registrar caminhos dentro da vila.** Todo percurso entre
   a entrada de uma estrutura e o chafariz central fica guardado.
2. **Usar o registro para duas coisas:** calcular como os caminhos crescem e
   escolher onde nascem as estruturas novas.
3. **Forma de partida do caminho:**
   - 3 blocos de largura;
   - sinalizado com o caminho de terra do bioma;
   - buraco no traçado é preenchido, para o caminho ser seguro;
   - entre um bloco e o seguinte, no máximo 1 bloco de diferença de altura,
     para todo aldeão poder subir e descer.

## 2. O que o mod já tem

| Peça | Onde | O que faz hoje | Relação com o pedido |
|---|---|---|---|
| Índice de ruas | `RoadIndex` | Guarda as colunas de rua já vistas e busca lote entre elas | É um conjunto de colunas, não um grafo: não sabe que trecho liga o quê |
| Rua que cresce (Regra 15) | `RoadExtension` | Sem lote livre na beira da rua, estende a rua a partir de uma ponta | É a ideia de "a rua puxa casa e a casa estende a rua" (§3.2), sem rede |
| Calçamento | `RoadPaving` | Assenta bloco a bloco, com degrau de no máximo 1 (`groundNear`) e recusas (`refusalAt`) | Já cumpre a regra do desnível; o assentamento pode ser reaproveitado |
| Corredor da porta (Regra 70) | `Corridor` | 1 bloco de largura, reto a partir da porta, vira só em 90°, para no primeiro caminho; corta duna, **nunca aterra** | **Conflita** com 3 de largura e com "buracos são preenchidos" (§7, D1 e D2) |
| Lote na beira da rua | `StripLot`, `LotLevel` | O lote novo nasce ao lado da rua, com o chão nivelado | É a escolha de local "pelo caminho", só que sem ranking |
| Paleta | `VillagePalette.DIRT_PATH`, `VillageRoad` | O bloco de rua por estilo vem do catálogo da vila | Serve de "terra do bioma" para sinalizar |
| Custo do caminho | `LandPathNodeMakerMixin` (Regra 122) | O construtor de caminho do jogo já tem um ponto onde o mod muda o custo de um nó | Permite fazer o aldeão **preferir** o caminho (§5.6) |

O custo da busca de lote também importa aqui. No jogo da 0.3.93 houve 746 mil
recusas "ROAD" numa colônia. O item 7 das decisões de 27-10 ("lotes
candidatos guardados") e este estudo resolvem o mesmo problema: com a rede
registrada, o lote candidato nasce das arestas, e não de varrer o raio.

## 3. O que outros fizeram

### 3.1 Desafio GDMC (gerar assentamentos em Minecraft)

O GDMC (*Generative Design in Minecraft*) é uma competição anual de
geradores de vila em Minecraft. Os geradores ligam as construções por A*
(algoritmo de busca do caminho mais barato em grade). Alguns montam antes uma
**árvore geradora mínima**, o conjunto de ligações mais curto que conecta
todas as construções, e depois traçam cada ligação por A*.

- [Yates, tese de 2021](https://www.cs.mun.ca/~dchurchill/publications/pdf/theses/ChristopherYates_Thesis_Honours.pdf):
  espalha as casas por disco de Poisson (pontos sorteados com distância
  mínima entre si) e liga tudo com A* e árvore geradora.
- [Camilleri, 2022](https://www.um.edu.mt/library/oar/handle/123456789/107753):
  usa A* para garantir que toda construção está ligada ao assentamento.
- [Salge et al., visão geral do GDMC](https://arxiv.org/pdf/2108.02955):
  os critérios da competição são adaptação ao terreno, função, narrativa e
  estética.
- [van der Staaij et al., vencedor de 2022](https://arxiv.org/pdf/2309.10871):
  planejamento iterativo e descentralizado; a vila cresce aos poucos.

### 3.2 Pesquisa em geração procedural de estradas e vilas

- **Estradas com custo por inclinação.**
  [Galin et al., *Procedural Generation of Roads* (2010)](https://perso.liris.cnrs.fr/egalin/Articles/2010-roads.pdf):
  o traçado é o caminho mais curto ponderado pelo terreno. O custo
  considera inclinação, rio, lago e mata, e a estrada é escavada ao longo
  do traçado. Esta é a base do custo do §5.2.
- **Vilas em terreno qualquer.**
  [Emilien et al., *Procedural Generation of Villages on Arbitrary Terrains* (2012)](https://perso.liris.cnrs.fr/egalin/Articles/2012-villages.pdf):
  - um **mapa de interesse** (sol, inclinação, proximidade) escolhe onde
    nasce a próxima construção;
  - **a estrada nova atrai moradores e a casa nova estende a rede**, num
    ciclo;
  - o terreno é dividido em lotes por conquista anisotrópica, isto é,
    crescendo mais rápido em umas direções que em outras.

  É a referência mais próxima do pedido.
- **Cidades por regras.**
  [Parish e Müller, *Procedural Modeling of Cities* (2001)](https://history.siggraph.org/?p=116923):
  as ruas crescem por um sistema de regras (L-system). Metas globais
  (densidade, padrão de rua) propõem o próximo trecho, e restrições locais
  (água, terreno, encontro com rua existente) o ajustam. O trecho que chega
  perto de outro se funde a ele.

### 3.3 Trilhas que nascem do uso

- **Modelo do caminhante ativo.**
  [Helbing et al.](https://ar5iv.labs.arxiv.org/html/cond-mat/9806097) e
  [Helbing, Molnár e Schweitzer](https://arxiv.org/pdf/cond-mat/9805074):
  quem anda gasta o chão, e quem vem depois prefere o chão gasto. O
  resultado é uma rede de **desvio mínimo**, parecida com a que pedestres
  reais formam. Os próprios autores sugerem usar isso para planejar
  caminhos que as pessoas de fato percorrem.
- **Formigas.**
  [Vila por otimização de colônia de formigas (BNAIC 2024)](https://bnaic2024.sites.uu.nl/wp-content/uploads/sites/986/2024/11/Procedurally-Generating-Natural-Looking-Villages-in-Minecraft-with-Ant-Colony-Optimization-Algorithms.pdf):
  agentes andam entre as casas. O caminho mais curto e mais plano é
  reforçado e vira a rede. Repetir "casa nova, caminho novo" faz a vila
  crescer.

### 3.4 Mods de Minecraft

- **Millénaire.** O
  [registro de mudanças de 2018](https://www.curseforge.com/minecraft/mc-mods/millenaire/files/2554233)
  diz que os caminhos passaram a se centrar em construções-nó (poço,
  pracinha), com traçado melhor calculado e menos voltas ao acaso. É a ideia
  do chafariz como centro.
- **RoadWeaver e Road Architect** ([RoadWeaver](https://www.modpackindex.com/mod/87773/roadweaver),
  [Road Architect](https://modrinth.com/mod/roadarchitect)): estradas
  **entre** vilas, traçadas pelo terreno e depois alisadas. Servem de
  referência de alisamento; o problema deles é outro.

### 3.5 Centralidade e uso do solo

[Sintaxe espacial e comércio](https://hvlopen.brage.unit.no/hvlopen-xmlui/handle/11250/3066399):
o comércio se instala nas ruas mais "integradas" da rede, as de maior
centralidade.

- **Centralidade de intermediação** (*betweenness*): quantos percursos
  entre dois pontos quaisquer da rede passam por aquele trecho.
- **Centralidade de proximidade** (*closeness*): o quão perto, em média, o
  trecho fica de todos os outros.

Para a colônia, isso dá um critério objetivo de onde pôr o que é de todos
(armazém, oficinas) e onde pôr casa.

## 4. Conclusões da pesquisa

1. **A rede deve ser um grafo explícito.** Nós são o chafariz, as entradas
   e as junções; arestas são os trechos. Todos os trabalhos que crescem a
   vila guardam a rede assim, e é o que permite "registrar todo percurso".
2. **O traçado sai de um A* com custo de terreno** (Galin, GDMC). O desnível
   máximo de 1 vira proibição dura. Aterro e corte viram custo.
3. **Ligar cada entrada ao ponto mais barato da rede**, e não sempre ao
   chafariz. É a árvore crescendo (GDMC, Emilien): as ruas se juntam, a rede
   fica curta, e o chafariz continua alcançável por ela.
4. **O uso refina a rede** (Helbing, formigas). Contar por onde os aldeões
   de fato andam mostra onde falta atalho ou largura.
5. **A rede escolhe os lotes** (Emilien, sintaxe espacial). O lote nasce na
   frente das arestas; o que é público vai para os nós centrais e a casa
   para a periferia.

## 5. Proposta de desenho

### 5.1 O grafo da vila (`VillagePaths`, núcleo puro)

- **Nós:**
  - o chafariz, a raiz;
  - a entrada de cada estrutura: o bloco na frente da porta, e o encaixe de
    rua quando não há porta;
  - as junções, onde um trecho encontra outro.
- **Arestas:** a lista ordenada das colunas do eixo do trecho, cada uma com
  x, z e a altura do piso. A largura de 3 é o eixo mais um bloco de cada
  lado.
- **Guardado no save, por colônia**, como a mina. "Todo percurso
  registrado" é exatamente isto: dá para refazer, medir e reconstruir.
- **Vila Vanilla existente:** as ruas que o `RoadIndex` já conhece entram
  como arestas na adoção da vila, para não se traçar rua nova por cima.

### 5.2 O traçado: A* em grade com altura

Busca em colunas (x, z), cada passo com a altura do piso calculada.

| Fator | Tratamento |
|---|---|
| Distância | custo 1 por passo; reta custa menos que curva, para o caminho não ficar em zigue-zague |
| Desnível | **proibido acima de 1**; desnível de 1 custa um pouco mais que plano |
| Buraco (piso abaixo do terreno vizinho, ou vazio) | permitido, com custo por bloco a aterrar com terra do bioma |
| Morro (terreno acima do piso) | permitido até um corte máximo, com custo por bloco cortado (corte em degraus, como o `Corridor` já faz na duna) |
| Água, lava, obra aberta, curral, lote reservado | proibido |
| Caminho já existente | **custo baixo**, para os traçados se juntarem (§4.3) |
| Largura 3 | a seção inteira (eixo e os dois lados) tem que caber no mesmo y e ter 2 de altura livre |

Isto é o custo de Galin (2010) com a regra do 1 bloco como restrição dura.
O A* é limitado ao raio da vila e roda uma vez por estrutura nova. O custo
total é proporcional ao número de obras, não ao tamanho da vila.

### 5.3 Ligar uma estrutura

1. Quando a obra fecha, a entrada dela vira um nó.
2. O A* parte da entrada e para no primeiro ponto da rede, que é qualquer
   coluna de qualquer aresta. Esse ponto vira uma junção.
3. **Atalho:** se o caminho pela rede até o chafariz for mais longo que um
   fator do caminho direto (por exemplo, 1,6 vez), traça-se também uma
   ligação direta. Isso evita a árvore pura, em que tudo passa por um
   único tronco.

### 5.4 Assentar

O trecho é assentado pelo `RoadPaving`, que já respeita a Regra 3 (não
destrói o que é de outro) e o degrau de 1.

- **Seção:** as 3 colunas da seção ficam no mesmo y. O eixo e as bordas
  levam o caminho de terra do bioma.
- **Buraco:** aterrado com o chão do bioma (Regras 21 e 90), até a altura
  do piso.
- **Morro:** cortado em degraus de 1 bloco.
- **Altura livre:** 2 blocos acima do piso.
- **Quem faz:** o construtor sem obra, como já faz com o buraco de 1 bloco
  da Regra 70. Ou surge pela Regra 111, se o autor quiser o caminho pronto
  na hora (§7, D6).

### 5.5 O crescimento pela rede

- **Lote candidato:** a faixa de terreno ao longo de cada aresta, dos dois
  lados, com a frente para o caminho. Os lotes ficam guardados por aresta
  (item 7 das decisões) e são reavaliados só quando a aresta muda.
- **Ranking do lote:**
  - **casa:** periferia e baixa centralidade, perto da ponta que mais
    cresce;
  - **armazém e oficinas:** alta centralidade, calculada sobre o grafo (de
    dezenas a centenas de nós, cálculo barato, refeito só quando a rede
    muda);
  - **roça e curral:** borda da vila, terreno plano (mapa de interesse de
    Emilien).
- **A rua cresce quando a frente esgota.** Sem lote livre ao longo das
  arestas, a ponta mais central estende um trecho, e o lote novo nasce na
  beira dele. É a Regra 15, agora sobre o grafo.

### 5.6 O uso refina a rede (opcional, fase 2)

- **Preferir o caminho:** pelo `LandPathNodeMakerMixin`, o chão fora do
  caminho custa um pouco mais. O aldeão escolhe andar pelo caminho quando o
  desvio é pequeno.
- **Contar o tráfego:** o passo dos aldeões fora do caminho é contado num
  mapa grosso, com células de 4×4 colunas. Onde muita gente atravessa fora
  da rede, propõe-se um atalho (Helbing). Onde a aresta é muito usada, ela
  pode ganhar largura.

## 6. Alternativas

| | A. Grafo e A* com custo (recomendado) | B. Trilhas do uso | C. Ruas planejadas em grade | D. Híbrido A + B |
|---|---|---|---|---|
| Como | Traça de cada entrada até a rede, com o custo de terreno do §5.2 | Conta os passos dos aldeões e pavimenta onde o uso passa de um limiar | Eixos retos a partir do chafariz, com quadras | A desenha a rede; B refina com atalhos e largura |
| Prós | Determinístico, testável por GameTest, cumpre "todo percurso registrado", barato (um A* por obra) | Rede orgânica, mínimo desvio real | Simples, previsível, bonito no plano | O melhor de A, mais o ajuste orgânico |
| Contras | Precisa de save e de migração da Regra 70 | Lento (precisa de muito tráfego); o aldeão Vanilla anda por pathfinding e não "pisa" como pessoa; difícil de testar | Ignora o relevo; a vila Vanilla não é em grade; muito aterro e corte | Mais peças |
| Escala | Proporcional ao número de obras | Proporcional ao número de aldeões e passos | Fixa | A + uma amostra de passos |

## 7. Decisões que o autor precisa tomar

| # | Decisão | Opções |
|---|---|---|
| D1 | A largura 3 substitui a Regra 70 (corredor de 1 bloco da porta)? | 3 em tudo / 3 na rede e 1 no ramal da porta / 1 da porta até a rede e 3 nas arestas entre nós |
| D2 | "Nunca aterra" (Regra 70) passa a "aterra buraco do traçado"? | sim, com chão do bioma / só até N blocos de fundo, e acima disso desvia |
| D3 | Até quanto se corta morro? | 0 (desvia) / 2 / 4 blocos (próximo da terraplanagem planejada) |
| D4 | Bloco de sinalização no deserto e na neve | caminho de terra em todo bioma / caminho de terra só onde o bioma o tem, e no deserto arenito liso |
| D5 | Ligar ao chafariz direto ou ao ponto mais barato da rede | rede (recomendado), com atalho quando o desvio passa de 1,6× / sempre direto |
| D6 | Quem constrói o caminho | o construtor sem obra, bloco a bloco / surge na hora (Regra 111) / o construtor, e surge o que ficar 30 min parado |
| D7 | Ruas Vanilla já existentes | adotar como arestas (recomendado) / refazer em 3 de largura |
| D8 | Usar a rede para escolher lote (§5.5) | já na fase 1 / só depois de a rede estar no jogo |
| D9 | Fase 2, tráfego e preferência pelo caminho (§5.6) | fazer / deixar para depois |

## 8. Fases e verificação

1. **Grafo no núcleo e save.** Teste de unidade: aresta, junção, chafariz
   alcançável de todo nó, e serialização ida e volta.
2. **A* com custo.** GameTest em cenário com morro, buraco e água:
   - nenhum passo sobe ou desce mais de 1;
   - a seção de 3 cabe inteira;
   - água nunca é atravessada;
   - o traçado reusa a rede.

   Prova de mutação: tirar a proibição do desnível tem que derrubar o teste.
3. **Assentamento.** GameTest: o buraco é aterrado com o chão do bioma, o
   caminho de terra fica nas 3 colunas e a Regra 3 é respeitada.
4. **Ligar a obra fechada à rede.**
5. **Lote pela rede e ranking por centralidade.** Medir no jogo:
   - recusas de lote: o `LotRefusals` deve cair;
   - custo do ciclo: `cost_ledger.py`;
   - tempo de caminhada: `time_ledger.py`, a proporção de walk deve cair.
6. **Fase 2, se aprovada:** preferência pelo caminho e tráfego.

Como toda verificação deste projeto, as fases 2 a 5 só estão provadas
quando jogadas, com `time_ledger.py` e `action_report.py`.
