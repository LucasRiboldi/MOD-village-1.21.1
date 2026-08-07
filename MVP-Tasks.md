# MVP-Tasks.md

# Village Colony — MVP Development Tasks

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir a sequência de implementação do MVP.

O objetivo é transformar a arquitetura documentada em um mod Fabric funcional.

---

# Regra de Desenvolvimento

Cada tarefa deve:

* possuir uma responsabilidade única;
* gerar um resultado observável;
* não quebrar sistemas anteriores.

---

# Fase 0 — Preparação do Projeto

## TASK-001 — Criar Projeto Fabric

Objetivo:

Criar o projeto base.

Implementar:

* Fabric Loader;
* Fabric API;
* Minecraft 1.21.1;
* Java configurado.

Resultado esperado:

```text
Minecraft inicia com o mod carregado.
```

---

## TASK-002 — Configurar Identidade do Mod

Criar:

* mod id;
* nome;
* versão;
* arquivo fabric.mod.json.

Resultado:

```text
Fabric reconhece Village Colony.
```

---

## TASK-003 — Criar Estrutura de Pacotes

Criar:

```text
core/

fabric/

data/
```

Resultado:

Código preparado para crescimento.

---

# Fase 1 — Núcleo da Colônia

---

## TASK-004 — Criar Classe Principal do Mod

Criar:

```text
VillageColonyMod
```

Responsável por:

* inicialização;
* registro de eventos.

Resultado:

Mod inicia corretamente.

---

## TASK-005 — Criar Modelo Colony

Criar:

```text
Colony
```

Contendo:

* id;
* posição;
* estado.

Resultado:

A colônia pode existir em memória.

---

## TASK-006 — Criar Colony Manager

Criar:

```text
ColonyManager
```

Responsável por:

* registrar colônias;
* buscar colônias existentes.

Resultado:

O jogo consegue administrar colônias.

---

# Fase 2 — Persistência

---

## TASK-007 — Criar Colony Saved Data

Criar:

```text
ColonySavedData
```

Salvar:

* colônias;
* identificadores;
* posições.

Resultado:

Dados sobrevivem ao fechar mundo.

---

## TASK-008 — Testar Carregamento

Validar:

```text
Criar mundo

↓

Encontrar vila

↓

Salvar

↓

Fechar

↓

Abrir

↓

Colony permanece
```

---

# Fase 3 — Detecção da Vila

---

## TASK-009 — Detectar Estruturas Vanilla

Implementar:

* busca por vila;
* identificação do bioma.

MVP:

Somente:

```text
Plains Village
```

---

## TASK-010 — Criar Colônia Automaticamente

Quando vila encontrada:

Criar:

```text
Colony
```

Resultado:

A vila passa a existir como entidade lógica.

---

# Fase 4 — Sistema de Trabalhadores

---

## TASK-011 — Criar Worker Model

Criar:

```text
Worker
```

Dados:

* UUID;
* profissão;
* colônia.

---

## TASK-012 — Detectar Aldeões

Implementar:

```text
VillagerScanner
```

Resultado:

Todos aldeões da vila são registrados.

---

## TASK-012b — Persistir Trabalhadores

Estender:

```text
ColonySavedData
```

Decisão (2026-08-07): estender o save existente em vez de criar um
`WorkerSavedData`. Um segundo arquivo permitiria worker órfão apontando
para colônia que não foi gravada, sem transação que mantivesse os dois
em sincronia.

Gravar por trabalhador:

* villagerId;
* colonyId;
* profissão, quando houver.

Carregar com `Worker.restore`, que já aceita profissão ausente.

Motivo:

Profissão de colônia é decisão do mod, não existe no mundo Vanilla e
sumiria ao fechar o mundo. Sem isto, cada sessão redistribuiria funções
do zero e a TASK-014 não se sustenta.

Esta tarefa não constava do plano original. Ver Project-State.md §7.

---

## TASK-013 — Criar Sistema de Profissões

Implementar:

```text
ProfessionRegistry
```

Adicionar:

* Lumberjack;
* Manufacturer;
* Farmer;
* Builder.

---

## TASK-014 — Atribuição Inicial de Profissões

Quando faltar profissão:

Fluxo:

```text
Novo aldeão

↓

Verificar vaga

↓

Receber função
```

---

# Fase 5 — Sistema de Armazenamento

---

## TASK-015 — Detectar Baús dos Trabalhadores

Implementar:

Busca:

```text
Casa

↓

Cama

↓

Baú próximo
```

---

## TASK-016 — Registrar Storage

Criar:

```text
StorageRegistry
```

Guardar:

* posição;
* trabalhador proprietário.

---

## TASK-017 — Ler Inventário dos Baús

Implementar:

Contagem de:

* Oak Log;
* Oak Planks;
* Cobblestone.

---

# Fase 6 — Sistema de Recursos

---

## TASK-018 — Criar Resource Registry

Implementar:

```text
ResourceRegistry
```

Responsável por:

* quantidade;
* localização.

---

## TASK-019 — Criar Verificação de Déficit

Exemplo:

```text
Precisa:

64 Oak Planks


Possui:

20


Déficit:

44
```

---

## TASK-020 — Integrar Recursos com Simulação

A Colônia deve saber:

* o que possui;
* o que falta.

---

# Fase 7 — Sistema de Tarefas

---

## TASK-021 — Criar Task Model

Criar:

```text
Task
```

Estados:

* AVAILABLE;
* RESERVED;
* EXECUTING;
* COMPLETED.

---

## TASK-022 — Criar Task Manager

Responsável por:

* criar;
* buscar;
* finalizar tarefas.

---

## TASK-023 — Associar Tarefas a Profissões

Exemplo:

```text
Build House

↓

Builder
```

---

# Fase 8 — Primeiro Trabalhador Funcional

---

## TASK-024 — Implementar Lumberjack

Capacidade:

```text
COLLECT_WOOD
```

---

## TASK-025 — Criar Coleta de Madeira

Fluxo:

```text
Receber tarefa

↓

Encontrar árvore

↓

Quebrar bloco permitido

↓

Coletar item
```

---

## TASK-026 — Depositar em Pacotes de 32

Regra:

```text
32 Oak Logs

↓

Retornar casa

↓

Depositar baú
```

---

# Fase 9 — Fabricação

---

## TASK-027 — Implementar Manufacturer

Capacidade:

```text
CRAFT_ITEMS
```

---

## TASK-028 — Integrar Recipe Manager

Usar:

Receitas Vanilla.

---

## TASK-029 — Produzir Oak Planks

Fluxo:

```text
Oak Log

↓

Oak Planks

↓

Baú
```

---

# Fase 10 — Construção

---

## TASK-030 — Criar Blueprint

Representar:

* blocos;
* posições;
* materiais.

---

## TASK-031 — Ler Estrutura Vanilla

MVP:

```text
Plains Small House
```

---

## TASK-032 — Calcular Materiais

Gerar:

Lista necessária.

---

## TASK-033 — Criar Build Task

Enviar para Builder.

---

## TASK-034 — Implementar Builder

Capacidade:

```text
BUILD_STRUCTURE
```

---

## TASK-035 — Colocar Blocos

Fluxo:

```text
Selecionar bloco

↓

Verificar material

↓

Colocar

↓

Registrar
```

---

# Fase 11 — Registro de Infraestrutura

---

## TASK-036 — Criar Building Registry

Salvar:

* posição;
* tipo;
* colônia.

---

## TASK-037 — Marcar Blocos da Colônia

Todo bloco colocado recebe origem:

```text
Colony Infrastructure
```

---

# Fase 12 — Testes do MVP

---

## TASK-038 — Teste Vila Inicial

Validar:

* vila encontrada;
* colônia criada.

---

## TASK-039 — Teste Trabalhadores

Validar:

* aldeões registrados;
* profissões atribuídas.

---

## TASK-040 — Teste Recursos

Validar:

* coleta;
* armazenamento;
* leitura.

---

## TASK-041 — Teste Construção

Validar:

```text
Recursos

↓

Projeto

↓

Builder

↓

Casa nova
```

---

## TASK-042 — Teste Persistência

Validar:

* salvar;
* fechar;
* abrir;
* continuar.

---

# Critério Final do MVP

O MVP está concluído quando:

```text
Uma vila Vanilla existe.

↓

A Colônia é criada.

↓

Aldeões recebem funções.

↓

Recursos são coletados.

↓

Recursos são armazenados.

↓

Materiais são produzidos.

↓

Uma nova casa é construída.

↓

A nova estrutura pertence à vila.
```

---

# Próximas Expansões Após MVP

Não fazem parte desta versão:

* mineração;
* ferreiro;
* pedreiro;
* logística;
* transporte;
* defesa;
* distritos;
* múltiplas vilas;
* economia.
