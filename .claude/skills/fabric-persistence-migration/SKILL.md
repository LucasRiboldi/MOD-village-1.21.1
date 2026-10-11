---
name: fabric-persistence-migration
description: Persistência e migração de dados de um mod Fabric 1.21.1 — PersistentState, NBT, Codec, versionamento de esquema, renomeação de enum (BREEDER→SHEPHERD), save corrompido e fuzzing de NBT. Use ao auditar ou alterar o que o mod grava no mundo. Não use para estado que vive só em memória.
---

# fabric-persistence-migration

## Propósito

Garantir que um save antigo abre, que um save corrompido não derruba o
servidor e que o esquema gravado tem versão para migrar depois.

## Quando usar

- Dimensão **D5 (persistência e migração)** e parte da **D9 (robustez)**.
- Antes de renomear enum, campo ou chave NBT.
- Ao ver `NbtCompound.get*` sem checagem de tipo, ou `valueOf(String)` sobre
  texto lido do disco.

## Mapeamento para a auditoria

| Dimensão | O que este skill entrega |
|---|---|
| D5 Persistência | versão de esquema, migrações, teste com save antigo |
| D9 Robustez | leitura defensiva de NBT, fuzzing, `Identifier` inválido |
| D12 Compatibilidade | save criado em versão anterior do mod |

## Checklist

1. **Inventário.** Toda classe que estende `PersistentState` ou escreve
   `NbtCompound`: o que grava, com que chave, se grava versão.
2. **Versão de esquema.** Existe `DataVersion`/`schema` gravado? Há caminho
   de migração por versão, ou só leitura tolerante?
3. **Renomeações.** Para cada enum persistido por nome: leitura usa
   `valueOf` cru (lança `IllegalArgumentException`) ou mapa de aliases
   (`BREEDER` → `SHEPHERD`)? Há teste que lê o nome antigo?
4. **Nome inexistente.** Texto que não casa com nenhum valor (ex.
   `MANUFACTURER`): descarta com log, cai em default, ou derruba o carregamento?
5. **Identifier.** `Identifier.of` lança com texto inválido; `Identifier.tryParse`
   devolve null. Qual o código usa sobre dado lido do disco?
6. **Fuzzing leve.** Teste que alimenta o leitor com: compound vazio, tipo
   trocado (string onde era int), lista com elemento nulo, número fora da
   faixa, identificador malformado. Esperado: não lança; registra e segue.
7. **Ordem de escrita.** `markDirty()` chamado depois de toda mutação? Estado
   mutado fora da thread do servidor?

## Comandos

```bash
grep -rn "extends PersistentState\|writeNbt\|readNbt\|fromNbt\|toNbt" src/main/java
grep -rn "valueOf(" src/main/java | grep -i "nbt\|getString"
grep -rn "Identifier.of(\|new Identifier(" src/main/java
```

## Anti-padrões

- "A migração existe" porque há um `if ("BREEDER".equals(...))` — sem teste
  que leia um NBT antigo, não está provado.
- Teste de unidade que cria o objeto novo e o regrava: isso não exercita o
  caminho de leitura de dado antigo.
- `catch (Exception e) {}` silencioso na leitura: troca crash por perda de
  colônia sem rastro.
