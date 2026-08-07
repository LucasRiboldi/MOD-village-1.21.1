# Initial-Setup-Checklist.md

# Village Colony — Initial Setup Checklist

**Status:** Approved
**Version:** 1.0.0
**Phase:** Project Foundation

---

# 1. Purpose

Este documento define os passos obrigatórios para iniciar a implementação do Village Colony.

Nenhum sistema de gameplay deve ser desenvolvido antes da conclusão desta etapa.

O objetivo é garantir que:

* o ambiente Fabric está correto;
* o projeto compila;
* o Minecraft inicia;
* a estrutura inicial está preparada.

---

# 2. Foundation Principle

Antes de criar inteligência para a colônia:

A base técnica deve existir.

Fluxo obrigatório:

```text
Ambiente

↓

Projeto Fabric

↓

Mod carregando

↓

Arquitetura inicial

↓

Primeiro código de sistema
```

---

# 3. Development Environment Checklist

## Java

Status:

```text
[ ] Confirmado
```

Verificar:

* Java instalado;
* versão compatível com Minecraft 1.21.1;
* variável JAVA_HOME configurada.

---

## IDE

Status:

```text
[ ] Confirmado
```

Recomendado:

```text
IntelliJ IDEA
```

Verificar:

* Gradle reconhecido;
* projeto importado corretamente.

---

## Git

Status:

```text
[ ] Confirmado
```

Verificar:

* repositório criado;
* primeiro commit realizado.

---

# 4. Fabric Project Creation

## Gradle

Status:

```text
[ ] Criado
```

Verificar:

Arquivos:

```text
build.gradle

settings.gradle

gradle.properties

gradlew
```

---

## Fabric Loom

Status:

```text
[ ] Configurado
```

Verificar:

* plugin Fabric Loom;
* mappings;
* dependências.

---

## Minecraft Version

Obrigatório:

```text
1.21.1
```

Status:

```text
[ ] Confirmado
```

---

# 5. Initial Mod Metadata

Criar:

```text
fabric.mod.json
```

Verificar:

Nome:

```text
Village Colony
```

ID:

```text
villagecolony
```

---

Informações:

* versão;
* descrição;
* autores;
* entrypoints.

---

# 6. Package Structure

Criar:

```text
com.villagecolony
```

Estrutura:

A autoridade sobre o layout é:

```text
docs/decisions/ADR-006-Package-Layout.md
```

Resumo — domínio dentro da camada:

```text
com.villagecolony

├── VillageColonyMod.java

├── core

│   ├── type

│   ├── colony      (model + service)

│   ├── worker      (model + service)

│   ├── task        (model + service)

│   ├── resource    (model + service)

│   ├── storage     (model + service)

│   └── construction (model + service)


├── fabric

│   ├── adapter

│   ├── event

│   ├── integration

│   ├── mixin

│   └── brain


└── data

    └── save
```

O agrupamento por camada (`core/model`, `core/service`, `core/manager`)
que constava aqui foi **substituído** pela ADR-006.

---

# 7. First Mod Entry Point

Criar:

```text
VillageColonyMod.java
```

Responsabilidade:

* inicializar o mod;
* registrar eventos;
* confirmar carregamento.

---

Não deve:

* criar sistemas;
* controlar aldeões;
* executar lógica de colônia.

---

# 8. Initial Logging

Adicionar mensagem de inicialização.

Exemplo:

```text
[Village Colony] Mod initialized
```

Objetivo:

Confirmar carregamento.

---

# 9. First Build Test

Executar:

```bash
./gradlew build
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```

---

# 10. First Client Test

Executar:

```bash
./gradlew runClient
```

Verificar:

Minecraft inicia.

---

Confirmar:

```text
[Village Colony] Mod initialized
```

aparece no log.

---

# 11. Clean Environment Test

Criar:

```text
Novo mundo teste
```

Verificar:

* mundo abre;
* nenhum crash;
* nenhum erro crítico.

---

# 12. Server Compatibility Check

Executar teste:

```text
Servidor dedicado local
```

Verificar:

* mod carrega;
* não depende de jogador;
* não usa código apenas client-side.

---

# 13. Repository Structure Validation

Após configuração:

Estrutura esperada:

```text
MOD village++1.21.1

├── src

├── gradle

├── build.gradle

├── fabric.mod.json

├── docs

├── claude

└── README.md
```

---

# 14. First Commit

Após conclusão:

Criar commit:

```text
Initial Fabric project setup
```

---

# 15. Forbidden Actions Before Completion

Não criar antes desta etapa:

* Worker System;
* Resource System;
* Construction System;
* AI;
* Tasks;
* novas entidades.

---

# 16. Foundation Completion Criteria

A fase está concluída quando:

## Ambiente

✅ Java configurado

✅ Gradle funcionando

✅ Fabric configurado

## Projeto

✅ Mod compila

✅ Minecraft inicia

✅ Mod aparece no log

## Estrutura

✅ Pacotes criados

✅ Documentação preservada

---

# 17. Next Phase

Após conclusão:

Avançar para:

```text
Phase 2 — Core Models
```

Primeiras classes:

```text
Colony

Worker

Task

Resource

Storage

Building
```

---

# 18. Claude Code Execution Instruction

Ao executar esta fase:

O agente deve:

1. Ler toda documentação.
2. Criar somente a fundação.
3. Não implementar gameplay.
4. Confirmar build funcionando.
5. Atualizar Project-State.md.

---

# Final Rule

Um projeto sólido começa com uma fundação simples e verificável.

Nenhuma colônia pode crescer se o mundo onde ela vive não for estável.
