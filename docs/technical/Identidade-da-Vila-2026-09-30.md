# Identidade da vila — análise de 2026-09-30

Pedido do autor depois do playtest das 21:01 (JAR `de3af60`, Spark
`qI5h6MXtDA`): *"a casa do mod nascendo diversas vezes; só pode existir um
nascimento desta casa por vila"*, com análise de como a vila é definida, como
duas viram uma, do carregamento do mod e de onde as profissões trabalham.
Correção em `2895d71` (E51); emendas na ADR-003 (Emenda 5) e na ADR-007 (§7).

## 1. O que o jogo mostrou

| Sessão | Colônia nova | Onde | BigHouseMOD | Adultos criados | Absorvida por |
|---|---|---|---|---|---|
| 18:11 | `9a1d34f5` | 1745, -5265 (11 camas) | 1754, 71, -5267 | 7 | `e79a3177`, no mesmo ciclo |
| 18:11 | `8e56a609` | 1745, -5263 (20 camas) | 1774, 70, -5268 | 7 | `e79a3177`, 24 s depois |
| 21:20 | `44cd9e5a` | 1745, -5265 (26 camas) | 1716, 65, -5244 | 7 | — **ela absorveu `e79a3177`** |
| 21:21 | `f7659732` | 1683, -5307 (9 camas) | 1692, 74, -5336 | 7 | `44cd9e5a` |

Entre 21:02 e 21:20 o mesmo aglomerado tentou nascer **35 vezes**, uma por
ciclo de 30 s, e foi adiado por falta de lote seguro para a casa. Quando um
lote apareceu, a colônia nasceu com casa e moradores, e a fusão do fim do
ciclo só a juntou à vila depois disso.

O save, depois de sair do jogo, tem uma colônia só nessa vila (`44cd9e5a`,
41 camas), com **sete BigHouseMOD** — seis a mais, as quatro da tabela e duas
de sessões anteriores, trazidas pelas fusões — e até 42 adultos criados a
mais. O save tem 124 trabalhadores no total. A vila antiga, `e79a3177`, não existe mais como id: os 41
trabalhadores, as 9 construções e a mina passaram para a recém-nascida.

## 2. Como o mod define uma vila (ADR-003)

1. **Coleta:** POIs de cama (`HOME`) num raio de 64 blocos em volta do gatilho.
2. **Aglomerado:** camas a até 32 blocos umas das outras, transitivamente.
3. **Validação:** 3 camas ou mais e 2 aldeões vivos ou mais; só bioma planície.
4. **Centro:** a média das camas, ou o sino, se houver.
5. **Identidade (passo 6):** havia colônia com centro a até 64 blocos?
   Atualiza a existente. Se não, **cria uma nova**.

**O defeito estava no passo 5.** Ele comparava só centros, e a vila do autor
passa de 128 blocos de ponta a ponta: um aglomerado a 65+ blocos do centro
registrado era "vila nova", embora o §5 da mesma ADR mandasse "vila partida
em dois aglomerados continua uma colônia só". A fundação (ADR-018) roda no
nascimento, e por isso cada nascimento indevido ergueu uma BigHouseMOD.

**Quem dispara a detecção** — os três caminhos passam por
`VillageAdoption.detectAround`, e a correção vale para todos:

- a posição de cada jogador, a cada ciclo de 600 tiques;
- a cama de um chunk recém-carregado (fila `pending`, uma por tique);
- a sonda do centro de cada colônia ativa a até 64 blocos de um jogador.

## 3. Como duas colônias viram uma (ADR-007)

No fim da detecção de cada ciclo, `ColonyMergeTrigger` funde todo par que
responda sim a uma das perguntas. Antes da correção eram três:

- centros a até 32 blocos;
- uma construção de uma **encostando** numa da outra;
- os dois centros na mesma vila gerada pelo jogo (estrutura `#village`).

O sobrevivente era o de **mais camas observadas**, uma leitura do instante. Por
isso a recém-nascida, que viu 26 camas, venceu a antiga.

## 4. O que mudou (E51)

- **Identidade** (`ColonyIdentity`, core): o aglomerado pertence à colônia
  existente se (1) o centro está a até 64 blocos, (2) os dois estão na mesma
  vila gerada ou (3) uma cama dele fica a até 32 blocos de uma construção
  dela. Nos casos 2 e 3, os aldeões entram na colônia dona; não nasce
  colônia, a fundação não roda, e centro e contagem de camas não mudam.
  O log diz `Bed cluster at … is part of colony … — no new colony and no
  BigHouseMOD`, uma vez por colônia e motivo.
- **Fusão:** construções a até 32 blocos também fundem ("a poucos blocos uma
  da outra").
- **Sobrevivente:** o de mais trabalhadores; o empate segue a regra das camas.
- **Catálogo do construtor:** conferido, sem mudança. A BigHouseMOD
  (`villagecolony:houses/big_house_mod`) não está na lista de plantas
  (`VillageStructures`, só `minecraft:village/...`). Ela só nasce pela
  fundação, que agora só roda para vila de fato nova.

**Testes:** `ColonyIdentityTest` (7), `ColonyMergeTest` (+1) e o GameTest
`aClusterBesideAColonyBuildingIsNotANewColony`. Com a regra invertida, só
este último falha. **Nada disso foi visto em jogo.**

## 5. Carregamento do mod

`ServerLifecycleHandler.onServerStarted` esvazia os registros e a memória
inscrita (`ServerMemory`) e repõe do save colônias, trabalhadores, baús,
obras (como pendentes), construções e minas. A colônia volta `DORMANT` e só
passa a `ACTIVE` quando o chunk do centro está sendo simulado
(`updateLifecycles`, a cada ciclo). A obra pendente é retomada no primeiro
ciclo da colônia, comparando a planta com o mundo (`ConstructionResume`).

## 6. Onde as profissões trabalham (`VillageFocus`)

- **Planejar obra e detectar vila:** só colônias com jogador a até 64 blocos
  do centro.
- **Executar** (os oito ofícios, construtor, refeição, fuga do encalhado):
  **toda colônia `ACTIVE`**, isto é, com o chunk do centro simulando, esteja o
  jogador perto ou não. É a decisão do autor de 30-09, para a obra aberta
  continuar com o jogador longe. Antes, a execução também parava a mais de 64
  blocos.

Se a regra desejada for "as profissões só trabalham na vila onde o jogador
está", a mudança é em `VillageFocus.isWorking`, e ela desfaz aquela decisão.
**Fica para o autor decidir.**

## 7. Medidas do save (`scripts/world_survey.py`)

Lido do save fechado às 21:23: estruturas geradas (`region/`), camas e sinos
registrados (`poi/`) e o registro do mod. Numeração de norte para sul.

**As vilas geradas.** São 35 no mundo explorado. A caixa 3D (todas as peças
somadas) vai de 86 a 161 blocos em X, de 96 a 158 em Z e de 8 a 55 em Y. Uma
vila Vanilla de planície ocupa, portanto, ~110–160 blocos de lado.

**A vila do autor (#4).** A caixa vai de `1627, 63, -5376` a `1764, 85, -5220`:
**138 × 23 × 157 blocos**, 14 casas, 66 camas e 1 sino. Das 66 camas, **39
são das BigHouseMOD** (6 ou 7 por casa); a vila gerada tinha 27. As camas se espalham por 99 × 15 × **128** blocos. O centro da
colônia (`1745, 71, -5265`) fica a 60 blocos do centro da caixa, perto da
borda sul. Uma das BigHouseMOD (`1777, 74, -5263`) caiu 10 blocos fora da
caixa da vila.

**Vizinhas.** De centro a centro, a vizinha mais próxima fica entre **238 e
636 blocos** (mediana 408). O vão entre as caixas de duas vilas vai de **37
blocos** (#23 e #26, o único par abaixo de 64) a 501. Nenhum par fica a até 32.
A vizinha da vila #4 é a #3, a 318 blocos de centro a centro (vão de 175).

**Colônias.** A #20 tem **duas colônias** (`900b4a06` e `5fc0e0b9`, centros a
78 blocos), nunca fundidas: os chunks não estavam carregados para a pergunta
"mesma vila gerada". As outras quatro colônias estão uma em cada vila (#16,
#22, #25).

**Cuidado com as camas.** Das 35 vilas, 14 aparecem com 0 camas. O jogo só
registra a cama como POI quando o chunk é carregado; vila nunca visitada não
existe para o mod, que procura camas e não estruturas.

## 8. As réguas do mod, em 3D

| Pergunta | Régua | Altura conta? |
|---|---|---|
| Que camas a varredura enxerga | esfera de 64 blocos em volta do gatilho (`getInCircle` do POI) | sim |
| Duas camas no mesmo aglomerado | 32 blocos | não, só X e Z |
| Aglomerado novo ou colônia conhecida | centro a até 64 | não |
| Mesma vila gerada | o jogo responde pela estrutura | — |
| Cama perto de construção (E51) | vão de até 32 até a caixa | não |
| Aldeões que validam o aglomerado | caixa das camas + 32 em todo lado | sim |
| Fusão: centros sobrepostos | 32 | não |
| Fusão: construções encostam | caixas com 1 bloco de folga | sim |
| Fusão: construções próximas (E51) | vão de até 32 | não |
| Planejar e detectar perto do jogador | 64 do centro da colônia | não |

O centro da colônia é a média das camas do aglomerado, ou o sino quando há
um a até 32 blocos de uma cama dele. Ele não é o centro da vila gerada.

## 9. Pendências

- 🔴 Playtest: nenhuma linha `Placed BigHouseMOD` na vila conhecida; a linha
  `is part of colony` aparece no lugar.
- 🟠 Save: sete BigHouseMOD (seis a mais) e os adultos criados com elas na
  colônia `44cd9e5a`. O mod
  não os desfaz sozinho; limpar o save pede decisão do autor.
- 🟠 Execução só na vila do jogador: decisão (§6).
- 🟡 O lenhador (`TreeScanner.findNearestLog`) é 73% do custo do mod no Spark
  de 21h.
