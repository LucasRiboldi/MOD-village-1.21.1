---
name: tudo-pelo-grafo-0440
description: "0.4.40, ADR-048: escolhas do autor 1A 2A 3 4B 5A 6A 7A 8A 9A; todo caminho e rede pelo grafo, toda vila no sistema atual, escrita do MAE removida; candidata não jogada"
metadata:
  node_type: memory
  type: project
  modified: 2026-11-06T19:39:19.893Z
---

Em 2026-11-06 o autor respondeu à varredura: "3 ignora a antiga, esquece o que é velho e passa sempre a usar o sistema mais atual". Regra permanente: não manter ramo para vila/save antigo.

- `PathProposals` é o único jeito de pedir caminho; `RoadNetwork.of` é a rede; `IdleWorkers` a lista de livres; `DoorFronts` o leitor de porta; a obra MAE usa a régua do construtor.
- Saíram `NewVillage`, `VillageOrigin`, `PathLinks.tend`, `WorkPath`, `StreetGaps` e a escrita do MAE.
- A prova de geração saiu a pedido do autor; `OldChunkReceipt` só mantém o tipo registrado para chunk antigo não gerar aviso.
- Refactor grande em Python: heredoc longo com aspas quebra o bash do harness; escrever o script com Write.
- Mutação precisa compilar: Error Prone reprova variável sem uso e comparação impossível.

**Why:** sistemas paralelos de caminho e prova por seção causavam conflito e obra que nunca terminava.
**How to apply:** tratar como candidata até o jogo. Conferir `TODO.md`, "Abertos da varredura de 11-06". Plano mestre do MAE, fora do repo, ainda descreve o fluxo antigo.

**Jogo da 0.4.40 (Spark WCWRBHTKlX, 2026-11-06, 3,5 h):** PR 57 integrada. Confirmados: obra MAE em 7 min, 0 `traced a path`, pedreiro com 168 `PAVED` e 104 `LEVELLED`, 7 de 75 entradas soltas, sem erro ao fechar. Aberto: `NOT_IN_GAME` sem obra automática, causa não provada; `VC_MINE_HOME` repetido a cada minuto; 19 colunas calçadas de novo depois de voltar ao solo.
