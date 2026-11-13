# ADR-043 — Grafo hierárquico de ruas com migração compatível

Estado: aceita pelo autor em 2026-11-01 e implementada na `main` (`RoadLines`, `RoadGraphRefresh`). Emendada pela ADR-050 (persistência do grafo); a ADR-048, a ADR-052 e a Regra 264 (sem ponte) mudam o que o grafo traça.

## Contexto

O autor escolheu substituir o planejamento por um grafo hierárquico e migrar o índice existente. O diário atual superestima passos pavimentados, e o jogo ainda apresenta sintomas de conexões e cotas inadequadas sem reprodução completa. Trocar o planejador não dispensa essa reprodução.

## Decisão

1. Um grafo puro representa centro, entradas, junções, extremidades e trechos físicos. Cadeias de grau dois são contraídas preservando a geometria; circuitos recebem âncora determinística. Adjacência exige passo cardinal com desnível de no máximo um bloco.
2. Na importação, ruas confirmadas no componente do centro formam a rede principal. Componentes desconectados permanecem representados como secundários, sem ligação inventada. Novos ramais são secundários; sua geometria depende do terreno e do acesso de cada porta.
3. Dados são derivados e revalidáveis. O grafo não substitui a verdade física nem cria reservas permanentes por planejamento. O contrato espacial `ROAD_AREA` da ADR-017 permanece aplicável.
4. Persistência nova será versionada em paralelo ao índice legado, sem retirar `roads.columns` nesta entrega. Migração será idempotente, sem escrita de blocos, com tratamento explícito de chunk indisponível, corrupção e versão futura.
5. Vilas preexistentes não recebem execução física da política nova, mesmo quando descobertas depois da atualização. Somente geração efetivamente nova habilita a política; migração de vilas antigas representa suas ruas sem restaurar ou remodelar blocos.
6. Larguras: três blocos para rua principal e um para ramal; portas usam a cota do apoio da metade inferior. Preservar estruturas, minas, lavouras, água funcional e proteções do jogador.

## Origem de geração — ativação pendente

A pesquisa em Minecraft 1.21.1/Yarn build.3 confirmou que `ChunkGenerator.trySetStructureStart`
chama `StructureAccessor.setStructureStart` ao criar um start válido. `setStructureStarts`
não repete a criação quando já existe start válido. O hook de origem deve envolver essa
chamada específica no `ChunkGeneratorMixin`, sem marcar starts no construtor: a leitura de NBT
também constrói `StructureStart` e não prova geração nova.

O marcador fica na instância do start e em seu próprio NBT, sem gravar `PersistentState`
na thread de geração. `StructureStartMixin` limita-se a armazenar
o marcador e delegar leitura/gravação em `fromNbt`/`toNbt`; não cancela geração nem IA.
Ausência, conteúdo inválido ou versão desconhecida não habilitam a política física.
Conteúdo desconhecido deve permanecer opaco ao salvar. A descoberta consulta, nunca cria,
essa prova de origem. O hook de criação foi implementado. Sete GameTests verificam
persistência, cópias defensivas, rejeição de origem inválida ou futura, criação pelo
callsite Vanilla transformado e descoberta tardia de start legado sem marcação retroativa.
Esses testes passaram junto com os três cenários de migração (10/10); ativação ainda pendente.

## Entrega e limites

As primeiras etapas implementam `RoadGraph`, testes puros e o codec paralelo `roadGraphs` versão 1. O leitor preserva payload futuro sem interpretá-lo e mantém `roads.columns` legível. O codec rejeita listas de tipo incorreto e limita colunas físicas distintas, permitindo a repetição da âncora de um circuito. Nós internos exigem divisão do trecho: aceitar uma junção ou centro no interior de uma aresta impediria as rotas de alcançar suas extremidades.

`RoadGraphMigration` revalida somente chunks carregados, sem escrever blocos ou modificar
o legado. Ausência física e falta de apoio excluem a coluna; chunks desconhecidos adiam
o grafo inteiro. Índice acima do limite novo permanece pendente, sem truncamento.
Três GameTests iniciais comprovam leitura repetível, geometria, ausência de reconstrução
e adiamento. A revisão adicionou quatro reproduções: rua sob telhado, dois pisos candidatos,
rua além da janela vertical e referência estrutural remota. Todos falharam antes da correção;
o último demonstrou carregamento indireto até `STRUCTURE_STARTS` na consulta por material de bioma.

A inspeção consulta o material dentro da coluna já presente no legado, sem resolver referências
estruturais. Busca até 12 blocos abaixo do topo, incluindo o topo. Dois pisos elegíveis adiam
a publicação. Com a cota de origem legada abaixo da janela, também adia, mesmo encontrando piso superior;
não interpreta essa cobertura como remoção comprovada. A janela limita o custo desta etapa;
o índice legado permanece intacto para nova inspeção. Os quatro casos falharam antes da correção;
a rodada posterior passou 7/7, incluindo os três cenários anteriores. Uma quinta reprodução
demonstrou piso Y=80 ocultando rua Y=64; após ampliar o adiamento, passaram 8/8. O adaptador ainda não está
ligado ao ciclo de planejamento/persistência.

Ainda não há ativação do planejador ou mudança de gameplay. As etapas seguintes estão no plano da rede; a aprovação da arquitetura não declara concluídas migração, execução ou validação no jogo.

## Emenda de 2026-11-05 — o grafo é um mapa de trilhos (Regra 177)

Decisão do autor, com desenho do antes e depois. Vale em vila nova; `RoadGraph.from` e o codec seguem como estão.

- O grafo não contrai mais cada bloco de caminho: `RoadLines` afina a faixa física até a linha do meio e só ela entra em
  `RoadGraph.from`. Rua larga dá uma linha; nós a até 3 blocos se fundem; a frente de porta nunca se funde.
- **Muda:** "componentes desconectados não recebem ligações inventadas". Vão de até 2 blocos liga, e parte separada se liga
  à principal por `VillagePathPlanner`, com o trecho calçado depois; a que não se liga fica fora do grafo.
- **Muda:** grafo pode citar coluna ainda sem caminho, enquanto o construtor não a calça. Nesse intervalo a publicação de
  rua do MAE recusa, porque confere cada coluna no mundo.
- **Muda:** coluna com dois pisos fica com o de cima, em vez de adiar o grafo inteiro.
- `RoadGraphRefresh` relê a caixa da vila a cada 5 minutos; não relê com trabalho MAE ativo (`metadataAllowed`).

Desenho aprovado: `docs/superpowers/specs/2026-11-01-hierarchical-roads-and-pens-design.md`.
Plano: `docs/superpowers/plans/2026-11-01-hierarchical-roads.md`.
