# Rede hierárquica — plano de implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** substituir o planejamento de ruas sem perder o índice espacial nem modificar vilas antigas.
**Architecture:** grafo puro de nós e trechos revalidados pelo mundo. Adaptadores Fabric confirmam entradas, terreno e execução; persistência mantém compatibilidade legada.
**Tech Stack:** Java 21, Fabric Minecraft 1.21.1, JUnit e Fabric GameTest existentes.
**Spec:** ../specs/2026-11-01-hierarchical-roads-and-pens-design.md — aprovado pelo autor em 01-11.

## Global Constraints

- Rua principal de três blocos; ramal de um; deslocamentos X/Z, desnível máximo de um bloco.
- Não executar novas ruas em vilas preexistentes. Descoberta não prova geração nova.
- Migração sem escrita de blocos; `roads.columns` legado preservado para retorno.
- Sem dependências novas; `core/` não importa Minecraft nem Fabric.
- Build de referência aprovado em 01-11, com tarefas existentes UP-TO-DATE.

## Review Focus

- Testes combinados podem alterar observador global ou região de outro cenário: tarefa 1.
- Rua removida pelo jogador não pode reaparecer pela migração: tarefa 3.
- Versão futura e chunks indisponíveis não equivalem a índice vazio: tarefa 3.
- Porta superior e terreno mais alto ao lado não podem deslocar apoio da entrada: tarefa 4.
- Vila antiga descoberta depois da atualização continua protegida: tarefa 4.

### Task 1: tornar explícita a causa da falha combinada de fuga

**Files:** `src/gametest/java/com/villagecolony/fabric/work/PenEscapeGameTest.java` e apenas o cenário contaminante identificado pela reprodução.
**Interfaces:** consome o tique real de `PenEscape`; produz cenário de saída física pelo portão, sem condução manual.

- [ ] Repetir `runGametest -PgametestOnly=PathNetwork,PenEscape,FencedIn,ColonyPathing`, guardar saída e identificar falha pela posição, chão e condição de observação.
- [ ] Reduzir a combinação até explicar o contraste com `-PgametestOnly=PenEscape`. Adicionar asserção precoce que identifique a contaminação, antes de alterar montagem ou limpeza.
- [ ] Corrigir somente a fronteira identificada. Não desabilitar IA, não teletransportar para passar, não afrouxar exigência de travessia.
- [ ] Rodar combinação e cenário isolado. Documentar causa e prova; se falhar por causa não explicada, não declarar estabilizado nem avançar integração.

### Task 2: grafo hierárquico puro

**Files:** criar `core/construction/model/RoadGraph.java`, `RoadGraphTest.java` no pacote equivalente de `src/test`; criar ADR-043 após confirmar numeração livre.
**Interfaces:** `RoadGraph.from(Set<ColonyPos> confirmed, ColonyPos hub, Set<ColonyPos> entrances)` produz nós de centro/entrada/junção/extremidade e trechos ordenados de colunas com classe principal/secundária. Dados imutáveis; apenas adjacência cardinal com diferença vertical ≤1.

- [x] Testar em vermelho: diagonal não conecta, diferença Y=2 não conecta, junção em T preservada, entrada interna divide trecho, curva não perde geometria, circuito sem extremidade preservado, entrada desconectada não fabrica rua, ordem determinística.
- [x] Implementar contração de cadeias de grau dois, preservando centro, entradas, junções e geometria completa. Classificar como principal trechos do componente do centro que já eram ruas confirmadas; ramais novos recebem classe secundária na execução.
- [x] Executar `test --tests '*RoadGraphTest'` e unitários do planejador existente. Registrar desenho na ADR e declarar que ainda não há integração.

### Task 3: migração do índice

**Files:** `data/save/RoadIndexSave.java`, `ColonySavedData.java`, novo codec do grafo e testes de persistência correspondentes.
**Interfaces:** leitores legados continuam recebendo `ColonyRoads`; leitor novo distingue legado, atual, futuro e inválido. Grafo só usa colunas confirmadas por adaptador Fabric; desconhecido fica pendente.

- [ ] Criar testes vermelhos para ida/volta legada, repetição idempotente, colônia desconhecida, corrupção, versão futura preservada e grafo parcialmente validado.
- [ ] Adicionar formato versionado paralelo sem retirar `roads.columns`. Não publicar grafo não validado. Não sobrescrever payload de versão futura.
- [ ] GameTests comprovam que carregar/migrar não escreve blocos, chunk indisponível não é apagamento e rua removida não é reconstruída.
- [ ] Rodar unitários do codec e GameTests de save; validar leitura pelo formato antigo.

### Task 4: integrar planejamento e execução

**Files:** `PathLinks`, `PathPaving`, `RoadNetwork`, `RoadIndex`, `VillagePathPlanner`; adaptador de origem de geração em `fabric/` localizado antes da edição. Testes novos nos respectivos pacotes.
**Interfaces:** `PathLinks` planeja ramal de entrada ao componente alcançável do centro; `PathPaving` só registra escrita real; grafo não vira reserva permanente antes da obra.

- [ ] Criar GameTests vermelhos para porta inferior/superior, rua desconectada, apoio, proteções, invalidação de trecho e origem antiga/nova.
- [ ] Executar grafo e A* local por trecho, preservando invariantes e cadência; invalidar apenas conexões afetadas. Corrigir contagem PAVED com prova por diferenças reais do mundo.
- [ ] Provar que vila antiga, inclusive descoberta tardiamente, não recebe a execução nova; testes de vila nova exercitam integração física inteira.
- [ ] Rodar build, unitários, GameTests focados e bateria completa; comparar custo com a mesma entrada. Atualizar ADR, índices, regras, STATE, TODO, README e registro de desenvolvimento.
- [ ] Commit atômico por tarefa concluída; depois de cada feature commitada, copiar e conferir SHA-256 do JAR local conforme instrução do autor. Não publicar antes dos gates.

## Registro de execução

- 01-11: desenho aprovado; execução local escolhida pelo agente para manter interfaces compartilhadas no mesmo contexto, sem novo pedido de aprovação.
- Build de referência: BUILD SUCCESSFUL, 3 s; testes UP-TO-DATE, não contados como execução nova.
- Task 1: montagem alinhada passou 4/4 isolada e combinada. Bateria combinada 15/16: falha no desvio de água em ColonyPathing, ainda investigada. Corrida de saída antes da inspeção demonstrada; causa histórica exata não comprovada.
- Planos de currais são posteriores à estabilização e independentes da migração da rede.
- Task 2: sete testes do grafo e doze do planejador passam. Build completo com codec e extração de RoadGraphStore passou: 1.681 testes JUnit, zero falhas.
- Task 3, parcial: sete testes do codec novo e os testes do índice legado passam. Os casos de lista com elementos de tipo incorreto e circuito com 6.000 colunas falharam antes da correção e passaram depois. Reconstrução e validação física ainda não foram integradas; não marcar a migração como concluída.
- Rotas: `RoadGraphRoutes` calcula distâncias ao centro por Dijkstra nos trechos contraídos e expande apenas colunas alcançáveis. Quatro testes falharam com retorno vazio e passaram após a implementação; rodada com os sete testes do grafo passou 11/11. Sem chamada no planejador de gameplay.
- Task 3, inspeção física: três GameTests de `RoadGraphMigration` passaram junto com cinco testes de persistência de origem (8/8). Remoção de rua, apoio ausente e chunk desconhecido não provocam escrita ou reconstrução. Ainda falta publicar a migração no ciclo operacional.
- Task 1, experimento espacial: o mesmo cenário com apoio físico passou em três posições, inclusive coordenadas altas. Coordenadas isoladas não explicam a falha combinada; causa continua aberta.
- Task 4, reprodução: `PathNetwork` executou seis cenários; quatro existentes passaram e os dois novos falharam pela causa esperada. Monte em frente à entrada deslocava o primeiro piso um bloco acima do apoio da porta; chão sem apoio retornava sucesso e era pavimentado. Log `village-entry-support-red.log` no diretório temporário. Após ancorar a entrada e validar apoio antes das escritas, a rodada passou 6/6 (`village-entry-origin-green.log`). A montagem antiga recebeu apoio físico; o poço manteve três blocos de profundidade. Integração hierárquica continua pendente.
- Fronteira arquitetural: `aMixinOnlyDelegates` falhou com quatro dependências de `fabric.world.VillageOrigin`; passou após mover o helper para `fabric.integration`. Nenhuma ampliação da whitelist.
- Revisão completa posterior executou 1.689 testes e revelou quatro falhas nos novos casos de coerção de versão e múltiplos centros do codec (`village-foundation-build-review.log`). Após corrigir tipo exato da versão e centro único, codec e legado passaram 18/18 (`village-roadgraph-codec-green.log`).
- Nova rodada `build runGametest` passou: 1.689 JUnit sem falhas e 29/29 GameTests combinados (`village-reviewed-foundation-combined.log`). Essa evidência antecede os novos controles de migração, diário e terreno externo; não encerra a causa da intermitência da água.
- Deliberação de execução: correções locais de entrada, apoio e diário podem ser provadas antes da integração operacional. A tarefa 1 continua bloqueando a ativação do grafo e dos novos currais.
- Origem: rodada expandida passou 10/10 (sete de origem e três de migração), incluindo o callsite Vanilla real e descoberta tardia. README gerado continua consistente com `docs/RULES.md`.
- Diário: `theJournalCountsOnlyTheSingleBlockActuallyChanged` falhou com duas ações `PAVED`, ambas com quantidade 3, apesar de apenas um piso mudar (`migration-journal-water-red.log`). Correção compara o estado antes/depois e registra uma unidade do material efetivo; PathNetwork passou 7/7.
- Migração: quatro testes novos falharam pela causa esperada, incluindo referência remota realmente indisponível em STRUCTURE_STARTS (`migration-red-valid-reference.log`). Inspeção agora aceita rua sob teto, adia pisos ambíguos ou cobertura insuficiente e não consulta referências estruturais para reconhecer material de rua legado. Rodada posterior passou 7/7; integração operacional permanece pendente.
- Controle A/B/A: após assentamento físico, consultas na mesma entidade, posição, penalidades e tique retornaram 31 nós secos nas três fases. A faixa externa de vinte blocos não causou diferença nessa montagem. Não reproduz toda a geometria histórica, que tinha água em duas alturas; a causa permanece aberta.
- Revalidação posterior: `build runGametest -PgametestOnly=PathNetwork,VillageOrigin,RoadGraphMigration,PenEscape,FencedIn,ColonyPathing,WaterTerrainControl` passou, com 35/35 GameTests (`village-reviewed-migration-build.log`). JUnit permanece com 1.689 testes, zero falhas; não equivale à bateria completa de GameTests.
- Segunda revisão: piso superior Y=80 ocultava rua legada Y=64 fora da janela. Oitavo cenário falhou antes da correção e passou depois; migração 8/8 (`migration-upper-floor-red.log`, `migration-upper-floor-green.log`). Qualquer janela acima da origem legada adia a publicação, inclusive quando encontra candidato.
- Codec: dois novos testes falharam porque nó interno era aceito sem dividir a aresta (`village-unsplit-node-red.log`). A validação agora recusa essa topologia antes de publicar ou marcar o store como alterado. Os treze testes do codec passaram na validação completa posterior.
- Validação completa: `build runGametest` passou em 2min17s, com 1.691 testes JUnit sem falhas e 977/977 GameTests (`village-hierarchy-full-validation.log`). Inclui os oito cenários de migração e os dois controles de terreno. A falha histórica de água não reapareceu; o gate de integração continua aberto.
- Deliberação posterior do autor: manter o bloqueio da integração e concluir primeiro o diagnóstico da água, mesmo após a bateria completa verde. Próximo experimento compara a geometria original sem o fechamento adicional dos controles anteriores.
- Entrega parcial autorizada depois do encerramento dos testes: commit, push, merge e JAR local. Publicar as fundações e correções locais não ativa o novo planejador nem os novos currais; o bloqueio de gameplay permanece. Últimos três controles focados passaram sem reproduzir a falha. `remapJar` e identidade das três cópias locais conferidos, sem reabrir a bateria.
- Água: geometria histórica com água em duas alturas também retornou rota seca no controle (`water-budget-control.log`, 2/2 cenários de controle). Como `bad n7` não apareceu, os orçamentos maiores não foram consultados; a hipótese de orçamento permanece inconclusiva. Diagnóstico da rota original agora registra custos g/f/h e comprimento durante nova execução combinada.
