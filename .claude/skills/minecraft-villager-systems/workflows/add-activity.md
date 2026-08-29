# Workflow — nova Activity

**Leia isto antes de decidir.** Na maioria dos casos a resposta é: não crie.

---

## 1. O fato que muda tudo

`[FATO]` MC 1.21.1: quem escolhe a Activity ativa é a **`Schedule`**, através de
uma task Vanilla de `CORE` que consulta o horário.

> **Uma Activity que a Schedule não conhece nunca é escolhida.**

Registrar `meumod:trabalho_da_colonia` e esperar que ative **não funciona**. A
task nunca roda, e o bug parece inexplicável — você conferiu o registro, a task,
o gate, e tudo está certo.

Forçar a ativação exigiria uma task de `CORE` chamando `doExclusively` a cada
tick. Mais peça, mais custo, mesmo efeito.

## 2. A alternativa que quase sempre é melhor

**Task em `CORE` carregando ela mesma as condições que a Activity daria:**

```java
@Override
protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
    return ehHorarioDeTrabalho(world, villager)   // a condição que a Activity daria
        && temDestino(villager);                   // a condição de estado
}
```

| | Activity nova | Task em CORE com condições |
|---|---|---|
| Precisa mexer na Schedule | **sim** | não |
| Risco de nunca ativar | **alto** | nenhum |
| Peças | Activity + Schedule + task | task |
| Conflito com outros mods | maior | menor |
| Resultado em jogo | o mesmo | o mesmo |

**O resultado em jogo é idêntico.** O que muda é o lugar do registro — e ele muda
para o lado mais barato e de menos conflito.

## 3. Quando a Activity nova se justifica

Poucos casos, e todos exigem mexer na Schedule:

```text
[ ] o modo é MUTUAMENTE EXCLUSIVO com WORK/REST/MEET de verdade
[ ] várias tasks compartilham exatamente as mesmas condições de entrada e saída
[ ] você VAI mexer na Schedule, deliberadamente
[ ] você verificou que a Schedule ativa a sua Activity
```

Se você não vai mexer na Schedule, **não crie a Activity**.

## 4. Se decidiu criar

`templates/activity-plan.md`:

```text
TRIGGER       o que a ativa — e onde isso é decidido
PRIORITY      em relação a WORK, REST, MEET, PANIC, RAID
MEMORIES      quais são exigidas
BEHAVIORS     quais tasks vivem nela
SENSORS       quais alimentam as memórias
TRANSITION    de onde vem, para onde vai
INTERRUPTION  o que a interrompe
EXIT          o que a encerra
```

### Registrar

```java
public static final Activity MINHA_ACTIVITY =
        Registry.register(Registries.ACTIVITY, Identifier.of(MOD_ID, "minha"), new Activity("minha"));
```

Confirme a forma na sua versão: `javap -cp "$MC_JAR" net.minecraft.entity.ai.brain.Activity`

### E a Schedule

Sem isto, o resto é decorativo:

```text
[ ] a Schedule do aldeão precisa conhecer a Activity
[ ] em qual janela de tempo ela ativa
[ ] o que ela DESLOCA (WORK? IDLE?)
[ ] o que acontece com o comportamento Vanilla nessa janela
```

`[FATO]` MC 1.21.1: `Schedule` traz `EMPTY`, `SIMPLE`, `VILLAGER_BABY`,
`VILLAGER_DEFAULT` e um `ScheduleBuilder`.

**Mexer na Schedule Vanilla é invasivo** — você está retirando tempo de
comportamentos que o jogador espera. Isso é risco de compatibilidade MEDIUM ou
HIGH, e precisa de justificativa escrita.

## 5. Prioridade em relação ao Vanilla

Sua Activity deve **perder** para:

```text
PANIC · PRE_RAID · RAID · HIDE
```

Um aldeão que continua no seu modo durante uma incursão é um aldeão que morre —
e um sistema que o jogador vai odiar.

## 6. Testar

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
```

```text
[ ] a Activity É ATIVADA                        ← o teste que quase sempre falha
[ ] as tasks dela rodam
[ ] ela desativa quando deveria
[ ] os comportamentos Vanilla que ela desloca voltam depois
[ ] em pânico, ela cede
[ ] durante raid, ela cede
[ ] o aldeão ainda dorme, come e socializa
```

**Comece pelo primeiro.** Se a Activity não ativa, o resto não importa — e é o
resultado mais provável se a Schedule não a conhece.

## 7. Se não ativar

```text
1. a Schedule conhece a Activity?        ← causa mais provável
2. a janela de tempo está certa?
3. as memórias exigidas estão presentes?
4. outra Activity está ganhando?
```

Ver `workflows/villager-debugging.md`.

## Fechamento

`checklists/behavior-activity.md`.

E a pergunta honesta no relato: **isto precisava mesmo ser uma Activity?** Se a
resposta virar "não" durante o trabalho, voltar para uma task em CORE é ganho,
não retrabalho.
