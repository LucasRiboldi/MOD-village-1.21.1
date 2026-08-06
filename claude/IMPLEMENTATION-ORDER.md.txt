# Implementation Order

# Village Colony Implementation Sequence

---

# Regra Principal

A implementação deve seguir exatamente esta ordem.

Não avançar fases incompletas.

---

# Phase 1 — Project Foundation

## Objetivo

Criar ambiente Fabric funcional.

Implementar:

* Gradle;
* Fabric Loader;
* Fabric API;
* Entry Point.

Resultado:

```
Minecraft inicia com o mod carregado.
```

---

# Phase 2 — Core Models

Criar modelos:

```
Colony

Worker

Task

Resource

Storage

Building
```

Sem integração Minecraft.

Resultado:

```
Core existe independente.
```

---

# Phase 3 — Persistence

Implementar:

```
ColonySavedData
```

Salvar:

* colônias;
* trabalhadores;
* construções;
* tarefas.

Resultado:

```
Dados sobrevivem ao reiniciar mundo.
```

---

# Phase 4 — Colony Detection

Implementar:

```
VillageScanner
```

Detectar:

* vilas Vanilla;
* bioma Plains.

Resultado:

```
Uma vila cria uma Colony.
```

---

# Phase 5 — Worker Registration

Implementar:

```
VillagerAdapter

WorkerService
```

Registrar:

* UUID;
* profissão;
* armazenamento.

Resultado:

```
A colônia conhece seus aldeões.
```

---

# Phase 6 — Storage System

Implementar:

```
StorageService
```

Detectar:

* baús;
* proprietários;
* recursos.

Resultado:

```
A colônia conhece seus estoques.
```

---

# Phase 7 — Resource System

Implementar:

```
ResourceService
```

Controlar:

* madeira;
* pedra;
* materiais.

Resultado:

```
A colônia entende seus recursos.
```

---

# Phase 8 — Task System

Implementar:

```
TaskManager
```

Criar:

* tarefas;
* estados;
* prioridades.

Resultado:

```
A colônia gera trabalho.
```

---

# Phase 9 — First Worker

Implementar:

```
Lumberjack
```

Fluxo:

```
Recebe tarefa

↓

Coleta madeira

↓

Volta casa

↓

Deposita
```

---

# Phase 10 — Manufacturing

Implementar:

```
Manufacturer
```

Usar:

* receitas Vanilla.

---

# Phase 11 — Construction

Implementar:

```
Blueprint

ConstructionProject

Builder
```

Primeira construção:

```
Plains Small House
```

---

# Phase 12 — Expansion

Implementar:

* estradas;
* expansão;
* registro de infraestrutura.

---

# Release MVP

Somente liberar quando:

```
Vila Vanilla

↓

Trabalhadores

↓

Recursos

↓

Produção

↓

Construção

↓

Persistência
```

funcionarem juntos.
