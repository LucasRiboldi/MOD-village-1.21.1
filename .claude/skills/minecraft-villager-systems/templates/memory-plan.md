# Plano de memória — <nome>

> Uma por memória nova. **Memória não é variável: é conhecimento com tempo de
> vida.**

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Existe uma Vanilla que serve?

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i "<conceito>"
```

`[FATO]` MC 1.21.1 — já existem:

```java
MemoryModuleType<GlobalPos>        HOME · JOB_SITE · POTENTIAL_JOB_SITE · MEETING_POINT
MemoryModuleType<List<GlobalPos>>  SECONDARY_JOB_SITE
MemoryModuleType<WalkTarget>       WALK_TARGET
MemoryModuleType<LookTarget>       LOOK_TARGET
MemoryModuleType<BlockPos>         NEAREST_BED
MemoryModuleType<Long>             CANT_REACH_WALK_TARGET_SINCE
```

**Resposta:** <serve / não serve, e por quê>

> Reusar a Vanilla é melhor: o jogo já a preenche, expira e persiste. Uma memória
> paralela para o mesmo conceito produz duas verdades que divergem.

## Identificação

| | |
|---|---|
| Nome | `meumod:<path>` |
| Tipo | `MemoryModuleType<?>` |
| Codec | sim / não |

## Quem escreve

| Quem | Quando | Valor |
|---|---|---|
| sensor / task / evento | | |

> Se ninguém escreve, a memória nunca fica preenchida e a task que a exige nunca
> roda. É a causa número um de "a task não executa".

## Quem lê

| Quem | Para quê |
|---|---|

## Tempo de vida

```text
[ ] permanente na sessão
[ ] expira em <n> ticks
[ ] limpa por evento específico
```

## Default

<O que significa "vazia"? É estado válido, ou erro?>

## Limpeza

> O campo mais esquecido. Produz o aldeão que "lembra" de um alvo que não existe
> mais.

| Situação | Limpa? |
|---|---|
| tarefa concluída | |
| alvo sumiu | |
| aldeão mudou de profissão | |
| aldeão morreu / foi convertido | |
| chunk descarregou | |
| servidor reiniciou | |

## Persistência

```java
new MemoryModuleType<>(Optional.of(BlockPos.CODEC))   // persiste
new MemoryModuleType<>(Optional.empty())              // só de sessão
```

**Escolha:** <com / sem codec>

**Por quê:**

| Se é | Codec |
|---|---|
| vínculo, identidade, reivindicação | **sim** |
| alvo atual, intenção do momento | **não** |
| cooldown em andamento | depende — reiniciar é aceitável? |

> Persistir intenção do momento faz o aldeão, na sessão seguinte, ir até um alvo
> que o jogador já removeu.

## Registro

```java
public static final MemoryModuleType<BlockPos> MEU_ALVO =
        Registry.register(Registries.MEMORY_MODULE_TYPE,
                Identifier.of(MOD_ID, "meu_alvo"),
                new MemoryModuleType<>(Optional.of(BlockPos.CODEC)));
```

```text
[ ] no entrypoint, incondicional, determinístico
[ ] namespace próprio
[ ] REGISTRADA NO PERFIL DO BRAIN do aldeão   ← senão não funciona
```

> A última é a etapa que falta quando "registrei a memória e não funciona".

## Sincronização

<O cliente precisa saber? Memórias são server-side por padrão.>

## Uso como gate

```java
super(Map.of(ModMemories.MEU_ALVO, MemoryModuleState.VALUE_PRESENT), MIN, MAX);
```

<Qual estado a task exige?>

## Teste

```text
[ ] é escrita quando deveria
[ ] expira quando deveria
[ ] é limpa em cada situação da tabela
[ ] a task que a exige roda
[ ] com codec: sobrevive a fechar e reabrir o mundo
[ ] sem codec: NÃO sobrevive — e isso é intencional
```
