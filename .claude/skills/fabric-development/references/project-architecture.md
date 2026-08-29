# Arquitetura de projeto

Onde o código mora, e quanta estrutura o projeto merece **hoje**.

## A pergunta central

**Qual é a menor arquitetura que ainda está correta?**

Os dois erros são simétricos e igualmente caros:

| Erro | Sintoma | Custo |
|---|---|---|
| **Framework prematuro** | `ItemFactory` para três itens; interface com uma implementação | cerimônia em toda mudança; indireção que não paga |
| **God Class** | uma classe de mil linhas que faz tudo | nada é testável; toda mudança toca o mesmo arquivo |

A escolha não é de gosto: é do **tamanho real do problema**. Um item novo não
precisa de fábrica. Um sistema de sete profissões com biomas e persistência
precisa de separação — e sem ela vira a classe de mil linhas.

## Quando abstração se paga

Só crie abstração quando houver **pelo menos um** destes, já presente:

```text
REPETIÇÃO              o mesmo código em três lugares (dois é coincidência)
VARIAÇÃO               casos que diferem de verdade, hoje
EXTENSIBILIDADE REAL   alguém vai estender — não "talvez um dia"
TESTABILIDADE          a abstração é o que permite testar sem subir Minecraft
SEPARAÇÃO DE DOMÍNIO   regra de negócio que não deveria conhecer BlockPos
```

"Pode ser útil no futuro" não está na lista. Abstração criada por expectativa é
paga hoje e quase nunca usada.

## Estrutura de pacotes

### Por tipo técnico — bom no começo

```text
mod/
├── item/  block/  entity/  mixin/  util/
```

Funciona até cerca de vinte classes. Depois, cada feature fica espalhada por
cinco pastas e nenhuma pasta conta uma história.

### Por domínio — bom quando cresce

```text
mod/
├── registry/
├── villager/
│   ├── ai/  profession/  task/  data/
├── mining/
│   ├── logic/  data/  integration/
├── automation/
├── network/
├── client/
└── compatibility/
```

A estrutura passa a **refletir o domínio**, e uma feature mora num lugar.

**Não aplique nenhuma das duas cegamente.** A estrutura deve crescer com o
projeto; reorganizar cedo demais é trabalho sem ganho.

## Núcleo independente do Minecraft

Quando a regra de domínio é rica, vale separá-la do jogo:

```text
core/       regra pura — não conhece BlockPos, ServerWorld, ItemStack
fabric/     lê o mundo, escreve no mundo, registra eventos
adapter/    a fronteira de conversão, num lugar só
data/       serialização
```

```java
// adapter — a fronteira, e só ela converte
public final class MinecraftTypeAdapter {
    public static ColonyPos toColonyPos(BlockPos pos) {
        return new ColonyPos(pos.getX(), pos.getY(), pos.getZ());
    }
    public static BlockPos toBlockPos(ColonyPos pos) {
        return new BlockPos(pos.x(), pos.y(), pos.z());
    }
}
```

**Ganho concreto:** o núcleo vira testável com JUnit puro, em milissegundos, sem
subir Minecraft. Regra de domínio complexa testada assim é incomparavelmente mais
barata de evoluir.

**Custo:** conversão de tipos na fronteira, e a disciplina de manter a fronteira
num lugar só.

Vale quando a regra é complexa e independente do jogo. **Não vale** para um bloco
que solta partícula — ali o "domínio" é o Minecraft.

Se adotar, a regra que faz funcionar: **nenhuma classe do core conhece tipo do
Minecraft, e nenhuma conversão acontece fora do adapter.** Sem isso, os tipos
vazam em um mês e a separação vira pasta vazia.

## Onde o estado mora

A pergunta que mais define arquitetura em mod: **quem é dono deste estado?**

| Estado | Dono natural |
|---|---|
| do momento, descartável | campo local |
| de uma entidade | a própria entidade (NBT / DataTracker) |
| de um bloco | BlockEntity |
| de um item | Data Components |
| do mundo | `PersistentState` |
| preferência do jogador/servidor | arquivo de config |

Estado sem dono claro vira `static Map`, e daí vêm quatro bugs de uma vez: não
persiste, vaza entre saves, não é thread-safe e ninguém sabe quem escreve.

Se um ponto de acesso global for mesmo necessário, torne-o **explícito e
limpo**: um lugar só, limpo no start e no stop do servidor, com a decisão
documentada. Ver `persistence.md`.

## Ponto de entrada

```java
public class MeuMod implements ModInitializer {
    public static final String MOD_ID = "meumod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModItems.register();
        ModBlocks.register();
        ServerLifecycleHandler.register();
    }
}
```

**O entrypoint chama; ele não implementa.** Se `onInitialize` cresce, é sinal de
que a lógica está no lugar errado. Acrescentar um evento não deveria significar
alterar esta classe.

Logger nomeado com o mod id faz toda linha ser atribuída a `(meumod)` e
filtrável — o que importa quando o jogador manda um log de trinta mods.

## Acoplamento entre features

```text
✗  MiningSystem lê campos internos de ConstructionSystem
✓  as duas conversam por um contrato explícito, ou por um estado com dono claro
```

Feature acoplada a detalhe interno de outra é o que torna impossível mexer numa
sem quebrar a outra — e é a forma mais comum de um mod ficar intocável.

## Sinais de que a arquitetura está errada

```text
[ ] uma mudança simples toca cinco arquivos não relacionados
[ ] ninguém sabe dizer quem é dono de um estado
[ ] a mesma informação existe em dois lugares e pode divergir
[ ] há uma classe que todo mundo importa
[ ] não dá para testar nada sem subir o jogo
[ ] há abstração com uma implementação só, criada "para o futuro"
[ ] o entrypoint tem lógica
```

Nem todo sinal exige ação imediata. Mas três ou mais juntos é dívida que vai
cobrar juros na próxima feature.
