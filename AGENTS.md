# AGENTS.md — Village Colony (MOD-village-1.21.1)

Instruções para agentes que seguem o padrão AGENTS.md (Codex CLI e afins).

## ⚠️ Fonte canônica de regras

**Leia `CLAUDE.md` na raiz do projeto antes de qualquer tarefa.** Ele é o
documento canônico de instruções para agentes de IA neste repositório.
Este arquivo apenas resume o essencial e aponta para lá; em caso de
divergência, **`CLAUDE.md` vence**.

Também são leitura obrigatória, conforme o caso:
- `PROJECT_CONSTITUTION.md` — princípios permanentes do projeto.
- `docs/proxima-sessao.md` — estado atual da sessão de trabalho.
- `README.md` — visão geral do mod.

## Visão geral do projeto

- Mod **Fabric** para **Minecraft 1.21.1**, Java 21, build via Gradle wrapper.
- Transforma vilas Vanilla em colônias autônomas.
- **Versões fixas** em `gradle.properties`. Nunca use `latest`, `+` ou
  versões dinâmicas em dependências.

## Build, testes e validação

```bash
./gradlew build          # compila + testa
./gradlew test           # testes unitários (inclui testes de arquitetura)
./gradlew runGametest    # sobe servidor Fabric de teste (lento, minutos)
```
