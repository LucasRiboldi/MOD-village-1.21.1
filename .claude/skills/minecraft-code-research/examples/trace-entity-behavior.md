# Exemplo — rastrear o comportamento de uma entidade

**Situação real:**

> "Minha colônia continua achando que tem um lenhador depois que o aldeão morre.
> A vaga nunca reabre."

Este exemplo mostra o modo **FORENSIC**: o bug já existe, e a pesquisa é para
achar a causa — não para escolher arquitetura.

---

## Fase 0 — Enquadrar

```text
SISTEMA         remoção de trabalhador do registro da colônia
COMPORTAMENTO   ao perder o aldeão, a vaga da profissão deveria reabrir
VERSÃO          1.21.1 · Yarn 1.21.1+build.3 · Fabric API 0.116.15+1.21.1
OBJETIVO        investigar bug
MODO            FORENSIC
```

Sintoma observado: a contagem de profissões nunca diminui. O baú do aldeão morto
fica reservado para sempre e ninguém mais pode usá-lo.

## Reproduzir

Aldeão com profissão atribuída, morto por zumbi à noite. Na sessão seguinte, a
colônia ainda conta um lenhador e não atribui outro.

## Traçar: como o mod fica sabendo que o aldeão sumiu?

```bash
grep -rn "WorkerService.remove\|\.remove(" src/main/java/com/villagecolony | head
```

Descoberta imediata:

> `[FATO]` `WorkerService.remove` existe e **não tem nenhum chamador**.

Essa é a causa mecânica. Mas a pergunta interessante é a seguinte: **por onde ele
deveria ser chamado?**

## Duas fontes candidatas

**Candidata A — varredura.** O mod já varre aldeões por perto. Bastaria remover
quem não aparecer.

**Candidata B — evento.** A Fabric expõe eventos de entidade.

### Por que a varredura está errada

> `[INFERÊNCIA]` Ausência na varredura **não é prova de morte**. Um aldeão fora
> do raio, ou num chunk descarregado, não está morto — apenas não foi visto.

Se o mod apagasse o registro por ausência, ele apagaria trabalhadores vivos toda
vez que o jogador se afastasse. O bug trocaria de forma, não sumiria.

> `[DECISÃO]` Só o evento serve como prova de que o aldeão se foi.

## Fase 6 — O que a Fabric oferece

```bash
J=$(find . -name "fabric-entity-events-v1-*-sources.jar" | head -1)
unzip -l "$J" | grep "\.java$"
unzip -p "$J" net/fabricmc/fabric/api/entity/event/v1/ServerLivingEntityEvents.java
```

> `[FATO]` `ServerLivingEntityEvents` expõe `AFTER_DEATH` e `MOB_CONVERSION`.
> Fonte: `fabric-entity-events-v1` sources, versão 0.116.15+1.21.1.

```java
ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> { ... });
ServerLivingEntityEvents.MOB_CONVERSION.register(...);
```

## A descoberta que só a leitura entrega

Ler `MOB_CONVERSION` revelou o que o nome de `AFTER_DEATH` esconde:

> `[FATO]` Zumbificação é uma **conversão**, não uma morte. O aldeão mordido por
> um zumbi passa por `MOB_CONVERSION` e **nunca** dispara `AFTER_DEATH`.

E isso é decisivo:

> `[INFERÊNCIA]` Zumbificação é o caso **mais comum** de perder um trabalhador em
> jogo. Um mod que escutasse apenas `AFTER_DEATH` teria corrigido o bug para o
> caso raro e o mantido para o caso frequente — e pareceria consertado nos testes.

Este é o tipo de coisa que memória e tutorial não entregam. O sources jar
entregou em dois comandos.

## Ordem das operações

Traçando o que precisa acontecer na remoção:

```text
evento dispara
    ↓
tirar a marca do baú          ← precisa da posição, que está no registro
    ↓
esquecer o trabalhador        ← apaga o registro
```

> `[FATO]` A ordem importa: desmarcar depende de ler a posição do baú, e esquecer
> apaga o registro que a guarda. Invertido, o baú fica marcado para sempre.

E o que **não** deve ser removido:

> `[DECISÃO]` O baú e o conteúdo ficam. Quem morreu era o dono, não o conteúdo. O
> que sai é a promessa de que aquele baú tem dono.

## Cadeia final

```text
TRIGGER      aldeão morre ou é convertido
   ↓
EVENT        ServerLivingEntityEvents.AFTER_DEATH  |  MOB_CONVERSION
   ↓
VALIDATION   é VillagerEntity? o mundo é ServerWorld?
   ↓
ACTION       desmarcar baú → esquecer trabalhador
   ↓
STATE        WorkerService perde a entrada; a vaga reabre
   ↓
PERSISTENCE  gravado no próximo save
```

## Conclusão

**Causa raiz:** o mod não tinha nenhuma fonte de verdade sobre a saída do aldeão.
`WorkerService.remove` existia sem chamador porque a única fonte considerada
(varredura) era inválida por construção.

**Correção:** registrar os **dois** eventos. `AFTER_DEATH` sozinho seria uma
correção parcial que passaria nos testes e falharia em jogo.

**Recomendação:** degrau 5 da escada (Fabric Events). Sem Mixin.

**Risco:** LOW. Eventos são o mecanismo previsto; a lógica é idempotente
(esquecer duas vezes é inofensivo), então ordem entre mods não importa.

**Cuidado registrado para o futuro:**

- `[RISCO]` Curar um zumbi devolve um aldeão com **identidade nova**. Ele será
  tratado como aldeão novo — o que é correto, mas precisa estar documentado para
  ninguém "consertar" isso depois achando que é bug.

---

## O que este exemplo demonstra

1. **O modo FORENSIC começa pelo estado, não pelo código.** "Quem deveria chamar
   isto?" achou a causa mais rápido que ler a implementação.
2. **A hipótese barata era a errada.** Varredura parecia suficiente e teria
   trocado um bug por outro, pior e mais difícil de ver.
3. **Ler a API revelou um caso que ninguém teria adivinhado.** `MOB_CONVERSION`
   não aparece se você só procura "morte".
4. **A correção parcial era a armadilha.** Escutar só `AFTER_DEATH` passaria em
   qualquer teste e falharia no caso mais comum de jogo real.
