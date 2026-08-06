# MVP.md

# Village Colony — Minimum Viable Product

**Version:** 0.1.0

**Status:** Planned

---

# Objetivo

Criar a primeira versão funcional de uma colônia autônoma utilizando uma vila Vanilla de Minecraft 1.21.1.

O MVP deve provar que uma vila consegue:

* reconhecer sua existência;
* organizar seus recursos;
* atribuir funções aos aldeões;
* criar tarefas;
* executar produção básica;
* expandir sua infraestrutura.

---

# Princípio do MVP

O MVP não busca criar uma colônia completa.

Ele busca validar a arquitetura principal:

```
Vila Vanilla

↓

Colônia

↓

Recursos

↓

Demandas

↓

Tarefas

↓

Aldeões

↓

Nova Construção
```

---

# Ambiente Suportado

## Minecraft

Versão:

```
1.21.1
```

## Loader

```
Fabric
```

## Bioma inicial

Apenas:

```
Plains Village
```

Outros biomas serão adicionados futuramente.

---

# Funcionalidades Incluídas

---

# 1. Registro da Colônia

O mod deve detectar uma vila Vanilla existente e criar uma entidade lógica:

```
Colony
```

A colônia deve armazenar:

* posição da vila;
* aldeões pertencentes;
* construções conhecidas;
* recursos monitorados;
* profissões existentes.

---

# 2. Proteção da Vila Original

Toda estrutura gerada pelo Minecraft deve ser registrada como:

```
Original Village Infrastructure
```

Esses blocos nunca podem ser removidos pelos aldeões.

Inclui:

* casas;
* caminhos;
* camas;
* estações de trabalho;
* estruturas decorativas.

---

# 3. Sistema de Recursos

O MVP monitora apenas:

## Recursos Naturais

```
Oak Log
Cobblestone
```

## Recursos Processados

```
Oak Planks
```

---

# 4. Sistema de Armazenamento

A colônia deve possuir um inventário lógico de recursos.

Os recursos coletados pertencem à colônia.

Aldeões possuem um bau de estoque permanente.

Fluxo:

```
Coletor

↓

Armazém

↓

Produção

↓

Armazém

↓

Construção
```

---

# 5. Profissões MVP

Apenas quatro profissões serão suportadas.

---

## Lenhador

Responsabilidade:

* coletar madeira.
* guardar um pack de madeira coletada em seu bau

Equipamento inicial:

* Machado de Madeira.

Produção:

```
Oak Log
```

---

## Fabricante

Responsabilidade:

* transformar recursos.

Receitas iniciais:

```
Oak Log

↓

Oak Planks
```

---

## Fazendeiro

Responsabilidade:

* manter produção de comida.

No MVP apenas será monitorado.

A lógica avançada de agricultura fica para versões futuras.

---

## Construtor

Responsabilidade:

* executar projetos de construção.
* identifica o que falta contruir
* buscar nos baus dos trabalhadores o recurso necessario

Ele:

* não coleta;
* não fabrica;
* não decide construções.

Ele apenas executa projetos aprovados pela colônia.

---

# 6. Sistema de Tarefas

Toda ação da colônia deve ser uma Task.

Estados:

```
Available

↓

Reserved

↓

Executing

↓

Completed

↓

Cancelled
```

---

# 7. Primeira Cadeia de Produção

O ciclo inicial será:

```
Lenhador

↓

Oak Log

↓

Fabricante

↓

Oak Planks

↓

Construtor

↓

Nova Casa
```

---

# 8. Primeira Construção

O MVP terá apenas:

```
Plains Small House
```

A construção será carregada das estruturas Vanilla.

O sistema deverá:

1. selecionar a estrutura;
2. calcular materiais;
3. verificar estoque;
4. aguardar recursos;
5. construir;
6. registrar como infraestrutura da colônia.

---

# 9. Expansão Orgânica

A primeira expansão seguirá:

```
Estrada existente

↓

Extensão da estrada

↓

Novo lote

↓

Nova casa
```

O construtor poderá remover apenas:

* grama;
* flores;
* folhas;
* neve;
* blocos naturais necessários para preparar o terreno.

Nunca poderá remover:

* vila original;
* infraestrutura da colônia.

---

# 10. Sucessão de Profissões

Quando uma profissão essencial ficar vazia:

Exemplo:

```
Construtor morreu
```

A colônia registra:

```
Missing Profession:
Builder
```

O próximo aldeão adulto disponível recebe essa profissão.

---

# Fora do MVP

Não será implementado:

* outros biomas;
* mineração;
* evolução de ferramentas;
* comércio inteligente;
* personalidade dos aldeões;
* felicidade;
* economia;
* distritos;
* interface gráfica;
* múltiplas vilas;
* guerra;
* defesa avançada;
* reformas de construções;
* demolição;
* construções personalizadas.

---

# Critérios de Sucesso

O MVP será considerado concluído quando:

* uma vila Vanilla for detectada;
* uma Colônia for criada;
* aldeões forem registrados;
* profissões forem controladas;
* recursos forem monitorados;
* um lenhador coletar madeira;
* um fabricante produzir tábuas;
* um construtor construir uma nova casa;
* a nova casa for registrada como infraestrutura permanente.

---

# Resultado Esperado

Após o MVP, o jogador deve observar:

"Uma vila Vanilla que começou a se organizar sozinha e lentamente expandiu sua comunidade."

Esse comportamento será a fundação para todas as futuras versões.
