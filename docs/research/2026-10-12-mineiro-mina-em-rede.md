# Mineiro: por que parou, como outros projetos fazem, e a mina em rede

**Modo:** FORENSIC (por que o mineiro parou) + DEEP (comparação e proposta).
**Versão:** MC 1.21.1, Fabric, Yarn 1.21.1+build.3, mod 0.3.33.
**Pedido do autor (12-10):** o mineiro entra na mina, abre vários caminhos minerando tudo e
seguindo os veios, cobre zonas grandes, desce de nível e repete até o fim; depois cria caminhos
laterais e volta subindo; conhece o caminho; se não puder subir, põe bloco; sempre seguro, sem cair
em buraco. A mina é um percurso infinito e interligado, construído pelo mineiro.

## 1. O que o mineiro produziu, sessão a sessão

`MINED` no diário de ações (`villagecolony-actions-*.jsonl`) — `[FATO]`:

| Sessão | Blocos | Desistências |
|---|---|---|
| 08-10 18:18 | 1136 | 11 |
| 08-10 23:45 | 1335 | 17 |
| 09-10 11:43 | 926 | 25 |
| 11-10 10:04 | 1024 | 32 |
| 11-10 19:26 | 1340 | 12 |
| 11-10 23:12 (0.3.31) | 454 | 9 |
| **12-10 09:09 (0.3.32)** | **0** | 11 |

Mesmo save, mesma mina. A queda não veio de uma regra nova que proíbe cavar: veio do estado em
que a mina chegou.

## 2. Por que parou — leitura do save

Mina gravada (`villagecolony_colonies.dat`, `mines[0]`) — `[FATO]`: entrada do nível em −377, 38,
−1000, escada para o norte, galeria para o sul, cursores dos ramais 441 / 398 / 475 / 481. Três
ramais esgotados, um aberto (o 2), com pedras em y 20–25 a ~25 blocos da entrada.

Busca de alcance no save (`tools/reach.py`, regras de aldeão: pé em ar, cabeça em ar, sólido
embaixo; sobe 1, cai até 3) — `[FATO]`:

- **Descendo da boca (−392, 68, −985):** chega-se a 7.952 lugares de pé, de y=72 até y=12.
- **Subindo de dentro (−375, 29, −999):** não se passa de **y=34**.
- Degraus de mão única no caminho principal: **−377, 35, −1004 → −378, 32, −1004** (3 blocos) e
  −377, 31, −984 → −378, 28, −984 (3). Os outros estão em cavernas naturais que a mina rompeu
  (x −354…−367).
- **As pedras do ramal aberto são alcançáveis a pé de onde os mineiros estavam:** há lugar de pé
  em −372, 21, −1025, ao lado da pedra −372, 20, −1025.

Juntando com o log (`gave up the stone … got no closer than 18 blocks … walking to −378, 32,
−1004`) — `[INFERÊNCIA]`:

1. Os mineiros desceram (pela navegação do jogo, que aceita queda de 3) para baixo de y=34 e
   **não sobem de volta**.
2. A perna da mina (`MinerLeg.legTowards`) os mandava pela ordem de cavar até a entrada do nível,
   **através do degrau de 3**, e não direto ao lugar de pé da pedra. Sem chegar mais perto em 400
   tiques, a tarefa voltava (`MineLease`), e o próximo pegava a mesma pedra.
3. O ramal 2 nunca termina (ele só termina no fim ou com posição impossível, não com pedra que
   ninguém alcança), e o nível **só desce com todos os ramais terminados**
   (`Mine.advanceIfEveryOpenArmIsDone`). A mina parou com um ramal só aberto.
4. Até a 0.3.31 o mineiro sem ramal raspava pedra na superfície; a Regra 53 (0.3.32) tirou isso, e
   o que era ocupação virou ócio visível.

## 3. Como outros projetos fazem

### MineColonies (Forge/NeoForge) — o mais parecido

Código lido: `ldtteam/minecolonies`, ramo `version/main` (12-10-2026) — `[FATO]`:

- **Poço vertical** com escada de mão na parede de trás e enchimento de pedregulho
  (`EntityAIStructureMiner.advanceLadder`, `SHAFT_RADIUS = 3`).
- **Piso garantido:** antes de descer, todo bloco não sólido, fluido ou minério num quadrado de
  11×11 (`SAFE_CHECK_RANGE = 5`) na altura do piso vira pedregulho (`secureBlock`). Não há buraco no
  chão do poço.
- **Níveis** ao longo do poço; cada nível é um **grafo de nós** a cada 7 blocos (`MineNode`,
  `DISTANCE_TO_NEXT_NODE = 7`) com tipos `TUNNEL`, `CROSSROAD`, `BEND_LEFT/RIGHT`, `CROSS_THREE_*`
  (cada um é um esquema: corredor com piso, apoio de madeira e tocha).
- Ao terminar um nó, os vizinhos entram na fila de abertos (`MinerLevel.closeNextNode`); **nó com
  fluido não é aberto**.
- O mineiro trabalha **de pé no centro do nó-pai já pronto** (`searchANodeToMine`,
  `standingPosition` no pai) e, se o pai não estiver pronto, volta a ele: ele nunca anda por onde
  não foi construído.
- Veio: minera o veio inteiro, não só o que está no corredor (wiki).

### Baritone (bot de jogador)

`cabaletta/baritone`, `Settings.java` — `[FATO]`: A* sobre movimentos (andar, subir 1, descer,
cair, coluna) com custo; `allowPlace` (põe bloco para fazer ponte e coluna), `allowBreak`,
`maxFallHeightNoWater = 3` (queda máxima sem dano), `allowDownward` (desligado obriga escadaria em
vez de poço), `legitMine` (só minera o minério que vê, explorando). A lição útil para aldeão: **todo
movimento tem volta** — se desce, sabe subir; se falta piso, põe bloco.

### Outros mods de aldeão minerador

Daiku (mineração em ramal), Hire Villager Worker e The Worker (cavam para baixo), Villages Alive
(pedreiras 14×14): nenhum descreve rede de túneis com retorno seguro — `[FATO]` pela descrição
pública; código não lido (`FUTURE RESEARCH`).

## 4. Comparação

| Aspecto | Village Colony hoje | MineColonies | O que o autor pediu |
|---|---|---|---|
| Descida | escada em caracol de 10 degraus por nível | poço com escada de mão | descer nível a nível até o fundo |
| Nível | salão + 4 ramais fixos que **descem em escada** | grafo de nós a cada 7 blocos, plano | várias galerias, zonas grandes |
| Quando desce | só com os 4 ramais terminados | por escolha de nível | quando o nível está coberto |
| Ramal travado | trava o nível para sempre | nó com fluido nem abre | segue para outro caminho |
| Segurança | sem piso garantido; rompe caverna; quedas de 3 | piso 11×11 garantido, fluido vedado | nunca cair em buraco |
| Caminho | perna pela ordem de cavar | sempre pelo nó-pai pronto | conhecer o caminho |
| Subir | encalhado → fuga com escada (0.3.32) | escada de mão | pôr bloco para subir |
| Mineiros | 1 por ramal (4) | 1 por mina | vários |
| Veio | segue o veio (`MineVein`) | veio inteiro | focar nos veios |

## 5. Proposta: a mina em rede (ADR-042, aguardando o autor)

Ver [`docs/decisions/ADR-042-mina-em-rede.md`](../decisions/ADR-042-mina-em-rede.md). Em resumo:

1. **Poço seguro** na boca da borda (Regra 53): escadaria reta com patamar a cada nível; piso de
   cada patamar e dos 2 blocos em volta garantido com pedregulho (como `secureBlock`); fluido
   vedado. Escada de degrau 1: sempre se sobe.
2. **Nível = grafo de nós** (grade de 7 blocos, plano, 3 de altura × 3 de largura), com corredores
   ligando nós; cada nó tem estado (aberto, em obra, pronto, vedado). Vários mineiros, um nó cada.
3. **Veio primeiro:** ao abrir um corredor, minério visto na parede vira desvio (o veio inteiro,
   com teto de blocos) e volta ao nó.
4. **Andar só pelo que está pronto:** caminho = rota no grafo de nós prontos (BFS), pernas de nó
   em nó de ≤ 7 blocos; nunca por queda. Piso que falta no corredor recebe bloco; degrau de mais de
   1 recebe bloco de degrau.
5. **Caverna e fluido:** abertura do corredor para ar que não é da mina ou para fluido é vedada com
   pedregulho (como MineColonies veda o nó com fluido).
6. **Descer:** quando não houver nó aberto alcançável no raio do nível (ou coberto X%), o poço
   desce mais um nível; o nível antigo continua no grafo (volta-se por ele).
7. **No fundo:** caminhos laterais e uma segunda escadaria de volta, ligando os níveis por outro
   lado — a mina vira circuito, e quem está embaixo tem duas saídas.
8. **Memória no save:** o grafo (nós e estados) por mina, como a MineColonies grava `MinerLevel`.

Custo `[INFERÊNCIA]`: o grafo de um nível tem dezenas de nós; BFS no grafo é barato. O piso e a
vedação gastam pedregulho, que a própria mina produz.

## 6. O que já foi feito (0.3.34, sem esperar a decisão)

- `MinerDirectPath`: quando a navegação do jogo chega ao lugar de pé da pedra, o mineiro vai
  direto, sem a perna. GameTest com controle (parede no meio: não aceita).
- Ramal cuja pedra três vezes seguidas ninguém alcançou **termina** (`MinerSteps.stuck`), e o nível
  pode descer.

`[VALIDAÇÃO NECESSÁRIA]`: jogar e ver `MINED` voltar acima de zero e `went one level deeper` no log.
