# Development-Roadmap.md

# Village Colony — Development Roadmap

Version 1.0.0

Status Approved

---

# 1. Visão Geral

O Village Colony é um mod para Minecraft 1.21.1 utilizando Fabric que transforma uma vila Vanilla em uma colônia autônoma.

O objetivo do desenvolvimento é criar uma evolução natural do comportamento dos aldeões

```text
Vila Vanilla

↓

Colônia organizada

↓

Produção de recursos

↓

Construção

↓

Expansão orgânica
```

---

# 2. Filosofia de Desenvolvimento

O projeto seguirá uma evolução incremental.

Cada versão deve

 adicionar uma capacidade completa;
 manter compatibilidade com versões anteriores;
 possuir um objetivo jogável;
 evitar sistemas incompletos.

---

# 3. Princípios de Desenvolvimento

## 3.1 Não substituir Minecraft Vanilla

O mod deve complementar

 aldeões;
 profissões;
 estruturas;
 receitas;
 mundo.

Não criar uma simulação paralela.

---

## 3.2 Sistemas independentes

Cada sistema deve poder evoluir separadamente.

Exemplo

```text
Resource System

não depende de

Construction System
```

---

## 3.3 Primeiro estabilidade, depois complexidade

A prioridade é

1. funcionamento;
2. persistência;
3. desempenho;
4. expansão.

---

# 4. Roadmap Geral

## Onde o projeto está — 2026-08-28

Este arquivo é o **plano**; o estado de verdade vive no
[`README.md`](README.md) e a lista canônica de pendências no
[`TODO.md`](TODO.md). O quadro abaixo é só a régua deste plano.

```text
v0.1 Fundação        ✅ feito
v0.2 Colônia         ✅ feito, verificado em jogo
v0.3 Trabalhadores   ✅ feito, verificado em jogo — as SETE profissões
                        buscam recurso e guardam no próprio baú
v0.4 Recursos        ✅ feito — madeira, pedra, lã, areia, minério, lavoura
                        ⚠️ o elo da areia nunca começou em jogo
v0.5 Produção        ✅ feito, verificado em jogo — tábua, tocha, vidraça,
                        tronco descascado, fundição
v0.6 Construção      ✅ verificado em jogo em 2026-08-19
                        ⚠️ uma casa por bioma enquanto a Regra 28 valer
v0.7 Expansão        🔨 em andamento — a rua cresce, o lote é achado por
                        índice, e a vila levanta a casa seguinte
v1.0 MVP Completo    🔒 falta a sessão em que a vila roda sozinha do
                        começo ao fim sem o jogador

── depois do MVP ─────────────────────────────────────────────

v1.1 Vila            🔨 casa sem barreira de teste, e o Pedreiro
v1.2 Vila desenvolvida  ⬜ Pecuarista, Armazenista, estoque central
v1.3 Cidade          ⬜ as 13 profissões vanilla como agentes
v2.0 Cidade autônoma ⬜ árvore tecnológica pelo Bibliotecário
```

**O arco depois do v1.0 tem documento próprio:**
[`docs/technical/Village-Economy.md`](docs/technical/Village-Economy.md)
— as famílias de material por bioma, as profissões que faltam, a cadeia
produtiva, a tabela recurso→profissão e as cinco fases de crescimento,
de acampamento a cidade autônoma.

**A ressalva que este projeto não omite:** *coberto por teste* e *visto
funcionando em jogo* são coisas diferentes, e as duas aparecem separadas
em toda lista. O maior item aberto hoje é o **mineiro, que não cavou um
bloco em sete sessões** — ver o README.

---

```text
v0.1 Fundação

↓

v0.2 Colônia

↓

v0.3 Trabalhadores

↓

v0.4 Recursos

↓

v0.5 Produção

↓

v0.6 Construção

↓

v0.7 Expansão

↓

v1.0 MVP Completo
```

---

# Versão 0.1 — Fundação Técnica

## Objetivo

Criar a base Fabric do projeto.

Nesta fase a colônia ainda não possui comportamento.

---

# Implementações

## Projeto Fabric

Criar

 Minecraft 1.21.1;
 Fabric Loader;
 Fabric API;
 Gradle.

---

## Estrutura de Código

Criar

```text
core

fabric

data
```

---

## Inicialização

Implementar

 mod entrypoint;
 registro de eventos;
 configuração inicial.

---

## Persistência Básica

Criar

```text
ColonySavedData
```

---

# Resultado Esperado

Ao iniciar o Minecraft

```text
Fabric carrega

↓

Village Colony inicia

↓

Dados podem ser salvos
```

---

# Fora desta versão

Não implementar

 aldeões;
 tarefas;
 recursos;
 construção.

---

# Versão 0.2 — Sistema de Colônia

## Objetivo

Criar a entidade lógica da vila.

---

# Implementações

## Detecção de Vila

Adicionar

```text
VillageScanner
```

Responsável por encontrar

 Plains Village.

---

## Criação da Colônia

Criar

```text
Colony
```

Dados

 localização;
 estado;
 identificação.

---

## Registro Persistente

Salvar

 ID;
 posição;
 tipo.

---

# Resultado Esperado

O jogador entra em uma vila

```text
Minecraft Village

↓

Colony criada

↓

Estado salvo
```

---

# Fora desta versão

Não implementar

 trabalhadores;
 recursos;
 expansão.

---

# Versão 0.3 — Sistema de Trabalhadores

## Objetivo

Transformar aldeões Vanilla em membros da colônia.

---

# Implementações

## Worker Model

Criar

```text
Worker
```

---

## Scanner de Aldeões

Implementar

```text
VillagerScanner
```

---

## Profissões

Adicionar

 Lumberjack;
 Manufacturer;
 Farmer;
 Builder.

---

## Associação

Cada aldeão recebe

 colônia;
 profissão;
 estado.

---

# Resultado Esperado

A vila mostra

```text
10 aldeões

↓

10 trabalhadores registrados
```

---

# Fora desta versão

Não implementar

 execução de trabalho;
 coleta;
 construção.

---

# Versão 0.4 — Sistema de Recursos

## Objetivo

Criar consciência material da colônia.

---

# Implementações

## Storage System

Adicionar

 registro de baús;
 associação trabalhador → baú.

---

## Resource Registry

Implementar

 leitura de inventários;
 contagem de recursos.

---

## Recursos MVP

Suportar

```text
Oak Log

Oak Planks

Cobblestone
```

---

# Resultado Esperado

A colônia sabe

```text
Tenho

64 Oak Logs

32 Oak Planks
```

---

# Fora desta versão

Não implementar

 coleta automática;
 fabricação;
 construção.

---

# Versão 0.5 — Sistema de Tarefas e Produção

## Objetivo

Criar o primeiro comportamento autônomo.

---

# Implementações

## Task System

Criar

 Task;
 TaskManager;
 estados.

---

## Lumberjack

Adicionar

 coleta de madeira;
 ferramenta inicial;
 retorno para casa.

---

## Pacote de Entrega

Implementar

```text
32 itens

↓

Depositar no baú
```

---

## Manufacturer

Adicionar

 receitas Vanilla;
 produção de tábuas.

---

# Resultado Esperado

Fluxo

```text
Lenhador

↓

Oak Log

↓

Fabricante

↓

Oak Planks

↓

Baú
```

---

# Fora desta versão

Não implementar

 construção.

---

# Versão 0.6 — Sistema de Construção

## Objetivo

Criar expansão física da vila.

---

# Implementações

## Blueprint System

Adicionar

 leitura de estruturas Vanilla;
 lista de blocos.

---

## Construction Project

Criar

 planejamento;
 materiais;
 progresso.

---

## Builder

Adicionar

 execução;
 colocação de blocos.

---

# Primeira Construção

Suportar

```text
Plains Small House
```

---

# Resultado Esperado

A vila consegue criar

```text
Nova necessidade

↓

Recursos

↓

Construtor

↓

Nova casa
```

---

# Versão 0.7 — Expansão Orgânica

## Objetivo

Fazer a vila crescer naturalmente.

---

# Implementações

## Sistema de Localização

Adicionar

 análise de terreno;
 conexão com estradas.

---

## Registro de Infraestrutura

Adicionar

 origem do bloco;
 proteção.

---

## Expansão pela Estrada

Regra

```text
Estrada existente

↓

Nova estrada

↓

Nova casa
```

---

# Resultado Esperado

A vila cresce sem destruir sua origem.

---

# Versão 1.0 — MVP Completo

## Objetivo

Entregar a primeira versão jogável.

---

# Funcionalidades Obrigatórias

## Colônia

 detecta vila;
 salva estado;
 acompanha população.

---

## Trabalhadores

 possuem profissões;
 executam tarefas.

---

## Recursos

 são coletados;
 armazenados;
 utilizados.

---

## Produção

 usa receitas Vanilla.

---

## Construção

 cria novas estruturas;
 registra infraestrutura.

---

# Critério de Sucesso

Uma vila Vanilla deve conseguir

```text
Existir

↓

Organizar aldeões

↓

Produzir recursos

↓

Criar materiais

↓

Construir nova casa

↓

Continuar crescendo
```

---

# Pós-MVP

Após v1.0 serão avaliados

## Novas Profissões

 Minerador;
 Pedreiro;
 Ferreiro;
 Pescador.

---

## Logística

 transportadores;
 carroças;
 armazenamento central.

---

## Sociedade

 especialização;
 hierarquia;
 evolução da vila.

---

## Mundo

 múltiplas colônias;
 relações entre vilas;
 comércio.

---

# Controle de Escopo

Novos sistemas só devem ser adicionados quando

1. O núcleo atual estiver estável.
2. O sistema anterior possuir uso real.
3. Não comprometer desempenho.
4. Não quebrar a filosofia Vanilla.

---

# Visão Final

O Village Colony não deve parecer um jogo separado dentro do Minecraft.

Ele deve parecer

```text
Uma vila Vanilla que finalmente aprendeu a crescer sozinha.
```
