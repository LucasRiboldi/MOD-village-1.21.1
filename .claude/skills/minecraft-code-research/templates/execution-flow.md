# Fluxo de execução — <feature>

> Traçado de um comportamento, do gatilho ao efeito. **Siga as chamadas** — não
> liste métodos soltos. Guarde em `docs/research/systems/`.

**Minecraft:** <versão> · **Mappings:** <…> · **Data:** AAAA-MM-DD

## Pergunta

<O que este traçado responde.>

## Cadeia

```text
TRIGGER
   ↓
EVENT / TICK / INTERACTION
   ↓
ENTRY METHOD
   ↓
VALIDATION
   ↓
DECISION
   ↓
ACTION
   ↓
STATE CHANGE
   ↓
SYNC
   ↓
PERSISTENCE
```

## Passo a passo

| # | Etapa | Classe · método | Side | Etiqueta |
|---|---|---|---|---|
| 1 | TRIGGER | | server/client | `[FATO]` |
| 2 | ENTRY | | | |
| 3 | VALIDATION | | | |
| 4 | DECISION | | | |
| 5 | ACTION | | | |
| 6 | STATE CHANGE | | | |
| 7 | SYNC | | | |
| 8 | PERSISTENCE | | | |

> Marque cada elo. Elo em `[HIPÓTESE]` é onde o próximo esforço deve ir — `javap`
> se for assinatura, experimento se for ordem ou estado.

## Detalhamento dos elos incertos

### Elo <n> — <etapa>

**O que sei:** `[FATO]` <…>
**O que suponho:** `[HIPÓTESE]` <…>
**Como confirmar:** `[VALIDAÇÃO NECESSÁRIA]` <comando ou experimento>

## Frequência

| Etapa | Com que frequência roda | Multiplicado por |
|---|---|---|
| | por tick / por evento / sob demanda | nº de entidades / jogadores |

> Ver `references/performance-analysis.md`. Custo unitário importa pouco;
> frequência × população importa muito.

## Onde interceptar

| Ponto da cadeia | Mecanismo disponível | Degrau |
|---|---|---|
| | evento Fabric / registro / tag / protected / mixin | 1–11 |

<Qual é o ponto mais barato que ainda alcança o efeito desejado?>

## Falhas possíveis

| Etapa | O que pode dar errado | O que acontece hoje |
|---|---|---|
| | alvo sumiu / chunk descarregado / estado mudou | |

## Conclusão

<Resposta à pergunta. E, se for o caso, onde encaixar a mudança.>
