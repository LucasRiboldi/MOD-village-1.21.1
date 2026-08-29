# Workflow — Mixin

Modo **SURGERY**. Mixin é integração, não arquitetura. Este workflow existe para
garantir que ele seja a **última** opção considerada e a **menor** possível.

Referência: `references/mixin-development.md`.
Se a decisão ainda não foi tomada, a análise é em
`minecraft-code-research/references/mixin-analysis.md`.

---

## 1. A escada de extensão — antes de tudo

Verifique **nesta ordem** e registre o motivo de cada "não":

```text
 1. Sistema Vanilla já faz isso?
 2. Existe registro que aceita entrada nova?
 3. É data-driven (tag, loot table, recipe, datapack)?
 4. Existe API Fabric?          ← procure nos sources jar, não de memória
 5. Existe evento Fabric?
 6. Resolve por composição?
 7. Existe interface implementável?
 8. Existe método protected extensível?
 —  Access widener basta?
 9. Accessor / Invoker Mixin
10. Inject Mixin
11. Overwrite Mixin
```

**"Não procurei" não preenche nenhuma linha.** Ausência afirmada de memória é
hipótese, e costuma estar errada.

Se a escada parou antes do 9, **este workflow acabou** — implemente pelo degrau
que resolveu.

## 2. Delimitar o escopo do Mixin

A pergunta não é "Mixin sim ou não". É **"Mixin para quê"** — e a resposta muda o
tamanho do risco.

```text
✗ "Mixin no aldeão para controlar o trabalho dele"
✓ "Mixin só para INSTALAR uma task; a lógica fica numa classe normal"
```

Reduza até: o Mixin faz **uma** coisa, em **três** linhas, e **delega**.

## 3. Verificar o alvo

Nunca escreva o alvo de memória.

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

unzip -l "$MC_JAR" | grep "<Classe>.class"
javap -s -cp "$MC_JAR" net.minecraft.<pacote>.<Classe> | grep -A1 "<metodo>"
```

```text
[ ] a classe existe nesta versão
[ ] o método existe
[ ] a assinatura foi copiada da saída do javap
[ ] se há sobrecarga, o descriptor entra no method =
[ ] o alvo não é lambda, classe anônima nem método sintético
```

## 4. Plano

`templates/mixin-plan.md`. Os campos que decidem:

```text
TARGET · METHOD · DESCRIPTOR
INJECTION TYPE · INJECTION POINT
EXPECTED STATE       o que já é verdade quando o código roda
MODIFICATION         o que muda
FAILURE MODE         o que acontece se não aplicar / se lançar
VERSION RISK
COMPATIBILITY RISK
TEST
```

Escolha o **tipo menos invasivo que resolve**:

```text
Accessor → Invoker → Inject → WrapOperation → Modify* → Redirect → Overwrite
```

`@Redirect` e `@Overwrite` são **exclusivos**: o segundo mod que mirar o mesmo
alvo não roda. Se precisa envolver uma chamada, prefira `@WrapOperation`, que
encadeia.

## 5. Escrever

```java
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "initBrain", at = @At("TAIL"))
    private void modid$installTask(Brain<VillagerEntity> brain, CallbackInfo info) {
        MinhaClasseNormal.install(brain);   // delega — a lógica não mora aqui
    }
}
```

```text
[ ] nome do método com prefixo modid$
[ ] a lógica NÃO está no Mixin
[ ] não cancela, não remove comportamento Vanilla (ou está justificado)
[ ] não assume índice de lista
[ ] TAIL se for uma vez; se RETURN, o efeito é idempotente
```

E do outro lado, a degradação:

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new MinhaTask()));
    } catch (RuntimeException failure) {
        LOGGER.warn("Could not install the task — this villager stays vanilla", failure);
    }
}
```

**Nenhuma exceção sua escapa de dentro de método Vanilla.** O custo de degradar é
uma entidade comum; o de propagar pode ser o tick do servidor.

## 6. Registrar

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

`defaultRequire: 1` faz o boot **falhar alto** se o injector não aplicar. É o que
você quer: Mixin que silenciosamente não aplica vira bug fantasma meses depois.

Mixin de cliente vai na lista `client`, nunca na comum.

## 7. Compilar e verificar a aplicação

```bash
./gradlew build
./gradlew runClient
```

```text
[ ] compila
[ ] o log de boot NÃO tem aviso de mixin
[ ] o comportamento novo acontece
[ ] o comportamento Vanilla preservado CONTINUA acontecendo
```

O quarto item é o que separa "funcionou" de "funcionou sem quebrar nada".

```bash
./gradlew runServer      # se o alvo existe no servidor
./gradlew runGametest
```

## 8. Compatibilidade

`checklists/mixin.md` e `checklists/compatibility.md`.

```text
[ ] classificado LOW / MEDIUM / HIGH
[ ] sei quais mods conhecidos miram esta classe
[ ] se HIGH: justificativa escrita E plano de degradação
[ ] conflito conhecido documentado, não escondido
```

## 9. Documentar

O Mixin fica no repositório com o **motivo ao lado**:

```text
[ ] mixin-plan.md preenchido, guardado
[ ] comentário no código dizendo por que o extension point não bastou
[ ] risco de versão registrado
```

Mixin sem motivo documentado é Mixin que ninguém vai saber remover quando a API
finalmente cobrir o caso — e vai continuar lá, comprando risco de graça.

## Sinal de alarme

Se ao longo deste workflow você acumulou **mais de dois ou três Mixins para uma
única feature**, pare. Isso quase sempre indica que a arquitetura está lutando
contra o Vanilla em vez de se encaixar nele — e a resposta está lá atrás, no
passo 1.
