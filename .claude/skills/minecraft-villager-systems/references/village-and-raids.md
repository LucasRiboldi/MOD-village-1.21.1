# Vila, ameaça e raids

O aldeão não existe sozinho. Toda feature precisa dizer em qual escopo ela opera.

## A pergunta de escopo

```text
É INDIVIDUAL?   um aldeão
É por POI?      um local de trabalho
É por VILA?     um agrupamento
É por CHUNK?
É por MUNDO?
```

Escolher errado produz bugs característicos:

| Escopo errado | Sintoma |
|---|---|
| individual quando era por vila | dois aldeões fazem a mesma coisa e brigam |
| por vila quando era individual | um aldeão bloqueia todos os outros |
| por mundo quando era por vila | duas vilas compartilham estado sem sentido |
| por chunk | tudo quebra quando o chunk descarrega |

## O que é uma "vila" para o Vanilla

Não há um objeto `Village` central e persistente no jogo moderno. A vila **emerge**
de POIs próximos: camas, locais de trabalho e sinos.

Consequência importante: **os limites da vila são fluidos.** Construir uma cama
longe pode esticar a vila; quebrar POIs pode dividi-la.

Se o seu mod tem um conceito próprio de colônia com limites definidos, ele é
**seu** — e você precisa decidir:

```text
[ ] como a colônia é detectada
[ ] o que a define (camas? aldeões? um bloco central?)
[ ] o que acontece quando duas se aproximam — fundem? coexistem?
[ ] o que acontece quando ela encolhe
[ ] onde esse estado persiste
```

E, importante: **o seu conceito não é o do Vanilla.** Não presuma que o jogo vai
respeitar seus limites — golem, raid e reprodução seguem as regras deles.

## Ameaça e pânico

```text
inimigo próximo → sensor → memória → Activity PANIC → tasks de fuga
```

`PANIC` sobrepõe o trabalho, e **isso é correto**. Um sistema de trabalho que não
cede lugar faz o aldeão colher madeira durante um ataque de zumbis.

```text
[ ] a minha task perde para PANIC
[ ] o shouldRun considera "não estou em pânico"
[ ] o trabalho retoma depois, sem perder o progresso
```

O último item é o que faz o sistema parecer bem-feito: fugir e voltar ao trabalho
é melhor que fugir e recomeçar do zero.

## Raids

`[FATO]` MC 1.21.1 — as Activities `PRE_RAID`, `RAID`, `HIDE` e `CELEBRATE`
existem em `net.minecraft.entity.ai.brain.Activity`.

Durante uma incursão o aldeão se esconde. Qualquer sistema de trabalho precisa
**parar**.

```text
[ ] a minha task perde para RAID e PRE_RAID
[ ] aldeão escondido não é tratado como "sumiu"
[ ] o trabalho retoma depois da incursão
```

O segundo item é sutil e produz um bug feio: um registro que remove aldeões "não
vistos" apaga a vila inteira durante um raid, porque todos estão escondidos.

## Golem de ferro

O golem nasce de aldeões reunidos e reage à reputação do jogador. Se o seu mod
muda a movimentação ou o agrupamento dos aldeões, ele pode mudar — sem querer — a
geração de golens.

```text
[ ] os aldeões ainda se reúnem?
[ ] a geração de golem continua funcionando?
```

## Ameaças e proteção

Se o mod constrói ou protege blocos, defina o limite com cuidado:

```text
[ ] o que é "da colônia" e o que é do jogador?
[ ] o mod pode quebrar bloco que o jogador colocou?  ← quase sempre NÃO
[ ] o mod pode quebrar bloco da vila gerada?          ← quase sempre NÃO
[ ] quais são as exceções, e por quê?
```

Bloco quebrado por engano é **dano no save do jogador**. É o tipo de erro que
custa a confiança e não é recuperável.

Regras estreitas e explícitas valem mais que regras genéricas: "só tronco e folha
com copa viva, ligados ao que foi encontrado" é auditável; "árvores num raio" não
é.

## Coordenação entre vilas

```text
[ ] duas vilas próximas compartilham recurso?
[ ] um aldeão pode trabalhar para a vila errada?
[ ] o que acontece quando as vilas se fundem?
```

Se o seu sistema tem colônias, a fusão é um caso real e precisa de regra — não de
descoberta em produção.

## Checklist

```text
[ ] o escopo de cada feature está declarado (individual/POI/vila/mundo)
[ ] o conceito de vila/colônia do mod está definido, se houver
[ ] o trabalho perde para PANIC, PRE_RAID e RAID
[ ] aldeão escondido não é tratado como ausente
[ ] o trabalho retoma depois da ameaça
[ ] a geração de golem não foi quebrada
[ ] as regras de proteção de bloco são estreitas e explícitas
[ ] fusão/divisão de vila tem comportamento definido
```
