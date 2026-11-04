# ADR-046 — Acesso físico e construção supervisionada de estruturas MAE

Estado: contrato A, implementação e testes focados aprovados pelo autor
em 2026-11-04. Implementação parcial; escrita de edifícios ainda não habilitada.
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
