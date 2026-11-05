# ADR-045 — Terreno local e prova encadeada de ruas MAE

Estado: desenho ampliado aprovado pelo autor em 2026-11-02; implementação na
branch `codex/mae-terrain`, baseada na Fase 1 integrada pelo PR #48.
Esta extensão da [ADR-044](ADR-044-mae-region-safety.md) não ativa obras no jogo.

## Contexto

A Fase 1 recusa flores e protege ruas durante a inspeção. Preparar terreno
local exige distinguir footprint, volume de trabalho e contexto somente lido.
O autor aprovou vegetação baixa controlada, circulação com salto normal e
conexão a ruas comprovadas, inclusive abertas pelo mod depois da geração.
O [desenho aprovado](../superpowers/specs/2026-11-02-mae-terrain-design.md)
contém os limites; a [Fase 2](../technical/MAE-Fase-2.md) registra evidências.

## Decisão

1. Manter prova de geração por seção, proteções da ADR-044 e bounding box original
   inteiro, incluindo vazios. Nenhuma recaptura, migração de terreno legado ou
   importação de hashes de outra região. Sem prova íntegra, bloquear.
2. `reserveTerrain` cria reserva explícita com política `terrain_v2`. Somente sua
   aceitação promove o envelope `maeWork` de v1 para v2. Ler v1 não o modifica;
   `reserve` mantém o contrato conservador. Contratos desconhecidos permanecem
   opacos, bloqueados e sem reinterpretação por leitores antigos.
3. `TerrainPlanner` avalia no máximo oito candidatos fornecidos pelo chamador,
   sem Minecraft. Compara posições alteradas, movimentação de solo, desnível
   máximo, `LotScore` e coordenadas. Não cria busca global ou outro grafo.
4. Corte/aterro de até dois blocos por coluna; duas camadas contínuas de suporte
   sob o piso; transição a até seis blocos do footprint ou corredor explícito.
   Toda superfície preparada precisa alcançar a entrada por passos ortogonais
   com desnível máximo de um, dois blocos livres e folga adicional de cabeça no
   lado baixo dos degraus. Não depender do salto especial do mod.
5. `Building` conserva seus limites. `WorkArea` inclui todas as escritas;
   margem de inspeção de dois blocos não concede escrita. Manter 4.096 células
   no trabalho, oito seções com margem e teto combinado de 1.024 registros
   de plano/histórico/publicação. Reservas mantêm os tetos da ADR-044.
6. Permitir somente estas plantas baixas: `SHORT_GRASS`, `FERN`, `DANDELION`,
   `POPPY`, `BLUE_ORCHID`, `ALLIUM`, `AZURE_BLUET`, `RED_TULIP`, `ORANGE_TULIP`,
   `WHITE_TULIP`, `PINK_TULIP`, `OXEYE_DAISY`, `CORNFLOWER`, `LILY_OF_THE_VALLEY`.
   Remover apenas as posições previstas. Flores na margem ficam intactas;
   árvores, plantas altas, água, lavouras, estruturas e proteções continuam vetadas.
7. `MaeTerrainPreparation` amostra incrementalmente e revalida a seção inteira
   antes de continuar. Limites compartilhados por mundo: 256 leituras amostradas
   e dois milissegundos entre operações indivisíveis. O writer mantém quatro
   tentativas e dois milissegundos. Hashes, inspeções e aprovação são síncronos,
   limitados por volume; esses limites não constituem SLA de dois milissegundos.
8. Âncora de rua é explícita, na mesma região, conferida em X/Y/Z no mundo,
   índice e grafo `PRIMARY` da própria colônia. Preservar suporte e clearance
   da rua como contexto somente lido. Consulta não chama migração/ativação de grafo.
   Sem âncora, o resultado informa `external_access_unverified`.
9. Rua posterior à geração exige reserva anterior, plano tipado `road_v2` e
   mudanças confirmadas no diário da própria região. Confirmar cada estado e
   hash antes de publicar no índice/grafo existente. Preservar papéis `ENTRANCE`;
   extensão deve alcançar o hub. Índice sozinho não comprova a rua.
10. Os sete pontos legados — `RoadPaving`, `PathPaving`, `WorkPath`,
    `BuilderPaving`, `SiteOpening.pavePathTo`, `StreetGaps`, `StreetSurface.unworn` —
    recusam antes de qualquer efeito em seção supervisionada. Não há fallback
    direto, agendamento ou conversão implícita em operação MAE. Produtores
    legados de metadados adiam redescoberta na dimensão com reserva MAE:
    seus percursos podem atravessar outra colônia e não têm filtro espacial seguro.
11. Uma obra ativa por região. Seções compartilhadas por inspeções são exclusivas;
    trabalho pausado, bloqueado ou desconhecido mantém a exclusão após recarga.
    Publicação ou escrita incerta bloqueia retomada e repetição de materiais.
12. Sortear e fixar loot antes da escrita, registrar intenção, escrever e confirmar
    o estado físico, entregar por `Cuttings.keep`, confirmar materiais e avançar.
    Baús nomeados são preservados; estoque público cheio deixa sobra física no
    chão. Não prometer transação atômica entre mundo, inventário e NBT.
13. Ampliar `FOUNDATION_GROUND` (Regra 90) somente para aterro, superfície e apoio
    de células MAE aprovadas, inclusive corredor e transição, usando solo estável
    do bioma. Calçamento mantém `SHAPED_GROUND`. Contar apenas colocações reais
    confirmadas. Limites e regras do escritor legado não mudam.
14. Plano sem alterações é válido, mas não cria trabalho vazio ou permissão futura.
    A execução final verifica suporte, clearance e conectividade de toda superfície.
    Divergência bloqueia com motivo; nunca troca silenciosamente lote ou âncora.

## Consequências

O contrato local pode ser validado sem abrir saves do jogador. Não resolve o
planejamento global das próximas fases, não conecta automaticamente a vila e
não integra escritores legados de edificações. Reservas podem bloquear rotas ou
redescoberta de metadados além da própria caixa; é a escolha conservadora até
existir integração espacial comprovada. Mudança natural na mesma seção também
invalida a evidência. Medição em carga e playtest continuam necessários antes
de ativar orquestração automática.

## Emenda de 2026-11-05 ao item 10 (autor) — região entregue

O item 10 valia para qualquer reserva, sem prazo: a primeira obra parava para sempre a redescoberta de ruas e a releitura
do grafo (Regra 177) em toda a dimensão, e o caminho comum não entrava nas seções da região. A obra pronta nunca se ligava
à vila, porque `road_v2` só certifica rua dentro da região e o caminho comum não entra nela.

Agora a região tem fim. Em vila nova, obra concluída entrega a região (`MaeHandover`, Regra 187): ela deixa de contar em
`legacyAllowed`, em `metadataAllowed`, nas travas de seção e na sobreposição, e não recebe outra obra. Reserva sem nenhuma
escrita também é entregue quando a obra falha. Durante a obra, o item 10 segue igual: pausado, bloqueado ou desconhecido
mantém a exclusão, e escrita sem confirmação nunca é entregue. `/vc mae soltar` entrega as paradas a pedido do autor.

Consequência: depois da entrega, o caminho comum pode mexer perto da obra, porque o histórico da região fica só como
registro. Vila antiga não tem entrega automática e continua conservadora.

*Não escolhidas:* manter a região fechada e seguir só com a releitura, o que deixaria a porta sem ligação física; e manter
o item 10 como estava, o que congelaria o grafo do mundo na primeira obra.
