# Histórico de regressão

Um defeito comportamental corrigido não some: vira linha aqui, com a causa, a
correção e **o teste que impede a volta**. Defeito sem teste entra assim mesmo,
dizendo que não tem — é o que separa "corrigido" de "corrigido e protegido".

Formato: `DEFEITO → MEDIDA → CAUSA → CORREÇÃO → TESTE`.

---

## 2026-09-09 — a sessão dos seis defeitos

Todos vistos em jogo, todos medidos em log, nenhum encontrado pela bateria.

---

### R-001 — a parede recusada voltava a ser avaliada a cada cinco minutos

**Medida.** Sessão de 09-06: **925 linhas `Not a tree` sobre 140 coordenadas**
— cada parede reavaliada sete vezes —, 273 desistências por travamento, e
**48 árvores derrubadas** na sessão inteira. Das amostras de estado, 749 com o
lenhador procurando e 93 cortando: 7% do expediente com machado na mão.

**Causa.** `TreeMarks.REJECTED` tinha prazo fixo de dez ciclos, e `isRejected`
**apagava a marca** ao vê-la vencida. Apagar matava a contagem junto: toda
recusa voltava a ser a primeira, e o prazo nunca crescia. Os intervalos entre
recusas da mesma coordenada eram exatamente 5 min — o prazo, batendo.

**Correção.** A marca sobe a mesma escada de `UNREACHABLE` (`memoryFor`), a
contagem é do grupo de troncos e não do bloco por onde a busca entrou, e a
entrada vencida **fica** — quem a apaga é `forgetStaleMarks`, no prazo longo.

**Teste.** `TreeMarksTest`, 5 casos. Commit `41ce5d2`.

---

### R-002 — a barreira acusava uma cadeia que nunca teve a vez

**Medida.** 24 `TEST BARRIER skipped stripped_oak_log` com **50 toras de
carvalho no baú** da mesma colônia, no mesmo instante.

**Causa.** `BuilderWork` riscava a peça na primeira falta e chamava
`markPlaced`. O fabricante sabe descascar desde 09-05, mas nunca ganhava o
ciclo — descascou **um** tronco na sessão inteira.

**Correção.** Carência de 5 ciclos por (obra, peça) antes de riscar. E
`hasMaterialForNextBlock` passou a perguntar por `willStrike` em vez de
`chainFor`: enquanto a barreira espera, a peça segura a obra como qualquer
outra; depois que ela desiste, libera. As duas respostas têm de casar, senão a
obra acorda, tenta, falha e dorme todo ciclo.

**Teste.** `TestBarrierGraceTest`, 7 casos. Commit `41ce5d2`.

---

### R-003 — o mineiro não parava na meta, e o relatório comparava coisas diferentes

**Medida.** 496 amostras com a meta ultrapassada, 442 no mesmo mineiro em
`105 of 32 so far`.

**Causa.** Dois defeitos no mesmo lugar. `task.amount()` era lido por **um só
lugar no mod inteiro** — o `MinerReport` — para escrever o log; nada em produção
comparava o trazido com o pedido. E `MinerHaul.deposit` devolve **tudo** o que
coube no baú (terra, carvão, minério), então "105 of 32" nem comparava as mesmas
coisas: a leitura natural era verdadeira por acidente.

**Correção.** `deposit` separa o que foi pedido do total (`Haul`), o `Job` conta
os dois, e a tarefa encerra com a pedra já depositada — mesma fronteira em que o
lenhador e o fabricante param.

**Teste.** `MinerGameTest.theMinerStopsOnceTheOrderIsFilled`, **conferido nos
dois sentidos**: com o encerramento desligado ele falha. Commit `a8b5977`.

---

### R-004 — uma roça sem lote parava a vila inteira

**Medida.** Colônia `634bf5cc`, uma hora: **78 de 108 ciclos com
`assigned 0 tasks (0 open)`**, com 86 pedregulhos e 83 tábuas paradas no baú.
`sweep: 108 planner runs, 108 answered by the index, 0 complete rounds`.

**Causa.** `ConstructionPlanner` escolhe as plantas da roça quando a população a
deve. Se o lote está fora do alcance do fazendeiro, ele recusa e **encerra a
passagem — sem nunca tentar uma casa**. O lote de amanhã é o mesmo de hoje.
Sem obra não há pedido de tábua nem de pedra além do piso: construtor e mineiro
ficam sem tarefa. O lenhador seguia, porque tem meta própria — e era isso que
fazia a vila parecer meio viva.

**Correção.** A roça recusada cede a vez por 20 ciclos e as casas passam. A cota
de uma por quinze aldeões continua de pé; muda só quando é cobrada. O prazo
existe para o recuo não virar o defeito oposto.

**Teste.** `FarmPostponementTest`, 6 casos. **A ligação com o planejador não
está coberta** — são duas linhas, e só a sessão em jogo a confirmou.
Commit `ab7bfeb`.

---

### R-005 — a prioridade do fabricante era sorteio

**Medida.** 16 `stripped_oak_log` riscados com 59 toras no baú; na mesma sessão
o fabricante fez 13 lotes de escada, 7 de tábua, 1 porta, 1 vidraça — e
**zero** descascados.

**Causa.** Uma linha. `ConstructionProject.remainingMaterials` monta um
`LinkedHashMap` na ordem da planta e devolvia `Map.copyOf`, que é imutável e
**sem ordem**, embaralhada a cada execução da máquina virtual. O
`ManufacturerWork` percorre esse mapa e **para no primeiro material que
consegue produzir** — então a ordem *é* a prioridade dele.

**Correção.** `Collections.unmodifiableMap`. Não foi preciso escrever prioridade
nenhuma: `nextBlock` é a primeira posição que falta, e é dela que a lista começa
a ser contada, então o primeiro material já é o que trava a obra. Uma tentativa
de reordenar no fabricante foi escrita e **descartada por redundante**.

**Teste.** `ConstructionProjectTest`, 2 casos, conferidos nos dois sentidos. O de
ordem usa **seis** materiais de propósito: com dois, o sorteio acertaria metade
das vezes e o teste passaria em código quebrado. Commit `1c4e627`.

---

### R-006 — o mineiro girava no próprio eixo

**Medida.** Queixa do autor. Log:

```
digging Diorito at 2434,44,-1428 · he is at 2430,64,-1435 · walking to 2427,61,-1429
stall 1198 → 1798 → 2398/2400   ·   still 0/300
```

**Causa.** `stepAlongTheShaft` ancora em `orderIndexNear`, que aceitava qualquer
posição da ordem a até **uma perna — 8 blocos**. Quem está na superfície em cima
da mina passa nesse teste. Daí em diante a cadeia é rigorosa — os dois guardas
protegem contra bloco não cavado e contra parede —, mas **nenhum olha o primeiro
elo**: se o aldeão não está no corredor, não há corredor entre ele e o passo. O
javadoc do método sempre prometeu *"nulo quando ele não está na passagem"*.

**O `still 0/300` é a assinatura**, e vale como padrão: o guarda de imobilidade
não pega quem **gira**. A navegação não traça caminho para dentro da rocha, vira
o aldeão para o alvo e o deixa lá.

**Correção.** `IN_THE_PASSAGE = 2`, e o número foi **medido**: o caracol entrega
cada degrau como três posições, e de cima da mina a mais próxima é a **cabeça**
da escada, a 3,16 — enquanto na boca é o **piso**, a 1,00. Por isso `REACH` (4)
não serve.

**Teste.** `MinerLegTest.fromOnTopOfTheMineHeStillGoesInThroughTheMouth`, escrito
**antes** da correção e falhando com o valor exato do defeito (`731,61,893`).
O teste de superfície que já existia passava porque punha o aldeão a 20 blocos,
onde a ordem já está fora de alcance. Commit `3e367d5`.

---

### R-007 — a vila restaurada não tinha quem provasse que volta a trabalhar

**Não é defeito visto em jogo: é lacuna fechada antes de virar um.**

43 testes de persistência provavam que o estado **volta**; nenhum perguntava se,
depois de voltar, **alguém trabalha**. Entre as duas coisas existe um despertar:
toda colônia volta `DORMANT`, e `runColonyCycles` pula quem não está `ACTIVE`.
Se esse elo quebrar nada estoura — o mundo abre, os aldeões estão lá, e a vila
fica parada para sempre.

**Teste.** `SessionResumeTest`, 5 casos, conferido nos dois sentidos.
Commit `bbc274a`.
