# Plano de persistência — <sistema>

> Preencha antes de adicionar estado. **Nada é salvo automaticamente**: só
> persiste o que tem par escrita/leitura.

**Minecraft:** <versão> · **Data:** AAAA-MM-DD

## Inventário de estado

| Estado | Dono | Mecanismo | Sobrevive ao restart | Sincroniza |
|---|---|---|---|---|
| | entidade / bloco / mundo / item | campo · DataTracker · NBT · PersistentState · Component | sim / não | sim / não |

> Estado que precisa **aparecer no cliente E sobreviver ao save** precisa de
> `DataTracker` **e** NBT — dois códigos, não um.

## O que NÃO será persistido

> Tão importante quanto o que será. Registre com o motivo, senão alguém
> "conserta" isto depois achando que é esquecimento.

| Estado | Por que não | Como é recuperado |
|---|---|---|
| | existe no mundo, redescobrível | varredura / consulta ao mundo |

**Critério:** existe no mundo → pergunte ao mundo. Só existe na cabeça do mod →
salve.

<Persistir o que o mundo já sabe cria uma segunda verdade que envelhece.>

## Escopo do arquivo

```text
[ ] quais dados vão no MESMO arquivo?
```

> Se A aponta para B por id, **A e B no mesmo arquivo**. Dois arquivos podem ser
> gravados em momentos diferentes; um restart no meio deixa registro órfão. Não há
> transação entre arquivos.

| Arquivo (`KEY`) | Contém |
|---|---|
| `meumod_<nome>` | |

<`KEY` é o nome do arquivo em `data/`. **Mudá-lo abandona os saves existentes.**>

## Mundo

<`server.getOverworld()` prende ao Overworld — é uma **decisão**. Em servidor com
múltiplos mundos, o estado é do Overworld. Escreva isso, não presuma.>

## Serialização

```text
[ ] há método de escrita
[ ] há método de leitura
[ ] markDirty() é chamado após TODA mutação
[ ] a assinatura confere com a versão (WrapperLookup é 1.20.5+)
```

<Confirme: `javap -cp "$MC_JAR" net.minecraft.world.PersistentState`>

## Estado global em memória

<Existe registro em memória? Onde vive?>

```text
[ ] limpo no SERVER_STARTED, antes de carregar
[ ] limpo no SERVER_STOPPING, depois de salvar
```

> Nos dois lados, não em um só. O processo abre outro save sem reiniciar, e o
> estado do mundo anterior vaza. Invisível em dev, certo em produção.

## Versionamento e migração

```text
[ ] há int de versão no compound raiz
[ ] campos novos têm default seguro
[ ] campos removidos são ignorados sem erro
[ ] save da versão anterior do mod carrega
```

<Sem versão gravada, migração futura vira adivinhação. Um `int` custa nada.>

## Chunk unload

<Algum estado está associado a uma posição e vive só em memória? Ele some quando
o chunk descarrega — o dono deveria ser a block entity ou o `PersistentState`.>

## Falhas

| Situação | Comportamento |
|---|---|
| campo ausente no save | default seguro, sem crash |
| tipo inesperado | default + aviso |
| dado corrompido | recriar vazio + aviso, não crashar |
| referência órfã (aponta para o que não existe) | descartar + aviso |

## Verificação

```text
[ ] criar mundo → usar → FECHAR → REABRIR → o estado voltou
[ ] mundo da versão anterior do mod abre sem perder dados
[ ] abrir outro save na mesma sessão não traz estado do anterior
[ ] gametest cobrindo save/load, se possível
```

> O primeiro pega o bug mais comum de todos: `markDirty` esquecido.
