# Vanilla-Integration.md

# Village Colony — Vanilla Integration

**Status:** Accepted — 2026-08-06
**Version:** 1.0.0
**Depends on:** ADR-004-Mixin-Policy (Accepted)

---

# 1. Purpose

Este documento descreve **como** o Village Colony se conecta ao Minecraft
sem substituí-lo.

A ADR-004 define a política.

Este documento define os pontos de integração concretos.

---

# 2. Princípio

```text
O Minecraft continua no controle.

A colônia apenas pede a vez.
```

Quando a colônia não tem nada a pedir, o aldeão é indistinguível de um
aldeão Vanilla.

---

# 3. Pontos de Integração

---

## 3.1 Server Tick

Fonte:

```text
Fabric API — ServerTickEvents
```

Uso:

Executar o Simulation Loop em ciclos.

Frequência conforme `Performance-Rules.md §4`.

Mixin necessário:

```text
Nenhum
```

---

## 3.2 World Load / Save

Fonte:

```text
Fabric API — ServerWorldEvents
ServerLifecycleEvents
```

Uso:

Carregar e salvar `ColonySavedData`.

Mixin necessário:

```text
Nenhum
```

---

## 3.3 Morte de aldeão

Fonte preferencial:

```text
Fabric API — ServerLivingEntityEvents.AFTER_DEATH
```

Uso:

Remover worker, liberar storage, registrar profissão faltante.

Mixin necessário:

```text
Nenhum, se o evento for suficiente
```

Conforme ADR-004 §3, Mixin 2.

---

## 3.4 Detecção de vila

Fonte:

```text
ServerWorld → PointOfInterestStorage
```

Uso:

Clusterização de POIs de cama, conforme ADR-003.

Mixin necessário:

```text
Nenhum
```

---

## 3.5 Leitura de baú

Fonte:

```text
World.getBlockEntity(pos)

→ Inventory
```

Restrição:

Somente com chunk já carregado.

Conforme ADR-002.

Mixin necessário:

```text
Nenhum
```

---

## 3.6 Receitas

Fonte:

```text
server.getRecipeManager()
```

Uso:

Toda fabricação, conforme `PROJECT_CONSTITUTION.md §8`.

Nota de versão:

Em 1.21.x a consulta de receita usa `RecipeInput`
(ex.: `CraftingRecipeInput`), não mais `Inventory`.

Código de tutoriais anteriores a 1.21 não compila.

Mixin necessário:

```text
Nenhum
```

---

## 3.7 Estruturas

Fonte:

```text
server.getStructureTemplateManager()
```

Identificadores MVP:

```text
minecraft:village/plains/houses/plains_small_house_1
...
minecraft:village/plains/houses/plains_small_house_8
```

Mixin necessário:

```text
Nenhum
```

Ver §5.

---

## 3.8 Comportamento do aldeão

Fonte:

```text
VillagerEntity.getBrain()
```

Mixin necessário:

```text
SIM
```

Este é o **único** ponto que exige mixin.

Conforme ADR-004 §3, Mixin 1.

---

# 4. Resumo da superfície de mixin

```text
Pontos de integração:  8

Que exigem mixin:      1
```

Sete dos oito pontos usam API pública.

Isso é o que "Vanilla First" significa na prática.

---

# 5. Processamento de template

O `Construction-System.md` subestima esta etapa.

Um template Vanilla **não** é uma lista pronta de blocos.

---

## 5.1 Blocos a descartar

```text
minecraft:jigsaw

minecraft:structure_void

minecraft:structure_block
```

Jigsaw blocks definem conexões de worldgen.

Colocá-los no mundo é um bug visível.

---

## 5.2 Blocos com dados

Um bloco de baú no template pode carregar:

```text
metadata: loot table
```

Regra:

Colocar o baú **vazio**.

Gerar loot violaria `PROJECT_CONSTITUTION.md §9` — Resource Conservation.

---

## 5.3 Fundação

Templates de casa não incluem terreno.

Vilas Vanilla usam processadores de terreno durante o worldgen que não
estão disponíveis na colocação manual.

Consequência:

Terreno irregular produz casa flutuando ou enterrada.

Necessário:

Etapa `PREPARING` avalia e nivela o terreno natural, dentro dos limites
de `MVP.md §9`.

---

## 5.4 BlockState para Item

A conversão **não é 1:1**.

Casos que exigem tratamento:

```text
Porta        → 2 BlockStates, 1 item

Cama         → 2 BlockStates, 1 item

Slab duplo   → 1 BlockState, 2 items

Água / ar    → 0 items
```

Contagem ingênua produz lista de materiais errada.

---

# 6. Riscos de compatibilidade

---

## Conversão zombie villager

```text
Villager

↓ (zumbificado)

ZombieVillager

↓ (curado)

Villager NOVO com UUID NOVO
```

O worker fica órfão silenciosamente.

Tratamento:

Ao acordar a colônia, worker cujo UUID não resolve é removido e a
profissão é marcada como faltante.

Conforme `Save-Data-System.md`.

---

## Ferramenta visível

Villagers não renderizam item segurado como mobs armados.

O "Machado de Madeira" de `Profession-System.md` será invisível.

---

Opções:

```text
A) Aceitar invisível — ferramenta é regra, não visual

B) Mixin de render — client-side
```

Recomendado:

Opção A.

A opção B conflita com `Initial-Setup-Checklist.md §12`
(não depender de código client-side).

---

## Outros mods de aldeão

`VillagerEntity.initBrain` é o ponto de conflito mais comum.

Mitigação conforme ADR-004 §7.

---

# 7. Regra final

Antes de escrever um mixin, responder:

> Existe API pública para isso?

Se existir, o mixin não deve ser escrito.
