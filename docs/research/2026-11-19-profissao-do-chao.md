# Uma profissão para o chão da vila — 2026-11-19

Pedido do autor: uma profissão só para manter o terreno e a calçada da vila. Ela calça a linha do grafo, devolve ao
solo do bioma o caminho fora da linha (uma linha única, que acompanha o grafo), tira o desnível geral (o máximo possível
com 1 bloco de diferença), nivela o espaço entre as estruturas na altura da rua da construção e limpa o que estiver
acima. O grafo atravessa o fardo de feno, e quem calça o tira.

## 1. Quem faz isso hoje

O trabalho já existe no código, espalhado como ajuda de quem está sem tarefa do próprio ofício:

| Trabalho | Regra | Classe | Quem faz |
|---|---|---|---|
| Calçar a linha do grafo | 214, 215, 220, 242 | `PathPaving` | construtor, pedreiro, carpinteiro, fundidor, sem ofício (rodízio) |
| Devolver ao solo o caminho fora do grafo | 208, 242 | `VillageHelpers`, `PathTrim` | pedreiro, carpinteiro, fundidor |
| Tapar falha de rua | 149 | `StreetGaps` | as ajudas |
| Nivelar o vão entre construções em rampa | 242 | `GroundSmoothing` | pedreiro, carpinteiro, fundidor |
| Fila do terreno da vila (desnível ≤ 1) | 323 | `TerrainQueue` | pedreiro, carpinteiro, fundidor, construtor ocioso, sem ofício |
| Tirar neve | 220 | `SnowShovel` | pedreiro, sem ofício |

Medido no jogo da 0.4.93 em planície (diário de ações, ~3 h):

| Quem | Nivelou | Calçou |
|---|---|---|
| Sem ofício | 2.453 | 590 |
| Carpinteiro | 179 | 12 |
| Pedreiro | 148 | 1 |
| Construtor | 103 | 2 |
| Fundidor | 19 | 8 |

**Quem não tem ofício já é o calceteiro de fato**, com 86% do nivelamento e 96% da calçada. Mesmo assim, o grafo
terminou com `toPave=24` e `offLine=211`: 211 blocos de caminho fora da linha esperando voltar ao solo. A fila do
terreno parou no dia 8 com `levelled=0`. O trabalho fica sem dono quando ninguém está à toa.

Essas ajudas são as exceções à regra temporária de 02-11 ("nenhuma profissão faz atividade de outra"). Com um dono,
as exceções 215, 220 e 242 podem acabar.

## 2. Como o Minecraft e o mod fazem uma profissão

- No Vanilla 1.21.1, a profissão (`VillagerProfession`) é ligada a um POI de local de trabalho. O aldeão sem profissão
  procura o POI livre e assume a profissão dele.
- O mod **não** usa `VillagerProfession`. A profissão da colônia (`ProfessionType`) é uma camada por cima da profissão
  Vanilla. Cada bancada do mod é um POI próprio (`ModContent`, `PointOfInterestHelper`) sem profissão Vanilla
  associada, então o Vanilla não emprega nela.
- Uma profissão nova pede:
  1. um valor no `ProfessionType`;
  2. a definição em `ProfessionRegistry`: ferramenta (pá de ferro) e uma capacidade nova (`MAINTAIN_GROUND`);
  3. a bancada: bloco, POI, texturas das seis faces, modelo, loot e a passiva da Regra 132;
  4. a vaga em `ProfessionAssigner` e `HiringQuota`;
  5. a cama (a BigHouseMOD tem 7 camas para os titulares, Regra 109);
  6. os nomes nos relatórios: `WorkerNameplate`, `ActivityLog`, `time_ledger.py`, `action_report.py`,
     `structure_catalog.py`;
  7. os GameTests e o Sentinela.
- **Save:** acrescentar um valor ao enum não quebra o save. Uma versão anterior lê o nome desconhecido como `null` e
  recontrata o aldeão (comentário de `CARPENTER` em `ProfessionType`).

## 3. Nomes

| Nome | Identificador | Por quê |
|---|---|---|
| **Calceteiro** | `PAVER` | ofício real de quem assenta a calçada portuguesa; diz calçada e chão numa palavra |
| Zelador da Vila | `GROUNDSKEEPER` | cuida do chão comum, não de uma obra |
| Guarda-Caminhos | `WAYWARDEN` | soa de jogo; foca na rede de ruas |
| Aplainador | `LEVELER` | foca no nivelamento |

A recomendação é **Calceteiro**: é um ofício que existe e cobre as duas metades do trabalho, a calçada e o chão.

## 4. O dia do calceteiro (proposta)

A ordem vem do que mais atrapalha quem anda:
1. **Linha do grafo a calçar** (`toPave`): a rua que falta. O fardo de feno na linha sai para os baús, e o grafo o
   conta como passável.
2. **Caminho fora da linha** (`offLine`): volta ao solo do bioma, para ficar uma linha só. Ficam a sobra da vila gerada
   (Regra 316), o encontro, a entrada e a única ligação (Regra 208).
3. **Entre estruturas:** o chão até a altura da rua da construção, com o que estiver acima limpo (Regra 242).
4. **Fila do terreno** (Regra 323): buraco e bloco isolados primeiro, depois o degrau maior que 1.
5. Neve na rua, no inverno do bioma.

Uma linha diária `VC_GROUND` mede o efeito: `toPave`, `offLine`, degraus maiores que 1, nivelados e calçados no dia.
