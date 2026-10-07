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

# 10. Como rodar e escrever testes (2026-10-06, ADR-035 §7)

Esta seção é a prática do dia a dia; os números que a justificam estão em
`docs/audit/TEST-STRATEGY.md` §7.

## Rodar

| Situação | Comando | Tempo medido |
|---|---|---|
| Uma classe de unitário | `gradlew test --tests "*Classe*"` | ~30 s (quase tudo é `compileJava`) |
| Uma família de GameTest | `gradlew runGametest -PgametestOnly=Miner,Lumber` | ~22 s com uma classe |
| Bateria comum, antes do commit | `gradlew build runGametest` | ~46 s de GameTest |
| Mexeu em estrutura, catálogo ou suprimento | `gradlew runGametest -PgametestAudit=only` | ~88 s |
| Tudo junto | `gradlew runGametest -PgametestAudit=include` | ~2 min |
| Depurar um cenário olhando o mundo | `gradlew runGametestServer`, entrar por `localhost`, `/test run <classe>.<método>` | — |
| Fechar P0 ou release | bateria ×2 `--rerun-tasks` + auditoria + `pitest` | — |

`-PgametestOnly` casa por trecho do nome simples da classe, sem diferença de
maiúsculas. **Limite conhecido:** alguns testes de fabricação dependem do
contexto da bateria inteira — `-PgametestOnly=CraftingGameTest,BuilderGameTest`
derruba `theWorkAsksForItsUncataloguedPieceBeforeWaiting` e
`theCycleOpensTheCraftingTaskByItself` mesmo sem mudança nenhuma (medido em
2026-10-06 no commit `36891a42`). Para fechar um item, vale a bateria comum. O filtro só muda a cópia que vai ao servidor de teste; o
registro em `src/gametest/resources/fabric.mod.json` continua com tudo, e o
`GameTestRegistryTest` reprova classe com `@GameTest` fora dele.

O CI roda a bateria comum em todo push; a auditoria e o PIT em PR, na `main`
e no disparo manual.

## Escrever

- **Onde:** GameTest novo vai em `src/gametest/java/com/villagecolony/gametest/`.
  Só vai para o pacote da classe testada (`fabric.work`, `fabric.event`…)
  quando precisa de acesso de pacote — e diz isso no javadoc.
- **Registrar:** toda classe nova entra em `fabric-gametest` no
  `fabric.mod.json` do gametest; o `GameTestRegistryTest` cobra.
- **Montar:** `ColonyFixture.colonyAt(context, centro)` cria, registra e já é
  dono da colônia; `fixture.equippedWorker(...)` é o padrão para
  trabalhador. `emptyHandedWorker(...)` só quando o assunto não é tempo de
  quebra. Sempre `fixture.cleanUp()` no `finally`.
- **Terreno repetido:** `Arena.forceChunks` e `Arena.floor`. Helper novo só
  vai para a `Arena` quando já existir idêntico em mais de um teste.
- **Auditoria de dados** (lê catálogo, NBT, receitas inteiras; não depende de
  aldeão): entra em `gametestAudits` no `build.gradle`, fora da bateria comum.
- **Migração:** teste antigo passa para a fixture quando for tocado por outro
  motivo; nunca em massa.

---

# Final Rule

Uma funcionalidade só existe quando pode ser observada funcionando dentro do Minecraft.
