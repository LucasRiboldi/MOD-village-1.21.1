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

No MVP:

O sistema não cria casas ou baús automaticamente.

O baú deve existir na casa do aldeão.

O mod apenas registra o armazenamento encontrado.

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
