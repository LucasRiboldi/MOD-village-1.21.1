# Decisões de arquitetura (ADR)

Índice gerado em 2026-11-13 a partir dos arquivos desta pasta. **Estado** é o da decisão hoje; **Emendada por** lista as
ADRs que dizem emendá-la no próprio cabeçalho. O texto de cada ADR é a fonte: em divergência, vale o arquivo, e o índice
se corrige. ADR nova pega o próximo número livre (o maior hoje é 055) e entra aqui no mesmo commit.

| ADR | Título | Estado | Emendada por |
|---|---|---|---|
| [001](ADR-001-Core-Principles.md) | Core Principles | Aceita |  |
| [002](ADR-002-Chunk-Loading-Strategy.md) | Chunk Loading Strategy | Aceita |  |
| [003](ADR-003-Village-Detection.md) | Village Detection | Aceita, emendada |  |
| [004](ADR-004-Mixin-Policy.md) | Mixin Policy | Aceita |  |
| [005](ADR-005-Core-Type-Isolation.md) | Core Type Isolation | Aceita |  |
| [006](ADR-006-Package-Layout.md) | Package Layout | Aceita |  |
| [007](ADR-007-Colony-Merge.md) | Colony Merge | Aceita |  |
| [008](ADR-008-Block-Orientation.md) | Block Orientation | Aceita |  |
| [009](ADR-009-Autonomous-Village-Evolution.md) | Autonomous Village Evolution | Aceita |  |
| [010](ADR-010-Lending-A-Hand-When-Work-Stalls.md) | A mão emprestada: o trabalhador troca de foco quando o dele não anda | Revertida em parte |  |
| [011](ADR-011-Producer-Profession-Growth.md) | Architecture Decision Record 011 | Substituída em parte |  |
| [012](ADR-012-Player-World-Changes.md) | Releitura do mundo após alterações do jogador | Aceita, emendada |  |
| [013](ADR-013-Mine-Route-Rerouting.md) | Ciclo finito da mina e troca da boca no limite | Aceita |  |
| [014](ADR-014-Smelter-Surface-Gathering.md) | Coleta de superfície do fundidor | Aceita |  |
| [015](ADR-015-Construction-First-Material-Distribution.md) | Prioridade híbrida de materiais e cobertura de blocos | Aceita |  |
| [016](ADR-016-construction-economy-and-site-discovery.md) | Economia de construção e descoberta de lotes | Aceita |  |
| [017](ADR-017-lot-ground-and-road-area.md) | Elegibilidade de lotes e area de estrada | Aceita |  |
| [018](ADR-018-village-foundation-contract.md) | Contrato de fundacao automatica da vila | Substituída em parte |  |
| [019](ADR-019-vanilla-structure-catalog-and-terrain-safe-gathering.md) | catálogo Vanilla explícito e coleta segura fora da vila | Aceita |  |
| [020](ADR-020-reparo-ciclico-e-viveiro-da-vila.md) | reparo cíclico de construções e viveiro da vila | Aceita |  |
| [021](ADR-021-pastor-canonico-e-migracao-do-criador.md) | Pastor canonico e migracao do Criador | Aceita |  |
| [022](ADR-022-construction-supply-when-biome-has-no-route.md) | Suprimento de construcao sem rota no bioma | Aceita |  |
| [023](ADR-023-deferred-unsupported-construction-pieces.md) | Pecas de construcao sem apoio permanecem pendentes | Aceita |  |
| [024](ADR-024-physical-warehouse-index.md) | Armazém físico por fotografia (WarehouseIndex) | Retirada (13-11) |  |
| [025](ADR-025-mineiro-autonomo.md) | Mineiro autônomo: mina andável, movimento com ações e registro de veios | Aceita |  |
| [026](ADR-026-bosque-fundacional-da-vila.md) | bosque fundacional da vila | Aceita |  |
| [027](ADR-027-water-mine-access.md) | Acesso selado de mina sob agua e eixos 3x3 | Aceita |  |
| [028](ADR-028-regra-nao-cria-recurso-retirada.md) | Retirada da regra "a colônia não cria recurso" | Aceita |  |
| [029](ADR-029-oficio-da-colonia-exclui-oficio-vanilla.md) | Ofício da colônia exclui ofício Vanilla | Aceita |  |
| [030](ADR-030-profession-policies.md) | Políticas de profissão por mundo | Aceita |  |
| [031](ADR-031-village-resource-sweep-and-fluid-index.md) | Varredura de recursos pela caixa da vila | Aceita |  |
| [032](ADR-032-profession-chest-reserved-slots.md) | Slots reservados nos baús profissionais | Substituída em parte |  |
| [033](ADR-033-emergency-mine-overflow-storage.md) | Armazenamento de emergência no salão da mina | Aceita |  |
| [034](ADR-034-emendas-constituicao-2026-10-06.md) | Emendas à Constituição e reconciliação da fundação | Aceita |  |
| [035](ADR-035-direcao-tecnica-pos-auditoria.md) | Direção técnica depois da auditoria | Aceita |  |
| [036](ADR-036-decisoes-do-autor-2026-10-06.md) | Decisões do autor de 2026-10-06 | Aceita |  |
| [037](ADR-037-decisoes-do-playtest-2026-10-07.md) | Decisões do autor depois do playtest de 2026-10-07 | Aceita |  |
| [038](ADR-038-rotinas-e-trajetos-2026-10-07.md) | Rotinas, trajetos e ócio das profissões | Aceita |  |
| [039](ADR-039-decisoes-pendentes-2026-10-07.md) | Respostas às pendências de 07-10 | Aceita |  |
| [040](ADR-040-despacho-fisico-de-material-da-obra.md) | Despacho fisico de material da obra | Aceita |  |
| [041](ADR-041-decisoes-da-auditoria-2026-10-10.md) | Decisões do autor sobre a auditoria de 10-10 | Aceita |  |
| [042](ADR-042-mina-em-rede.md) | Mina em rede: poço seguro, níveis em grafo e caminho só pelo que está pronto | Aceita |  |
| [043](ADR-043-hierarchical-road-graph.md) | Grafo hierárquico de ruas com migração compatível | Aceita, emendada | ADR-050 |
| [044](ADR-044-mae-region-safety.md) | Elegibilidade e escrita supervisionada do MAE | Aceita, emendada | ADR-047 |
| [045](ADR-045-mae-terrain-road-chain.md) | Terreno local e prova encadeada de ruas MAE | Aceita, emendada | ADR-047 |
| [046](ADR-046-mae-structure-access.md) | Acesso físico e construção supervisionada de estruturas MAE | Aceita, emendada | ADR-047 |
| [047](ADR-047-mae-fila-do-construtor.md) | A obra MAE entra na fila do construtor | Aceita, emendada | ADR-051 |
| [048](ADR-048-tudo-pelo-grafo.md) | Todo caminho e toda rede pelo grafo | Aceita, emendada | ADR-050 |
| [049](ADR-049-socorro-do-mae.md) | O socorro do MAE: a vila sem lote pede a própria obra | Aceita, emendada | ADR-051 |
| [050](ADR-050-grafo-estavel.md) | Grafo estável: todo caminho conta, toda parte liga em reta e o trecho que ficou trava | Aceita |  |
| [051](ADR-051-mae-fechamento.md) | Fechamento do MAE: socorro da vila inteira, bairro, água interna e ruínas antigas | Aceita |  |
| [052](ADR-052-pontas-levam-a-vila.md) | As pontas azuis levam a vila para fora | Aceita |  |
| [053](ADR-053-patamar-da-mina.md) | O patamar nas dobras da escada da mina | Aceita |  |
| [054](ADR-054-client-side-pixel-art-overlays.md) | Overlays pixel art no cliente | Aceita |  |
| [055](ADR-055-decisoes-do-autor-2026-11-13.md) | Decisões do autor de 13-11 (pendências da avaliação de qualidade) | Aceita |  |
