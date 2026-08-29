# Escrever Mixin

Como escrever, não se deve escrever. A decisão vem de `workflows/mixin-workflow.md`;
a análise, de `minecraft-code-research/references/mixin-analysis.md`.

**Mixin é integração, não arquitetura.** Ele é bytecode aplicado no carregamento,
mirando um método concreto de uma versão concreta. Tudo abaixo decorre disso.

## O padrão que funciona

```java
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))
    private void meumod$installTask(Brain<VillagerEntity> brain, CallbackInfo info) {
        MinhaClasseNormal.install(brain);
    }
}
```

Três propriedades que tornam este Mixin barato:

1. **`TAIL`, sem cancelar** — acrescenta depois que o Vanilla terminou.
2. **Delega** — o Mixin tem três linhas; a lógica é uma classe normal, testável.
3. **Prefixo `meumod$`** — evita colisão de nome com outro mod no mesmo alvo.

E do outro lado, a degradação:

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException falha) {
        LOGGER.warn("[meumod] não instalou a task — este aldeão fica vanilla", falha);
    }
}
```

**Nenhuma exceção sua escapa de dentro de método Vanilla.** Custo de degradar: uma
entidade comum. Custo de propagar: possivelmente o tick do servidor.

## Configuração

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.exemplo.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["VillagerEntityMixin"],
  "client": ["ClientOnlyMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

E no `fabric.mod.json`:

```json
"mixins": ["meumod.mixins.json"]
```

`defaultRequire: 1` faz o boot **falhar alto** quando o injector não aplica. É o
que você quer: Mixin que silenciosamente não aplica vira bug fantasma meses
depois, e o rastro já esfriou.

`require = 0` só quando a ausência do alvo é esperada **e tratada**.

Mixin de cliente na lista `client`. No servidor dedicado, a lista comum é
carregada e um alvo client-only quebra o boot.

## Verificar o alvo — sempre

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

unzip -l "$MC_JAR" | grep "VillagerEntity.class"
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 initBrain
```

Havendo sobrecarga, o descriptor é obrigatório:

```java
@Inject(method = "initBrain(Lnet/minecraft/entity/ai/brain/Brain;)V", at = @At("TAIL"))
```

Leitura de descriptor:

```text
Z boolean · B byte · C char · S short · I int · J long · F float · D double · V void
[X array · Lpacote/Classe; objeto
```

## Tipos, do mais barato ao mais caro

| Tipo | Uso | Convive |
|---|---|---|
| `@Accessor` | ler/escrever campo | sim |
| `@Invoker` | chamar método privado | sim |
| `@Inject` | rodar código num ponto | geralmente |
| `@WrapOperation` | envolver uma chamada (MixinExtras) | **sim, encadeia** |
| `@ModifyArg` / `@ModifyVariable` / `@ModifyConstant` | trocar valor | frágil |
| `@Redirect` | substituir uma chamada | **não — exclusivo** |
| `@Overwrite` | substituir o método | **não** |

Precisa envolver uma chamada? **`@WrapOperation` antes de `@Redirect`** — ele
encadeia, o outro é exclusivo por construção.

## Injection points

```java
@At("HEAD")     // entrada
@At("TAIL")     // antes do ÚLTIMO return — uma vez
@At("RETURN")   // antes de CADA return — n vezes
@At(value = "INVOKE", target = "L.../Classe;metodo()V")
@At(value = "FIELD", target = "L.../Classe;campo:Ltipo;")
```

**`TAIL` é uma vez; `RETURN` é por return.** Método com três saídas roda seu
código três vezes com `RETURN`. Se o efeito não for idempotente, é bug — e um
difícil de ver.

`INVOKE` e `FIELD` são precisos e, por isso, os mais frágeis entre versões: um
refactor interno da Mojang que não muda assinatura nenhuma já quebra o ponto.

## Acessar o próprio objeto

```java
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void meumod$noTick(CallbackInfo info) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        ...
    }
}
```

O cast duplo é o idioma. A classe do Mixin é `abstract` e nunca instanciada — ela
é um molde aplicado sobre o alvo.

## Cancelar

```java
@Inject(method = "metodo", at = @At("HEAD"), cancellable = true)
private void meumod$talvezCancele(CallbackInfo info) {
    if (condicao) info.cancel();
}
```

Cancelar é caro em compatibilidade: você suprime comportamento que outros mods
podem esperar. Faça só com justificativa escrita, e o mais tarde possível na
cadeia.

## Alvos que não existem como você pensa

- **Lambda / classe anônima** → método sintético (`lambda$metodo$0`). `@Inject` no
  método externo não pega o corpo.
- **Método de ponte** em hierarquia genérica → o alvo real pode ser o da superclasse.
- **`default` de interface** → mire a interface.
- **Construtor** → `<init>`, com regras próprias para `TAIL`.
- **`record`** → gera métodos que não estão no fonte.

Para ver a verdade: `javap -p -c` no alvo mostra o bytecode.

## Access widener — o degrau antes do Mixin

Quando o alvo é `private`/`final` e você só precisa de acesso:

```text
accessible  method  net/minecraft/entity/passive/VillagerEntity  initBrain  (L...;)V
```

Declarado no `fabric.mod.json` (`accessWidener`) e num `.accesswidener`. Não é
isento de risco de versão — o descriptor é o mesmo que quebra — mas **não conflita
com outros mods** como um Mixin.

## Verificar que aplicou

```bash
./gradlew build
./gradlew runClient
./gradlew runServer      # se o alvo existe no servidor
```

```text
[ ] compila
[ ] o log de boot NÃO tem aviso de mixin
[ ] o comportamento novo acontece
[ ] o comportamento Vanilla preservado CONTINUA acontecendo   ← o que separa
```

## Mau cheiro

```text
[ ] @Overwrite em método grande — reimplementa Vanilla e congela a versão
[ ] @Redirect em método chamado por muita gente
[ ] regra de negócio no corpo do Mixin
[ ] Mixin em classe de outro mod sem dependência declarada
[ ] require = 0 sem tratar a ausência
[ ] mais de dois ou três Mixins para uma única feature
```

O último é sinal de que a arquitetura está lutando contra o Vanilla em vez de se
encaixar nele. A resposta está na escada de extensão, não em mais um injector.

## Documentar

O Mixin fica no repositório **com o motivo ao lado**: um comentário dizendo por
que o extension point não bastou, e o `templates/mixin-plan.md` guardado.

Sem isso, ninguém vai saber removê-lo quando a API finalmente cobrir o caso — e
ele continua lá, comprando risco de compatibilidade de graça.
