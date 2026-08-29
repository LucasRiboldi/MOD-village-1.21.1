# Relatório de depuração de IA — <título curto>

> Preencha **antes** de tocar em código. Separar observação de interpretação é
> onde a depuração ganha ou perde o dia.

**Data:** AAAA-MM-DD · **Status:** aberto / investigando / corrigido

## Sintoma

<Em linguagem de jogo. **Observação, não interpretação.**>

```text
✓  "o aldeão fica parado ao lado do baú, girando"
✗  "a memória não está sendo escrita"
```

## Comportamento esperado

## Comportamento real

## Ambiente

| | |
|---|---|
| Minecraft / Mappings | |
| Fabric Loader / API | |
| Versão do mod | |
| Singleplayer ou dedicado | |
| Mundo novo ou save antigo | |
| Nº de aldeões | |
| Outros mods | |
| Reproduz sempre / às vezes / uma vez | |

## Reprodução

1.
2.
3.

> **Não corrija o que você não conseguiu reproduzir.** Sem reprodução não há como
> saber se consertou.

---

## Estado observado

> **A pergunta central: o que o aldeão acha que sabe?**

| | Observado | Esperado |
|---|---|---|
| Profissão | | |
| Activity ativa | | |
| `JOB_SITE` | | |
| `HOME` | | |
| `WALK_TARGET` | | |
| memórias do mod | | |
| task rodando | | |
| POI reivindicado | | |

## A ordem de investigação

```text
[ ] 1. Qual Activity está ativa? deveria ser essa?
[ ] 2. Quais memórias estão preenchidas? alguma vencida?
[ ] 3. Qual sensor deveria escrever? rodou?
[ ] 4. O gate de memórias da task passa?
[ ] 5. Qual task está rodando?
[ ] 6. Qual task está bloqueando a desejada?
[ ] 7. WALK_TARGET está posto e sendo MANTIDO?
[ ] 8. O POI está reivindicado por ele?
```

> Quase sempre a task está correta e a memória que ela exige nunca foi escrita.

## Isolamento

| Condição | O bug some? | Conclusão |
|---|---|---|
| um aldeão só | | lógica individual |
| sem outros mods | | conflito |
| mundo novo | | persistência |
| singleplayer | | side/autoridade |
| em vila gerada | | POI/estrutura |

## Logs

```text
<trecho — a PRIMEIRA anomalia, não a última>
```

```text
[ ] há aviso de Mixin no log de BOOT?
```

---

## Causa raiz

> Em termos de **mecanismo**, não de sintoma.

```text
✗  "a memória estava errada"
✓  "o sensor só escreve JOB_SITE com o chunk carregado, e a task assume que ela
    sempre existe"
```

<…>

### Por quantos caminhos ela se manifesta?

| Caminho | Coberto pela correção |
|---|---|
| morte | |
| **conversão** | |
| singleplayer | |
| multiplayer | |
| chunk carregado / descarregado | |

> A pergunta que evita a correção parcial — a que passa nos testes e falha em
> jogo.

## Correção

<A menor mudança que resolve a causa.>

```text
[ ] resolve a CAUSA, não o sintoma
[ ] cobre TODOS os caminhos
[ ] não mistura refatoração nem feature nova
```

## Teste de regressão

```text
[ ] escrevi um teste que FALHAVA antes e passa depois
[ ] o caso reportado não acontece mais
[ ] o aldeão ainda dorme, come e socializa
[ ] cede em PANIC e RAID
[ ] verificado RODANDO
```

**Limitação do gametest:** o mundo de teste é vazio — sem vila gerada, sem POI
natural. <O que precisou de sessão de jogo.>

## Conhecimento gerado

| Onde registrar | O quê |
|---|---|
| `docs/research/` | descoberta sobre o Vanilla |
| ADR | se a causa era arquitetural |

> Bug corrigido sem causa registrada volta — e a próxima pessoa refaz a
> investigação inteira.
