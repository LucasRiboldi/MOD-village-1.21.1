\# Varredura do plano contra o código — 2026-09-11



\*\*Pedido do autor em 2026-09-11, e ele veio antes de tudo o que sobrou.\*\*



No ciclo de 09-11 três itens do \[`Plano-de-Correcao.md`](Plano-de-Correcao.md)

caíram por leitura: o `furniture()` que não estava morto, a asserção

defensiva no `assign()` que quebraria a contratação, e a reserva de tora

por espécie que refaria a discordância de 09-10. Um quarto — o P1.11 —

devolveu-se maior do que entrou.



\*\*Os quatro têm a mesma forma.\*\* O plano foi escrito a partir do `TODO.md`,

e o `TODO.md` guarda pendências desde agosto. Algumas delas pararam de ser

verdade sem que ninguém as relesse — quatro linhas daquela lista já tinham

sido derrubadas por leitura antes, e o padrão se repetiu aqui.



\---



\## O que a varredura faz, por item ainda aberto



1\. Achar no código o que o item afirma — a constante, o método, a linha.

2\. Conferir se a afirmação ainda vale \*\*hoje\*\*, e não quando foi escrita.

3\. Marcar o item como `confirmado`, `vencido` ou `maior do que parece`,

&#x20;  com a evidência ao lado.



\*\*O que ela não é:\*\* não é refazer o plano. Os itens confirmados seguem na

ordem em que estão; o que muda é parar de descobrir a vencidura um a um,

no meio da implementação.



\---



\## O placar final — 2026-09-11



Somando P0, P1, P2 e P3:



| | confirmados e entregues | vencidos |

|---|---|---|

| \*\*P0\*\* | P0.3, P0.5 | P0.2 (premissa), P0.4 |

| \*\*P1\*\* | P1.5, P1.11, P1.13 | P1.1, P1.2, P1.4, P1.6, P1.7, P1.8 |

| \*\*P2\*\* | P2.1 (instrumentado) | — |

| \*\*P3\*\* | — | P3.1, P3.2 |



\*\*Doze itens vencidos contra sete entregues.\*\*



Não é um plano ruim: é um plano escrito a partir de sintomas, e sintoma de

log envelhece mais devagar do que o código que o produz. Três dos doze

foram derrubados por decisões tomadas \*\*depois\*\* de o plano ser escrito:



\- A escada do `TreeMarks` em 09-09 (que fechou o P0.4 antes de o plano

&#x20; pedir).

\- A ADR-010 em 09-02 (que fechou o P1.2 e reabriu o P1.4).

\- A divisão do `MANUFACTURER` em 09-10 (que fez o P3.2 apontar para uma

&#x20; profissão inexistente).



E um deles, o P3.2, \*\*pedia para desfazer uma decisão de 09-10 e apagar o

teste que a prova.\*\*



\---



\## O que a varredura devolveu, item por item



\### P0



| item | veredito | a evidência |

|---|---|---|

| P0.2 | ❌ \*\*vencido na premissa\*\* | os dois números da linha são \*baús com conteúdo\* e \*baús lidos\*; o sufixo `(N unreachable)` não aparece em nenhuma das medições citadas |

| P0.3 | ✅ \*\*confirmado, ruptura achada\*\* | o baú da boca da mina não é `WorkerStorage` de ninguém, e as três contagens da colônia só percorrem `WorkerStorage` |

| P0.4 | ❌ \*\*vencido\*\* | `memoryFor` dá 6.000→12.000→24.000→48.000 desde 09-09, e o `TODO.md` diz que funciona |

| P0.5 | ✅ \*\*confirmado e entregue\*\* | `MinerHaul.deposit` descartava o que não coubesse, com a colônia tendo espaço |



\### P1



| item | veredito | a evidência |

|---|---|---|

| P1.1 | ❌ \*\*vencido, lido cláusula a cláusula\*\* | as sete profissões passam por `WorkerStrikes.gaveUp`; o descanso tem `Worker.rest`; a Regra 28 virou diagnóstico em 08-21 |

| P1.2 | ❌ \*\*vencido\*\* | `Worker.REST\_CYCLES = 4`, e o E43 está citado nominalmente ao lado |

| P1.4 | ❌ \*\*vencido\*\* | as três cláusulas estão cumpridas; `shortenStallLimitTo` existe e é o gancho que o item pedia |

| P1.7 | ❌ \*\*refaria o defeito de 09-10\*\* | `INTERCHANGEABLE\_IN\_THE\_WALL` traz `WOOD` e `PLANKS` — separar por espécie recria a discordância entre conta e construtor |

| P1.8 | ❌ \*\*o campo não está morto\*\* | `furniture()` é o primeiro critério de ordenação da obra em `StructureBlueprintReader` |



\### P2



| item | veredito | a evidência |

|---|---|---|

| P2.1 | ✅ \*\*instrumentado\*\* | a linha nova reparte por fase; "persistence", que o item lista, não roda no ciclo |



\### P3



| item | veredito | a evidência |

|---|---|---|

| P3.1 | ❌ \*\*não há o que extrair\*\* | nenhuma das classes que o item nomeia existe; o mod não registra comando nenhum |

| P3.2 | ❌ \*\*contradiz decisão posterior\*\* | o `MANUFACTURER` foi dividido em 09-10; as duas metades já têm teste |



\---



\## A varredura se pagou, e a prova é o P0.2



O P0.2 sozinho era o item nº 2 do plano e carregava \*\*cinco frentes\*\*:



\- `ColonyChestScanReport`

\- `ColonyChestCache` com invalidação por evento

\- `CHESTS\_PER\_TICK = 4`

\- `MAX\_STALENESS\_TICKS`

\- log por ciclo



\*\*Nenhuma delas tinha defeito que a justificasse.\*\* Implementá-las teria

custado dias e mexido no caminho mais quente do ciclo da colônia para

corrigir uma \*\*leitura errada de log\*\*.



O que sobrou foi só a frase — e ela foi consertada em `ChestSurvey.coverage()`,

com cinco casos. Um dia de trabalho contra os dias que teriam sido gastos

nas cinco frentes.



\---



\## O padrão que a varredura confirma



Os dois vencidos de P0 (P0.2 e P0.4) foram escritos a partir de sintomas

de sessão que o \*\*código já tinha respondido\*\*:



\- O \*\*P0.4\*\* em 09-09, dois dias antes de o plano ser escrito.

\- O \*\*P0.2\*\* nunca foi verdade.



\*\*A lição não é sobre estes dois itens:\*\* é que \*\*sintoma de log envelhece

mais devagar do que o código que o produz\*\*, e uma lista feita de sintomas

precisa ser relida contra o código antes de virar trabalho.



\---



\## Como repetir esta varredura



Quando o autor escrever um plano novo, ou quando este for refeito:



```text

1\. Extrair cada item numerado (P0.1, P0.2, ...).

2\. Para cada item, achar no código o símbolo, a constante ou o método

&#x20;  que ele nomeia.

3\. Conferir se o comportamento que o item descreve ainda existe.

4\. Marcar:

&#x20;    ✅ confirmado        — o item ainda vale, a evidência está ao lado

&#x20;    ❌ vencido          — o item já foi resolvido, ou nunca foi verdade

&#x20;    ⚠️ maior do que      — o item é verdadeiro mas subestima o problema

&#x20;       parece

5\. Registrar o placar. Se mais de um terço estiver vencido, a origem do

&#x20;  plano precisa ser revista — ele foi escrito a partir de sintomas, e

&#x20;  sintomas envelhecem.

