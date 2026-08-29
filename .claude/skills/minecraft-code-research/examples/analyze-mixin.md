# Exemplo — avaliar um Mixin

**Pedido:**

> "Preciso que todo aldeão tenha uma task minha no cérebro. Vou fazer um Mixin em
> `VillagerEntity` que remove as tasks de trabalho do Vanilla e põe as minhas."

A primeira metade do pedido é razoável. A segunda é onde a arquitetura frágil
começa. Este exemplo mostra a avaliação que separa as duas.

---

## Fase 0 — Enquadrar

```text
SISTEMA         Brain do aldeão
COMPORTAMENTO   instalar uma task do mod em todo aldeão
VERSÃO          1.21.1 · Yarn 1.21.1+build.3
OBJETIVO        modificar comportamento
MODO            FEATURE
```

## Antes do Mixin: a escada

Rodando `checklists/before-modifying-vanilla.md`:

| Degrau | Verificação | Resultado |
|---|---|---|
| 1. Sistema Vanilla | O Brain aceita tasks novas? | **sim**, via `setTaskList` |
| 2. Registro | Dá para registrar a task sem tocar na entidade? | não — o Brain é montado por instância |
| 3. Data-driven | IA de aldeão é JSON? | não |
| 4–5. Fabric API | Existe hook para o Brain? | **não** — verificado nos sources de `fabric-entity-events-v1` |
| 6–8. Composição/herança | Dá para estender `VillagerEntity`? | não para o aldeão **existente** do mundo |
| 9. Accessor | Resolve? | não — preciso executar código, não ler campo |
| 10. Inject | Resolve? | **sim** |

> `[DECISÃO]` Mixin é necessário, mas **só para instalar**. A lógica não precisa
> morar nele.

Repare no que a escada já eliminou: não é "Mixin sim ou não", é "Mixin **para
quê**". A resposta muda o tamanho do risco.

## Verificar o alvo

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

unzip -l "$MC_JAR" | grep "VillagerEntity.class"
javap -s -cp "$MC_JAR" net.minecraft.entity.passive.VillagerEntity | grep -A1 -i initBrain
```

> `[FATO]` `initBrain` existe em `VillagerEntity` e recebe `Brain<VillagerEntity>`.
> Sem sobrecarga — o `method = "initBrain"` basta, sem descriptor.

```bash
javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.Brain | grep -i "setTaskList"
```

> `[FATO]` `setTaskList(Activity, int, ImmutableList)` **acrescenta** à lista da
> Activity. Nenhuma task Vanilla é removida.

Este fato desmonta a segunda metade do pedido: **não é preciso remover nada.**

## A parte perigosa do pedido

> "remove as tasks de trabalho do Vanilla"

| Risco | Avaliação |
|---|---|
| Compatibilidade | **HIGH** — qualquer mod de aldeão que dependa das tasks Vanilla quebra |
| Índice de lista | remover por índice assume que ninguém inseriu antes — outro mod pode ter |
| Regressão Vanilla | o aldeão perde comportamentos que o jogador espera (dormir, comer, socializar) |
| Reversibilidade | se a task do mod falhar, o aldeão fica sem nenhuma |

> `[INFERÊNCIA]` Remover não é necessário para o objetivo declarado. Acrescentar
> alcança o mesmo resultado em jogo, com risco muito menor.

## O Mixin recomendado

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

```json
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.villagecolony.fabric.mixin",
  "compatibilityLevel": "JAVA_21",
  "mixins": ["VillagerEntityMixin"],
  "injectors": { "defaultRequire": 1 }
}
```

E do outro lado, a instalação que **não pode derrubar o Vanilla**:

```java
public static void install(Brain<VillagerEntity> brain) {
    try {
        brain.setTaskList(Activity.CORE, PRIORITY, ImmutableList.of(new GoToWorkTargetTask()));
    } catch (RuntimeException failure) {
        LOGGER.warn("Could not install the colony brain task — this villager stays vanilla", failure);
    }
}
```

## Por que este desenho é de baixo risco

| Propriedade | Efeito |
|---|---|
| `@Inject` em `TAIL` | roda uma vez, depois do Vanilla montar o cérebro |
| não cancela | `CallbackInfo` sem `cancellable` |
| não remove task | `setTaskList` acrescenta |
| não assume índice | não toca na lista existente |
| delega | o Mixin tem 3 linhas; a lógica é uma classe normal e testável |
| degrada | exceção capturada → **um aldeão sem a task é um aldeão Vanilla** |

A última linha é o padrão a copiar. Custo de degradar: um aldeão comum. Custo de
propagar exceção de dentro de método Vanilla: possivelmente o tick do servidor.

## Onde a task vive — a decisão não óbvia

O plano inicial era registrar uma Activity própria, `villagecolony:colony_work`,
no nível de `WORK`. Investigando:

> `[FATO]` Quem escolhe a Activity ativa em 1.21.1 é a `Schedule` do aldeão,
> através de uma task Vanilla de CORE.

> `[INFERÊNCIA]` Uma Activity que a Schedule não conhece **nunca seria escolhida**.
> Forçá-la exigiria justamente uma task de CORE chamando `doExclusively` a cada
> tick — mais peça para o mesmo efeito.

> `[DECISÃO]` A task vive em `CORE` e carrega ela mesma as duas condições que a
> Activity daria: só age com destino posto, e só durante o horário de trabalho.
> O resultado em jogo é o mesmo; muda o lugar do registro, para o lado mais barato
> e de menos conflito.

Sobre a prioridade dentro de CORE:

> `[DECISÃO]` Prioridade depois das tasks Vanilla de CORE (água, portas, pânico,
> acordar, sino, incursão). Ficar depois é de propósito: pânico e incursão devem
> decidir primeiro.

## Conclusão

**Recomendação:** fazer o Mixin — mas apenas o `@Inject` em `TAIL` que delega.
**Não** remover tasks Vanilla.

**Classificação de compatibilidade:** LOW.

**Resposta ao usuário:** a primeira metade do plano está certa e é necessária. A
segunda não é: `setTaskList` acrescenta, então acrescentar já resolve, e remover
compraria risco HIGH sem ganho.

**Pendências:**

- [ ] gametest confirmando que a task aparece em aldeão recém-criado
- [ ] gametest confirmando que os comportamentos Vanilla (dormir, comer)
      continuam
- [ ] testar em `runServer`

---

## O que este exemplo demonstra

1. **A pergunta certa não é "Mixin sim ou não", é "Mixin para quê".** Restringir
   o escopo do Mixin derrubou o risco de HIGH para LOW.
2. **Um `javap` desmontou metade do pedido.** Saber que `setTaskList` acrescenta
   tornou a remoção desnecessária.
3. **Degradação é requisito, não detalhe.** "Sem o Mixin, o aldeão é Vanilla" é o
   que torna a falha aceitável.
4. **A decisão sobre CORE vs. Activity nova** só apareceu porque a pesquisa foi
   até a `Schedule`. Parar antes teria produzido uma Activity que nunca rodaria —
   e um dia inteiro depurando.
