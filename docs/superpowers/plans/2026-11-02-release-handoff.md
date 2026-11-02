# Release Handoff Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Padronizar o pré-voo de PR e o registro auditável de releases.

**Architecture:** Um script Python valida metadados existentes sem editar documentos humanos. Um registro Markdown por versão é a fonte de evidência; o workflow de release o incorpora à descrição e ao resumo do GitHub.

**Tech Stack:** Python 3.9+, GitHub Actions, Markdown.

**Spec:** Pedido do autor em 2026-11-02: criar `FECHAR-RAPIDO` e `REGISTRO-RELEASE`.

## Global Constraints

- Não editar automaticamente README, STATE ou CHANGELOG.
- Release exige JAR como asset e SHA-256, conforme ADR-036.
- Testes Java permanecem desativados no modo de correção por playtest.

## Review Focus

- Registro pendente não pode publicar release.
- Versão ausente em documento vivo deve falhar no pré-voo.
- SHA deve vir do JAR informado, nunca de texto manual.

### Task 1: Comandos e registros

**Files:** `scripts/release_workflow.py`, `docs/releases/README.md`, `docs/releases/v0.4.4.md`

- [x] Criar `FECHAR-RAPIDO` e `REGISTRO-RELEASE`.
- [x] Recusar registro incompleto ou pendente.

### Task 2: Integração e documentação

**Files:** `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `README.md`, `STATE.md`, `CHANGELOG.md`, `gradle.properties`

- [x] Manter CI completo em PR/main e reduzir duplicação em push de branch.
- [x] Incluir evidência de release no GitHub e documentar palavras-chave.
