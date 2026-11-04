# ADR-046 — Acesso físico e construção supervisionada de estruturas MAE

Estado: contrato A, implementação e testes focados aprovados pelo autor
em 2026-11-04. Implementação parcial; escrita de edifícios ainda não habilitada.
O esquema `structure_v3` (seção própria abaixo) é proposta aguardando o autor.
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

## Esquema `structure_v3` — proposta, aguardando aprovação do autor

Detalha o item 8. Nada desta seção está implementado. As escolhas marcadas
**P1–P4** são propostas; as alternativas ficam ao lado para o autor decidir.

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
  de reservas v1/v2. *Alternativa:* manter o envelope em 2 e confiar na
  recusa por `type` desconhecido; o leitor antigo, porém, gravaria
  `state=blocked` por cima do job novo.
- O leitor novo aceita 1, 2 e 3. Jobs v1/v2 seguem pelos caminhos atuais.
  `type` ou `contract` desconhecido recusa com `unknown_structure_contract`;
  `MaeSectionLocks` trata o job desconhecido como seção ocupada.
- [?] Na implementação, comprovar que os sete pontos legados também recusam
  quando `supported()` é falso, sem lançar exceção no tick.

### Região e projeto

O job vive em `region.job` de uma região com `policy=terrain_v2`, já usada pela
preparação. Uma obra ativa por região; o orçamento global de escrita continua
o de `MaeWorldWriter` (quatro escritas e 2 ms por tick, 1.024 registros).

| Campo do job | Conteúdo |
|---|---|
| `type`, `contract` | `"structure_v3"`, `1` |
| `id` | id do `Building`/projeto; o mesmo da reserva de lote |
| `blueprint`, `template` | id do modelo e SHA-256 do NBT efetivamente lido por `readForMae` |
| `rotation`, `origin` | a rotação e a origem já aplicadas; não reaplicar |
| `area` | caixa do edifício, volume ≤ 4.096, contida na região |
| `terrain` | id e `expectedSections` finais do job `terrain_v2` concluído |
| `access` | abertura, apoio e portões escolhidos por `MaeStructureAccess` |
| `plan`, `cursor` | passos em ordem; índice do próximo |
| `state`, `reason` | `approved`, `waiting`, `paused`, `blocked`, `complete` |
| `pendingWrite`, `expectedSections` | como no writer atual |

A aprovação exige o job de terreno `complete` e sem pendência, e compara as
seções atuais com o `expectedSections` final dele pelo diário existente; não
recaptura evidência como se fosse geração intacta.

### Passo e efeitos planejados

Cada passo é uma peça com todas as suas partes:

- `parts`: lista com `position`, `before`, `after` e `role` (`main`,
  `upper_half`, `bed_head`, `support`). Porta e cama levam as duas partes no
  mesmo passo. Apoio que `BlockShaping.groundFor` criaria vira parte `support`
  planejada na aprovação, ou o modelo é recusado; o writer não chama
  `BlockShaping` nem `placeSecondHalf`.
- `material`: `item` e `count` exatos, ou ausente para remoção.
- **P2.** O contrato 1 aceita só o material exato da planta, tirado dos baús.
  Substituição (`MaterialChoice`) e peça que surge (`AppearingPieces`, ADR-036)
  ficam fora; o modelo que dependa delas fica bloqueado com motivo, conforme o
  item 9. *Alternativa:* registrar a escolha na intenção, com o `after`
  vestido, ao custo de recalcular `expectedSections` no passo.
- Teto: plano + histórico ≤ 1.024 registros; acima disso a aprovação recusa
  com `history_budget_exceeded`, sem dividir a obra automaticamente.

### Sequência de um passo

1. **Revalidar:** `validateJob` atual, mais o template pelo hash, cada `before`
   no mundo, proteção, seções da região e o acesso escolhido.
2. **Material:** se o baú não tiver o item, `state=waiting` com o item no
   `reason`, sem intenção e sem escrita; reavaliar com limite de taxa.
   **P3.** A ordem do cursor é estrita no contrato 1, sem pôr peças de lado.
   *Alternativa:* lista de adiadas, como o construtor legado; exige revalidar
   apoio fora de ordem.
3. **Intenção:** histórico com `result=pending` e `materials=reserved`
   (item, quantidade, baú), `pendingWrite=true`, `store.update`.
4. **P4. Retirada antes da escrita:** retirar do baú, depois escrever as
   partes com `NOTIFY_LISTENERS | FORCE_STATE`. Um crash nesse intervalo perde
   no máximo o material de um passo; nunca cria bloco sem material ("a colônia
   não inventa matéria"). *Alternativa:* escrever antes de retirar, que
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

No último passo, `MaeStructureAccess` confere o acesso com os blocos
realmente presentes. Passou: `BuildClosing` conclui o `Building` com
`ConstructionState.COMPLETED` e o job fica `complete`. Falhou:
`access_not_verified`, bloqueio, e o edifício não conclui. Nenhum outro
indicador persistido de acesso. Após recarga, obra concluída não reconstrói
nem consome material.

### Regressões exigidas pela implementação

Corte de proteção, parte fora do plano, região errada, terreno incompleto,
template alterado, contrato desconhecido, envelope v3 lido como opaco,
falta de material, e cada linha da tabela de retomada provocada por crash
simulado entre intenção, retirada, escrita e confirmação.
