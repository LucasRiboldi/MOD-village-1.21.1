---
name: indice-de-ruas-vira-rua-candidata
description: Reservar colunas no índice de ruas faz cada uma virar rua candidata com o próprio roadY — armadilha ao montar cenário de lote
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-15T23:38:22.893Z
---

Em cenário de `BuildSiteGameTest`, `reserveRoadFootprint` registra as
colunas no índice `ROADS` — e `findAmongRoads` depois percorre **cada uma
delas como rua candidata**, tirando o `roadY` do chão daquela coluna.

Consequência que quebra cenários: se você levantar as colunas reservadas
para criar um degrau, elas viram ruas no nível de cima, e o lote ao lado
fica no nível da *sua própria* rua — a régua da Regra 19 não reprova nada.
Em 2026-09-15 isso deu `off_road=28 road=62` num teste que esperava
`road=0`.

**Why:** o índice de ruas serve a dois papéis — "esta coluna é estrada
reservada" (`isRoadArea`) e "esta coluna é ponto de partida de busca"
(`findAmongRoads`). Montar cenário mexendo nele afeta os dois.

**How to apply:** para testar a régua do nível, reserve **só a coluna
central** (`reserveRoad`) e altere as colunas do lote **fora** do índice.
Se precisar que a coluna do lote seja simultaneamente estrada e outra
coisa, lembre que ela também virará ponto de partida. Relacionado a
[[log-decide-a-constante]] e a [[gametest-copia-do-predicado]].
