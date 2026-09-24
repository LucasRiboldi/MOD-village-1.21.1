# ADR-023 - Pecas de construcao sem apoio permanecem pendentes

**Status:** Aceita
**Data:** 2026-09-24

## Contexto

Uma planta pode conter uma `ladder`, `wall_torch` ou outra peca que nao pode
ser assentada no estado fisico atual do mundo. O fluxo anterior retirava a
peca da lista depois da recusa do Minecraft. Isso registrava progresso que nao
existiu, declarava a obra completa com uma lacuna e permitia que o reparo
posterior repetisse a mesma tentativa.

A alternativa de remover a peca da planta troca o defeito por uma construcao
silenciosamente incompleta. Reordenar a planta tambem nao resolve os casos em
que o apoio foi quebrado pelo jogador ou ainda sera posto por outra obra.

## Decisao

Uma recusa por falta de apoio gera `Skipped(UNSUPPORTED)`. A peca continua na
planta e nao entra em blocos postos, materiais restantes imediatos ou contagem
de conclusao. O projeto registra uma `DeferredPiece` com posicao absoluta,
bloco planejado, motivo e uma assinatura dos estados do alvo e de seus seis
vizinhos.

O registro e persistido como adiamento, nao como estado do mundo. A fonte de
verdade continua sendo o bloco fisico: a cada ciclo de planejamento, a
assinatura e recalculada. Se ela nao mudou, nenhuma nova tarefa e criada; se
mudar, o adiamento e removido e a peca volta a fila normal. O construtor
encerra a tarefa ao adiar a peca, e o relogio de inatividade nao abandona uma
obra cujas pecas restantes estejam todas adiadas.

## Consequencias

- Nenhuma peca recusada passa a contar como construcao concluida.
- Reiniciar o servidor nao reabre a mesma tentativa sem uma mudanca fisica.
- Uma obra pode aguardar indefinidamente uma mudanca de apoio; isto e
  intencional, porque a alternativa seria apagar a peca ou inventar suporte.
- A decisao nao reordena blueprints e nao muda o tratamento de material ausente;
  ela cobre somente a impossibilidade fisica de assentamento.

## Verificacao

`ConstructionOutcomeTest` prova os resultados de execucao;
`ConstructionProjectTest` prova o adiamento e a invalidacao pela assinatura;
`ConstructionSaveTest` prova a persistencia. Os cenarios
`BuildProgressGameTest` provam no servidor a obra parcial sem reabertura e a
nova tarefa depois de o apoio mudar.
