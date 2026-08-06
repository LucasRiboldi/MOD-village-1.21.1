# Performance-Rules.md

# Village Colony Performance Guidelines

**Status:** Approved

---

# 1. Purpose

Minecraft possui processamento limitado por tick.

Este documento define regras obrigatórias para manter desempenho adequado.

---

# 2. Performance Objective

O mod deve funcionar:

* em mundos pequenos;
* em mundos grandes;
* com múltiplas vilas.

---

Objetivo:

```text
Adicionar vida

sem destruir TPS
```

---

# 3. Tick Management

## Forbidden

Nunca executar:

```text
scan completo do mundo

a cada tick
```

---

Exemplo proibido:

```java
for(World world)

for(AllBlocks)

check()
```

---

# 4. Scheduled Updates

Preferir:

```text
20 ticks

100 ticks

600 ticks
```

---

Exemplo:

## Curto prazo

Movimentos:

20 ticks.

---

## Médio prazo

Recursos:

100 ticks.

---

## Longo prazo

Planejamento:

600 ticks.

---

# 5. Entity Searching

## Forbidden

Buscar todos os aldeões do mundo constantemente.

---

## Correct

Manter registro:

```text
Colony

↓

Workers

↓

Villagers
```

---

# 6. World Scanning

Evitar:

* procurar blocos infinitamente;
* analisar chunks sem necessidade.

---

Preferir:

* eventos;
* cache;
* áreas limitadas.

---

# 7. Data Storage

Salvar apenas:

Necessário:

* UUID;
* estados;
* referências.

---

Não salvar:

* mundo inteiro;
* blocos duplicados;
* inventários completos.

---

# 8. Collections

Preferir:

* listas pequenas;
* mapas indexados;
* estruturas adequadas.

---

Evitar:

* listas globais gigantes;
* duplicação de dados.

---

# 9. Memory Management

Evitar manter:

* entidades mortas;
* referências inválidas;
* objetos temporários.

---

Sempre remover:

* workers inexistentes;
* colônias removidas.

---

# 10. Construction System

Construção deve possuir:

Limites:

* quantidade de blocos por ciclo;
* velocidade controlada.

---

Nunca:

```text
colocar 5000 blocos instantaneamente
```

---

# 11. Logging

Logs devem ser controlados.

Permitido:

```text
Colony created

Worker assigned

Construction completed
```

---

Evitar:

```text
Worker searching...

Worker searching...

Worker searching...
```

a cada tick.

---

# 12. Scaling Goal

O sistema deve evoluir:

```text
1 vila

↓

10 vilas

↓

100 vilas
```

sem arquitetura diferente.

---

# Final Rule

A simulação deve parecer viva, mas o código deve permanecer econômico.
