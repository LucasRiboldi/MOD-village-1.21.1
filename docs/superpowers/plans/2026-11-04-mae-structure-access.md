# MAE Fase 3 — Plano de implementação de acesso a estruturas

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** construir modelos comprovados com acesso físico supervisionado e
quatro portões no curral grande, preservando mundo antigo, materiais e retomada.

**Architecture:** estender os metadados de `Blueprint`; interpretar templates e
colisões na camada Fabric; reutilizar terreno, writer e construção existentes.
Habilitar cada modelo somente após comprovar seus requisitos. Nenhum segundo
catálogo, grafo, sistema de construção ou estado persistido de conclusão.

**Tech Stack:** Java 21, Minecraft 1.21.1, Fabric, JUnit e Fabric GameTest já
configurados. Sentinela Local registra as provas; nenhuma dependência nova.

**Spec:** [ADR-046](../../decisions/ADR-046-mae-structure-access.md),
[matriz aprovada](../../technical/MAE-Fase-3-Matriz.md),
[inventário](../../technical/MAE-Fase-3-Inventario.csv) e
[auditoria](../../technical/MAE-Fase-3.md).

**Estado em 2026-11-04:** implementação e testes focados aprovados pelo autor,
com 6.1 Sol/high. Metadados, extração, validação física local, estados estritos
e variante supervisionada de quatro portões implementados. Writer de edifícios
ainda não habilitado. Provas atuais registradas no `STATE.md`.

| Tarefa | Estado verificável |
| --- | --- |
| 0 | Linha e liberação focada conferidas; PR #51 integrado |
| 1–2 | Metadados, rotação, limites e leitor validados; checkpoint parcial de pausa |
| 3 | Validação local prevista/real e orçamento validados; não autoriza escrita |
| 4 | Variante MAE de quatro portões, agrupamento e capacidade validados; travessia de animais pendente |
| 5 | Estados e partes de representantes comprovados; materiais/conteúdo funcional pendentes |
| 6–7 | Writer, persistência, consumo, retomada e habilitação pendentes |
| 8 | Registro nas suites existentes e documentos parciais; entrega não realizada |

## Global Constraints

- Implementação e unitários/GameTests focados deste recorte foram liberados.
  Bateria completa do Sentinela, publicação e integração desta fase não fazem
  parte dessa liberação. Novas etapas seguem `CLAUDE.md` §0.2.2.
- Os quatro commits 0.4.12–0.4.15 foram integrados pelo PR #51, após CI verde,
  como `9dc8b4f2`. A linha `codex/mae-structure-access` parte dessa `main`
  atualizada. Trabalho documental preservado; `.serena/` permanece fora da entrega.
- Aplicar apenas a novas vilas e áreas elegíveis com evidência íntegra da mesma
  região. Não modificar saves, recapturar prova ou certificar terreno legado.
- Preservar todos os limites das ADRs 044–045, inclusive volume, seções,
  histórico combinado e orçamento por mundo. Um template grande pode ser recusado.
- Não ativar orquestração automática antes de carga real e playtest da Fase 2.
  Não antecipar o grafo da Fase 4, reparar minas ou mudar outras profissões.
- Testes futuros devem falhar pela causa correta antes da correção. Não usar
  movimento aleatório ou sleeps como critério; controlar fixture e tempo em ticks.
- Cada entrega funcional incrementa a versão patch, registra sua prova no
  Sentinela e atualiza os documentos afetados. Commit e distribuição do JAR
  dependem da autorização da entrega; JAR nunca entra no git.

## Review Focus

| Caso que precisa de prova | Responsável |
|---|---|
| Duas portas, uma interna, com rotação e entrada elevada | Tarefas 1–3 |
| Depósito sem porta e declaração divergente do template carregado | Tarefas 2–3 |
| Escada/laje com cabeça bloqueada ou rota que exige água/salto especial | Tarefa 3 |
| Quatro portões reconhecidos como um curral; pastor entra e fecha | Tarefa 4 |
| Recarga entre intenção, escrita e confirmação sem duplicar material | Tarefas 6–7 |

## Contratos compartilhados

Estas interfaces delimitam responsabilidade. Ajustes de assinatura durante a
implementação devem atualizar este mapa e todos os chamadores no mesmo recorte.

| Contrato proposto | Responsabilidade e compatibilidade |
|---|---|
| `BlueprintAccess(Kind kind, List<ColonyPos> opening, Side outward)` | Dados neutros imutáveis; `Kind` distingue porta, portão e vão. Não contém `BlockState`, NBT ou prova de acesso. |
| `Blueprint.accesses()` e `withAccesses(...)` | Lista opcional de candidatos. `Blueprint.of(...)` conserva assinatura e lista vazia; `withStreetLayer(...)` e `rotated(...)` conservam e transformam metadados junto das peças. |
| `StructureAccessReader` | Extrai candidatos antes do filtro de jigsaw e confere declarações de vãos. Usa o template carregado por `StructureBlueprintReader`, sem reler arquivo a cada tick. |
| `MaeStructureAccess` | Validação Fabric limitada à área aprovada: geometria prevista antes de aprovar e geometria real após a obra. Resultado distingue candidato, passagem comprovada e motivo de recusa. |
| `MaeTerrainPreparation.Proposal` | Recebe entrada explicitamente selecionada, sem exigir uma única porta. A fábrica experimental atual não se torna fallback automático para modelos sem prova. |
| `MaeWorldWriter.approveStructure(...)` | Aprovação tipada proposta para peças e efeitos secundários. Compartilha reserva, diário, prova, proteção e orçamento existentes; não transforma o writer em gerador sem materiais. |

`entry` no planejamento de terreno continua significando célula de apoio.
A altura dos pés e o topo de colisão são calculados pelo validador Fabric;
não derivar tudo de `origin.y()` nem impor igualdade entre Y da porta e do piso.

### Tarefa 0 — Preparar a linha autorizada

**Arquivos:** nenhuma alteração de comportamento. Consultar `STATE.md`,
`CLAUDE.md`, esta ADR e o plano; preservar o diff documental existente.

- [x] Obter confirmação de etapa e liberação dos testes focados, separadamente
  da autorização documental. Recomendar 6.1 Sol com raciocínio alto para integração.
- [x] Conferir referências remotas, PRs e divergência; resolver a base com os
  commits locais preservados antes de criar a branch de implementação.
- [x] Registrar comandos de validação autorizados e a linha de base disponível.
  Não disparar bateria completa ou modificar CI por causa deste plano.

### Tarefa 1 — Metadados de acesso e rotação

**Criar:** `src/main/java/com/villagecolony/core/construction/model/BlueprintAccess.java`.

**Modificar:** `src/main/java/com/villagecolony/core/construction/model/Blueprint.java`.

**Provas:** estender `src/test/java/com/villagecolony/core/construction/model/BlueprintRotationTest.java`
e `BlueprintTest.java`, no mesmo diretório.

- [x] Criar regressões para lista vazia compatível, múltiplos acessos,
  imutabilidade e quatro rotações, inclusive porta elevada e passagem dupla.
- [x] Confirmar a falha por ausência/perda do metadado, não por erro de fixture.
- [x] Acrescentar o contrato neutro; conservar origem, pivô e ordem de construção.
  Mapear coordenadas do template para o mesmo referencial usado pelas peças.
- [x] Provar que `withStreetLayer` não perde acessos e que girar quatro vezes
  recupera peças e metadados. Validar fronteiras e orientação sem importar Minecraft.
- [ ] Rodar somente unitários afetados, revisar diff e preparar commit atômico
  conforme o formato do projeto, quando a entrega estiver autorizada.

### Tarefa 2 — Extrair entradas e declarar vãos

**Criar:** `src/main/java/com/villagecolony/fabric/integration/StructureAccessReader.java`
e `src/main/resources/data/villagecolony/mae/structure_access.json`.

**Modificar:** `src/main/java/com/villagecolony/fabric/integration/StructureBlueprintReader.java`.
Reutilizar `StreetLayer.java` e `PaletteStates.java` nesse diretório; alterar
somente se a extração compartilhada evitar duplicação real.

**Provas:** estender `src/gametest/java/com/villagecolony/gametest/BlueprintReaderGameTest.java`.

- [x] Registrar fixtures mínimas de porta interna/externa, passagem dupla,
  conector horizontal, conector vertical e depósito sem porta.
- [x] Inspecionar fisicamente os depósitos e o pórtico antes de declarar seus
  vãos. Não inventar coordenadas a partir das dimensões do CSV.
- [x] Extrair candidatos do mesmo template que fornece as peças. A declaração
  JSON associa apenas id existente, abertura e direção; não duplica catálogo.
- [x] Conferir cada declaração contra a geometria efetivamente carregada,
  inclusive substituição por datapack. Template alterado com vão obstruído deve
  ser recusado; hash dos bytes comprimidos do CSV não funciona como prova runtime.
- [x] Manter leitura e construção legadas compatíveis. Um candidato não
  habilita o modelo nem autoriza escrita. Atualizar comentários afetados pela ADR.
- [ ] Validar o leitor focado e revisar o diff antes do commit autorizado.

### Tarefa 3 — Validar circulação e integrar propostas locais

**Criar:** `src/main/java/com/villagecolony/fabric/integration/MaeStructureAccess.java`
e `src/gametest/java/com/villagecolony/fabric/integration/MaeStructureAccessGameTest.java`.

**Modificar:** `src/main/java/com/villagecolony/fabric/integration/MaeTerrainPreparation.java`.
Estender `MaeTerrainGameTest.java` no diretório de GameTests acima.

- [ ] Reproduzir recusa incorreta de múltiplas portas e aceitação indevida de
  entrada sem apoio, cabeça livre ou conexão ao destino interno.
- [x] Avaliar cada candidato com terreno e peças previstos sobrepostos ao
  mundo somente lido; verificar novamente no mundo após construção. Não exigir
  que uma casa ainda não construída já exista para planejar seu acesso.
- [x] Usar formas de colisão e dimensões do aldeão, incluindo lajes, escadas,
  portas e portões. Considerar interação explícita com portões. Não inventar
  um novo pathfinder global ou depender de salto especial do mod.
- [x] Verificar conectividade de toda superfície de circulação preparada.
  Água funcional e lavoura ficam preservadas, fora da superfície de circulação.
- [ ] Produzir propostas por acesso elegível; escolher entre propostas válidas
  com os critérios existentes e desempate estável. Manter motivo claro para
  ausência de acesso, orçamento excedido, chunk ausente e proteção divergente.
- [x] Preservar rua comprovada como contexto somente lido na mesma região.
  Sem ligação externa comprovada, conservar `external_access_unverified`.
- [ ] Provar limites de corte/aterro, suporte e transição sem ampliar orçamento;
  validar apenas unitários/GameTests focados liberados e preparar commit atômico.

### Tarefa 4 — Quatro portões e um único curral

**Criar:** `src/main/resources/data/villagecolony/structure/mae/large_animal_pen.nbt`.
Opção B do autor: variante de quatro portões somente em `readForMae`;
`structure/colony/large_animal_pen.nbt` permanece idêntico no fluxo automático.
Não adicionar marcador de geração ou migrar reservas nesta etapa.

**Modificar:** `src/main/java/com/villagecolony/fabric/work/ShepherdPens.java` e, somente nos
chamadores necessários, `ShepherdHerding.java` e `FencedIn.java` nesse diretório.

**Provas:** estender `src/gametest/java/com/villagecolony/fabric/work/PenModelGameTest.java`,
`ShepherdPensGameTest.java` e `ShepherdHerdingGameTest.java` no mesmo diretório.
Conferir invariantes de `src/main/java/com/villagecolony/fabric/work/PenPlans.java`.

- [ ] Reproduzir a ausência dos três portões e a perda de acessos alternativos
  pela deduplicação atual de `ShepherdPens.penBehind`. Uma cerca não conta como portão.
- [x] Substituir somente as três peças na variante MAE aprovada na ADR; preservar o portão
  norte, tamanho, água, 12 cultivos e quatro tochas. Conferir todas as rotações.
- [x] Agrupar acessos pela área cercada real, sem persistir outro curral.
  Conservar os chamadores que precisam de um portão escolhido; selecionar
  acesso válido conforme o trajeto do pastor, não pela ordem de varredura.
- [ ] Reutilizar `FencedIn.open` e `closeWhenClear`. Provar passagem, fechamento
  e ausência de fuga durante a rotina controlada, inclusive acesso bloqueado.
- [ ] Provar capacidade total 20, um curral grande por vila e funcionamento dos
  pequenos/extras legados. Não reformar NBT já colocado em saves antigos.
- [ ] Atualizar linha e SHA-256 do inventário somente depois da mudança física.
  Validar classes afetadas e preparar commit autorizado com versão patch.

### Tarefa 5 — Comprovar estados e conteúdo por modelo

**Modificar conforme a prova:** `src/main/java/com/villagecolony/fabric/integration/PaletteStates.java`,
`StructureBlueprintReader.java` e `src/main/java/com/villagecolony/fabric/work/BlockShaping.java`.
Não ampliar `ShapeStates` globalmente sem necessidade demonstrada.

**Provas:** estender `BlueprintReaderGameTest.java`, `PenModelGameTest.java` e
`src/gametest/java/com/villagecolony/fabric/integration/ColonyModelsGameTest.java`.

- [ ] Derivar requisitos das famílias da matriz: casas, oficinas, currais,
  depósitos e conexão local. Validar representantes dos cinco estilos e
  bloquear explicitamente os demais modelos ainda não comprovados.
- [ ] Provar metades de porta, camas, baús, estados de cultivo e inicialização
  funcional necessária. Não tratar presença do bloco como prova de seu conteúdo.
- [ ] Implementar somente lacunas reproduzidas nos tratamentos existentes.
  Não copiar inventário, loot, entidades, UUIDs ou dados arbitrários do template.
- [ ] Conferir origem física dos materiais, água e sementes conforme regras
  atuais. Sem suporte, informar recusa; não gerar recursos para fazer o teste passar.
- [ ] Registrar por modelo o que passou, o que não é necessário e o que bloqueia
  habilitação. Conservar a política do construtor legado fora do MAE.
- [ ] Validar o recorte liberado e preparar commit atômico.

### Tarefa 6 — Escrita supervisionada das peças

**Modificar:** `src/main/java/com/villagecolony/fabric/integration/MaeWorldWriter.java`,
os contratos MAE correspondentes em `src/main/java/com/villagecolony/data/save/MaeWorkStore.java`
e os pontos responsáveis em `src/main/java/com/villagecolony/fabric/work/BuildReserve.java`
e `BlockShaping.java`.

**Provas:** criar `src/gametest/java/com/villagecolony/fabric/integration/MaeStructureWriteGameTest.java`;
estender `MaeContractGameTest.java` e as regressões MAE afetadas no mesmo diretório.

- [x] Mapear todos os efeitos da colocação antes de editar: bloco principal,
  segunda metade, apoio, inicialização admitida e consumo de material.
- [x] Fixar o esquema de `structure_v3` na ADR-046: associação com região e
  projeto, efeitos planejados, intenção, confirmação e cursor. Leitores antigos
  devem bloquear o formato desconhecido; não reinterpretar v1/v2 ou migrar reservas.
- [x] Criar regressões para corte de proteção, escrita fora do plano, região
  errada, falta de prova, contrato desconhecido e falha após escrita física.
- [x] Implementar aprovação explícita após preparação concluída; revalidar
  os efeitos do terreno pelo diário existente. Não recapturar evidência como
  se a área alterada fosse uma nova geração intacta.
- [x] Conduzir todas as partes da peça pelo writer e pelo fluxo de materiais
  existente. Não ligar `BlockShaping` diretamente ao mundo supervisionado como fallback.
- [x] Provar retomada entre intenção/escrita/confirmação/consumo sem duplicação
  ou perda silenciosa. Ambiguidade bloqueia e informa causa; orçamento permanece
  global, com uma obra ativa por região e histórico combinado dentro do teto atual.
  Interrupção simulada pelo estado gravado, não por crash real do servidor.
- [x] Validar focadamente; revisar compatibilidade e diff antes do commit autorizado.

### Tarefa 7 — Conclusão, retomada e habilitação supervisionada

**Modificar:** `src/main/java/com/villagecolony/fabric/work/BuildClosing.java`,
`ConstructionResume.java` e os chamadores MAE identificados nas tarefas anteriores.
Reutilizar `src/main/java/com/villagecolony/fabric/integration/BlueprintPlacement.java`.

**Provas:** estender `src/gametest/java/com/villagecolony/fabric/work/BuildClosingGameTest.java`,
`src/gametest/java/com/villagecolony/fabric/integration/BlueprintResumeGameTest.java`
e `MaeStructureWriteGameTest.java`.

- [x] Provar que conclusão sem mobília não certifica acesso e que projeto
  concluído não reconstrói nem consome materiais novamente após recarga.
- [x] Revalidar a geometria real e a compatibilidade do template antes de
  retomar. Conservar os estados já persistidos por `ColonySavedData` e
  `ColonySaveReader`; não salvar outro booleano de acesso pronto.
  `BuildClosing` não é chamado (opera sobre o projeto vivo do legado); o
  registro final `BUILDINGS.registerOrMerge` é o mesmo — ADR-046.
- [ ] Permitir exercício supervisionado de uma casa, oficina, curral, depósito
  e conexão local comprovados, dentro dos tetos. Modelo sem prova segue bloqueado.
  Feito em GameTest para casa, oficina, curral e depósito (`MaeStructureSite`);
  a conexão local de rua e o disparo `/vc mae obra` foram feitos depois; falta exercitar em jogo.
- [ ] Antes de ligar à fila automática, comprovar carga real e playtest da
  Fase 2 com relatórios. Sem isso, entregar o recorte supervisionado e registrar
  a ativação como pendência; não marcar toda a Fase 3 concluída.
- [ ] Registrar motivos de recusa no relatório existente com modelo, posição
  e etapa. Evitar varreduras por tick e spam de log.
- [ ] Validar retomada e comportamento físico controlados; preparar commit autorizado.

### Tarefa 8 — Sentinela, documentação e entrega

**Modificar:** `STATE.md`, `TODO.md`, `README.md`, `CHANGELOG.md`,
`docs/archive/technical/Development-Log.md`, relatório e matriz da Fase 3.
Atualizar `Sentinela/TUTORIAL.md` se mudar suites, argumentos ou capacidade visível.
Versão funcional: conferir `gradle.properties` e manifesto efetivamente gerado.

- [x] Garantir que as novas classes de prova sejam descobertas nas suites
  `unit` e `gametest` existentes de `scripts/test_lab.py`; não criar outro runner.
- [x] Documentar os comandos focados após existirem as classes (`MAE-Fase-3.md`). Exemplo anterior:
  `.\gradlew.bat runGametest "-PgametestOnly=MaeStructureAccessGameTest,MaeStructureWriteGameTest,PenModelGameTest,ShepherdPensGameTest,ShepherdHerdingGameTest"`.
- [ ] Após liberação específica, reunir resultados em uma execução proporcional
  do Sentinela: `python scripts/test_lab.py fast --only unit,gametest`.
  Esse comando executa as suites completas escolhidas, não somente o filtro anterior.
  Não agendar execução automática ou chamar o Sentinela sem pedido do autor.
- [x] Atualizar inventário e provas por modelo, distinguindo validação automática,
  carga real e playtest pendente. Não reutilizar resultados históricos como atuais.
- [ ] Quando autorizado a entregar: commit por tarefa, push e PR; acompanhar CI
  e integrar conforme aprovação. Depois atualizar a `main` local e remota.
- [ ] Gerar `remapJar`, copiar para `downloads/` e TLauncher, comparar SHA-256
  e manifesto. Preservar cópia desativada anterior; não anexar JAR ao repositório.
  Publicação no GitHub usa asset de release e SHA-256 na descrição, quando autorizada.

## Critérios de saída

- Modelos habilitados têm acesso externo e interno comprovado, materiais e
  conteúdo suportados; demais modelos apresentam recusa explícita.
- Curral novo tem quatro portões e funciona como um cercado de até 20 animais.
- Toda escrita MAE passa por prova, proteção, diário, orçamento e material;
  retomada não duplica peças nem recursos. Mundo antigo permanece intacto.
- Relatório lista testes realmente executados, limitações e evidência de playtest.
  Sem carga real/playtest da Fase 2, ativação automática continua pendente.
- Grafo da Fase 4 e correções gerais de IA permanecem fora deste recorte.

## Revisão documental

O plano cobre o contrato A e os cinco casos de revisão. Mantém distintas a
evidência de template, a proposta de terreno e a passagem física observada.
Não pressupõe APIs de colocação de edifícios já existentes no writer.
Assinaturas propostas ficam concentradas no mapa compartilhado; as tarefas
posteriores dependem delas, sem criar caminhos paralelos.

Implementação parcial revisada e validada pelos testes focados registrados no
relatório da Fase 3. Não houve carga real, playtest ou entrega desta fase.
Os itens ainda abertos não são certificados pelos testes já executados.
