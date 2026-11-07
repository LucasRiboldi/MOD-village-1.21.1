---
name: ci-verde-esconde-pit-parado
description: O passo PIT do CI tem continue-on-error; CI verde não diz que o PIT rodou — ele ficou parado do 78e7efc ao 7619b1d
metadata:
  node_type: memory
  type: project
  modified: 2026-09-25T03:20:56.418Z
---

Em 2026-09-25 o `./gradlew pitest` parava antes da primeira mutação
("Tests failing without mutation": `ServerMemoryRegistrationTest`) desde o
`78e7efc`, e o CI seguia verde porque o passo tem `continue-on-error: true`.
O STATE.md chegou a registrar "PIT verde no local e no CI" para esse commit.

Causa: o PIT roda sem `fabric-loader-junit` (build.gradle), e aquele teste
em `core.type` chama `Bootstrap.initialize()`. Corrigido excluindo-o do PIT
(`excludedTestClasses`) e movendo o teste puro para `ServerMemoryTest`.

**Why:** "tem PIT no CI" e "o PIT rodou" são coisas diferentes, igual a
[[contagem-honesta-de-testes]] e [[gametest-registro-silencioso]].
Desde 2026-09-25 o passo roda sem `continue-on-error` (não há
`mutationThreshold`, então só reprova o PIT que nem começa). Se alguém puser
threshold no build.gradle, o CI passa a reprovar por pontuação.

**How to apply:** para afirmar PIT verde, ler `Generated N mutations` no log
(local ou `gh run view --log`), nunca o status do job. Teste novo em
`com.villagecolony.core.*` que precise do jogo carregado quebra o PIT de novo.
