# Data Components e NBT de item

Mudança estrutural em **1.20.5**: itens deixaram de carregar NBT solto e passaram
a **Data Components** — um mapa tipado, com codec e sincronização.

**Consequência direta: todo tutorial de "NBT customizado em item" anterior a
1.20.5 está errado para você.** Verifique antes de seguir qualquer exemplo.

## O que mudou

| | Antes (≤1.20.4) | Agora (1.20.5+) |
|---|---|---|
| Dados de item | `NbtCompound` solto | Data Components tipados |
| Tipagem | string-based, frágil | forte, com codec |
| Sincronização | manual | pelo codec |
| Descoberta de erro | runtime | compilação |

NBT **continua existindo** — para entidade, block entity, `PersistentState` e
dados de mundo. O que mudou foi especificamente o item stack.

## Verificar na sua versão

```bash
MC_JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name "minecraft-merged-*.jar" | grep -v intermediary | head -1)

unzip -l "$MC_JAR" | grep -i "DataComponentType"
javap -cp "$MC_JAR" net.minecraft.component.DataComponentTypes | head -40
```

Se `DataComponentTypes` existe, você está em 1.20.5+ e é este o caminho.

## Antes de criar componente próprio

```text
[ ] Um componente Vanilla já resolve?     (dano, encantamento, nome, lore, custom_data…)
[ ] O dado pertence mesmo ao ITEM?        ou ao bloco/entidade/mundo?
[ ] Registry entry resolveria?            se é conteúdo fixo, não dado por stack
[ ] PersistentState é mais adequado?      se é estado do mundo
```

O segundo é o mais importante. Dado que "acompanha o item" mas na verdade
descreve um lugar do mundo não pertence ao item — vai para onde o lugar mora.

## Escolher o mecanismo pelo ciclo de vida

```text
dado de um ITEM STACK específico  → Data Component
estado de uma ENTIDADE            → NBT de entidade + DataTracker se o cliente vê
estado de um BLOCO                → block state (se pequeno) ou BlockEntity NBT
estado do MUNDO                   → PersistentState
conteúdo fixo do mod              → registry
preferência do jogador/servidor   → config
```

**Não use uma estratégia para todos os tipos de dado.** É o erro que produz
`static Map<UUID, Coisa>` como banco de dados universal.

## Codec

O componente precisa de codec — para serializar no save e para sincronizar com o
cliente. Ele é o que torna o sistema tipado, e é onde a maior parte do trabalho
está.

```text
[ ] há codec de persistência
[ ] há codec de rede (pode ser o mesmo)
[ ] campos novos têm default → save antigo continua carregando
[ ] o componente é comparado por igualdade de forma sensata
```

O último importa mais do que parece: stacks só empilham se os componentes forem
iguais. Um componente com um timestamp faz cada item virar um stack de um.

## Sincronização

Componentes sincronizáveis chegam ao cliente automaticamente. Isso é conveniente
e tem um custo: **tudo que você põe no componente viaja pela rede** em toda
atualização do stack.

```text
[ ] o cliente precisa mesmo deste dado?
[ ] o componente é pequeno?
[ ] há dado sensível que não deveria ir para o cliente?
```

## Migração

Se o mod existia antes de 1.20.5, os itens salvos têm NBT antigo.

```text
[ ] item salvo no formato antigo ainda carrega?
[ ] há conversão, ou o dado é perdido?
[ ] a perda é aceitável e está DECLARADA?
```

Perda de dados silenciosa em item que o jogador acumulou é a pior forma de
descobrir uma migração. Se não vai migrar, diga — no changelog, antes de
publicar.

Ver `migration.md`.

## NBT onde ele ainda vale

```java
// entidade
@Override
public void writeCustomDataToNbt(NbtCompound nbt) {
    super.writeCustomDataToNbt(nbt);
    nbt.putInt("energia", energia);
}

// block entity (1.20.5+ leva o WrapperLookup)
@Override
protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
    super.writeNbt(nbt, registries);
    nbt.putInt("progresso", progresso);
}
```

Regras que continuam valendo:

```text
[ ] chave de NBT é contrato — renomear abandona o dado salvo
[ ] tipo lido tem que bater com o escrito
[ ] campo ausente precisa de default seguro, não de crash
[ ] getInt em chave inexistente devolve 0 — isso é bug ou default? decida
```

O último é sutil: o NBT não distingue "não existe" de "vale zero". Se a diferença
importa, grave um marcador explícito.

## Checklist

```text
[ ] verifiquei se a versão usa Data Components ou NBT para item
[ ] componente Vanilla verificado antes de criar próprio
[ ] o dado pertence mesmo ao item
[ ] codec de persistência e de rede
[ ] default para campos novos
[ ] o componente não impede stacking sem querer
[ ] migração de formato antigo decidida e declarada
[ ] TESTADO: item salvo antes, carregado depois
```
