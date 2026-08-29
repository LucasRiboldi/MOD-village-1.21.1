# Análise de persistência

A pergunta que organiza tudo: **o que sobrevive a quê?**

```text
tick  <  chunk unload  <  world save  <  server restart  <  troca de save
```

Cada mecanismo cobre um trecho dessa linha. Escolher o errado produz o bug
característico: "o mod esquece tudo quando eu fecho o mundo" — descoberto semanas
depois de escrito.

## Mecanismos

| Mecanismo | Guarda | Sobrevive ao restart? | Escopo |
|---|---|---|---|
| Campo Java comum | estado do momento | **não** | instância |
| Campo `static` | — | **não** | processo (perigoso) |
| `DataTracker` (entidade) | estado visível ao cliente | não por si só | entidade |
| NBT de entidade | estado da entidade | **sim** | entidade |
| NBT de block entity | estado do bloco | **sim** | posição |
| Data Components | dados de item stack (1.20.5+) | **sim** | item |
| `PersistentState` | estado do mundo | **sim** | mundo |
| Arquivo de config | preferências | sim | instalação |

## A regra que mais falha

**Nunca assuma persistência só porque existe um campo.**

Não existe serialização automática. Um campo só é salvo se houver código
escrevendo **e** código lendo. Ao analisar, procure o par:

```bash
grep -rn "writeNbt\|readNbt\|writeCustomDataToNbt\|readCustomDataFromNbt" src/main/java | head -20
```

Campo sem par escrita/leitura = campo que some. Sem exceção.

## `PersistentState` — estado do mundo

Para o que pertence ao mundo e não a uma entidade ou bloco. Padrão verificado em
MC 1.21.1:

```java
public final class MeuSavedData extends PersistentState {

    public static final String KEY = "meumod_dados";       // nome em data/. Mudar invalida saves.

    public static final PersistentState.Type<MeuSavedData> TYPE =
            new PersistentState.Type<>(MeuSavedData::new, MeuSavedData::readNbt, null);

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ...
        return nbt;
    }

    private static MeuSavedData readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        ...
    }

    public static MeuSavedData get(MinecraftServer server) {
        return server.getOverworld()
                .getPersistentStateManager()
                .getOrCreate(TYPE, KEY);
    }
}
```

Pontos que decidem se funciona:

- **`markDirty()`** depois de mudar. Sem isso o jogo não grava, e a perda é
  silenciosa — o estado parece certo até o restart.
- **É por mundo.** `server.getOverworld()` prende ao Overworld, o que é uma
  decisão: em servidor com múltiplos mundos, o estado é do Overworld. Isso precisa
  ser escrito, não presumido.
- **`RegistryWrapper.WrapperLookup`** nas assinaturas é 1.20.5+. Confirme com
  `javap` antes de copiar qualquer exemplo.
- **`KEY` é o nome do arquivo** em `data/`. Mudá-lo abandona os saves existentes.

## O que sempre salvar junto

Dados que se referenciam por id **precisam ir no mesmo arquivo**. Dois arquivos
podem ser gravados em momentos diferentes; um restart no meio deixa um registro
órfão apontando para outro que não existe.

Se A aponta para B por id, A e B no mesmo `PersistentState`. Não há transação
entre arquivos.

## O que NÃO salvar

Tão importante quanto o que salvar. Não persista o que o **mundo já sabe**:

- posição de bloco que pode ser quebrado pelo jogador entre sessões
- progresso de obra que é legível olhando o que está construído
- resultado de varredura que é refeito barato

Persistir isso cria uma **segunda verdade** que envelhece. O save diz que há um
baú na posição X; o jogador quebrou o baú; agora o mod acredita numa coisa que não
existe. Redescobrir é mais barato que reconciliar.

O critério: **existe no mundo?** Então pergunte ao mundo. **Só existe na cabeça do
mod?** (uma profissão atribuída, um id de colônia) Então salve.

## Estado estático e troca de save

`static` sobrevive ao **processo**, não ao mundo. O jogador pode fechar um save e
abrir outro sem reiniciar — e o estado do mundo anterior vaza para o novo.

Se um ponto de acesso global for mesmo necessário, o mínimo é limpá-lo
explicitamente nos dois lados:

```java
ServerLifecycleEvents.SERVER_STARTED.register(server -> { registro.clear(); carregar(server); });
ServerLifecycleEvents.SERVER_STOPPING.register(server -> { salvar(server); registro.clear(); });
```

Limpar no **start** e no **stop**, não só num deles. E isso é decisão a
documentar, não detalhe de implementação.

## Chunk unload

Entre o tick e o save existe um evento intermediário: o chunk descarrega e a
block entity sai da memória, sem o mundo ser salvo. Estado em memória associado a
uma posição some aí.

Se a lógica precisa sobreviver a isso, o dono é a block entity (que persiste) ou
o `PersistentState`, não um mapa em memória.

## Migração de formato

Save antigo com formato novo é um caso real assim que o mod tem usuários.

```text
O que acontece ao ler um save gravado pela versão anterior?
Campos novos têm default?
Campos removidos são ignorados sem erro?
Há número de versão no NBT?
```

Sem versão gravada, a migração vira adivinhação. Um `int` de versão no compound
raiz custa nada e salva o futuro.

## O que investigar

```text
RUNTIME STATE → SAVE → SERIALIZATION → WORLD STORAGE → LOAD → RECONSTRUCTION
```

Para cada estado do sistema:

```text
Quem é o dono?
Qual mecanismo persiste?
Sobrevive a: tick? chunk unload? save? restart? troca de save?
Quem escreve? Quem lê? (mostre os dois)
markDirty é chamado?
O que acontece se faltar no save? (default seguro?)
Isso deveria ser persistido, ou é redescobrível?
```

## Sinais

**Bom:** par escrita/leitura visível; `markDirty` após mutação; dados
relacionados no mesmo arquivo; comentário dizendo por que algo **não** é
persistido; limpeza no start e no stop.

**Ruim:** `static Map` com estado de mundo; campo sem par de serialização;
persistir o que o mundo já guarda; nenhuma versão no formato; `getOrCreate` com
chave montada dinamicamente.
