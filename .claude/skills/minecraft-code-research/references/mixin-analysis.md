# Análise de Mixin

Mixin não é mágica e não é arquitetura. É **transformação de bytecode aplicada no
carregamento**, mirando um método concreto de uma versão concreta. Tudo que segue
decorre disso.

Duas consequências que explicam quase todos os problemas:

1. **O alvo tem que existir exatamente como escrito.** Nome, descriptor,
   instrução no injection point. Mudou a versão, pode ter sumido.
2. **Vários mods podem mirar o mesmo alvo.** O que acontece nesse caso depende do
   tipo de injeção — e alguns tipos simplesmente não convivem.

## Anatomia

```java
@Mixin(VillagerEntity.class)                              // alvo
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))       // onde
    private void villagecolony$installColonyTask(         // modid$ evita colisão de nome
            Brain<VillagerEntity> brain, CallbackInfo info) {

        ColonyBrainInitializer.install(brain);            // delega: a lógica não mora aqui
    }
}
```

E a configuração:

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

`defaultRequire: 1` significa **falhe alto se o injector não aplicar**. Isso é o
que você quer: um Mixin que silenciosamente não aplica vira um bug fantasma meses
depois. `require = 0` só se a ausência do alvo for esperada e tratada.

## Tipos e escada de risco

| Tipo | O que faz | Risco | Convive com outros mods? |
|---|---|---|---|
| `@Accessor` | lê/escreve campo | **baixo** | sim |
| `@Invoker` | chama método privado | **baixo** | sim |
| `@Inject` | roda código num ponto | **médio** | geralmente sim |
| `@ModifyArg` / `@ModifyArgs` | troca argumento de uma chamada | **alto** | frágil |
| `@ModifyVariable` | troca variável local | **alto** | frágil |
| `@ModifyConstant` | troca constante | **alto** | frágil |
| `@Redirect` | substitui uma chamada inteira | **alto** | **não** — exclusivo |
| `@WrapOperation` (MixinExtras) | envolve uma chamada, encadeável | médio | sim, e é a alternativa boa ao Redirect |
| `@Overwrite` | substitui o método todo | **muito alto** | **não** |

A escala é orientação, não lei. Um `@Inject` cancelável em método muito chamado
pode ser mais perigoso que um `@Accessor`. Julgue pelo caso, mas **justifique
quando subir**.

**`@Redirect` e `@Overwrite` são exclusivos por construção:** o segundo mod que
tentar o mesmo alvo não roda ou roda sobre estado alterado. Se puder usar
`@WrapOperation`, use — ele encadeia.

## Injection points

```java
@At("HEAD")                                        // entrada
@At("TAIL")                                        // antes do último return
@At("RETURN")                                      // antes de CADA return
@At(value = "INVOKE",  target = "L.../Classe;metodo()V")
@At(value = "FIELD",   target = "L.../Classe;campo:Ltipo;")
@At(value = "CONSTANT", args = "intValue=64")
```

Diferença que morde: **`TAIL` é uma vez, `RETURN` é por return.** Método com
três saídas roda seu código três vezes com `RETURN`. Se o efeito não for
idempotente, isso é um bug.

`INVOKE` e `FIELD` são precisos e, por isso, os mais frágeis entre versões: eles
dependem de a chamada existir naquele ponto. Um refactor interno da Mojang que
não muda assinatura nenhuma quebra o injection point.

## O que investigar antes de escrever

Para todo Mixin, responda — o template está em `templates/mixin-analysis.md`:

```text
TARGET CLASS · TARGET METHOD · DESCRIPTOR
INJECTION TYPE · INJECTION POINT · PRIORITY
CANCELÁVEL? · USA LOCAIS? · SIDE EFFECTS
POR QUE ESTE MIXIN EXISTE?
QUAL COMPORTAMENTO VANILLA É MODIFICADO?
O QUE ACONTECE SE ELE FALHAR?
```

A última é a mais negligenciada e a mais importante. Um Mixin que falha deve
degradar para **comportamento Vanilla**, não para estado inconsistente.

## A regra antes do Mixin

Antes de recomendar Mixin, responda por escrito:

```text
Existe API?             → fabric-analysis.md
Existe evento?          → fabric-analysis.md
Existe registro?        → registry-analysis.md
É data-driven?          → data-driven-analysis.md
Existe composição?
Existe interface implementável?
Existe método protected extensível?
Existe access widener suficiente?   → mappings.md
```

Se existe alternativa e você ainda quer Mixin, **escreva por quê**. Motivos
válidos existem: o extension point não cobre o caso, a API dispara tarde demais,
a alternativa exigiria duplicar um sistema Vanilla inteiro. Motivo inválido: foi
mais rápido.

## Verificar o alvo — sempre

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

# a classe existe?
unzip -l "$MC_JAR" | grep "VillagerEntity.class"

# o método existe, e com qual descriptor?
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 initBrain
```

Havendo sobrecarga, o `method =` **precisa** do descriptor:

```java
@Inject(method = "initBrain(Lnet/minecraft/entity/ai/brain/Brain;)V", at = @At("TAIL"))
```

## Alvos que não existem como você pensa

- **Lambdas e classes anônimas** viram métodos sintéticos (`lambda$metodo$0`).
  `@Inject` no método externo não pega o corpo do lambda.
- **Métodos sintéticos de ponte** aparecem em hierarquias genéricas; o alvo real
  pode ser o da superclasse.
- **Interfaces com `default`** exigem mirar a interface, não o implementador.
- **Construtores** são `<init>`, e `@At("TAIL")` neles tem regras próprias.
- **`record`** gera métodos que não existem no fonte.

Descobrir a verdade: `javap -p -c` no alvo mostra o bytecode real.

## Ler o Mixin de outro mod

Ao analisar um mod externo, os mixins são o mapa do que ele realmente faz ao
Vanilla:

```bash
find . -name "*.mixins.json" -not -path "*/build/*" -exec cat {} \;
grep -rn "@Inject\|@Redirect\|@Overwrite\|@ModifyVariable\|@WrapOperation" src/ | head -40
```

Para cada um, pergunte: **qual comportamento Vanilla isso muda, e o que quebraria
se dois mods fizessem o mesmo?** Um mod com muitos `@Redirect` em classes
centrais é um mod com alto risco de conflito — informação valiosa tanto para
compatibilidade quanto para decidir se ele serve de referência arquitetural.

## Bom cheiro / mau cheiro

**Bom sinal**

- poucos Mixins, em pontos rasos e estáveis
- `@Inject` em `TAIL` sem cancelar, apenas acrescentando
- toda a lógica numa classe normal; o Mixin só delega
- falha tratada: sem o Mixin, o comportamento é o Vanilla
- comentário explicando por que o extension point não bastou

**Mau sinal**

- `@Overwrite` em método grande — reimplementa Vanilla e congela a versão
- `@Redirect` em método chamado por muita gente
- lógica de negócio dentro do corpo do Mixin
- Mixin em classe de outro mod sem dependência declarada
- `require = 0` sem tratar a ausência
- exceção lançada de dentro de método Vanilla — pode derrubar o tick

## Exemplo comentado

```java
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))
    private void villagecolony$installColonyTask(
            Brain<VillagerEntity> brain, CallbackInfo info) {
        ColonyBrainInitializer.install(brain);
    }
}
```

Por que este é de baixo risco:

- `TAIL`, sem cancelar, sem remover task Vanilla — acrescenta ao fim
- não assume índice de lista nem estado prévio
- delega tudo; o Mixin é três linhas e não tem regra de negócio
- do outro lado, `install` captura `RuntimeException` e apenas loga: **um aldeão
  sem a task é um aldeão Vanilla**, que é o estado de antes

Esse último ponto é o padrão a copiar. Nunca deixe exceção sua escapar de dentro
de um método Vanilla: o custo de degradar é um aldeão comum; o custo de propagar
pode ser o tick do servidor.
