# Resource-System.md

# Village Colony — Resource System

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir como a colônia identifica, controla e utiliza recursos.

O sistema de recursos permite que a colônia:

* saiba quais materiais possui;
* saiba onde estão armazenados;
* identifique faltas;
* solicite coleta;
* solicite fabricação;
* abasteça construções.

---

# Princípio Fundamental

Recursos pertencem à colônia.

O aldeão apenas transporta e armazena temporariamente.

Exemplo:

```text
Colônia

↓

Possui:

100 Oak Logs

↓

Distribuídos:

Baú do Lenhador
Baú do Fabricante
Baú do Construtor
```

---

# Fontes de Recursos

Os recursos possuem três origens.

---

# 1. Recursos Naturais

São obtidos diretamente do mundo.

Exemplos MVP:

```text
Oak Log

Cobblestone
```

Fluxo:

```text
Mundo

↓

Coleta

↓

Baú do Trabalhador

↓

Registro da Colônia
```

---

# 2. Recursos Processados

São criados através de receitas Vanilla.

Exemplo:

```text
Oak Log

↓

Oak Planks
```

Fluxo:

```text
Material bruto

↓

Fabricante

↓

Recipe Manager

↓

Produto

↓

Armazenamento
```

---

# 3. Recursos Estruturais

São materiais necessários para construções.

Exemplo:

```text
Oak Planks

Oak Door

Oak Fence
```

Eles são definidos automaticamente pela análise das estruturas Vanilla.

---

# Modelo de Recurso

Todo recurso conhecido possui:

```text
Resource

- id
- itemType
- category
- quantity
- locations
```

---

# Categorias

## Natural

Obtido do mundo.

Exemplo:

```text
OAK_LOG
```

---

## Processed

Criado por receitas.

Exemplo:

```text
OAK_PLANKS
```

---

## Construction

Necessário para projetos.

Exemplo:

```text
OAK_DOOR
```

---

# Registro de Recursos

A Colônia mantém uma visão agregada.

Exemplo:

```text
Colony Resource Registry


Oak Logs

Total: 64


Locations:

Chest A:
40

Chest B:
24
```

---

# Armazenamento

No MVP não existe armazém central.

Cada trabalhador possui seu próprio armazenamento.

Exemplo:

```text
Lumberjack

↓

Chest

↓

Oak Logs
```

---

# Worker Storage

Cada baú registrado possui:

```text
Storage

- owner
- position
- inventory
- lastUpdate
```

---

# Atualização de Recursos

A colônia não monitora cada mudança de item individualmente.

Ela atualiza através de ciclos de sincronização.

Fluxo:

```text
Simulation Loop

↓

Scan Worker Storage

↓

Compare Registry

↓

Update Resources
```

---

# Necessidade de Recursos

A colônia possui metas mínimas.

Exemplo:

```text
Minimum Stock

Oak Log:
32

Oak Planks:
64
```

---

# Déficit

Quando:

```text
Quantidade Atual < Quantidade Necessária
```

é criado um déficit.

Exemplo:

```text
Necessário:

64 Oak Planks


Atual:

20 Oak Planks


Déficit:

44
```

---

# Geração de Tarefas

O déficit gera tarefas.

Exemplo:

```text
Oak Planks faltando

↓

Verificar receita

↓

Necessário Oak Log

↓

Verificar Oak Log

↓

Criar tarefa de coleta
```

---

# Cadeia de Recursos

A colônia resolve recursos através de uma cadeia.

Exemplo:

```text
Construção precisa:

Oak Door


↓

Recipe Manager


↓

Precisa:

Oak Planks


↓

Precisa:

Oak Logs


↓

Criar tarefa:

Collect Oak Logs
```

---

# Prioridade de Recursos

No MVP:

## Prioridade 1

Recursos de sobrevivência.

Exemplo:

```text
Food
```

---

## Prioridade 2

Recursos de produção.

Exemplo:

```text
Oak Log
```

---

## Prioridade 3

Recursos de construção.

Exemplo:

```text
Oak Door
```

---

# Coleta de Recursos

Um coletor deve:

1. receber tarefa;
2. possuir ferramenta necessária;
3. localizar recurso;
4. coletar;
5. guardar no armazenamento;
6. atualizar registro.

---

# Ferramentas

Cada coleta possui requisito.

Exemplo:

```text
Collect Oak Log

Requires:

Axe
```

Ferramentas iniciais:

```text
Lumberjack

↓

Wooden Axe
```

---

# Fabricação

Toda fabricação utiliza o sistema Vanilla.

Fluxo:

```text
Requested Item

↓

Recipe Manager

↓

Ingredients

↓

Check Resources

↓

Craft Task

↓

Manufacturer
```

---

# Consumo de Recursos

Recursos são consumidos somente quando:

* uma receita é concluída;
* uma construção utiliza o material.

Nunca remover recursos antes da execução.

---

# Construção e Recursos

Antes de iniciar uma construção:

```text
Construction Project

↓

Generate Material List

↓

Resource Check

↓

Available?

↓

Start Construction
```

Caso falte:

```text
Pause Construction

↓

Generate Resource Demand
```

---

# Regras Importantes

## Recursos nunca aparecem automaticamente.

---

## Recursos nunca são duplicados.

---

## A colônia conhece recursos.

---

## Aldeões possuem apenas armazenamento físico.

---

## Receitas devem sempre utilizar Minecraft Recipe Manager.

---

# Fora do MVP

Não implementar:

* economia;
* comércio;
* preços;
* moedas;
* mercado;
* troca entre vilas;
* mineração automática avançada;
* armazenamento central inteligente.

---

# Objetivo Final

Criar um sistema simples onde a vila consiga entender:

"Tenho estes recursos."

"Preciso destes recursos."

"Este aldeão consegue produzir ou coletar."

"Esta construção pode começar."

Esse sistema será a base para toda a cadeia produtiva futura da colônia.
