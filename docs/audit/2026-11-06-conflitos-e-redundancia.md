# Varredura de conflitos e redundância — 2026-11-06

Pedido do autor, depois da Regra 214: todo sistema baseado no grafo. Lido no código da 0.4.39 e no jogo da 0.4.38 (50 min). Escolhas do autor,
todas aplicadas na 0.4.40: 1A, 2A, 3 (esquecer o que é velho e usar sempre o sistema atual), 4B, 5A, 6A, 7A, 8A e 9A. Ver ADR-048.

## 1. Caminho que nasce fora do grafo, em vila nova

| Sistema | O que faz | Medido na 0.4.38 |
|---|---|---|
| `RoadExtension` e `RoadPaving` (Regra 15) | Estendem a rua para achar lote | 1 linha no log |
| `SiteOpening.pavePathTo` e `CorridorCut` (Regra 70) | O caminho do lote recuado surge ao abrir a obra, sem aldeão | Sem linha própria no log |
| `BuilderPaving.nextGap` (Regra 149) | Tapa falha de até 2 blocos, varrendo a caixa | 3 calçadas |
| `RoadSpill` | Cresce a caixa da vila por corrente de caminho | Não medido |

Nenhum deles pergunta ao grafo antes de calçar. A rua nova só entra no grafo na releitura seguinte, e o `PathTrim` pode devolver ao solo o
que não virou linha. Nenhuma coluna foi calçada duas vezes nesse jogo: o conflito está no desenho, e não apareceu como repetição medida.

- **A:** todos só propõem colunas ao grafo, e quem calça é sempre o `PathPaving`.
- **B:** só `RoadExtension` e o corredor passam pelo grafo; a falha pequena continua como está.
- **C:** fica como está.

## 2. Duas respostas para "onde tem rua"

`RoadIndex.roadsOf` aparece em 18 arquivos e responde pelo caminho físico. O grafo responde pela linha do meio. Lote, alcance da obra e
traçado usam o índice; o sinalizador usa o grafo.

- **A:** em vila nova, quem precisa de rede pergunta ao grafo; o índice fica como memória das colunas físicas.
- **B:** só a escolha de lote e o alcance da obra passam ao grafo.
- **C:** fica como está.

## 3. Vila antiga

`PathLinks` continua traçando em vila antiga. O plano mestre (§0.2) proíbe editar vila antiga pelo MAE, e `RoadGraphMigration` só lê.

- **A:** vila antiga fica como está: dois sistemas, um para cada tipo de vila.
- **B:** a releitura do grafo também vale para vila antiga, e `PathLinks` sai. Muda uma decisão do plano mestre.

## 4. Código do MAE sem uso

A obra pedida não chama mais a escrita própria do MAE (ADR-047). Sem chamador em produção:
`MaeStructureSite`, `MaeStructureExecution`, `MaeStructureMaterials`, `MaeStructureAccess`, `MaeStructureRoad`, `MaeStructurePieces`,
`MaeTerrainExecution`, `MaeTerrainPreparation`, `MaeRoadExecution`, `MaeRoadPreparation`, `MaeRoadEvidence` e `MaeSectionLocks`: 2.702 linhas.
`MaeWorldWriter` (500) só serve à leitura das regiões antigas. São 14 classes de GameTest do MAE, parte delas testando só esse código.

A prova de geração (`MaeGenerationEvidence`) calcula SHA-256 de cada seção de cada chunk novo: 6,4% do tempo do servidor no Spark
`btHEJgIYWb`, medido com o mundo gerando. Hoje ela só responde "este chunk é novo?". `VillageOrigin.isCurrent` responde quase o mesmo para a
vila; para lote fora da vila, não foi conferido.

- **A:** remove o código sem uso e troca a prova por pergunta mais barata.
- **B:** remove o código sem uso e mantém a prova.
- **C:** mantém tudo, para eventual uso em Fases seguintes.

## 5. Dois leitores da porta

`DoorFronts` (Regra 207) e `PathLinks.entranceOf` respondem a mesma pergunta com regras diferentes, e `RoadGraphRefresh.front` usa os dois em
sequência. `BuildingGaps` usa o segundo. A prova disso é o caso do quintal fechado, em que só o primeiro segue a Regra 209.

- **A:** só `DoorFronts`. **B:** fica como está.

## 6. Quem está livre

`IdleHelps.isFree`, `PathPaving.isFree`, `VillageHelpers` e `GroundPickup` têm cada um a sua lista. A Regra 215 obrigou a somar `IdleHelps` e
`SnowShovel` à do `PathPaving`; nova ajuda esquecida daria dois alvos para o mesmo aldeão.

- **A:** uma pergunta só, com a lista de ajudas em um lugar. **B:** fica como está.

## 7. Quem nivela o chão

`LotTerraform`, `SiteSurroundings`, `GroundSmoothing`, `BuildingGaps` e o solo suave de geração nivelam com réguas próprias. O texto das
Regras 149, 156, 178 e 186 diz que é o pedreiro; o jogo mostra o construtor (24 `LEVELLED`, nenhum do pedreiro), porque a regra temporária
de 11-02 suspendeu as ajudas cruzadas. Ao mesmo tempo o pedreiro fica ocioso 65% do expediente.

- **A:** devolve o nivelamento ao pedreiro, como função secundária. **B:** mantém no construtor e corrige o texto das regras.

## 8. Duas réguas de lote

`LotLevel`, `MaeProtection` e `FoundationPreparation.refusal` decidem o mesmo lote com critérios diferentes: a obra pedida pode passar numa e
não abrir na outra, e 3 de 4 pedidos foram recusados no jogo da 0.4.38.

- **A:** a obra pedida usa a mesma régua do construtor, mais a proteção da vila original. **B:** fica como está.

## 9. Regras que se contradizem no texto

Regra 70 (corredor de 1), 123 (rede de 3) e 186 (rua de 1) descrevem larguras diferentes, com emendas. A Regra 127 ainda diz que o pastor colhe
a lavoura; a regra temporária proíbe e a ajuda não está em `IdleHelps`. Sem efeito no jogo, mas leva a erro quem lê.

- **A:** reescrever em uma regra por assunto, sem mudar comportamento. **B:** fica como está.
