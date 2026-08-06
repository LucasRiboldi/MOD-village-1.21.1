# Profession-System.md

# Village Colony — Profession System

Version 1.0.0

Status Approved

---

# Objetivo

Definir como profissões funcionam dentro da colônia.

O sistema de profissões é responsável por

 identificar capacidades dos aldeões;
 atribuir funções;
 validar quais tarefas cada aldeão pode executar;
 substituir profissões quando necessário.

---

# Princípio Fundamental

Uma profissão representa uma capacidade.

Ela não representa inteligência.

A profissão responde

 O que este aldeão sabe fazer

A Colônia responde

 O que precisa ser feito agora

---

# Relação com Aldeões Vanilla

O sistema não substitui as profissões Vanilla.

O aldeão continua possuindo

 profissão Vanilla;
 estação de trabalho;
 rotina Vanilla.

A profissão da colônia funciona como uma camada adicional.

Exemplo

```text
Minecraft Villager

+

Colony Profession

=

Colony Worker
```

---

# Modelo de Profissão

Toda profissão possui

```text
Profession

- id
- name
- requiredTool
- capabilities
- allowedTasks
```

---

# Capacidades

Uma capacidade define uma ação que a profissão consegue executar.

Exemplos

```text
CAPABILITY_COLLECT_WOOD

CAPABILITY_CRAFT

CAPABILITY_BUILD
```

---

# Profissões do MVP

O MVP possui quatro profissões.

---

# 1. Lumberjack

## Função

Coletar madeira para a colônia.

---

## Capacidade

```text
COLLECT_WOOD
```

---

## Ferramenta

Inicial

```text
Wooden Axe
```

---

## Produção

Entrada

```text
World
```

Saída

```text
Oak Log
```

---

## Fluxo

```text
Task

Collect Oak Log

↓

Lumberjack

↓

Equip Axe

↓

Encontrar árvore

↓

Coletar madeira

↓

Guardar recurso

↓

Finalizar tarefa
```

---

# 2. Manufacturer

## Função

Transformar recursos utilizando receitas Vanilla.

---

## Capacidade

```text
CRAFT_ITEMS
```

---

## Ferramenta

Nenhuma no MVP.

---

## Produção Inicial

Entrada

```text
Oak Log
```

Saída

```text
Oak Planks
```

---

## Fluxo

```text
Task

Craft Oak Planks

↓

Manufacturer

↓

Consultar Recipe Manager

↓

Consumir ingredientes

↓

Criar item

↓

Armazenar resultado
```

---

# 3. Farmer

## Função

Manter produção básica de alimentos.

---

## Capacidade

```text
MAINTAIN_FOOD
```

---

## Estado no MVP

A profissão existe para controle populacional.

A lógica completa de agricultura será adicionada futuramente.

---

## Futuro

Possíveis capacidades

 plantar;
 colher;
 replantar;
 armazenar alimentos.

---

# 4. Builder

## Função

Executar construções aprovadas pela colônia.

---

## Capacidade

```text
BUILD_STRUCTURE
```

---

## Ferramenta

Nenhuma no MVP.

---

## Responsabilidades

Pode

 posicionar blocos;
 preparar terreno natural;
 executar projetos.

Não pode

 escolher construções;
 coletar recursos;
 fabricar materiais;
 remover infraestrutura protegida.

---

# Ferramentas das Profissões

Ferramentas são propriedade da profissão.

O trabalhador recebe automaticamente a ferramenta inicial.

MVP

 Profissão     Ferramenta 
 ------------  ---------- 
 Lumberjack    Wooden Axe 
 Manufacturer  Nenhuma    
 Farmer        Wooden Hoe 
 Builder       Nenhuma    

---

# Evolução das Ferramentas

Futuro

A ferramenta poderá evoluir conforme recursos disponíveis.

Exemplo

```text
Wood

↓

Stone

↓

Iron

↓

Diamond
```

Esta funcionalidade não pertence ao MVP.

---

# Seleção de Profissão

A Colônia mantém uma necessidade mínima de funções.

Exemplo

```text
Population

6 Villagers

Needed

1 Builder
1 Lumberjack
1 Manufacturer
1 Farmer
```

---

# Nascimento de Novos Aldeões

Quando um novo aldeão adulto surgir

Fluxo

```text
Adult Villager

↓

Check Missing Profession

↓

Assign Profession

↓

Register Worker
```

---

# Morte de Trabalhadores

Quando um trabalhador morrer

A Colônia registra

```text
Missing Profession

=
Lumberjack
```

O próximo aldeão disponível poderá assumir essa função.

---

# Compatibilidade de Tarefas

Antes de receber uma tarefa

A Colônia verifica

```text
Task Requirement

↓

Profession Capability

↓

Worker Available

↓

Assignment
```

Exemplo

```text
Task

Build House

Required

BUILD_STRUCTURE

↓

Builder found

↓

Assign
```

---

# Estado do Trabalhador

Um aldeão possui

```text
Worker State

AVAILABLE

BUSY

RETURNING

IDLE
```

---

# Regras de Arquitetura

## Profissões não criam tarefas.

Apenas executam.

---

## Profissões não possuem prioridades.

A Colônia decide prioridades.

---

## Profissões não conhecem outras profissões.

O Lumberjack não sabe que o Builder existe.

---

## Novas profissões devem ser adicionadas sem modificar as existentes.

Exemplo futuro

```text
Miner

Fisher

Blacksmith

Mason
```

Devem apenas implementar novas capacidades.

---

# Objetivo Final

Criar trabalhadores especializados que funcionam como uma extensão natural dos aldeões Vanilla.

O sistema deve permitir que uma vila pequena evolua gradualmente para uma sociedade organizada sem substituir as regras originais do Minecraft.
