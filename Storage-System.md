# Storage-System.md

# Village Colony — Storage System

**Version:** 1.0.0

**Status:** Approved

---

# Objetivo

Definir como os recursos produzidos pelos aldeões são armazenados dentro da colônia.

O sistema representa a ideia de que cada trabalhador possui seu próprio espaço de armazenamento dentro da vila.

---

# Princípio Fundamental

Cada trabalhador possui um armazenamento próprio.

O recurso pertence à colônia, mas permanece fisicamente guardado no baú do trabalhador que o produziu.

```text
Aldeão

↓

Casa

↓

Baú pessoal

↓

Recursos da Colônia
```

---

# Armazenamento do Trabalhador

Cada trabalhador possui:

* uma casa;
* uma cama;
* um baú de armazenamento.

Local padrão:

```text
Casa do trabalhador

↓

Próximo à cama

↓

Baú pessoal
```

---

# Associação Trabalhador → Baú

Um trabalhador possui uma referência:

```text
Worker Storage

- workerId
- chestPosition
- ownerProfession
```

---

# Criação do Baú

**Mudou em 2026-08-15, pela Regra 8.** O que estava escrito aqui — "o
sistema não cria casas ou baús automaticamente" — deixou de valer para o
baú. Continua valendo para a casa.

A regra vigente:

```text
toda vila gerada pelo Minecraft ganha um baú ao lado de cada cama

cada aldeão fica vinculado a uma cama

e ao baú mais perto da sua cama
```

Ordem de preferência, e ela importa:

```text
1. o baú que já existe ao alcance da cama    ChestScanner
2. um baú novo, ao lado da cama              ChestPlacer
```

Nunca o inverso. Vila que já tem baú não ganha baú novo.

## De onde vem o baú

Do nada, e **isto é exceção declarada**. A regra de arquitetura do
`Construction-System.md` — a colônia não cria recurso — continua valendo
para todo o resto: o que a obra consome sai de baú, e nada do que o
trabalhador produz nasce do vazio.

A justificativa da exceção é que este baú não é produção da colônia. É
completar o que a geração de vila do Minecraft deixou incompleto — do
mesmo lado da linha que a detecção de vila, e não do lado da economia.

## O cuidado ao pôr

É a escrita mais invasiva que o mod faz: um bloco dentro da casa de
alguém. A escolha do lugar recusa mais do que aceita.

```text
substituível        ar, grama alta, flor. Bloco do jogador fica
                    onde está — Regra 3

nunca cama          os vizinhos de uma cama incluem a outra metade
                    dela

chão firme embaixo  senão o baú flutua, ou cai sobre areia

livre em cima       baú com bloco opaco em cima não abre, nem para
                    o aldeão nem para o jogador

não encostar        baú ao lado de baú vira baú duplo, e um
noutro baú          inventário com dois donos
```

Quatro vizinhos no nível da cama; se nenhum servir, os quatro um bloco
abaixo — chão de vila vanilla tem degrau, e é a mesma folga de um bloco
que o `ChestScanner` já aceita entre cama e baú. Se ainda assim nenhum
servir, o aldeão fica sem baú e a linha diz isso.

## O que isto ainda não cobre

O baú nasce quando um **trabalhador** precisa dele e a cama dele não
alcança nenhum. Cama de aldeão que não trabalha continua sem baú.

O enunciado da regra é mais largo — *cada* cama — e cobri-lo exige as
posições das camas por colônia, que a varredura hoje descarta:
`VillageCandidate` carrega centro e contagem, não a lista. Ver o H2 do
Backlog.

---

# Registro de Armazenamento

Quando a Colônia identifica um trabalhador:

Fluxo:

```text
Encontrar aldeão

↓

Encontrar casa

↓

Encontrar cama

↓

Procurar baú próximo

↓

Registrar armazenamento
```

---

# Capacidade de Armazenamento

O baú utiliza o inventário Vanilla.

Não existe:

* capacidade aumentada;
* inventário virtual;
* armazenamento mágico.

---

# Produção e Entrega

Um trabalhador não deposita item individualmente.

Ele acumula recursos durante sua atividade.

Quando atingir o limite:

```text
32 unidades
```

ele retorna para sua casa e deposita no baú.

---

# Exemplo

Lenhador:

```text
Recebe tarefa:

Coletar Madeira


↓

Coleta:

32 Oak Logs


↓

Retorna para casa


↓

Deposita no baú


↓

Atualiza estoque da Colônia
```

---

# Pacote de Recursos

O pacote mínimo de entrega é:

```text
32 itens
```

Esse valor representa uma carga de trabalho.

---

# Motivo do Pacote

Evita:

* aldeão voltando para casa a cada item;
* excesso de processamento;
* movimentação desnecessária.

Permite:

* rotina mais natural;
* melhor desempenho;
* comportamento previsível.

---

# Consulta da Colônia

A Colônia não possui os itens.

Ela possui apenas uma visão agregada.

Exemplo:

```text
Colony Resource Registry


Oak Log

Lenhador João:
32

Lenhador Pedro:
64


Total:

96 Oak Logs
```

---

# Consumo de Recursos

Quando uma construção precisa de materiais:

Fluxo:

```text
Construction System

↓

Resource Request

↓

Storage Registry

↓

Localizar baús

↓

Reservar itens

↓

Retirar recursos
```

---

# Reserva de Recursos

Antes de consumir um item:

Ele deve ser reservado.

Estados:

```text
AVAILABLE

↓

RESERVED

↓

CONSUMED
```

Isso evita:

* dois sistemas usando o mesmo item;
* construção iniciando sem material.

---

# Retirada de Recursos

No MVP:

A retirada pode ser feita pelo próprio sistema de construção.

O transporte físico ainda não existe.

Futuro:

```text
Trabalhador Transportador

↓

Busca recurso

↓

Entrega ao construtor
```

---

# Organização do Baú

No MVP:

Não existe organização automática.

O trabalhador apenas deposita.

Futuro:

* separação por categoria;
* armazenamento especializado;
* armazém central.

---

# Regras por Profissão

## Lumberjack

Produz:

```text
Oak Log
```

Entrega:

```text
Pacotes de 32
```

---

## Manufacturer

Produz:

```text
Oak Planks
```

Entrega:

```text
Pacotes de 32
```

---

## Farmer

Futuro:

Produção agrícola seguirá o mesmo modelo.

---

## Builder

Pode utilizar recursos disponíveis nos baús registrados.

---

# Atualização do Estoque

A Colônia atualiza:

* após depósito;
* após consumo;
* durante ciclos de sincronização.

---

# Proteção

O baú do trabalhador faz parte da infraestrutura da colônia.

Após registrado:

* não deve ser destruído por tarefas;
* não deve ser removido pela expansão;
* pertence ao trabalhador.

---

# Falhas

Caso o baú seja removido:

A Colônia registra:

```text
Storage Missing
```

O trabalhador fica sem depósito até encontrar outro.

---

# Fora do MVP

Não implementar:

* aldeão transportador;
* armazém central;
* sistema de logística;
* carrinhos automáticos;
* redes de armazenamento;
* comércio entre trabalhadores.

---

# Objetivo Final

Criar uma vila onde os recursos tenham uma origem física e visível.

O jogador deve conseguir observar:

```text
Aldeão trabalha

↓

Produz recurso

↓

Volta para casa

↓

Guarda no próprio baú

↓

A colônia utiliza esse recurso para crescer
```

O armazenamento deve parecer uma consequência natural da vida da vila, não um sistema invisível.
