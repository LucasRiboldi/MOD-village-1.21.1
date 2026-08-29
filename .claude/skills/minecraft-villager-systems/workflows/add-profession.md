# Workflow — nova profissão

Uma profissão traz sete peças junto. Por isso a primeira etapa é decidir se você
precisa mesmo de uma.

---

## 1. A pergunta que economiza tudo

> **Isto é identidade nova, ou capacidade nova?**

```text
IDENTIDADE   ele É um tipo diferente de trabalhador, com local próprio,
             visual próprio, e (talvez) comércio próprio       → profissão
CAPACIDADE   ele FAZ algo novo, em certas condições            → task + memória
```

| Pedido | Precisa de profissão? |
|---|---|
| "um aldeão minerador com bancada própria" | **sim** |
| "aldeões devem fugir de creepers" | **não** — sensor + memória + task |
| "o fazendeiro deve guardar no baú" | **não** — task |
| "um guarda que ataca zumbis" | **provavelmente não** |

Ver `examples/guard-villager-decision.md`. Criar profissão quando bastava
comportamento é o erro mais caro deste domínio — e só aparece depois de todo o
aparato estar escrito.

## 2. As sete peças

```text
1. BLOCO                  o local de trabalho
2. POINT_OF_INTEREST_TYPE com TODOS os block states
3. VILLAGER_PROFESSION    os predicados apontam para o POI
4. TASK no Brain          o comportamento (NÃO vem da profissão)
5. SCHEDULE               quando (a Vanilla normalmente basta)
6. TRADES                 se comercia — sistema SEPARADO
7. RESOURCES              lang, som, textura de roupa
```

Se alguma não faz sentido para o seu caso, ótimo — mas declare qual e por quê.

## 3. Verificar a versão

`[FATO]` MC 1.21.1 — `VillagerProfession` é um `record`:

```bash
javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession | head -20
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
```

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

> **Não há trades, tasks nem schedule no record.** Se você esperava mexer neles
> aqui, o desenho precisa mudar.

**A forma deste record mudou entre versões.** Código de tutorial mais antigo não
compila.

## 4. Plano

`templates/profession-plan.md`. Preencha antes de escrever.

## 5. Bloco e POI

`workflows/add-job-site.md` — faça isso primeiro. A profissão depende do POI.

```text
[ ] o POI inclui TODOS os block states do bloco
[ ] ticketCount coerente (1 = exclusivo)
[ ] searchDistance coerente
```

## 6. Registrar a profissão

```java
public static final VillagerProfession FERREIRO = Registry.register(
        Registries.VILLAGER_PROFESSION,
        Identifier.of(MOD_ID, "ferreiro"),
        new VillagerProfession(
                "ferreiro",
                entry -> entry.matchesKey(ModPois.FORJA_KEY),   // heldWorkstation
                entry -> entry.matchesKey(ModPois.FORJA_KEY),   // acquirableWorkstation
                ImmutableSet.of(),                               // gatherableItems
                ImmutableSet.of(),                               // secondaryJobSites
                ModSounds.TRABALHO_FERREIRO));
```

```text
[ ] POI registrado ANTES
[ ] no entrypoint, INCONDICIONAL e DETERMINÍSTICO
[ ] namespace próprio
[ ] os dois predicados apontam para o POI certo
```

Registro condicional produz ids diferentes entre cliente e servidor e **derruba a
conexão no handshake**.

```bash
./gradlew build
./gradlew runClient
```

**Teste já aqui:** um aldeão sem profissão deve adquirir a nova ao ver o bloco.
Se não adquire, o problema é o POI ou o predicado — e é muito mais barato
descobrir agora.

## 7. O comportamento

A profissão **não** traz comportamento. Ele vem de uma task:

`workflows/add-behavior.md`.

```text
[ ] a task verifica a profissão do aldeão
[ ] instalada por setTaskList, sem remover nada
[ ] perde para PANIC e RAID
```

## 8. Trades — se comercia

`references/trading.md`. **Sistema separado.**

E a decisão legítima:

```text
[ ] esta profissão NÃO comercia — declarado, e portanto sem níveis nem XP
```

Um aldeão de trabalho puro (produz para uma colônia) não precisa de trades,
níveis ou XP. O Vanilla não exige.

## 9. Resources

```text
[ ] lang: "entity.minecraft.villager.ferreiro"
[ ] som de trabalho registrado
[ ] textura de roupa (por VillagerType, ou uma só)
```

Sem textura ele funciona com o visual padrão — mas o jogador não distingue as
profissões, o que costuma ser o ponto.

## 10. Testar

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

```text
[ ] aldeão sem profissão adquire ao ver o bloco
[ ] só UM aldeão por bloco (ticketCount)
[ ] quebrar o bloco libera o POI
[ ] o aldeão vai até o local no horário de trabalho
[ ] o comportamento acontece
[ ] o visual e o nome estão certos
[ ] as trades funcionam (ou a ausência é intencional)
[ ] ele dorme, come e socializa normalmente
[ ] fechar e reabrir o mundo mantém a profissão
[ ] servidor dedicado: tudo acima continua
```

O penúltimo é o teste de persistência; o último é o que pega registro condicional.

## Fechamento

`checklists/profession.md` e `checklists/villager-feature.md`.

E, no relato, a pergunta do passo 1 respondida: **por que isto precisava ser uma
profissão?**
