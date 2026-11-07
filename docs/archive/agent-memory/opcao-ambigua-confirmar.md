---
name: opcao-ambigua-confirmar
description: "Opção escrita de forma ambígua (\"o curral ganha uma porta\") foi implementada como \"porta no lugar do portão\" e o autor não tinha pedido isso"
metadata:
  node_type: memory
  type: feedback
  modified: 2026-10-28T20:44:57.004Z
---

Ao escrever opções para o autor, dizer exatamente o que muda no mundo ("além do portão" ou "no lugar do portão").
Ao implementar uma escolha que muda algo visível (bloco, estrutura, aparência), repetir em uma linha o que vai
mudar antes de escrever código, se a opção admitir mais de uma leitura.

**Why:** 2026-10-28, T2: a opção B dizia "o curral ganha uma porta de madeira"; eu troquei o portão pela porta
(PenDoors, já com teste verde) e o autor respondeu "eu não pedi porta no lugar do portal". Ele escolheu então a
opção A (pastor por cima da cerca). O trabalho foi desfeito antes do commit.
**How to apply:** em toda lista de opções, evitar verbo vago ("ganha", "melhora"); nomear o que sai e o que entra.
Relacionado: [[decisoes-0103-correcoes]], [[decisoes-autor-2026-10-27-sete-itens]] (sempre perguntar opções).
