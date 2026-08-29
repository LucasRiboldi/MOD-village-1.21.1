# Onde encaixar sem quebrar o Vanilla

A página a ler **antes** de decidir arquitetura. Ela responde: para o que eu
quero, qual é o degrau mais barato da escada de extensão?

## O mapa

| Quero | Degrau | Mecanismo | Toca o Vanilla? |
|---|---|---|---|
| que ele note algo | 2 | registrar **sensor** + memória | não |
| que ele lembre de algo | 2 | registrar **memória** | não |
| um local de trabalho novo | 2 | registrar **POI** | não |
| uma profissão nova | 2 | registrar **VillagerProfession** | não |
| que ele faça algo novo | 2+6 | **task** acrescentada por `setTaskList` | não remove nada |
| que ele venda outra coisa | 2/3 | **trades** — sistema separado | não |
| comportamento condicional a bloco/item | 3 | **tag** de datapack | não |
| um horário próprio | 2 | Schedule — mas leia antes | depende |
| mudar comportamento Vanilla existente | 9–11 | **Mixin**, justificado | sim |
| remover comportamento Vanilla | 11 | Mixin invasivo | **sim, alto risco** |

**As seis primeiras linhas cobrem a maior parte dos pedidos** — e nenhuma delas
toca o Vanilla. É por isso que a maioria dos mods de aldeão poderia ter muito
menos Mixin do que tem.

## O único Mixin que costuma ser necessário

Instalar tasks no Brain exige um gancho, porque o Brain é montado por instância:

```java
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))
    private void meumod$installTask(Brain<VillagerEntity> brain, CallbackInfo info) {
        MinhaInstalacao.install(brain);        // delega
    }
}
```

`[FATO]` verificado em MC 1.21.1: `VillagerEntity.initBrain` recebe
`Brain<VillagerEntity>`, sem sobrecarga — o `method = "initBrain"` basta.

Por que este Mixin é de baixo risco:

```text
[x] @Inject em TAIL — depois de o Vanilla montar o cérebro
[x] não cancela
[x] setTaskList ACRESCENTA — nenhuma task Vanilla é removida
[x] não assume índice de lista
[x] delega: três linhas, sem regra de negócio
[x] degrada: exceção capturada → o aldeão fica Vanilla
```

**Um Mixin, uma vez.** Todas as tasks do seu mod entram por ele. Se você está
escrevendo o segundo ou terceiro Mixin em `VillagerEntity`, pare e reveja o
desenho.

## O que NÃO fazer

```text
✗  remover tasks Vanilla
✗  substituir o Brain inteiro
✗  @Redirect em métodos de IA
✗  Mixin em classes internas de POI ou pathfinding
✗  capturar o aldeão para desligar a IA
```

Cada um resolve o caso imediato e cobra em compatibilidade: mods de aldeão e mods
de performance disputam exatamente estas classes.

Sobre o último: capturar o aldeão num bloco (a abordagem de alguns mods) é
simples e previsível, e **incompatível com qualquer mod que espere um aldeão
normal no mundo** — inclusive o seu, se você tiver outro sistema.

## Verificar antes de escrever

Nenhum nome desta página deve ser usado sem confirmação na sua versão:

```bash
grep -E "minecraft_version|yarn_mappings" gradle.properties
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

javap -cp "$MC_JAR" net.minecraft.village.VillagerProfession | head -20
javap -cp "$MC_JAR" net.minecraft.world.poi.PointOfInterestType
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.MemoryModuleType | grep -i job
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 initBrain
```

As APIs de aldeão mudaram entre versões — `VillagerProfession` virou `record`,
`PointOfInterestType` também. Código de tutorial mais antigo não compila.

## A Fabric ajuda em quê

```bash
find . -path "*loom-cache/remapped_mods*" -name "*-sources.jar" | grep -i "object-builder\|entity-events\|lifecycle"
```

| Preciso | Módulo |
|---|---|
| registrar POI, tipo de entidade, atributos | `fabric-object-builder-api-v1` |
| morte e **conversão** de aldeão | `fabric-entity-events-v1` |
| entrada/saída do mundo, tick | `fabric-lifecycle-events-v1` |
| sincronizar registros do mod | `fabric-registry-sync-v0` |

**Não afirme que a Fabric não cobre sem ter aberto os jars.** Ver
`minecraft-code-research/references/fabric-analysis.md`.

## A pergunta antes de subir a escada

```text
1. O Vanilla já faz isso?
2. Existe registro que aceita minha entrada?     ← POI, memória, sensor, profissão
3. É data-driven? (tag, loot table)
4. A Fabric API cobre?
5. Existe evento?
6. Dá para resolver com uma TASK acrescentada?
7. …
9-11. Mixin
```

Para aldeões, os degraus **2 e 6** resolvem quase tudo. Chegar ao 9 é normal uma
vez (o `initBrain`); chegar ao 11 quase sempre significa que a pergunta 6 não foi
bem respondida.

## Quando o Mixin é mesmo necessário

Casos legítimos:

```text
[ ] instalar tasks no Brain (initBrain)     ← o padrão
[ ] observar um evento que a Fabric não expõe
[ ] impedir um comportamento Vanilla específico, com justificativa
```

Para cada um: `fabric-development/templates/mixin-plan.md`, e a
justificativa **escrita**. Sem ela, ninguém três meses depois consegue saber se o
Mixin era necessário ou preguiça — e ele fica lá, comprando risco de graça.
