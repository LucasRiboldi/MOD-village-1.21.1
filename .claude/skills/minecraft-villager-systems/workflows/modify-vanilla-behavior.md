# Workflow — alterar comportamento Vanilla do aldeão

Cobre "quero mudar como o aldeão trabalha / comercia / dorme / se reproduz /
fofoca". Modo **SURGERY**: o comportamento existe, o jogador o conhece, e mexer
nele tem custo.

---

## 1. Qual sistema, exatamente?

```text
[ ] trabalho          → references/work-and-schedules.md
[ ] comércio          → references/trading.md
[ ] horário           → references/work-and-schedules.md
[ ] reprodução        → references/breeding-and-food.md
[ ] gossip/reputação  → references/gossip-and-reputation.md
[ ] vila/raid         → references/village-and-raids.md
[ ] movimento         → references/pathfinding-and-movement.md
```

## 2. Entender antes de mudar

`workflows/analyze-villager-behavior.md`. **Não pule.**

Você precisa da cadeia completa antes de tocar em qualquer coisa:

```text
GATILHO → SENSOR → MEMÓRIA → ACTIVITY → TASK → AÇÃO → LIMPEZA
```

Se algum elo estiver em `[HIPÓTESE]`, pare e acione `minecraft-code-research`.
Mudar um sistema que você não mapeou é como o patch nasce torto.

## 3. Acrescentar antes de substituir

A pergunta que decide o custo:

```text
Preciso IMPEDIR o comportamento Vanilla, ou ACRESCENTAR ao lado dele?
```

| Objetivo | Caminho |
|---|---|
| ele faz **algo a mais** | task nova acrescentada — degrau 2+6 |
| ele reconhece **outro bloco** | POI + profissão — degrau 2 |
| ele vende **outra coisa** | trades — sistema separado |
| ele recolhe **outro item** | `gatherableItems` da profissão |
| condição depende de bloco/item | **tag** de datapack — degrau 3 |
| ele **não deve mais** fazer X | Mixin — degrau 9+, justificado |

**Só a última linha exige tocar o Vanilla.** As outras convivem.

## 4. A confusão mais comum

```text
✗  "quero mudar o que o fazendeiro vende"  →  mexer na profissão
✓  trades são um sistema SEPARADO
```

`[FATO]` MC 1.21.1: o record `VillagerProfession` contém `id`, os dois predicados
de workstation, `gatherableItems`, `secondaryJobSites` e `workSound`. **Não
contém trades, tasks nem schedule.**

Antes de mexer na profissão, confirme que o que você quer mudar está mesmo lá.

## 5. Se for mesmo preciso impedir algo Vanilla

Modo SURGERY. `fabric-development/workflows/mixin-workflow.md`.

```text
[ ] a escada de extensão foi percorrida, com o motivo de cada "não" escrito
[ ] o alvo foi verificado com javap NESTA versão
[ ] é o tipo MENOS invasivo que resolve
[ ] NÃO removo task Vanilla (ou está justificado, com risco HIGH declarado)
[ ] degrada: sem o Mixin, o aldeão é Vanilla
```

E o custo, declarado:

```text
[ ] qual comportamento o jogador perde?
[ ] isso é intencional e desejável?
[ ] outros mods de aldeão dependem disso?
```

## 6. Alterar a Schedule

Mexer na Schedule Vanilla é **retirar tempo** de comportamentos que o jogador
espera.

```text
[ ] qual janela eu ocupo?
[ ] o que ela deslocava?
[ ] o aldeão ainda dorme, come e socializa?
[ ] ele consegue voltar para casa antes da noite?
```

Risco de compatibilidade MEDIUM ou HIGH. Precisa de justificativa escrita.

## 7. Preservar o que não é o alvo

O teste que separa uma alteração cirúrgica de uma destrutiva:

```text
[ ] ele ainda dorme
[ ] ele ainda come
[ ] ele ainda socializa (MEET)
[ ] ele ainda foge em PANIC
[ ] ele ainda se esconde em RAID
[ ] ele ainda comercia
[ ] ele ainda se reproduz
[ ] golens ainda nascem
```

Alteração que quebra três destes não era cirúrgica — era substituição parcial, e
o jogador vai perceber.

## 8. Mudança incremental

```text
menor mudança possível → build → testar → observar em jogo → seguir
```

Nunca altere trabalho + trades + schedule no mesmo passo. Se algo quebrar, você
não saberá qual.

## 9. Testar

```bash
./gradlew build
./gradlew runGametest
./gradlew runClient
./gradlew runServer
```

```text
[ ] o comportamento novo acontece
[ ] o comportamento Vanilla preservado CONTINUA (a lista do passo 7)
[ ] em pânico e em raid, o Vanilla assume
[ ] fechar e reabrir o mundo mantém o estado
[ ] servidor dedicado
[ ] com 20 aldeões, o TPS aguenta
```

E `workflows/villager-regression.md` — alteração de IA quebra coisas não
relacionadas com frequência surpreendente.

## 10. Documentar

```text
[ ] o que mudou, em linguagem de jogo
[ ] o que foi PRESERVADO
[ ] o comportamento Vanilla que o jogador perde, se houver
[ ] risco de compatibilidade classificado
[ ] conflitos conhecidos com mods de aldeão
```

## Fechamento

`checklists/villager-feature.md`.

A pergunta honesta no relato: **eu impedi algo do Vanilla, ou só acrescentei?**
Se impediu, diga o quê — o jogador que instalar o mod merece saber.
