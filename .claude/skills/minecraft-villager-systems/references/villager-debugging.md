# Depurar IA de aldeão

A pergunta que resolve a maioria dos casos:

> **O que o aldeão acha que sabe?**

Comece pelo **estado**, não pelo código. Quase sempre a task está correta e a
memória que ela exige nunca foi escrita.

## A ordem que funciona

```text
1. Qual ACTIVITY está ativa?        deveria ser essa?
2. Quais MEMÓRIAS estão preenchidas? alguma errada ou vencida?
3. Qual SENSOR deveria escrever?     rodou?
4. O GATE de memórias da task passa?
5. Qual TASK está rodando?
6. Qual task está BLOQUEANDO a desejada?
7. WALK_TARGET está posto e sendo MANTIDO?
8. O POI está reivindicado por ele?
```

Seguir esta ordem economiza horas. Começar pelo passo 5 faz você conferir dez
vezes um código correto.

## Sintoma → causa

| Sintoma | Suspeite primeiro |
|---|---|
| **parado, sem fazer nada** | memória nunca escrita, ou gate da task não passa |
| **anda dois blocos e volta** | `getNavigation().startMovingTo` em vez de `WALK_TARGET` |
| **vai e não chega** | alcance de conclusão errado, ou destino inalcançável |
| **a task nunca roda** | Activity errada, ou memória exigida ausente |
| **roda uma vez e para** | `shouldKeepRunning` devolvendo false; memória expirou |
| **oscila entre dois alvos** | alvo recalculado por tick, sem cooldown |
| **ignora o bloco de trabalho** | POI não registrado, ou block state faltando |
| **adota e larga a profissão** | predicado da profissão não bate com o POI |
| **dois no mesmo bloco** | `ticketCount` maior que 1 |
| **para de trabalhar à noite** | correto — é a Schedule |
| **trabalha durante um raid** | a task não perde para RAID/PANIC |
| **a vaga nunca reabre** | `MOB_CONVERSION` não registrado |
| **trabalhadores somem** | ausência tratada como morte |
| **esquece tudo ao reabrir** | memória sem codec, ou estado do mod não salvo |
| **funciona em SP, quebra em MP** | autoridade ou sincronização |
| **lag com vila grande** | busca/pathfinding por tick, sem escalonar |
| **para após atualizar o MC** | mapping/assinatura/semântica mudou |

## Instrumentar

```java
LOGGER.info("[meumod] {} activity={} jobSite={} walkTarget={} carga={}",
        villager.getUuid(), activityAtual, jobSite, walkTarget, carga);
```

```text
✓  log de ESTADO — os valores que explicam a decisão
✗  log de PASSAGEM — "entrei na task"
```

Passagem prova que o código rodou, que quase nunca é a dúvida. Estado mostra
**por que** ele decidiu o que decidiu.

```text
[ ] prefixo do mod → filtrável num log de trinta mods
[ ] NUNCA por tick por aldeão sem limite
[ ] "avisar uma vez" para condições recorrentes
[ ] remover ou baixar para debug antes de publicar
```

Log por tick por aldeão enche o disco **e muda o timing**, escondendo o bug.

## Ferramentas do jogo

```text
F3+B          hitboxes
F3+G          bordas de chunk
/debug start|stop   profiler do tick
```

Alguns ambientes de desenvolvimento mostram o estado do Brain sobre a entidade —
vale verificar se o seu tem, porque é a ferramenta mais direta que existe para
este domínio.

## Isolar

```text
[ ] reproduz com um aldeão só?          → é lógica individual
[ ] só com vários?                       → é coordenação/reserva
[ ] só em vila gerada?                   → depende de POI/estrutura
[ ] só depois de reabrir o mundo?        → persistência
[ ] só em servidor dedicado?             → side/autoridade
[ ] só depois de horas?                  → acúmulo, vazamento, expiração
[ ] some sem outros mods?                → conflito
```

Cada resposta corta o espaço de busca pela metade.

## Gametest como bancada

```java
@GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = "aldeao")
public void oAldeaoVaiAteOAlvo(TestContext context) { ... }
```

Reprodutível, ao contrário de sessão manual.

**Declare a limitação:** o mundo do gametest é **vazio** — sem vila gerada, sem
estruturas, sem POI natural. Comportamento que depende de vila real só é
verificável em sessão de jogo, e isso precisa estar escrito no resultado.

## Antes de dizer que consertou

```text
[ ] reproduzi ANTES da correção
[ ] entendi a CAUSA, não só o sintoma
[ ] cobri TODOS os caminhos da causa
    (morte E conversão · trabalho E descanso · SP E MP)
[ ] escrevi um teste que falhava antes
[ ] os comportamentos Vanilla continuam (dorme, come, socializa)
[ ] verifiquei RODANDO
```

O terceiro é o que evita a correção parcial — a que passa nos testes e falha em
jogo.

Registro em `templates/ai-debug-report.md`.
