# Sentinela Observatório — desenho aprovado

Data: 02-11-2026
Status: implementado localmente; baterias do mod temporariamente suspensas.

## Objetivo

Transformar a saída do Sentinela Local em uma evidência legível sem introduzir
outro framework, dependência de rede ou execução automática. O autor escolhe o
perfil, as suites, a repetição e o limite de tempo na tela Windows ou na linha
de comando.

## Contrato

- `scripts/test_lab.py` continua sendo a única execução dos comandos Gradle.
- `--only unit,property` filtra somente suites já pertencentes ao perfil.
- `--budget-minutes MIN` não mata Gradle. Quando o limite chega, a suite em
  curso termina; a próxima não inicia. O relatório recebe `INTERRUPTED` e pode
  ser retomado por `--resume`.
- Cada suite copia seus XMLs alterados. O painel lê essa cópia, não o diretório
  mutável `build/`.
- `dashboard.html` é estático, autocontido e abre localmente. Exibe casos
  aprovados/falhos/errados/ignorados, assinaturas agrupadas, testes lentos,
  suites, logs e histórico das últimas 50 execuções.
- XML ausente ou inválido não muda o resultado Gradle. Isso evita um painel
  incompleto declarar sucesso ou falha inexistente.

## Limites deliberados

Não há paralelismo de GameTests, execução em login, envio de telemetria,
dependência Allure nem medição de GPU nesta versão. O filtro é por suite, não
por domínio de gameplay: categorias como ruas e profissões exigem marcadores
de teste confiáveis antes de serem expostas na tela.

## Validação desta entrega

O modo temporário do projeto proíbe rodar JUnit, GameTests, PIT e perfis do
Sentinela. A entrega valida sintaxe Python, importação da tela e `--dry-run`.
Uma execução real permanece ação explícita do autor.
