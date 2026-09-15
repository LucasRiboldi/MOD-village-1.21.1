# ADR-016 — Economia de construção e descoberta de lotes

**Estado:** Aceita pelo autor em 2026-09-13
**Escopo:** Fabric 1.21.1; materiais, prioridades de obra e seleção de lotes.

## Contexto

A colônia conhece materiais de duas formas incompatíveis. Blueprints e obras
usam `ResourceId`, que pode nomear qualquer bloco registrado; estoque, metas e
tarefas ainda usam o catálogo fechado `ResourceType`. Assim, um material
presente em uma planta pode não ser contado nem encaminhado a um produtor. A
decisão anterior de cobrir materiais sem profissão dedicada (ADR-015) ainda não
está integrada ao fluxo genérico de tarefas.

A escolha de obra também não expressa claramente a necessidade atual da vila,
e a seleção de lotes não verifica previamente se há um ponto de aproximação
alcançável. O jogador pode liberar ou criar terreno depois da primeira busca,
mas a releitura depende hoje sobretudo de invalidações por eventos e do cursor
limitado existente.

## Decisão

1. Preservar `ResourceType` para cadeias tipadas já implementadas e ampliar a
   contagem de `ResourceTally` para aceitar `ResourceId`. A fronteira Fabric
   converte itens registrados em IDs; o Core continua sem dependência de
   Minecraft. Leituras de baú continuam ao vivo, sem persistir inventários.
2. Encaminhar necessidades por produtor declarado quando houver cadeia
   conhecida. Materiais de construção sem produtor dedicado usam as funções de
   cobertura aprovadas na ADR-015 (fundidor e criador), sem remover suas
   tarefas originais. Um pedido genérico só é aberto quando a execução sabe
   obter ou fabricar o ID; não criar recursos sem origem física nem tarefas
   impossíveis. A contagem de material e a aceitação pelo construtor devem usar
   exatamente a mesma regra de substituição.
3. Ordenar as próximas construções pela necessidade observável da vila: casas
   quando a capacidade de camas não atende os aldeões adultos, estruturas
   profissionais necessárias à cadeia ainda ausentes e, depois, outras
   estruturas elegíveis. A regra não altera empregos Vanilla nem remove obras
   abertas. A elegibilidade de solo e estrada e definida pela ADR-017 e nao e
   alterada por esta ADR.
4. A seleção de lote deve rejeitar locais sem ponto de aproximação
   navegável/alcançável antes de reservar a obra. Diagnóstico em jogo será
   solicitado por comando e exibirá partículas temporárias e motivos para
   candidatos encontrados, sem alterar terreno ou criar marcador persistente.
5. Reavaliar candidatos em passagens incrementais, com orçamento por tick,
   apenas em chunks carregados. Eventos de mudança do jogador continuam
   invalidando os índices conforme ADR-012; a revarredura periódica é uma
   recuperação para terreno liberado/alterado sem depender de evento, não uma
   varredura global nem carregamento forçado.

## Consequências

- A implementação será dividida em cinco lotes independentes e verificáveis:
  (1) contagem e roteamento de materiais por ID; (2) prioridade de estruturas;
  (3) acessibilidade do lote; (4) comando de diagnóstico com partículas;
  (5) revarredura incremental periódica.
- Cada lote requer testes unitários e/ou GameTests do contrato tocado. Os
  GameTests medem correção e custo limitado em servidor headless; TPS e
  progresso sustentado de aldeões ainda exigem validação em mundo de
  desenvolvimento.
- O primeiro lote não converte automaticamente qualquer bloco em recurso
  minerável: a regra de produção precisa apontar para uma cadeia real existente
  e respeitar proteção da vila, origem física e ferramentas.
- A prioridade de construção usa estado observado no mundo e mantém obras já
  iniciadas. Dados Vanilla não serão duplicados em save.
- Nenhum lote pode forçar chunk loading, escavar solo protegido ou modificar o
  terreno; a politica de elegibilidade e estrada permanece a da ADR-017.
