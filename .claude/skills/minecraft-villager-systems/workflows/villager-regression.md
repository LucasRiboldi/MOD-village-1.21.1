# Workflow — regressão de aldeão

**Uma alteração aparentemente pequena na IA quebra sistemas não relacionados.**
Este workflow existe porque isso acontece com frequência surpreendente.

Rode depois de qualquer mudança que toque Brain, task, memória, sensor, POI,
profissão ou schedule.

---

## Por que aldeão é especialmente frágil

As camadas compartilham recursos. Uma task que ocupa o tempo do aldeão tira tempo
de outra; uma memória que expira cedo demais quebra uma task Vanilla; um POI mal
registrado muda a atribuição de profissão da vila inteira.

Nada disso aparece no teste da feature que você acabou de escrever.

## O ciclo do dia completo

Observe um aldeão por **pelo menos um dia inteiro** de jogo:

```text
[ ] ACORDA de manhã
[ ] vai ao SINO (MEET)
[ ] vai TRABALHAR no horário
[ ] faz a NOVA coisa que você implementou
[ ] SOCIALIZA
[ ] VOLTA PARA CASA ao anoitecer
[ ] DORME na cama
```

Um item faltando é regressão, mesmo que a sua feature funcione.

## Profissão e local de trabalho

```text
[ ] aldeão sem profissão ainda adquire uma ao ver um POI livre
[ ] as profissões Vanilla continuam funcionando
[ ] quebrar o local de trabalho ainda libera o aldeão
[ ] a atribuição respeita ticketCount
```

## Comércio

```text
[ ] a tela abre
[ ] as ofertas aparecem
[ ] comerciar dá XP e sobe de nível
[ ] o restock acontece
[ ] os preços refletem a reputação
```

## Reprodução

```text
[ ] aldeões ainda se reproduzem
[ ] a cama ainda limita a população
[ ] bebês crescem
[ ] bebês não trabalham
[ ] a população ESTABILIZA — não explode
```

## Ameaça

```text
[ ] em PANIC ele foge, e a sua task cede
[ ] em RAID ele se esconde, e a sua task cede
[ ] golens ainda nascem
[ ] depois da ameaça, o trabalho RETOMA
```

Os dois "cede" são os que mais falham depois de acrescentar comportamento.

## Persistência

```text
[ ] fechar e reabrir o mundo:
      profissão preservada · local de trabalho preservado
      nível e XP preservados · o estado do seu mod voltou
[ ] abrir outro save na mesma sessão não traz estado do anterior
[ ] mundo da versão ANTERIOR do mod abre
```

## Ciclo de vida

```text
[ ] aldeão morto sai do registro       (AFTER_DEATH)
[ ] aldeão zumbificado sai              (MOB_CONVERSION)  ← o mais esquecido
[ ] curar o zumbi cria um aldeão NOVO, tratado como novo
[ ] aldeão que se afasta NÃO é removido
[ ] aldeão em chunk descarregado NÃO é removido
[ ] aldeão escondido em raid NÃO é removido
```

Os três últimos são o mesmo bug com três disfarces: **ausência tratada como
morte**.

## Vários aldeões

```text
[ ] 2 aldeões não disputam o mesmo alvo
[ ] a reserva expira se o dono morrer
[ ] a vaga reabre
[ ] 10 aldeões distribuem o trabalho
[ ] 50 aldeões: o TPS aguenta
```

O teste de **2** pega conflito; o de **50** pega custo.

## Multiplayer

```bash
./gradlew runServer
```

```text
[ ] o servidor dedicado sobe
[ ] o cliente conecta
[ ] os aldeões se comportam igual
[ ] o comércio funciona conectado
[ ] o que o cliente exibe está correto ao ENTRAR, sem interagir
```

## Automatizar o que der

```bash
./gradlew runGametest
```

Gametest cobre lógica isolada de forma reprodutível — sessão manual não é
reprodutível.

**Declare o que ele não cobre:** o mundo de teste é vazio, sem vila gerada, sem
POI natural. O ciclo do dia completo e a atribuição de profissão em vila real só
se verificam em jogo.

## Compatibilidade

```text
[ ] quantos Mixins em VillagerEntity? (mais de um é sinal de alerta)
[ ] alguma task Vanilla foi removida?
[ ] a Schedule Vanilla foi alterada?
[ ] testado com um mod de aldeão, se houver
```

## Relatar

```text
VERIFICADO RODANDO   o que você executou e observou
TEM TESTE ESCRITO    coberto, não executado agora
NÃO VERIFICADO       o que ficou de fora, e por quê
```

**Nunca diga que passou sem ter executado.** Se você não observou o dia inteiro,
diga que não observou — é mais barato que descobrir depois que o aldeão parou de
dormir.
