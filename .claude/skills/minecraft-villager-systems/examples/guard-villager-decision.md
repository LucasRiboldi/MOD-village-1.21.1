# Exemplo — "isto precisa ser uma profissão?"

**Pedido:**

> "Quero um aldeão guarda, que ataque zumbis e proteja a vila."

O pedido diz "aldeão guarda". A palavra sugere profissão. **A decisão de aceitar
ou recusar essa sugestão é a mais cara deste domínio** — e ela acontece antes de
qualquer linha de código.

---

## A pergunta

> **Isto é identidade nova, ou capacidade nova?**

```text
IDENTIDADE   ele É um tipo diferente de trabalhador — local próprio,
             visual próprio, talvez comércio próprio
CAPACIDADE   ele FAZ algo novo, em certas condições
```

## O que uma profissão custa

`[FATO]` MC 1.21.1 — o record `VillagerProfession`:

```java
VillagerProfession(String id,
                   Predicate<RegistryEntry<PointOfInterestType>> heldWorkstation,
                   Predicate<RegistryEntry<PointOfInterestType>> acquirableWorkstation,
                   ImmutableSet<Item> gatherableItems,
                   ImmutableSet<Block> secondaryJobSites,
                   SoundEvent workSound)
```

Escolher "profissão" compromete você com **sete peças**:

```text
1. BLOCO de trabalho          ← o guarda trabalha em qual bloco?
2. POI                        ← ele reivindica o quê?
3. VILLAGER_PROFESSION
4. TASK                       ← isto ele precisa mesmo
5. SCHEDULE                   ← ele guarda só no horário de trabalho?
6. TRADES                     ← o que um guarda vende?
7. RESOURCES                  ← textura, som, lang
```

Três dessas perguntas não têm resposta boa. **Isso é o sinal.**

## Testando a hipótese "profissão"

| Peça | Faz sentido para um guarda? |
|---|---|
| bloco de trabalho | forçado — inventar um "posto de guarda" só para justificar |
| POI e reivindicação | então **só um** aldeão guarda por posto? |
| Schedule | ele para de guardar à noite? **é justamente quando precisa** |
| trades | o que um guarda vende? |
| um aldeão só | e se você quiser que **todos** reajam a uma ameaça? |

Cinco atritos. A hipótese está errada.

## A hipótese "capacidade"

Reformulando o pedido em termos de camadas:

```text
"o aldeão deve NOTAR zumbis por perto"          → SENSOR
"e LEMBRAR onde estão"                           → MEMÓRIA
"e AGIR quando isso for verdade"                 → TASK
```

Três peças, todas do degrau 2 da escada, nenhuma tocando o Vanilla.

E as respostas ficam naturais:

```text
Qualquer aldeão pode?         sim, ou filtre por outra condição
Precisa de bloco?             não
Precisa reivindicar algo?     não
Funciona à noite?             sim — é uma condição de estado, não de horário
Precisa de trades?            não
```

## A decisão

> `[DECISÃO]` **Capacidade, não profissão.** Um sensor de ameaça, uma memória
> `MemoryModuleType<LivingEntity>` e uma task de combate.

Se depois o design pedir "um aldeão que porta armadura, mora no quartel e vende
escudos", **aí** vira profissão — e a task já estará escrita, reaproveitada.

O caminho errado não é reversível barato: as sete peças escritas e depois
descartadas custam a sessão inteira.

## O desenho resultante

```text
SENSOR de ameaça
  ↓ escreve
MEMÓRIA "hostil próximo"   (com validade curta — a ameaça se move)
  ↓ gate da task
TASK de combate em CORE
  ↓
WALK_TARGET / ataque
```

```text
[ ] a task perde para PANIC?  ← decisão de design:
      um guarda que entra em pânico não é guarda.
      Então ela precisa ganhar de PANIC — e isso é o único ponto delicado.
```

Esse detalhe merece registro: **é a única parte do desenho que compete com o
Vanilla**, e portanto a única que carrega risco de compatibilidade.

## A tabela de decisão, generalizada

| Pedido | Profissão? | Por quê |
|---|---|---|
| "aldeão minerador com bancada própria" | **sim** | bloco, reivindicação, identidade, talvez comércio |
| "aldeão que foge de creepers" | não | sensor + memória + task |
| "o fazendeiro guarda no baú" | não | task |
| "aldeão guarda" | **não** | sem bloco, sem horário, sem comércio |
| "aldeão que coleta madeira para a colônia" | **sim** | tem base, tem reivindicação, tem identidade |
| "todo aldeão foge de raios" | não | task para todos |

O padrão: **profissão quando há lugar, reivindicação e identidade.** Capacidade
quando há condição e ação.

## O que fazer com o pedido literal

O usuário pediu "aldeão guarda". A entrega é a capacidade — e a diferença precisa
ser dita, não escondida:

> Implementei como **capacidade**, não como profissão: qualquer aldeão passa a
> reagir a hostis próximos.
>
> Não virou profissão porque um guarda não tem bloco de trabalho, não deveria
> parar à noite, e não teria o que vender — as três peças de uma profissão que
> não fariam sentido aqui.
>
> Se a intenção era que **só alguns** aldeões guardem, com um posto e visual
> próprios, isso é profissão e é meia hora a mais — a task já está pronta e seria
> reaproveitada. Me diga qual dos dois você quer.

---

## O que este exemplo demonstra

1. **A palavra do pedido não é o desenho.** "Guarda" sugere profissão; o domínio
   diz capacidade.
2. **O teste é procurar atrito.** Se três das sete peças não têm resposta boa, a
   hipótese está errada.
3. **O caminho barato é reversível; o caro não é.** Capacidade vira profissão
   depois, reaproveitando a task. Profissão descartada custa a sessão.
4. **A ambiguidade foi devolvida ao usuário**, com as duas opções e o custo de
   cada uma — em vez de resolvida em silêncio.
