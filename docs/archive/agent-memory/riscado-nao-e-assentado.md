---
name: riscado-nao-e-assentado
description: "BuilderWork.placeOne risca blocos sem assentar, a obra é dada por terminada com 0 postos, e o reparo reabre a mesma lacuna para sempre"
metadata: 
  node_type: memory
  type: project
  modified: 2026-09-22T23:39:55.337Z
---

`BuilderWork.placeOne` tem **quatro saídas que riscam a peça sem assentar
nada** (`markPlaced` sem `job.placed++`): bloco que o jogo não conhece, algo
no caminho (`!isReplaceable`), peça sem apoio (`!canPlaceAt`) e a barreira de
teste. O comentário do próprio arquivo já dizia: *"riscado não é assentado"*.

Consequência medida em jogo (22-09): a lista `remaining` esvazia por riscos, o
`complete()` dispara, a obra é registrada como **terminada com `0 blocks
placed`**, e a casa entra no registro com a lacuna intacta. O
`BuildingRepairPlanner` então compara planta × mundo, reencontra os mesmos
blocos e reabre — a cada trinta segundos, sem fim.

**Why:** a vaga de obra da colônia é **única**, então esse laço não era ruído:
ele impedia qualquer construção nova de nascer. O sintoma que o autor viu foi
"as obras estão terminando e recomeçando".

**How to apply:** ao investigar "vila não constrói nada", procure no log
`starts repair sweep` repetido na mesma origem com o mesmo número de blocos, e
`finished ... — 0 blocks placed`. As duas frases juntas são a assinatura. O
conserto de 22-09 foi comparar os blocos de pé na abertura com os do
fechamento e não repetir tentativa que não avançou — ver
`FoundationRepairGameTest.aRepairThatPlacedNothingIsNotOpenedAgain`.

Fica em aberto o que gera a lacuna: `ladder` e `wall_torch` pedem apoio que a
parede não tem no momento em que a ordem de assentamento chega neles.

Ver [[reparo-adota-casa-fantasma]] — mesmo planejador, defeito irmão, e foi
investigar aquele que deu o mapa para achar este.
