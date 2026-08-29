# Exemplo — uma feature do começo ao fim

**Pedido:**

> "Quero que o aldeão lenhador leve a madeira que cortou até o baú dele."

Modo **FEATURE**, do pedido à entrega. Este é o exemplo que amarra as três skills.

---

## Passo 1 — Entender

```text
OBJETIVO    o lenhador deposita a madeira colhida no baú reservado a ele
SCOPE       transporte e depósito
NON GOALS   escolher a árvore, cortar, reservar o baú — já existem
VERSÃO      1.21.1 · Yarn 1.21.1+build.3 · Fabric API 0.116.15+1.21.1
```

Os **non goals** são metade do trabalho aqui: sem eles, "levar a madeira ao baú"
cresce durante a implementação e vira "refazer o sistema de trabalho".

## Passo 2 — A pesquisa já existe?

```bash
ls docs/research docs/decisions 2>/dev/null
grep -ril "brain\|walk_target\|baú\|storage" docs/ | head
```

Encontrado:

```text
docs/research/systems/villager-movement.md   → [FATO] WALK_TARGET controla o caminho
docs/decisions/ADR-004-mixin-policy.md       → task acrescentada em CORE, nada removido
docs/research/systems/storage.md             → [FATO] o baú é redescoberto por varredura,
                                                não persistido
```

E uma lacuna:

```text
[HIPÓTESE] a task consegue interromper o trabalho no meio para ir depositar
```

> **Isto bloqueia o desenho.** Se a interrupção não for possível como imagino, a
> arquitetura muda. Pausa para `minecraft-code-research`.

A pesquisa devolve:

```text
[FATO] shouldKeepRunning devolvendo false para a task no mesmo tick; o Brain
       encerra e reavalia. Verificado em MultiTickTask via javap, 1.21.1.
[FATO] tick/tryStarting/stop são final — o ciclo é dirigido pelo Brain.
```

Com isso, o desenho fecha. **Cinco minutos de pesquisa evitaram uma arquitetura
errada.**

## Passo 3 — Plano

`templates/feature-plan.md`, as seções que decidem:

```text
DATA         inventário do aldeão · posição do baú reservado
STATE        "o que ele carrega" e "para onde vai"
OWNERSHIP    servidor. O cliente não precisa saber de nada.
LIFECYCLE    começa ao encher; termina ao depositar ou perder o baú
CLIENT       — não se aplica: nenhum visual novo
SERVER       toda a lógica
PERSISTENCE  — ver abaixo
NETWORKING   — não se aplica: nada atravessa a fronteira
```

### A decisão de persistência

```text
o que ele carrega   → já está no inventário do aldeão, que o Vanilla salva
a posição do baú    → redescoberta por varredura a cada sessão
o destino atual     → intenção do momento
```

> `[DECISÃO]` **Nada novo é persistido.**
>
> Salvar o destino faria o aldeão, na sessão seguinte, ir até um baú que o
> jogador pode ter quebrado. Redescobrir é mais barato que reconciliar — e
> persistir criaria uma segunda verdade que envelhece.

Essa decisão vai **escrita** no plano. Sem isso, alguém "conserta" isso depois
achando que é esquecimento.

## Passo 4 — Arquitetura

**Qual é a menor arquitetura correta?**

```text
[ ] sistema Vanilla resolve?      não — depósito em baú reservado é regra do mod
[ ] cabe num registro?            não
[ ] é data-driven?                não
[ ] Fabric API cobre?             não
[ ] precisa de classe nova?       uma task, e a regra de "está cheio?"
[ ] precisa de Mixin?             NÃO — o Mixin de initBrain já existe
```

> `[DECISÃO]` Uma task nova, acrescentada à lista já instalada. **Zero Mixin
> novo.** A infraestrutura de integração com o Vanilla já foi paga.

Não há `DepositManager`, não há `TransportService`. Uma task e uma função.

## Passo 5 — Núcleo mínimo

A task que não faz nada além de existir e ser escolhida:

```java
public final class DepositarTask extends MultiTickTask<VillagerEntity> {

    public DepositarTask() {
        super(Map.of(), MAX_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return estaCheio(villager) && baúDe(villager).isPresent();
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity villager, long time) {
        baúDe(villager).ifPresent(pos -> irAte(villager, pos));
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
        return estaCheio(villager) && baúDe(villager).isPresent();
    }
}
```

```bash
./gradlew build
```

## Passo 6 — Integrar, uma peça por vez

```java
brain.setTaskList(Activity.CORE, PRIORITY_DEPOSITO, ImmutableList.of(new DepositarTask()));
```

`setTaskList` **acrescenta** — nada do Vanilla é removido, e a task de corte
continua onde está.

```bash
./gradlew build
```

### Movimento — o erro que a pesquisa evitou

```java
// ✗ o cérebro reescreve o destino no mesmo tick
villager.getNavigation().startMovingTo(x, y, z, 0.5f);

// ✓
brain.remember(MemoryModuleType.WALK_TARGET,
        new WalkTarget(new BlockPosLookTarget(baú), VELOCIDADE, ALCANCE));
```

E **manter** a memória enquanto o destino valer.

### Leitura de bloco segura

```java
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
if (chunk == null) return Optional.empty();     // não carregado: não sei agora
```

`world.getBlockEntity` forçaria carga de chunk de dentro do tick.

## Passo 7 — Estados de falha

O mundo não é estático entre a decisão e a ação:

| Situação | Comportamento |
|---|---|
| o baú foi quebrado | esquecer a reserva, voltar a trabalhar |
| o chunk descarregou | pular este ciclo, sem travar |
| o baú está cheio | desistir após N tentativas, não insistir |
| o aldeão morreu no caminho | o evento já limpa o registro |
| não chega em 24000 ticks | timeout da task encerra |

**Retry infinito é bug mesmo sem exceção nenhuma.**

## Passo 8 — Testar

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "deposito")
public void oLenhadorDepositaNoBaú(TestContext context) {
    ...
    context.assertTrue(baúTem(madeira), "a madeira não chegou ao baú");
    context.complete();
}
```

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

**O que o gametest não cobre, declarado:** o mundo de teste é vazio — não há vila
gerada. O caminho "aldeão dentro de uma vila real, com a agenda Vanilla ativa" só
é verificável em sessão de jogo.

## Passo 9 — Validar

```text
[x] o lenhador vai ao baú quando enche
[x] deposita
[x] volta a trabalhar
[x] os comportamentos Vanilla continuam (dorme, come, socializa)
[x] fechar e reabrir o mundo → sem estado novo perdido (não há estado novo)
[x] runServer sobe
[x] 20 aldeões: sem queda perceptível de TPS
```

O quarto item é o que separa "funciona" de "funciona sem quebrar nada".

## Passo 10 — Entregar

`templates/implementation-summary.md`:

> **Feature:** o lenhador leva a madeira ao baú reservado.
>
> **Arquitetura:** uma task em `Activity.CORE`, acrescentada à lista existente.
> **Zero Mixin novo** — a integração já estava paga.
>
> **Persistência:** nenhuma nova, deliberadamente. O destino é intenção do
> momento; o baú é redescoberto. Persistir criaria uma segunda verdade.
>
> **Verificado rodando:** `build`, `runGametest`, `runClient`, `runServer`,
> save/load, 20 aldeões.
>
> **Não verificado:** comportamento dentro de vila gerada — o mundo do gametest é
> vazio, e isso só se verifica em sessão de jogo.
>
> **Compatibilidade:** LOW. Nada removido, nada exclusivo, degrada para Vanilla.

E o conhecimento volta para a base:

```text
docs/research/systems/villager-movement.md
  + [FATO] shouldKeepRunning=false encerra a task no mesmo tick
```

---

## O que este exemplo demonstra

1. **A pesquisa entrou onde faltava, e só ali.** Cinco minutos evitaram uma
   arquitetura errada; repesquisar o que já estava documentado teria sido
   desperdício.
2. **Non goals impediram a feature de crescer** durante a implementação.
3. **A decisão de NÃO persistir foi escrita** — é o tipo de decisão que alguém
   desfaz por engano se ela não estiver registrada.
4. **Zero Mixin novo.** A infraestrutura de integração já existia; a feature
   pagou só o próprio custo.
5. **Estados de falha desenhados antes**, não descobertos em produção.
6. **O relato separa verificado de não verificado**, e diz por quê.
