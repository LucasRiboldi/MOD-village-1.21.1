# Análise de código Vanilla

Como achar o código certo do Minecraft, lê-lo com proveito e sair com fatos em
vez de impressões.

## Onde o Vanilla está no disco

Num projeto Loom, o Minecraft remapeado já está em cache. Descubra:

```bash
grep -E "minecraft_version|yarn_mappings" gradle.properties

MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven \
         -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)
echo "$MC_JAR"
```

Três coisas diferentes, não confunda:

| Artefato | O que é | Serve para |
|---|---|---|
| `minecraft-merged-*.jar` | classes compiladas, **nomes Yarn** | `javap`, confirmar existência e assinatura |
| `minecraft-merged-intermediary-*.jar` | mesmas classes, nomes intermediary | raramente; é o que o loader usa em runtime |
| `mappings.tiny` | tabela obf ↔ intermediary ↔ yarn | procurar nome, conferir se existe |

**Fontes decompilados** não vêm por padrão. Gere uma vez por versão:

```bash
./gradlew genSources     # pode demorar vários minutos
find ~/.gradle/caches/fabric-loom -name "*-sources.jar" | grep minecraft
```

Depois disso você lê o corpo do método sem extrair nada:

```bash
unzip -p "$MC_SOURCES" net/minecraft/entity/passive/VillagerEntity.java | sed -n '1,120p'
```

## Estratégia de busca

O erro comum é procurar pelo nome que você imagina. Procure pelo que o jogo
**mostra**, que é estável entre versões:

| Você tem | Procure por |
|---|---|
| Um texto da tela | a chave de tradução em `assets/minecraft/lang/en_us.json` |
| Um bloco/item | o id (`minecraft:composter`) no registro e nas loot tables |
| Um som | o nome em `sounds.json` |
| Um comportamento | a memória/POI envolvidos, não a classe da entidade |
| Um comando | a classe `*Command` correspondente |

Depois vá do id para o código:

```bash
# a classe existe com esse nome nesta versão?
unzip -l "$MC_JAR" | grep -i "Composter"

# quais classes mencionam o conceito?
unzip -l "$MC_JAR" | grep -i "villager" | head -30

# o nome de método existe? (coluna final do tiny = yarn)
grep -P "\tinitBrain$" "$MAPPINGS"
```

Com sources gerados, busca textual no jar:

```bash
mkdir -p /tmp/mcsrc && unzip -q -o "$MC_SOURCES" -d /tmp/mcsrc
grep -rn "MemoryModuleType.JOB_SITE" /tmp/mcsrc/net/minecraft | head -20
```

Essa última é a busca mais produtiva que existe: **quem usa** conta mais sobre o
sistema do que a declaração.

## Ler uma classe com proveito

Ordem que economiza tempo:

1. **Assinatura da classe** — o que ela estende e implementa já diz metade.
   `javap -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | head -5`
2. **Campos** — onde o estado mora. Campo `final` = identidade; campo mutável =
   estado que alguém escreve, e você quer saber quem.
3. **Métodos públicos e protected** — a superfície. Os `protected` são os
   extension points naturais por herança.
4. **Só então o corpo** do método que interessa.

O que perguntar de cada classe importante (formato completo em
`templates/vanilla-class-analysis.md`):

```text
RESPONSABILIDADE · HERANÇA · INTERFACES · CICLO DE VIDA
CAMPOS · MÉTODOS · ESTADO · DEPENDÊNCIAS
CALLERS · SIDE EFFECTS · EVENTOS · PERSISTÊNCIA
EXTENSION POINTS · RISCOS DE MODIFICAÇÃO
```

## Callers importam mais que a classe

Uma classe Vanilla isolada raramente explica o comportamento. O que explica é
**quem a chama, quando, e o que faz com o resultado**.

```bash
grep -rn "\.initBrain(" /tmp/mcsrc/net/minecraft | head
grep -rn "new VillagerEntity(" /tmp/mcsrc/net/minecraft | head
```

Duas descobertas típicas que só o caller entrega:

- o método é chamado **duas vezes** por caminhos diferentes (spawn e load), e sua
  mudança precisa valer nos dois
- o retorno é ignorado no único caller, e portanto mudá-lo não muda nada

## Encontrar extension points

Antes de concluir "só dá com Mixin", varra estes, nesta ordem:

1. **O registro aceita entrada nova?** Profissão, POI, memória, sensor, tipo de
   entidade — muita coisa é registro, e registrar é o degrau mais barato.
2. **É data-driven?** Loot table, recipe, tag, advancement, worldgen — JSON de
   datapack substitui Java com frequência. Ver `data-driven-analysis.md`.
3. **O método é `protected` numa classe que dá para estender?**
4. **Existe interface implementável** que o Vanilla consulta?
5. **A Fabric API cobre?** Ver `fabric-analysis.md`.
6. **Existe evento Vanilla** (raro, mas existe em alguns sistemas)?

Só depois disso `mixin-analysis.md`.

## Sinais de perigo ao ler

Anote quando encontrar — viram `[RISCO]` no documento final:

- **`static` mutável** no Vanilla: estado global, compartilhado entre mundos.
- **Método chamado no tick** de todas as entidades: qualquer custo seu ali é
  multiplicado por N.
- **Método que carrega chunk** (`World.getBlockState`, `getBlockEntity`): chamá-lo
  de dentro do tick ou de um evento de carga trava a thread do servidor.
  A alternativa que só lê chunk carregado:

  ```java
  WorldChunk chunk = world.getChunkManager().getWorldChunk(pos.getX() >> 4, pos.getZ() >> 4);
  BlockState state = chunk == null ? null : chunk.getBlockState(pos);   // null = não carregado
  ```

  `[FATO]` verificado em MC 1.21.1: `getWorldChunk` devolve `null` para chunk não
  carregado, sem forçar geração.
- **Assinatura com `RegistryWrapper.WrapperLookup`**: é 1.20.5+; código antigo não
  compila e tutoriais anteriores estão errados para você.
- **Lambda ou classe anônima** dentro do método que você quer atingir com Mixin:
  o alvo real é uma classe sintética, e o `@Inject` no método externo não pega.

## Fechando

Saia da análise com a cadeia montada, não com um resumo:

```text
Entry → Core Class → Supporting → Data → State → Events → Persistence → Integration
```

E com cada elo etiquetado (`evidence-and-claims.md`). Elo em `[HIPÓTESE]` é onde
o próximo esforço deve ir — seja `javap`, seja experimento.
