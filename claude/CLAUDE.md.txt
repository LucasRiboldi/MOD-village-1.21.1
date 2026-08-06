# Village Colony — Claude Code Project Instructions

**Project:** Village Colony
**Minecraft Version:** 1.21.1
**Mod Loader:** Fabric
**Language:** Java

---

# 1. Project Identity

Este projeto é um mod Minecraft Fabric chamado **Village Colony**.

O objetivo é transformar uma vila Vanilla em uma colônia autônoma onde os aldeões:

* trabalham;
* armazenam recursos;
* produzem materiais;
* constroem novas estruturas;
* expandem a vila naturalmente.

O jogador não é o administrador da colônia.

O jogador não fornece ordens diretas.

A colônia deve funcionar sozinha.

---

# 2. Core Vision

A filosofia principal:

> "Criar uma vila Vanilla que aprendeu a crescer sozinha."

O mod não deve parecer um sistema separado do Minecraft.

Ele deve parecer uma evolução natural dos aldeões existentes.

---

# 3. Fonte da Verdade

Minecraft Vanilla é sempre a fonte da verdade.

O mundo real possui:

* blocos;
* entidades;
* inventários;
* estruturas;
* aldeões.

O mod apenas adiciona:

* memória;
* organização;
* planejamento;
* comportamento coletivo.

---

# 4. Documentação Obrigatória

Antes de implementar qualquer sistema, consultar:

```
PROJECT_CONSTITUTION.md

MVP.md

Architecture-Foundation.md

Simulation-Loop.md

Profession-System.md

Resource-System.md

Construction-System.md

Storage-System.md

Save-Data-System.md

Fabric-Implementation-Plan.md

Data-Model.md

Class-Architecture.md

Development-Roadmap.md
```

Esses documentos possuem prioridade sobre decisões improvisadas.

---

# 5. Arquitetura Obrigatória

O projeto segue três camadas.

```
Minecraft/Fabric

↓

Adapter Layer

↓

Service Layer

↓

Core Model Layer
```

---

# 6. Regras de Dependência

Permitido:

```
Fabric

↓

Services

↓

Models
```

Proibido:

```
Model

↓

Minecraft

```

Modelos nunca devem conhecer:

* VillagerEntity;
* ServerWorld;
* BlockEntity;
* Fabric API.

---

# 7. Responsabilidade das Camadas

## Model

Representa dados.

Exemplo:

```
Worker
Colony
Task
Resource
Building
Storage
```

Não executa ações.

---

## Services

Executam regras.

Exemplo:

```
ColonyService

TaskService

ResourceService

ConstructionService
```

---

## Fabric Adapter

Conecta o mod ao Minecraft.

Exemplo:

```
VillagerAdapter

ChestAdapter

BlockPlacementAdapter
```

---

# 8. Restrições Permanentes

Nunca criar:

* inventário global da vila;
* necessidades novas para aldeões;
* moeda interna;
* economia artificial;
* servidor externo;
* banco de dados externo;
* dependências pagas;
* sistemas que exigem manutenção online.

---

# 9. Filosofia de Recursos

Recursos devem existir fisicamente.

Correto:

```
Aldeão

↓

Baú

↓

Recurso

↓

Construção
```

Errado:

```
Colônia

↓

+500 madeira virtual
```

---

# 10. Filosofia de Construção

Aldeões:

* não destroem estruturas originais da vila;
* utilizam materiais existentes;
* registram blocos criados pela colônia.

Todo bloco colocado pela colônia deve possuir registro interno.

---

# 11. Desenvolvimento Incremental

Nunca implementar várias fases simultaneamente.

Sempre:

1. Ler documentação.
2. Escolher uma tarefa.
3. Implementar.
4. Testar.
5. Atualizar documentação se necessário.

---

# 12. Antes de Código

Antes de criar arquivos:

Informar:

* qual tarefa está sendo implementada;
* quais arquivos serão alterados;
* impacto esperado.

---

# 13. Qualidade Esperada

O código deve ser:

* modular;
* escalável;
* simples;
* legível;
* sustentável.

O objetivo não é apenas funcionar.

O objetivo é permitir crescimento por anos.
