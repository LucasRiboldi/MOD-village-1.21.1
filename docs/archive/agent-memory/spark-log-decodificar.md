---
name: spark-log-decodificar
description: Como ler um link spark.lucko.me sem navegador — baixar o protobuf cru e os três erros de leitura que inflaram os números em 2026-09-24
metadata:
  node_type: memory
  type: reference
  modified: 2026-09-25T05:01:37.893Z
---

Link `spark.lucko.me/<id>` é SPA; o WebFetch só vê a casca. O dado cru vem de
`https://spark-usercontent.lucko.me/<id>` (protobuf `SamplerData`, sem gzip) e
o WebFetch salva o .bin em tool-results. Decodificar em Python com parser
wire-format manual (sem .proto compilado).

Formato real (verificado na amostra wIEM9zz90l):
- `threads[0]` (campo 2) tem `children` (campo 3) = **lista plana** de todos os
  StackTraceNode; `children_refs` (campo 9, packed varint) são índices nessa lista.
  Raiz = nó que ninguém referencia.
- `times` (campo 8, packed double) = um valor por janela de 1 min, e é tempo
  **INCLUSIVO**. Somar os filhos por cima conta tudo várias vezes (errei isso:
  cadeia Thread.run→runWith com o mesmo valor é a prova).
- `n` de um método = pontos na árvore, não número de chamadas.
- Metadado campo 5 = agregador: `3`=tick_length_threshold (µs), `4`=ticks incluídos.
  **O filtro NÃO é "só ticks lentos"** (errei isso em 24-09): o tick do spark
  conta o intervalo inteiro, com a espera até o próximo — em 25-09, minutos com
  MSPT máx 46 ms gravaram 57 ticks. `Unsafe.park` é essa espera, não trava. Para
  "travou?", use o MSPT do campo 7, não a lista de ticks gravados.
- Comparar dois perfis: o mesmo script em `spark_report_lib.load(path)`; mas
  cheque se a ATIVIDADE foi a mesma (obra parada não chama código de obra).
- Campo 7 = estatística por janela: 4=TPS, 5=MSPT mediano, 6=MSPT máx, 1=ticks
  gravados, 8=entidades, 10=chunks. Leia isto PRIMEIRO: responde "travou?".

Relacionado: [[log-decide-a-constante]], [[lento-nao-e-travado]].
