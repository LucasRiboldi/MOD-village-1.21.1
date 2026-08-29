# Exemplo — profissão completa

**Pedido:**

> "Quero um aldeão ferreiro que trabalhe na forja, produza lingotes e venda
> ferramentas."

Aqui a resposta à pergunta de escopo é **sim, é profissão** — e o exemplo mostra
por quê, e as sete peças em ordem.

---

## 1. Identidade ou capacidade?

| Peça | Faz sentido? |
|---|---|
| bloco de trabalho | **sim** — a forja |
| POI e reivindicação | **sim** — uma forja, um ferreiro |
| Schedule | **sim** — ele funde no horário de trabalho |
| trades | **sim** — vende ferramentas |
| visual próprio | **sim** |
| só alguns aldeões | **sim** |

Seis respostas boas, nenhum atrito.

> `[DECISÃO]` **Profissão.** Contraste com
> `examples/guard-villager-decision.md`, onde três das seis não tinham resposta.

## 2. Verificar a forma

```bash
javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession | head -20
```

`[FATO]` MC 1.21.1 — é um `record`:

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

> **Não há trades, tasks nem schedule aqui.** Se o seu plano previa mexer neles
> pela profissão, ele precisa mudar agora — não depois.

## 3. As sete peças, em ordem

```text
1. BLOCO forja
2. POI                     ← todos os block states
3. VILLAGER_PROFESSION
4. TASK                    ← o comportamento NÃO vem da profissão
5. SCHEDULE                ← a Vanilla basta
6. TRADES                  ← sistema separado
7. RESOURCES
```

## 4. Bloco e POI

`examples/custom-job-site.md` — faça primeiro, e **teste a reivindicação antes de
seguir**.

## 5. A profissão

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
[ ] INCONDICIONAL        ← condicional derruba a conexão no handshake
[ ] DETERMINÍSTICO
```

**Dois predicados, não um:** `held` é o que ele mantém, `acquirable` é o que ele
pode adquirir. Regras diferentes — um aldeão pode manter um local que já não
poderia adquirir.

```bash
./gradlew build && ./gradlew runClient
```

**Teste agora:** um aldeão sem profissão adquire ao ver a forja? Se não, o
problema é o POI ou o predicado — e ainda não há comportamento confundindo o
diagnóstico.

## 6. O comportamento

A profissão não traz comportamento. Uma task:

```java
public final class FundirTask extends MultiTickTask<VillagerEntity> {

    public FundirTask() {
        super(Map.of(MemoryModuleType.JOB_SITE, MemoryModuleState.VALUE_PRESENT),
              MIN_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return ehFerreiro(villager)
            && ehHorarioDeTrabalho(world, villager)
            && temMinerio(villager);
    }
    ...
}
```

```text
[ ] o gate exige JOB_SITE — declarativo, mais barato que checar em shouldRun
[ ] verifica a profissão
[ ] instalada por setTaskList, sem remover nada
[ ] perde para PANIC e RAID
```

`examples/custom-behavior.md` para o padrão completo.

## 7. Trades — sistema separado

`templates/trade-plan.md`.

```text
Nível 1  4 ferro       → 1 emerald
Nível 2  1 emerald     → 1 picareta de ferro
Nível 3  ...
```

**A verificação que não pode faltar:**

```text
[ ] as ofertas fecham um ciclo de lucro?
    "4 ferro → 1 emerald" + "1 emerald → 1 picareta de ferro (3 ferro)"
    → o jogador perde ferro no ciclo. NÃO fecha. ✓
```

Duas ofertas que se fecham transformam o aldeão numa máquina de emeralds — o erro
de economia mais comum, e silencioso.

## 8. Resources

```text
[ ] lang: "entity.minecraft.villager.ferreiro"
[ ] som de trabalho registrado
[ ] textura de roupa
[ ] o bloco: lang, modelo, textura, loot table, tags, item group
```

Sem textura ele funciona com o visual padrão — mas o jogador não distingue o
ferreiro, que era o ponto.

## 9. Testar

```bash
./gradlew build && ./gradlew runGametest && ./gradlew runClient && ./gradlew runServer
```

```text
[x] aldeão sem profissão adquire ao ver a forja
[x] só UM por forja
[x] a forja acesa mantém o POI
[x] quebrar a forja libera
[x] ele vai à forja no horário de trabalho
[x] funde e produz
[x] visual, nome e som corretos
[x] as trades aparecem e sobem de nível
[x] nenhum ciclo infinito de lucro
[x] ele dorme, come e socializa normalmente
[x] fechar e reabrir mantém profissão, nível e XP
[x] servidor dedicado: tudo acima
[x] as profissões Vanilla continuam funcionando
```

Os dois últimos são os mais pulados: o penúltimo pega registro condicional, o
último é regressão.

## Entregar

> **Feature:** profissão ferreiro, com forja como local de trabalho, fundição e
> comércio de ferramentas.
>
> **Arquitetura:** POI + profissão (registros, degrau 2) + uma task (degrau 6).
> **Zero Mixin novo** — a instalação de tasks já existia.
>
> **Por que profissão e não capacidade:** tem lugar, reivindicação, identidade
> visual e comércio — as quatro peças que definem identidade.
>
> **Verificado rodando:** aquisição, exclusividade, trabalho, comércio, ciclo do
> dia, persistência, `runServer`.
>
> **Compatibilidade:** LOW. Registros novos, nada removido.

---

## O que este exemplo demonstra

1. **A pergunta de escopo tem resposta "sim" aqui** — e o contraste com o guarda
   mostra como distinguir.
2. **A profissão não traz comportamento nem trades.** Três sistemas separados.
3. **Testar a aquisição antes do comportamento.**
4. **A verificação de ciclo de lucro** é obrigatória e quase sempre esquecida.
5. **Registro incondicional** — o bug que só aparece em multiplayer.
