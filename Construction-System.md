# Construction-System.md

# Village Colony — Construction System

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir como a colônia planeja, prepara e executa novas construções.

O sistema de construção permite que a vila:

* identifique necessidade de expansão;
* selecione estruturas Vanilla;
* calcule materiais;
* aguarde recursos;
* execute construção;
* registre nova infraestrutura.

---

# Princípio Fundamental

Construções são decisões da Colônia.

O aldeão Construtor apenas executa.

O Construtor nunca:

* escolhe o que construir;
* escolhe onde construir;
* coleta materiais;
* fabrica materiais;
* destrói infraestrutura existente.

---

# Modelo de Construção

Uma construção é representada por um projeto.

```text
Construction Project

- id
- structureType
- colony
- position
- rotation
- requiredMaterials
- status
- owner
```

---

# Estados da Construção

Uma construção possui os seguintes estados:

```text
PLANNED

↓

PREPARING

↓

WAITING_RESOURCES

↓

BUILDING

↓

COMPLETED
```

---

# PLANNED

A Colônia decidiu construir.

Neste estado:

* estrutura selecionada;
* posição definida;
* materiais calculados.

Nenhum bloco foi alterado.

---

# PREPARING

O local está sendo preparado.

O Construtor pode remover apenas:

* grama;
* flores;
* folhas;
* neve;
* blocos naturais necessários.

O Construtor nunca remove:

* estruturas Vanilla;
* estradas existentes;
* infraestrutura da Colônia.

---

# WAITING_RESOURCES

A construção aguarda materiais.

Exemplo:

```text
Projeto:

Plains Small House


Necessário:

Oak Planks: 64

Disponível:

Oak Planks: 20
```

Resultado:

```text
Waiting Resources
```

A Colônia cria demandas para obter os materiais faltantes.

---

# BUILDING

O Construtor está executando.

Fluxo:

```text
Selecionar próximo bloco

↓

Verificar material

↓

Posicionar bloco

↓

Registrar progresso

↓

Continuar
```

---

# COMPLETED

Quando todos os blocos forem colocados:

A construção:

* torna-se infraestrutura da Colônia;
* recebe proteção;
* é registrada permanentemente.

---

# Fonte das Construções

Todas as construções do MVP vêm das estruturas Vanilla.

Exemplo:

```text
minecraft:village/plains/houses/
```

---

# Blueprint

Antes da construção, uma estrutura Vanilla é convertida em um Blueprint.

O Blueprint contém:

```text
Blueprint

- blocks
- relativePositions
- materials
- size
```

Exemplo:

```text
Small House

Blocks:

Oak Planks
Cobblestone
Oak Door
Glass Pane
```

---

# Seleção da Estrutura

No MVP:

Apenas:

```text
Plains Small House
```

será suportada.

---

# Expansão Orgânica

Toda construção nova deve seguir a regra:

```text
Estrada existente

↓

Extensão da estrada

↓

Área disponível

↓

Nova construção
```

---

# Seleção do Local

Antes de criar um projeto, a Colônia verifica:

## Espaço

Existe área suficiente?

---

## Terreno

O terreno permite construção?

---

## Proteção

Existe estrutura protegida?

---

## Conexão

A construção estará ligada a uma estrada?

---

# Estradas

As estradas fazem parte da expansão.

Ordem obrigatória:

```text
Construir estrada

↓

Construir casa
```

Nunca:

```text
Casa isolada

↓

Criar estrada depois
```

---

# Materiais

Os materiais são obtidos automaticamente.

Fluxo:

```text
Structure

↓

Analyze Blocks

↓

Material List

↓

Resource System
```

---

# Exemplo

Estrutura:

```text
Plains Small House
```

Materiais:

```text
Oak Log
Oak Planks
Cobblestone
Glass Pane
Oak Door
```

A Colônia verifica:

```text
Possui tudo?

Sim

↓

Construção inicia

Não

↓

Criar demandas
```

---

# Execução pelo Builder

O Builder recebe:

```text
Build Task

- projectId
- location
- blueprint
```

Ele executa:

```text
Mover

↓

Selecionar bloco

↓

Colocar bloco

↓

Atualizar progresso
```

---

# Registro de Infraestrutura

Após concluída:

```text
Building Registry
```

recebe:

```text
Building

- id
- type
- position
- ownerColony
- completed=true
```

---

# Proteção da Construção

Uma construção concluída:

Não pode ser destruída por:

* aldeões;
* tarefas;
* expansão futura.

Ela passa a ser parte da vila.

---

# Falhas de Construção

Uma construção pode ser interrompida por:

* falta de recursos;
* morte do construtor;
* terreno inválido;
* carregamento do mundo.

O projeto deve continuar posteriormente.

---

# Cancelamento

Uma construção só pode ser cancelada se:

* a Colônia não precisar mais dela;
* o local se tornar inválido.

O cancelamento deve preservar o mundo.

---

# Regras de Arquitetura

## Construção nunca cria recursos.

---

## Construção nunca decide expansão.

---

## Construção nunca substitui estruturas Vanilla.

---

## Cada bloco colocado deve possuir uma origem.

Origem possível:

```text
Natural

Original Village

Colony Infrastructure
```

---

# Fora do MVP

Não implementar:

* casas personalizadas;
* reformas;
* demolições;
* reconstrução automática;
* estilos arquitetônicos;
* construções gigantes;
* múltiplos biomas.

---

# Objetivo Final

Criar um sistema onde uma vila Vanilla possa crescer naturalmente:

```text
Necessidade

↓

Projeto

↓

Recursos

↓

Construtor

↓

Nova Infraestrutura

↓

Nova Vila
```

A construção deve parecer uma evolução natural da geração Vanilla do Minecraft.
