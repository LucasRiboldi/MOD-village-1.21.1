# ADR-046 — Acesso físico e construção supervisionada de estruturas MAE

Estado: contrato A, implementação e testes focados aprovados pelo autor
em 2026-11-04. Implementação parcial; escrita de edifícios ainda não habilitada.
O esquema `structure_v3` (seção própria abaixo) foi aprovado pelo autor em
2026-11-04, com P1–P4; ainda não implementado.
Esta ADR complementa as ADRs [044](ADR-044-mae-region-safety.md) e
[045](ADR-045-mae-terrain-road-chain.md). Não ativa obras no jogo.

## Contexto

A [matriz da Fase 3](../technical/MAE-Fase-3-Matriz.md) inventaria 143 templates
vanilla autorizados e 18 arquivos próprios na auditoria inicial. A variante
supervisionada acrescentada nesta fase totaliza 19 arquivos próprios. Há portas múltiplas, portas elevadas,
currais com portões e depósitos sem porta. A presença de um bloco de porta não
comprova circulação entre a vila e o interior útil da construção.

`MaeTerrainPreparation.Proposal.forProject` exige uma única porta inferior e
usa `Blueprint.doorSide()`. Esse contrato experimental não atende ao catálogo
inteiro. `StructureBlueprintReader` descarta conectores durante a leitura e não
transporta payload genérico de block entities. O writer MAE atual aprova terreno
e ruas; ele ainda não oferece um contrato completo para construir edifícios.

O autor aprovou combinar evidência da planta e validação física, pediu quatro
portões no curral grande e delegou as escolhas compatíveis com as regras atuais.

## Decisão

1. Reutilizar `Blueprint`, os catálogos existentes, a rotação e a seleção por
   bioma. Acrescentar somente metadados neutros de acesso. Não criar outro
   catálogo de modelos, grafo de caminhos ou estado persistido de conclusão.
2. Extrair candidatos de portas, portões e conectores horizontais de entrada
   antes de filtrar jigsaws. Agrupar portas que formem uma passagem e distinguir
   saídas de portas internas. Facing e parede mais próxima são indícios, não prova.
3. Declarar vãos abertos por id de modelo existente. A declaração contém apenas
   geometria de acesso, conferida contra o template efetivamente carregado.
   Mudança do template exige nova validação; o CSV não é autorização de escrita.
   Depósitos sem porta permanecem bloqueados no MAE até essa comprovação.
4. A camada Fabric interpreta NBT, colisões e interação; o core recebe tipos
   próprios, conforme a ADR-005. Metadados acompanham a mesma origem, pivô e
   rotação das peças. Não aplicar uma segunda rotação ou confundir piso e pés.
5. Comprovar apoio, largura, corpo, cabeça e continuidade entre exterior e
   destino interno útil. Considerar colisões parciais, portas, portões, lajes e
   escadas. Toda superfície preparada de circulação deve permitir passagem por
   terra com salto normal. Água funcional, cultivos e paredes não são circulação.
6. Preferir o acesso ligado à circulação preparada; desempatar por coordenadas
   locais em ordem estável. Recusar com motivo quando nenhum acesso for válido.
   Não carregar chunks à força nem aumentar os limites das ADRs 044–045:
   corte/aterro até dois blocos, duas camadas de apoio e transição até seis.
7. Preservar a sequência: selecionar e orientar a planta; avaliar acessos e
   terreno; reservar e aprovar; preparar; construir com materiais pelo fluxo
   existente; verificar o acesso no mundo. Validação antecipada usa a geometria
   final prevista; a validação após a obra consulta os blocos realmente presentes.
8. Antes de qualquer colocação MAE de edifício, estender o writer com aprovação
   tipada de estrutura, aqui denominada `structure_v3`. Cada operação deve
   revalidar prova e proteções, registrar intenção e confirmação e contabilizar
   materiais uma única vez. Portas e camas incluem suas partes secundárias.
   Um efeito fora do plano, como apoio criado implicitamente por `BlockShaping`,
   não pode contornar o writer. O detalhamento deve preservar contratos antigos,
   bloqueio de formatos desconhecidos, orçamento compartilhado e retomada segura.
   Esta proposta não autoriza converter reservas antigas automaticamente.
9. Suportar somente estados e dados necessários aos modelos comprovados.
   Conferir camas, portas, baús, orientação, cultivos e demais requisitos de cada
   família. Não copiar `Items`, loot tables, entidades, UUIDs ou NBT arbitrário.
   Sem tratamento comprovado, recusar o modelo no MAE e manter o legado intacto.
   Água, cultivos e materiais obedecem às regras de origem física já vigentes.
10. Reutilizar `ConstructionState.COMPLETED`, `BuildClosing`, `ColonySavedData`
    e `ColonySaveReader`. Conclusão que dispensa mobília não comprova acesso.
    Revalidar a passagem ao retomar e após recarga, sem reconstruir uma obra
    concluída ou criar outro indicador persistido de sucesso. Divergência ou
    escrita incerta bloqueia a operação e informa a causa.

### Curral grande

- Manter a identidade `villagecolony:colony/large_animal_pen` e o tamanho
  15×3×15. A leitura supervisionada `readForMae` usa o asset
  `villagecolony:mae/large_animal_pen`; a leitura automática `read` conserva o
  asset anterior de um portão, sem mudar o catálogo nem o id do projeto.
- Conservar o portão norte `(7,1,0)` e substituir somente as cercas centrais
  leste `(14,1,7)`, sul `(7,1,14)` e oeste `(0,1,7)` por portões orientados
  conforme a cerca. Iniciar fechados, sem alimentação de redstone.
- Reutilizar abertura e fechamento de `FencedIn`. O pastor escolhe um portão
  fisicamente válido; não presumir que o pathfinding o abre automaticamente.
  Reconhecer os quatro acessos como um único cercado, sem multiplicar capacidade.
- Preservar água, 12 cultivos, quatro tochas, capacidade de 20 animais e limite
  de um curral grande por vila. Currais pequenos e extras existentes continuam
  funcionais, conforme as regras já aprovadas.
- Opção B escolhida pelo autor em 2026-11-04: os quatro portões ficam restritos
  ao MAE supervisionado. O marcador de origem atual não distingue a geração
  desta fase; por isso não autoriza trocar o asset do fluxo automático.
  Não adicionar marcador ou migrar reservas para antecipar essa ativação.
- O novo template vale somente para novos currais em novas vilas elegíveis.
  Não reformar currais existentes, migrar saves ou abrir quatro ramais obrigatórios.
  Uma saída exterior bloqueada não autoriza cortar proteção para liberá-la.

## Alternativas consideradas

- **A, escolhida:** combinar conectores, portas, portões e vãos declarados com
  validação física. Aproveita os modelos existentes sem presumir que todos passam.
- **B, não escolhida:** declarar manualmente todas as entradas. Duplica informação
  presente nos templates e amplia manutenção sem dispensar validação física.
- **Primeira porta ou parede mais próxima:** insuficiente para portas internas,
  modelos sem porta, níveis distintos e colisões parciais; não certifica acesso MAE.

## Consequências e limites

A habilitação é por modelo comprovado, não por contagem de arquivos. Modelos
sem acesso, conteúdo suportado, materiais ou orçamento suficiente permanecem
bloqueados com motivo. A leitura do catálogo não concede elegibilidade de terreno.

A integração automática permanece bloqueada pela carga real e pelo playtest
da Fase 2. A Fase 4 cuida da ligação ao grafo; conserva o gate histórico de
navegação na água. Esta ADR não muda IA geral, profissões ou saves antigos.

O [plano da Fase 3](../superpowers/plans/2026-11-04-mae-structure-access.md)
define entregas e comprovação. O `STATE.md` registra as provas executadas e
os bloqueios restantes; a auditoria inicial conserva seu caráter histórico.

### Prova de leitura e limites da implementação parcial

A leitura MAE verifica o template efetivamente carregado, seus limites declarados,
partes secundárias e estados nas quatro rotações. Propriedades estritas ficam em
`PaletteStates`, sem fazer `integration` depender da colocação em `work`.
Payloads não suportados, inventários não vazios, loot, entidades e estados de
jigsaw com NBT são recusados. Prova de estados não certifica materiais, conteúdo
funcional, circulação de cada modelo ou permissão de escrita.

O NBT do pórtico tinha `size` e posições como `IntArray`, incompatíveis com as
listas de inteiros lidas por `StructureTemplate`. Corrigiu-se apenas a codificação:
mesmas dimensões 9×7×5, 164 peças, posições e estados. Não houve reforma de mina.
O validador de acesso recusa dimensões extremas antes de multiplicar o volume;
o teto de leitura não pode ser contornado por overflow.

O writer de edifícios, o consumo de materiais e a retomada de `structure_v3`
continuam pendentes. Antes de implementá-los, o detalhamento deve distinguir
intenção persistível de durabilidade: `PersistentState.markDirty` não confirma
uma transação atômica entre inventário, chunks e diário. Uma retomada ambígua
precisa bloquear; um teste de serialização isolado não prova recuperação após crash.

## Esquema `structure_v3`

Detalha o item 8. O autor aprovou as escolhas **P1–P4** em 2026-11-04; as
alternativas ficam registradas como não escolhidas. Writer, conclusão com
acesso conferido e retomada estão em `MaeStructureExecution`. A ligação com a
fila automática continua bloqueada pela carga real e pelo playtest da Fase 2.

### Premissa de durabilidade

O Minecraft grava o arquivo da colônia (`PersistentState`) e os chunks — onde
ficam os blocos e o inventário dos baús — em momentos distintos, sem transação
entre eles. Depois de um crash, qualquer combinação de "diário antigo/novo" com
"bloco e baú antigos/novos" é possível. Nenhum desenho de campos elimina isso.
O contrato, portanto, não promete recuperação automática de um passo
interrompido: promete **detectar** a interrupção, **nunca repetir** o passo e
**bloquear com causa**, como o writer de terreno já faz (ADR-044).

### Envelope e compatibilidade

- **P1.** `maeWork.version` passa a 3 somente quando a primeira aprovação
  `structure_v3` for aceita, como `reserveTerrain` promove para 2 hoje.
  Leitores até a 0.4.15 aceitam só 1 e 2: o payload inteiro fica opaco,
  preservado na gravação, e toda operação MAE da dimensão recusa. Sem conversão
  de reservas v1/v2. *Não escolhida:* manter o envelope em 2 e confiar na
  recusa por `type` desconhecido; o leitor antigo, porém, gravaria
  `state=blocked` por cima do job novo.
- O leitor novo aceita 1, 2 e 3. Jobs v1/v2 seguem pelos caminhos atuais.
  `type` ou `contract` desconhecido recusa com `unknown_structure_contract`;
  `MaeSectionLocks` trata o job desconhecido como seção ocupada.
- Os pontos legados passam por `MaeRoadAccess`: com `supported()` falso,
  `legacyAllowed` e `metadataAllowed` devolvem falso sem lançar exceção. É o
  caminho de um leitor até v2 diante de um save v3, provado com envelope
  futuro em `MaeRoadGateGameTest`. Com envelope v3 legível, a caixa da região
  com obra `structure_v3` bloqueia o legado só nas próprias seções.

### Região e projeto

O job vive em `region.job` de uma região com `policy=terrain_v2`, já usada pela
preparação. Uma obra ativa por região; o orçamento global de escrita continua
o de `MaeWorldWriter` (quatro escritas e 2 ms por tick, 1.024 registros).

| Campo do job | Conteúdo |
|---|---|
| `type`, `contract` | `"structure_v3"`, `1` |
| `id` | id do `ConstructionProject` |
| `blueprint`, `template` | id do modelo e SHA-256 das partes (posição relativa e estado) do modelo lido por `readForMae` e girado |
| `rotation`, `origin` | a rotação e a origem já aplicadas; não reaplicar |
| `area` | caixa do edifício, volume ≤ 4.096, contida na região |
| `terrain` | id e `expectedSections` finais do job `terrain_v2` concluído; a ligação exige o mesmo `blueprint` e a mesma pegada em x/z |
| `access` | abertura, apoio, portões, destino útil (`goal`) e circulação da prova de `MaeStructureAccess` |
| `plan`, `cursor` | passos em ordem; índice do próximo |
| `state`, `reason` | `approved`, `waiting`, `paused`, `blocked`, `built`, `complete` |
| `pendingWrite`, `expectedSections` | como no writer atual |

A circulação e o destino útil da prova saem de `MaeStructureAccess.beginFor`:
circulação é a superfície preparada fora da pegada (a de dentro vira piso da
obra); o destino é a primeira célula transitável, com apoio, para dentro de um
vão declarado, atravessando porta ou portão. Vãos cuja célula de fora está na
circulação vêm primeiro, a mesma condição com que o levantamento aceita um
acesso. Sem célula assim, `no_interior_goal`. A derivação só escolhe a
pergunta; quem prova a passagem continua sendo o levantamento.

A aprovação exige o job de terreno `complete` e sem pendência, e compara as
seções atuais com o `expectedSections` final dele pelo diário existente; não
recaptura evidência como se fosse geração intacta. Terreno já plano (escolha do
autor, 2026-11-04): no lugar do job, vale um levantamento viável sem mudança da
mesma região, modelo e pegada, cuja evidência ainda bate com o mundo no momento
da aprovação; nada novo é gravado e `terrain` guarda `flat=true` e a evidência.
Prova velha recusa com `terrain_job_required`.

`MaeStructureSite` conduz o fluxo supervisionado do item 7 numa região já
reservada: levantamento do terreno pela saída escolhida (pegada inteira logo
abaixo da origem e o piso de fora do vão que sai da pegada em menos passos;
empate vai para porta, portão e vão aberto), prova do acesso com as mudanças
previstas do terreno, que precisa confirmar a mesma entrada, escrita do
terreno quando há mudança, e obra até a prova real. Vive só na sessão; os jobs
do writer são o que sobrevive à recarga. Não é chamado pela fila automática.

O disparo supervisionado é o comando `/vc mae obra` (`MaeCommand`, `MaeSites`),
escolha do autor de 2026-11-04: origem centrada no jogador com rotação pelo
olhar, ou busca de lote nivelado pelo mesmo mapa de alturas da casa fundadora
(`FoundationLotSearch`, desnível até 2, sem vegetação a tirar). A região
reservada é a pegada com folga de 5 na horizontal, 7 abaixo e 5 acima. Até 4
obras simultâneas; `parar` pausa o job aprovado e não desfaz nada.

Ao concluir, a obra pedida por comando liga a entrada à rua: `MaeStructureRoad`
procura, dentro da região, um trajeto de chão firme até o grafo de ruas MAE
publicado ou, sem grafo nem índice de ruas, até a semente perto do centro; o
trajeto passa por `MaeRoadPreparation` e pelo writer (`road_v2`). Sem rua
certificada ao alcance, a obra termina sem ela, com nota. As ruas MAE só se
ligam dentro de uma região, então a obra seguinte reaproveita a região ociosa
da colônia que a comporta. A proteção trata a caixa das obras concluídas na
própria região como trabalho MAE, derivada do diário, sem campo novo no save.

### Passo e efeitos planejados

Cada passo é uma peça com todas as suas partes:

- `parts`: lista com `position`, `before`, `after` e `role` (`main`,
  `upper_half`, `bed_head`, `support`). Porta e cama levam as duas partes no
  mesmo passo. Apoio que `BlockShaping.groundFor` criaria vira parte `support`
  planejada na aprovação, ou o modelo é recusado; o writer não chama
  `BlockShaping` nem `placeSecondHalf`.
- `material`: `item` e `count` exatos — um por peça, dois na laje dupla.
- O contrato 1 só escreve em ar. Peça já presente com o estado planejado sai do
  plano; célula ocupada por outra coisa recusa com `occupied_structure_target`.
  Fluido, cultivo, solo arado e bloco sem item recusam o modelo com
  `unsupported_structure_piece`; nenhuma parte `support` é gerada.
- **P2.** O contrato 1 aceita só o material exato da planta, tirado dos baús.
  Substituição (`MaterialChoice`) e peça que surge (`AppearingPieces`, ADR-036)
  ficam fora; o modelo que dependa delas fica bloqueado com motivo, conforme o
  item 9. *Não escolhida:* registrar a escolha na intenção, com o `after`
  vestido, ao custo de recalcular `expectedSections` no passo.
- Teto: plano + histórico ≤ 1.024 registros; acima disso a aprovação recusa
  com `history_budget_exceeded`, sem dividir a obra automaticamente.

### Sequência de um passo

1. **Revalidar:** `validateJob` atual, cada `before` no mundo, proteção
   (as peças já confirmadas desta obra não contam como volume protegido) e
   seções da região. O template pelo hash é conferido na aprovação e na
   retomada, não a cada passo, porque relê e prova o modelo inteiro. O acesso
   escolhido fica registrado; sua conferência é a da Conclusão.
2. **Material:** se o baú não tiver o item, `state=waiting` com o item no
   `reason`, sem intenção e sem escrita; reavaliar com limite de taxa.
   **P3.** A ordem do cursor é estrita no contrato 1, sem pôr peças de lado.
   *Não escolhida:* lista de adiadas, como o construtor legado; exige revalidar
   apoio fora de ordem.
3. **Intenção:** histórico com `result=pending` e `materials=reserved`
   (item, quantidade, baú), `pendingWrite=true`, `store.update`.
4. **P4. Retirada antes da escrita:** retirar do baú, depois escrever as
   partes com `NOTIFY_LISTENERS | FORCE_STATE`. Um crash nesse intervalo perde
   no máximo o material de um passo; nunca cria bloco sem material ("a colônia
   não inventa matéria"). *Não escolhida:* escrever antes de retirar, que
   arrisca bloco grátis em vez de perda.
5. **Confirmar:** conferir cada parte e as seções esperadas; histórico com
   `result=applied` e `materials=consumed`; `pendingWrite=false`, cursor + 1.
   Falha na conferência: `write_not_confirmed`, bloqueio.

### Retomada

Só por `resume` explícito, como hoje; nunca no carregamento.

| Situação ao retomar | Resultado |
|---|---|
| sem `pendingWrite` e seções iguais às esperadas | continua do cursor |
| sem `pendingWrite` e seções diferentes | `world_changed_since_approval` |
| `pendingWrite`, todas as partes em `before` | `materials_uncertain` com item e baú |
| `pendingWrite`, todas as partes em `after` | `materials_uncertain` |
| `pendingWrite`, partes misturadas | `partial_parts` |
| template com hash diferente | `template_changed` |

Toda linha exceto a primeira bloqueia. Liberar um bloqueio exige decisão
explícita sobre o material; o comando para isso fica pendente e não faz parte
do contrato 1.

### Conclusão

O último passo confirmado deixa o job em `built`, que mantém as seções
ocupadas. Nas fatias seguintes, `MaeStructureAccess` repete a prova com
`built=true`, o mesmo destino e a mesma circulação gravados, sobre os blocos
realmente presentes e o projeto refeito do template conferido pelo hash. A
prova é incremental e vive só na sessão; após recarga, recomeça do zero.

- Passou: o `Building.of(project, true)` entra no registro de edifícios por
  `registerOrMerge`, o mesmo registro final do `BuildClosing`, e o job fica
  `complete`, o que libera as seções. O `BuildClosing` em si não é chamado: ele
  opera sobre o `ConstructionProject` vivo do construtor legado e o move para
  `ConstructionState.COMPLETED`, e a obra MAE não tem esse projeto vivo.
- Falhou: `access_not_verified`, com a causa da prova em `detail`, bloqueio, e
  o edifício não entra no registro.
- `resume` de obra já escrita confere o template e volta a `built`, para
  provar o acesso de novo; nunca reescreve.
- Nenhum outro indicador persistido de acesso. Após recarga, obra `complete`
  não reconstrói, não consome material nem registra o edifício de novo.

### Regressões

`MaeStructureModelsGameTest` exercita o caminho inteiro com quatro modelos
reais lidos por `readForMae` (casa, oficina, curral e depósito), e
`MaeStructureCatalogGameTest` fixa o que o contrato 1 aceita no catálogo
representativo; detalhes na matriz da Fase 3.
`MaeStructureCompletionGameTest` cobre: conclusão única após a prova real,
sem reescrita nem novo registro após recarga; acesso que não passa bloqueia
sem registrar; `template_changed` na retomada; retomada volta a conferir e
conclui. `MaeStructureWriteGameTest` cobre: escrita única com consumo único e recarga
do diário; porta com as duas metades no mesmo passo; espera por material sem
intenção e continuação quando ele chega; template alterado; peça não suportada (água);
contrato desconhecido; mundo alterado após aprovação; terreno ausente;
envelope futuro opaco e preservado; `materials_uncertain` e `partial_parts`.
`MaeStructureWriteGameTest` também cobre todas as partes em `after` com
`pendingWrite` (`materials_uncertain`) e projeto de outra colônia
(`wrong_project_or_colony`); `MaeRoadGateGameTest` cobre os pontos legados
com envelope v3. A interrupção é simulada pelo estado gravado
(`pendingWrite` e blocos), não por crash real do servidor. O leitor até v2
não é executado: sua recusa é inferida do mesmo caminho de envelope
desconhecido. O cenário de conclusão usa um job de terreno sintético; a
cadeia terreno real → obra escrita (`built`) é provada em `MaeStructureWriteGameTest`, sem conclusão.
