# Plano de Activity — <nome>

> **Leia `workflows/add-activity.md` antes de preencher.** Na maioria dos casos a
> resposta é: não crie.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## O teste de necessidade

`[FATO]` MC 1.21.1: quem escolhe a Activity ativa é a **`Schedule`**. Uma Activity
que ela não conhece **nunca é escolhida**.

```text
[ ] o modo é MUTUAMENTE EXCLUSIVO com WORK/REST/MEET de verdade?
[ ] várias tasks compartilham exatamente as mesmas condições de entrada e saída?
[ ] eu VOU mexer na Schedule, deliberadamente?
[ ] eu verifiquei que a Schedule vai ativar a minha Activity?
```

**Se o terceiro for "não", não crie a Activity.**

## A alternativa

**Task em `CORE` carregando as condições que a Activity daria:**

```java
protected boolean shouldRun(ServerWorld world, VillagerEntity villager) {
    return ehHorarioDeTrabalho(world, villager) && temDestino(villager);
}
```

| | Activity nova | Task em CORE |
|---|---|---|
| Mexe na Schedule | **sim** | não |
| Risco de nunca ativar | **alto** | nenhum |
| Peças | 3 | 1 |
| Resultado em jogo | **o mesmo** | **o mesmo** |

**Decisão:** <Activity nova / task em CORE>
**Justificativa:**

---

> Preencha o resto **apenas** se decidiu pela Activity nova.

## Identificação

| | |
|---|---|
| Nome | `meumod:<path>` |

## Trigger

<O que a ativa — e **onde isso é decidido**. Se a resposta não menciona a
Schedule, a Activity não vai ativar.>

## Schedule

```text
[ ] a Schedule do aldeão conhece esta Activity
[ ] janela de tempo: <de … até …>
[ ] o que ela DESLOCA: <WORK? IDLE? MEET?>
[ ] o comportamento Vanilla dessa janela: <o que o jogador perde>
```

`[FATO]` MC 1.21.1: `Schedule` traz `EMPTY`, `SIMPLE`, `VILLAGER_BABY`,
`VILLAGER_DEFAULT` e um `ScheduleBuilder`.

> **Mexer na Schedule Vanilla é invasivo.** Risco de compatibilidade MEDIUM ou
> HIGH, com justificativa escrita.

## Prioridade

**Deve perder para:** `PANIC` · `PRE_RAID` · `RAID` · `HIDE`

<Um aldeão que continua no seu modo durante uma incursão é um aldeão que morre.>

**Deve ganhar de:** <…>

## Memórias

| Memória | Exigida para entrar | Para permanecer |
|---|---|---|

## Sensores

| Sensor | Alimenta qual memória |
|---|---|

## Tasks

| Task | Prioridade dentro da Activity |
|---|---|

## Transições

| De | Para | Condição |
|---|---|---|
| | esta Activity | |
| esta Activity | | |

## Interrupção

<O que a interrompe no meio.>

## Saída

<O que a encerra normalmente, e o que acontece depois.>

## Teste

> **Comece pelo primeiro.** Se ela não ativa, o resto não importa — e é o
> resultado mais provável quando a Schedule não a conhece.

```text
[ ] a Activity É ATIVADA                    ← o teste que quase sempre falha
[ ] as tasks dela rodam
[ ] ela desativa quando deveria
[ ] o comportamento Vanilla que ela desloca VOLTA depois
[ ] em PANIC ela cede
[ ] durante RAID ela cede
[ ] o aldeão ainda dorme, come e socializa
[ ] fechar e reabrir o mundo não deixa o aldeão preso nela
```

## Se não ativar

```text
1. a Schedule conhece a Activity?     ← causa mais provável
2. a janela de tempo está certa?
3. as memórias exigidas estão presentes?
4. outra Activity está ganhando?
```

## Revisão honesta

**Depois de implementar: isto precisava mesmo ser uma Activity?**

<Voltar para uma task em CORE é ganho, não retrabalho.>
