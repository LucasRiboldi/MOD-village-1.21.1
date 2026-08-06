# Simulation-Loop.md

# Village Colony — Simulation Loop

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir o ciclo de atualização da colônia.

Este documento descreve quando a colônia:

* observa o mundo;
* atualiza informações;
* identifica necessidades;
* cria tarefas;
* acompanha execução.

---

# Princípio Fundamental

A colônia não funciona como uma IA contínua.

Ela funciona através de ciclos de avaliação.

Em cada ciclo:

```text
Observar

↓

Avaliar

↓

Planejar

↓

Executar

↓

Atualizar
```

---

# Visão Geral

```text
Minecraft World

↓

Colony Update Cycle

↓

Resource Analysis

↓

Demand Generation

↓

Task Creation

↓

Villager Execution

↓

State Update
```

---

# Frequência do Ciclo

A simulação não deve executar decisões pesadas em todos os ticks do Minecraft.

O ciclo da colônia possui uma frequência própria.

Exemplo:

```
Minecraft Tick

↓

Atualização leve

↓

Colony Tick periódico

↓

Processamento da simulação
```

---

# Etapas do Loop

## 1. World Observation

A colônia coleta informações atuais.

Informações observadas:

* aldeões existentes;
* profissões;
* construções;
* recursos;
* tarefas ativas.

Não são tomadas decisões nesta etapa.

---

# 2. Colony State Update

A colônia atualiza seu estado interno.

Exemplos:

* novo aldeão nasceu;
* aldeão morreu;
* construção terminou;
* recurso foi armazenado;
* tarefa foi cancelada.

---

# 3. Resource Evaluation

A colônia verifica seus recursos.

Exemplo:

```
Necessário:

Oak Planks: 64

Disponível:

Oak Planks: 20
```

Resultado:

```
Resource Deficit:
Oak Planks -44
```

---

# 4. Demand Generation

Quando existe uma necessidade, a colônia cria uma demanda.

Exemplos:

## Madeira insuficiente

```
Demand:

Collect Oak Log
```

---

## Material insuficiente

```
Demand:

Craft Oak Planks
```

---

## Necessidade de expansão

```
Demand:

Build New House
```

---

# 5. Task Creation

Demandas são transformadas em tarefas executáveis.

Exemplo:

```text
Demand

↓

Collect Oak Log

↓

Task
```

Uma tarefa possui:

```
Task

- Type
- Priority
- Required Profession
- Required Resources
- Executor
- Status
```

---

# 6. Task Assignment

A colônia procura um aldeão compatível.

Critérios:

1. profissão correta;
2. aldeão disponível;
3. ferramenta necessária;
4. distância aceitável.

Exemplo:

```
Task:

Collect Oak Log

↓

Required:

Lumberjack

↓

Assigned:

Villager #12
```

---

# 7. Task Execution

O aldeão executa a tarefa.

Durante a execução:

* segue regras da profissão;
* utiliza ferramentas;
* coleta ou transforma recursos;
* atualiza seu armazenamento.

---

# 8. Task Completion

Quando concluída:

A tarefa muda:

```
EXECUTING

↓

COMPLETED
```

A colônia atualiza:

* recursos;
* construção;
* profissão;
* histórico.

---

# 9. Reassessment

Após mudanças importantes, a colônia reavalia sua situação.

Exemplo:

Antes:

```
Falta madeira
```

Depois:

```
Madeira suficiente
```

Nova prioridade:

```
Produzir tábuas
```

---

# Prioridade das Demandas

No MVP:

A ordem de prioridade será:

## 1. Sobrevivência

Exemplos:

* comida;
* recursos básicos.

---

## 2. Produção

Exemplos:

* madeira;
* materiais processados.

---

## 3. Construção

Exemplos:

* nova casa;
* expansão.

---

# Estados da Colônia

No MVP existem apenas três estados.

---

## Stable

A colônia possui recursos suficientes.

Nenhuma expansão necessária.

---

## Production

A colônia precisa repor recursos.

Prioridade:

* coleta;
* fabricação.

---

## Expansion

A colônia possui recursos suficientes e pode crescer.

Prioridade:

* construção;
* infraestrutura.

---

# Exemplo Completo

Situação:

```
Vila possui 5 casas

População aumenta

↓

Faltam camas

↓

Colony State:

Expansion

↓

Criar Projeto:

New House

↓

Calcular materiais

↓

Verificar estoque

↓

Falta Oak Planks

↓

Criar Task:

Craft Oak Planks

↓

Fabricante executa

↓

Material disponível

↓

Criar Task:

Build House

↓

Construtor executa

↓

Nova casa registrada
```

---

# Regras Importantes

## A colônia nunca executa ações diretamente.

Ela apenas cria tarefas.

---

## Aldeões nunca criam tarefas.

Eles apenas executam tarefas recebidas.

---

## Uma tarefa possui apenas um executor.

Evita conflitos.

---

## Tarefas podem ser canceladas.

Exemplos:

* aldeão morreu;
* construção removida;
* recurso deixou de ser necessário.

---

# Objetivo do Loop

Criar uma simulação simples onde:

* a colônia observa;
* a colônia decide;
* os aldeões trabalham;
* o mundo muda;
* a colônia se adapta.

Esse ciclo é a base de toda evolução futura do projeto.
