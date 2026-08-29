# Workflow — depurar IA de aldeão

Sintoma típico: "o aldeão está parado", "vai para o lugar errado", "ignora o
bloco", "não faz nada".

**Comece pelo estado, não pelo código.**

---

## 1. Observar e descrever

Em linguagem de jogo, separando observação de interpretação:

```text
✓  "o aldeão fica parado ao lado do baú, girando"
✗  "a memória não está sendo escrita"
```

A segunda já é hipótese. Escrevê-la como observação é como a depuração sai do
trilho.

```text
[ ] o que ele faz
[ ] o que deveria fazer
[ ] quando começa
[ ] acontece com todos os aldeões, ou com um?
```

## 2. Reproduzir

**Não corrija o que você não conseguiu reproduzir.**

```text
[ ] passos mínimos
[ ] mundo novo ou save antigo?
[ ] singleplayer ou servidor dedicado?
[ ] quantos aldeões?
[ ] outros mods?
[ ] acontece sempre, ou às vezes?
```

## 3. A ordem que funciona

```text
1. Qual ACTIVITY está ativa?         deveria ser essa?
2. Quais MEMÓRIAS estão preenchidas?  alguma errada ou vencida?
3. Qual SENSOR deveria escrever?      rodou?
4. O GATE de memórias da task passa?
5. Qual TASK está rodando?
6. Qual task está BLOQUEANDO a desejada?
7. WALK_TARGET está posto e sendo MANTIDO?
8. O POI está reivindicado por ele?
```

**Quase sempre a task está correta e a memória que ela exige nunca foi escrita.**
Começar pelo passo 5 faz você conferir dez vezes um código certo.

## 4. Instrumentar — estado, não passagem

```java
LOGGER.info("[meumod] {} activity={} jobSite={} walkTarget={} alvo={}",
        villager.getUuid(), activityAtual, jobSite, walkTarget, alvo);
```

```text
[ ] log de ESTADO, não de passagem
[ ] prefixo do mod
[ ] NUNCA por tick por aldeão sem limite
[ ] remover ou baixar para debug depois
```

Log por tick por aldeão enche o disco **e muda o timing**, escondendo o bug.

## 5. Triagem por sintoma

| Sintoma | Suspeite primeiro |
|---|---|
| parado, sem fazer nada | memória nunca escrita, ou gate não passa |
| anda dois blocos e volta | `getNavigation()` em vez de `WALK_TARGET` |
| vai e não chega | alcance de conclusão, ou destino inalcançável |
| a task nunca roda | Activity errada, ou memória ausente |
| roda uma vez e para | `shouldKeepRunning` false; memória expirou |
| oscila entre alvos | recalculado por tick, sem cooldown |
| ignora o bloco | POI não registrado, ou block state faltando |
| adota e larga a profissão | predicado da profissão não bate |
| dois no mesmo bloco | `ticketCount` > 1 |
| para à noite | **correto** — é a Schedule |
| trabalha durante raid | a task não perde para RAID |
| a vaga nunca reabre | `MOB_CONVERSION` não registrado |
| trabalhadores somem | ausência tratada como morte |
| esquece ao reabrir | memória sem codec, ou estado não salvo |
| SP funciona, MP quebra | autoridade ou sincronização |
| lag com vila grande | busca/pathfinding por tick |
| parou após atualizar o MC | mapping/semântica mudou |

## 6. Isolar

```text
[ ] com um aldeão só?      → lógica individual
[ ] só com vários?          → coordenação/reserva
[ ] só em vila gerada?      → POI/estrutura
[ ] só após reabrir?        → persistência
[ ] só em dedicado?         → side/autoridade
[ ] só após horas?          → acúmulo, vazamento, expiração
[ ] some sem outros mods?   → conflito
```

Cada resposta corta o espaço pela metade.

## 7. Causa raiz

Em termos de **mecanismo**:

```text
✗  "a memória estava errada"
✓  "o sensor só escreve JOB_SITE quando o chunk está carregado, e a task
    assume que ela sempre existe"
```

A segunda diz o que consertar **e** o que mais pode estar quebrado pela mesma
razão.

### Por quantos caminhos a causa se manifesta?

A pergunta que evita a correção parcial:

```text
[ ] morte E conversão?
[ ] trabalho E descanso?
[ ] singleplayer E multiplayer?
[ ] chunk carregado E descarregado?
```

Cobrir um só produz uma correção que passa nos testes e falha em jogo.

## 8. Corrigir e testar

```bash
./gradlew build && ./gradlew runGametest
```

```text
[ ] teste que FALHAVA antes e passa depois
[ ] o caso reportado não acontece mais
[ ] todos os caminhos da causa cobertos
[ ] os comportamentos Vanilla continuam (dorme, come, socializa)
[ ] verificado RODANDO
```

**Declare a limitação do gametest:** o mundo de teste é vazio — sem vila gerada,
sem POI natural. O que depende de vila real só se verifica em sessão de jogo.

## 9. Registrar

`templates/ai-debug-report.md`.

```text
[ ] causa raiz em termos de mecanismo
[ ] descoberta sobre o Vanilla → docs/research/
[ ] se a causa era arquitetural → ADR
```

Bug corrigido sem causa registrada volta, e a próxima pessoa refaz a investigação
inteira.

## Fechamento

Relate: **o que era, por que acontecia, o que mudou, o que foi verificado
rodando.** Se algum caminho ficou descoberto, diga qual.
