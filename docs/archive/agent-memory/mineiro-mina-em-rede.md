---
name: mineiro-mina-em-rede
description: Mineiro 0% em 12-10 — quedas de 3 blocos de mão única, perna mandando à entrada, ramal inalcançável trava o nível; 0.3.34 e ADR-042 (mina em rede) aguardando o autor
metadata:
  type: project
---

Leitura do save com busca de alcance (scratchpad tools/reach.py, cliffs.py sobre mcablocks.py):
da boca chega-se a y=12; de dentro não se passa de y=34 (degrau −377,35,−1004 → 32). Pedras do
ramal aberto eram alcançáveis a pé; a perna (MinerLeg) mandava à entrada do nível; nível só desce
com os 4 ramais terminados e ramal inalcançável nunca termina.

0.3.34 (da64ea66, JAR 1975DF2F…): MinerDirectPath (navegação do jogo, folga 1 bloco — o caminho
Vanilla para 1 antes do alvo; MinerReach.ARRIVAL é 0) e 3 desistências seguidas terminam o ramal.
Pesquisa: MineColonies (poço + secureBlock 11×11, nós a cada 7, trabalha do nó-pai pronto),
Baritone (maxFallHeightNoWater 3, allowPlace). Proposta ADR-042 com D1–D9, decisões abertas:
altura entre níveis (8?), largura 3, teto do veio 64, escadaria ou escada de mão.
Armadilha: gametest de navegação — criar o aldeão em coordenada absoluta e waitAndRun(5).
Ver [[obra-esperando-tijolo]], [[mineiro-preso-na-descida]].

**ADR-042 aplicada na 0.3.35 (b4d572ed, JAR CCEAD667…), autor: "apliquem tudo".** Rede só depois do
salão (galleryPhase = arm0.cut ≥ SHARED_BLOCKS — branchesOpenNow==ARMS sozinho liga cedo e quebrou 2
MinerGameTest). Descer exige terminar os ramais antes (advanceIfEveryOpenArmIsDone devolve WAITING).
Altura entre níveis ficou 10 (escadaria em caracol), D8 = anéis laterais + rampa existente. Arquivos
no teto: MineDigging 499. Nada jogado.
