# Profissões

Uma profissão **não é** um enum, e **não é** onde o comportamento mora.

## O que ela realmente é

`[FATO]` MC 1.21.1 — `net.minecraft.village.VillagerProfession` é um `record`:

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

Constantes Vanilla: `NONE`, `ARMORER`, `BUTCHER`, `CARTOGRAPHER`, `CLERIC`,
`FARMER`, `FISHERMAN`, `FLETCHER`, `LEATHERWORKER`, e as demais.

### O que a profissão contém

```text
id                      identidade
heldWorkstation         qual POI ele MANTÉM
acquirableWorkstation   qual POI ele pode ADQUIRIR
gatherableItems         quais itens ele recolhe do chão
secondaryJobSites       blocos auxiliares (ex.: plantação para o fazendeiro)
workSound               o som ao trabalhar
```

### O que ela NÃO contém

```text
✗ tasks / comportamento
✗ trades
✗ schedule
✗ níveis
✗ XP
```

> **Esta é a confusão mais cara deste domínio.** "Quero mudar o que o fazendeiro
> vende" **não** é mexer na profissão. "Quero que ele faça outra coisa" **não** é
> mexer na profissão.

| Você quer | Onde mexer |
|---|---|
| ele reconhece outro bloco de trabalho | `heldWorkstation` / `acquirableWorkstation` + POI |
| ele recolhe outro item do chão | `gatherableItems` |
| ele faz outra coisa | **task** no Brain |
| ele vende outra coisa | **trades** — `references/trading.md` |
| ele trabalha em outro horário | **schedule** |
| ele é um novo tipo de trabalhador | profissão nova + POI + task |

## A cadeia completa de uma profissão nova

```text
BLOCO                            o local de trabalho
  ↓
POINT_OF_INTEREST_TYPE           com TODOS os block states
  ↓
VILLAGER_PROFESSION              os predicados apontam para o POI
  ↓
TASK no Brain                    o comportamento (não vem da profissão)
  ↓
SCHEDULE                         quando (normalmente a Vanilla basta)
  ↓
TRADES                           se ele comercia (sistema separado)
  ↓
RESOURCES                        lang, som, textura de roupa
```

**Sete peças.** É por isso que "isto precisa mesmo ser uma profissão?" é a
pergunta a fazer antes — ver `examples/guard-villager-decision.md`.

## Registrar

```java
public static final RegistryEntry<PointOfInterestType> POI_FORJA = /* registrar primeiro */;

public static final VillagerProfession FERREIRO = Registry.register(
        Registries.VILLAGER_PROFESSION,
        Identifier.of(MOD_ID, "ferreiro"),
        new VillagerProfession(
                "ferreiro",
                entry -> entry.matchesKey(POI_FORJA_KEY),      // heldWorkstation
                entry -> entry.matchesKey(POI_FORJA_KEY),      // acquirableWorkstation
                ImmutableSet.of(),                              // gatherableItems
                ImmutableSet.of(),                              // secondaryJobSites
                SoundEvents.ENTITY_VILLAGER_WORK_ARMORER));
```

> **Confirme as assinaturas na sua versão.** A forma deste record mudou entre
> releases — `javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession`.

**Ordem:** POI antes da profissão. E registro no entrypoint, **incondicional e
determinístico** — profissão registrada condicionalmente produz ids diferentes
entre cliente e servidor e derruba a conexão.

## Níveis e experiência

Separados da profissão:

```text
Level 1  Novice → 2 Apprentice → 3 Journeyman → 4 Expert → 5 Master
```

O nível libera trades e é ganho com XP de comércio. **Se o seu aldeão não
comercia, nível e XP não fazem nada** — e você não precisa deles.

Isso importa para o desenho: uma profissão de trabalho puro (mineiro, lenhador
que só produz para a colônia) pode existir sem trades, sem níveis e sem XP. O
Vanilla não exige.

## Atribuição e perda

```text
aldeão sem profissão + POI livre ao alcance → adquire
aldeão com profissão + POI destruído        → perde (se ainda for nível 1)
aldeão nível 2+                              → mantém a profissão
```

`[INFERÊNCIA]` A regra de "nível 2+ mantém" existe para o jogador não perder um
comerciante bom por acidente. Se o seu mod atribui profissão por conta própria,
pense se quer a mesma proteção — e **verifique a regra exata na sua versão**
antes de depender dela.

## Aparência

A textura da roupa vem do **`VillagerType`** (bioma) combinado com a profissão.
Uma profissão nova sem textura aparece com o visual padrão — funciona, mas o
jogador não distingue.

```text
[ ] textura por VillagerType, ou uma só
[ ] lang: "entity.minecraft.villager.<id>"
[ ] som de trabalho
```

## Quando a profissão é do seu mod, não do Vanilla

Alguns mods atribuem "função" por lógica própria, sem usar
`VillagerProfession`. É legítimo, e tem custo:

| | `VillagerProfession` | Função própria do mod |
|---|---|---|
| Persistência | Vanilla salva | **você salva** |
| POI / reivindicação | Vanilla gerencia | você gerencia |
| Aparência | integrada | precisa de outro caminho |
| Compatibilidade | mods de aldeão reconhecem | invisível para eles |
| Flexibilidade | limitada ao modelo Vanilla | total |

Use `VillagerProfession` quando o conceito é "profissão de aldeão" de verdade.
Use função própria quando é um papel dentro de um sistema do seu mod — e então
**declare que precisa persistir**, porque o Vanilla não vai fazer isso por você.

## Checklist

Ver `checklists/profession.md` e `templates/profession-plan.md`.

```text
[ ] a profissão é MESMO necessária (não é só comportamento)
[ ] POI registrado antes
[ ] os dois predicados apontam para o POI certo
[ ] registro incondicional e determinístico
[ ] a task do comportamento existe (não vem da profissão)
[ ] trades definidos, se comercia — sistema SEPARADO
[ ] resources: lang, som, textura
[ ] testado: aldeão sem profissão adquire ao ver o bloco
[ ] testado: quebrar o bloco libera o POI
```
