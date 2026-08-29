# Mappings

O Minecraft é distribuído ofuscado. Todo nome legível que você escreve — `VillagerEntity`,
`initBrain`, `WALK_TARGET` — é uma tradução aplicada pelo toolchain. Traduções
diferentes existem ao mesmo tempo, e mudam entre versões.

Consequência prática: **um nome que funciona num tutorial pode não existir no seu
projeto**, e o compilador só descobre depois que você escreveu tudo.

## Os três sistemas

| Mapping | Quem usa | Aparência |
|---|---|---|
| **Obfuscated** | o jar oficial | `com`, `ccs`, `a` |
| **Intermediary** | Fabric em runtime; estável entre versões | `class_4095`, `method_18882` |
| **Yarn** | Fabric na compilação (comunidade) | `Brain`, `setTaskList` |
| **Mojmap** | oficial da Mojang; comum em NeoForge/Forge | `Brain`, `setMemory` |

Yarn e Mojmap **não são iguais**. Nomes coincidem às vezes e divergem noutras
(`ServerWorld` em Yarn é `ServerLevel` em Mojmap; `BlockPos` é igual). Código
copiado de projeto Mojmap não compila num projeto Yarn sem tradução.

O intermediary é o pivô: é ele que aparece dentro do jar publicado, e é por isso
que um mod compilado com Yarn roda ao lado de um compilado com Mojmap.

## Descobrir o que o projeto usa

```bash
grep -E "minecraft_version|yarn_mappings|loader_version|fabric_version" gradle.properties
grep -n "mappings" build.gradle
```

`mappings "net.fabricmc:yarn:1.21.1+build.3:v2"` → Yarn.
`mappings loom.officialMojangMappings()` → Mojmap.

Isto vai no topo de qualquer documento de pesquisa. Conclusão sobre nome sem
versão registrada é conclusão sem validade.

## Verificar um nome — as três checagens

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)
```

**1. A classe existe com esse nome?**

```bash
unzip -l "$MC_JAR" | grep "VillagerEntity.class"
```

**2. Quais as assinaturas reais?**

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.task.MultiTickTask
```

Saída verificada em 1.21.1 / yarn 1.21.1+build.3:

```text
protected boolean shouldRun(net.minecraft.server.world.ServerWorld, E);
protected void run(net.minecraft.server.world.ServerWorld, E, long);
protected boolean shouldKeepRunning(net.minecraft.server.world.ServerWorld, E, long);
public net.minecraft.entity.ai.brain.task.MultiTickTask(java.util.Map<...>, int, int);
```

Isso é `[FATO]`. O que você lembrava antes de rodar era `[HIPÓTESE]`.

**3. O nome de método existe no mapping?**

```bash
grep -P "\tsetTaskList$" "$MAPPINGS"
```

Cada linha do `.tiny` traz `descriptor  obf  intermediary  yarn`:

```text
m  (Lcom;ILcom/google/common/collect/ImmutableList;)V  a  method_18882  setTaskList
```

Três sobrecargas com o mesmo nome Yarn e intermediaries diferentes significam que
você **precisa do descriptor** para mirar a certa num Mixin.

## Descriptors — o que o Mixin realmente usa

`@Inject(method = "...")` casa por nome **e** descriptor quando há sobrecarga. Sem
descriptor, o Mixin falha no boot com "ambiguous" ou acerta a errada.

Leitura rápida de descriptor:

```text
(Lnet/minecraft/entity/ai/brain/Brain;)V
 └─ um parâmetro Brain, retorno void

(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/entity/passive/VillagerEntity;J)Z
 └─ ServerWorld, VillagerEntity, long → boolean

Z boolean · B byte · C char · S short · I int · J long · F float · D double · V void
[X array de X · Lpacote/Classe; objeto
```

Obtenha o descriptor real com:

```bash
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 initBrain
```

## Onde os nomes quebram entre versões

Mudanças que já pegaram muita gente, e o que elas significam para você:

- **1.20.5+** — Data Components substituem boa parte do NBT de item. Assinaturas
  de serialização ganharam `RegistryWrapper.WrapperLookup`. Todo código anterior
  de `writeNbt`/`readNbt` mudou de forma.
- **1.20.5+** — a API de networking passou a `CustomPayload` com registro de tipo.
  Packets antigos (`PacketByteBuf` cru com `Identifier`) não valem.
- **1.21** — mudanças em enchantments (viraram data-driven) e em `Identifier`
  (construtor direto deu lugar a factories como `Identifier.of`).
- **Yarn muda entre builds** da mesma versão do MC. `1.21.1+build.3` e
  `1.21.1+build.10` podem renomear um método. Por isso a versão de mappings entra
  no registro, não só a do Minecraft.

Nenhum desses itens dispensa a verificação. Use-os como **onde desconfiar
primeiro**, não como verdade a repetir.

## Antes de reusar código de outro projeto

Checklist honesto — e a resposta "não sei" conta como reprovado:

```text
[ ] Qual versão do Minecraft aquele projeto usa?
[ ] Qual mapping? (Yarn / Mojmap / outro)
[ ] Qual versão da Fabric API?
[ ] As classes citadas existem na minha versão?      → unzip -l
[ ] As assinaturas batem?                            → javap
[ ] Algum método foi removido ou movido?
[ ] O comportamento mudou, mesmo com nome igual?
```

O último é o mais traiçoeiro: nome idêntico, assinatura idêntica, semântica nova.
Só a leitura do corpo (`genSources`) ou um experimento resolve.

## Access wideners

Quando o alvo é `private`/`final` e você precisa dele, existe um degrau antes do
Mixin: o access widener, declarado no `fabric.mod.json` e num `.accesswidener`.

```text
accessible  method  net/minecraft/entity/passive/VillagerEntity  initBrain  (L...;)V
```

Ele **não** é isento de risco de versão — o descriptor é o mesmo que quebra. Mas
é menos invasivo que um Mixin e não conflita com outros mods do mesmo jeito.

## Regra final

Escreva o nome depois de verificar, não antes. `javap` custa segundos; um Mixin
que falha no boot custa uma sessão de depuração e, quando falha silenciosamente,
custa um bug que aparece semanas depois.
