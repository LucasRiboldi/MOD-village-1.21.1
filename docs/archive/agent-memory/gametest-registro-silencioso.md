---
name: gametest-registro-silencioso
description: Classe de gametest fora do fabric.mod.json some da bateria sem falhar — só a contagem acusa
metadata: 
  node_type: memory
  type: project
  modified: 2026-08-21T04:30:41.879Z
---

Toda classe de gametest precisa estar listada no entrypoint
`fabric-gametest` de `src/gametest/resources/fabric.mod.json`. Uma que
não esteja **não falha: ela deixa de existir**, e a bateria diz "All N
required tests passed" com N menor.

Aconteceu em 2026-08-21 ao mover três testes para um arquivo novo: 143 →
140, e a mensagem foi de sucesso.

**Why:** o único sinal é a contagem total, e ela só serve se alguém a
compare com a anterior.

**How to apply:** ao criar arquivo de gametest, registrar no
`fabric.mod.json` no mesmo passo; e sempre comparar a contagem de testes
com a do ciclo anterior antes de dar um ciclo por fechado — é o que o
ritual do [[relatorio-commit-ritual]] pede no relatório.
