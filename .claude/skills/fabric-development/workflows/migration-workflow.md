# Workflow — migrar de versão

Modo **SURGERY**. Trocar a versão do Minecraft quebra coisas em camadas, e a
tentação é corrigir erro de compilação até o build passar.

**Isso é o erro central da migração.** Build passando não significa
comportamento preservado: o método pode ter o mesmo nome, a mesma assinatura, e
semântica nova.

Referência: `references/migration.md`.

---

## 1. Inventário — o antes

Registre o estado atual **antes de tocar em qualquer coisa**:

```bash
grep -E "minecraft_version|yarn_mappings|loader_version|fabric_version|junit" gradle.properties
grep -n "loom\|toolchain\|JavaLanguageVersion" build.gradle
cat src/main/resources/fabric.mod.json
find . -name "*.mixins.json" -not -path "*/build/*" -exec cat {} \;
```

```text
Minecraft · Fabric Loader · Fabric API · Loom · Java · Mappings · Gradle
```

E o inventário do que **vai quebrar**:

```text
[ ] quantos Mixins, e em quais classes
[ ] quais APIs Fabric são usadas
[ ] qual o formato de persistência
[ ] há packets?
[ ] há datagen?
```

## 2. Estado limpo de partida

```bash
git status
./gradlew build       # passa HOJE?
```

Migração começando de build quebrado é impossível de avaliar. E faça isso **numa
branch**, não na principal.

## 3. Subir as versões — uma coisa por vez

```text
1. Minecraft + Yarn      ← o grosso da quebra
2. Fabric Loader
3. Fabric API
4. Loom / Gradle / Java  ← se a versão nova exigir
```

Tudo de uma vez transforma cinco causas num pântano só.

```bash
./gradlew --refresh-dependencies build
./gradlew genSources     # gere os fontes da versão NOVA
```

## 4. As seis camadas de quebra

Nesta ordem, porque cada uma esconde a seguinte:

```text
BUILD BREAKS      → o Gradle/Loom não resolve
API BREAKS        → classe/método da Fabric mudou
MAPPING BREAKS    → nome Yarn mudou
MIXIN BREAKS      → alvo sumiu, descriptor mudou
DATA BREAKS       → formato de save/JSON mudou
RUNTIME BREAKS    → compila, roda, faz outra coisa   ← o perigoso
```

O último não aparece no compilador. É o que este workflow existe para pegar.

## 5. Corrigir com verificação, não por tentativa

Para **cada** erro de compilação:

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)
MAPPINGS=$(find ~/.gradle/caches/fabric-loom -name "mappings.tiny" | head -1)

unzip -l "$MC_JAR" | grep "<Classe>.class"        # ainda existe?
javap -s -cp "$MC_JAR" <fqn> | grep -A1 "<metodo>" # qual a assinatura agora?
grep -P "\t<metodo>$" "$MAPPINGS"                  # foi renomeado?
```

E a pergunta que o compilador não faz:

```text
O nome é o mesmo. A assinatura é a mesma.
O COMPORTAMENTO é o mesmo?
```

Quando houver dúvida, leia o corpo nos sources gerados. Renomeação silenciosa de
semântica é rara, mas é exatamente o que produz o bug que ninguém acha depois.

### Pontos de ruptura conhecidos

Use como **onde desconfiar primeiro** — não como verdade a repetir sem verificar:

| Versão | Mudança |
|---|---|
| 1.20.5 | Data Components substituem NBT de item |
| 1.20.5 | serialização ganha `RegistryWrapper.WrapperLookup` |
| 1.20.5 | networking passa a `CustomPayload` tipado |
| 1.21 | encantamentos viram data-driven |
| 1.21 | `Identifier` passa a factory (`Identifier.of`) |
| qualquer | Yarn muda entre **builds** da mesma versão do MC |

## 6. Mixins — um a um

Mixin é onde a migração mais silenciosamente falha.

```text
[ ] a classe alvo ainda existe
[ ] o método ainda existe, com o mesmo descriptor
[ ] o injection point ainda existe
    (INVOKE/FIELD quebram com refactor interno que não muda assinatura)
[ ] defaultRequire: 1 está ativo → falha alto em vez de silenciar
```

```bash
./gradlew runClient    # o log de boot acusa mixin que não aplicou
```

**Aviso de mixin no boot não é ruído.** É a migração dizendo onde está quebrada.

## 7. Dados e saves

```text
[ ] o formato de NBT mudou?
[ ] o caminho de datapack mudou? (recipe/recipes, loot_table/loot_tables)
[ ] tags mudaram de nome?
[ ] há versão gravada no save que permita migrar?
```

**Teste explícito:** crie um mundo na versão antiga, abra na nova.

```text
[ ] o mundo abre
[ ] o estado do mod voltou
[ ] nada foi apagado silenciosamente
```

Se o formato mudou sem migração, o jogador perde o progresso — e isso precisa ser
uma decisão declarada, não um efeito colateral.

## 8. Runtime

Compilar é o começo.

```bash
./gradlew runClient
./gradlew runServer
./gradlew runGametest
```

```text
[ ] o jogo inicia
[ ] o servidor dedicado inicia
[ ] o conteúdo aparece (registros ok)
[ ] os resources aparecem (nada de cubo preto e rosa)
[ ] a feature principal funciona
[ ] gametest passa
[ ] o log não tem exceção nova
```

## 9. Regressão comportamental

A parte que quase todo mundo pula, e a razão de existir deste workflow.

```text
[ ] cada feature do mod foi exercitada em jogo
[ ] os comportamentos Vanilla que o mod preserva continuam
[ ] performance comparável (a nova versão pode ter mudado custos)
[ ] multiplayer testado
```

## 10. Relatório

`templates/migration-report.md`:

```text
[ ] versões antes → depois
[ ] o que quebrou, por camada
[ ] o que mudou de semântica (não só de nome)
[ ] o que NÃO foi testado
[ ] riscos remanescentes
[ ] o que ficou para depois
```

Atualize também a documentação técnica que fixa versões — se o projeto tem um
documento que declara o ambiente oficial, ele muda **no mesmo commit**.

## Fechamento

`checklists/release.md`.

Relate honestamente **o que foi verificado rodando** e **o que apenas compila**.
Migração "concluída" que só compilou é migração não concluída, e declarar isso é
mais barato do que descobrir depois.
