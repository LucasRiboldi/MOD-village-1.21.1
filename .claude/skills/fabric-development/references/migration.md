# Migração de versão

O erro central da migração: **corrigir erros de compilação até o build passar.**

Build passando não significa comportamento preservado. O método pode ter o mesmo
nome, a mesma assinatura, e semântica nova — e isso não aparece no compilador.

Processo em `workflows/migration-workflow.md`.

## O loop de verificação

Vale para toda migração e para toda dúvida de API. É a técnica mais útil desta
skill inteira.

```bash
grep -E "minecraft_version|yarn_mappings" gradle.properties
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)

# a classe ainda existe?
unzip -l "$MC_JAR" | grep "VillagerEntity.class"

# qual a assinatura AGORA?
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 initBrain

# o método foi renomeado?
grep -P "\tsetTaskList$" "$MAPPINGS"
```

E a pergunta que o compilador não faz:

> O nome é o mesmo. A assinatura é a mesma. **O comportamento é o mesmo?**

Quando houver dúvida, leia o corpo: `./gradlew genSources`.

## As seis camadas de quebra

Nesta ordem, porque cada uma esconde a seguinte:

```text
BUILD BREAKS      Gradle/Loom não resolve
API BREAKS        classe/método da Fabric mudou
MAPPING BREAKS    nome Yarn mudou
MIXIN BREAKS      alvo sumiu, descriptor mudou, injection point moveu
DATA BREAKS       formato de save/JSON mudou
RUNTIME BREAKS    compila, roda, faz outra coisa      ← o perigoso
```

O último não aparece no compilador. É por isso que a regressão comportamental é
obrigatória.

## Pontos de ruptura conhecidos

Use como **onde desconfiar primeiro** — nunca como verdade a repetir sem
verificar.

| Versão | Mudança | O que quebra |
|---|---|---|
| **1.20.5** | Data Components substituem NBT de item | todo código de NBT customizado em item |
| **1.20.5** | serialização ganha `RegistryWrapper.WrapperLookup` | `writeNbt`/`readNbt` de block entity e `PersistentState` |
| **1.20.5** | networking → `CustomPayload` tipado | todos os packets |
| **1.21** | encantamentos viram data-driven | código que registrava encantamento |
| **1.21** | `Identifier` passa a factory (`Identifier.of`) | construção de ids |
| qualquer | **Yarn muda entre builds** da mesma versão do MC | nomes de método |

O último é o mais esquecido: `1.21.1+build.3` e `1.21.1+build.10` podem renomear
métodos. Por isso a **versão de mappings** entra no registro, não só a do
Minecraft.

## Ordem de atualização

Uma coisa por vez:

```text
1. Minecraft + Yarn      ← o grosso da quebra
2. Fabric Loader
3. Fabric API
4. Loom / Gradle / Java
```

Tudo junto transforma cinco causas num pântano só.

```bash
./gradlew --refresh-dependencies build
./gradlew genSources
```

## Mixins — onde a migração falha em silêncio

```text
[ ] a classe alvo ainda existe
[ ] o método ainda existe, com o mesmo descriptor
[ ] o injection point ainda existe
[ ] defaultRequire: 1 está ativo
```

`INVOKE` e `FIELD` são os mais frágeis: dependem de a chamada existir naquele
ponto. Um refactor interno da Mojang que **não muda assinatura nenhuma** já
quebra o injection point.

```bash
./gradlew runClient    # o log de boot acusa mixin que não aplicou
```

**Aviso de mixin no boot não é ruído.** É a migração dizendo onde está quebrada.

## Dados e saves

```text
[ ] o formato de NBT mudou?
[ ] caminhos de datapack mudaram? (recipe/recipes, loot_table/loot_tables)
[ ] tags mudaram de nome?
[ ] há versão gravada que permita migrar?
```

**Teste explícito:** mundo criado na versão antiga, aberto na nova.

```text
[ ] o mundo abre
[ ] o estado do mod voltou
[ ] nada foi apagado silenciosamente
```

Se o formato mudou sem migração, o jogador perde progresso — e isso precisa ser
uma **decisão declarada**, não um efeito colateral descoberto por quem atualizou.

## Regressão comportamental

A parte que quase todo mundo pula, e a razão de o build passar não bastar:

```text
[ ] cada feature do mod exercitada em jogo
[ ] os comportamentos Vanilla que o mod preserva continuam
[ ] performance comparável (a versão nova pode ter mudado custos)
[ ] multiplayer testado
```

## Documentação de ambiente

Se o projeto tem um documento que fixa o ambiente oficial (versões de MC, Yarn,
Loader, Fabric API, Java), ele muda **no mesmo commit** que o
`gradle.properties`.

Documento de versão que diverge do build é pior que ausente: alguém vai confiar
nele.

## Relatório

`templates/migration-report.md`:

```text
[ ] versões antes → depois
[ ] o que quebrou, por camada
[ ] o que mudou de SEMÂNTICA, não só de nome
[ ] o que NÃO foi testado
[ ] riscos remanescentes
```

## Checklist

```text
[ ] build passava ANTES de começar
[ ] trabalho numa branch
[ ] uma camada de versão por vez
[ ] cada nome verificado com javap/mappings, não adivinhado
[ ] semântica conferida onde houve dúvida
[ ] mixins verificados um a um, sem aviso no boot
[ ] save da versão anterior abre
[ ] runClient, runServer e runGametest passam
[ ] regressão comportamental feita, não presumida
[ ] relatório separa "verificado rodando" de "apenas compila"
```
