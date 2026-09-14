# Auditoria Tecnica - Village Colony

**Data:** 2026-09-14  
**Snapshot auditado:** `2a0a4b7b33173ce7d3a9052ef2f43571fe77f483` (`main`)  
**Escopo:** mod Fabric 1.21.1, Java 21, codigo, testes, recursos, persistencia,
mixins, CI/CD e o log de jogo mais recente. Nenhuma dependencia foi atualizada e
nenhuma regra de jogo foi alterada durante esta auditoria.

## Sumario executivo

O projeto tem uma base arquitetural disciplinada: o `core/` esta separado da
adaptacao Fabric, ha um unico mixin sem `@Overwrite`, a persistencia conserva o
que o mundo nao consegue recompor, e a suite cobre muitas regras de dominio e
GameTests. A cadeia ainda nao entrega, de modo confiavel, a experiencia central
da vila que continua construindo sozinha.

Ha um P0 objetivo: o CI esta vermelho no commit auditado. Um GameTest de coleta
de terra falhou no runner Linux, embora os mesmos 324 GameTests tenham passado
localmente. A causa da divergencia ainda nao foi demonstrada e nao deve ser
tratada como fato.

Os P1 explicam diretamente o relato em jogo de construcoes que nao aparecem ou
ficam pela metade: a maior parte dos lotes e recusada pela politica de solo, uma
obra que esgota a paciencia vira construcao parcial permanente, e materiais de
blueprints fora do catalogo finito podem nao gerar trabalho produtor. A mineracao
nao estava completamente parada no log: houve mineracao e entrega de blocos,
mas varios mineiros ficaram sem corte produtivo enquanto outros trabalhavam.

## Metodologia e limites

- Leitura dirigida por `STATE.md`, `CLAUDE.md`, `docs/PATTERNS.md`, `TODO.md`,
  ADRs e arquivos diretamente ligados aos sintomas.
- Grafo existente consultado para rastrear planejamento, estoque e trabalho. O
  grafo e anterior ao commit auditado; ele foi usado como indice, nunca como
  prova de comportamento atual.
- Log analisado: `%APPDATA%\\.minecraft\\logs\\latest.log`.
- CI analisado: [run 34814235426](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/runs/34814235426).
- Nao houve atualizacao de Minecraft, Fabric, Yarn, Loom ou dependencias.
- O validador local da skill `minecraft-ci-release` exige `bash`, indisponivel
  neste Windows. A revisao do YAML e a consulta ao run remoto foram feitas,
  mas esse validador especifico nao foi executado.

## Validacao observada

| Verificacao | Resultado |
| --- | --- |
| `python -m unittest discover -s tests` | 74 testes aprovados |
| `./gradlew.bat build` | aprovado localmente |
| `./gradlew.bat runGametest` | 324 GameTests obrigatorios aprovados localmente |
| GitHub Actions no commit auditado | falhou: 1 GameTest obrigatorio |
| Artefato de producao local | `village-colony-0.3.0.jar`, SHA-256 `EF0138BE7180FC47FB905C42EF7F64A8A31FE68CFCFCBCE4C07CAF72DB467231` |

## Pontos fortes

- `DependencyRuleTest` protege o isolamento do `core/`; a busca da auditoria
  nao encontrou import de Minecraft/Fabric/data em dominios do core.
- `VillagerEntityMixin` usa `@Inject` em `TAIL`, sem cancelamento e sem
  `@Overwrite`; a logica fica fora do mixin.
- `fabric.mod.json` declara Java 21, Minecraft `~1.21.1`, Fabric Loader e API;
  o icone referenciado existe. As versoes do build sao fixas.
- O pipeline roda testes Python, build e GameTests; em falha, preserva relatorios.
- A suite ja afirma protecao de blocos do jogador, minas, arvores, baus por
  profissao, materiais e retomada de varredura.
- Nao foram encontrados workers assincronos, `Thread.sleep`, executores ou
  `@Overwrite` que ameacem a thread do servidor.

## Achados

### AUD-001 - P0 - CI vermelho por GameTest nao reproduzido localmente

- **Categoria:** CI/CD e confiabilidade de testes.
- **Evidencia:** `.github/workflows/ci.yml:54`; `SurfaceGatheringGameTest.java:121`;
  [run 34814235426](https://github.com/LucasRiboldi/MOD-village-1.21.1/actions/runs/34814235426).
- **Descricao:** o runner Linux encerrou com `1 required tests failed` em
  `surfacegatheringgametest.smeltergathersdirtoutsidetheprotectedvillageradius`.
  A execucao local posterior concluiu os 324 testes obrigatorios sem falha.
- **Causa:** nao confirmada. O teste escolhe setor pelo estado carregado e repete
  ticks na mesma janela de GameTest; isso e uma hipotese de sensibilidade ao
  ambiente, nao um diagnostico final.
- **Impacto:** o CI impede uma entrega verificada e deixa o artefato do commit
  sem atestacao remota.
- **Reproducao:** executar o run indicado no GitHub; localmente, repetir o teste
  isolado e a suite completa ate obter evidencia positiva ou negativa.
- **Recomendacao:** primeiro tornar a falha observavel (setor, alvo, inventario,
  tempo e razao de inatividade no erro); depois remover a fonte de variacao ou
  corrigir o produto somente se a falha a provar. Exigir repeticao em Linux antes
  de liberar qualquer lote posterior.
- **Confianca:** alta para o CI vermelho; baixa para a hipotese de causa.

### AUD-002 - P1 - Demanda de blueprint pode nao chegar a um produtor

- **Categoria:** autonomia de recursos e construcao.
- **Evidencia:** `StructureBlueprintReader.java:282`; `WorkMaterials.java:184-224`;
  `MinecraftTypeAdapter.java:52-240`; `ResourceType.java:21-181`;
  `BuilderWork.java:742-747`.
- **Descricao:** blueprints leem IDs arbitrarios do palette Vanilla, mas as
  demandas de fundicao/coleta so seguem quando o item e convertido ao conjunto
  finito `ResourceType`. Um ID sem mapeamento simplesmente nao entra em uma
  tarefa produtora; o construtor pode ficar aguardando material que a colonia
  nao sabe pedir.
- **Causa:** a fronteira entre `ResourceId` generico do blueprint e a tabela
  fechada de producao nao possui um contrato de cobertura nem diagnostico de
  lacuna.
- **Impacto:** uma casa pode depender de intervencao do jogador, contrariando a
  autonomia e o requisito de destinar todo recurso a uma profissao.
- **Reproducao:** escolher uma estrutura Vanilla que contenha item sem
  `toResourceType`, abrir o projeto e observar ausencia de tarefa produtora.
- **Recomendacao:** definir uma politica de capacidade de materiais: cada ID de
  blueprint deve ser produzido, substituido explicitamente ou bloqueado antes
  da obra com razao legivel. Esta e uma decisao transversal de dominio e requer
  ADR antes de implementacao.
- **Confianca:** alta.

### AUD-003 - P1 - Politica de lote e o gargalo dominante de construcao

- **Categoria:** selecao de local e progresso de vila.
- **Evidencia:** `BuildSiteScanner.java:1127-1129,1235-1292`; `STATE.md` (P0.7);
  log de jogo, `03:06:00`.
- **Descricao:** de 57.584 candidatos recusados no log, 39.228 foram recusados
  por `NOT_NATURAL_GROUND` (68,1%). Apenas 725 foram obstrucoes. O scanner e
  acionado, mas sua politica encontra poucos lotes aceitaveis para a vila real.
- **Causa:** `isLotGround` aceita solo natural ou rocha nua; alteracoes do
  jogador e solo classificado como nao natural sao excluidos para preservar a
  Regra 3. A decisao pendente P0.7 ainda governa esse limite.
- **Impacto:** o planejador continua varrendo e nao abre obra; parece que a
  construcao "nao nasceu" mesmo com construtores disponiveis.
- **Reproducao:** vila com terreno modificado, estrada, construcoes ou chao nao
  classificado como natural; consultar as recusas no log.
- **Recomendacao:** fechar P0.7 antes de mudar o filtro. Depois, usar niveis de
  elegibilidade, revarredura incremental e diagnostico visual para separar
  terreno corrigivel pelo jogador de area permanentemente protegida.
- **Confianca:** alta.

### AUD-004 - P1 - Obra esgotada e abandonada nao volta quando o recurso chega

- **Categoria:** recuperacao de construcao.
- **Evidencia:** `WaitingWork.java:105-170`; `PlanRefusalsTest`;
  `ConstructionResumeGameTest`.
- **Descricao:** apos a paciencia de uma obra `WAITING_RESOURCES`, `giveUp`
  registra a construcao parcial, remove o projeto e mantem o lote ocupado. O
  proprio comentario afirma que ninguem a retoma depois.
- **Causa:** politica deliberada para evitar que uma planta impossivel bloqueie
  toda a colonia.
- **Impacto:** uma casa parcialmente construida pode nunca terminar mesmo se a
  cadeia de recursos for corrigida ou o jogador abastecer o bau.
- **Reproducao:** deixar uma obra aguardar ate expirar, adicionar o recurso ao
  bau e observar que nao ha projeto aberto para finaliza-la.
- **Recomendacao:** escolher e registrar em ADR uma politica de obra
  revisavel: retomar automaticamente sob condicao comprovada, ou deixar uma
  obra explicitamente abandonada e recuperavel por comando. A implementacao
  deve preservar a protecao do lote e impedir repeticao infinita.
- **Confianca:** alta.

### AUD-005 - P1 - Mineradores nao usam capacidade de forma sustentada

- **Categoria:** trabalho de aldeao e recuperacao de mina.
- **Evidencia:** `MineDigging.java:47-50,216-230,847-945`; log de jogo entre
  `02:56` e `03:05`.
- **Descricao:** o log mostra mineracao real (entregas de 10, 41, 68, 71 e 102
  blocos), portanto nao prova mina totalmente parada. Ao mesmo tempo, ha
  repetidas linhas `no miner cut work`, ramais sem pedra apos 64 posicoes e
  mineiros aguardando outro ramal enquanto a capacidade nao esta plenamente
  produtiva.
- **Causa:** confirmada apenas como combinacao de quota curta por busca, posse de
  ramal e recusas de alcance; nao ha prova de defeito unico no caminho.
- **Impacto:** a extracao parece desistir e a cadeia de pedra/carvao/ferro perde
  cadencia, atrasando construcoes e fundicao.
- **Reproducao:** tres ou mais mineiros em ramais com pedra esparsa, bloqueada ou
  fora de alcance; observar entrega e redistribuicao por varios ciclos.
- **Recomendacao:** criar regressao que meca ocupacao por mineiro e transfira
  ramal apos falhas repetidas; instrumentar motivo de espera, ultimo alvo e
  proxima revalidacao. So entao ajustar quota, lease ou abertura de ramal.
- **Confianca:** media para a insuficiencia observada; baixa para uma causa unica.

### AUD-006 - P2 - Testes de integracao nao fecham a divergencia com a vila viva

- **Categoria:** estrategia de testes.
- **Evidencia:** 324 GameTests locais verdes, `ColonyEnduranceGameTest`,
  `VillageStructuresGameTest`, `SurfaceGatheringGameTest` e o log de jogo.
- **Descricao:** a suite verifica muitas regras isoladas e pequenos fluxos, mas
  a divergencia CI/local e os relatos de vila real mostram falta de uma prova
  deterministica da cadeia completa em terreno e catalogo representativos.
- **Impacto:** testes verdes podem coexistir com uma vila sem obra aberta ou com
  trabalhador ocioso no mundo do jogador.
- **Recomendacao:** introduzir gradualmente um harness de vila controlada que
  observe ciclos, demandas, lotes, tarefas, inventarios e progresso de blocos;
  comecar por mineracao, fundicao e uma obra curta.
- **Confianca:** alta.

### AUD-007 - P2 - Classes de integracao concentram responsabilidades demais

- **Categoria:** manutencao e risco de regressao.
- **Evidencia:** `BuildSiteScanner.java` (1.298 linhas), `VillageDetectionHandler.java`
  (1.163), `MinerWork.java` (1.094), `MineDigging.java` (1.034),
  `CraftingWork.java` (914) e `BuilderWork.java` (859).
- **Descricao:** as maiores classes misturam descoberta, politicas, estado
  transitorio, efeito no mundo, log e recuperacao. Isto torna pequenos reparos
  caros e encobre contratos entre etapas.
- **Impacto:** risco crescente de regressao e auditoria lenta; nao ha evidencia
  de falha funcional causada apenas pelo tamanho.
- **Recomendacao:** sem refatoracao ampla. Quando cada P1 for corrigido, extrair
  somente a responsabilidade diretamente tocada, mantendo a API e os testes.
- **Confianca:** alta.

### AUD-008 - P2 - Checagem de espacos no CI nao compara mudanca alguma

- **Categoria:** CI/CD.
- **Evidencia:** `.github/workflows/ci.yml:42`.
- **Descricao:** `git diff --check` em checkout limpo nao recebe intervalo de
  commits e, portanto, normalmente nao examina a alteracao do push ou PR.
- **Impacto:** erros de whitespace podem escapar apesar do passo ter nome de
  validacao.
- **Recomendacao:** para push, comparar `HEAD^..HEAD`; para PR, comparar a base
  do merge. Manter uma cobertura segura para o primeiro commit.
- **Confianca:** alta.

### AUD-009 - P3 - Diagnostico existe no log, nao para o jogador

- **Categoria:** observabilidade.
- **Evidencia:** `LotRefusals`, `IdleLog` e `MineDigging`; busca nao encontrou
  registro de comando de diagnostico em `src/main/java`.
- **Descricao:** o log informa recusas e inatividade, mas o jogador nao ve a
  razao nem o proximo lote, alvo ou material aguardado dentro do jogo.
- **Impacto:** correcao de terreno e relato de falhas dependem de leitura de log.
- **Recomendacao:** apos os P0/P1, criar comando administrativo com particulas
  temporarias e resumo textual, sem interface cliente obrigatoria.
- **Confianca:** alta.

## Priorizacao do ciclo controlado

1. **Bloqueador de entrega:** AUD-001. Sem CI verde, nenhuma correcao seguinte
   tem evidencia remota suficiente.
2. **Bloqueador de obra:** AUD-003 e a decisao P0.7 sobre terreno aceito.
3. **Persistencia de obra:** AUD-004, que requer ADR antes de codigo.
4. **Cadeia material:** AUD-002, tambem requer ADR por atravessar dominio e
   adaptador.
5. **Cadencia de mineracao:** AUD-005, com regressao e telemetria antes de
   alterar parametros.

## Skills usadas

| Skill | Uso |
| --- | --- |
| `fabric-development` | build, estrutura Fabric, metadados e mixins |
| `minecraft-code-research` | rastreamento de sistemas Vanilla/Fabric antes de conclusoes |
| `minecraft-villager-systems` | trabalho, alvos, horarios e comportamento de aldeoes |
| `minecraft-testing` | leitura de GameTests e criterio de regressao |
| `minecraft-ci-release` | revisao de pipeline, artefatos e versoes |
| `graphify` | indice de relacoes entre planejamento, estoque e trabalho |

Nao se aplicaram `minecraft-multiloader`, plugins de servidor, comandos,
datapacks, world generation, resource pack, WorldEdit e Essentials: o escopo e
um mod Fabric de jogabilidade, sem entrega desses subsistemas.

## Decisoes pendentes do autor

- Fechar P0.7: quais terrenos alterados pelo jogador podem tornar-se lote sem
  violar a Regra 3.
- Escolher a politica de retomada de obra parcial (AUD-004).
- Aprovar a politica de cobertura de materiais de blueprint (AUD-002).

O plano de execucao controlada esta em
`docs/superpowers/plans/2026-09-14-controlled-audit-cycle.md`.
