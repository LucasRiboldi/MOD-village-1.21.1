# Alternativa A - plano de implementacao

## Objetivo

Aplicar a alternativa conservadora de estabilidade sem substituir mecanismos ja
existentes: encerrar obras que nao podem mais progredir, preservar a orientacao
horizontal das plantas conforme a ADR-008 e manter explicito o que ainda exige
decisao ou playtest.

## Etapas

1. **T1 - obra totalmente adiada**
   - Alterar primeiro o GameTest para exigir que a obra saia da fila ativa ao
     fim da janela de paciencia.
   - Remover a excecao que zera o relogio de progresso para sempre.
   - Preservar a construcao parcial e a reserva do lote contra sobreposicao.

2. **ADR-008 - orientacao horizontal**
   - Fazer `BlueprintBlock` carregar `Optional<Side>` sem quebrar os
     construtores de conveniencia existentes.
   - Girar a direcao junto com as coordenadas em `Blueprint.rotated`.
   - Ler `facing` da paleta NBT em `StructureBlueprintReader`.
   - Aplicar a direcao no `BlockState` quando a geometria nao tiver uma regra
     mais forte.

3. **Auditoria da alternativa A ja existente**
   - Confirmar que `MineMarks`, `DetourWalker`/`StrandedEscape`,
     `SweepDeadline`/`PlanningBudget` e `ServerMemory` ja cobrem as demais
     fundacoes da alternativa.
   - Nao ampliar a politica de distancia da rua sem decisao arquitetural.

4. **Verificacao e estado vivo**
   - Rodar testes unitarios focados, `build` e a bateria completa de GameTests.
   - Atualizar `STATE.md`, `TODO.md` e o log de desenvolvimento.
   - Levantar do codigo a matriz de atividades das oito profissoes.
