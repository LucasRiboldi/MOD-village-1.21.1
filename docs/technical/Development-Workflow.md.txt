# Development-Workflow.md

# Village Colony Development Workflow

**Status:** Approved
**Version:** 1.0.0

---

# 1. Purpose

Este documento define o fluxo oficial de desenvolvimento do Village Colony.

Ele determina como novas funcionalidades, correções e melhorias devem ser planejadas, implementadas, testadas e integradas.

---

# 2. Development Philosophy

O desenvolvimento deve seguir:

```text id="8m1vnp"
Entender

↓

Planejar

↓

Implementar

↓

Validar

↓

Documentar

↓

Finalizar
```

---

# 3. Session Initialization

Toda sessão de desenvolvimento deve iniciar com análise do estado atual.

Antes de modificar código:

Verificar:

* documentação existente;
* tarefas abertas;
* arquitetura atual;
* código relacionado.

---

# 4. First Action Rule

A primeira ação nunca deve ser escrever código.

Primeiro responder:

1. Qual problema está sendo resolvido?
2. Qual sistema é responsável?
3. Quais arquivos serão alterados?
4. Existe alguma decisão arquitetural envolvida?

---

# 5. Task Selection

Toda implementação deve estar relacionada a uma tarefa.

Fonte:

```text id="2k5xlf"
MVP-Tasks.md
```

ou:

```text id="t0v6hh"
Future-Features.md
```

---

Uma tarefa deve possuir:

* objetivo;
* escopo;
* arquivos envolvidos;
* critério de conclusão.

---

# 6. Task Analysis

Antes de implementar:

Criar uma análise curta:

```text id="55y2pc"
Objetivo:

Sistema afetado:

Arquivos:

Riscos:

Teste necessário:
```

---

# 7. Architecture Check

Antes de criar uma nova classe:

Perguntar:

## Existe uma classe responsável?

Se sim:

Modificar.

---

## Não existe?

Criar nova classe seguindo:

```text id="x5lh9z"
Model

Service

Adapter
```

---

# 8. Implementation Rules

Durante implementação:

## Fazer:

* alterações pequenas;
* código modular;
* nomes claros;
* respeitar arquitetura.

---

## Evitar:

* refatorações gigantes;
* misturar sistemas;
* criar soluções temporárias.

---

# 9. File Modification Rules

Antes de alterar arquivos existentes:

Avaliar:

* impacto;
* dependências;
* compatibilidade.

---

Alterações grandes devem ser divididas.

Exemplo:

Errado:

```text id="i0tmjv"
Criar todo Construction System
em uma alteração
```

---

Correto:

```text id="0y9vgu"
Criar:

Blueprint Model

↓

Construction Service

↓

Builder Adapter
```

---

# 10. Implementation Order

A ordem interna deve seguir:

```text id="5w8z7m"
Data Model

↓

Service Logic

↓

Fabric Adapter

↓

Integration

↓

Test
```

---

Exemplo:

Sistema de recursos:

Primeiro:

```text id="2tbb4n"
Resource
```

Depois:

```text id="6f4f1x"
ResourceService
```

Depois:

```text id="44q9gy"
ChestAdapter
```

---

# 11. Testing After Changes

Toda implementação deve ser validada.

Mínimo:

## Compilação

Verificar:

```bash
./gradlew build
```

---

## Execução

Verificar:

```bash
./gradlew runClient
```

---

## Teste funcional

Confirmar comportamento dentro do Minecraft.

---

# 12. Change Review

Antes de considerar uma tarefa concluída:

Revisar:

## Arquitetura

* camadas respeitadas?

---

## Código

* classes possuem responsabilidade única?

---

## Performance

* existe operação pesada?

---

## Persistência

* dados sobrevivem ao reload?

---

# 13. Documentation Update

Toda mudança importante deve atualizar documentação.

Exemplos:

Nova decisão:

```text
ADR
```

Novo sistema:

```text
System Documentation
```

Nova tarefa:

```text
MVP-Tasks
```

---

# 14. Commit Strategy

Alterações devem ser organizadas em unidades pequenas.

Exemplos:

```text id="6qv7z8"
feat: add colony detection

feat: register villagers

fix: repair worker persistence

refactor: improve task handling
```

---

# 15. Feature Completion Criteria

Uma funcionalidade só está pronta quando:

## Código

Existe implementação.

---

## Integração

Funciona dentro do Minecraft.

---

## Persistência

Funciona após salvar/carregar.

---

## Documentação

Está registrada.

---

## Performance

Não causa impacto significativo.

---

# 16. Bug Fix Workflow

Correções seguem:

```text id="4pwv4j"
Reproduzir

↓

Identificar camada

↓

Corrigir causa

↓

Testar novamente

↓

Registrar
```

---

Nunca:

* esconder erro;
* adicionar exceções artificiais;
* ignorar logs.

---

# 17. Refactoring Rules

Refatoração deve acontecer quando:

* reduz complexidade;
* melhora manutenção;
* corrige arquitetura.

---

Não refatorar apenas por preferência pessoal.

---

# 18. AI Development Rules

Quando utilizando Claude Code:

O agente deve:

1. Ler documentação.
2. Entender arquitetura.
3. Propor plano.
4. Implementar somente o necessário.
5. Testar.
6. Explicar alterações.

---

O agente não deve:

* criar sistemas futuros;
* alterar arquitetura sem aprovação;
* ignorar documentos existentes.

---

# 19. Release Workflow

Antes de uma versão:

Executar:

```text id="xk93t0"
Build

↓

Test

↓

Review

↓

Update Documentation

↓

Release
```

---

# 20. MVP Development Flow

O MVP deve seguir:

```text id="w9jv4k"
Foundation

↓

Colony

↓

Workers

↓

Resources

↓

Production

↓

Construction

↓

Expansion
```

---

# 21. Final Development Principle

Cada linha de código deve responder:

> "Esta implementação aproxima o Village Colony de uma vila Vanilla realmente viva?"

Se não aproximar, ela provavelmente não pertence ao projeto.
