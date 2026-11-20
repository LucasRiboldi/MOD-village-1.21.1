# A lógica do fazendeiro — 2026-11-19

Pedido do autor depois do jogo da 0.4.93 em planície (colônia ccac8347): entender se a ideia do fazendeiro está
aplicada e se ela o deixa útil por mais tempo. Fontes: `latest.log`, o diário `villagecolony-actions-20261119-140317.jsonl`
(fazendeiro `61b9e362`), `docs/RULES.md` e o código de `fabric/work/Farmer*`, `LargeFarmPlans` e `ColonyCycle`.

## 1. O que o fazendeiro fez no jogo

| Medida | Valor |
|---|---|
| Tempo (21 janelas de 5 min, `time_ledger`) | 4% trabalho, 29% andando, 16% bloqueado, 50% ocioso, 1% encalhado |
| Trabalho só na primeira janela | 76% às 14:08; depois, de 0% a 2% em 19 das 20 janelas (14% numa) |
| Colheitas | 185 (52 nos primeiros 10 min); depois de 9 a 16 a cada 20 min, um dia de jogo |
| Pó de osso | 174 aplicações, todas de pó que surgiu (`APPEARED`); só 5 fabricados |
| Semeadas e terra arada | 2 semeaduras; nenhuma terra arada no log |
| Tarefa `COLLECT_FOOD` | 30 vezes pega, 29 largada com "could not reach the crop" (`NO_PATH`) depois de 1.500 a 5.000 tiques |
| Plantação grande | nunca pedida; nenhuma obra de roça em 3 h |

As 29 desistências apontam para o trigo em 386, 64, -178 a -180, **dentro do curral grande** (portão 380, 64, -178).
O curral grande tem 12 cultivos de propósito (Regra 168), e o chão dele fica fora do caminho de quem não é pastor
(Regra 122). O fazendeiro tenta entrar, fica "fenced in" (17 vezes) e anda em volta: é a nota "fazendeiro rodando no
mesmo lugar" (386, 67, -187).

## 2. O que é o trabalho de um fazendeiro

O trabalho segue a terra e a estação, não o estoque. A roça tem um ciclo: arar, semear, regar, adubar, proteger,
colher e guardar a semente. Nos dias em que nada está maduro, o fazendeiro não fica parado:
- amplia o campo, se há mais bocas para alimentar ou terra boa sobrando;
- aduba com o composto que ele mesmo faz;
- conserta a cerca e o caminho da roça;
- transforma a colheita: pão, fardo de feno, ração;
- varia a cultura (abóbora, melancia, cana) para não depender de uma só.

O que está dentro do curral é pasto do rebanho, cuidado de quem cuida dos animais.

## 3. A ideia do mod contra a vida real

| No ciclo real | No mod | Aplicado? |
|---|---|---|
| Colher e replantar | colhe e replanta no mesmo golpe | sim |
| Arar e semear canteiro novo | `FarmerTill` na borda da roça; Regras 289, 315 e 325 | sim, **só com tarefa aberta** |
| Adubar | ronda do pó de osso (Regra 310) | sim, com pó que surge, quase sem composto próprio |
| Compostar | `FarmerCompost` (Regra 237) | sim, só com tarefa aberta |
| Transformar a colheita | ceia e pão (Regra 217), `FarmerBakery` | em parte: pão sim, feno não |
| Ampliar o campo | roça por 15 adultos ou por fazendeiro (Regras 67 e 163); plantação grande (Regras 241 e 267) | **não dispara**: o pedido da plantação grande nasce dentro da tarefa |
| Variar a cultura | trigo, cenoura, batata e beterraba (Regra 325) | em parte |
| Respeitar o curral | o fazendeiro mira o trigo do curral | **não** |

## 4. Onde a ideia falha

1. **O dia do fazendeiro depende da fila de tarefas.** Ele só trabalha com uma tarefa `COLLECT_FOOD` aberta, e a
   tarefa abre pela falta no estoque (`ColonyCycle`, produção `FARMED`). Sem tarefa, nada roda: arar, compostar, o pó de
   osso e o pedido da plantação grande ficam todos dentro de `FarmerWork.step`, que só existe com tarefa. É a fonte dos
   50% ociosos ("no task open for it", 29 vezes).
2. **A plantação grande nunca é pedida.** `LargeFarmPlans.nothingToDo` só é chamado dentro da tarefa. Quando há tarefa,
   o trigo do curral aparece como "maduro", então o fazendeiro nunca está "sem nada", e o pedido não nasce.
3. **O trigo do curral prende o fazendeiro.** `CropPatch.survey` vê toda planta no raio, também a do chão do curral,
   que o caminho dele não alcança. Ele perde até 5.000 tiques por tentativa e a tarefa volta para a fila com punição. É
   a fonte dos 16% bloqueados.

## 5. Conclusão

A ideia está escrita e quase toda implementada, mas fica presa atrás de uma porta: a tarefa de comida. Na vila madura,
com estoque perto da meta, o fazendeiro passa a maior parte do expediente sem tarefa, e o que o faria útil (ampliar,
adubar, compostar, pedir a plantação grande) não roda. O trigo do curral fecha o pouco tempo que sobra.
