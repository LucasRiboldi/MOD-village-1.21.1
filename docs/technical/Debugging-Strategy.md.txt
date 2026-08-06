# Debugging-Strategy.md

# Village Colony Debugging Strategy

**Status:** Approved
**Version:** 1.0.0

---

# 1. Purpose

Este documento define o processo oficial de diagnóstico e correção de problemas do Village Colony.

O objetivo é permitir que desenvolvedores encontrem problemas de forma sistemática sem adicionar soluções temporárias que prejudiquem a arquitetura.

---

# 2. Debugging Philosophy

O processo de depuração deve responder:

1. O evento aconteceu?
2. O sistema recebeu a informação?
3. O estado foi alterado corretamente?
4. A ação foi executada no Minecraft?
5. O resultado foi salvo?

---

O fluxo geral:

```text
Minecraft Event

↓

Adapter

↓

Service

↓

Model State

↓

Minecraft Action

↓

Persistence
```

O problema normalmente está em uma dessas etapas.

---

# 3. Debugging Principles

## 3.1 Não corrigir sintomas

Exemplo:

Problema:

"O aldeão não constrói."

Não fazer:

```text
Aumentar velocidade do construtor
```

Antes verificar:

* existe tarefa?
* tarefa possui materiais?
* trabalhador foi atribuído?
* posição é válida?

---

# 4. Debug Layers

O sistema deve ser investigado por camadas.

---

# Layer 1 — Minecraft / Fabric

Pergunta:

"O Minecraft chamou meu código?"

Verificar:

* eventos;
* ticks;
* carregamento;
* entidades.

---

Exemplo:

```text
[COLONY_EVENT]
Server tick executed
```

---

# Layer 2 — Adapter

Pergunta:

"A informação foi convertida corretamente?"

Exemplo:

```text
VillagerEntity

↓

Worker
```

Verificar:

* UUID;
* posição;
* mundo correto.

---

# Layer 3 — Service

Pergunta:

"A regra de negócio funcionou?"

Exemplo:

```text
WorkerService

↓

TaskService
```

Verificar:

* criação;
* prioridade;
* estado.

---

# Layer 4 — Model

Pergunta:

"O estado interno está correto?"

Exemplo:

```text
Worker:

profession = BUILDER

task = BUILD_HOUSE
```

---

# Layer 5 — World Action

Pergunta:

"A ação aconteceu no mundo?"

Exemplo:

```text
placeBlock()

↓

Block colocado
```

---

# 5. Logging System

Todo sistema deve possuir logs controlados.

Formato:

```text
[CATEGORY] Event Description
```

---

Categorias oficiais:

```text
[COLONY]

[WORKER]

[TASK]

[RESOURCE]

[STORAGE]

[BUILD]

[SAVE]

[ERROR]
```

---

# 6. Log Examples

## Criação de Colônia

```text
[COLONY]
Created colony:
id=xxxx
position=x,y,z
```

---

## Registro de Trabalhador

```text
[WORKER]
Registered worker:
uuid=xxxx
profession=LUMBERJACK
```

---

## Criação de Tarefa

```text
[TASK]
Created:
type=COLLECT_WOOD
worker=xxxx
```

---

## Construção

```text
[BUILD]
Placed block:
minecraft:oak_planks
position=x,y,z
```

---

# 7. Debug Levels

O sistema deve possuir níveis.

---

# INFO

Eventos importantes.

Exemplo:

```text
Colony created
Worker registered
Building completed
```

---

# DEBUG

Informações detalhadas.

Exemplo:

```text
Checking resource availability
Searching storage
Evaluating task
```

---

# WARN

Situações inesperadas.

Exemplo:

```text
Worker missing storage
```

---

# ERROR

Falhas reais.

Exemplo:

```text
Failed loading colony data
```

---

# 8. Tick Debugging

Minecraft funciona por ticks.

Quando algo não acontece:

Verificar:

```text
O tick está executando?
```

---

Adicionar temporariamente:

```text
[SIMULATION]
Tick processed
```

---

Nunca deixar:

```text
log por tick permanente
```

---

# 9. Common Problems

---

# Problema: Aldeão não trabalha

Investigar:

## Passo 1

Existe Worker?

```text
Worker registrado?
```

---

## Passo 2

Possui profissão?

```text
profession != null
```

---

## Passo 3

Possui tarefa?

```text
task != null
```

---

## Passo 4

A tarefa possui recursos?

---

## Passo 5

O aldeão está carregado?

---

# Problema: Recursos não aparecem

Investigar:

```text
Worker

↓

Inventory

↓

Storage

↓

ResourceService
```

Verificar:

* item correto;
* baú correto;
* chunk carregado.

---

# Problema: Construção parada

Verificar:

## Projeto

Existe:

```text
ConstructionProject
```

---

## Materiais

Possui:

```text
ResourceRequirement
```

---

## Trabalhador

Existe:

```text
Builder assigned
```

---

## Mundo

Pode colocar bloco?

---

# Problema: Dados desaparecem

Investigar:

```text
Save

↓

Load

↓

Restore
```

Verificar:

* UUID;
* NBT;
* versão dos dados.

---

# 10. Debug Commands Future Support

Futuramente o mod poderá possuir comandos:

```text
/villagecolony info

/villagecolony workers

/villagecolony resources

/villagecolony tasks
```

---

# 11. Development Tools

Ferramentas recomendadas:

## Logs

Primeira ferramenta.

---

## IDE Debugger

Usar:

* breakpoints;
* inspeção de estado;
* stack trace.

---

## Minecraft Test Worlds

Manter mundos:

```text
test_world_empty

test_world_village

test_world_construction
```

---

# 12. Debug Test Scenarios

Criar cenários controlados.

---

# Scenario 1 — New Village

Objetivo:

Validar criação.

Esperado:

```text
Village detected

Colony created
```

---

# Scenario 2 — Worker Cycle

Objetivo:

Validar trabalho.

Esperado:

```text
Task created

Worker assigned

Action completed
```

---

# Scenario 3 — Save Reload

Objetivo:

Validar persistência.

Esperado:

```text
Data restored
```

---

# Scenario 4 — Construction

Objetivo:

Validar expansão.

Esperado:

```text
Materials consumed

Block placed

Building registered
```

---

# 13. Debug Data Visibility

Sempre que possível, informações importantes devem ser observáveis.

Exemplos:

* posição da colônia;
* profissão;
* tarefa atual;
* recursos conhecidos;
* construção ativa.

---

# 14. Avoid Temporary Code

Não deixar no projeto:

* prints;
* comandos de teste;
* hacks;
* valores fixos.

Antes de finalizar:

Remover ou transformar em sistema oficial.

---

# 15. Bug Report Template

Todo problema deve registrar:

```text
## Problem

Descrição.

## Environment

Minecraft:
Fabric:
Version:

## Steps

Como reproduzir.

## Expected

Resultado esperado.

## Actual

Resultado ocorrido.

## Logs

Mensagens relevantes.
```

---

# 16. Final Debugging Rule

Nunca perguntar:

"Por que não funciona?"

Perguntar:

"Em qual camada o comportamento deixou de acontecer?"

Essa abordagem mantém o Village Colony investigável, escalável e sustentável durante toda a evolução do projeto.
