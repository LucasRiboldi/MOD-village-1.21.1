# Checklist — persistência

> **Nada é salvo automaticamente.** Só persiste o que tem par escrita/leitura.

## Escolha do mecanismo

```text
[ ] cada estado tem dono e mecanismo definidos
[ ] o mecanismo bate com o que precisa sobreviver:
      tick < chunk unload < world save < server restart < troca de save
[ ] estado que o cliente vê E que persiste tem DOIS códigos
    (DataTracker + NBT) — não um
```

| Estado | Mecanismo correto |
|---|---|
| do momento | campo Java |
| de entidade | NBT de entidade |
| de bloco | block state (pequeno) ou NBT de block entity |
| de item | Data Components (1.20.5+) |
| do mundo | `PersistentState` |
| preferência | config |

## Serialização

```text
[ ] existe método de ESCRITA
[ ] existe método de LEITURA
[ ] os dois tratam os mesmos campos
[ ] o tipo lido bate com o escrito
[ ] a assinatura confere com a versão (WrapperLookup é 1.20.5+)
```

```bash
grep -rn "writeNbt\|readNbt\|writeCustomDataToNbt\|readCustomDataFromNbt" src/main/java
```

## markDirty

```text
[ ] markDirty() é chamado após TODA mutação
```

> Sem ele o jogo não grava, e a perda é **silenciosa**: o estado parece certo até
> o restart. É o bug de persistência mais comum que existe.

## Escopo do arquivo

```text
[ ] dados que se referenciam por id estão no MESMO arquivo
[ ] o KEY não mudou entre versões (mudar abandona os saves)
[ ] a decisão de prender ao Overworld está escrita, se aplicável
```

> Dois arquivos podem ser gravados em momentos diferentes; um restart no meio
> deixa registro órfão. **Não há transação entre arquivos.**

## O que NÃO persistir

```text
[ ] o que existe no mundo NÃO é persistido
[ ] a decisão de não persistir está ESCRITA, com o motivo
```

> Persistir o que o mundo já sabe cria uma segunda verdade que envelhece. E sem a
> decisão registrada, alguém "conserta" isso depois.

**Critério:** existe no mundo → pergunte ao mundo. Só existe na cabeça do mod →
salve.

## Estado global em memória

```text
[ ] nenhum estado de mundo em static solto
[ ] se há registro global, é limpo no SERVER_STARTED (antes de carregar)
[ ] e limpo no SERVER_STOPPING (depois de salvar)
[ ] a decisão de ter acesso global está documentada
```

> Nos dois lados, não em um só. O processo abre outro save sem reiniciar — e o
> estado do mundo anterior vaza. Invisível em dev, certo em produção.

## Chunk unload

```text
[ ] nenhum estado importante vive só em mapa de memória por posição
[ ] o dono é a block entity ou o PersistentState
```

## Versionamento

```text
[ ] há int de versão no compound raiz
[ ] campos novos têm default seguro
[ ] campos removidos são ignorados sem erro
[ ] campo ausente não crasha
```

> `getInt` em chave inexistente devolve 0. Se "ausente" e "zero" são diferentes
> para você, grave um marcador explícito.

## Falhas

```text
[ ] campo ausente → default seguro
[ ] tipo inesperado → default + aviso
[ ] dado corrompido → recriar vazio + aviso, nunca crashar
[ ] referência órfã → descartar + aviso
```

## Verificação executada

```text
[ ] criar mundo → usar a feature → FECHAR → REABRIR → o estado voltou
[ ] mundo da versão ANTERIOR do mod abre sem perder dados
[ ] abrir outro save na mesma sessão não traz estado do anterior
[ ] gametest cobrindo save/load, se possível
```

> O primeiro é o teste decisivo e quase nunca é feito. Ele pega o `markDirty`
> esquecido, o par escrita/leitura incompleto e o KEY errado — de uma vez.
