# Playtest 2026-10-08, 10:01–10:17 (JAR 0.3.12)

Sessão de 15 min no save "Novo mundo", sem crash (o C1 da 0.3.12 segurou: obra pendente e
varredura pausada no save, como nos dois crashes). Sem Spark nesta sessão.

## `time_ledger.py` (2 janelas de 5 min)

| profissão | work | walk | wait | blocked | idle | veredito |
|---|---|---|---|---|---|---|
| BUILDER | 26% | 21% | 1% | 8% | 44% | fluxo (53% sem trabalhar) |
| CARPENTER | 12% | 32% | 46% | 1% | 10% | fluxo |
| FARMER | 1% | 43% | 41% | 0% | 15% | fluxo |
| LUMBERJACK | 5% | 37% | 0% | **49%** | 8% | travamento + fluxo |
| MASON | 25% | 28% | 36% | 1% | 11% | fluxo |
| MINER | 2% | 37% | 15% | 3% | 43% | fluxo (61%) |
| SHEPHERD | 2% | 45% | 12% | 1% | 40% | fluxo |
| SMELTER | 0% | 5% | 3% | 0% | **91%** | fluxo (94%) |

## `action_report.py` (12,4 min)

| profissão | pegas | feitas | soltas | conclusão | ações/min |
|---|---|---|---|---|---|
| BUILDER | 10 | 9 | 0 | 90% | PLACED 21,4, SET_ASIDE 4,1 |
| CARPENTER | 50 | 47 | 0 | 94% | CRAFTED 9,3 |
| MASON | 37 | 35 | 0 | 95% | CRAFTED 12,3 |
| LUMBERJACK | 2 | 0 | 0 | 0% | FELLED_TREE 7,4 (17 árvores, 92 toras) |
| MINER | 9 | 0 | 9 | 0% | MINED 1,6, GAVE_UP 0,6 |
| SHEPHERD | 9 | 0 | 6 | 0% | SHEARED 1,5 |
| FARMER | 4 | 0 | 2 | 0% | HARVESTED 1,3 |
| **SMELTER** | **191** | **0** | **191** | 0% | — |

Motivos: BUILDER 51× SET_ASIDE "no material"; MINER 4× GAVE_UP "has not moved a block", 2× "got
no closer", 1× "detour failed". `CRAFTED` saía sem o item (corrigido).

## Defeitos achados e corrigidos (0.3.13)

1. **Fundidor em roda: 191 pegas, 191 soltas.** Sem minério em nenhum dos 34 baús, ele pegava a
   tarefa de lingote, soltava e pegava de novo no tique seguinte. Nova trava de reserva
   `SMELT_INPUT` (`SmeltInput`): pedido de fundir só é elegível com o cru num baú — o do pedido ou
   o de peça que as obras usam (o plano B do `SmelterFallback`, pedido de 30-09, continua).
2. **Mineiro pausado por "baú cheio" com o baú com espaço.** Querendo `raw_iron`, a pedra da
   galeria passava do teto por tipo (3×64) e era descartada de propósito — mas "nada guardado"
   disparava a pausa do baú cheio e a tarefa voltava à fila. `Haul.spilled` separa o que ficou no
   chão (baú cheio de verdade) do que o teto descartou; só o primeiro pausa.
3. **Regressão do E1: mineiro "chegava" sem alcançar.** A busca a pé do `MiningTarget` devolvia o
   primeiro lugar de pé que alcançava — o mais perto **dele**, na borda do braço (3,9 pela conta do
   bloco, 4,1 pela posição real). Ele ficava parado no próprio lugar até desistir. Agora vale o
   lugar alcançado mais perto da pedra.
4. **`CRAFTED` sem o item no diário.**

## Vistos e não corrigidos

- **E3 funcionou:** um "nothing to work on in the whole radius" e o descanso; nenhum "still
  sweeping". A espera do fazendeiro é a lavoura crescendo. Duas vezes "could not reach the crop"
  na mesma lavoura, colhida depois.
- **E7 funcionou:** "no wool and the flock is full (16 sheep)"; a lã foi estocada na terceira busca.
- **Lenhador 49% "blocked":** produziu bem (17 árvores), mas as árvores estão a 58–134 blocos e a
  navegação para e recomeça nas caminhadas longas. Plantar perto da vila (viveiro) é o caminho.
- **Mina com entrada que não liga à superfície:** o mineiro 731d73b0 em y65, em cima da mina, não
  acha caminho até a boca em y38 ("found no detour within 16 blocks") — M2.
- **"smelter surface: ainda procurando (clay_ball) — há 9 min"** no `/vc log` depois de a argila
  ter sido estocada: a espera não se apaga quando a busca termina estocando.
- Nenhuma linha `[OBRA]`/`[CADEIA]` nesta sessão: a obra seguiu pondo as peças que tinha.
