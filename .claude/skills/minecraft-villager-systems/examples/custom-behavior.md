# Exemplo — comportamento novo, sem profissão

**Pedido:**

> "Quero que o fazendeiro leve o que colheu até um baú, em vez de ficar com tudo
> no inventário."

O caminho mais comum do domínio: **uma task + uma memória**. Sem profissão, sem
POI novo, sem Activity nova, e sem Mixin novo.

---

## 1. Qual camada?

```text
"ele deve LEVAR algo até um lugar"   → TASK
"ele precisa saber PARA ONDE"        → MEMÓRIA
"ele precisa ACHAR o baú"            → sensor, ou busca sob demanda
```

Nada aqui é identidade. **Não é profissão.**

## 2. Verificar o que já existe

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i "job\|home\|chest"
```

`JOB_SITE` e `HOME` existem — mas nenhum representa "o baú dele". Memória nova.

## 3. As duas peças

**Memória** (`templates/memory-plan.md`):

```text
NOME          meumod:deposit_chest
TIPO          GlobalPos          ← como JOB_SITE e HOME, por consistência
QUEM ESCREVE  a busca, quando encontra um baú livre
QUEM LÊ       a task de depósito
VALIDADE      permanente na sessão
CODEC         não                ← a posição do baú EXISTE NO MUNDO, redescoberta
LIMPEZA       baú quebrado · aldeão morto/convertido · alvo inválido
```

> A decisão de **não** persistir é deliberada: persistir faria o aldeão, na
> sessão seguinte, ir até um baú que o jogador já quebrou. Redescobrir é mais
> barato que reconciliar.
>
> Isso vai **escrito** no plano — senão alguém "conserta" depois.

**Task** (`templates/behavior-plan.md`):

```text
GATE         inventário cheio + memória do baú presente
ACTIVITY     CORE, com as condições dentro
PRIORIDADE   depois das tasks Vanilla de CORE
AÇÃO         WALK_TARGET até o baú → depositar
FIM          inventário vazio, ou baú sumiu
TIMEOUT      MAX_RUN_TIME
```

## 4. A task

```java
public final class DepositarTask extends MultiTickTask<VillagerEntity> {

    private static final float VELOCIDADE = 0.5f;
    private static final int MAX_RUN_TIME = 24_000;

    public DepositarTask() {
        super(Map.of(), MAX_RUN_TIME, MAX_RUN_TIME);
    }

    @Override
    protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
        return estaCheio(villager) && bauDe(villager).isPresent();
    }

    @Override
    protected void run(ServerWorld world, VillagerEntity villager, long time) {
        bauDe(villager).ifPresent(pos -> mirar(villager, pos));
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld world, VillagerEntity villager, long time) {
        return estaCheio(villager) && bauDe(villager).isPresent();
    }

    @Override
    protected void keepRunning(ServerWorld world, VillagerEntity villager, long time) {
        bauDe(villager).ifPresent(pos -> mirar(villager, pos));   // MANTÉM o WALK_TARGET
    }
}
```

`[FATO]` MC 1.21.1: `tick`, `tryStarting` e `stop` são `final` — o Brain dirige.

## 5. O movimento

```java
private void mirar(VillagerEntity villager, BlockPos destino) {
    villager.getBrain().remember(MemoryModuleType.WALK_TARGET,
            new WalkTarget(new BlockPosLookTarget(destino), VELOCIDADE, ALCANCE));
}
```

```text
✗  villager.getNavigation().startMovingTo(...)   → o cérebro reescreve no mesmo tick
```

E a memória é **reposta em `keepRunning`** — é isso que segura o aldeão no
caminho.

## 6. Leitura de bloco segura

```java
private static Optional<ChestBlockEntity> bauEm(ServerWorld world, BlockPos pos) {
    WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
    if (chunk == null) {
        return Optional.empty();       // não carregado: caso NORMAL, não erro
    }
    ...
}
```

`world.getBlockEntity` **carregaria o chunk** — de dentro do tick, isso é gerar
terreno no laço.

## 7. Instalar

```java
brain.setTaskList(Activity.CORE, PRIORIDADE_DEPOSITO, ImmutableList.of(new DepositarTask()));
```

Chamado pela instalação que o Mixin em `initBrain` **já dispara**. Nenhum Mixin
novo.

`[FATO]` `setTaskList` acrescenta — nenhuma task Vanilla é removida.

## 8. Falha

| Situação | Comportamento |
|---|---|
| baú quebrado | esquecer a memória, procurar outro |
| baú cheio | desistir após N tentativas |
| chunk descarregou | pular o ciclo |
| não alcança | `MAX_RUN_TIME` encerra |
| anoiteceu | a Schedule assume; a task para |
| inimigo perto | `PANIC` assume |
| aldeão morreu | a marca do baú é liberada |

**Ordem na morte:** liberar a marca do baú **antes** de esquecer o trabalhador —
a marca precisa da posição, que o esquecimento apaga.

## 9. Testar

```bash
./gradlew build && ./gradlew runGametest && ./gradlew runServer
```

```text
[x] ele vai ao baú quando enche
[x] deposita
[x] volta a trabalhar
[x] o CICLO DO DIA continua: acorda, sino, trabalha, socializa, dorme
[x] em pânico ele foge, e a task cede
[x] quebrar o baú no meio do caminho: ele desiste, sem travar
[x] fechar e reabrir: sem estado novo perdido (não há estado persistido novo)
[x] 20 aldeões: TPS estável
```

O quarto item é o teste de não-regressão do Vanilla — e o mais pulado.

## 10. Entregar

> **Feature:** o fazendeiro deposita a colheita num baú.
>
> **Arquitetura:** uma memória (sem codec) e uma task em `CORE`. **Zero Mixin
> novo** — a instalação já existia.
>
> **Persistência:** nenhuma nova, deliberadamente. A posição do baú existe no
> mundo e é redescoberta; persistir criaria uma segunda verdade.
>
> **Verificado rodando:** `build`, `runGametest`, `runServer`, ciclo do dia
> completo, 20 aldeões.
>
> **Compatibilidade:** LOW. Nada removido, nada exclusivo, degrada para Vanilla.

---

## O que este exemplo demonstra

1. **A camada certa era task + memória.** Nenhuma das outras cinco foi tocada.
2. **A decisão de não persistir foi escrita** — é o tipo de decisão que alguém
   desfaz por engano.
3. **`WALK_TARGET` mantido**, não navegação direta.
4. **Zero Mixin novo:** a infraestrutura já estava paga.
5. **O teste do ciclo do dia** é o que separa "funciona" de "funciona sem quebrar
   o Vanilla".
