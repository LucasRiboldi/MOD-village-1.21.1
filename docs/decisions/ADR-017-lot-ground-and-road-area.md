# ADR-017 - Elegibilidade de lotes e area de estrada

**Estado:** Aceita pelo autor em 2026-09-15
**Escopo:** P0.7; selecao de lotes Fabric 1.21.1.

## Contexto

O scanner recusava pedra e outros pisos solidos por uma taxonomia de blocos.
Na amostra de 6.583 recusas, 4.578 foram `NOT_NATURAL_GROUND`. Ao mesmo
tempo, gravilha e terracota podem ser piso natural ou preparo do jogador, mas
tambem sao materiais de pavimentacao. O material isolado nao identifica uma
estrada.

## Decisao

1. Todo piso solido disponivel e candidato a lote. A origem geologica ou a
   autoria do preparo nao e inferida pelo tipo de bloco.
2. P0.7 nao escava, preenche, nivela ou terraplana. O scanner apenas le o
   mundo; vegetacao substituivel nao e tomada por piso.
3. Os materiais oficiais de estrada sao somente `minecraft:dirt_path`,
   `minecraft:gravel` e `minecraft:terracotta`. Uma coluna e estrada apenas
   quando tem um desses materiais e pertence a `ROAD_AREA` da colonia.
4. `ROAD_AREA` vem do indice persistido `ColonyRoads`, de uma reserva de obra
   em andamento ou de pavimentacao original protegida. Uma mudanca manual de
   material nao cria reserva espacial.
5. A elegibilidade do footprint segue esta prioridade: protecao, construcao
   existente, `ROAD_AREA`, piso solido, nivel da estrada e volume livre. Uma
   unica coluna recusada reprova todo o lote.
6. Esta decisao nao altera mineracao, recursos, profissoes ou geracao de
   terreno.

## Consequencias

- Gravilha e terracota fora de `ROAD_AREA` voltam a ser candidatas a lote.
- Estradas existentes e novas dependem do indice espacial; fixtures e saves
  devem restaurar ou persistir `ColonyRoads` para identifica-las.
- GameTests cobrem pedra, gravilha, terracota, estrada reservada, footprint,
  construcoes/protecao e ausencia de modificacao do mundo. A validacao visual
  em jogo continua necessaria.

## Verificacao

`runGametest --rerun-tasks` executou 327 GameTests sem falhas em 2026-09-15.
O JAR distribuido nao foi atualizado nesta entrega.
