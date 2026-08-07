# ADR-003-Village-Detection.md

# Architecture Decision Record 003

# Village Colony Village Detection

**Status:** Accepted
**Date:** 2026-08-06
**Accepted:** 2026-08-06
**Decision Type:** Architecture / Integration
**Blocks:** TASK-009, TASK-010, Phase 4, v0.2

---

# 1. Context

A documentação trata `VillageScanner` como uma tarefa simples.

Tecnicamente não é.

---

## O problema

**O Minecraft não possui um objeto `Village`.**

Não existe classe, registro ou API que responda "onde está a vila".

Vila é um fenômeno **emergente** de três coisas independentes:

```text
POIs

+

Villager Brain memories

+

Proximidade
```

---

## POIs disponíveis em 1.21.1

```text
minecraft:home            (cama)

minecraft:meeting         (sino)

minecraft:armorer
minecraft:butcher
minecraft:farmer
...                       (workstations)
```

Acessíveis via:

```text
ServerWorld

↓

PointOfInterestStorage
```

---

## Por que locateStructure não resolve

`ServerWorld.locateStructure(...)` encontra a **estrutura gerada no worldgen**.

Isso não é a vila viva.

Diferenças:

* uma vila cresce além da estrutura original;
* uma vila pode ser destruída e a estrutura continua registrada;
* um jogador pode criar uma vila do zero, sem estrutura alguma;
* a estrutura não sabe quantos aldeões existem.

Usar `locateStructure` como detecção produziria colônias fantasma.

---

# 2. Decision

Detectar vilas por **clusterização de POIs de cama ocupados**.

A cama é o marcador correto porque:

* define população real;
* é o que o Vanilla usa para breeding e raid;
* existe em vila gerada e em vila construída pelo jogador;
* `MVP.md` já define a casa como base do storage do trabalhador.

---

# 3. Algoritmo

---

## Gatilho

Não varrer o mundo.

Executar apenas quando:

```text
Chunk carregado contendo POI de cama

ou

Ciclo longo (600 ticks) em chunk já carregado
```

Conforme `Performance-Rules.md §4` e `§6`.

---

## Passo 1 — Coleta local

```text
PointOfInterestStorage

↓

Buscar POI tipo HOME

↓

Raio limitado a 64 blocos do ponto de gatilho
```

Nunca raio infinito.

---

## Passo 2 — Cluster

Agrupar camas por proximidade.

Regra:

```text
Duas camas pertencem ao mesmo cluster

se a distância entre elas for <= 32 blocos
```

Aplicar transitivamente.

---

## Passo 3 — Validação

Um cluster vira colônia somente se:

```text
Camas >= 3

E

Aldeões vivos no raio >= 2
```

Evita:

* uma cama solta no meio do mundo;
* acampamento temporário do jogador;
* vila abandonada sem população.

---

## Passo 4 — Centro

```text
centerPosition = média das posições das camas do cluster
```

Se existir um sino (`meeting`) no cluster:

O sino tem prioridade como centro.

Motivo: é o centro social real da vila Vanilla.

---

## Passo 5 — Bioma

```text
biomeType = bioma no centerPosition
```

MVP aceita apenas:

```text
PLAINS
```

Cluster em outro bioma é ignorado, não é erro.

---

## Passo 6 — Identidade

Antes de criar, verificar sobreposição.

```text
Existe colônia cujo centro esteja a <= 64 blocos?

Sim → atualizar a existente

Não → criar nova Colony
```

Impede colônias duplicadas ao reentrar na área.

---

# 4. Estabilidade da identidade

O centro da vila **se move** conforme camas são adicionadas ou removidas.

Regra:

O `colonyId` (UUID) **nunca muda**.

Apenas `centerPosition` é atualizado.

Workers e Buildings permanecem ligados ao UUID.

---

# 5. Fusão e divisão

---

## Fusão

Duas colônias cujos centros se aproximem a menos de 32 blocos:

MVP:

Não fundir.

Registrar aviso:

```text
[COLONY] Overlapping colonies detected
```

Fusão exige nova ADR.

---

## Divisão

Uma vila que se parta em dois clusters distantes:

MVP:

Manter uma única colônia.

Não dividir.

---

# 6. Perda de vila

Se um cluster deixar de atender a validação do Passo 3:

```text
Camas < 3

ou

Aldeões vivos = 0
```

A colônia **não é apagada**.

Ela recebe:

```text
ColonyState: DORMANT
```

Motivo:

Apagar destruiria o registro de Buildings construídos, violando
`PROJECT_CONSTITUTION.md §10` — Permanent Infrastructure.

---

# 7. Consequences

---

## Data-Model.md

`ColonyState` ganha um valor:

```text
STABLE

PRODUCTION

EXPANSION

DORMANT
```

Alinhado com ADR-002.

---

## Class-Architecture.md

Resolver a ambiguidade de nomes:

```text
VillageScanner   → detecta vilas (cluster de POI)

VillagerScanner  → detecta aldeões dentro de uma colônia
```

São dois componentes distintos.

---

## Performance-Rules.md

Registrar os limites fixados aqui:

```text
Raio de busca:        64 blocos

Distância de cluster: 32 blocos

Ciclo:                600 ticks
```

---

# 8. Values Summary

```text
Raio de coleta            64
Distância de cluster      32
Camas mínimas              3
Aldeões mínimos            2
Distância anti-duplicata  64
Ciclo                    600 ticks
```

Todos devem ser constantes nomeadas, nunca literais no código.

Conforme `CODE-STANDARDS.md §3`.

---

# 9. Final Statement

A vila não é uma estrutura.

A vila é onde os aldeões dormem.
