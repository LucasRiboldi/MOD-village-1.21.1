# O mineiro autônomo — movimento, segurança e busca de minério

**Modo:** DEEP · **Data:** 2026-09-25 · **Objetivo:** substituir sistema
(movimento do mineiro) com decisão documentada antes do código.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Mappings | Yarn 1.21.1+build.3 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.116.15+1.21.1 |
| Conferido em | 2026-09-25, via `gradle.properties` |

## A pergunta

Pedido do autor, depois do playtest de 25-09 em que o mineiro empacou na mesma
pedra de todas as sessões (553, 39, 158), ficou preso a y=41 e saiu da escala:

> qual deveria ser a movimentação natural do mineiro, independente, capaz de
> entrar e sair da mina sem ajuda do jogador, colocando e tirando bloco para se
> mover sem travar, que analise toda a área acessível registrando e colhendo os
> veios mais valiosos, evitando buracos, água e lava, e resolvendo as próprias
> dificuldades.

## Resposta direta

O mineiro trava porque **o mod cava, mas quem anda é o Vanilla, e o Vanilla não
cava nem constrói**. Toda a mobilidade depende de a mina real ficar idêntica à
geometria planejada; qualquer diferença (caverna, degrau alto, vão no piso,
fluido) vira um lugar onde a navegação não acha caminho, e o mineiro desiste,
volta, desiste de novo.

A saída que os quatro projetos estudados convergem é a mesma, em graus
diferentes: **o trabalhador precisa de uma camada de movimento própria, que
pode quebrar e pôr bloco**, e de regras de segurança que rodam antes de cada
bloco quebrado. A proposta é construir isso em três fases, da menos para a mais
invasiva, cada uma com teste próprio — ver §6 e a ADR-025.

## 1. O que existe hoje no mod

- `[FATO]` O mineiro anda só pela navegação Vanilla. O `GoToWorkTargetTask`
  mantém o destino na memória `WALK_TARGET`, e o caminho sai do
  `LandPathNodeMaker` (`fabric/brain/GoToWorkTargetTask.java`, javadoc da
  classe).
- `[FATO]` A mina é uma **geometria fixa** (`MineShaft`: caracol 3-2-3-2, sala
  5×5×2, quatro ramais de dez degraus e sala, nível a cada dez blocos, outra
  boca no fundo). A ordem de corte é um índice salvo (`positionAt`).
- `[FATO]` Cada corte olha minério no bloco e ao lado e segue o veio
  (`MineCuts` → `OreVein.isOre`/`OreVein.beside` → `MineArm.followVein`).
- `[FATO]` O mineiro só mexe no mundo para se locomover em dois lugares:
  `MineFlooding` tapa a face de onde vem a água que a picareta liberou, e
  `StrandedEscape` quebra bloco para abrir saída — e desiste quando não há
  "caminho natural e seco" (`StrandedEscape`, log `cannot dig out`).
- `[FATO]` A marca que afasta a pedra recusada (`MineMarks.refuse`) vive só em
  memória (`ServerMemory`); o save não a guarda. Log de 25-09: a mesma pedra
  553, 39, 158 recusada em 01:xx, 08:32, 08:34, 08:53 e 09:15.
- `[FATO]` Sem obra pedindo e com o piso atendido, não havia tarefa de
  mineração (log de 25-09: vinte minutos de `no miner work: no task open`).
  Corrigido no `2a5a762` (meta de pedra com o espaço dos baús dos mineiros).

## 2. O que o Vanilla oferece — e o que não oferece

- `[FATO]` Penalidades padrão de `PathNodeType` (1.21.1, `<clinit>` lido por
  `javap`): `LAVA = -1` (nunca), `WATER = 8`, `WATER_BORDER = 8`,
  `DANGER_FIRE = 8`, `DAMAGE_FIRE = 16`, `DANGER_OTHER = 8`,
  `DAMAGE_OTHER = -1`, `BLOCKED = -1`, `BREACH = 4`. **Água não é proibida**,
  só cara: evitar água exige `setPathfindingPenalty(WATER, -1)` (já estudado em
  `opcoes-de-melhoria-das-profissoes.md`).
- `[FATO]` `LandPathNodeMaker` chama `getSafeFallDistance` e `getStepHeight` e
  nunca `setBlockState`/`breakBlock`: **a navegação só lê o mundo**.
- `[FATO]` Alvo em ar é trocado pelo primeiro chão abaixo
  (`MobNavigation.findPathTo(BlockPos, int)`, ver `E32-miner-walk-target.md`).
- `[INFERÊNCIA]` Queda segura padrão ≈ 3 blocos para mob sem alvo
  (`MobEntity.getSafeFallDistance()` delega para `getSafeFallDistance(0)`;
  valor não reconferido aqui). Baritone usa o mesmo número
  (`maxFallHeightNoWater = 3`).
- `[DECISÃO]` Não há degrau Vanilla nem da Fabric API que faça um mob cavar ou
  construir para passar. A camada é do mod; **não pede Mixin** — composição
  sobre `ServerWorld.breakBlock`/`setBlockState` e sobre a navegação existente
  (degrau 6 da escada de extensão).

## 3. O que outros projetos fizeram

| | Movimento | Põe/tira bloco para andar | Fluidos e quedas | Como acha minério |
|---|---|---|---|---|
| **Baritone** ([código](https://github.com/cabaletta/baritone)) | A* próprio sobre movimentos: `Traverse`, `Ascend`, `Descend`, `Diagonal`, `Downward`, `Fall`, `Pillar`, `Parkour` | Sim: quebra e põe (`allowBreak`, `allowPlace`, ambos ligados), mas **colocar custa caro** (`blockPlacementPenalty = 20`) e quebrar tem penalidade extra (2) | Queda sem água até 3; custos em ticks (andar ≈ 4,6; água ≈ 9,1; subir escada ≈ 8,5); `strictLiquidCheck` para não abrir fluido | Modo *legit* (`legitMine`): só minério **visível** a até ~10 blocos; sem nada à vista, anda em linha na mesma altura (branch mining) até ver. Lista de até 64 alvos por distância, recalculada a cada 5 ticks |
| **mineflayer-pathfinder** ([código](https://github.com/PrismarineJS/mineflayer-pathfinder)) | A* com `Movements` configuráveis | `canDig`, `scafoldingBlocks`, torres 1×1 | `maxDropDown = 4`, `dontCreateFlow` (não quebra bloco que segura líquido), `liquidCost` | — (é só o caminho) |
| **MineColonies** ([wiki](https://minecolonies.com/wiki/buildings/miner/)) | Poço com escada de mão e plataforma a cada 3 blocos; ramais pelos níveis | Sim: escada, tábua, laje, cerca, tocha | **Preenche** água, lava, areia e cascalho com bloco de enchimento; pula plataforma onde acha ar | Túneis de exploração nos níveis escolhidos; ao achar minério, **esvazia o veio inteiro** |
| **Workers (talhanation)** ([código](https://github.com/talhanation/workers)) | Escava um volume definido (túnel, poço, sala) | Sim | **Tapa** fonte de água e lava; opcionalmente **põe piso** onde há buraco para não cair; tocha no escuro | Minera o minério **exposto nas paredes** do volume |

Três lições comuns, `[INFERÊNCIA]` a partir da tabela:

1. **Ninguém confia no pathfinding do mob para andar num lugar que ele mesmo
   escava.** Ou o trabalhador tem movimento próprio (Baritone, mineflayer), ou a
   obra garante que o lugar é andável ao ser cavado (MineColonies, Workers).
2. **Fluido e buraco se tratam antes de quebrar**, não depois: não abrir
   bloco que segura líquido, tapar a fonte, pôr piso no vão.
3. **Minério se acha pelo que é visível** a partir de onde se anda, e se
   explora em linha reta numa altura fixa quando nada está à vista — nunca
   por varredura oculta ("x-ray"), que seria antinatural para um aldeão.

## 4. Onde o mineiro atual falha, lido contra as lições

| Lição | Hoje | Consequência medida |
|---|---|---|
| 1. Lugar cavado tem de ser andável | Geometria fixa + navegação Vanilla, sem conserto do piso nem degrau | Pedra 553, 39, 158 inalcançável em todas as sessões |
| 2. Fluido e vão antes de quebrar | Só a água **depois** de liberada (`MineFlooding`); nada para lava vizinha nem para vão | `StrandedEscape` desiste ("no natural, dry way up") |
| 3. Minério visível e registrado | Segue veio **no corte**; não lembra minério visto de longe; nada é salvo | Veio visto e não alcançado some ao recarregar |
| Memória de fracasso | `MineMarks` só em memória | Volta à mesma pedra a cada sessão |

## 5. O movimento natural proposto

O mineiro de um jogador cuidadoso faz, nessa ordem de preferência:

1. **Anda** pelo que já está aberto (navegação Vanilla — barata e natural).
2. **Sobe ou desce um degrau** cavando o bloco da cabeça ou o do degrau
   (escada de mina, 1 de altura por 1 de avanço), nunca poço vertical sem
   escada.
3. **Tapa** vão no piso com pedregulho antes de pisar, e **ponte** de um bloco
   sobre buraco de até 3 de largura; buraco maior, contorna.
4. **Pilar** (pular e pôr bloco embaixo) só para sair de buraco sem outra
   saída. O pilar fica no mundo, como o jogador o deixa.
5. **Não quebra** bloco que tem lava ou água na face seguinte; se precisa
   passar, **tapa** a face do fluido primeiro (pedregulho), depois quebra.
6. **Escada de mão** só no poço vertical da mina, e só com escadas que a
   colônia fabricou (a colônia não cria matéria — regra do Construction-System).

Tudo isso é **caro** no custo do caminho, como no Baritone: pôr bloco vale ~20
passos, quebrar vale a dureza do bloco mais uma penalidade. O mineiro só
modifica o mundo quando andar não resolve.

### Busca de minério "legítima"

- **Registro de veios (`OreLedger`)**: a cada corte, o mineiro registra os
  minérios **expostos** (face voltada a ar) que estão a até ~8 blocos em linha
  de visão das células andáveis por onde passou. Persistido por colônia, com
  teto (ex.: 128 entradas), invalidado ao ler o mundo (bloco que não é mais
  minério sai).
- **Valor**: diamante > esmeralda > ouro > lápis ≈ redstone > ferro > cobre >
  carvão, ponderado pela distância de caminhada (custo do caminho, não
  distância em linha reta) e pela necessidade da colônia (a meta de ferro e
  carvão sobe o peso).
- **Exploração**: sem veio registrado alcançável, segue a geometria atual
  (ramais na altura do nível) — que já é o "branch mining" do Baritone e do
  MineColonies.
- **Veio inteiro**: o `OreVein` já segue o veio; com o registro, o mineiro volta
  ao veio que viu de longe em vez de só ao que tocou.

### Resolver as próprias dificuldades

- **Marca de fracasso persistida**: `MineMarks` vai para o save, com prazo em
  tiques de mundo; a pedra recusada não volta na sessão seguinte.
- **Diagnóstico antes de desistir**: se o destino não é alcançável pela
  navegação, o planejador local (fase 2) procura um caminho com quebra/colocação
  num raio pequeno (≤ 16 blocos, orçamento de nós por tique); só sem caminho a
  pedra é marcada.
- **Encalhado sai por conta própria**: o `StrandedEscape` ganha a mesma caixa de
  ferramentas (degrau, ponte, tampa, pilar); "sem caminho natural e seco" deixa
  de ser o fim.

## 6. Plano em fases (ADR-025, aceita)

| Fase | O quê | Risco | Prova |
|---|---|---|---|
| **1. Mina que se mantém andável** | Ao cortar: piso garantido sob cada célula de passagem (pedregulho do baú do mineiro), fluido vizinho tapado **antes** de quebrar (água e lava), recusa de quebrar bloco que segura fluido sem tampa possível. `MineMarks` persistido. Penalidade de água do mineiro em -1 | Baixo: só acrescenta blocos que a colônia tem | GameTests: corte com lava ao lado não abre a lava; vão no piso vira pedregulho; pedra recusada continua recusada depois de salvar e carregar |
| **2. Planejador local com ações** | A* limitado (raio ≤ 16, orçamento por tique) sobre andar, degrau, cavar, pôr, ponte, pilar, com custos à la Baritone; usado quando a navegação Vanilla falha e pelo `StrandedEscape` | Médio: custo de CPU e comportamento novo | GameTests de cenário: buraco de 2, degrau de 2, poço sem escada, bolsão de água; métrica de nós por tique no `CycleCost` |
| **3. Registro de veios e prioridade por valor** | `OreLedger` persistido; escolha do próximo alvo por valor ÷ custo | Baixo-médio: mais estado no save | Testes de unidade da prioridade; GameTest de veio visto de longe e colhido |

## 7. Riscos

- `[RISCO]` **CPU**: A* com quebra/colocação explode se ilimitado. Mitigação:
  raio e orçamento de nós por tique, cache por posição, e só roda depois de a
  navegação Vanilla falhar (a maioria dos passos continua barata).
- `[RISCO]` **Recurso**: pôr bloco consome pedregulho; a colônia precisa ter.
  O mineiro produz pedregulho de sobra, mas o primeiro vão pode chegar antes.
  Mitigação: pedregulho do próprio baú primeiro; sem ele, a célula é marcada e
  o mineiro contorna.
- `[RISCO]` **Naturalidade**: pilar e ponte deixam blocos no mundo. É o que um
  jogador faz, e o autor pediu; registrar para não "limpar" depois.
- `[RISCO]` **Compatibilidade**: nenhuma Mixin nova; tudo sobre
  `ServerWorld.setBlockState/breakBlock` e navegação existente. Risco LOW.
- `[RISCO]` **Proteção**: quebrar/pôr bloco passa por `BlockProtection`
  (nunca estrutura da vila nem bloco do jogador).

## 8. O que ainda precisa de validação

- `[FATO]` **Geometria de 553, 39, 158 lida do save e reconstruída** em
  `MineStallForensicGameTest` (x 545..556, y 37..47, z 153..162; 119 células
  abertas). A navegação Vanilla **acha caminho e chega** ao lugar de pisar
  (552, 40, 158) a partir de onde o mineiro parou (549, 41, 158). Nenhuma das
  falhas geométricas da §4 explica aquele travamento: não há vão, degrau alto
  nem fluido no recorte.
- `[FATO]` Mesmo recorte: 51 blocos de `dirt_path` no subsolo da mina (y 30-46,
  inclusive uma sala 5x5 em y=34). Origem não provada; os dois únicos
  escritores são `RoadPaving` (que exige chão natural) e o nivelamento do
  construtor.
- `[HIPÓTESE]` O travamento está no cérebro, não no mapa: tarefa de andar que
  não roda, alvo trocado ou caminho descartado. Para decidir, a linha
  `stuck`/`gives up` do mineiro passou a trazer `brain: activity …, our walk
  task running|not running, walk target …, navigation …`
  (`MinerReport.brainOf`). **A próxima sessão de jogo decide.**
- `[VALIDAÇÃO NECESSÁRIA]` `getSafeFallDistance` do aldeão sem alvo, por
  `javap` do corpo de `Entity.getSafeFallDistance(float)`.
- `[HIPÓTESE]` A navegação Vanilla basta para 90%+ dos passos se a fase 1
  garantir piso, altura livre e degrau de 1. Impacto se errada: a fase 2 vira
  obrigatória, não reserva.

## 9. Fase 1 — o que entrou (2026-09-25)

| Item do plano | Estado | Onde | Prova |
|---|---|---|---|
| `MineMarks` persistido | feito | `WorkMarksSavedData` (chave `villagecolony_marks`), ligado no `ServerLifecycleHandler` | unitários de ida e volta do NBT e de `marks()`/`restore()`; **não visto em jogo** |
| Piso sob a passagem | feito | `MineFloor.patch`, chamado no `MinerHands` depois da vedação | 4 GameTests: vão vira pedregulho; degrau planejado, célula fora da passagem e chão firme ficam como estão |
| Penalidade de água -1 | feito | `MinerCaution.keepOutOfWater`, a cada passo do mineiro | GameTest com controle: sem a cautela o aldeão atravessa o fosso nadando; com ela, dá a volta seca |
| Fluido tapado antes de quebrar | já existia, equivalente | `MineFlooding.seal` roda no mesmo tique da quebra; o fluido só corre no tique agendado seguinte | GameTest antigo `thePickThatOpensWaterSealsItAtOnce` |
| Recusar quebrar bloco que segura fluido sem tampa possível | **pendente** | caso: nascente protegida (água da vila) que o `seal` não pode tapar | — |
| Diagnóstico do cérebro no travamento | feito | `MinerReport.brainOf` | só em jogo |

Desvios do plano, registrados:

- O piso **não consome** pedregulho do baú, como a vedação do
  `MineFlooding` também não. A conta do material entra com a fase 2, que põe
  bloco com frequência; na fase 1 são vãos raros.
- A geometria repete dez células: o último degrau de cada escada cai dentro da
  sala que ela abre (seis no caracol, quatro no ramal). É a geometria salva, não
  defeito; `MineShaft.plannedCells()` devolve 210 células para 220 índices.

## 10. Próximo passo

Sessão de jogo para ler as linhas `brain:` do mineiro e ver `The mine floored`;
depois, a fase 2 (planejador local com ações) com o diagnóstico em mãos.
