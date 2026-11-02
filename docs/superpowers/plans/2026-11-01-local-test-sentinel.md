# Sentinela Local de Testes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Criar uma varredura local manual que execute as verificações reais do mod e preserve um relatório curto e os artefatos de cada execução.

**Architecture:** Um único programa Python da biblioteca padrão agenda comandos Gradle já existentes, captura sua saída e copia relatórios produzidos pelo Gradle. Wrappers PowerShell e shell apenas encaminham argumentos. A ferramenta não modifica `build.gradle`, CI ou o código do mod.

**Tech Stack:** Python 3 padrão, Gradle Wrapper, XML JUnit/GameTest/PIT existentes.

**Spec:** Solicitação do autor em 2026-11-01: rodar localmente, somente sob chamada explícita, com perfis de pesos diferentes e relatórios úteis ao desenvolvimento.

## Global Constraints

- Minecraft 1.21.1 e Java 21; usar apenas comandos já presentes no Gradle.
- Não adicionar dependências Python, Gradle ou CI.
- Não iniciar execução automática, tarefa agendada ou job de CI.
- Não mascarar instabilidade: uma repetição que diverge resulta `UNSTABLE` e código de saída não zero.
- Durante modo temporário de correção por playtest, não executar JUnit, GameTests ou baterias completas.

## Review Focus

- Diretório de relatório já existente: cada execução deve criar identificador próprio, sem sobrescrever outra.
- Falha de Gradle sem XML: o resumo deve conservar código de saída, erro e logs.
- Relatório XML antigo: só copiar artefato atualizado após início da suite.
- Argumentos inválidos: a ferramenta deve falhar antes de iniciar Gradle.
- Windows e Unix: ambos devem selecionar o wrapper Gradle correto.

---

### Task 1: Orquestrador local manual

**Files:**
- Create: `scripts/test_lab.py`
- Create: `scripts/test_lab.ps1`
- Create: `scripts/test_lab.sh`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: `quick`, `fast`, `full`, `--repeat N`, `--dry-run`, `--list` e
  `--resume <UTC-id>`.
- Produces: `reports/test-runs/<UTC-id>/metadata.json`, `summary.json`, `summary.md`, logs e artefatos copiados.

- [x] **Step 1: Criar verificação seca de perfis**

`python scripts/test_lab.py quick --dry-run` lista as suites sem iniciar Gradle; argumento fora dos três perfis sai não zero.

- [x] **Step 2: Implementar o menor orquestrador**

`quick` roda `test` e `propertyTest`; `fast` acrescenta `runGametest --rerun-tasks`; `full` acrescenta auditoria GameTest e `pitest`. Cada suite grava stdout/stderr, duração, código e XML atualizado. `--repeat` repete o perfil inteiro e classifica divergência como `UNSTABLE`.

- [x] **Step 2.1: Corrigir robustez de relatórios**

A comparação de artefatos percorre arquivos internos, em vez de depender do
mtime do diretório. O resumo é gravado após cada suite; `--resume <UTC-id>`
continua somente suites pendentes. `--list` expõe os perfis sem executar Gradle.

- [x] **Step 2.2: Provar persistência local sem Gradle**

`tests/test_test_lab.py` prova que conteúdo alterado com tamanho e mtime iguais
continua sendo copiado, e que `--resume` grava `resumed_at` em `metadata.json`.
Os dois testes usam diretórios temporários e não chamam Gradle.

- [x] **Step 3: Criar wrappers sem lógica**

`scripts/test_lab.ps1` e `scripts/test_lab.sh` chamam o Python do sistema e repassam todos os argumentos.

- [x] **Step 4: Ignorar relatórios locais**

Adicionar somente `reports/test-runs/` a `.gitignore`.

- [x] **Step 5: Verificar sem baterias**

Rodar `python scripts/test_lab.py quick --dry-run` e o equivalente PowerShell. Confirmar perfil e zero execução Gradle.

### Task 2: Registro e operação

**Files:**
- Modify: `README.md`
- Modify: `STATE.md`
- Modify: `TODO.md`
- Modify: `docs/archive/technical/Development-Log.md`

**Interfaces:**
- Consumes: comandos de Task 1.
- Produces: instrução curta para rodar cada perfil e estado vivo do mecanismo.

- [x] **Step 1: Documentar comando e custos esperados**

README explica perfis, local do último relatório e que GPU só será usada por futura validação visual, não por JUnit/GameTest de servidor.

- [x] **Step 2: Registrar pendência da camada visual**

STATE, TODO e diário deixam claro: ferramenta é manual, não muda CI e cliente/GameTest visual ainda exige prova isolada no perfil `village-max`.

- [x] **Step 3: Verificar referências locais**

Usar busca textual para confirmar os três perfis e o caminho de relatório em README e script.
