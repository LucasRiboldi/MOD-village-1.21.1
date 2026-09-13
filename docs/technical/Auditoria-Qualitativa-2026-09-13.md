# Auditoria qualitativa do projeto — 2026-09-13

## Veredito

O projeto tem boas fronteiras de arquitetura e uma bateria de testes ampla para um mod Fabric: na verificação desta sessão passaram 798 testes unitários, 312 GameTests e 74 testes Python. Isso não resolve a divergência mais importante: no playtest de 09-13, o autor ainda observou a casa parada e o mineiro sem trabalhar. O defeito exato desse estado não foi isolado; portanto não considero a construção nem a mineração aprovadas em jogo.

Não encontrei evidência para uma correção de gameplay segura sem reprodução. A correção de `structure_void` elimina uma exigência falsa do blueprint e tem regressão específica, mas o teste novo só valida o catálogo de materiais da casa média. Ele não prova a conclusão da obra real. Para a mineração, `MineMarks`, o desvio ao encontrar fluido e `MineFlooding.seal` existem no código; também há testes para essas regras. O fluxo completo da galeria no mundo do autor ainda precisa ser observado.

## Escopo e método

Fiz varredura da árvore de produção, testes, recursos, configuração, ADRs e documentação viva: 176 classes Java de produção, 78 classes de teste unitário, 35 arquivos de GameTest e 6 recursos. A leitura aprofundada cobriu limites `core`/Fabric/save, ciclo de tick, atribuição e execução de trabalho, mineração, construção/retomada, persistência e lifecycle. Usei Graphify como mapa de relações e confirmei as conclusões no código e nos testes. O histórico extenso foi consultado por símbolo, não lido integralmente, conforme as regras do repositório.

As alterações locais em `StructureBlueprintReader` e `BlueprintReaderGameTest` foram lidas e preservadas. A auditoria não substituiu nem reescreveu o relatório técnico já existente.

## Achados

### P0 — Resultado de jogo ainda não aceito

**Evidência:** a sessão de 09-13 registra casa parada e mineiro parado; lenhadores e fazendeiros foram vistos funcionando. `StructureBlueprintReader` agora ignora `structure_void`, e o GameTest novo garante que esse bloco não entra no BOM. Essa prova é estreita e não demonstra retomada e conclusão de uma casa em andamento. Para mineração, os testes cobrem escada de recusas, desvios e selagem de fluido, mas a documentação registra que a arena de mineração não hospeda uma galeria completa.

**Risco:** os sintomas centrais do produto continuam reproduzidos pelo autor, sem assinatura de log/código suficiente para escolher uma correção. Alterar prazo, prioridade ou geometria sem identificar a fase que parou pode mascarar o defeito ou regredir outro caminho.

**Próximo passo:** na próxima sessão, registrar a identidade do jar, a última mensagem de progresso e o estado do projeto/mina no ponto da parada; reproduzir e associar a evidência ao worker, alvo, tarefa e ramo. Acrescentar o cenário de retomada real da construção e a galeria à bateria antes de declarar resolvido.

### P1 — E45: geometria do mineiro no limite de profundidade

`Mine.deepenIfEveryArmIsDone()` reinicia os ramais no limite, mas `MineShaft.turned()` preserva `descent`; a hélice de `positionAt()` é calculada a partir dessa orientação. Assim, girar `gallery` não muda os degraus da hélice. O teste de rotação verifica a escada estável e a galeria orientada, mas não prova que um obstáculo na descida passa a ter rota alternativa. `MineSave` usa `SHAPE_VERSION = 4`, então a alteração também afeta saves existentes.

**Decisão:** não implementei mudança de geometria. Separar orientação da boca e rota, decidir compatibilidade/migração e fixar comportamento exige ADR e aprovação do autor conforme as regras de arquitetura.

### P1 — Lacunas entre bateria e comportamento sustentado

`TODO.md` identifica E41 (degradação após muitos ciclos) e E42 (impasse entre profissões). A bateria atual verde é uma fotografia de regressão, não uma prova de operação prolongada nem de que uma demanda inacessível de fazenda não bloqueia uma casa viável. Persistência possui cobertura, mas salvar/carregar estado não equivale a continuar o trabalho real após despertar.

Prioridade recomendada: GameTest de ciclo longo por profissão; depois, cenário E42 descrito no TODO, com lote de roça fora de alcance e uma casa disponível; por fim, exercício de salvar, recarregar, reativar e observar a obra/mina retomando em mundo de jogo.

### P1 — Estado documental E44 e KF-001 estava falso ou misturado

O código já contém `MineMarks`: escada de prazos, recusa por bloco/região e integração em busca e fronteira; há unitários e GameTests correspondentes. A descrição de “mina sem equivalente” em `STATE.md`, `TODO.md`, `RULES.md` e `PATTERNS.md` estava obsoleta. Atualizei os documentos vivos para indicar implementação presente e validação em jogo pendente. O bloco isolado que fica para trás em certos túneis é uma escolha documentada para evitar recuo infinito do cursor; não é prova de que o sintoma de jogo esteja resolvido.

Também corrigi a mistura em KF-001. A quota de uma busca por tick é realmente compartilhada entre jobs de mineração (e há quota semelhante no lenhador), sendo um possível limite de vazão/fairness em produção. Porém, o registro detalhado de `known-failures.md` mostra que a instabilidade do teste vinha da tarefa ser reservada novamente pelo ciclo de 600 ticks antes da asserção. Não há base para dizer que a quota global causou aquele flake ou que torná-la “por colônia” o corrige.

### P2 — Documentação de arquitetura e governança envelhecida

- O Javadoc de `VillageColonyMod` ainda diz que tarefas não são geradas e que projetos de construção não são persistidos. O fluxo atual cria tarefas, e `ColonySavedData`/`ServerLifecycleHandler` salvam e restauram projetos.
- O Javadoc de `ProfessionType` ainda atribui capacidades à futura TASK-013, embora o sistema de capacidades esteja presente.
- `CLAUDE.md` declara o MVP completo e verificado em jogo, apesar dos relatos atuais; também manda ler §§1–2, que não aparecem no arquivo atual, e termina com um fence `text` aberto. `AGENTS.md` aponta para essas seções.
- `docs/RULES.md` tinha um fence `markdown` solto envolvendo o documento inteiro. Corrigi esse fence e retirei E44 da lista de decisões pendentes.
- `docs/technical/Testing-Strategy.md` define níveis genéricos Unit/Integration/Minecraft World, mas não descreve o source set/runner `runGametest`, seus limites ou como separar evidência automatizada de validação manual. Não existe task Gradle de integração separada nomeada no fluxo atual.
- A formulação de P0.7 em `TODO.md` (“decidido pelo número”) pode ser lida como decisão tomada, embora a tabela e `STATE.md` digam que a decisão do autor ainda está pendente.

Os Javadocs e o ponto canônico `CLAUDE.md` não foram alterados neste lote: precisam de atualização coordenada para não reescrever conteúdo local do autor nem inventar as seções de workflow ausentes.

### P2 — CI/CD (Categoria 1.9 mantida)

Consultei a skill `minecraft-ci-release` e preservei a categoria 1.9. O workflow atual tem bons fundamentos: actions fixadas por SHA, Java 21, testes Python, `build`, `runGametest`, artefato do jar e relatórios em falha. Não há publicação de GitHub Release; o artefato manual pode ser uma escolha válida do projeto.

O passo `git diff --check` roda após checkout limpo no runner. Sem buscar/comparar a base do PR, ele verifica a árvore de trabalho vazia, não o whitespace introduzido pelo diff do PR. Recomendo torná-lo PR-aware ou removê-lo em favor de uma verificação que examine explicitamente as mudanças.

### P2 — Concentração de responsabilidade e vazão

Dez classes de produção excedem a diretriz local de 500 linhas. Os maiores focos incluem `BuildSiteScanner` (~1087), `VillageDetectionHandler` (~1009), `MinerWork` (~907), `MineDigging` (~887) e `BuilderWork` (~745). Isso aumenta custo de revisão e risco de regressão, mas não justifica divisão mecânica: extrair por responsabilidade depois de testes que fixem os contratos.

`MinerWork.tick()` concede uma busca nova por chamada do tick e compartilha essa vaga entre os jobs iterados. É uma contenção real, embora não explique KF-001. Medir espera por alvo, idade do job e competição entre colônias antes de alterar a quota; a ordem do mapa pode afetar quem recebe a vaga primeiro.

## Pontos fortes

- `core` independente de Minecraft/Fabric e regra de dependências verificada por teste; exceção de coordenação explícita.
- Camada Fabric concentra a adaptação ao mundo; sem `@Overwrite`, com um mixin estreito e delegação para os serviços de trabalho.
- Eventos de morte/conversão liberam tarefas, alvos, registros de trabalho e baús; shutdown limpa estado estático e salva.
- `ColonySavedData` deriva de `PersistentState` e cobre colônias, trabalhadores, projetos, minas e cursores declarados, com testes de leitura/restauração.
- Regras de recurso físico, chunks carregados, salvamento do que o mundo não guarda e versões de dependências fixas estão refletidas em código, testes e ADRs.
- A suíte mistura lógica isolada e GameTests Fabric, e as correções recentes acrescentaram testes de regressão em vez de depender só de inspeção.

## Lote executado e próximos passos

**Executado neste lote documental:** atualização do estado E44 para “implementado, falta prova em jogo”; correção da causa atribuída a KF-001 e da posição real da quota global; remoção da contradição correspondente em `RULES.md` e `PATTERNS.md`; fechamento do fence indevido de `RULES.md`. Nenhuma lógica Java ou geometria foi modificada.

**Aguardar revisão do autor antes do próximo lote:** primeiro, reproduzir a casa/mineração com evidência de log e jar; depois deliberar E45 e sua ADR. Não aplicar alteração de geometria ou regras de lote automaticamente. A skill `minecraft-ci-release` e a categoria 1.9 foram mantidas.

## Complemento após o playtest — 2026-09-13

O log revisto pertence ao JAR antigo instalado antes da correção de
`structure_void`; portanto, ele não valida a versão local atual. O relato
visual de terra no chão também não vem acompanhado de bloco/posição no log.
Ainda assim, a inspeção encontrou uma regra independente e reproduzível:
`BuilderWork.isShapedFromTheGround` aceitava toda `BlockTags.DIRT`, e
`placeOne` assentava esses blocos sem retirar material do estoque.

Foi acrescentado `aHouseNeedsStockForDirt`, que falhou antes da alteração
com a terra colocada e passou depois. A regra agora reserva a colocação sem
estoque para farmland, água, `dirt_path` e cultivos; terra comum exige item
no baú. Validação: 313 GameTests e `build` passaram. A obra real ainda
precisa ser observada em jogo; isso corrige a regra confirmada, mas não
prova que cada bloco de terra visto no relato veio deste caminho.

Na mineração, o log confirma repetição da frente que fecha sem espaço para
ficar, seguida da reabertura da mesma hélice. Não alterei E45: a rota deriva
de `descent`, enquanto `turned()` só gira a galeria; uma rota alternativa
persistida precisa de ADR para definir geometria e migração do save versão
4. Este é o checkpoint para revisão do autor antes de outro lote de gameplay.

## Referências principais

- Estado e fila: `STATE.md`, `TODO.md`, `docs/RULES.md`, `docs/PATTERNS.md`.
- Mineração: `MineMarks`, `MineDigging`, `MineFrontier`, `MineFlooding`, `MinerWork`, `MineShaft`, `MineSave`, `MineMarksTest`, `MinerGameTest`.
- Construção: `StructureBlueprintReader`, `BlueprintReaderGameTest`, `ConstructionPlanner`, `BuilderWork`, `ConstructionResumeGameTest`.
- Ciclo e persistência: `VillageDetectionHandler`, `ServerLifecycleHandler`, `VillagerLifecycleHandler`, `ColonySavedData`.
- Testes e release: `build.gradle`, `.github/workflows/ci.yml`, `docs/technical/Testing-Strategy.md`, `docs/behavioral-tests/known-failures.md`.
- Referência Fabric consultada: [testes automatizados](https://docs.fabricmc.net/develop/automatic-testing), [eventos](https://docs.fabricmc.net/develop/events) e [Saved Data](https://docs.fabricmc.net/develop/serialization/saved-data). As páginas atuais documentam Fabric 26.2; foram usadas para princípios gerais de teste, eventos e persistência, não como referência de API específica para 1.21.1.
