# Checklist — comportamento e Activity

## A task

```text
[ ] estende MultiTickTask (ou o equivalente da versão)
[ ] o GATE de memórias está no construtor
[ ] MAX_RUN_TIME definido
[ ] shouldRun e shouldKeepRunning são coerentes
[ ] a lógica pesada NÃO está em shouldRun (ele é avaliado com frequência)
```

`[FATO]` MC 1.21.1: `tick`, `tryStarting` e `stop` são `final` — o Brain dirige o
ciclo. Você sobrescreve `shouldRun` / `run` / `shouldKeepRunning` /
`keepRunning` / `finishRunning`.

## Instalação

```text
[ ] setTaskList ACRESCENTA — nenhuma task Vanilla removida
[ ] prioridade DEPOIS das tasks Vanilla de CORE
[ ] não assumo índice de lista
[ ] exceção capturada — sem a task, o aldeão é Vanilla
[ ] instalada pelo Mixin em initBrain que JÁ EXISTE
```

> Se você está criando o **segundo** Mixin em `VillagerEntity`, pare e reveja o
> desenho.

## Movimento

```text
[ ] uso WALK_TARGET, não getNavigation().startMovingTo
[ ] a memória é MANTIDA em keepRunning enquanto o destino valer
[ ] alcance de conclusão sensato (0 faz ele nunca "chegar")
[ ] velocidade coerente com o comportamento (0.5f = passo de trabalho)
[ ] LOOK_TARGET considerado
```

## Cede ao Vanilla

```text
[ ] perde para PANIC
[ ] perde para PRE_RAID e RAID
[ ] respeita a Schedule (não recria horário com getTimeOfDay)
[ ] o aldeão ainda dorme, come e socializa
```

## Falha

```text
[ ] alvo sumiu           → esquecer, escolher outro
[ ] caminho falhou       → desistir após N tentativas
[ ] chunk descarregado   → pular (caso NORMAL)
[ ] recurso indisponível → cooldown
[ ] timeout              → MAX_RUN_TIME encerra
[ ] ao desistir: reserva liberada, memória limpa
```

> **Retry infinito é bug.** `[FATO]` o Vanilla tem
> `CANT_REACH_WALK_TARGET_SINCE` justamente por isso.

## Performance

```text
[ ] shouldRun é barato
[ ] nenhuma varredura dentro da task (isso é trabalho de sensor)
[ ] pathfinding não recalcula por tick
[ ] escalonado entre aldeões, se periódico
[ ] medido com 10 e 50 aldeões
```

---

## Se criou uma Activity

> **Leia `workflows/add-activity.md` antes.** Na maioria dos casos: não crie.

```text
[ ] a Schedule do aldeão CONHECE esta Activity
[ ] a janela de tempo está definida
[ ] sei o que ela desloca, e o jogador perde o quê
[ ] ela perde para PANIC, PRE_RAID, RAID e HIDE
[ ] transições de entrada e saída definidas
```

`[FATO]` Quem escolhe a Activity é a `Schedule`. **Uma Activity que ela não
conhece nunca é escolhida** — a task nunca roda e o bug parece inexplicável.

```text
[ ] TESTEI que a Activity É ATIVADA          ← o teste que quase sempre falha
```

Se não ativa:

```text
1. a Schedule conhece a Activity?   ← causa mais provável
2. a janela está certa?
3. as memórias exigidas estão presentes?
4. outra Activity está ganhando?
```

## Teste

```text
[ ] a task roda quando deveria
[ ] NÃO roda quando não deveria
[ ] termina corretamente
[ ] o timeout funciona
[ ] cada caso de falha se comporta como planejado
[ ] o ciclo do dia Vanilla continua
[ ] cede em PANIC e RAID
[ ] gametest passa
[ ] fechar e reabrir o mundo não deixa o aldeão preso
```

## Revisão

**A Activity precisava mesmo existir?** Voltar para uma task em CORE é ganho, não
retrabalho.
