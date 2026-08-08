# ADR-003-Village-Detection.md

# Architecture Decision Record 003

# Village Colony Village Detection

**Status:** Accepted (emendada — ver §10)
**Date:** 2026-08-06
**Accepted:** 2026-08-06
**Amended:** 2026-08-06 — três emendas vindas da implementação
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

---

# 10. Emendas

Duas correções ao texto original, ambas descobertas durante a
implementação das TASK-009 e TASK-010.

O corpo da ADR acima permanece como foi aceito; o que vale é esta
seção onde houver conflito.

---

## Emenda 1 — `DORMANT` vira `ABANDONED` em ColonyState

**Data:** 2026-08-06

**Afeta:** §6 e §7

---

O texto original manda `ColonyState` ganhar o valor `DORMANT` e afirma
estar "alinhado com ADR-002".

Não está. As duas condições são distintas:

```text
ADR-002   DORMANT = chunk descarregado

ADR-003   DORMANT = vila sem população
```

Uma vila abandonada com o jogador parado ao lado atende à segunda e
não à primeira. Os dois estados são simultâneos e independentes.

---

Decisão:

```text
ColonyLifecycle   ACTIVE | DORMANT      (ADR-002)

ColonyState       ... | ABANDONED       (esta ADR, §6)
```

Motivo: dois `DORMANT` com significados diferentes no mesmo objeto são
uma armadilha para quem lê o código depois.

---

Estado da implementação:

```text
O valor ABANDONED existe.

Nada o atribui ainda.
```

A regra do §6 exige distinguir "vila deixou de ser viável" de "vila não
foi observada". Hoje `VillageScanner.scan` devolve apenas clusters
aprovados: um cluster reprovado simplesmente não aparece, e as duas
situações ficam indistinguíveis.

Implementar exige o scanner reportar também os clusters avaliados e
reprovados perto de colônias conhecidas. Fica para tarefa própria.

---

## Emenda 2 — completude da observação decide o centro

**Data:** 2026-08-06

**Afeta:** §3, §4 e §6

---

O texto original tem uma contradição interna:

```text
§4   centro = média das camas do cluster

§6   colônia existente é atualizada

§3   coleta limitada a 64 blocos
```

Uma vila é maior que 64 blocos. Logo **nenhuma detecção enxerga a vila
inteira**, e cada gatilho produz um cluster diferente.

Atualizar sempre significa deixar a última detecção vencer.

---

Observado em jogo em 2026-08-06:

```text
1109,730 → 1080,733    3 camas

1080,733 → 1109,730   12 camas

1109,730 → 1080,733    3 camas
```

O centro oscilava entre uma visão de 12 camas e outra de 3.

Pior: perseguindo visões parciais, a colônia se afastou mais de 64
blocos da vila real. A detecção seguinte não achou colônia por perto e
criou outra — a vila trocou de UUID, violando o §4 desta própria ADR.

---

Decisão:

```text
Colony.observedBeds

  camas da melhor observação já vista
```

```text
O centro só se move quando a nova observação tem

observedBeds >= o valor registrado.
```

Empate move, porque a vila pode se deslocar mantendo o número de camas.

O campo é persistido. Save anterior à emenda lê 0 e autocorrige na
primeira detecção da sessão.

---

Emenda de 2026-08-07 — a colônia pode encolher:

```text
uma observação COMPLETA escapa da regra
e pode baixar observedBeds
```

Sem ela `observedBeds` só crescia, e vila que perdesse camas ficava com
o centro congelado para sempre.

Duas coisas dão essa autoridade.

**A sonda** — o caminho que funciona na prática:

```text
o ciclo longo varre também a partir do centro
de cada colônia ativa: é a sonda

duas leituras seguidas da sonda, mesma âncora,
a segunda não maior que a primeira
  → a vila encolheu
```

A sonda parte do mesmo ponto a cada ciclo, então suas leituras são
comparáveis entre si — a posição do jogador muda a cada passo e nunca se
repete. Uma leitura que se confirma na seguinte não é acidente de
posição.

Exige repetição: a leitura anterior já precisa estar abaixo da contagem
registrada. Sem isso, a sonda que viu 38 e depois 33 confirmaria o 33
contra si mesma, e uma visão parcial isolada encolheria a colônia.

`probeAnchor` e `probeBeds` são gravados a cada leitura da sonda, aceita
ou recusada. Ligá-los à observação aceita foi um defeito: a colônia vem
do save com âncora nula, nenhuma observação é aceita enquanto ela
estiver grande demais, e a âncora nunca nascia.

Só a sonda leva âncora. A varredura do jogador e a do chunk vêm sem: um
jogador parado na borda repetiria a mesma visão pobre ciclo após ciclo,
e ela se confirmaria — a deriva entrando pela porta aberta para o
encolhimento.

**Prova geométrica** — rara, mas serve na primeira observação, quando
ainda não há âncora com que comparar:

```text
toda cama vista a até SEARCH_RADIUS - CLUSTER_DISTANCE
do gatilho — 64 - 32 = 32 blocos
```

A prova vem da definição de cluster: cama ligada a outra está a no
máximo `CLUSTER_DISTANCE` dela, então uma vizinha ainda cairia dentro do
raio de busca e teria sido coletada.

Ela sozinha não bastou. Em jogo, em 2026-08-07, uma vila de 38 camas
teve cinco camas destruídas e recusou cinco observações seguidas de 32 e
33 camas: a vila é maior que a margem de 32 blocos, e nenhuma observação
real ali jamais se prova completa. Ver §15.

Mede na horizontal, como a clusterização. Cama muito acima ou abaixo
entra no cluster e poderia cair fora da esfera: o erro possível é deixar
de encolher, nunca encolher errado.

Fora dos dois casos a resposta é "não sei", e a regra original continua
valendo — é o que impede a oscilação de voltar.

---

Verificado em jogo após a correção:

```text
12 → 13 → 15 → 21 camas
```

Contagem monotonicamente crescente. O centro converge.

---

## Emenda 3 — gatilho ancorado na cama

**Data:** 2026-08-06

**Afeta:** §3

---

O §3 define o gatilho como "chunk carregado contendo POI de cama", sem
dizer de que ponto parte a coleta de raio 64.

Ancorar no chunk não funciona:

```text
ChunkPos.getStartPos()  →  BlockPos(startX, 0, startZ)

getInCircle             →  distância em três dimensões
```

Partindo de y=0, uma cama em y=64 consome todo o raio antes de qualquer
deslocamento horizontal. A busca voltava sempre vazia.

---

Decisão:

```text
O gatilho é a posição do POI de cama encontrado no chunk.
```

Está na altura certa por definição.
