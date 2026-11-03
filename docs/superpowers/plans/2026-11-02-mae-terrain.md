# MAE Fase 2 — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preparar terreno local elegível e transitável, com vegetação baixa controlada e conexão a rua comprovada, incluindo ruas posteriores à geração certificadas pelo writer MAE.

**Architecture:** Reutilizar scanner, `LotScore`, `Building`, `WorkArea`, writer e diário da Fase 1. Separar terreno e operação de rua; integrar os sete pontos de pavimentação somente em seções supervisionadas, com prova anterior à escrita e sem fallback direto.

**Tech Stack:** Java 21, Minecraft 1.21.1, Yarn 1.21.1+build.3, Fabric API 0.116.15+1.21.1, JUnit e GameTest existentes.

**Spec:** [desenho ampliado aprovado pelo autor em 2026-11-02](../specs/2026-11-02-mae-terrain-design.md).

**Estado em 2026-11-03:** implementação local e gate focado concluídos em
`codex/mae-terrain`, sobre a Fase 1 integrada pelo PR #48 (`b5706316`). Modelo
e testes focados liberados pelo autor. Passaram 37 unitários/estáticos e 48
GameTests; resultados e limitações em [MAE-Fase-2](../../technical/MAE-Fase-2.md).
Os passos RED iniciais sem registro individual preservado permanecem sem marca;
isso não significa falha atual. As regressões reproduzidas durante a revisão
estão descritas no relatório. Entrega e ativação automática não estão concluídas.

## Global Constraints

- Desenho aprovado; executar somente após cumprir os gates de branch/modelo e liberar os testes focados.
- Integrar Fase 1 por PR e criar branch a partir da `main` atualizada; sem linha sobreposta.
- Corte/aterro até dois, apoio de duas camadas, transição até seis, passagem com desnível até um e clearance de dois blocos.
- Até oito candidatos, 256 leituras e dois milissegundos por fatia supervisionada; orçamento compartilhado por mundo.
- Manter limites e proteções da ADR-044, salvo vegetação baixa e contexto de âncora somente de leitura explicitamente aprovados.
- Operação de terreno não escreve na rua, suporte ou clearance da âncora. Operação separada de rua exige plano aprovado e cadeia confirmada desde a geração.
- Corredor conecta à rua comprovada sem alterar o grafo. Só a operação de rua publica trechos fisicamente confirmados na rede existente.
- Prova por seção, bounding box original e proteção do jogador permanecem obrigatórios; ruas antigas sem certificado não ganham autorização retroativa.
- Toda superfície de circulação preparada deve ser acessível, com colisão válida e sem depender de pulo especial.
- Ampliação de `FOUNDATION_GROUND` para corredor e transição aprovada no desenho; registrar ADR e atualizar Regra 90 na implementação.
- Nenhuma escrita em save do jogador ou ativação automática. Integração legada limitada aos pontos de rua descritos no desenho.
- Reserva v2 antecede a rua. Rua e lote usam a mesma região; nenhuma importação de hashes, transferência ou sobreposição entre regiões.
- Núcleo sem Minecraft/Fabric/NBT; nenhuma dependência ou Mixin novo.

## Review Focus

- Flores na margem permanecem intactas; remover somente as incluídas no plano.
- Interrupção entre bloco, inventário e diário bloqueia; nunca repete entrega incerta.
- Envelope v1 permanece inalterado na leitura; somente `reserveTerrain` aceita promove a v2, bloqueando o writer antigo.
- Reserva com flores usa política explícita; `reserve` mantém o filtro conservador e pedidos recusados não promovem versão.
- Plano sem alteração é válido sem enviar lista vazia ao writer; não concede autorização futura.
- Transição e apoio cabem no volume autorizado; margem de inspeção não concede escrita.
- Âncora consta no grafo/índice da colônia e no mundo; consulta não chama ativação que persiste outro estado.
- Plataforma isolada ou barreira de cabeça reprova o plano, mesmo com o eixo do corredor livre.
- Edição externa na mesma seção nunca é incorporada ao confirmar uma rua legítima.
- Todos os efeitos dos sete pontos de pavimentação, inclusive corte prévio e índice, passam pelo gate antes de ocorrer.
- Trecho parcial ou publicação incerta não cria ligação concluída; intenção pendente não é repetida automaticamente.

## Tarefa 1 — Política e planejamento puro

**Arquivos:** criar `src/main/java/com/villagecolony/core/construction/model/TerrainPlan.java`,
`TerrainPlanner.java` no mesmo diretório e
`src/test/java/com/villagecolony/core/construction/model/TerrainPlannerTest.java`.
Registrar a ADR de extensão da política com número livre verificado e linkar seu índice real.

**Interfaces propostas:** `TerrainPlanner.plan(Surface sample, Request request)` retorna
`TerrainPlan`; os registros de entrada pertencem ao planejador e contêm apenas
coordenadas, alturas, apoio, impedimentos, transitabilidade e requisitos do acesso. `TerrainPlan`
contém estado de viabilidade, colunas finais, métricas e motivo de recusa.
Mapeamento de estados e itens permanece exclusivamente no Fabric.

- [x] Fixar na ADR os limites do desenho aprovado, sem atribuir aprovação a propostas rejeitadas.
- [x] Escrever cenários de plano sem mudanças, encosta, depressão, apoio insuficiente,
  caverna, âncoras incompatíveis, transição fora de área e desempate determinístico.
  Cobrir superfície isolada, acesso só diagonal, degrau com barreira de cabeça e
  todas as posições de circulação alcançáveis a partir da entrada.
  Cada teste deve afirmar os valores exatos dos limites acima.
- [ ] Executar `./gradlew.bat test --tests com.villagecolony.core.construction.model.TerrainPlannerTest`;
  registrar falha pela ausência do contrato esperado.
- [x] Implementar somente enumeração limitada, recusas e comparação lexicográfica do desenho.
- [x] Reexecutar o comando; exigir zero falhas antes de seguir.

## Tarefa 2 — Levantamento e autorização explícita de terreno

**Arquivos:** criar `src/main/java/com/villagecolony/fabric/integration/MaeTerrainPreparation.java`
e `src/gametest/java/com/villagecolony/fabric/integration/MaeTerrainGameTest.java`;
alterar `MaeProtection.java`, `MaeWorldWriter.java` e o manifesto GameTest existente.

**Interfaces propostas:** o adaptador expõe início de levantamento e continuação
supervisionada por fatia; seu resultado preparado contém `Building` original,
`WorkArea` completo, `TerrainPlan`, mudanças antes/depois e requisitos de materiais.
A nova entrada de terreno do writer consome esse resultado. A entrada atual
`approve(ServerWorld, UUID, Building, List<Change>)` mantém o contrato da Fase 1.
Criar também `reserveTerrain` com a lista fechada de plantas e política persistida;
`reserve` permanece conservadora. A reserva não autoriza escrita ou início da obra.
Receber o contexto de rua já delimitado, somente de leitura, e consultar grafo/índice
existentes sem ativar migração. Reservar continua exigindo o hash de geração íntegro.
Para rua futura, delimitar previamente o volume de terreno/rua e a política v2;
fixar a âncora na aprovação do terreno somente após confirmação e publicação da
rua nessa própria região. Não ampliar contexto durante a execução.
Separar inspeção de leitura da autorização para quebrar: rua protegida não recebe
escrita, mas pode ser lida pelo contrato explícito. Manter veto a jogador, mina,
edificação, fluidos, BlockEntity e bounding box original. Conferir X/Y/Z no grafo
e mundo; uma coluna do índice não identifica a altura correta sozinha.

- [x] Escrever GameTests para reserva explícita com flores e recusa pelo contrato antigo,
  flor somente na margem, remoção prevista, planta
  protegida, árvore/água/lavoura, grama de duas metades, chunk ausente e mudança
  do mundo entre fatias. Verificar zero escritas em todos os casos recusados.
- [ ] Executar `./gradlew.bat runGametest -PgametestOnly=MaeTerrainGameTest`;
  registrar falha pelo comportamento ausente, não por fixture sem prova de geração.
- [x] Implementar amostragem limitada, política fechada e separação entre estrutura,
  trabalho e inspeção. Reusar `WorkArea`; não registrar um `Building` com limites falsos.
- [x] Classificar apoio, colisão e circulação de toda a superfície preparada, não
  somente corredor. Reusar os modelos existentes; não persistir outro grafo de navegação.
- [x] Acrescentar testes de aprovação fora do volume, apoio e transição além da reserva,
  dois solicitantes no mesmo tick e plano sem alterações. Cobrir conexão à âncora
  comprovada sem mudança da rua; rejeitar seção divergente, estrutura original,
  caminho falso, outra colônia e grafo ausente/divergente. Comparar grafo antes/depois.
- [x] Reexecutar GameTests de terreno e segurança com
  `./gradlew.bat runGametest -PgametestOnly=MaeTerrainGameTest,MaeSafetyGameTest`.

## Tarefa 3 — Prova encadeada e integração supervisionada de ruas

**Arquivos:** `MaeWorldWriter.java`, `MaeGenerationEvidence.java`, `MaeProtection.java`,
`MaeWorkStore.java`, criar `fabric/integration/MaeRoadPreparation.java` e
`src/gametest/java/com/villagecolony/fabric/integration/MaeRoadEvidenceGameTest.java`.
Integrar `RoadPaving`, `PathPaving`, `WorkPath`, `BuilderPaving`, `SiteOpening.pavePathTo`,
`StreetGaps` e `StreetSurface.unworn`, incluindo seus chamadores e `CorridorCut` no
fluxo de acesso. Usar os stores existentes de grafo/índice, sem outra rede persistida.
Incluir os produtores de metadados `RoadNetwork.indexIfMissing`,
`RoadIndex.indexWhatWasSeen` e `RoadGraphActivation.roads`: não redescobrir trecho
supervisionado parcial ou incerto como rua concluída.

**Interface:** operação de rua tipada, aprovada explicitamente na região v2;
plano contém alterações exatas, precondições e intenção de publicação. O writer
executa e confirma cada mudança; só então publica o trecho concluído. A adaptação
das entradas não cria agendamento ou aprovação automática.

- [ ] Escrever o cenário geração/reserva/rua/terreno e confirmar falha inicial por
  falta do contrato, não por ausência de recibo na fixture. Cobrir recarga entre etapas.
- [ ] Executar `./gradlew.bat runGametest -PgametestOnly=MaeRoadEvidenceGameTest` após
  liberação dos testes; registrar a causa da falha antes de implementar.
- [x] Registrar e confirmar mudanças usando o diário e hashes da própria região.
  Nunca recapturar prova, normalizar células ou aceitar índice como evidência.
- [x] Fazer as sete entradas recusar ou encaminhar antes de cortar, limpar,
  depositar, pavimentar, adotar coluna no índice ou registrar sucesso. Nenhum
  fallback direto em seção supervisionada, inclusive fora do volume da região.
- [x] Testar cada entrada, corte anterior em `SiteOpening`, manutenção certificada,
  caminho manual/de outra colônia, edição vizinha e mudança natural não prevista.
  Recusas devem ter zero efeitos físicos, materiais e de publicação.
- [x] Cobrir intenção sem confirmação, falha antes/depois da escrita e publicação
  parcial/incerta. Não repetir efeitos automaticamente nem publicar aresta incompleta.
- [x] Testar scanner/reindexação/ativação durante obra parcial ou incerta e após
  recarga; derivar exclusões do diário, sem publicar os blocos descobertos antes da confirmação.
- [x] Serializar seções compartilhadas e manter uma obra ativa por região. Testar
  teto combinado de plano/histórico/publicação e bloqueio entre regiões sem importação.
- [x] Separar calçamento `SHAPED_GROUND` de apoio `FOUNDATION_GROUND`, preservando
  as Regras 15/69/90/111 e contagem real sem duplicação.
- [x] Demonstrar fixture aceita com rua conectada fora do bounding box original;
  se impossível com os dados atuais, registrar impedimento e voltar ao autor,
  sem relaxar proteção ou alegar acesso funcional.
- [x] Reexecutar o GameTest de prova e os de terreno/segurança após implementação.

## Tarefa 4 — Execução, materiais e compatibilidade persistente

**Arquivos:** `MaeWorldWriter.java`, adaptador de terreno, `MaeProtection.java`,
`src/main/java/com/villagecolony/fabric/work/Cuttings.java`,
`src/main/java/com/villagecolony/data/save/MaeWorkStore.java`,
`src/test/java/com/villagecolony/data/save/MaeWorkStoreTest.java` e GameTests de terreno/segurança.
Alterar também `src/main/java/com/villagecolony/core/coordination/SupplyException.java`
e `docs/RULES.md` para a ampliação aprovada; manter payload desconhecido no store.

**Interface:** leitor aceita envelopes v1/v2; só a primeira `reserveTerrain` aceita
promove para v2. Política de terreno versionada estende a reserva e o trabalho persistidos,
com finalidade das mudanças e intenção/resultado de materiais. O writer continua
único executor. Reutilizar `Cuttings.keep` para destino, expondo apenas o necessário.
Incluir tipo de operação de rua, versão da cadeia e intenção/confirmação da
publicação; contratos desconhecidos ficam opacos e bloqueados.
Persistir precondições e contexto somente de leitura da âncora, sem copiar outro
grafo autoritativo; a execução revalida destino e nunca o troca implicitamente.

- [ ] Reproduzir com testes a ausência de política de vegetação, perda de metadados
  ao recarregar e risco de repetir efeito material incerto. Nenhuma falha injetada
  pode produzir entrega ou consumo automático duplicado.
- [x] Implementar ordem de plantas/cortes/apoios, integração física de materiais,
  intenção e confirmação, verificação final e bloqueio de resultado incerto.
- [x] Cobrir alteração da âncora, retomada e tentativa de escrever em rua/suporte/clearance.
  Verificar conservação de proteção e hashes, inclusive na reserva e no final.
- [x] Registrar a ampliação aprovada de `FOUNDATION_GROUND` para superfície,
  aterro e apoio do volume MAE, incluindo corredor local e transição; sem efeito
  fora das células aprovadas. Sincronizar Regra 90, enum e README gerado.
- [x] Testar estoque cheio, baú nomeado, sobra no chão e contabilização real da
  exceção `FOUNDATION_GROUND`, inclusive execução parcial. Exigir divergência zero entre confirmado e observado
  em execução normal; incerteza após falha deve aparecer explicitamente como bloqueio.
- [x] Testar leitura de v1 sem mutação, promoção na reserva aceita e ausência de
  promoção na reserva recusada, trabalho da Fase 1
  sem discriminador, trabalho novo recarregado, recusa pelo contrato antigo e versão
  desconhecida opaca. Reexecutar `MaeWorkStoreTest` e os GameTests focados anteriores.

## Tarefa 5 — Cobertura Sentinela e entrega candidata

**Arquivos:** testes e manifesto já citados; `Sentinela/TUTORIAL.md`, `README.md`,
`STATE.md`, `TODO.md`, `CHANGELOG.md`, `gradle.properties`, relatório da Fase 2 e
`docs/archive/technical/Development-Log.md`. Registrar somente o estado comprovado.

- [x] Confirmar que `TerrainPlannerTest` entra na suite unit e `MaeTerrainGameTest`/`MaeRoadEvidenceGameTest`
  na suite gametest existentes do Sentinela; não adicionar outro runner ou executar
  a bateria completa sem autorização. Atualizar tutorial com cobertura e limitações.
- [x] Executar unitários do planejador/store e os GameTests focados do terreno e
  prova de rua/segurança; verificar arquitetura, manifesto e casos de origem conforme o diff real.
- [x] Medir duração e número de leituras/escritas das operações supervisionadas nas
  fixtures. Registrar maior duração observada sem declarar SLA ou carga real validada.
- [x] Verificar com aldeão real percurso da entrada a pontos de toda a superfície
  de circulação e à âncora, incluindo bordas e degraus. Distinguir validação local
  de comprovação da navegação de toda a vila, que permanece fora do gate desta fase.
- [x] Rodar `py -3 scripts/rules_to_readme.py --check` e `git diff --check`.
- [x] Fazer revisão da branch; documentar cenários ainda dependentes de carga/playtest.
- [ ] Se autorizada a entrega, subir a versão candidata para 0.4.10 se a base continuar
  0.4.9, gerar `remapJar`, copiar o JAR para `downloads/` e TLauncher e comparar SHA-256.
  Manter JAR fora do git. Commit/push/PR seguem o escopo autorizado pelo autor.

## Execução recomendada

Execução nativa nesta sessão, tarefa a tarefa, com uma revisão independente ao final.
As interfaces são próximas e reutilizam a fundação existente; evitar um agente por
arquivo reduz contexto duplicado. Desenho aprovado; não iniciar antes de cumprir
os gates de branch, modelo e testes previstos em `CLAUDE.md` §0.2.1–0.2.2.
