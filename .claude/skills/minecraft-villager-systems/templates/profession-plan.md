# Plano de profissão — <nome>

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## A pergunta antes de tudo

> **Isto é identidade nova, ou capacidade nova?**

```text
[ ] identidade  → profissão (7 peças abaixo)
[ ] capacidade  → task + memória, e este documento não é necessário
```

**Justificativa:**

<Criar profissão quando bastava comportamento é o erro mais caro do domínio — e
só aparece depois de todo o aparato estar escrito. Ver
`examples/guard-villager-decision.md`.>

## Identidade

| | |
|---|---|
| Id | `meumod:<path>` |
| Nome exibido | |
| O que ele faz, em uma frase | |

## A forma do record

`[FATO]` MC 1.21.1 — `VillagerProfession` é um `record`:

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

> **Não há trades, tasks nem schedule aqui.** Confirme a forma na sua versão:
> `javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession`

## Local de trabalho (POI)

| | |
|---|---|
| Bloco | |
| POI type | |
| `ticketCount` | <1 = exclusivo> |
| `searchDistance` | |

<Detalhe em `templates/job-site-plan.md`. **Registrar o POI antes da profissão.**>

## Predicados

| | Predicado | Por quê |
|---|---|---|
| `heldWorkstation` | | o que ele mantém |
| `acquirableWorkstation` | | o que ele pode adquirir |

<Dois predicados, não um, porque manter e adquirir são regras diferentes.>

## `gatherableItems`

<O que ele recolhe do chão. — vazio, se não recolhe nada.>

## `secondaryJobSites`

<Blocos auxiliares (como a plantação para o fazendeiro). — vazio.>

## Comportamento

> **Não vem da profissão.** Uma ou mais tasks no Brain.

| Task | Documento |
|---|---|
| | `templates/behavior-plan.md` |

## Schedule

```text
[ ] uso a Schedule Vanilla (recomendado)
[ ] alterei — o que deslocou, e por quê:
```

## Trades

> **Sistema SEPARADO da profissão.** Ver `references/trading.md`.

```text
[ ] esta profissão comercia    → templates/trade-plan.md
[ ] NÃO comercia — declarado, e portanto sem níveis nem XP
```

<Um aldeão de trabalho puro não precisa de trades, níveis ou XP. O Vanilla não
exige.>

## Níveis e experiência

<Só se comercia. — não se aplica.>

## Som

| | |
|---|---|
| `workSound` | |
| Registrado em | |

## Resources

```text
[ ] lang: "entity.minecraft.villager.<id>"
[ ] textura de roupa (por VillagerType, ou uma só)
[ ] som registrado
[ ] o bloco de trabalho tem lang, modelo, textura e loot table
```

<Sem textura ele funciona com o visual padrão, e o jogador não distingue — o que
costuma ser o ponto da profissão.>

## Registro

```text
[ ] POI registrado ANTES
[ ] no entrypoint
[ ] INCONDICIONAL          ← condicional derruba a conexão no handshake
[ ] DETERMINÍSTICO
[ ] namespace próprio
```

## Casos de falha

| Situação | Comportamento |
|---|---|
| o bloco de trabalho é destruído | |
| o aldeão morre | liberar o POI |
| o aldeão é convertido | idem — `MOB_CONVERSION` |
| dois aldeões querem o mesmo bloco | `ticketCount` |
| o aldeão não alcança o local | |
| chunk descarregado | caso normal |

## Performance

| | |
|---|---|
| Quantos desta profissão numa vila | |
| Custo do comportamento | |
| Frequência | |

## Teste

```text
[ ] aldeão sem profissão adquire ao ver o bloco
[ ] só UM por bloco
[ ] quebrar o bloco libera o POI
[ ] ele vai até o local no horário de trabalho
[ ] o comportamento acontece
[ ] visual e nome corretos
[ ] trades funcionam (ou a ausência é intencional)
[ ] ele dorme, come e socializa normalmente
[ ] fechar e reabrir o mundo mantém a profissão
[ ] servidor dedicado
```
