# Plano — um aldeão que nivela o solo da vila (2026-10-08)

> Pedido do autor (08-10): "um aldeão que corrija e alinhe o solo da aldeia — caminhos, buracos,
> degraus de no máximo 1 bloco, acesso a pé por toda a vila, morros e declives equalizados, solo
> melhor para as construções". **Isto é um plano, não uma decisão:** as perguntas do fim são do
> autor, e nada aqui foi implementado.

## 1. O problema, medido nos playtests

- **Lote que a trava recusa** (07-10): obra planejada um acima do chão plano; nenhum construtor a
  pegava (`docs/reports/Playtest-2026-10-08.md`). Corrigido na régua, não no terreno.
- **Mineiro preso a pedra de pé inalcançável** e **construtor que "chega" fora do alcance**
  (memórias `folga-de-chegada-contra-alcance`, `mineiro-em-cima-da-mina`): degrau maior que 1
  bloco é a causa comum de "andando 93% do tempo".
- **Aldeão encalhado** (`ClimbOut`, `PenEscape`, F-1): buraco e degrau de 2 prendem o aldeão.
- **A rua de vila do jogo não respeita a altura**: o gerador traça o caminho sem olhar desnível,
  e casas ficam inacessíveis — o mesmo problema que os autores do GDMC apontam no gerador Vanilla.

O que já existe no mod e o plano reaproveita:

| Classe | O que faz hoje |
|---|---|
| `RoadExtension`, `RoadPaving`, `RoadIndex` | a rua cresce com a vila e é calçada bloco a bloco (Regra 15) |
| `FoundationPreparation` | aterra lacunas de 1 camada na base do lote (régua de 50% apoiado) |
| `DoorStep` | põe o degrau na frente da porta de lote um acima |
| `LotGround`, `VillageBiomes.foundationGroundAt` | o que é chão natural e qual solo do bioma usar |
| `BlockProtection`, Regra 3 | o que nunca se quebra (peça de vila, coisa do jogador) |
| `MineSurface` (raspagem) | o mineiro sem ramal já tira pedra da superfície |

## 2. O que os outros fizeram (referências)

| Referência | O que fazem com o terreno | O que vale copiar |
|---|---|---|
| [RoadWeaver](https://github.com/shiroha-233/RoadWeaver) (mod Java, MIT) | A* ciente de altura, bioma e chão; **corta e aterra** para a rua passar; **laje nos degraus**; a fundação da rua interpola o terreno em volta e só aterra onde a rua fica acima do chão (encosta convexa); ponte sobre água, túnel na montanha | laje como meio-degrau; aterrar **só** sob a rua e rampear para fora; custo de altura no A* |
| [StructBlend](https://www.curseforge.com/minecraft/mc-mods/structblend) (mod) | preenche o vão sob a construção com terra/pedra, **corta o morro** que atrapalha, faz rampa suave de ~16 blocos (24 em fundação larga), nivela o chão com o piso para não sobrar valeta de 1 bloco | a rampa de saída a partir da borda do lote; "não deixar valeta" em volta da casa |
| [RoadArchitect](https://modrinth.com/mod/roadarchitect) | acha as vilas e liga com A* e cache de terreno; a rede persiste no save | cache de altura por coluna, rede de ruas persistida |
| [Settlement Roads](https://github.com/Coun7ered/settlement-roads-new) | ruas entre vilas; planeja evitar água e montanha com túnel e ponte | — (só traçado) |
| [Village Terrain Filter](https://modrinth.com/project/e16fJykz) | recusa vila em terreno com desvio de ±10 blocos (grade de 16, raio 64) | a medida de "quão acidentado" por grade amostrada |
| [GDMC — Impressões do desafio (2021)](https://ar5iv.labs.arxiv.org/html/2108.02955) | os jurados **premiam adaptar ao terreno** e criticam "aplainar um grande grid e construir em cima"; pedem caminhos transitáveis com desnível > 1 bloco, nivelar desníveis curtos, escada diagonal em vez de escada de mão; fundações desproporcionais e pilares de 50 m são o erro típico | **mexer o mínimo**: corrigir o que impede andar, não achatar a vila |
| [GDMC — relatório do 1º ano](https://arxiv.org/abs/2103.14950), [competição](https://arxiv.org/abs/1803.09853) | escolha de lote por planura (5.000 posições avaliadas por elevação em volta e na frente da casa) | nota de lote por desvio de altura (já fazemos em `LotLevel`) |
| [Patterson & Ward, Temple 2022](https://cis.temple.edu/~pwang/5603-AI/Project/2022S/pattersonblaker/Ward_Patterson_Presentation.pdf) | terraplanagem por BFS no mapa de alturas; lote pelo desvio-padrão da altura em subgrades; A* com a altura como barreira | BFS de alcançabilidade a partir do sino |
| [HSTEHSTE GDMC 2022](https://github.com/HSTEHSTEHSTE/GDMC_2022/blob/main/README.md) | mapa de alturas `MOTION_BLOCKING_NO_LEAVES`; rua 1 acima da água | a mesma fonte de altura que `SiteMarker` já usa |

**Lição comum:** ninguém sério achata a vila. O que funciona é (a) garantir que **todo ponto
importante seja alcançável a pé** com degrau ≤ 1, (b) corrigir **só** o que impede isso, com
laje/escada/rampa, e (c) preparar o lote da obra antes do construtor chegar.

## 3. A proposta — o "Calceteiro" (nome a decidir)

Uma profissão nova, ou uma tarefa nova de profissão existente (pergunta 1). Ela trabalha sobre um
**mapa de alcançabilidade da vila**, recalculado devagar, e corrige defeitos em ordem de impacto.

### 3.1 O mapa (só leitura, fatiado por ciclo)

- Grade de colunas da caixa da vila (Regra 45 / Emenda 6), altura de
  `MOTION_BLOCKING_NO_LEAVES`, chunk só se carregado (§11: nunca forçar carregamento).
- **BFS a partir do sino** com a regra de passo do aldeão: vizinho alcançável se `|Δy| ≤ 1`, cabe
  em pé (2 de ar) e não é fluido. Custo fatiado (`CycleCost` mede a fase; meta < 2 ms por ciclo).
- Alvos que **precisam** ser alcançáveis: porta de cada casa, sino, bancadas, baús da colônia,
  boca da mina, roças, lote da obra aberta.
- Saída: lista de **defeitos**, cada um com posição, tipo e quanto custa corrigir.

### 3.2 Os defeitos e a correção de cada um (sempre a menor que resolve)

| Defeito | Detecção | Correção | Material |
|---|---|---|---|
| **Buraco** (1–3 de fundo, ≤ 4 colunas) na rua ou entre alvos | coluna com chão abaixo dos 4 vizinhos | aterrar até o nível dos vizinhos | solo do bioma (`foundationGroundAt`) |
| **Degrau de 2** no caminho mínimo entre alvos | aresta do BFS recusada por `Δy = 2` | **laje** no meio (meio-degrau, RoadWeaver) ou 1 bloco de terra | laje do bioma / terra |
| **Degrau de 3+** | componente desconexa do BFS | **escada** em diagonal (GDMC) ou rampa de 1:1, nunca escada de mão | escada do bioma |
| **Morrinho** (1–2 acima) dentro da rua | coluna acima dos 4 vizinhos na rua | rebaixar (cortar) até a rua | o que sai vai para o baú |
| **Valeta em volta da casa** | coluna 1 abaixo encostada na parede | aterrar (StructBlend) | terra |
| **Lote quase bom** (o `LotLevel` recusou por `OFF_ROAD_LEVEL` com ≤ N colunas) | `LotRefusals` | preparar o lote antes da obra: cortar/aterrar ≤ 1 camada | terra / o que sai |
| **Declive grande / montanha** | desvio > X na grade (Village Terrain Filter) | **não achatar**: ligar por escada/rampa e deixar o relevo | — |

### 3.3 Regras que não se negociam

- **Regra 3:** nunca corta peça de vila, bloco do jogador, cultivo, baú, cama; nunca aterra
  sobre fluido sem decidir (água de roça e de canal são da vila).
- **Mínimo de mudança:** só corrige o que está no caminho mínimo entre alvos ou no lote da obra.
  Orçamento por dia de jogo (ex.: 64 blocos movidos) para a vila não virar canteiro.
- **Sem achatar montanha:** acima de um desnível X, liga com escada; não remove o morro.
- **Material real:** o que se aterra sai do baú (terra do coletor de superfície, pedregulho do
  mineiro); o que se corta entra no baú. A colônia não inventa matéria (ADR-028).
- **Não brigar com a rua:** a rua da colônia (`RoadIndex`) é a espinha; o calceteiro conserta a
  rua existente antes de criar caminho novo.

### 3.4 Prova (como saber que funcionou)

- GameTest por defeito (buraco, degrau de 2, de 3, morrinho, valeta) num cenário fechado em
  bedrock (memória `planejador-acha-caminho-fora-do-cenario`).
- Métrica de jogo: **% de alvos alcançáveis a pé a partir do sino** (linha nova `VC_REACH`),
  antes e depois; e no `time_ledger`, a queda de `blocked`/`stranded` e de `walk` do mineiro e do
  construtor.

## 4. Fases sugeridas

1. **Só medir** (sem mexer no mundo): o mapa + `VC_REACH` + lista de defeitos no `/vc log`.
   Dá o número que decide o resto (regra "o log decide a constante").
2. **Buraco e valeta** (aterrar, o mais barato e o que mais prende aldeão).
3. **Degrau de 2 com laje** e **morrinho na rua**.
4. **Escada para desnível grande** e **preparação de lote quase bom**.

## 5. Perguntas para o autor

1. Profissão nova ("Calceteiro"/"Agrimensor"), ou tarefa do construtor quando não há obra, ou do
   mineiro quando sem ramal?
2. Laje como meio-degrau é aceitável no visual da vila, ou só blocos inteiros/escadas?
3. Orçamento: quantos blocos movidos por dia de jogo?
4. Pode aterrar **água rasa** (poça de 1 bloco) que não é de roça nem de canal?
5. Começa pela fase 1 (só medir) antes de mexer no mundo?
