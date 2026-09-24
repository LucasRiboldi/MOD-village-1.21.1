# Auditoria técnica do projeto

**Data:** 2026-09-21
**Projeto:** Village Colony, Fabric para Minecraft 1.21.1
**Branch:** codex/bighousemod
**Escopo:** código, arquitetura, profissões, regras de jogo, estruturas,
persistência, testes, documentação, CI e artefato.

## Sumário executivo

O projeto tem uma base real e bem protegida: separa core de fabric, usa o
mundo como fonte de verdade, possui GameTests para as regras recentes de lote,
fundação, mina, viveiro e cancelamento, e o build de Java passou.

O problema imediato é objetivo e reproduzível: a bateria atual de GameTests
executou 397 casos e falhou em
FarmPlanGameTest.thenextturnafterahouseisnonresidential. A regra de
alternância de construções ainda não está comprovada. Enquanto essa falha
existir, o projeto não deve ser tratado como pronto para release.

Também há uma lacuna entre o que já foi implementado e o que foi confirmado em
um save: BigHouseMOD, reparo cíclico, viveiro, frente arenosa, seleção de lote
e fluxo de cancelamento ainda precisam de playtest final. Isso é diferente de
uma falha confirmada; a documentação agora separa as duas coisas.

## Como a varredura foi feita

Foram aplicadas as práticas das skills de graphify, fabric-development,
minecraft-code-research, minecraft-villager-systems, minecraft-testing,
minecraft-ci-release e systematic-debugging.

- Leitura dirigida de STATE.md, CLAUDE.md, docs/PATTERNS.md, TODO.md, ADRs e
  documentação de testes.
- Mapeamento Graphify atualizado: 6.551 nós, 21.930 relações e 340
  comunidades; não foram encontrados ciclos de importação Java.
- Inspeção de ProfessionType, ProfessionRegistry, ProfessionAssigner,
  HousePlans, BuildSiteScanner, ConstructionPlanner, BuilderWork,
  BigHouseFoundation e recursos NBT.
- Execução local de unitários, build, GameTests e testes Python.
- Conferência dos dois JARs e do SHA-256.
- Revisão dos três READMEs do projeto; o README interno de avaliação de skills
  permanece um documento metodológico separado, não um manual do mod.

## Inventário atual

| Área | Quantidade ou estado |
|---|---|
| Java de produção | 207 arquivos em src/main/java |
| Testes unitários | 103 classes/suítes |
| GameTests | 49 classes-fonte |
| Markdown versionado | 139 arquivos fora de build/ e graphify-out/ |
| Minecraft | 1.21.1 |
| Java | 21 |
| Mod | 0.3.0 alpha |
| Import cycles Java | nenhum encontrado pelo grafo |
| JAR de downloads/ e build/libs/ | mesmo SHA-256: 983638B6FE18D5814B4BC5A5192FBA23A0214FE31B59B6BA642B4A9B773DF81B |

## Profissões e nomenclatura

### Funções operacionais

1. MINER: mineração, coleta de pedra, areia e minério.
2. LUMBERJACK: derrubada de árvores, madeira e apoio ao viveiro.
3. MASON: materiais de pedra e alvenaria. É o equivalente atual ao pedido de
   ferreiro; não há uma profissão Vanilla nova para justificar outro enum.
4. SMELTER: fornalha e coleta superficial sob demanda.
5. CARPENTER: madeira processada e materiais derivados.
6. FARMER: lavoura e plantio de árvores.
7. BREEDER: lã e materiais de origem animal; o executor ainda é ShepherdWork.
8. BUILDER: reserva, reparo e construção.

SHEPHERD é uma entrada legada preservada para saves antigos e é normalizada
para BREEDER. MANUFACTURER aparece em documentos históricos, mas não existe no
enum atual nem na implementação. Essa foi uma inconsistência documental
corrigida nos READMEs principais.

### Fundação da vila

ProfessionAssigner.FOUNDATION_ORDER contém seis titulares: MINER, LUMBERJACK,
MASON, SMELTER, BREEDER e BUILDER. FARMER e CARPENTER entram no crescimento
normal e não ocupam os dois conjuntos removidos da BigHouseMOD. Essa regra é
posterior à formulação anterior de oito profissões dentro da casa e está
registrada em ADR-020.

## Regras consolidadas

### Fundação

- Toda vila adotada cria uma BigHouseMOD uma vez.
- A Vanilla continua existindo sem edição.
- A casa própria tem seis camas e seis baús distintos, com cama HOME para os
  seis titulares fundacionais.
- Agricultor e carpinteiro continuam ativos no registro, na atribuição e nas
  tarefas; somente seus conjuntos de cama/baú ficam fora da casa.
- A BigHouseMOD não participa do catálogo profissional e ignora cancelamento
  por Tocha das Almas.

### Produção

- Inventários e materiais vêm de blocos e itens reais do mundo.
- Demandas são abertas quando há trabalho que precisa do recurso.
- Receitas são consultadas no jogo.
- O mineiro preserva construções e trabalha com avanço lento quando areia ou
  cascalho criam bloqueio novo.
- A boca da mina deve estar seca, acessível, distante da vila e, quando
  possível, direcionada a terreno alto.
- Agricultor e lenhador mantêm até dez árvores do bioma em viveiro marcado,
  usando rebento e terra enraizada.

### Construção

- A seleção profissional usa apenas whitelist Vanilla por bioma.
- A BigHouseMOD nasce pela fundação e nunca é escolhida como obra comum.
- A primeira obra é uma casa.
- Casas e obras não residenciais são intercaladas; o tipo não residencial não
  pode repetir o anterior.
- O lote deve ser acessível, ter apoio natural válido e não intersectar
  construções, estruturas Vanilla, blocos do jogador ou projetos pendentes.
- A varredura cobre a área inteira e uma janela livre absoluta de 25 blocos
  acima dela.
- A conclusão do último bloco encerra o projeto; não deve reiniciá-lo.
- Uma construção incompleta é revisada ciclicamente antes de uma obra nova,
  mas uma tentativa sem progresso cede a vez para não congelar a vila.
- Tocha das Almas dentro de obra profissional cancela zona, tarefas e registro
  parcial; a fundação fica protegida.

## Erros confirmados

### P0 — alternância de obras falha no GameTest

Evidência: execução de runGametest --rerun-tasks em 2026-09-21.

Falha: FarmPlanGameTest.thenextturnafterahouseisnonresidential.

Impacto: o contrato casa -> não residencial A -> casa -> não residencial B
não está garantido. A vila pode abrir uma casa consecutiva e violar a regra de
planejamento pedida pelo autor.

Próxima ação: reproduzir com uma colônia e estado de planos isolados, verificar
a transição persistida do tipo anterior, criar uma regressão mínima e corrigir
apenas depois de demonstrar a causa.

### P1 — instabilidade histórica da fixture de entidade distante

known-failures.md registra 3 falhas em 15 execuções do cenário do fundidor,
aproximadamente 20%, com a entidade ausente do ServerWorld entre o spawn e o
primeiro tick. A falha é do cenário antes do comportamento produtivo ser
exercitado, mas reduz a confiabilidade da rede de testes.

Próxima ação: registrar o setor devolvido por farthestLoadedSector,
coordenadas, chunk carregada e estado da entidade antes de alterar timeout ou
produção.

### P1 — validação de jogo ainda incompleta

Os GameTests comprovam contratos isolados, mas ainda não comprovam em save real
a criação visual da BigHouseMOD, a retomada de uma obra abandonada, a sequência
de construções, os dez viveiros, a coleta integral após areia cair e o fluxo
end-to-end da fila. Isso é uma lacuna de evidência, não uma falha afirmada.

## Melhorias recomendadas

1. Criar GameTest de alternância com persistência do último tipo não residencial.
2. Adicionar teste de endurance de muitos ciclos, com contador de tarefas,
   projetos, reservas e custo de varredura.
3. Adicionar cenário de deadlock entre fazendeiro, produtor e construtor.
4. Instrumentar motivo de espera, alvo, setor, inventário e última ação de
   cada trabalhador durante um stall.
5. Refatorar os arquivos Java acima de 500 linhas por responsabilidade, sem
   mudar contrato durante a correção do P0.
6. Definir uma política para IDs de material de blueprint sem mapeamento para
   ResourceType: produzir, substituir explicitamente ou recusar antes da obra
   com diagnóstico.
7. Decidir se dependências de runtime devem ser ranges ou versões fixas. O
   projeto exige versões fixas, mas fabric.mod.json aceita
   fabricloader >=0.16.0, fabric-api >=... e java >=21.
8. Mover documentos históricos que descrevem classes inexistentes para uma área
   histórica e marcar cada documento vivo com sua fonte canônica.
9. Manter um único relatório atual de hash do JAR e gerar a cópia em downloads/
   somente após build de release verificado.

## Inconsistências encontradas

- README.md antigo dizia que o estado de 19-09 ainda não havia fechado uma casa;
  o estado atual registra uma casa concluída.
- O README e docs/behavioral-tests/README.md tinham contagens antigas de testes
  e o nome inexistente MANUFACTURER.
- O README e o relatório inicial da auditoria chegaram a publicar SHA-256
  anterior; ambos foram atualizados para o hash do JAR sincronizado.
- CLAUDE.md dizia “seis funções fundacionais e sete profissões de crescimento”
  sem explicar a diferença entre oito funções operacionais, sete produtores e
  seis titulares da casa.
- STATE.md declara teto de 150 linhas, mas conserva histórico muito acima do
  teto; isso contradiz a regra do próprio arquivo e atrasa a leitura do estado.
- ProfessionAssigner cita Profession-System.md, que não é a fonte atual de
  regras e pode confundir manutenção.
- Há documentos históricos que ainda usam “schema”, “manufacturer” e nomes de
  classes de uma arquitetura que não está no código atual.

## Conflitos de requisitos resolvidos

- A regra inicial pedia oito conjuntos dentro da casa; a decisão posterior
  retirou apenas cama e baú de agricultor e carpinteiro para liberar porta e
  escada. O código atual mantém as duas profissões no registro, nas tarefas e
  no crescimento: a BigHouseMOD tem seis titulares físicos e a vila tem oito
  funções operacionais.
- A BigHouseMOD deve nascer com a vila, mas não pode ser escolhida pelos
  profissionais. São dois fluxos distintos: fundação automática e catálogo de
  obras comuns.
- A regra de não haver blocos acima da obra não substitui a validação do solo:
  o lote precisa passar tanto pela janela vertical de 25 blocos quanto pelo
  apoio natural, acessibilidade, proteção de estruturas e ausência de projetos.
- “Ferreiro” foi solicitado como função, mas o catálogo real usa MASON e a
  cadeia Vanilla de alvenaria já tem executor. Criar outro enum duplicaria a
  responsabilidade; a documentação usa MASON como nome técnico e pedreiro
  como nome exibido.

## Revisão técnica comum

### Arquitetura

Ponto forte: core não importa Minecraft/Fabric/data, a fronteira tem teste, e
não foram encontrados ciclos de importação Java.

Risco: BuildSiteScanner, VillageDetectionHandler, MinerWork, MineDigging,
ConstructionPlanner, BuilderWork e CraftingWork acumulam responsabilidades
demais. O maior arquivo é BuildSiteScanner, com 1.774 linhas.

### Concorrência e estado

O código trabalha no ciclo do servidor, sem workers assíncronos, mas há estado
estático em registries e fixtures de testes. O relógio de mundo e o orçamento
global de busca podem contaminar cenários e produzir resultados dependentes da
ordem. O isolamento de cada GameTest deve continuar sendo tratado como
requisito de produto da suíte.

### Persistência

Há testes para colônia, construção, mina, estradas, cursor de varredura e
retomada. Continua valendo testar explicitamente a sequência
SAVE -> LOAD -> CONTINUE -> VERIFY para uma obra em execução, não apenas para uma
colônia que voltou dormente.

### Segurança do mundo

As proteções de volume, estruturas Vanilla, projetos pendentes, fundação e
Tocha das Almas estão cobertas por regressões recentes. O risco restante é
semântico: qualquer novo caminho de colocação de bloco deve passar pelas mesmas
reservas e pela mesma classificação de propriedade.

### CI e release

O workflow está presente e as versões de build são fixadas em gradle.properties.
O estado atual não é verde porque a bateria obrigatória falhou. Além disso, os
ranges em fabric.mod.json precisam de decisão explícita para não contradizer a
política de dependências fixas.

## Nota

| Área | Nota |
|---|---:|
| Arquitetura e isolamento | 8,0/10 |
| Regras e funcionalidade | 7,0/10 |
| Testes | 7,0/10 |
| Manutenção | 6,0/10 |
| Documentação após esta revisão | 7,5/10 |
| Release e CI | 7,0/10 |
| **Projeto inteiro** | **7,0/10** |

A nota não significa que o projeto esteja pronto para distribuição: a falha
P0 de GameTest e os playtests pendentes ainda bloqueiam essa conclusão.

## Ordem de trabalho recomendada

1. Corrigir e provar a alternância de construções.
2. Repetir o GameTest completo e investigar a fixture distante se oscilar.
3. Executar os playtests de BigHouseMOD, reparo, viveiro, mina no deserto e
   lote vertical.
4. Criar endurance e deadlock tests.
5. Refatorar os maiores arquivos e decidir a política de ranges.
6. Só então atualizar o JAR de release, hash e publicação.
