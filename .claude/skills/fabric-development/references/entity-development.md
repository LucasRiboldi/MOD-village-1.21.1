# Desenvolver entidades

Uma entidade é **duas coisas**: um objeto autoritativo no servidor e uma
representação no cliente. Manter isso na cabeça evita a maior parte dos bugs que
só aparecem em multiplayer.

## Os três registros

Entidade exige três coisas distintas, e faltando qualquer uma o resultado é
crash ou invisibilidade:

```java
// 1. o tipo — comum
public static final EntityType<MeuMob> MEU_MOB =
        Registry.register(Registries.ENTITY_TYPE,
                Identifier.of(MOD_ID, "meu_mob"),
                EntityType.Builder.create(MeuMob::new, SpawnGroup.CREATURE)
                        .dimensions(0.6f, 1.95f)
                        .build());

// 2. atributos — comum; SEM ISTO, crash no spawn
FabricDefaultAttributeRegistry.register(MEU_MOB, MeuMob.createAttributes());

// 3. renderer — SÓ NO CLIENTE
EntityRendererRegistry.register(MEU_MOB, MeuMobRenderer::new);
```

> A API de builder e o nome das classes mudaram entre versões. Confirme com
> `javap` antes de copiar.

O terceiro vai no entrypoint `client`. No `main`, o servidor dedicado lança
`NoClassDefFoundError` no boot.

## Ciclo de vida

```text
spawn / load → initialize → tick → ai tick → interaction → damage
                                                              ↓
                                    death · conversion · despawn · chunk unload
                                                              ↓
                                                        save / remove
```

### As quatro saídas

**Morte não é a única.** Ao reagir a "a entidade se foi", cubra todas as que
importam:

| Saída | Evento | Observação |
|---|---|---|
| morte | `ServerLivingEntityEvents.AFTER_DEATH` | |
| **conversão** | `ServerLivingEntityEvents.MOB_CONVERSION` | zumbificação — **não** passa por morte |
| despawn | — | a entidade simplesmente some |
| chunk unload | — | **não é morte** |

E a regra que evita apagar o que está vivo:

> **Ausência não é morte.** Uma entidade fora do raio, ou em chunk descarregado,
> não morreu — apenas não foi vista. Registro que remove por ausência de varredura
> vai apagar entidades vivas assim que o jogador se afastar.

Só o evento serve como prova.

## Dados — três mecanismos, três propriedades

| Mecanismo | Para | Sincroniza | Persiste |
|---|---|---|---|
| campo Java | estado do tick | não | não |
| `DataTracker` | o que o cliente precisa ver | **sim** | não |
| NBT (`writeCustomDataToNbt`) | o que sobrevive ao save | não | **sim** |

O erro clássico é escolher um e esperar as três propriedades. **Estado que
precisa aparecer no cliente e sobreviver ao save precisa dos dois** — dois
códigos, não um.

```java
@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt);
    nbt.putInt("energia", energia);
}

@Override
public void readCustomDataFromNbt(NbtCompound nbt) {
    super.readCustomDataFromNbt(nbt);
    energia = nbt.getInt("energia");
}
```

Sem o par escrita/leitura, o campo some no restart. Nada é salvo automaticamente.

## Não ponha tudo na classe da entidade

A classe de entidade cresce sem limite se você deixar. Separe quando a lógica
justificar:

```text
Entity      ciclo de vida, ponte com o Minecraft
Behavior    o que ela faz
Data        o que ela sabe
AI          como decide          → ai-development.md
Integration como toca o mundo
```

Não crie as cinco por reflexo — crie quando a classe começar a fazer coisas
demais. Ver `project-architecture.md`.

## Interação e autoridade

```text
cliente detecta clique → envia intenção → SERVIDOR valida → executa →
altera estado → sincroniza → cliente exibe
```

O cliente pode prever para a animação parecer imediata, mas a verdade vem do
servidor. Ver `client-server.md`.

## Métodos que mudam o mundo pedem `ServerWorld`

```java
// ✓ o compilador impede a chamada errada
public void colher(ServerWorld world, BlockPos pos) { ... }

// ✗ checagem em runtime, fácil de esquecer
public void colher(World world, BlockPos pos) {
    if (world.isClient()) return;
}
```

Exigir `ServerWorld` na assinatura transforma um erro de runtime em erro de
compilação. É a diferença mais barata que existe.

## Estados de falha

**O mundo não é estático entre a decisão e a ação.** O jogador quebra o bloco, o
chunk descarrega, outra entidade pega o alvo.

Todo comportamento precisa tratar:

```text
alvo sumiu · caminho não encontrado · recurso indisponível
chunk descarregado · entidade morreu · jogador interrompeu
mundo mudou · servidor reiniciou
```

E precisa ter:

```text
[ ] TIMEOUT
[ ] condição de DESISTÊNCIA
[ ] o que fazer DEPOIS de desistir
```

Sem isso, a entidade fica presa tentando o impossível para sempre — consumindo
CPU, sem progresso, e parecendo quebrada para o jogador.

## Leitura de bloco segura

```java
// ✗ carrega o chunk que faltar — do tick, é gerar terreno no laço
BlockState state = world.getBlockState(pos);

// ✓ só lê o que já está carregado; null = "não sei agora, pulo"
WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
BlockState state = chunk == null ? null : chunk.getBlockState(pos);
```

Chamado de dentro de um evento de carga de chunk, o primeiro **trava a thread do
servidor**. Ver `performance.md`.

## Performance

Entidades são numerosas. Antes de pôr qualquer coisa no tick:

```text
Quantas existem num mundo real?     (não no teste com duas)
O custo é por entidade?
Há busca de bloco/entidade? Qual raio? Qual frequência?
Há pathfinding? Com que frequência?
Pode ser reativo em vez de por tick?
Dá para escalonar por entityId % intervalo?
```

## Checklist

```text
[ ] EntityType registrado
[ ] atributos registrados          senão crash no spawn
[ ] renderer registrado NO CLIENTE senão some ou crasha
[ ] NBT com par escrita/leitura
[ ] DataTracker para o que o cliente vê
[ ] todas as saídas tratadas (morte, conversão, despawn, chunk)
[ ] estados de falha com timeout
[ ] leitura de bloco sem forçar carga
[ ] lógica de servidor exige ServerWorld
[ ] testado em runServer
```
