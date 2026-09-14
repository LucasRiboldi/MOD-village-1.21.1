# Plano: coleta de superfície do fundidor

## Problema

O fundidor depende de areia para cumprir metas de vidro, mas a coleta ainda
está roteada ao mineiro e a grama não tem recurso/tarefa. O fundidor também
não recebe ferramenta, e não existe regra para afastar a coleta de grama das
estruturas da vila.

## Dono e arquivos previstos

- Core: `Production`, `ResourceType`, `Capability`, `TaskType`,
  `ProfessionRegistry`, `ColonyCycle`, `WorkDemand` e `ColonyGoals`.
- Fabric: adaptador de recursos/ferramentas, equipamento, busca por superfície,
  execução da coleta, ciclo e limpeza dos trabalhadores.
- Testes: responsabilidade profissional, metas, orientação por estruturas,
  ferramenta e GameTests da coleta/loot.
- Documentação viva: ADR-014, `STATE.md`, `TODO.md`, matriz de profissões e
  registro de desenvolvimento.

## Decisões

1. Fundidor recebe uma capacidade `COLLECT_SURFACE_RESOURCE`, além de
   `SMELT_ITEMS`; areia e `grass_block` são tarefas dessa capacidade.
2. A areia continua sendo pedida somente quando faltar para vidro. A grama é
   pedida por material real de uma construção aberta.
3. Ferramenta inicial do fundidor: pá de ferro com Toque Suave, para obter o
   bloco real por loot vanilla.
4. Grama nunca é coletada no raio horizontal de 64 blocos do centro. Fora dele,
   a busca usa o setor cardeal com maior distância mínima até peças de vila,
   obtidas apenas em chunks já carregados; sem peças observáveis, usa direção
   determinística e registra a condição.
5. O trabalho quebra apenas blocos validados e guarda drops no baú pessoal;
   se não houver espaço, não quebra.

## Etapas e verificação

1. Testes vermelhos para produção→tarefa, dono da capacidade e demanda de
   `grass_block`.
2. Implementar modelo, metas e ferramenta; rodar testes focados e `build`.
3. Implementar seleção espacial, busca e trabalho Fabric com loot vanilla;
   GameTests cobrindo distância, setor, estruturas e drop Toque Suave.
4. Atualizar documentação viva e rodar `./gradlew build` e
   `./gradlew runGametest`.

## Limite

Não altera mineração subterrânea, profissões sem relação com a coleta, nem
faz commit/push ou atualização de JAR nesta tarefa.
