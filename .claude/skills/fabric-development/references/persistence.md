# Persistência

A pergunta que organiza tudo: **o que sobrevive a quê?**

```text
tick  <  chunk unload  <  world save  <  server restart  <  troca de save
```

Escolher o mecanismo errado produz o bug característico — "o mod esquece tudo
quando eu fecho o mundo" — descoberto semanas depois de escrito.

## Mecanismos

| Mecanismo | Guarda | Sobrevive ao restart | Escopo |
|---|---|---|---|
| campo Java | estado do momento | não | instância |
| `static` | — | **não** | processo (perigoso) |
| `DataTracker` | o que o cliente vê | não | entidade |
| NBT de entidade | estado da entidade | **sim** | entidade |
| NBT de block entity | estado do bloco | **sim** | posição |
| Data Components | dados de item (1.20.5+) | **sim** | item |
| `PersistentState` | estado do mundo | **sim** | mundo |
| config | preferências | sim | instalação |

## A regra que mais falha

**Nada é salvo automaticamente.**

Um campo só persiste se houver código escrevendo **e** código lendo. Não existe
serialização implícita.

```bash
grep -rn "writeNbt\|readNbt\|writeCustomDataToNbt\|readCustomDataFromNbt" src/main/java
```

Campo sem par escrita/leitura = campo que some. Sem exceção.

## `PersistentState` — estado do mundo

Padrão verificado em MC 1.21.1:

```java
public final class MeuSavedData extends PersistentState {

    public static final String KEY = "meumod_dados";   // nome em data/. Mudar abandona saves.

    public static final PersistentState.Type<MeuSavedData> TYPE =
            new PersistentState.Type<>(MeuSavedData::new, MeuSavedData::readNbt, null);

    private final List<Coisa> coisas = new ArrayList<>();

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList lista = new NbtList();
        for (Coisa c : coisas) { ... }
        nbt.put("coisas", lista);
        return nbt;
    }

    private static MeuSavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        MeuSavedData data = new MeuSavedData();
        NbtList lista = nbt.getList("coisas", NbtElement.COMPOUND_TYPE);
        ...
        return data;
    }

    public void sync(Collection<Coisa> novas) {
        coisas.clear();
        coisas.addAll(novas);
        markDirty();                                    // ← sem isto, não grava
    }

    public static MeuSavedData get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE, KEY);
    }
}
```

Quatro pontos que decidem se funciona:

1. **`markDirty()`** depois de mudar. A perda sem ele é silenciosa: o estado
   parece certo até o restart.
2. **É por mundo.** `getOverworld()` prende ao Overworld — uma **decisão**, não um
   detalhe. Em servidor com múltiplos mundos, o estado é do Overworld, e isso
   precisa estar escrito.
3. **`RegistryWrapper.WrapperLookup`** nas assinaturas é 1.20.5+. Confirme com
   `javap`.
4. **`KEY` é o nome do arquivo.** Mudá-lo abandona os saves existentes.

## Dados relacionados vão no mesmo arquivo

Se A aponta para B por id, **A e B no mesmo `PersistentState`**.

Dois arquivos podem ser gravados em momentos diferentes; um restart no meio deixa
um registro órfão apontando para outro que não existe. **Não há transação entre
arquivos.**

## O que NÃO persistir

Tão importante quanto o que persistir. **Não salve o que o mundo já sabe:**

```text
posição de bloco que o jogador pode quebrar entre sessões
progresso de obra que é legível olhando o que está construído
resultado de varredura que é refeito barato
```

Persistir isso cria uma **segunda verdade que envelhece**. O save diz que há um
baú em X; o jogador quebrou o baú; o mod acredita numa coisa que não existe.
Redescobrir é mais barato que reconciliar.

O critério:

```text
Existe no mundo?        → pergunte ao mundo, não salve
Só existe na cabeça do mod?  → salve
```

Uma profissão atribuída, um id de colônia, uma reserva — isso não existe no mundo
e precisa ser salvo. A posição de um baú existe, e é redescobrível.

**Registre a decisão de não persistir**, com o motivo. Sem isso, alguém vai
"consertar" isso depois achando que é esquecimento.

## Estado estático e troca de save

`static` sobrevive ao **processo**, não ao mundo. O jogador fecha um save e abre
outro sem reiniciar — e o estado do mundo anterior vaza.

Se um ponto de acesso global for mesmo necessário:

```java
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    ESTADO.clear();                    // ← limpar antes de carregar
    carregar(server);
});

ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    salvar(server);
    ESTADO.clear();                    // ← e depois de salvar
});
```

**Nos dois lados, não em um só.** Esse bug é invisível em desenvolvimento — o dev
reinicia entre testes — e certo em produção.

E documente a decisão: acesso global é escolha, não acidente.

## Chunk unload

Entre o tick e o save existe a descarga: a block entity sai da memória sem o
mundo ser salvo. Estado em memória associado a uma posição some aí.

Se precisa sobreviver, o dono é a block entity (que persiste) ou o
`PersistentState` — não um mapa em memória.

## Migração de formato

Save antigo com formato novo é caso real assim que o mod tem usuários.

```text
[ ] o save antigo carrega?
[ ] campos novos têm default seguro?
[ ] campos removidos são ignorados sem erro?
[ ] há número de versão no NBT?
```

Um `int` de versão no compound raiz custa nada e salva o futuro. Sem ele, a
migração vira adivinhação.

**Teste explícito:** crie um mundo na versão anterior do mod, abra na nova.

## Checklist

```text
[ ] cada estado tem dono e mecanismo definidos
[ ] par escrita/leitura existe
[ ] markDirty após mutação
[ ] dados relacionados no mesmo arquivo
[ ] o que é redescobrível NÃO é persistido (e a decisão está escrita)
[ ] estado global limpo no start E no stop
[ ] versão gravada no formato
[ ] TESTADO: fechar e reabrir o mundo
[ ] TESTADO: save da versão anterior abre
```

Detalhamento em `checklists/persistence.md`. Plano em `templates/persistence-plan.md`.
