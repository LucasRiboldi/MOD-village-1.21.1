# Ruas naturais: estudo e decisões (2026-10-28)

Pedido do autor depois do jogo da 0.3.102 (planície, 4h16, Spark `WT6mMz7jQQ`): *"focar na criação e
crescimento das ruas, ponto sensível"*; *"espaço de suspiro entre as construções"*; *"os caminhos devem ir
crescendo naturalmente"*; *"deixar a vila mais bonita e natural"*.

## O que o jogo mostrou

| Medida (4h16) | Valor |
|---|---|
| Recusas de lote por `ROAD` | 150.816 |
| Recusas por `TOO_CLOSE` (3 blocos) | 129.616 |
| Recusas por `NOT_NATURAL_GROUND` | 29.562 |
| Lotes 3 a 8 abaixo da rua | 10.962 |
| Rua crescida | ~50 blocos, em trechos de 5 |
| Caixa da vila | 155×129 → 213×235 |
| Casa mais longe | ~140 blocos do centro, em morro (y 97, centro y 73) |

A rua parava por pouco:
- um tronco de bétula;
- grama marcada como "posta pela colônia";
- "algo em cima";
- "sem motivo registrado".

Sem lote perto, a busca ia longe. Hoje três sistemas não conversam:
- a rua que cresce em trechos de 16 (`VillageRoad`, `RoadPaving`);
- o corredor de 1 bloco do lote (`Corridor`, `StripLot`);
- a rede de caminhos das portas (Regras 123–125).

## O que a pesquisa diz

- **GDMC — Generative Design in Minecraft** ([Salge et al. 2018](https://arxiv.org/abs/1803.09853)).
  - A vila do jogo traça a rede de ruas sem o terreno e depois a projeta na superfície: degraus e
    buracos.
  - As entradas da competição ligam construções por A* sobre o relevo.
  - Uma abordagem por agentes ([Esko](https://www.diva-portal.org/smash/get/diva2:1603625/FULLTEXT02.pdf))
    examina o terreno antes de começar a rua, e um agente "estende" enquanto outro "conecta".
- **Emilien et al. 2012, "Procedural Generation of Villages on Arbitrary Terrains"**
  ([PDF](https://perso.liris.cnrs.fr/egalin/Articles/2012-villages.pdf)).
  - Crescimento em ciclos sobre um **mapa de interesse** (proximidade, declive, acesso).
  - *A rua nova atrai moradores, e a casa nova estica a rua.*
  - Os lotes se alinham à rua e ao relevo.
- **Colonização do espaço para ruas** ([Minho](https://uce-cg.di.uminho.pt/?p=925)).
  - O controle de ângulo vai da malha ao orgânico.
  - O raio de "morte" regula quantos ramais nascem.
- **Zigue-zague em encosta íngreme** ([SBGames 2020](https://www.sbgames.org/proceedings2020/ComputacaoFull/209687.pdf)):
  A* sobre o mapa de alturas.
- **Millénaire** ([CurseForge](https://www.curseforge.com/minecraft/mc-mods/millenaire/files/2554233)).
  - Os caminhos partem de "nós" (poço, pracinha).
  - O caminho fica um pouco abaixo do bloco, como o caminho de terra.

## Decisões do autor (28-10)

1. **Rua junto com a obra:** o lote é escolhido junto ao trecho de rua que vai servi-lo, e os dois são
   construídos juntos.
2. **Rua e caminho separados:** rua de 3 de largura, estrutura da vila; caminho de porta à parte. Os
   dois usam o traçador rente ao chão (degrau ≤ 1, corte e aterro de 1, contorna).
3. **Adensar antes de esticar:** os dois lados das ruas existentes se ocupam primeiro. A rua estica pela
   ponta mais perto do centro, ou abre um ramal num cruzamento.
4. **Forma:** curvas que seguem o relevo (diagonais em escada e voltas); quase reta no plano.
5. **Espaço de suspiro, os quatro:**
   - quintal de 2 na frente;
   - folga lateral de 4 a 6, sorteada;
   - um lote verde (jardim, árvore, banco ou poço) a cada ~5 casas, e uma pracinha nos cruzamentos maiores;
   - árvores do bioma e luz ao longo das ruas.
6. **Chão da rua — mistura gasta natural:**
   - caminho de terra no meio, com cascalho e terra grossa nas bordas;
   - na areia, caminho de areia com arenito nas bordas.
   - **Esses blocos, o que a vila cria e todo material de estrutura da vila entram na lista do que
     nenhuma profissão minera nem recolhe.**
7. **Lote pelo mapa de interesse:**
   - a nota de cada candidato soma perto do centro, terreno suave, perto de rua, folga dos vizinhos e
     não encostar em barranco;
   - a obra vai ao de maior nota;
   - a nota sai no `VC_STATE` para ajustar os pesos.

## Desenho

- **`StreetPlan`** (núcleo, puro): o mapa de interesse e a escolha do par lote+trecho de rua.
  - Lê o relevo (`PathTerrain`), a rede atual e as construções.
  - Devolve o lote e o trecho que liga a frente dele à rua mais próxima.
- **Traçador:** o `VillagePathPlanner` (rente ao chão) com:
  - custo de curva baixo, para seguir o contorno;
  - largura 3 para a rua;
  - custo de declive, para o zigue-zague na encosta.
- **Assentamento:**
  - o trecho de rua entra na obra como peças de chão, assentadas pelo construtor;
  - a borda da rua é sorteada entre os blocos da mistura.
- **Proteção:** toda coluna de rua, praça, jardim e poste fica marcada como da colônia
  (`BlockProtection.isColonyBuilt`). O lenhador, o mineiro e a coleta de superfície já recusam o que é
  da colônia; falta conferir cada coleta.
- **Lote verde e pracinha:**
  - plantas pequenas próprias, ou peças do catálogo do jogo;
  - entram no rodízio de obras a cada ~5 casas e nos cruzamentos de 3+ ruas.
- **Medida:**
  - `VC_STATE` ganha: colunas de rua, ramais, a nota do último lote e a distância média casa→centro;
  - o `state_report.py` mostra a evolução.

## Ordem de implementação

1. Proteção dos blocos da vila (lista única) e a mistura de bordas: base para tudo.
2. Mapa de interesse e lote junto à rua, com a rua junto da obra (`StreetPlan`).
3. Forma em curva (custos do traçador) e quintal/folga.
4. Lote verde, pracinha, árvores e luz.
5. Medida no `VC_STATE`.

Cada etapa com GameTest e um commit.
