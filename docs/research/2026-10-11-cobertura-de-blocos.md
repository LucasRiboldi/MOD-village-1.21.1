# Cobertura de blocos por profissão — 2026-10-11

Pedido do autor: *todas as profissões devem conseguir produzir todos os blocos que as
estruturas da vila usam, e todos os blocos que produzem os blocos da vila; carpintaria
e fundidor produzem um pouco de cada item para o estoque.*

**Fonte de verdade:** `StructureCoverageGameTest` (roda na bateria) escreve
`build/gametest/structure-coverage.txt` (quem produz cada item) e `ChainRootsGameTest`
escreve `build/gametest/chain-roots.txt` (onde cada cadeia começa). As contagens
abaixo são da 0.3.28; regere-as rodando `gradlew runGametest`.

## Divisão por material (escolha do autor) — `CraftFamily`

| Material | Ofício | Exemplos |
|---|---|---|
| Madeira | Carpinteiro | escada, porta, cerca, baú, barril, escada de mão, tocha, estante |
| Pedra, barro, terracota, fornalha | Pedreiro | tijolo, muro, terracota tingida, fornalha, alto-forno, defumador, vaso |
| Metal, vidro, gelo, neve | Fundidor | barra e bloco de ferro, lampião, caldeirão, vidraça, gelo compactado, bloco de neve |
| Tecido e corante | Pastor | lã colorida, tapete, cama, estandarte, corante, couro |
| Roça | Fazendeiro | papel, livro, farinha de osso, bloco de osso, feno |

## Contagem (187 itens nas cinco vilas, com a cadeia de receitas)

| Dono | Antes (0.3.27) | Depois (0.3.28) |
|---|---|---|
| Carpinteiro | 103 | 42 |
| Pedreiro | 27 | 31 |
| Fundidor (bancada) | — | 12 |
| Pastor (bancada) | — | 40 |
| Fazendeiro (bancada) | — | 5 |
| Fundidor (fornalha / superfície) | 12 / 7 | 12 / 7 |
| Lenhador, mineiro, pastor (coleta), fazendeiro | 10, 1, 3, 8 + 2 | iguais |
| "O mundo" (sem tarefa) | 14 | 14 no relatório; 9 deles agora têm `ResourceType` e coleta |

**Folhas de cadeia sem `ResourceType`:** 24 → 9. Ficam vara de blaze e bolsa de tinta
(drops da Regra 41) e capim alto, samambaia grande e flor-tocha (4ª tentativa; não se
obtêm quebrando bloco).

## Estoque de um pouco de tudo — `ItemStock`

Os itens das plantas do estilo da vila e os ingredientes deles (três degraus de
receita), da madeira (carpinteiro) e do metal e vidro (fundidor): 8 de cada, em rodízio,
abaixo do teto de 64. A obra vem antes. Item que não sai fica 5 min fora do rodízio.

## Coleta nova — `PlantPatch`

- Fazendeiro: papoula, dente-de-leão, centáurea, tulipa vermelha, margarida, grama,
  samambaia, bambu e cana. Bambu e cana saem pelo topo (a base volta a crescer); flor,
  grama e samambaia ganham uma farinha de osso na grama de onde saíram.
- Fundidor: gelo (toque suave) e bola de neve (pá), com rota no bioma nevado.

## O que fica para o playtest

- Se o estoque de 8 de cada gasta madeira demais antes de a obra pedir.
- Se o pastor e o fazendeiro, agora com bancada, deixam o ofício de coleta parado.
- Os itens fundidos sem `ResourceType` (terracota esmaltada) ainda não entram no estoque.
