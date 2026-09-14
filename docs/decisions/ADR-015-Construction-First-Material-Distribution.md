# ADR-015 — Prioridade híbrida de materiais e cobertura de blocos

**Status:** Aceita pelo autor em 2026-09-13
**Tipo:** Economia / distribuição de trabalho

## Contexto

O ciclo da colônia atendia demandas de produção e construção sem distinguir
materiais que destravam uma obra dos destinados apenas a estoque. Além disso,
`smooth_stone_slab` aparecia em plantas, mas a cadeia do pedreiro não declarava
a pedra lisa produzida pela fornalha. Materiais de construção fora do catálogo
fechado de `ResourceType` não entram na contagem dos baús nem recebem tarefas.

## Decisão

- Adotar o modelo híbrido C: materiais faltantes da obra ativa têm prioridade;
  trabalhadores excedentes continuam atendendo metas de produção.
- O pedreiro fabrica `smooth_stone_slab`; o fundidor produz `smooth_stone` a
  partir de `stone`, seguindo receitas Vanilla.
- Não impor teto numérico arbitrário às reservas. A capacidade física dos baús
  continua sendo o limite de armazenamento existente.
- Quando não houver obra ativa ou seus materiais estiverem cobertos, manter a
  produção/estoque como destino do trabalho, incluindo blocos de construção.
- Materiais sem profissão produtora dedicada serão destinados ao fundidor e ao
  criador (inclui saves legados de pastor) como funções de cobertura, sem
  retirar suas funções originais.

## Escopo ainda não implementado

O catálogo de `ResourceType`, `ResourceTally` e `ChestInventoryReader` é fechado.
Portanto, a cobertura de materiais arbitrários e o estoque de qualquer bloco
exigem suporte por `ResourceId` na contagem, nas metas/tarefas e na execução.
Até esse lote, a decisão de fallback está aprovada, mas não deve ser anunciada
como comportamento já funcional. A validação visual em jogo também continua
necessária para a distribuição e a cadeia da pedra lisa.

## Validação desta etapa

`./gradlew.bat build` e `./gradlew.bat runGametest`: 316/316 GameTests passaram.
Os testes cobrem a prioridade de material de obra sobre excedente e a receita
real Vanilla de `stone` para `smooth_stone` usada pela laje. Isso não comprova
comportamento visual em um mundo do autor.
