# Sentinela Local — melhorias futuras preservadas

Data: 02-11-2026. Não implementar sem nova solicitação.

## B — publicação e histórico compartilhado

Avaliar Allure somente quando relatórios precisarem ser comparados entre
máquinas ou CI. Preservar `summary.json`, XMLs e `dashboard.html`; a nova
camada deve ser opcional e ter versão fixada. Critérios: valor para mais de uma
máquina, exportação reproduzível e nenhuma substituição dos arquivos locais.

## C — laboratório de desempenho

Adicionar perfil explícito de carga para GameTests seriais, cenários repetidos
e gravação Java Flight Recorder limitada por duração. Separar GPU/cliente em
spike Fabric próprio, pois servidor headless não mede renderização. Critérios:
orçamento de CPU, memória e disco definido pelo autor; relatório JFR salvo em
`reports/`; nenhuma carga inicia por padrão.

## Pré-requisito comum

Criar marcadores confiáveis por domínio de gameplay antes de oferecer filtros
como `ruas`, `terreno` ou `profissões`. O manifesto atual informa cobertura,
mas não é um seletor executável de testes.
