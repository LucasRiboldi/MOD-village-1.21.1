# Bancadas exclusivas do mod — design (2026-10-13)

## Objetivo

Blocos exclusivos do Village Colony que o **player** fabrica para dobrar a produção
de uma profissão. Três bancadas, réplicas funcionais das estações Vanilla, com
**emissão de luz**. Quando o aldeão titular trabalha vinculado à bancada do mod da
sua profissão, ele produz **o dobro por ciclo de trabalho** (1 unidade → 2 no baú),
no mesmo ritmo. A **colônia nunca** fabrica nem coloca essas bancadas — só o player,
por receita de crafting sempre disponível.

Decisão do autor, 2026-10-13. É a primeira infra de conteúdo registrado do mod.

## Escopo (abordagem A, aprovada)

- Três bancadas, as profissões que trabalham diante de uma estação + ferreiro:

  | Bloco do mod (`villagecolony:…`) | Espelha (Vanilla) | Profissão | Produção dobrada |
  |---|---|---|---|
  | `carpenter_bench` | `crafting_table` | CARPENTER | `CRAFTED_WOOD` |
  | `mason_cutter` | `stonecutter` | MASON | `CRAFTED_STONE` |
  | `blacksmith_forge` | `blast_furnace` | SMELTER | `SMELTED` |

- Luz: `luminance = 10` em todos os estados.
- Textura: **cor distinta** da Vanilla (marca "é a do mod").
- Vínculo: **POI próprio do mod** (um `PointOfInterestType` por bancada), **sem**
  `VillagerProfession` associada — o Vanilla não emprega nele e a detecção de vila
  (por POI de cama/sino) não é afetada. O fluxo do mod reivindica/ocupa (um titular
  por bancada, como o cortador de pedra hoje).
- Dobro: a **quantidade por ciclo** (×2 no depósito ao baú), não a velocidade.
- Receita: `crafting_shaped` na mesa de trabalho, sempre disponível. Estação Vanilla
  correspondente + 4× `gold_ingot` nos cantos.
- Fora de escopo (YAGNI): GUI/inventário no bloco; bancadas para profissões de campo
  (mineiro/lenhador/fazendeiro/pastor); fator configurável; datapack.

## Arquitetura

### Registro de conteúdo (novo)

- `ModBlocks` (`com.villagecolony.fabric.content`): registra os 3 blocos + `BlockItem`
  de cada em `Registries.BLOCK`/`Registries.ITEM`, namespace `villagecolony`. Cada bloco
  estende o bloco Vanilla (`CraftingTableBlock`, `StonecutterBlock`, `AbstractFurnaceBlock`/
  `BlastFurnaceBlock`) com `AbstractBlock.Settings.copy(vanilla).luminance(s -> 10)`.
  - Se herdar `BlastFurnaceBlock` exigir BlockEntity próprio, usar `Blocks.copy` de um
    bloco simples com a forma da fornalha em vez de herdar o comportamento de fundição
    (a fundição é do mod, não da fornalha física). Decidir na implementação; preferir a
    forma mais simples que registre sem BlockEntity.
- `ModPoiTypes` (`…fabric.content`): um `PointOfInterestType` por bancada, registrado com
  os `BlockState` do bloco correspondente, **ticket count 1, search distance 1**, e **sem**
  vínculo a `VillagerProfession`. Exposto por um mapa `ProfessionType → RegistryKey<PoiType>`.
- Chamadas em `VillageColonyMod.onInitialize` (cedo): `ModBlocks.register()`,
  `ModPoiTypes.register()`.
- Tag `#villagecolony:profession_bench` (data) marca os 3 blocos; o código reconhece a
  bancada por tag, não por lista de nomes (regra do projeto).

### Vínculo bancada ↔ titular

- `CraftStation.bench(world, profession, chest)` passa a procurar **primeiro** a bancada
  do mod da profissão, pelo POI do mod (raio `POI_RADIUS = 64`, `OccupationStatus.ANY`).
  Achou → é o spot. Não achou → comportamento atual (crafting_table/stonecutter/sino/baú).
- `CraftStation` ganha o caso `SMELTER` (hoje não tem spot): procura a `blacksmith_forge`
  do mod. Sem ela, o fundidor segue como hoje (sem bancada, sem dobro).
- "Um titular por bancada": decorre do `spotFor` — cada titular resolve seu spot; com uma
  só bancada, o mais próximo a usa. Sem estado de reivindicação novo além do POI.

### O dobro (um ponto só)

- `ModBenchBonus.doubles(world, workerId, profession)` → `true` se o spot atual do titular
  (via `CraftStation.spotFor`) é uma bancada do mod cuja profissão == a do titular.
  Implementado lendo o `BlockState` do spot e conferindo a tag + o mapa profissão→bloco.
- Aplicado no depósito do produto ao baú:
  - Carpinteiro/Pedreiro: em `CraftingSteps`, no ponto em que o produto entra no baú.
  - Fundidor: em `SmelterWork.convert`, no `made.getCount()` depositado.
  - Em ambos: `count = doubles(...) ? base * 2 : base`.
- É o único lugar onde o número muda — isolado, testável, provável por mutação.

## Fluxo de dados

1. Player crafta a bancada (receita) e a coloca perto do baú/vila da profissão.
2. `CraftStation.spotFor` do titular encontra o POI do mod → fixa o spot na bancada.
3. Ao concluir uma unidade de produção, `ModBenchBonus.doubles(...)` é true → o depósito
   ao baú dobra.
4. Sem bancada do mod: tudo como hoje (spot antigo, produção 1×).

## Tratamento de erro / bordas

- Bancada removida pelo player: `spotFor` revalida a cada `REFRESH_TICKS` (6000) e volta ao
  comportamento antigo; o dobro para de valer no próximo ciclo. Sem estado preso.
- Duas bancadas da mesma profissão: o `spotFor` escolhe a mais próxima; as duas dão dobro a
  quem as usa (uma por titular). Sem erro.
- Profissão sem bancada do mod (mineiro etc.): `doubles` sempre false; nada muda.
- A colônia não enxerga as bancadas como produzíveis (não entram em `Production`/
  `CraftingLookup`/`CraftFamily`), então nenhum aldeão tenta fabricá-las.

## Testes

- **Unidade** (`ModBenchBonusTest`, sem mundo onde possível, senão GameTest):
  vínculo correto → 2; sem vínculo / profissão errada → 1.
- **GameTest** (`ProfessionBenchGameTest`):
  1. Carpinteiro com `carpenter_bench` no raio → spot é a bancada e o baú recebe 2 em vez de 1.
  2. Sem a bancada → spot antigo e produção 1×.
  3. Fundidor com `blacksmith_forge` → funde o dobro; sem ela → 1×.
  4. O bloco emite luz 10 (`getLuminance`/`luminance` do estado).
- **Mutação**: remover o `* 2` (ou o reconhecimento da tag) derruba os testes de produção.
- **Arquitetura/metadados**: cada bloco novo tem modelo, lang (pt_br + en_us), tag e receita;
  conferir se `ModMetadataTest` exige algo e satisfazer.

## Arquivos

**Novos (código):** `fabric/content/ModBlocks.java`, `fabric/content/ModPoiTypes.java`,
`fabric/content/ModBenchBonus.java` (ou `work/ModBenchBonus.java`), testes.
**Novos (resources):** `assets/villagecolony/blockstates/{carpenter_bench,mason_cutter,blacksmith_forge}.json`,
`models/block/*`, `models/item/*`, `textures/block/*` (cor distinta), `lang/pt_br.json` + `en_us.json`
(entradas novas), `data/villagecolony/recipe/*.json`, `data/villagecolony/tags/block/profession_bench.json`.
**Editados:** `VillageColonyMod.onInitialize` (registros), `fabric/work/CraftStation.java`
(preferir bancada do mod + caso SMELTER), `fabric/work/CraftingSteps.java` e
`fabric/work/SmelterWork.java` (aplicar o dobro no depósito).

Nenhum arquivo acima de 500 linhas.

## Versão

Sai como **0.3.52** (a 0.3.51 é a correção do deadlock da mina). Commit por marco; commit
+ push + JAR atualizado ao final, conforme pedido do autor.
