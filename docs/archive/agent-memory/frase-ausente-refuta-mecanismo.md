---
name: frase-ausente-refuta-mecanismo
description: A frase que o caminho suspeito escreveria e não escreveu refuta o mecanismo; contei linhas em vez de ler mais código
metadata: 
  node_type: memory
  type: feedback
  modified: 2026-09-17T01:21:05.178Z
---

No E45 (2026-09-16) o diagnóstico dizia que o laço vinha de
`deepenIfEveryOpenArmIsDone` + `restartAt` reabrindo o braço. Errado. E a
prova não veio de ler mais código — veio de contar duas frases no log:

- `went one level deeper` = **0** (o caminho suspeito a escreveria)
- `no miner branch work` = **17.517** (só é escrita quando ele **não** roda)

Eu mesmo propus antes uma segunda causa errada (`MineFrontier` repondo o
cursor). Ela caiu pelo mesmo teste: `The gallery really ends at` = **1**
contra 17.518 recusas.

**Why:** cada caminho de código deste mod deixa uma frase própria. A frase
**ausente** é evidência tão forte quanto a presente, e é mais barata que
reler a cadeia de chamadas. Duas hipóteses minhas morreram nesse teste antes
de custarem uma linha de implementação.

**How to apply:** antes de afirmar um mecanismo, pergunte "que frase este
caminho escreveria?" e conte-a no log. Se o número não bate com a
frequência do efeito, o mecanismo está errado — pare e procure outro. Vale
também para a razão entre causa e efeito (ver [[log-decide-a-constante]] e o
argumento de 1/55s vs 10/s no próprio E45). Relacionado:
[[contagem-honesta-de-testes]], [[lento-nao-e-travado]].
