# Testing-Strategy.md

# Village Colony Testing Strategy

**Status:** Approved

---

# 1. Purpose

Este documento define como validar o desenvolvimento do Village Colony.

O objetivo não é apenas verificar se o código compila.

O objetivo é garantir que a colônia realmente funciona dentro do Minecraft.

---

# 2. Testing Levels

O projeto utiliza três níveis:

```text
Unit Tests

↓

Integration Tests

↓

Minecraft World Tests
```

---

# 3. Unit Tests

Objetivo:

Validar lógica independente do Minecraft.

---

Testar:

* modelos;
* serviços;
* regras.

---

Exemplos:

## Colony

Testar:

* criação;
* identificação;
* estados.

---

## ResourceService

Testar:

* cálculo;
* disponibilidade;
* consumo.

---

## TaskService

Testar:

* criação;
* atribuição;
* conclusão.

---

# 4. Integration Tests

Objetivo:

Verificar comunicação entre sistemas.

---

Exemplo:

```text
Worker

↓

Task

↓

Resource

↓

Storage
```

---

Validar:

* trabalhador recebe tarefa;
* recurso existe;
* armazenamento atualiza.

---

# 5. Minecraft World Tests

Obrigatório para funcionalidades visuais.

---

# Teste: Criação de Colônia

Procedimento:

1. Criar mundo novo.
2. Encontrar vila Vanilla.
3. Entrar na área.
4. Verificar criação da Colony.

Resultado esperado:

```text
Colony criada.
```

---

# Teste: Persistência

Procedimento:

1. Criar colônia.
2. Salvar mundo.
3. Fechar Minecraft.
4. Abrir novamente.

Resultado esperado:

```text
Colony permanece.
```

---

# Teste: Trabalhadores

Procedimento:

1. Encontrar aldeões.
2. Registrar trabalhadores.
3. Reiniciar mundo.

Resultado:

```text
Workers continuam associados.
```

---

# Teste: Recursos

Procedimento:

1. Trabalhador coleta recurso.
2. Retorna para casa.
3. Deposita no baú.

Resultado:

```text
Item aparece fisicamente.
```

---

# Teste: Construção

Procedimento:

1. Criar projeto.
2. Disponibilizar materiais.
3. Executar construção.

Resultado:

```text
Novo bloco colocado.

Registro criado.
```

---

# 6. Regression Testing

Toda atualização deve verificar:

* versões anteriores continuam funcionando;
* saves antigos carregam;
* aldeões continuam válidos.

---

# 7. Debug Testing

Durante desenvolvimento usar:

Logs:

```text
[COLONY]

[WORKER]

[RESOURCE]

[BUILD]
```

---

# 8. Release Checklist

Antes de qualquer versão:

## Código

* compila;
* sem erros;
* sem warnings críticos.

---

## Minecraft

* inicia;
* cria mundo;
* salva;
* carrega.

---

## Sistemas

* colônia funciona;
* trabalhadores funcionam;
* recursos funcionam;
* construção funciona.

---

# 9. MVP Acceptance Test

O MVP está aprovado quando:

```text
Criar mundo

↓

Encontrar vila

↓

A vila se registra

↓

Aldeões trabalham

↓

Recursos acumulam

↓

Materiais são produzidos

↓

Nova construção acontece

↓

Mundo salva e continua
```

---

# Final Rule

Uma funcionalidade só existe quando pode ser observada funcionando dentro do Minecraft.
