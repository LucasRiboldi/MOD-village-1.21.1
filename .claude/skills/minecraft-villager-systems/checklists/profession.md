# Checklist — profissão

## A pergunta que vem antes

```text
[ ] respondi: isto é IDENTIDADE nova, ou CAPACIDADE nova?
[ ] se capacidade → task + memória, e este checklist não se aplica
```

> Criar profissão quando bastava comportamento é o erro mais caro do domínio, e
> só aparece depois de as sete peças estarem escritas.

## As sete peças

```text
[ ] 1. BLOCO de trabalho
[ ] 2. POINT_OF_INTEREST_TYPE
[ ] 3. VILLAGER_PROFESSION
[ ] 4. TASK no Brain (o comportamento NÃO vem da profissão)
[ ] 5. SCHEDULE (a Vanilla normalmente basta)
[ ] 6. TRADES (sistema separado) — ou a ausência declarada
[ ] 7. RESOURCES
```

<Peça que não se aplica: declare qual e por quê.>

## A forma do record

```bash
javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession | head -20
```

`[FATO]` MC 1.21.1:

```java
VillagerProfession(String id, Predicate<...> heldWorkstation,
                   Predicate<...> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

```text
[ ] confirmei a forma NA MINHA VERSÃO
[ ] sei que NÃO há trades, tasks nem schedule aqui
```

## Registro

```text
[ ] POI registrado ANTES da profissão
[ ] Registries.VILLAGER_PROFESSION
[ ] no entrypoint
[ ] INCONDICIONAL        ← condicional derruba a conexão no handshake
[ ] DETERMINÍSTICO
[ ] namespace próprio
```

## Predicados

```text
[ ] heldWorkstation aponta para o POI certo
[ ] acquirableWorkstation aponta para o POI certo
[ ] sei por que são dois (manter ≠ adquirir)
```

## Comportamento

```text
[ ] a task existe, e verifica a profissão do aldeão
[ ] instalada por setTaskList, sem remover nada
[ ] perde para PANIC e RAID
```

## Comércio

```text
[ ] comercia → trade-plan.md preenchido
[ ] NÃO comercia → declarado, e portanto sem níveis nem XP
```

<Um aldeão de trabalho puro não precisa de trades. O Vanilla não exige.>

## Resources

```text
[ ] lang: "entity.minecraft.villager.<id>"
[ ] som de trabalho registrado
[ ] textura de roupa (ou aceito o visual padrão, declaradamente)
[ ] o bloco tem lang, modelo, textura, loot table, tags e item group
```

## Ciclo de vida

```text
[ ] aldeão sem profissão adquire ao ver o POI livre
[ ] quebrar o bloco libera o POI
[ ] aldeão morto libera o POI          (AFTER_DEATH)
[ ] aldeão convertido libera o POI     (MOB_CONVERSION)
[ ] ticketCount respeitado — um por bloco
```

## Persistência

```text
[ ] a profissão sobrevive a fechar e reabrir o mundo
[ ] o estado extra do meu mod (se houver) também
```

## Teste

```text
[ ] aldeão sem profissão adquire ao ver o bloco
[ ] só UM aldeão por bloco
[ ] quebrar o bloco libera
[ ] ele vai até o local no horário de trabalho
[ ] o comportamento acontece
[ ] visual e nome corretos
[ ] trades funcionam (ou a ausência é intencional)
[ ] ele dorme, come e socializa normalmente
[ ] fechar e reabrir mantém a profissão
[ ] servidor dedicado: tudo acima continua
[ ] as profissões Vanilla continuam funcionando
```

> O penúltimo pega registro condicional; o último é regressão que quase ninguém
> faz.

## No relato

**Por que isto precisava ser uma profissão?**
