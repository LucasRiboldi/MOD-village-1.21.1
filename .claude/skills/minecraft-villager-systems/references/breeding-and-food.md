# Reprodução e comida

Dois sistemas ligados, e a ligação **não é** a que parece.

## O erro de partida

```text
✗  inventário tem comida  →  logo, está disposto a reproduzir
```

Não é assim. "Ter comida" e "estar disposto" são estados diferentes, ligados por
uma condição que envolve mais coisas.

**Verifique a implementação atual antes de assumir qualquer regra desta página** —
os detalhes de reprodução mudaram entre versões.

## O que entra na decisão

```text
COMIDA                    ele tem, e quanto
DISPOSIÇÃO (willingness)  o estado derivado
CAMAS                     precisa haver cama livre na vila
POPULAÇÃO                 limite proporcional às camas
IDADE                     bebês não reproduzem
COOLDOWN                  após reproduzir
CONDIÇÕES DA VILA         a vila precisa existir e ser válida
```

**A cama é o limitador real.** É por isso que uma "fazenda de aldeões" é, na
prática, uma fazenda de camas. Um mod que aumenta a população sem tocar nas camas
não muda nada.

## O ciclo da comida

```text
ITEM no chão
  ↓  o aldeão recolhe (gatherableItems da profissão)
INVENTÁRIO
  ↓
REQUISITO de comida
  ↓
DISPOSIÇÃO
  ↓
comportamento de reprodução
```

`[FATO]` `gatherableItems` é componente do record `VillagerProfession` — é ele
que determina o que aquela profissão recolhe do chão.

Aldeões também **compartilham** comida entre si. Um sistema que assume que a
comida fica com quem a recolheu vai errar.

## Antes de alterar reprodução

```text
Qual condição torna o aldeão disposto?
Como a cama entra na decisão?
Onde esse estado é armazenado?
Qual task executa?
Qual evento cria o bebê?
Qual o cooldown?
O que impede a população de explodir?
```

Sem essas respostas, alterar reprodução produz um de dois resultados: nada muda,
ou a vila cresce sem limite e mata o TPS.

## O risco de performance

Reprodução é a única mecânica de aldeão que **cria entidades sozinha**.

```text
[ ] há limite superior de população?
[ ] o limite é por vila, e a vila é bem definida?
[ ] o que acontece se o jogador construir 200 camas?
```

Ver `references/villager-performance.md`. Um sistema que aumenta a taxa de
reprodução sem limite é um gerador de lag com atraso — o problema aparece horas
depois.

## Bebês

```text
bebê → cresce → adulto
```

```text
[ ] bebês usam Schedule própria (VILLAGER_BABY)
[ ] bebês não trabalham nem adquirem profissão
[ ] a SUA task deve checar idade, quando fizer diferença
[ ] o crescimento é temporizado e persiste
```

Task que não checa idade faz bebê trabalhar. Além de estranho visualmente, quebra
a expectativa do jogador.

## Se o seu sistema atribui papéis

Um mod que dá função aos aldeões precisa decidir o que acontece com os novos:

```text
[ ] bebê nasce sem função — quem atribui, e quando?
[ ] a função é atribuída ao virar adulto?
[ ] há vaga? o sistema respeita limite por função?
[ ] o bebê conta na população da colônia?
```

Esquecer isso produz o sintoma "a colônia cresce e ninguém trabalha".

## Alimentação como recurso do mod

Se o seu mod usa comida como recurso de sistema (colônia consome comida):

```text
[ ] isso interfere na reprodução Vanilla?
[ ] o aldeão pode ficar sem comida por causa do seu sistema?
[ ] é intencional?
```

Consumir a comida que a reprodução Vanilla usaria **desliga a reprodução**
silenciosamente. Se for intencional, declare; se não for, é bug.

## Checklist

```text
[ ] verifiquei a implementação atual em vez de assumir
[ ] entendi o papel da cama e da população
[ ] há limite superior de população
[ ] o cooldown é respeitado
[ ] bebês são tratados (ou explicitamente ignorados)
[ ] a atribuição de função a novos aldeões está definida
[ ] o consumo de comida do meu sistema não desliga a reprodução sem querer
[ ] testado: vila cresce e estabiliza, não explode
```
