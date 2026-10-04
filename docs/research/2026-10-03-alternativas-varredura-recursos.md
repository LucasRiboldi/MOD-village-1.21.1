# Alternativas para varredura ampla de recursos — 2026-10-03

## Pergunta

Vale a pena ler todos os blocos de chunks próximos para criar rotas de serviço
das profissões? A resposta atual é **não sem medição**. Um chunk 1.21.1 com
384 blocos de altura contém 16 x 16 x 384 = 98.304 posições. Ler oito chunks
inteiros já aproxima 786 mil consultas, antes de pathfinding, filtros e chunks
verticais adicionais. Repetir isso no thread do servidor ameaça o orçamento de
50 ms por tick; fazê-lo fora do thread exige snapshots imutáveis que o mod não
possui e não pode ler o mundo diretamente com segurança.

## Alternativas

| Opção | Custo | Atualização | Qualidade da rota | Risco |
|---|---:|---|---|---|
| A. Varredura integral de blocos | 98.304 leituras/chunk | refazer após mudanças | alta fotografia inicial | alto custo e índice rapidamente obsoleto |
| B. Superfície por heightmap e cursor | 256 colunas/chunk | incremental | boa para areia, solo e árvores | não cobre minério subterrâneo |
| C. Índice por eventos de bloco | baixo após aquecimento | invalidação imediata | bom para recursos conhecidos | precisa cobrir geração, explosão, pistão e jogador |
| D. Candidatos preguiçosos + pathfinding Vanilla | pago quando há demanda | mundo validado no uso | melhor relação custo/benefício | primeira busca pode demorar vários ciclos |
| E. POIs e inventários, superfície por cursor, mina geométrica | especializado | por subsistema | rotas coerentes com cada profissão | mais de um índice especializado |
| F. Janela próxima ao jogador | limitada aos chunks ativos | natural pelo carregamento | boa onde o jogo acontece | não planeja área distante descarregada |

## Recomendação

Adotar a opção E, combinada com D: baús e camas usam POIs/registries; recursos
de superfície mantêm a espiral incremental já limitada a 1.024 colunas; a mina
usa sua geometria persistida; cada alvo é validado no mundo e pelo pathfinding
antes da reserva. Um índice por eventos (C) só deve entrar depois de um perfil
mostrar que a busca preguiçosa ainda domina o MSPT.

Não carregar chunks para completar índice, não persistir água/lava derivada do
mundo e não acessar `ServerWorld` fora do thread do servidor. A experiência a
medir antes de nova implementação é: leituras por ciclo, candidatos válidos,
tempo até primeiro alvo, falhas de rota e MSPT p95 em vila grande. Comparar B,
C e D no mesmo save; só aceitar mudança que mantenha p95 abaixo de 50 ms e
reduza o tempo parado sem aumentar chunks carregados.

## Estado

Estudo apenas. A espiral de superfície e o índice transitório de fluidos da
ADR-031 permanecem; nenhuma varredura integral de chunk foi ativada.

## Atualização de 2026-10-04

O aviso `site_sweep_budget_exhausted` visto no Spark pertence ao
`BuildSiteScanner` da construção, não à coleta de recursos: é o cursor de lote
de obra respeitando seu orçamento, e não prova de uma varredura de vila parada.
Para a coleta, foi corrigido o caso em que uma coluna de chunk descarregado era
avançada e nunca mais observada: ela fica pendente e é revista em passagens
incrementais quando o chunk carregar. Água e lava continuam sendo memória
transitória e são reconstruídas quando a área cresce.

Próximas alternativas, nesta ordem: medir leituras, candidatos e falhas por
profissão; usar heightmap apenas onde a medição acusar superfície cara; só então
prototipar um índice invalidado por eventos. A leitura de todos os blocos do
chunk segue rejeitada enquanto não houver perfil que prove benefício maior que o
custo.
