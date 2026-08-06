# Claude Code — Project Initialization Prompt

Você está assumindo o desenvolvimento do projeto **Village Colony**.

Antes de escrever qualquer código, sua primeira responsabilidade é compreender completamente a visão, arquitetura e regras do projeto.

Este projeto possui uma documentação completa que deve ser considerada a fonte oficial de decisão.

---

# 1. Contexto do Projeto

O objetivo é criar um mod para:

```
Minecraft Java Edition 1.21.1
Fabric
Java
```

Nome do projeto:

```
Village Colony
```

A ideia central:

Criar uma reimaginação das vilas Vanilla onde os aldeões formam uma colônia autônoma capaz de:

* trabalhar;
* coletar recursos;
* armazenar recursos;
* produzir materiais;
* construir novas estruturas;
* expandir naturalmente.

O jogador não é necessário para a sobrevivência da colônia.

---

# 2. Regra Principal

Antes de qualquer implementação:

LEIA TODOS OS DOCUMENTOS DO PROJETO.

Não comece criando classes.

Não suponha arquitetura.

Não simplifique decisões existentes.

Primeiro compreenda.

---

# 3. Documentos Obrigatórios

Analise nesta ordem:

## Identidade

```
README.md
PROJECT_CONSTITUTION.md
MVP.md
```

---

## Arquitetura

```
Architecture-Foundation.md
Data-Model.md
Class-Architecture.md
Simulation-Loop.md
```

---

## Sistemas

```
Profession-System.md
Resource-System.md
Storage-System.md
Construction-System.md
Save-Data-System.md
```

---

## Desenvolvimento

```
Development-Roadmap.md
MVP-Tasks.md
```

---

## Regras do Agente

Leia obrigatoriamente:

```
claude/CLAUDE.md
claude/DEVELOPMENT-RULES.md
claude/IMPLEMENTATION-ORDER.md
claude/CODE-STANDARDS.md
```

---

## Documentação Técnica

Leia:

```
docs/technical/Fabric-Version.md
docs/technical/Performance-Rules.md
docs/technical/Testing-Strategy.md
docs/technical/Debugging-Strategy.md
docs/technical/Development-Workflow.md
docs/technical/Project-State.md
docs/technical/Initial-Setup-Checklist.md
```

---

# 4. Sua Primeira Tarefa

Após analisar os documentos, NÃO implemente nada ainda.

Produza uma análise técnica contendo:

---

## A. Entendimento do Projeto

Explique:

* qual problema o mod resolve;
* qual experiência pretende criar;
* qual é o papel dos aldeões;
* qual é o papel do jogador.

---

## B. Auditoria da Arquitetura

Avalie:

* se a arquitetura é coerente;
* se existe algum conflito entre documentos;
* se existem decisões que precisam ser refinadas.

---

## C. Auditoria Técnica Fabric

Analise:

* compatibilidade Minecraft 1.21.1;
* Fabric;
* Java;
* possíveis limitações da API.

---

## D. Auditoria de Escalabilidade

Avalie:

* múltiplas vilas;
* performance;
* persistência;
* tamanho futuro do projeto.

---

## E. Riscos Encontrados

Liste:

* riscos técnicos;
* riscos arquiteturais;
* riscos de performance;
* riscos de complexidade.

---

## F. Melhorias Recomendadas

Se encontrar problemas:

Não altere arquivos ainda.

Primeiro apresente:

```
Problema encontrado:

Impacto:

Solução recomendada:

Documento afetado:
```

---

# 5. Regras de Decisão

Você deve respeitar:

## Não substituir Vanilla

Os aldeões Vanilla permanecem como base.

---

## Não criar sistemas desnecessários

Não adicionar:

* novas necessidades;
* novas entidades NPC;
* economia virtual;
* inventário global;
* dependências externas.

---

## Mundo como fonte da verdade

Itens, blocos e construções devem existir fisicamente no Minecraft.

---

# 6. Após a Análise

Depois da auditoria, aguarde minha aprovação.

Não:

* criar código;
* criar arquivos Java;
* alterar arquitetura;
* iniciar Gradle.

A próxima etapa será definida após sua análise.

---

# 7. Critério de Sucesso Desta Etapa

Esta etapa será concluída quando você conseguir responder claramente:

"Como construir uma vila Vanilla autônoma, modular e escalável usando Fabric 1.21.1 sem quebrar a filosofia original do projeto?"

Somente depois disso iniciaremos a implementação.
